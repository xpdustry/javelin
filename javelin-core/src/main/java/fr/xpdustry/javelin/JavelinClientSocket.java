/*
 * Javelin, a simple communication protocol for broadcasting events on a network.
 *
 * Copyright (C) 2023 Xpdustry
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
import org.checkerframework.checker.nullness.qual.*;
import org.java_websocket.client.*;
import org.java_websocket.framing.*;
import org.java_websocket.handshake.*;

final class JavelinClientSocket extends AbstractJavelinSocket {

    private final AtomicBoolean connecting = new AtomicBoolean();
    private final ExecutorService executor;
    private final JavelinClientWebSocket socket;

    JavelinClientSocket(final URI serverUri, final int workers, final @Nullable PasswordAuthentication authentication) {
        this.executor = Executors.newFixedThreadPool(workers);
        this.socket = new JavelinClientWebSocket(serverUri, authentication);
    }

    @Override
    public CompletableFuture<Void> start() {
        if (getStatus() != Status.UNUSABLE && getStatus() == Status.CLOSED && connecting.compareAndSet(false, true)) {
            final var future = new CompletableFuture<Void>();
            ForkJoinPool.commonPool().execute(() -> {
                try {
                    if (!socket.connectBlocking()) {
                        future.completeExceptionally(new IOException("Failed to connect."));
                    } else {
                        future.complete(null);
                    }
                } catch (final InterruptedException e) {
                    future.cancel(true);
                } finally {
                    connecting.set(false);
                }
            });
            return future;
        }
        return CompletableFuture.failedFuture(
                new IllegalStateException("The client socket can't be started in it's current state."));
    }

    @Override
    public CompletableFuture<Void> restart() {
        if (getStatus() != Status.UNUSABLE && getStatus() != Status.CLOSING && connecting.compareAndSet(false, true)) {
            final var future = new CompletableFuture<Void>();
            ForkJoinPool.commonPool().execute(() -> {
                try {
                    if (!socket.reconnectBlocking()) {
                        future.completeExceptionally(new IOException("Failed to connect."));
                    } else {
                        future.complete(null);
                    }
                } catch (final InterruptedException e) {
                    future.cancel(true);
                } finally {
                    connecting.set(false);
                }
            });
            return future;
        }
        return CompletableFuture.failedFuture(
                new IllegalStateException("The client socket can't be restarted in it's current state."));
    }

    @Override
    public CompletableFuture<Void> close() {
        if (getStatus() == Status.OPEN) {
            final var future = new CompletableFuture<Void>();
            ForkJoinPool.commonPool().execute(() -> {
                try {
                    socket.closeBlocking();
                    executor.shutdown();
                    future.complete(null);
                } catch (final InterruptedException e) {
                    future.cancel(true);
                }
            });
            return future;
        } else if (!executor.isShutdown()) {
            return CompletableFuture.runAsync(executor::shutdown);
        }
        return CompletableFuture.failedFuture(
                new IllegalStateException("The client socket can't be closed in it's current state."));
    }

    @Override
    protected void onEventSend(final ByteBuffer buffer) {
        socket.send(buffer);
    }

    @Override
    public Status getStatus() {
        if (connecting.get()) {
            return Status.OPENING;
        }
        return switch (socket.getReadyState()) {
            case OPEN -> Status.OPEN;
            case CLOSING -> Status.CLOSING;
            default -> executor.isShutdown() ? Status.UNUSABLE : Status.CLOSED;
        };
    }

    private final class JavelinClientWebSocket extends WebSocketClient {

        private JavelinClientWebSocket(final URI uri, final @Nullable PasswordAuthentication authentication) {
            super(uri, Internal.getJavelinDraft());
            if (authentication != null) {
                final var username = authentication.getUserName();
                final var password = authentication.getPassword();
                if (username.contains(":")) {
                    throw new IllegalArgumentException("username contains a colon: " + username);
                }
                final var userPass = (username + ':' + String.valueOf(password)).getBytes(StandardCharsets.UTF_8);
                this.addHeader(
                        Internal.AUTHORIZATION_HEADER,
                        "Basic " + Base64.getEncoder().encodeToString(userPass));
            }
            this.setConnectionLostTimeout(60);
        }

        @Override
        public void onOpen(final ServerHandshake handshake) {
            logger.info("The connection has been successfully established with the server.");
        }

        @Override
        public void onMessage(final String message) {
            logger.info("Received text message, ignoring (message={}).", message);
        }

        @Override
        public void onMessage(final ByteBuffer bytes) {
            executor.execute(() -> onEventReceive(bytes));
        }

        @Override
        public void onClose(final int code, final String reason, final boolean remote) {
            switch (code) {
                case CloseFrame.NORMAL -> logger.info("The connection has been closed.");
                case CloseFrame.GOING_AWAY -> logger.info("The connection has been closed by the server.");
                default -> logger.error(
                        "The connection has been unexpectedly closed (code={}, reason={}).", code, reason);
            }
        }

        @Override
        public void onError(final Exception ex) {
            logger.error("An exception occurred in the websocket client.", ex);
        }
    }
}
