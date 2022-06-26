/*
 * Javelin, a cross server communication library for Mindustry.
 *
 * Copyright (C) 2022 Xpdustry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.xpdustry.javelin;

import com.esotericsoftware.kryo.kryo5.*;
import com.esotericsoftware.kryo.kryo5.io.*;
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

final class WebSocketJavelinClient extends WebSocketClient implements JavelinClient {

  private static final Logger logger = LoggerFactory.getLogger(WebSocketJavelinClient.class);

  private final String username;
  private final char[] password;

  @SuppressWarnings("rawtypes")
  private final Map<MessageContext, MessageReceiver> receivers = new ConcurrentHashMap<>();
  private final Kryo kryo = new Kryo();
  private final ExecutorService executor = Executors.newCachedThreadPool();

  public WebSocketJavelinClient(final @NotNull String username, final char[] password, final @NotNull URI server) {
    super(server, new Draft_6455(
      Collections.singletonList(new PerMessageDeflateExtension()),
      List.of(new Protocol(""), new Protocol("ocpp2.0"))
    ));

    if (username.contains(":")) {
      throw new IllegalArgumentException("username contains a colon: " + username);
    }

    this.username = username;
    this.password = password;

    final var userPass = username + ':' + String.valueOf(password);
    final var encoded = Base64.getEncoder().encodeToString(userPass.getBytes(StandardCharsets.UTF_8));
    addHeader("Authorization", "Basic " + encoded);

    kryo.setRegistrationRequired(false);
    kryo.setAutoReset(true);
    kryo.setOptimizedGenerics(false);
  }

  @Override
  public <T> void sendMessage(@NotNull MessageContext<T> context, @NotNull T message, @NotNull String receiver) throws IOException {
    send(context, message, receiver);
  }

  @Override
  public <T> void broadcastMessage(@NotNull MessageContext<T> context, @NotNull T message) throws IOException {
    send(context, message, null);
  }

  @Override
  public <T> void bindReceiver(@NotNull MessageContext<? extends T> context, @NotNull MessageReceiver<T> receiver) {
    receivers.put(context, receiver);
  }

  @Override
  public <T> void unbindReceiver(@NotNull MessageContext<? extends T> context, @NotNull MessageReceiver<T> receiver) {
    receivers.remove(context, receiver);
  }

  @Override
  public void close() {
    super.close();
    executor.shutdown();
  }

  @Override
  public @NotNull URI getServerUri() {
    return uri;
  }

  @Override
  public @NotNull String getUsername() {
    return username;
  }

  @Override
  public char @NotNull [] getPassword() {
    return password;
  }

  @Override
  public boolean isConnected() {
    return isOpen();
  }

  @Override
  public void onOpen(final @NotNull ServerHandshake handshake) {
    logger.info("The connection has been successfully established with the server.");
  }

  @Override
  public void onMessage(final @NotNull String message) {
    logger.info("Received text message, ignoring (message={}).", message);
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
      }
      logger.trace("Received message from {} (context={})", sender, context);
    }
  }

  @Override
  public void onClose(final int code, @NotNull String reason, final boolean remote) {
    switch (code) {
      case CloseFrame.NORMAL -> logger.info("The connection has been closed.");
      case CloseFrame.GOING_AWAY -> logger.info("The connection has been closed by the server.");
      default -> logger.error("The connection has been unexpectedly closed (code={}, reason={}).", code, reason);
    }
  }

  @Override
  public void onError(final @NotNull Exception exception) {
    logger.error("An exception occurred.", exception);
  }

  private <T> void send(final @NotNull MessageContext<T> context, final @NotNull T message, final @Nullable String receiver) throws IOException {
    if (isClosed()) {
      throw new IOException("The client is not yet connected.");
    }
    try (final var output = new ByteBufferOutput(2048)) {
      output.writeString(receiver);
      output.writeString(context.getNamespace());
      output.writeString(context.getEndpoint());
      kryo.writeClass(output, context.getMessageType());
      kryo.writeObject(output, message);
      send(output.toBytes());
    } catch (final KryoBufferOverflowException e) {
      throw new IOException("The message size is too big.", e);
    }
  }
}
