package fr.xpdustry.javelin;

import com.esotericsoftware.kryo.kryo5.io.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
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

final class SimpleJavelinServer implements JavelinServer {

  private static final Logger logger = LoggerFactory.getLogger(SimpleJavelinServer.class);

  private final SimpleJavelinWebSocketServer server;
  private final JavelinAuthenticator authenticator;

  public SimpleJavelinServer(final int port, final @NotNull JavelinAuthenticator authenticator) {
    this.server = new SimpleJavelinWebSocketServer(port);
    this.authenticator = authenticator;
  }

  @Override
  public void start() {
    server.start();
  }

  @Override
  public void close() throws IOException {
    try {
      server.stop();
    } catch (InterruptedException e) {
      throw new IOException("The server closing has been interrupted.", e);
    }
  }

  @Override
  public @NotNull JavelinAuthenticator getAuthenticator() {
    return authenticator;
  }

  @Override
  public int getPort() {
    return server.getPort();
  }

  private final class SimpleJavelinWebSocketServer extends WebSocketServer {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final Pattern AUTHORIZATION_VALUE = Pattern.compile("^Basic (.+)$");

    public SimpleJavelinWebSocketServer(final int port) {
      super(new InetSocketAddress(port), List.of(new Draft_6455(Collections.singletonList(new PerMessageDeflateExtension()), List.of(new Protocol(""), new Protocol("ocpp2.0")))));
      setReuseAddr(true);
    }

    @Override
    public @NotNull ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(final @NotNull WebSocket conn, final @NotNull Draft draft, final @NotNull ClientHandshake request) throws InvalidDataException {
      final var authorization = request.getFieldValue(AUTHORIZATION_HEADER);
      final var matcher = AUTHORIZATION_VALUE.matcher(authorization);

      if (!matcher.matches()) {
        throw new InvalidDataException(CloseFrame.POLICY_VALIDATION, "Invalid Authorization header.");
      }

      final String username;
      final String password;

      try {
        final var userPass = matcher.toMatchResult().group(1);
        final var decoded = new String(Base64.getDecoder().decode(userPass), StandardCharsets.UTF_8);
        final var parts = decoded.split(":", 2);
        username = parts[0];
        password = parts[1];
      } catch (final Exception e) {
        e.printStackTrace();
        throw new InvalidDataException(CloseFrame.POLICY_VALIDATION, "Invalid credentials format.");
      }

      if (getConnections().stream().map(WebSocket::<String>getAttachment).anyMatch(name -> name.equals(username))) {
        throw new InvalidDataException(CloseFrame.POLICY_VALIDATION, "Already connected.");
      } else if (authenticator.isValid(username, password)) {
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
      logger.info("The client {} has connected.", conn.<String>getAttachment());
    }

    @Override
    public void onClose(final @NotNull WebSocket conn, final int code, final @NotNull String reason, final boolean remote) {
      switch (code) {
        case CloseFrame.NORMAL, CloseFrame.GOING_AWAY -> logger.info("The client {} connection has been closed.", conn.<String>getAttachment());
        default -> logger.error("The client {} connection has been closed unexpectedly (code={}, reason={}).", conn.<String>getAttachment(), code, reason);
      }
    }

    @Override
    public void onMessage(final @NotNull WebSocket conn, final @NotNull String message) {
      logger.debug("Received text message from {}, ignoring (message={}).", conn.<String>getAttachment(), message);
    }

    @Override
    public void onMessage(final @NotNull WebSocket conn, final @NotNull ByteBuffer message) {
      try (
        final var input = new ByteBufferInput(message);
        final var output = new Output(1024)
      ) {
        final var receiver = input.readString();
        final List<WebSocket> targets;
        if (receiver == null) {
          targets = getConnections().stream().filter(c -> c != conn).toList();
        } else {
          targets = getConnections().stream().filter(c -> c.getAttachment().equals(receiver)).toList();
        }
        output.writeString(conn.getAttachment());
        output.write(input.readBytes(input.available()));
        targets.forEach(c -> c.send(output.toBytes()));
      } catch (final IOException e) {
        logger.error("An exception occurred while dispatching a message.", e);
      }
    }

    @Override
    public void onError(final @NotNull WebSocket conn, final @NotNull Exception ex) {
      logger.error("An error occurred in {} connection.", conn.<String>getAttachment(), ex);
    }
  }
}
