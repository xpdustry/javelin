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

import arc.*;
import arc.util.*;
import fr.xpdustry.javelin.JavelinConfig.*;
import fr.xpdustry.javelin.JavelinSocket.*;
import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import mindustry.mod.*;
import org.checkerframework.checker.nullness.qual.*;

public final class JavelinPlugin extends Plugin {

    private static final File DIRECTORY = new File("javelin");
    private static final File CONFIG_FILE = new File(DIRECTORY, "config.properties");

    private static JavelinSocket socket = JavelinSocket.noop();
    private static UserAuthenticator authenticator =
            UserAuthenticator.create(new File(DIRECTORY, "users-v2.bin.gz").toPath());
    private static @MonotonicNonNull JavelinConfig config = null;

    public static UserAuthenticator getUserAuthenticator() {
        return authenticator;
    }

    public static void setUserAuthenticator(final UserAuthenticator authenticator) {
        JavelinPlugin.authenticator = authenticator;
    }

    public static JavelinConfig getJavelinConfig() {
        return config;
    }

    public static JavelinSocket getJavelinSocket() {
        return socket;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void init() {
        DIRECTORY.mkdir();

        // Load the config
        final var properties = new Properties();
        if (CONFIG_FILE.exists()) {
            try (final var reader = new FileReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (final IOException e) {
                throw new RuntimeException("Invalid config.", e);
            }
        } else {
            try (final var writer = new FileWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
                PropertiesJavelinConfig.getDefaults().store(writer, null);
            } catch (final IOException e) {
                throw new RuntimeException("Can't create default config for Javelin.", e);
            }
        }
        config = new PropertiesJavelinConfig(properties);

        // Setup Javelin
        if (config.getMode() == JavelinConfig.Mode.SERVER) {
            socket = JavelinSocket.server(
                    config.getServerPort(),
                    config.getWorkerCount(),
                    config.alwaysAllowLocalConnections(),
                    authenticator,
                    config.isLocalBroadcastEnabled());
        } else if (config.getMode() == JavelinConfig.Mode.CLIENT) {
            if (config.getClientUsername().isBlank()) {
                socket = JavelinSocket.client(config.getClientServerUri(), config.getWorkerCount());
            } else {
                socket = JavelinSocket.client(
                        config.getClientServerUri(),
                        config.getClientUsername(),
                        config.getClientPassword(),
                        config.getWorkerCount(),
                        config.isLocalBroadcastEnabled());
            }
        }

        if (config.getMode() != Mode.NONE) {
            Core.app.addListener(new JavelinApplicationListener(socket));
        }
    }

    @Override
    public void registerServerCommands(final CommandHandler handler) {
        handler.register("javelin-user-add", "<username> <password>", "Add a new user to the server.", args -> {
            if (authenticator.existsUser(args[0])) {
                Log.info("The user " + args[0] + " has been override.");
            } else {
                Log.info("The user " + args[0] + " has been added.");
            }
            authenticator.saveUser(args[0], args[1].toCharArray());
        });

        handler.register("javelin-user-remove", "<username>", "Removes a user from the server.", args -> {
            if (authenticator.existsUser(args[0])) {
                authenticator.deleteUser(args[0]);
                Log.info("The user " + args[0] + " has been removed.");
            } else {
                Log.info("The user " + args[0] + " does not exists.");
            }
        });

        handler.register("javelin-user-remove-all", "Removes all users from the server.", args -> {
            final var count = authenticator.countUsers();
            authenticator.deleteAllUsers();
            Log.info("@ users have been removed.", count);
        });

        handler.register("javelin-user-list", "List the users.", args -> {
            final var users = authenticator.findAllUsers();
            if (users.isEmpty()) {
                Log.info("No users...");
            } else {
                Log.info("Users: " + ColorCodes.blue + users.size());
                for (final var user : users) {
                    Log.info("\t- " + ColorCodes.blue + user);
                }
            }
        });

        handler.register("javelin-status", "Gets the status of the javelin socket.", args -> {
            Log.info(
                    "The javelin socket is currently @.",
                    socket.getStatus().name().toLowerCase(Locale.ROOT));
        });

        handler.register("javelin-restart", "Restarts the Javelin socket.", args -> {
            Log.info("The javelin socket will be restarted.");
            socket.restart().whenComplete((result, throwable) -> {
                if (throwable != null) {
                    Log.err("Failed to restart.", throwable);
                } else {
                    Log.info("Successfully restarted the socket.");
                }
            });
        });
    }

    private static final class JavelinApplicationListener implements ApplicationListener {

        private final JavelinSocket socket;
        private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        private JavelinApplicationListener(final JavelinSocket socket) {
            this.socket = socket;
        }

        @Override
        public void init() {
            Log.debug("Attempting initial start of the Javelin socket");
            try {
                socket.start().get(config.getInitialConnectionTimeout(), TimeUnit.SECONDS);
            } catch (final InterruptedException | ExecutionException | TimeoutException e) {
                Log.err("Failed initial start of the Javelin socket.", e);
            }
            if (config.isAutoRestartEnabled()) {
                executor.scheduleWithFixedDelay(this::checkStatus, 5L, 5L, TimeUnit.MINUTES);
            }
        }

        @Override
        public void dispose() {
            executor.shutdown();
            try {
                socket.close().get(15L, TimeUnit.SECONDS);
            } catch (final InterruptedException | ExecutionException | TimeoutException e) {
                Log.err("Failed to close the javelin socket", e);
            }
        }

        private void checkStatus() {
            if (socket.getStatus() == Status.CLOSED) {
                Log.debug("The Javelin socket is closed, restarting.");
                socket.restart().whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        Log.debug("Failed to restart Javelin: @", throwable);
                    }
                });
            }
        }
    }
}
