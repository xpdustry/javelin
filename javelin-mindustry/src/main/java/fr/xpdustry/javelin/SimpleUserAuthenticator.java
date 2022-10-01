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

import java.io.*;
import java.security.*;
import java.security.spec.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.jetbrains.annotations.*;

/**
 * Simple user authenticator with <a href="https://www.javacodegeeks.com/2012/05/secure-password-storage-donts-dos-and.html">basic password storage</a>.
 */
final class SimpleUserAuthenticator implements UserAuthenticator {

  private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
  private static final int DERIVED_KEY_LENGTH = 256;
  private static final int ITERATIONS = 100000;

  private final Map<String, EncryptedPassword> users = new ConcurrentHashMap<>();
  private final File file;

  SimpleUserAuthenticator(final @NotNull File file) {
    this.file = file;

    if (this.file.exists()) {
      try (final var input = new DataInputStream(new GZIPInputStream(new FileInputStream(this.file)))) {
        final var entries = input.readInt();
        for (var i = 0; i < entries; i++) {
          final var username = input.readUTF();
          final var passLen = input.readUnsignedByte();
          final var pass = input.readNBytes(passLen);
          final var saltLen = input.readUnsignedByte();
          final var salt = input.readNBytes(saltLen);
          users.put(username, new EncryptedPassword(pass, salt));
        }
      } catch (final IOException e) {
        throw new RuntimeException("Unable to load user authenticator users.", e);
      }
    }
  }

  @Override
  public boolean authenticate(final @NotNull String username, final char @NotNull [] password) {
    final var encrypted = users.get(username);
    return encrypted != null && encrypted.equals(getEncryptedPassword(password, encrypted.salt));
  }

  @Override
  public void saveUser(final @NotNull String username, final char @NotNull [] password) {
    final var salt = generateSalt();
    users.put(username, getEncryptedPassword(password, salt));
    save();
  }

  @Override
  public boolean existsUser(final @NotNull String username) {
    return users.containsKey(username);
  }

  @Override
  public long countUsers() {
    return users.size();
  }

  @Override
  public @NotNull @Unmodifiable List<String> findAllUsers() {
    return List.copyOf(users.keySet());
  }

  @Override
  public void deleteUser(final @NotNull String username) {
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
    try (final var output = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(file)))) {
      output.writeInt(users.size());
      for (final var entry : users.entrySet()) {
        output.writeUTF(entry.getKey());
        output.writeByte(entry.getValue().pass.length);
        output.write(entry.getValue().pass);
        output.writeByte(entry.getValue().salt.length);
        output.write(entry.getValue().salt);
      }
    } catch (final IOException e) {
      throw new RuntimeException("Unable to save user authenticator users.", e);
    }
  }

  private @NotNull EncryptedPassword getEncryptedPassword(final char[] password, final byte[] salt) {
    try {
      final var spec = new PBEKeySpec(password, salt, ITERATIONS, DERIVED_KEY_LENGTH);
      final var factory = SecretKeyFactory.getInstance(ALGORITHM);
      return new EncryptedPassword(factory.generateSecret(spec).getEncoded(), salt);
    } catch (final NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException("Failed to generate an encrypted password.", e);
    }
  }

  private byte[] generateSalt() {
    try {
      final var random = SecureRandom.getInstance("SHA1PRNG");
      final var salt = new byte[8]; // NIST recommends minimum 4 bytes. We use 8.
      random.nextBytes(salt);
      return salt;
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException("Failed to generate a salt.", e);
    }
  }

  private static final class EncryptedPassword {

    private final byte[] pass;
    private final byte[] salt;

    private EncryptedPassword(final byte[] pass, final byte[] salt) {
      this.pass = pass;
      this.salt = salt;
    }

    @Override
    public int hashCode() {
      return Objects.hash(Arrays.hashCode(pass), Arrays.hashCode(salt));
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
      return obj instanceof EncryptedPassword encrypted
        && Arrays.equals(salt, encrypted.salt)
        && Arrays.equals(pass, encrypted.pass);
    }
  }
}
