# Javelin

[![Build status](https://github.com/Xpdustry/Javelin/actions/workflows/build.yml/badge.svg?branch=master&event=push)](https://github.com/Xpdustry/Javelin/actions/workflows/build.yml)
[![Mindustry 6.0 | 7.0 ](https://img.shields.io/badge/Mindustry-6.0%20%7C%207.0-ffd37f)](https://github.com/Anuken/Mindustry/releases)
[![Xpdustry latest](https://repo.xpdustry.fr/api/badge/latest/releases/fr/xpdustry/javelin?color=00FFFF&name=Javelin&prefix=v)](https://github.com/Xpdustry/Javelin/releases)

## Description

A simple and fast communication system for Mindustry servers, enabling powerful features such as
global chats, synced moderation, discord integrations, etc...

## Usage

### First set up

1. Install the plugin on all the servers you wish to link and start them, it will create the
   necessary config files in the `./javelin` directory.

2. For the Javelin server, you can create one in one of your mindustry servers by setting 
   `fr.xpdustry.javelin.server.enabled` to true in the `./javelin/config.properties` config file.
   Also set `fr.xpdustry.javelin.server.port` for the port.

3. Once the server is set up, register your users with the `javelin-server-user-add` command.
   They are stored in a binary file (`./javelin/users.bin`) by default. Passwords are encrypted
   so no worries about security in most cases.

4. For each Mindustry server that need to connect to Javelin, in the config file, set :
   
   - `fr.xpdustry.javelin.client.enabled` to true.

   - `fr.xpdustry.javelin.client.username` with its registered username.

   - `fr.xpdustry.javelin.client.password` with its registered password.

   - `fr.xpdustry.javelin.client.address` with the server address.

5. Now, your Javelin network should be ready, enjoy.

   > If you need any help, We'll be happy to help you in the **#support** channel of
   the [Xpdustry Discord server](https://discord.xpdustry.fr).
   
### Javelin API

It's very simple.

First, add this in your `build.gradle` :

```gradle
repositories {
    // Replace with "https://repo.xpdustry.fr/snapshots" if you want to use the snapshots
    maven { url = uri("https://repo.xpdustry.fr/releases") }
}

dependencies {
    // Add "-SNAPSHOT" after the version if you are using the snapshot repository
    compileOnly("fr.xpdustry:javelin-mindustry:1.0.0" )
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

Finally, in your code, get the client with `Javelin.getClient()`, check if it's connected
with `isConnected()` and enjoy.

You can send or broadcast any message you like, but .

Here is an example class that can synchronize ban events :

```java
public final class BanSynchronizer implements MessageReceiver<String> {

    private static final MessageContext<String> CONTEXT = new MessageContext<>("xpdustry-moderation", "ban-sync", String.class);

    public BanSynchronizer() {
        // Binds this object for receiving incoming messages from other servers.
        JavelinPlugin.getClient().bindReceiver(CONTEXT, this);

        Events.on(EventType.PlayerBanEvent.class, e -> {
            final JavelinClient client = JavelinPlugin.getClient();
            try {
                if (client.isConnected()) {
                    client.broadcastMessage(CONTEXT, e.uuid);
                }
            } catch (final IOException ex) {
                Log.err("An unexpected exception occurred while broadcasting a ban.", ex);
            }
        });
    }

    @Override
    public void onMessageReceive(final @NotNull String uuid, final @Nullable String sender) {
        Vars.netServer.admins.banPlayer(uuid);
        final Player player = Groups.player.find(p -> p.uuid().equals(uuid));
        if (player != null) player.kick(Packets.KickReason.banned);
    }
}
```

### Tips

- In the client config, if you set `fr.xpdustry.javelin.client.timeout` to 0, your client will
  always reconnect to your Javelin server, even if it has been down for a long time.

- You can explicitly reconnect a Javelin client with the `javelin-client-reconnect` command.

- You can add `wss` support on javelin with a reverse proxy like nginx.
  Example let's encrypt / certbot :

  ```nginx
  # This is required to upgrade the websocket connection
  map $http_upgrade $connection_upgrade {
      default upgrade;
      '' close;
  }

  upstream javelin {
      # I use pterodactyl pannel so I use the node url "n1.xpdustry.fr", if you don't, use 127.0.0.1 or localhost
      # 12000 is the port of my javelin server
      server n1.xpdustry.fr:12000;
      keepalive 64;
  }

  server {
      # Don't forget to change that
      server_name javelin.xpdustry.fr;

      listen 443 ssl;
      listen [::]:443 ssl;

      # Don't forget to change that
      access_log /var/log/nginx/javelin.xpdustry.fr-access.log;
      error_log /var/log/nginx/javelin.xpdustry.fr-error.log;

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
      ssl_certificate /etc/letsencrypt/live/xpdustry.fr/fullchain.pem; # managed by Certbot
      ssl_certificate_key /etc/letsencrypt/live/xpdustry.fr/privkey.pem; # managed by Certbot
      include /etc/letsencrypt/options-ssl-nginx.conf;
      ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;
  }
  ```

  Now, your javelin server is accessible with `wss://javelin.yourdomain.something/`.

## Building

- `./gradlew jar` for a simple jar that contains only the plugin code.

- `./gradlew shadowJar` for a fatJar that contains the plugin and its dependencies (use this for
  your server).

## Testing

- `./gradlew runMindustryClient`: Run Mindustry in desktop with the plugin.

- `./gradlew runMindustryServer`: Run Mindustry in a server with the plugin.

## Running

This plugin is compatible with V6 and V7, but it requires the following dependencies :

- [xpdustry-distributor-core](https://github.com/Xpdustry/Distributor)

- [xpdustry-kotlin-stdlib](https://github.com/Xpdustry/KotlinRuntimePlugin)

If you run on v135 or lower, you will need [mod-loader](https://github.com/Xpdustry/ModLoaderPlugin)
for the dependency resolution.
