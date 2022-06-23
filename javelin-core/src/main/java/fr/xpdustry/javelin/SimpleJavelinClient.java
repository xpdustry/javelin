package fr.xpdustry.javelin;

import com.esotericsoftware.kryo.kryo5.*;
import com.esotericsoftware.kryo.kryo5.io.*;
import fr.xpdustry.javelin.exception.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import org.java_websocket.client.*;
import org.java_websocket.drafts.*;
import org.java_websocket.extensions.permessage_deflate.*;
import org.java_websocket.framing.*;
import org.java_websocket.handshake.*;
import org.java_websocket.protocols.*;
import org.jetbrains.annotations.*;
import org.slf4j.*;

final class SimpleJavelinClient implements JavelinClient {

  private static final Logger logger = LoggerFactory.getLogger(SimpleJavelinClient.class);

  private final String username;
  private final String password;
  private final SimpleJavelinWebSocketClient socket;
  private final Kryo kryo = new Kryo();
  @SuppressWarnings("rawtypes")
  private final Map<MessageContext, MessageReceiver> receivers = new ConcurrentHashMap<>();
  private final ExecutorService executor;

  public SimpleJavelinClient(final @NotNull String username, final @NotNull String password, final @NotNull URI uri, final int workers) {
    if (username.contains(":")) {
      throw new IllegalArgumentException("username contains a colon (" + username + ").");
    }
    this.username = username;
    this.password = password;
    this.socket = new SimpleJavelinWebSocketClient(uri);
    this.executor = Executors.newFixedThreadPool(workers);

    kryo.setRegistrationRequired(false);
    kryo.setAutoReset(true);
    kryo.setOptimizedGenerics(false);
  }

  @Override
  public void connect() throws IOException {
    try {
      socket.connectBlocking();
    } catch (final InterruptedException e) {
      throw new IOException("Interrupted connect operation.", e);
    }
  }

  @Override
  public void reconnect() throws IOException {
    try {
      socket.reconnectBlocking();
    } catch (final InterruptedException e) {
      throw new IOException("Interrupted reconnect operation.", e);
    }
  }

  @Override
  public void close() throws IOException {
    try {
      socket.closeBlocking();
      executor.shutdown();
    } catch (final InterruptedException e) {
      throw new IOException("Interrupted close operation.");
    }
  }

  @Override
  public <T> void broadcastMessage(final @NotNull MessageContext<T> context, final @NotNull T message) throws MessageSendException {
    send(context, message, null);
  }

  @Override
  public <T> void sendMessage(final @NotNull MessageContext<T> context, final @NotNull T message, final @NotNull String receiver) throws MessageSendException {
    send(context, message, receiver);
  }

  @Override
  public <T> void bindReceiver(@NotNull MessageContext<T> context, @NotNull MessageReceiver<T> receiver) {
    receivers.put(context, receiver);
  }

  @Override
  public <T> void unbindReceiver(@NotNull MessageContext<T> context, @NotNull MessageReceiver<T> receiver) {
    receivers.remove(context, receiver);
  }

  @Override
  public boolean isConnected() {
    return socket.isOpen();
  }

  @Override
  public boolean isClosed() {
    return socket.isClosed();
  }

  @Override
  public @NotNull String getUsername() {
    return username;
  }

  @Override
  public @NotNull String getPassword() {
    return password;
  }

  private <T> void send(final @NotNull MessageContext<T> context, final @NotNull T message, final @Nullable String receiver) throws MessageSendException {
    if (isClosed()) {
      throw new MessageSendException("The client is not yet connected.");
    }
    try (final var output = new Output(1024)) {
      output.writeString(receiver);
      output.writeString(context.getNamespace());
      output.writeString(context.getSubject());
      kryo.writeClass(output, context.getMessageType());
      kryo.writeObject(output, message);
      socket.send(output.toBytes());
    } catch (final KryoBufferOverflowException e) {
      throw new MessageSendException("The message size is too big.", e);
    }
  }

  private final class SimpleJavelinWebSocketClient extends WebSocketClient {

    public SimpleJavelinWebSocketClient(final @NotNull URI remote) {
      super(remote, new Draft_6455(Collections.singletonList(new PerMessageDeflateExtension()), List.of(new Protocol(""), new Protocol("ocpp2.0"))));
      final var userPass = username + ":" + password;
      final var encoded = Base64.getEncoder().encodeToString(userPass.getBytes(StandardCharsets.UTF_8));
      addHeader("Authorization", "Basic " + encoded);
    }

    @Override
    public void onOpen(final @NotNull ServerHandshake handshake) {
      logger.info("The connection has been successfully established with the server.");
    }

    @Override
    public void onMessage(final @NotNull String message) {
      logger.debug("Received text message, ignoring (message={}).", message);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void onMessage(final @NotNull ByteBuffer bytes) {
      try (final var input = new ByteBufferInput(bytes)) {
        final var sender = input.readString();
        final var namespace = input.readString();
        final var subject = input.readString();
        final var messageType = kryo.readClass(input).getType();
        final var context = new MessageContext(namespace, subject, messageType);
        final var receiver = receivers.get(context);
        if (receiver != null) {
          final var message = kryo.readObject(input, messageType);
          executor.execute(() -> receiver.handleMessage(message, sender));
        } else {
          logger.debug("Received message with no registered receiver (context={}, sender={}).", context, sender);
        }
      }
    }

    @Override
    public void onClose(final int code, final @NotNull String reason, final boolean remote) {
      switch (code) {
        case CloseFrame.NORMAL -> logger.info("The connection has been closed.");
        case CloseFrame.GOING_AWAY -> logger.info("The connection has been closed by the server.");
        default -> logger.error("The connection has been unexpectedly closed (code={}, reason={}).", code, reason);
      }
    }

    @Override
    public void onError(final @NotNull Exception ex) {
      logger.error("An exception occurred.", ex);
    }
  }
}
