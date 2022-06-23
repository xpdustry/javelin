package fr.xpdustry.javelin.exception;

import java.io.*;
import org.jetbrains.annotations.*;

public class MessageSendException extends IOException {

  @Serial
  private static final long serialVersionUID = -8288865433514621162L;

  public MessageSendException(final @NotNull String message) {
    super(message);
  }

  public MessageSendException(final @NotNull String message, final @NotNull Throwable cause) {
    super(message, cause);
  }

  public MessageSendException(final @NotNull Throwable cause) {
    super(cause);
  }
}
