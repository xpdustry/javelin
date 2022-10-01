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

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.java_websocket.client.*;
import org.java_websocket.framing.*;
import org.java_websocket.handshake.*;
import org.jetbrains.annotations.*;
import org.slf4j.*;

final class JavelinClientSocket extends AbstractJavelinSocket {

  private static final Logger logger = LoggerFactory.getLogger(JavelinClientSocket.class);

  private final AtomicBoolean connecting = new AtomicBoolean();
  private final JavelinClientWebSocket socket;

  JavelinClientSocket(final @NotNull URI serverUri, final @NotNull String username, final char @NotNull [] password, final int workers) {
    super(workers);
    this.socket = new JavelinClientWebSocket(serverUri, username, password);
  }

  @Override
  public @NotNull CompletableFuture<Void> start() {
    if (getStatus() == Status.CLOSED) {
      return CompletableFuture.runAsync(() -> {
        try {
          if (!socket.connectBlocking()) {
            throw new IOException("Failed to connect.");
          }
        } catch (final InterruptedException | IOException e) {
          throw new CompletionException(e);
        }
      });
    }
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public @NotNull CompletableFuture<Void> close() {
    return CompletableFuture.runAsync(() -> {
      if (getStatus() != Status.OPEN) {
        try {
          socket.closeBlocking();
        } catch (final InterruptedException e) {
          throw new CompletionException(e);
        }
      }
    }).thenCompose(v -> super.close());
  }

  @Override
  protected void onEventSend(final @NotNull ByteBuffer buffer) {
    socket.send(buffer);
  }

  @Override
  public @NotNull Status getStatus() {
    if (connecting.get()) {
      return Status.OPENING;
    }
    return switch (socket.getReadyState()) {
      case OPEN -> Status.OPEN;
      case CLOSING -> Status.CLOSING;
      default -> Status.CLOSED;
    };
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  private final class JavelinClientWebSocket extends WebSocketClient {

    private JavelinClientWebSocket(final URI uri, final @NotNull String username, final char @NotNull [] password) {
      super(uri, Internal.getJavelinDraft());
      if (username.contains(":")) {
        throw new IllegalArgumentException("username contains a colon: " + username);
      }

      final var userPass = (username + ':' + String.valueOf(password)).getBytes(StandardCharsets.UTF_8);
      this.addHeader(Internal.AUTHORIZATION_HEADER, "Basic " + Base64.getEncoder().encodeToString(userPass));
      this.setConnectionLostTimeout(0);
    }

    @Override
    public void onOpen(final @NotNull ServerHandshake handshake) {
      logger.info("The connection has been successfully established with the server.");
    }

    @Override
    public void onMessage(final @NotNull String message) {
      logger.info("Received text message, ignoring (message={}).", message);
    }

    @Override
    public void onMessage(final @NotNull ByteBuffer bytes) {
      onEventReceive(bytes);
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
      logger.error("An exception occurred in the websocket client.", ex);
    }
  }
}
