package fr.xpdustry.javelin;

import com.esotericsoftware.kryo.kryo5.io.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;
import org.java_websocket.*;
import org.java_websocket.drafts.*;
import org.java_websocket.exceptions.*;
import org.java_websocket.extensions.permessage_deflate.*;
import org.java_websocket.framing.*;
import org.java_websocket.handshake.*;
import org.java_websocket.protocols.*;
import org.java_websocket.server.*;
import org.jetbrains.annotations.*;
import org.slf4j.*;

public class JavelinServer implements Closeable {

  private static final Logger logger = LoggerFactory.getLogger(JavelinServer.class);

  private final WebSocketServer server;
  private final Authenticator authenticator;
  private final AtomicBoolean started = new AtomicBoolean(false);

  public JavelinServer(final int port, final @NotNull Authenticator authenticator) {
    this.server = new JavelinWebSocketServer(port);
    this.authenticator = authenticator;
  }

  public void start() {
    server.start();
    started.set(true);
  }

  @Override
  public void close() throws IOException {
    try {
      server.stop();
    } catch (InterruptedException e) {
      throw new IOException("The server closing has been interrupted.", e);
    } finally {
      started.set(false);
    }
  }

  public final @NotNull Authenticator getAuthenticator() {
    return authenticator;
  }

  public final int getPort() {
    return server.getPort();
  }

  public final boolean isOpen() {
    return started.get();
  }

  public final boolean isClosed() {
    return !started.get();
  }

  private final class JavelinWebSocketServer extends WebSocketServer {

    private static final String AUTHORIZATION = "Authorization";
    private static final Pattern BASIC_AUTHORIZATION = Pattern.compile("^Basic (.+)$");

    public JavelinWebSocketServer(final int port) {
      super(new InetSocketAddress(port), List.of(new Draft_6455(Collections.singletonList(new PerMessageDeflateExtension()), List.of(new Protocol(""), new Protocol("ocpp2.0")))));
      setReuseAddr(true);
    }

    @Override
    public @NotNull ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(final @NotNull WebSocket conn, final @NotNull Draft draft, final @NotNull ClientHandshake request) throws InvalidDataException {
      final var authorization = request.getFieldValue(AUTHORIZATION);
      final var matcher = BASIC_AUTHORIZATION.matcher(authorization);

      if (!matcher.matches()) {
        throw new InvalidDataException(CloseFrame.POLICY_VALIDATION, "Invalid Authorization header.");
      }

      final String username;
      final char[] password;

      try {
        final var userPass = matcher.toMatchResult().group(1);
        final var decoded = new String(Base64.getDecoder().decode(userPass), StandardCharsets.UTF_8);
        final var parts = decoded.split(":", 2); // username:password
        username = parts[0];
        password = parts[1].toCharArray();
      } catch (final Exception e) {
        throw new InvalidDataException(CloseFrame.POLICY_VALIDATION, "Invalid credentials format.");
      }

      if (isConnected(username)) {
        throw new InvalidDataException(CloseFrame.POLICY_VALIDATION, "Already connected.");
      }
      if (authenticator.isValid(username, password)) {
        conn.setAttachment(username);
        return super.onWebsocketHandshakeReceivedAsServer(conn, draft, request);
      } else {
        throw new InvalidDataException(CloseFrame.POLICY_VALIDATION, "Invalid credentials.");
      }
    }

    @Override
    public void onStart() {
      logger.info("The server has been successfully started.");
    }

    @Override
    public void onOpen(final @NotNull WebSocket conn, final @NotNull ClientHandshake handshake) {
      logger.info("{} has connected.", getWebSocketName(conn));
    }

    @Override
    public void onClose(final @NotNull WebSocket conn, final int code, final @NotNull String reason, final boolean remote) {
      switch (code) {
        case CloseFrame.NORMAL, CloseFrame.GOING_AWAY -> logger.info("{} connection has been closed.", getWebSocketName(conn));
        default -> logger.error("{} connection has been closed unexpectedly (code={}, reason={}).", getWebSocketName(conn), code, reason);
      }
    }

    @Override
    public void onMessage(final @NotNull WebSocket conn, final @NotNull String message) {
      logger.debug("Received text message from {}, ignoring (message={}).", getWebSocketName(conn), message);
    }

    @Override
    public void onMessage(final @NotNull WebSocket conn, final @NotNull ByteBuffer message) {
      try (
        final var input = new ByteBufferInput(message);
        final var output = new Output(1024)
      ) {
        final var receiver = input.readString();
        final Collection<WebSocket> receivers;
        if (receiver == null) {
          receivers = getConnections();
          receivers.remove(conn);
        } else {
          receivers = getConnections().stream()
            .filter(c -> getWebSocketName(conn).equals(receiver))
            .toList();
        }
        output.writeString(getWebSocketName(conn)); // Replace receiver with sender
        output.write(input.readBytes(input.available()));
        broadcast(output.toBytes(), receivers);
      } catch (final IOException e) {
        logger.error("An exception occurred while dispatching a message.", e);
      }
    }

    @Override
    public void onError(final @NotNull WebSocket conn, final @NotNull Exception ex) {
      logger.error("An error occurred in {} connection.", getWebSocketName(conn), ex);
    }

    private @NotNull String getWebSocketName(final @NotNull WebSocket socket) {
      return socket.getAttachment();
    }

    private boolean isConnected(final @NotNull String username) {
      return getConnections().stream()
        .map(this::getWebSocketName)
        .anyMatch(name -> name.equals(username));
    }
  }
}
