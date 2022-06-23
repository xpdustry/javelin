package fr.xpdustry.javelin;

import org.jetbrains.annotations.*;

@FunctionalInterface
public interface MessageReceiver<T> {

  void handleMessage(final @NotNull T message, final @Nullable String sender);
}
