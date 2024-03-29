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
import java.util.*;

final class PropertiesJavelinConfig implements JavelinConfig {

    private static final String SERVER_PORT_KEY = "fr.xpdustry.javelin.server.port",
            CLIENT_USERNAME_KEY = "fr.xpdustry.javelin.client.username",
            CLIENT_PASSWORD_KEY = "fr.xpdustry.javelin.client.password",
            CLIENT_SERVER_URI_KEY = "fr.xpdustry.javelin.client.address",
            MODE_KEY = "fr.xpdustry.javelin.socket.mode",
            WORKERS_KEY = "fr.xpdustry.javelin.socket.workers",
            ALWAYS_ALLOW_LOCAL_CONNECTIONS = "fr.xpdustry.javelin.server.always-allow-local-connections",
            AUTO_RESTART = "fr.xpdustry.javelin.socket.auto-restart",
            INITIAL_CONNECTION_TIMEOUT = "fr.xpdustry.javelin.socket.initial-connection-timeout",
            LOCAL_BROADCAST = "fr.xpdustry.javelin.socket.local-broadcast";

    private static final Properties DEFAULTS = new Properties();

    static {
        DEFAULTS.putAll(Map.of(
                SERVER_PORT_KEY, "8080",
                CLIENT_USERNAME_KEY, "",
                CLIENT_PASSWORD_KEY, "",
                CLIENT_SERVER_URI_KEY, "ws://localhost:8080",
                MODE_KEY, "NONE",
                WORKERS_KEY, "1",
                ALWAYS_ALLOW_LOCAL_CONNECTIONS, "false",
                AUTO_RESTART, "true",
                INITIAL_CONNECTION_TIMEOUT, "3",
                LOCAL_BROADCAST, "false"));
    }

    private final Properties properties;

    PropertiesJavelinConfig(final Properties properties) {
        this.properties = new Properties(DEFAULTS);
        this.properties.putAll(properties);
    }

    static Properties getDefaults() {
        return DEFAULTS;
    }

    @Override
    public Mode getMode() {
        return Mode.valueOf(properties.getProperty(MODE_KEY));
    }

    @Override
    public int getServerPort() {
        return Integer.parseInt(properties.getProperty(SERVER_PORT_KEY));
    }

    @Override
    public String getClientUsername() {
        return properties.getProperty(CLIENT_USERNAME_KEY);
    }

    @Override
    public char[] getClientPassword() {
        return properties.getProperty(CLIENT_PASSWORD_KEY).toCharArray();
    }

    @Override
    public URI getClientServerUri() {
        return URI.create(properties.getProperty(CLIENT_SERVER_URI_KEY));
    }

    @Override
    public int getWorkerCount() {
        return Integer.parseInt(properties.getProperty(WORKERS_KEY));
    }

    @Override
    public boolean alwaysAllowLocalConnections() {
        return Boolean.parseBoolean(properties.getProperty(ALWAYS_ALLOW_LOCAL_CONNECTIONS));
    }

    @Override
    public boolean isAutoRestartEnabled() {
        return Boolean.parseBoolean(properties.getProperty(AUTO_RESTART));
    }

    @Override
    public int getInitialConnectionTimeout() {
        return Integer.parseInt(properties.getProperty(INITIAL_CONNECTION_TIMEOUT));
    }

    @Override
    public boolean isLocalBroadcastEnabled() {
        return Boolean.parseBoolean(properties.getProperty(LOCAL_BROADCAST));
    }
}
