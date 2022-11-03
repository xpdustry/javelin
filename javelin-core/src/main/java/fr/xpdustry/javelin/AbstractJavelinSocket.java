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
import java.util.function.*;
import net.kyori.event.*;
import org.checkerframework.checker.nullness.qual.*;
import org.jetbrains.annotations.*;
import org.slf4j.*;

abstract class AbstractJavelinSocket implements JavelinSocket {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final EventBus<JavelinEvent> bus = new SimpleEventBus<>(JavelinEvent.class);
    private final Kryo kryo = new Kryo();

    {
        kryo.setRegistrationRequired(false);
        kryo.setAutoReset(true);
        kryo.setOptimizedGenerics(false);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    @Override
    public @NotNull <E extends JavelinEvent> CompletableFuture<Void> sendEvent(final @NotNull E event) {
        if (this.getStatus() != Status.OPEN) {
            return CompletableFuture.failedFuture(new IOException("The socket is not open."));
        } else {
            try (final var output = new ByteBufferOutput(ByteBuffer.allocate(Internal.MAX_EVENT_SIZE))) {
                kryo.writeClass(output, event.getClass());
                kryo.writeObject(output, event);
                onEventSend(output.getByteBuffer());
                return CompletableFuture.completedFuture(null);
            } catch (final KryoBufferOverflowException e) {
                return CompletableFuture.failedFuture(new IOException("The event is too large.", e));
            }
        }
    }

    @Override
    public @NotNull <E extends JavelinEvent> Subscription subscribe(
            final @NotNull Class<E> event, final @NotNull Consumer<E> subscriber) {
        final var forwarded = new ForwardingEventSubscriber<>(subscriber);
        bus.register(event, forwarded);
        return () -> bus.unregister(forwarded);
    }

    protected abstract void onEventSend(final @NotNull ByteBuffer buffer);

    protected void onEventReceive(final @NotNull ByteBuffer buffer) {
        try (final var input = new ByteBufferInput(buffer)) {
            final var registration = kryo.readClass(input);
            if (registration == null) {
                return;
            }
            @SuppressWarnings("unchecked")
            final var clazz = (Class<? extends JavelinEvent>) registration.getType();
            if (bus.hasSubscribers(clazz)) {
                bus.post(kryo.readObject(input, clazz)).exceptions().forEach((s, t) -> getLogger()
                        .error("An exception occurred while handling an event in " + s, t));
            }
        }
    }

    protected abstract Logger getLogger();

    private static final class ForwardingEventSubscriber<E> implements EventSubscriber<E> {

        private final Consumer<E> consumer;

        private ForwardingEventSubscriber(final Consumer<E> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void invoke(final @NonNull E event) {
            this.consumer.accept(event);
        }
    }
}
