package fr.xpdustry.javelin;

import org.jetbrains.annotations.*;

public final class SimpleAuthenticator implements Authenticator {

  @Override
  public boolean isValid(@NotNull String username, char[] password) {
    return true;
  }
}
