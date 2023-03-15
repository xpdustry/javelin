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

import static org.assertj.core.api.Assertions.*;

import java.net.*;
import java.time.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

public final class JavelinServerSocketTest {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5L);

    private TestJavelinAuthenticator authenticator;
    private JavelinServerSocket server;
    private JavelinClientSocket client1;
    private JavelinClientSocket client2;

    @BeforeEach
    void setup() {
        authenticator = new TestJavelinAuthenticator();
        server = new JavelinServerSocket(12345, 1, true, authenticator);
        client1 = createClient("client1");
        client2 = createClient("client2");
    }

    @Test
    void test_multiple_clients_simple() {
        assertThat(server.start()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(client1.start()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(client2.start()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(client1.close()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(client2.close()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(server.close()).succeedsWithin(DEFAULT_TIMEOUT);
    }

    @Test
    void test_clients_with_credentials() {
        server = new JavelinServerSocket(12345, 1, false, authenticator);
        authenticator.addUser("client1", "client1");

        assertThat(server.start()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(client1.start()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(client2.start()).failsWithin(DEFAULT_TIMEOUT);
        assertThat(client1.close()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(server.close()).succeedsWithin(DEFAULT_TIMEOUT);
    }

    @Test
    void test_server_event_broadcast() {
        final var event = new TestEvent("hey");
        final var received1 = new CompletableFuture<TestEvent>();
        final var received2 = new CompletableFuture<TestEvent>();

        // Start
        assertThat(server.start()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(client1.start()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(client2.start()).succeedsWithin(DEFAULT_TIMEOUT);

        // Subscribe
        client1.subscribe(TestEvent.class, received1::complete);
        client2.subscribe(TestEvent.class, received2::complete);

        // Send event
        assertThat(server.sendEvent(event)).succeedsWithin(DEFAULT_TIMEOUT);

        // Receive event
        assertThat(CompletableFuture.allOf(received1, received2)).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(received1).isCompletedWithValueMatching(event::equals);
        assertThat(received2).isCompletedWithValueMatching(event::equals);

        // Close
        assertThat(client1.close()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(client2.close()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(server.close()).succeedsWithin(DEFAULT_TIMEOUT);
    }

    @Test
    void test_client_event_broadcast() {
        final var event = new TestEvent("hey");
        final var received1 = new CompletableFuture<TestEvent>();
        final var received2 = new CompletableFuture<TestEvent>();

        // Start
        assertThat(server.start()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(client1.start()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(client2.start()).succeedsWithin(DEFAULT_TIMEOUT);

        // Subscribe
        server.subscribe(TestEvent.class, received1::complete);
        client1.subscribe(TestEvent.class, received2::complete);

        // Send event
        assertThat(client2.sendEvent(event)).succeedsWithin(DEFAULT_TIMEOUT);

        // Receive event
        assertThat(CompletableFuture.allOf(received1, received2)).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(received1).isCompletedWithValueMatching(event::equals);
        assertThat(received2).isCompletedWithValueMatching(event::equals);

        // Close
        assertThat(client1.close()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(client2.close()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(server.close()).succeedsWithin(DEFAULT_TIMEOUT);
    }

    @Test
    void test_same_user_connection() {
        final var server = new JavelinServerSocket(12345, 1, false, authenticator);
        authenticator.addUser("client1", "client1");

        final var client1A = createClient("client1");
        final var client1B = createClient("client1");

        assertThat(server.start()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(client1A.start()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(client1B.start()).failsWithin(DEFAULT_TIMEOUT);
        assertThat(client1A.close()).succeedsWithin(DEFAULT_TIMEOUT);
        assertThat(server.close()).succeedsWithin(DEFAULT_TIMEOUT);
    }

    private JavelinClientSocket createClient(final String name) {
        return new JavelinClientSocket(
                URI.create("ws://localhost:12345"), 1, new PasswordAuthentication(name, name.toCharArray()));
    }
}
