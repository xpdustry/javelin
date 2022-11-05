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

import static org.assertj.core.api.Assertions.*;

import fr.xpdustry.javelin.JavelinSocket.*;
import java.net.*;
import java.time.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

public final class JavelinClientSocketTest {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5L);

    private JavelinServerSocket server;
    private JavelinClientSocket client;

    @BeforeEach
    void setup() {
        server = new JavelinServerSocket(12345, 1, true, new TestJavelinAuthenticator());
        client = new JavelinClientSocket(URI.create("ws://localhost:12345"), 1, null);
    }

    @Test
    void test_client_simple() {
        assertThat(client.getStatus()).isEqualTo(Status.CLOSED);
        assertThat(server.getStatus()).isEqualTo(Status.CLOSED);

        assertThat(server.start()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(client.start()).succeedsWithin(DEFAULT_TIMEOUT);

        assertThat(client.getStatus()).isEqualTo(Status.OPEN);
        assertThat(server.getStatus()).isEqualTo(Status.OPEN);

        assertThat(client.close()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(server.close()).succeedsWithin(DEFAULT_TIMEOUT);

        assertThat(client.getStatus()).isEqualTo(Status.UNUSABLE);
        assertThat(server.getStatus()).isEqualTo(Status.UNUSABLE);
    }

    @Test
    void test_client_restart() {
        assertThat(server.start()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(client.start()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(client.restart()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(client.close()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(server.close()).succeedsWithin(DEFAULT_TIMEOUT);
    }

    @Test
    void test_client_restart_with_server() {
        final var server1 = new JavelinServerSocket(12345, 1, true, new TestJavelinAuthenticator());
        assertThat(server1.start()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(client.start()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(server1.close()).succeedsWithin(DEFAULT_TIMEOUT);

        final var server2 = new JavelinServerSocket(12345, 1, true, new TestJavelinAuthenticator());
        assertThat(server2.start()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(client.restart()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(client.close()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(server2.close()).succeedsWithin(DEFAULT_TIMEOUT);
    }

    @Test
    void test_client_send_event() {
        final var event = new TestEvent("bob");
        final var received = new CompletableFuture<TestEvent>();

        assertThat(server.start()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(client.start()).succeedsWithin(DEFAULT_TIMEOUT);

        server.subscribe(TestEvent.class, received::complete);
        assertThat(client.sendEvent(event)).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(received).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(received).isCompletedWithValueMatching(event::equals);

        assertThat(client.close()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(server.close()).succeedsWithin(DEFAULT_TIMEOUT);
    }

    @Test
    void test_client_fails_serverless_connection() {
        assertThat(client.start()).failsWithin(DEFAULT_TIMEOUT);
    }
}
