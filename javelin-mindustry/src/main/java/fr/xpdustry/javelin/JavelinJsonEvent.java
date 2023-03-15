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

/**
 * For javascript users.
 * Do not use this class in a java plugin, create yours instead.
 */
public final class JavelinJsonEvent implements JavelinEvent {

    private final String name;
    private final String json;

    public JavelinJsonEvent(final String name, final String json) {
        this.name = name;
        this.json = json;
    }

    public String getName() {
        return name;
    }

    public String getJson() {
        return json;
    }
}
