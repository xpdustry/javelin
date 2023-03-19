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

import static org.assertj.core.api.Assertions.assertThat;

import java.net.*;
import java.time.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

public final class JavelinSocketTest {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5L);

    @Test
    void test_server_local_broadcast() {
        final var server = new JavelinServerSocket(12345, 1, true, new TestJavelinAuthenticator(), true);
        final var completed = new CompletableFuture<TestEvent>();
        assertThat(server.start()).succeedsWithin(DEFAULT_TIMEOUT);

        final var event = new TestEvent("test");
        server.subscribe(TestEvent.class, completed::complete);
        server.sendEvent(event);
        assertThat(completed).isCompletedWithValue(event);

        assertThat(server.close()).succeedsWithin(DEFAULT_TIMEOUT);
    }

    @Test
    void test_client_local_broadcast() {
        final var server = new JavelinServerSocket(12345, 1, true, new TestJavelinAuthenticator(), false);
        final var client = new JavelinClientSocket(URI.create("ws://localhost:12345"), 1, null, true);
        final var completed = new CompletableFuture<TestEvent>();

        assertThat(server.start()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(client.start()).succeedsWithin(DEFAULT_TIMEOUT);

        final var event = new TestEvent("test");
        client.subscribe(TestEvent.class, completed::complete);
        client.sendEvent(event);
        assertThat(completed).isCompletedWithValue(event);

        assertThat(client.close()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(server.close()).succeedsWithin(DEFAULT_TIMEOUT);
    }
}
