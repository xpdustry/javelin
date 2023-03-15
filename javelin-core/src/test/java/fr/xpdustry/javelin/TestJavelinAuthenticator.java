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

import java.util.*;

public final class TestJavelinAuthenticator implements JavelinAuthenticator {

    private final Map<String, char[]> users = new HashMap<>();

    @Override
    public boolean authenticate(final String username, char[] password) {
        return users.containsKey(username) && Arrays.equals(users.get(username), password);
    }

    public void addUser(final String username, final String password) {
        users.put(username, password.toCharArray());
    }
}
