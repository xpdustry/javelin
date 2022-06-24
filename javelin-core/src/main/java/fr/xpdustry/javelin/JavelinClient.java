package fr.xpdustry.javelin;

import java.io.*;
import java.net.*;
import org.jetbrains.annotations.*;

public interface JavelinClient extends Closeable {

  static @NotNull JavelinClient websocket(final @NotNull String username, final char[] password, final @NotNull URI server) {
    return new WebSocketJavelinClient(username, password, server);
  }

  void connect() throws IOException;

  void reconnect() throws IOException;

  <T> void sendMessage(final @NotNull MessageContext<T> context, final @NotNull T message, final @NotNull String receiver) throws IOException;

  <T> void broadcastMessage(final @NotNull MessageContext<T> context, final @NotNull T message) throws IOException;

  <T> void bindReceiver(final @NotNull MessageContext<? extends T> context, final @NotNull MessageReceiver<T> receiver);

  <T> void unbindReceiver(final @NotNull MessageContext<? extends T> context, final @NotNull MessageReceiver<T> receiver);

  @NotNull URI getServerUri();

  @NotNull String getUsername();

  char[] getPassword();

  boolean isConnected();

  boolean isClosed();
}
