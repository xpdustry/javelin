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

import java.util.*;
import java.util.regex.*;
import org.java_websocket.drafts.*;
import org.java_websocket.extensions.permessage_deflate.*;
import org.java_websocket.protocols.*;

final class Internal {

  public static final int MAX_EVENT_SIZE = 8192;

  public static final String AUTHORIZATION_HEADER = "Authorization";
  public static final Pattern AUTHORIZATION_PATTERN = Pattern.compile("^Basic (.+)$");

  public static Draft getJavelinDraft() {
    return new Draft_6455(
      Collections.singletonList(new PerMessageDeflateExtension()),
      List.of(new Protocol(""), new Protocol("ocpp2.0"))
    );
  }
}
