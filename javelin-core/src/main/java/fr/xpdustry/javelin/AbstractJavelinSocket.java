/*
 * Javelin, a simple communication protocol for broadcasting events on a network.
 *
 * Copyright (C) 2022 Xpdustry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.xpdustry.javelin;

import com.esotericsoftware.kryo.kryo5.*;
import com.esotericsoftware.kryo.kryo5.io.*;
import com.esotericsoftware.kryo.kryo5.objenesis.strategy.*;
import java.io.*;
import java.nio.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import net.kyori.event.*;
import org.jetbrains.annotations.*;
import org.slf4j.*;

abstract class AbstractJavelinSocket implements JavelinSocket {

  protected final Logger logger = LoggerFactory.getLogger(getClass());
  private final EventBus<JavelinEvent> bus = EventBus.create(JavelinEvent.class);
  private final Kryo kryo = new Kryo();
  private final AtomicInteger idGen = new AtomicInteger();
  private final ExecutorService executor;

  protected AbstractJavelinSocket(final int workers) {
    this.executor = Executors.newFixedThreadPool(workers, runnable -> {
      final var worker = new Thread(runnable);
      worker.setDaemon(true);
      worker.setName("JavelinSocketWorker-" + idGen.getAndIncrement());
      return worker;
    });

    this.kryo.setRegistrationRequired(false);
    this.kryo.setAutoReset(true);
    this.kryo.setOptimizedGenerics(false);
    this.kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
  }

  @Override
  public <E extends JavelinEvent> void sendEvent(final @NotNull E event) throws IOException {
    if (this.getStatus() != Status.OPEN) {
      throw new IOException("The socket is not open.");
    }
    try (final var output = new ByteBufferOutput(Internal.MAX_EVENT_SIZE)) {
      kryo.writeClass(output, event.getClass());
      kryo.writeObject(output, event);
      onEventSend(output.getByteBuffer());
    } catch (final KryoBufferOverflowException e) {
      throw new IOException("The event is too large.", e);
    }
  }

  @Override
  public @NotNull <E extends JavelinEvent> Subscription subscribe(final @NotNull Class<E> event, final @NotNull Consumer<E> subscriber) {
    return bus.subscribe(event, subscriber::accept)::unsubscribe;
  }

  protected abstract void onEventSend(final @NotNull ByteBuffer buffer);

  protected void onEventReceive(final @NotNull ByteBuffer buffer) {
    final CompletableFuture<Void> future = new CompletableFuture<>();
    future.orTimeout(10, TimeUnit.MILLISECONDS);
    try (final var input = new ByteBufferInput(buffer)) {
      final var registration = kryo.readClass(input);
      if (registration == null) {
        return;
      }
      @SuppressWarnings("unchecked") final var clazz = (Class<? extends JavelinEvent>) registration.getType();
      if (bus.subscribed(clazz)) {
        executor.execute(() -> {
          final var event = kryo.readObject(input, clazz);
          bus.post(event).exceptions().forEach((subscriber, throwable) -> logger.error("An exception occurred while handling " + event, throwable));
        });
      }
    }
  }
}
