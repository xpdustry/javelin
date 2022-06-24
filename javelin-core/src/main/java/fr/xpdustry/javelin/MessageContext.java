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
