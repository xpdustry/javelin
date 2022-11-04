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
import org.checkerframework.checker.nullness.qual.*;
import org.java_websocket.*;
import org.java_websocket.drafts.*;
import org.java_websocket.exceptions.*;
import org.java_websocket.framing.*;
import org.java_websocket.handshake.*;
import org.java_websocket.server.*;

final class JavelinServerSocket extends AbstractJavelinSocket {

    private final JavelinServerWebSocket socket;
    private final boolean alwaysAllowLocalConnections;

    JavelinServerSocket(
            final int port,
            final int workers,
            final boolean alwaysAllowLocalConnections,
            final JavelinAuthenticator authenticator) {
        this.socket = new JavelinServerWebSocket(port, workers, authenticator);
        this.alwaysAllowLocalConnections = alwaysAllowLocalConnections;
    }

    @Override
    public CompletableFuture<Void> start() {
        if (socket.status.compareAndSet(Status.CLOSED, Status.OPENING)) {
            try {
                socket.start();
            } catch (final IllegalStateException e) {
                socket.startFuture.completeExceptionally(e);
            }
            return socket.startFuture.copy();
        }
        return CompletableFuture.failedFuture(
                new IllegalStateException("The socket can't be started in it's current state"));
    }

    @Override
    public CompletableFuture<Void> close() {
        if (socket.status.get() == Status.OPEN) {
            final var future = new CompletableFuture<Void>();
            ForkJoinPool.commonPool().execute(() -> {
                try {
                    socket.stop();
                    future.complete(null);
                } catch (final InterruptedException e) {
                    future.cancel(true);
                }
            });
            return future;
        }
        return CompletableFuture.failedFuture(
                new IllegalStateException("The socket can't be closed in it's current state"));
    }

    @Override
    protected void onEventSend(final ByteBuffer buffer) {
        socket.broadcast(buffer);
    }

    @SuppressWarnings("NullAway")
    @Override
    public Status getStatus() {
        return socket.status.get();
    }

    private final class JavelinServerWebSocket extends WebSocketServer {

        private final AtomicReference<Status> status = new AtomicReference<>(Status.CLOSED);
        private final CompletableFuture<Void> startFuture = new CompletableFuture<>();
        private final JavelinAuthenticator authenticator;

        private JavelinServerWebSocket(final int port, final int workers, final JavelinAuthenticator authenticator) {
            super(new InetSocketAddress(port), workers, Collections.singletonList(Internal.getJavelinDraft()));
            this.authenticator = authenticator;
            this.setReuseAddr(true);
            this.setConnectionLostTimeout(60);
        }

        @Override
        public void onStart() {
            logger.info("The server has been successfully started.");
            status.set(Status.OPEN);
            startFuture.complete(null);
        }

        @Override
        public ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(
                final WebSocket conn, final Draft draft, final ClientHandshake request) throws InvalidDataException {
            final var authorization = request.getFieldValue(Internal.AUTHORIZATION_HEADER);
            final var matcher = Internal.AUTHORIZATION_PATTERN.matcher(authorization);

            if (alwaysAllowLocalConnections
                    && conn.getRemoteSocketAddress().getAddress().isLoopbackAddress()) {
                final var address = conn.getRemoteSocketAddress().getAddress();
                if (address.isAnyLocalAddress() || address.isLoopbackAddress()) {
                    return super.onWebsocketHandshakeReceivedAsServer(conn, draft, request);
                }
            }
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
                if (getConnections().stream()
                        .map(WebSocket::<String>getAttachment)
                        .anyMatch(username::equals)) {
                    rejectConnection(conn, "Already connected");
                }

                conn.setAttachment(username);
                return super.onWebsocketHandshakeReceivedAsServer(conn, draft, request);
            } catch (final InvalidDataException e) {
                throw e;
            } catch (final Exception e) {
                rejectConnection(conn, "Invalid credential format");
            }

            throw new IllegalStateException();
        }

        @Override
        public void onOpen(final WebSocket conn, final ClientHandshake handshake) {
            logger.info("{} has connected.", conn.getRemoteSocketAddress());
        }

        @Override
        public void onClose(final WebSocket conn, final int code, final String reason, final boolean remote) {
            switch (code) {
                case CloseFrame.NORMAL, CloseFrame.GOING_AWAY -> logger.info(
                        "The connection {} has been closed.", conn.getRemoteSocketAddress());
                default -> logger.error(
                        "The connection {} has been closed unexpectedly (code={}, reason={}).",
                        conn.getRemoteSocketAddress(),
                        code,
                        reason);
            }
        }

        @Override
        public void onMessage(final WebSocket conn, final String message) {
            logger.debug(
                    "Received text message from {}, ignoring (message={}).", conn.getRemoteSocketAddress(), message);
        }

        @Override
        public void onMessage(final WebSocket conn, final ByteBuffer message) {
            final var receivers = new ArrayList<>(this.getConnections());
            receivers.remove(conn);
            broadcast(message, receivers);
            onEventReceive(message);
        }

        @Override
        public void onError(final @Nullable WebSocket conn, final Exception ex) {
            if (!startFuture.isDone()) {
                status.set(Status.CLOSED);
                startFuture.completeExceptionally(ex);
            }
            if (conn == null) {
                logger.error("An error occurred in the Javelin server.", ex);
            } else {
                logger.error(
                        "An error occurred in a client connection (address={}).", conn.getRemoteSocketAddress(), ex);
            }
        }

        private void rejectConnection(final WebSocket conn, final String reason) throws InvalidDataException {
            logger.info("Rejected connection from {}: {}", conn.getRemoteSocketAddress(), reason);
            throw new InvalidDataException(CloseFrame.POLICY_VALIDATION, reason);
        }

        @Override
        public void run() {
            super.run();
            status.set(Status.CLOSED);
        }
    }
}
