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
import java.util.concurrent.*;
import java.util.function.*;
import org.jetbrains.annotations.*;

public interface JavelinSocket {

  static @NotNull JavelinSocket server(final int port, final int workers, final @NotNull JavelinAuthenticator authenticator) {
    return new JavelinServerSocket(port, workers, authenticator);
  }

  static @NotNull JavelinSocket client(final @NotNull URI serverUri, final @NotNull String username, final char @NotNull [] password, final int workers) {
    return new JavelinClientSocket(serverUri, username, password, workers);
  }

  static @NotNull JavelinSocket noop() {
    return NoopJavelinSocket.INSTANCE;
  }

  @NotNull CompletableFuture<Void> start();

  @NotNull CompletableFuture<Void> close();

  <E extends JavelinEvent> @NotNull CompletableFuture<Void> sendEvent(final @NotNull E event);

  <E extends JavelinEvent> @NotNull Subscription subscribe(final @NotNull Class<E> event, final @NotNull Consumer<E> subscriber);

  @NotNull Status getStatus();

  enum Status {
    OPENING, OPEN, CLOSING, CLOSED
  }

  interface Subscription {

    void unsubscribe();
  }
}
