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

import java.io.*;
import java.net.*;
import org.jetbrains.annotations.*;

public interface JavelinClient extends Closeable {

  static @NotNull JavelinClient websocket(final @NotNull String username, final char[] password, final @NotNull URI server) {
    return new WebSocketJavelinClient(username, password, server);
  }

  void connect() throws IOException;

  void reconnect() throws IOException;

  <T> void sendMessage(final @NotNull MessageContext<T> context, final @NotNull T message, final @NotNull String receiver) throws IOException;

  <T> void broadcastMessage(final @NotNull MessageContext<T> context, final @NotNull T message) throws IOException;

  <T> void bindReceiver(final @NotNull MessageContext<? extends T> context, final @NotNull MessageReceiver<T> receiver);

  <T> void unbindReceiver(final @NotNull MessageContext<? extends T> context, final @NotNull MessageReceiver<T> receiver);

  @NotNull URI getServerUri();

  @NotNull String getUsername();

  char[] getPassword();

  boolean isConnected();

  boolean isClosed();
}
