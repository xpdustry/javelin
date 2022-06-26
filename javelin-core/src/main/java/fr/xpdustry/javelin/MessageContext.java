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

public final class MessageContext<T> {

  private final String namespace;
  private final String endpoint;
  private final Class<T> messageType;

  public MessageContext(final @NotNull String namespace, final @NotNull String endpoint, final @NotNull Class<T> messageType) {
    if (namespace.contains(":")) {
      throw new IllegalArgumentException("namespace contains a colon (" + namespace + ").");
    }
    this.namespace = namespace;
    this.endpoint = endpoint;
    this.messageType = messageType;
  }

  public @NotNull String getNamespace() {
    return namespace;
  }

  public @NotNull String getEndpoint() {
    return endpoint;
  }

  public @NotNull Class<T> getMessageType() {
    return messageType;
  }

  @Override
  public boolean equals(final @Nullable Object o) {
    if (this == o) {
      return true;
    } else {
      return o instanceof MessageContext<?> that
        && namespace.equals(that.namespace)
        && endpoint.equals(that.endpoint)
        && messageType.equals(that.messageType);
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(namespace, endpoint, messageType);
  }

  @Override
  public String toString() {
    return namespace + ":" + endpoint;
  }
}
