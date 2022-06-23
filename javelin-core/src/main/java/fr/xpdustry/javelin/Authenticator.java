package fr.xpdustry.javelin;

import org.jetbrains.annotations.*;

@FunctionalInterface
public interface Authenticator {

  boolean isValid(final @NotNull String username, final char[] password);
}
