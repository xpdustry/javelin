package fr.xpdustry.javelin;

import fr.xpdustry.javelin.exception.*;
import java.io.*;
import org.jetbrains.annotations.*;

public interface JavelinClient extends Closeable {

  void connect() throws IOException;

  void reconnect() throws IOException;

  <T> void broadcastMessage(final @NotNull MessageContext<T> context, final @NotNull T message) throws MessageSendException;

  <T> void sendMessage(final @NotNull MessageContext<T> context, final @NotNull T message, final @NotNull String receiver) throws MessageSendException;

  <T> void bindReceiver(final @NotNull MessageContext<T> context, final @NotNull MessageReceiver<T> receiver);

  <T> void unbindReceiver(final @NotNull MessageContext<T> context, final @NotNull MessageReceiver<T> receiver);

  boolean isConnected();

  boolean isClosed();

  @NotNull String getUsername();

  @NotNull String getPassword();
}
