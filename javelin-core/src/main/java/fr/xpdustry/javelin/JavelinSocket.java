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

import java.net.*;
import java.util.concurrent.*;
import java.util.function.*;

public interface JavelinSocket {

    static JavelinSocket server(final int port, final int workers, final JavelinAuthenticator authenticator) {
        return new JavelinServerSocket(port, workers, false, authenticator);
    }

    static JavelinSocket server(
            final int port,
            final int workers,
            boolean alwaysAllowLocalConnections,
            final JavelinAuthenticator authenticator) {
        return new JavelinServerSocket(port, workers, alwaysAllowLocalConnections, authenticator);
    }

    static JavelinSocket client(final URI serverUri, final String username, final char[] password, final int workers) {
        return new JavelinClientSocket(serverUri, workers, new PasswordAuthentication(username, password));
    }

    static JavelinSocket client(final URI serverUri, final int workers) {
        return new JavelinClientSocket(serverUri, workers, null);
    }

    static JavelinSocket noop() {
        return NoopJavelinSocket.INSTANCE;
    }

    CompletableFuture<Void> start();

    default CompletableFuture<Void> restart() {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("This socket cannot be restarted."));
    }

    CompletableFuture<Void> close();

    <E extends JavelinEvent> CompletableFuture<Void> sendEvent(final E event);

    <E extends JavelinEvent> Subscription subscribe(final Class<E> event, final Consumer<E> subscriber);

    Status getStatus();

    enum Status {
        OPENING,
        OPEN,
        CLOSING,
        CLOSED,
        UNUSABLE
    }

    interface Subscription {

        void unsubscribe();
    }
}
