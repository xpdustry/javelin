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

import java.util.*;
import org.jetbrains.annotations.*;

final class TestEvent implements JavelinEvent {

  private final String payload;

  public TestEvent(final String payload) {
    this.payload = payload;
  }

  public String getPayload() {
    return payload;
  }

  @Override
  public boolean equals(final @Nullable Object o) {
    return this == o || (o instanceof TestEvent event && Objects.equals(payload, event.payload));
  }

  @Override
  public int hashCode() {
    return payload.hashCode();
  }
}
