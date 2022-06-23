package fr.xpdustry.javelin;

import org.jetbrains.annotations.*;

@FunctionalInterface
public interface JavelinAuthenticator {

  boolean isValid(final @NotNull String username, final @NotNull String password);
}
