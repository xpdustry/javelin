package fr.xpdustry.javelin;

import java.io.*;
import org.jetbrains.annotations.*;

public interface JavelinServer extends Closeable {

  void start() throws IOException;

  @NotNull JavelinAuthenticator getAuthenticator();

  int getPort();
}
