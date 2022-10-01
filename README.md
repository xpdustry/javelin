# Javelin

[![Build status](https://github.com/Xpdustry/Javelin/actions/workflows/build.yml/badge.svg?branch=master&event=push)](https://github.com/Xpdustry/Javelin/actions/workflows/build.yml)
[![Mindustry 6.0 | 7.0 ](https://img.shields.io/badge/Mindustry-6.0%20%7C%207.0-ffd37f)](https://github.com/Anuken/Mindustry/releases)
[![Xpdustry latest](https://maven.xpdustry.fr/api/badge/latest/releases/fr/xpdustry/javelin?color=00FFFF&name=Javelin&prefix=v)](https://github.com/Xpdustry/Javelin/releases)

## Description

A simple and fast communication protocol for your internal network or Mindustry servers,
enabling powerful features such as global chats, synced moderation, discord integrations, etc...

## Runtime

This plugin is compatible with V6 and V7.

> If you run on v135 or lower, you will
> need [mod-loader](https://github.com/Xpdustry/ModLoaderPlugin)
> for the dependency resolution.

## Setup

This tutorial is aimed for non-advanced users looking to create their server network very easily :

1. Install the plugin in all the servers you wish to link and start them, it will create the
   necessary config files in the `./javelin` directory.

2. Choose a Mindustry server that will host the main Javelin server.

   > I suggest you to choose your hub server or the one with the most RAM and CPU power.

   Go in the config file of the said server in `./javelin/config.properties` and edit
   the following properties :

    - `fr.xpdustry.javelin.socket.mode` to `SERVER`.

    - `fr.xpdustry.javelin.server.port` : The port of your Javelin server (optional, default
      is `8080`).

    - `fr.xpdustry.javelin.socket.workers` : The number of threads handling the incoming and
      outgoing events (optional, **don't exceed your CPU core count**).

   Then in the server console, add users with the command `javelin-user-add <username> <password>`.

   > Users are saved in a binary file at `./javelin/users.bin.gz`, passwords are salted and hashed.

3. Once it's ready, restart your Mindustry server and your Javelin server should start along it.

4. Now, for each other server where Javelin is installed, edit the following properties in
   the config file at `./javelin/config.properties` :

    - `fr.xpdustry.javelin.socket.mode` to `CLIENT`.

    - `fr.xpdustry.javelin.client.address` to the main javelin server address such
      as `ws://xpdustry.fr:8080`.

    - `fr.xpdustry.javelin.client.username` to the username you assigned to this server.

    - `fr.xpdustry.javelin.client.password` to the password you assigned to this server.

    - `fr.xpdustry.javelin.socket.workers` : The number of threads handling the incoming and
      outgoing events (optional, **don't exceed your CPU core count**).

5. Restart all servers and enjoy the wonders of networking.

   > Having problems ? Don't mind asking help to the maintainers in the **#support** channel of
   the [Xpdustry Discord server](https://discord.xpdustry.fr).

## Usage

### Java

First, add this in your `build.gradle` :

```groovy
repositories {
    maven { url = uri("https://maven.xpdustry.fr/releases") }
}

dependencies {
    compileOnly("fr.xpdustry:javelin-mindustry:1.0.0")
}
```

Then, update your `plugin.json` file with :

```json
{
  "dependencies": [
    "xpdustry-javelin"
  ]
}
```

In your code, get the socket instance with `Javelin.getJavelinSocket()` (**do not call it
before `init`**).

Now, you can subscribe to the incoming events with `subscribe(event-class, subscriber)` and send
events with `sendEvent(event)`.

Here is an example Plugin that can synchronize ban events :

```java
public final class BanSynchronizer extends Plugin {

  @Override
  public void init() {
    // Get socket instance
    final JavelinSocket socket = JavelinPlugin.getJavelinSocket();

    Events.on(EventType.PlayerIpBanEvent.class, e -> {
      // If the socket is open, send the ban
      if (socket.getStatus() == JavelinSocket.Status.OPEN) {
        socket.sendEvent(new JavelinBanEvent(e.ip));
      }
    });

    socket.subscribe(JavelinBanEvent.class, e -> {
      // Ban player
      Vars.netServer.admins.banPlayerIP(e.getIP());
      // Kick player if connected
      final Player player = Groups.player.find(p -> p.ip().equals(e.getIP()));
      if (player != null) {
        player.kick(Packets.KickReason.banned);
      }
    });
  }

  public static final class JavelinBanEvent implements JavelinEvent {

    private final String ip;

    public JavelinBanEvent(final String ip) {
      this.ip = ip;
    }

    public String getIP() {
      return ip;
    }
  }
}
```

### JavaScript

You won't be able to define new `JavelinEvent` types like `JavelinBanEvent`,
but you can do everything else. Just don't forget to add the following in your `mod.json`.

```json
{
  "dependencies": [
    "xpdustry-javelin"
  ]
}
```

## Tips

- The socket isn't reusable, **do not close it yourself** !!!

- You can add `wss` support on javelin with a reverse proxy like nginx.
  Example with let's encrypt / certbot :

  ```nginx
  # This is required to upgrade the websocket connection
  map $http_upgrade $connection_upgrade {
      default upgrade;
      '' close;
  }

  upstream javelin {
      # The adress and port of your javelin server
      server adress:port;
      keepalive 64;
  }

  server {
      # The name of your server (example: javelin.xpdustry.fr)
      server_name javelin.domain.fr;

      listen 443 ssl;
      listen [::]:443 ssl;

      # Don't forget to change that
      access_log /var/log/nginx/javelin.domain.fr-access.log;
      error_log /var/log/nginx/javelin.domain.fr-error.log;

      location / {
          proxy_pass         http://javelin;
          proxy_set_header   Host              $host;
          proxy_set_header   X-Real-IP         $remote_addr;
          proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
          proxy_set_header   X-Forwarded-Proto $scheme;
          proxy_set_header   Upgrade           $http_upgrade;
          proxy_set_header   Connection        $connection_upgrade;
          proxy_http_version 1.1;
      }

      # This part is generated by certbot, so replace it with your own
      # Managed by Certbot
      ssl_certificate /etc/letsencrypt/live/javelin.domain.fr/fullchain.pem; # managed by Certbot
      ssl_certificate_key /etc/letsencrypt/live/javelin.domain.fr/privkey.pem; # managed by Certbot
      include /etc/letsencrypt/options-ssl-nginx.conf;
      ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;
  }
  ```

  Now, your javelin server is accessible with `wss://javelin.domain.fr/`.

## Building

- `./gradlew jar` for a simple jar that contains only the plugin code.

- `./gradlew shadowJar` for a fatJar that contains the plugin and its dependencies (use this for
  your server).

## Testing

- `./gradlew runMindustryClient`: Run the plugin in Mindustry desktop.

- `./gradlew runMindustryServer`: Run the plugin in Mindustry server.
