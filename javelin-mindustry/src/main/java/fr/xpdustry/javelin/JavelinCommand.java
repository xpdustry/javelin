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

import arc.util.*;
import org.jetbrains.annotations.*;

final class JavelinCommand {

  private final UserAuthenticator authenticator;

  public JavelinCommand(final @NotNull UserAuthenticator authenticator) {
    this.authenticator = authenticator;
  }

  public void registerServerCommands(final @NotNull CommandHandler handler) {
    handler.register("javelin-client-reconnect", "Tries to reconnect the Javelin client.", args -> {
      if (JavelinPlugin.getConf().isClientEnabled()) {
        Log.info("The client is not enabled.");
      } else {
        Log.info("The client will be reconnected.");
        JavelinPlugin.getClient().reconnect();
      }
    });

    handler.register("javelin-server-user-add", "<username> <password>", "Add a new user to the server.", args -> {
      if (authenticator.existsUser(args[0])) {
        Log.info("The user " + args[0] + " has been override.");
      } else {
        Log.info("The user " + args[0] + " has been added.");
      }
      authenticator.saveUser(args[0], args[1].toCharArray());
    });

    handler.register("javelin-server-user-remove", "<username>", "Removes a user from the server.", args -> {
      if (authenticator.existsUser(args[0])) {
        authenticator.deleteUser(args[0]);
        Log.info("The user " + args[0] + " has been removed.");
      } else {
        Log.info("The user " + args[0] + " does not exists.");
      }
    });

    handler.register("javelin-server-user-list", "List the users.", args -> {
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
  }
}
