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

import arc.*;
import arc.files.*;
import arc.util.*;
import java.io.*;
import java.util.*;
import mindustry.mod.*;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.*;

@SuppressWarnings("NullAway.Init")
public final class JavelinPlugin extends Plugin {

  private static final Fi CONFIG_FILE = new Fi("./javelin.properties");
  private static final Authenticator authenticator = new SimpleAuthenticator();

  private static JavelinConfig config;
  private static JavelinServer server;
  private static JavelinClient client;

  public static @Nullable JavelinClient getClient() {
    return client;
  }

  public static @Nullable JavelinServer getServer() {
    return server;
  }

  public static JavelinConfig getConf() {
    return config;
  }

  @Override
  public void init() {
    config = readConfig();

    if (config.isServerEnabled()) {
      server = JavelinServer.websocket(config.getServerPort(), config.getServerWorkerCount(), authenticator);

      Core.app.addListener(new ApplicationListener() {
        @Override
        public void init() {
          try {
            server.start();
          } catch (IOException e) {
            throw new RuntimeException("Failed to start Javelin server.");
          }
        }

        @Override
        public void exit() {
          try {
            server.close();
          } catch (IOException e) {
            Log.err("Failed to close javelin server.", e);
          }
        }
      });
    }

    if (config.isClientEnabled()) {
      client = JavelinClient.websocket(config.getClientUsername(), config.getClientPassword(), config.getClientServerUri());

      Core.app.addListener(new ApplicationListener() {
        @Override
        public void init() {
          try {
            client.connect();
          } catch (IOException e) {
            throw new RuntimeException("Failed to start Javelin client.");
          }
        }

        @Override
        public void exit() {
          try {
            client.close();
          } catch (IOException e) {
            Log.err("Failed to close javelin client.", e);
          }
        }
      });
    }
  }

  private @NotNull JavelinConfig readConfig() {
    final var properties = new Properties();
    if (CONFIG_FILE.exists()) {
      try (final var reader = CONFIG_FILE.reader()) {
        properties.load(reader);
      } catch (IOException e) {
        throw new RuntimeException("Invalid config.", e);
      }
    } else {
      try (final var writer = CONFIG_FILE.writer(false)) {
        PropertiesJavelinConfig.getDefaults().store(writer, null);
      } catch (IOException e) {
        throw new RuntimeException("Can't create default config for Javelin.", e);
      }
    }
    return new PropertiesJavelinConfig(properties);
  }
}
