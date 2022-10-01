/*
 * Javelin, a simple communication protocol for broadcasting events on a network.
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

import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.java_websocket.*;
import org.java_websocket.drafts.*;
import org.java_websocket.exceptions.*;
import org.java_websocket.framing.*;
import org.java_websocket.handshake.*;
import org.java_websocket.server.*;
import org.jetbrains.annotations.*;
import org.slf4j.*;

final class JavelinServerSocket extends AbstractJavelinSocket {

  private static final Logger logger = LoggerFactory.getLogger(JavelinServerSocket.class);

  private final AtomicReference<Status> status = new AtomicReference<>(Status.CLOSED);
  private final JavelinServerWebSocket socket;

  JavelinServerSocket(final int port, final int workers, final @NotNull JavelinAuthenticator authenticator) {
    super(workers);
    this.socket = new JavelinServerWebSocket(port, authenticator);
  }

  @Override
  public @NotNull CompletableFuture<Void> start() {
    return CompletableFuture.runAsync(() -> {
      if (status.get() == Status.CLOSED) {
        socket.start();
        status.set(Status.OPEN);
      }
    });
  }

  @Override
  public @NotNull CompletableFuture<Void> close() {
    return CompletableFuture.runAsync(() -> {
      if (status.get() == Status.OPEN) {
        status.set(Status.CLOSING);
        try {
          socket.stop();
        } catch (final InterruptedException e) {
          throw new CompletionException(e);
        } finally {
          status.set(Status.CLOSED);
        }
      }
    }).thenCompose(v -> super.close());
  }

  @Override
  protected void onEventSend(final @NotNull ByteBuffer buffer) {
    socket.broadcast(buffer);
  }

  @SuppressWarnings("NullAway") // dafuq ?
  @Override
  public @NotNull Status getStatus() {
    return status.get();
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  private final class JavelinServerWebSocket extends WebSocketServer {

    private final JavelinAuthenticator authenticator;

    private JavelinServerWebSocket(final int port, final @NotNull JavelinAuthenticator authenticator) {
      super(new InetSocketAddress(port), Collections.singletonList(Internal.getJavelinDraft()));
      this.authenticator = authenticator;
      this.setReuseAddr(true);
      this.setConnectionLostTimeout(0);
    }

    @Override
    public void onStart() {
      logger.info("The server has been successfully started.");
    }

    @Override
    public @NotNull ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(final @NotNull WebSocket conn, final @NotNull Draft draft, final @NotNull ClientHandshake request) throws InvalidDataException {
      final var authorization = request.getFieldValue(Internal.AUTHORIZATION_HEADER);
      final var matcher = Internal.AUTHORIZATION_PATTERN.matcher(authorization);

      if (!matcher.matches()) {
        rejectConnection(conn, "Invalid Authorization header");
      }

      try {
        final var userPass = matcher.toMatchResult().group(1);
        final var decoded = new String(Base64.getDecoder().decode(userPass), StandardCharsets.UTF_8);
        final var parts = decoded.split(":", 2);

        final var username = parts[0];
        final var password = parts[1].toCharArray();

        if (!authenticator.authenticate(username, password)) {
          rejectConnection(conn, "Invalid credentials");
        }
        // Check if already connected
        if (getConnections().stream().map(this::getWebSocketName).anyMatch(name -> name.equals(username))) {
          rejectConnection(conn, "Already connected");
        }

        conn.setAttachment(username);
        return super.onWebsocketHandshakeReceivedAsServer(conn, draft, request);
      } catch (final Exception e) {
        rejectConnection(conn, "Invalid credential format");
      }

      throw new IllegalStateException();
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
      final var receivers = new ArrayList<>(this.getConnections());
      receivers.remove(conn);
      broadcast(message, receivers);
      onEventReceive(message);
    }

    @Override
    public void onError(final @NotNull WebSocket conn, final @NotNull Exception ex) {
      logger.error("An error occurred in {} connection.", getWebSocketName(conn), ex);
    }

    private @NotNull String getWebSocketName(final @NotNull WebSocket socket) {
      return socket.getAttachment();
    }

    private void rejectConnection(final @NotNull WebSocket conn, final @NotNull String reason) throws InvalidDataException {
      logger.info("Rejected connection from {}: {}", conn.getRemoteSocketAddress(), reason);
      throw new InvalidDataException(CloseFrame.POLICY_VALIDATION, reason);
    }
  }
}
