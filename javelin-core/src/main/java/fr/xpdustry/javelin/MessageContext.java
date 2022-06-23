package fr.xpdustry.javelin;

import java.util.*;
import org.jetbrains.annotations.*;

public final class MessageContext<T> {

  private final String namespace;
  private final String subject;
  private final Class<T> messageType;

  public MessageContext(final @NotNull String namespace, final @NotNull String subject, final @NotNull Class<T> messageType) {
    if (namespace.contains(":")) {
      throw new IllegalArgumentException("namespace contains a colon (" + namespace + ").");
    }
    this.namespace = namespace;
    this.subject = subject;
    this.messageType = messageType;
  }

  public @NotNull String getNamespace() {
    return namespace;
  }

  public @NotNull String getSubject() {
    return subject;
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
        && subject.equals(that.subject)
        && messageType.equals(that.messageType);
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(namespace, subject, messageType);
  }

  @Override
  public String toString() {
    return namespace + ":" + subject;
  }
}
