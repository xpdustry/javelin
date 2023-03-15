/*
 * Javelin, a simple communication protocol for broadcasting events on a network.
 *
 * Copyright (C) 2023 Xpdustry
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

import com.password4j.*;
import com.password4j.types.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;
import org.checkerframework.checker.nullness.qual.*;

/**
 * Simple user authenticator.
 */
final class SimpleUserAuthenticator implements UserAuthenticator {

    final Map<String, HashedPassword> users = new ConcurrentHashMap<>();
    private final BcryptFunction bcrypt = BcryptFunction.getInstance(Bcrypt.B, 12);
    private final Path path;

    SimpleUserAuthenticator(final Path path) {
        if (!path.getFileName().toString().endsWith("bin.gz")) {
            throw new IllegalArgumentException(
                    "Unsupported file extension: expected file.bin.gz, got " + path.getFileName());
        }
        this.path = path;

        if (Files.exists(path)) {
            try (final var input = new DataInputStream(new GZIPInputStream(Files.newInputStream(this.path)))) {
                final var entries = input.readInt();
                for (var i = 0; i < entries; i++) {
                    final var username = input.readUTF();
                    final var passLen = input.readUnsignedByte();
                    final var pass = input.readNBytes(passLen);
                    final var salt = input.readUTF();
                    users.put(username, new HashedPassword(pass, salt));
                }
            } catch (final IOException e) {
                throw new RuntimeException("Unable to load user authenticator users.", e);
            }
        }
    }

    @Override
    public boolean authenticate(final String username, final char[] password) {
        final var hashed = users.get(username);
        return hashed != null && hashed.equals(getHashedPassword(password, hashed.salt));
    }

    @Override
    public void saveUser(final String username, final char[] password) {
        users.put(username, getHashedPassword(password));
        save();
    }

    @Override
    public boolean existsUser(final String username) {
        return users.containsKey(username);
    }

    @Override
    public long countUsers() {
        return users.size();
    }

    @Override
    public List<String> findAllUsers() {
        return List.copyOf(users.keySet());
    }

    @Override
    public void deleteUser(final String username) {
        if (users.remove(username) != null) {
            save();
        }
    }

    @Override
    public void deleteAllUsers() {
        users.clear();
        save();
    }

    private synchronized void save() {
        try (final var output = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(this.path)))) {
            output.writeInt(users.size());
            for (final var entry : users.entrySet()) {
                output.writeUTF(entry.getKey());
                output.writeByte(entry.getValue().pass.length);
                output.write(entry.getValue().pass);
                output.writeUTF(entry.getValue().salt);
            }
        } catch (final IOException e) {
            throw new RuntimeException("Unable to save user authenticator users.", e);
        }
    }

    private SimpleUserAuthenticator.HashedPassword getHashedPassword(final char[] password, final String salt) {
        final var hash = bcrypt.hash(new SecureString(password), salt);
        return new HashedPassword(hash.getBytes(), salt);
    }

    private SimpleUserAuthenticator.HashedPassword getHashedPassword(final char[] password) {
        final var hash = bcrypt.hash(new SecureString(password));
        return new HashedPassword(hash.getBytes(), hash.getSalt());
    }

    private static final class HashedPassword {

        private final byte[] pass;
        private final String salt;

        private HashedPassword(final byte[] pass, final String salt) {
            this.pass = pass;
            this.salt = salt;
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(pass), salt);
        }

        @Override
        public boolean equals(final @Nullable Object obj) {
            return obj instanceof HashedPassword hashed && Arrays.equals(pass, hashed.pass) && salt.equals(hashed.salt);
        }
    }
}
