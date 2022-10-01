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
import java.util.*;
import org.jetbrains.annotations.*;

final class PropertiesJavelinConfig implements JavelinConfig {

  private static final String
    SERVER_PORT_KEY = "fr.xpdustry.javelin.server.port",
    CLIENT_USERNAME_KEY = "fr.xpdustry.javelin.client.username",
    CLIENT_PASSWORD_KEY = "fr.xpdustry.javelin.client.password",
    CLIENT_SERVER_URI_KEY = "fr.xpdustry.javelin.client.address",
    MODE_KEY = "fr.xpdustry.javelin.socket.mode",
    WORKERS_KEY = "fr.xpdustry.javelin.socket.workers";

  private static final Properties DEFAULTS = new Properties();

  static {
    DEFAULTS.putAll(
      Map.of(
        SERVER_PORT_KEY, "8080",
        CLIENT_USERNAME_KEY, "unknown",
        CLIENT_PASSWORD_KEY, "unknown",
        CLIENT_SERVER_URI_KEY, "ws://localhost:8080",
        MODE_KEY, "NONE",
        WORKERS_KEY, "1"
      )
    );
  }

  private final Properties properties;

  PropertiesJavelinConfig(final @NotNull Properties properties) {
    this.properties = new Properties(DEFAULTS);
    this.properties.putAll(properties);
  }

  static @NotNull Properties getDefaults() {
    return DEFAULTS;
  }

  @Override
  public @NotNull Mode getMode() {
    return Mode.valueOf(properties.getProperty(MODE_KEY));
  }

  @Override
  public int getServerPort() {
    return Integer.parseInt(properties.getProperty(SERVER_PORT_KEY));
  }

  @Override
  public @NotNull String getClientUsername() {
    return properties.getProperty(CLIENT_USERNAME_KEY);
  }

  @Override
  public char @NotNull [] getClientPassword() {
    return properties.getProperty(CLIENT_PASSWORD_KEY).toCharArray();
  }

  @Override
  public @NotNull URI getClientServerUri() {
    return URI.create(properties.getProperty(CLIENT_SERVER_URI_KEY));
  }

  @Override
  public int getWorkerCount() {
    return Integer.parseInt(properties.getProperty(WORKERS_KEY));
  }
}
