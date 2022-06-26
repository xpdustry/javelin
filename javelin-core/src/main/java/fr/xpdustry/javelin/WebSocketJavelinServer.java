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

final class WebSocketJavelinServer extends WebSocketServer implements JavelinServer {

  private static final Logger logger = LoggerFactory.getLogger(WebSocketJavelinServer.class);

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final Pattern BASIC_AUTHORIZATION_PATTERN = Pattern.compile("^Basic (.+)$");

  private final Authenticator authenticator;
  private boolean started = false;

  public WebSocketJavelinServer(final int port, final int workers, final @NotNull Authenticator authenticator) {
    super(new InetSocketAddress(port), workers, Collections.singletonList(new Draft_6455(
      Collections.singletonList(new PerMessageDeflateExtension()),
      List.of(new Protocol(""), new Protocol("ocpp2.0"))
    )));

    setReuseAddr(true);
    this.authenticator = authenticator;
  }

  @Override
  public void start() {
    super.start();
    started = true;
  }

  @Override
  public void close() {
    try {
      stop();
    } catch (final InterruptedException ignored) {
    } finally {
      started = false;
    }
  }

  @Override
  public @NotNull Authenticator getAuthenticator() {
    return authenticator;
  }

  @Override
  public boolean isOpen() {
    return started;
  }

  @Override
  public boolean isClosed() {
    return !started;
  }

  @Override
  public void onStart() {
    logger.info("The server has been successfully started.");
  }

  @Override
  public @NotNull ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(final @NotNull WebSocket conn, final @NotNull Draft draft, final @NotNull ClientHandshake request) throws InvalidDataException {
    final var authorization = request.getFieldValue(AUTHORIZATION_HEADER);
    final var matcher = BASIC_AUTHORIZATION_PATTERN.matcher(authorization);

    if (!matcher.matches()) {
      throw new InvalidDataException(CloseFrame.POLICY_VALIDATION, "Invalid Authorization header.");
    }

    final String username;
    final char[] password;

    try {
      final var userPass = matcher.toMatchResult().group(1);
      final var decoded = new String(Base64.getDecoder().decode(userPass), StandardCharsets.UTF_8);
      final var parts = decoded.split(":", 2);
      username = parts[0];
      password = parts[1].toCharArray();
    } catch (final Exception e) {
      throw new InvalidDataException(CloseFrame.POLICY_VALIDATION, "Invalid credentials format.");
    }

    if (!authenticator.isValid(username, password)) {
      throw new InvalidDataException(CloseFrame.POLICY_VALIDATION, "Invalid credentials.");
    }
    if (isConnected(username)) {
      throw new InvalidDataException(CloseFrame.POLICY_VALIDATION, "Already connected.");
    }

    conn.setAttachment(username);
    return super.onWebsocketHandshakeReceivedAsServer(conn, draft, request);
  }

  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake) {
    logger.info("{} has connected.", getWebSocketName(conn));
  }

  @Override
  public void onClose(WebSocket conn, int code, String reason, boolean remote) {
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
        receivers = new ArrayList<>(getConnections());
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
