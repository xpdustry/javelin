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

import java.util.concurrent.*;
import java.util.function.*;

final class NoopJavelinSocket implements JavelinSocket {

    static final NoopJavelinSocket INSTANCE = new NoopJavelinSocket();

    private NoopJavelinSocket() {}

    @Override
    public CompletableFuture<Void> start() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> restart() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public <E extends JavelinEvent> CompletableFuture<Void> sendEvent(final E event) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public <E extends JavelinEvent> Subscription subscribe(final Class<E> event, final Consumer<E> subscriber) {
        return () -> {};
    }

    @Override
    public Status getStatus() {
        return Status.CLOSED;
    }
}
