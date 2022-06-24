package fr.xpdustry.javelin;

import java.io.*;
import org.jetbrains.annotations.*;

public interface JavelinServer extends Closeable {

  static @NotNull JavelinServer websocket(final int port, final int workers, final @NotNull Authenticator authenticator) {
    return new WebSocketJavelinServer(port, workers, authenticator);
  }

  void start() throws IOException;

  int getPort();

  @NotNull Authenticator getAuthenticator();

  boolean isOpen();

  boolean isClosed();
}
