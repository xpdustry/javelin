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

import java.nio.file.*;
import java.util.*;

public interface UserAuthenticator extends JavelinAuthenticator {

    static UserAuthenticator create(final Path path) {
        return new SimpleUserAuthenticator(path);
    }

    void saveUser(final String username, final char[] password);

    boolean existsUser(final String username);

    long countUsers();

    List<String> findAllUsers();

    void deleteUser(final String username);

    void deleteAllUsers();
}
