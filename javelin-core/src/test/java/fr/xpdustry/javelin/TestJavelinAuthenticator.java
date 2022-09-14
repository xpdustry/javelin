package fr.xpdustry.javelin;

import java.util.*;
import org.jetbrains.annotations.*;

public final class TestJavelinAuthenticator implements JavelinAuthenticator {

  private final Map<String, char[]> users = new HashMap<>();

  @Override
  public boolean authenticate(final @NotNull String username, char @NotNull [] password) {
    return users.containsKey(username) && Arrays.equals(users.get(username), password);
  }

  public void addUser(final String username, final String password) {
    users.put(username, password.toCharArray());
  }
}
