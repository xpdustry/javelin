package fr.xpdustry.javelin;

import java.net.*;
import org.jetbrains.annotations.*;

public interface JavelinConfig {

  boolean isServerEnabled();

  int getServerPort();

  int getServerWorkerCount();

  boolean isClientEnabled();

  @NotNull String getClientUsername();

  char[] getClientPassword();

  @NotNull URI getClientServerUri();
}
