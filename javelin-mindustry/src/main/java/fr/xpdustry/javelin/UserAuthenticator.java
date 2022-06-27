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

import java.util.*;
import org.jetbrains.annotations.*;

public interface UserAuthenticator extends JavelinServer.Authenticator {

  void saveUser(final @NotNull String username, final char[] password);

  boolean existsUser(final @NotNull String username);

  long countUsers();

  @NotNull List<String> findAllUsers();

  void deleteUser(final @NotNull String username);

  void deleteAllUsers();
}