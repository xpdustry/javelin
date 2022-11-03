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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

public final class SimpleUserAuthenticatorTest {

    private static final String USER1 = "user1";
    private static final String USER2 = "user2";

    private @TempDir Path tempDir;
    private SimpleUserAuthenticator authenticator;

    @BeforeEach
    void setup() {
        this.authenticator = new SimpleUserAuthenticator(tempDir.resolve("users.bin.gz"));
    }

    @Test
    void test_save() {
        assertThat(this.authenticator.countUsers()).isEqualTo(0L);
        this.authenticator.saveUser(USER1, USER1.toCharArray());
        this.authenticator.saveUser(USER2, USER2.toCharArray());
        assertThat(this.authenticator.countUsers()).isEqualTo(2L);
        assertThat(this.authenticator.findAllUsers()).containsOnly(USER1, USER2);
    }

    @Test
    void test_load() {
        this.authenticator.saveUser(USER1, USER1.toCharArray());
        this.authenticator.saveUser(USER2, USER2.toCharArray());
        final var newAuthenticator = new SimpleUserAuthenticator(tempDir.resolve("users.bin.gz"));
        assertThat(newAuthenticator.users).isEqualTo(this.authenticator.users);
    }

    @Test
    void test_authenticate() {
        this.authenticator.saveUser(USER1, USER1.toCharArray());
        assertThat(this.authenticator.authenticate(USER1, USER1.toCharArray())).isTrue();
        assertThat(this.authenticator.authenticate(USER1, USER2.toCharArray())).isFalse();
    }
}
