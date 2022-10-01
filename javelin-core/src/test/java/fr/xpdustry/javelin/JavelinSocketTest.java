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
import java.time.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

public final class JavelinSocketTest {

  private TestJavelinAuthenticator authenticator;
  private JavelinServerSocket server;
  private JavelinClientSocket client1;
  private JavelinClientSocket client2;

  @BeforeEach
  void setup() {
    authenticator = new TestJavelinAuthenticator();
    server = new JavelinServerSocket(2000, 1, authenticator);
    client1 = createClient("client1");
    client2 = createClient("client2");
  }

  @Test
  void test_simple_event_send() {
    final var received = new CompletableFuture<TestEvent>();

    // Setup clients
    authenticator.addUser("client1", "client1");
    authenticator.addUser("client2", "client2");

    // Start
    assertSocketStart(server);
    assertSocketStart(client1);
    assertSocketStart(client2);

    // Subscribe
    client2.subscribe(TestEvent.class, received::complete);

    // Test receive
    final var event = new TestEvent("hey");
    assertSocketSend(client1, event);
    assertReceivedEvent(event, received);

    // Close
    assertSocketClose(client1);
    assertSocketClose(client2);
    assertSocketClose(server);
  }

  @Test
  void test_unknown_user_connection() {
    // Setup server
    authenticator.addUser("client1", "client1");

    // Start
    assertSocketStart(server);

    // Test
    Assertions.assertThrows(IOException.class, () -> {
      try {
        client2.start().join();
      } catch (final CompletionException e) {
        throw e.getCause();
      }
    });

    // Close
    assertSocketClose(server);
  }

  @Test
  void test_same_user_connection() {
    // Setup server
    authenticator.addUser("client1", "client1");

    // Start
    assertSocketStart(server);
    assertSocketStart(client1);

    // Test
    Assertions.assertThrows(IOException.class, () -> {
      try {
        final var client1Clone = createClient("client1");
        client1Clone.start().join();
      } catch (final CompletionException e) {
        throw e.getCause();
      }
    });

    // Close
    assertSocketClose(client1);
    assertSocketClose(server);
  }

  @Test
  void test_server_broadcast() {
    final var received1 = new CompletableFuture<TestEvent>();
    final var received2 = new CompletableFuture<TestEvent>();

    // Setup clients
    authenticator.addUser("client1", "client1");
    authenticator.addUser("client2", "client2");

    // Start
    assertSocketStart(server);
    assertSocketStart(client1);
    assertSocketStart(client2);

    // Subscribe, send and check event
    client1.subscribe(TestEvent.class, received1::complete);
    client2.subscribe(TestEvent.class, received2::complete);

    // Test receive
    final var event = new TestEvent("hey");
    assertSocketSend(server, event);
    assertReceivedEvent(event, received1);
    assertReceivedEvent(event, received2);

    // Close
    assertSocketClose(client1);
    assertSocketClose(client2);
    assertSocketClose(server);
  }

  private JavelinClientSocket createClient(final String name) {
    return new JavelinClientSocket(URI.create("ws://localhost:2000"), name, name.toCharArray(), 1);
  }

  private void assertSocketStart(final JavelinSocket socket) {
    Assertions.assertTimeout(Duration.ofSeconds(10L), () -> socket.start().join());
  }

  private void assertSocketClose(final JavelinSocket socket) {
    Assertions.assertTimeout(Duration.ofSeconds(10L), () -> socket.close().join());
  }

  private <E extends JavelinEvent> void assertSocketSend(final JavelinSocket socket, E event) {
    Assertions.assertTimeout(Duration.ofSeconds(10L), () -> socket.sendEvent(event).join());
  }

  private <E extends JavelinEvent> void assertReceivedEvent(final E event, final CompletableFuture<E> future) {
    Assertions.assertTimeout(Duration.ofSeconds(10L), () -> Assertions.assertEquals(event, future.join()));
  }
}
