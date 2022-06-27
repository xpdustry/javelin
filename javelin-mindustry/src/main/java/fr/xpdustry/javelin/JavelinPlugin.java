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
import arc.util.*;
import java.io.*;
import java.nio.charset.*;
import java.util.*;
import mindustry.mod.*;
import org.jetbrains.annotations.*;

public final class JavelinPlugin extends Plugin {

  private static final File DIRECTORY = new File("javelin");
  private static final File CONFIG_FILE = new File(DIRECTORY, "config.properties");

  private static final UserAuthenticator authenticator = new SimpleUserAuthenticator(DIRECTORY);
  private static final JavelinCommand commands = new JavelinCommand(authenticator);

  private static JavelinConfig config;
  private static JavelinServer server;
  private static JavelinClient client;

  public static @NotNull JavelinClient getClient() {
    return client;
  }

  public static @NotNull JavelinServer getServer() {
    return server;
  }

  public static @NotNull JavelinConfig getConf() {
    return config;
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Override
  public void init() {
    DIRECTORY.mkdir();
    config = readConfig();

    server = new JavelinServer(config.getServerPort(), config.getServerWorkerCount(), authenticator);

    if (config.isServerEnabled()) {
      Core.app.addListener(new ApplicationListener() {
        @Override
        public void init() {
          server.start();
        }

        @Override
        public void exit() {
          server.close();
        }
      });
    }

    client = new JavelinClient(config.getClientServerUri(), config.getClientUsername(), config.getClientPassword());
    client.setConnectionLostTimeout(config.getClientConnectionLostTimeout());

    if (config.isClientEnabled()) {
      Core.app.addListener(new ApplicationListener() {
        @Override
        public void init() {
          client.connect();
        }

        @Override
        public void exit() {
          client.close();
        }
      });
    }
  }

  @Override
  public void registerServerCommands(final @NotNull CommandHandler handler) {
    commands.registerServerCommands(handler);
  }

  private @NotNull JavelinConfig readConfig() {
    final var properties = new Properties();
    if (CONFIG_FILE.exists()) {
      try (final var reader = new FileReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
        properties.load(reader);
      } catch (IOException e) {
        throw new RuntimeException("Invalid config.", e);
      }
    } else {
      try (final var writer = new FileWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
        PropertiesJavelinConfig.getDefaults().store(writer, null);
      } catch (IOException e) {
        throw new RuntimeException("Can't create default config for Javelin.", e);
      }
    }
    return new PropertiesJavelinConfig(properties);
  }
}
