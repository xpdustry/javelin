# Javelin

[![Build status](https://github.com/Xpdustry/Javelin/actions/workflows/build.yml/badge.svg?branch=master&event=push)](https://github.com/Xpdustry/Javelin/actions/workflows/build.yml)
[![Mindustry 6.0 | 7.0 ](https://img.shields.io/badge/Mindustry-6.0%20%7C%207.0-ffd37f)](https://github.com/Anuken/Mindustry/releases)
[![Xpdustry latest](https://maven.xpdustry.com/api/badge/latest/releases/fr/xpdustry/javelin-core?color=00FFFF&name=Javelin&prefix=v)](https://github.com/Xpdustry/Javelin/releases)

## Description

A simple and fast communication protocol for your internal network or Mindustry servers,
enabling powerful features such as global chats, synced moderation, discord integrations, etc...

## Runtime

This plugin is compatible with V6 and V7.

> If you run on v135 or lower, you will
> need [mod-loader](https://github.com/Xpdustry/ModLoaderPlugin)
> for the dependency resolution.

## Setup

This tutorial is aimed for non-advanced users looking to create a simple mindustry network :

1. Install the plugin in all servers you wish to link and start them, it will create the
   necessary config files in the `./javelin` directory.

2. Choose a Mindustry server that will host the Javelin server.

   > I suggest you to choose your hub server or the one that is the most stable.

   Go in the config file of that Mindustry server in `./javelin/config.properties` and edit
   the following properties :

    - `fr.xpdustry.javelin.socket.mode` to `SERVER`.

    - `fr.xpdustry.javelin.server.port` : The port of your Javelin server (optional, default
      is `8080`).

    - `fr.xpdustry.javelin.socket.workers` : The number of threads handling the incoming and
      outgoing messages (optional).

    - `fr.xpdustry.javelin.server.always-allow-local-connections` : Allows clients to connect
      without a password if they are on the same machine as the server (optional, default
      is `false`).

      > If this option is not enabled, or it's enabled but the client is not on the same machine,
      add them with the command `javelin-user-add <username> <password>`.

3. Once your main Mindustry server is ready, restart it and your Javelin server should start too.

4. Now, for each "client" server, edit the following properties in the config file
   at `./javelin/config.properties` :

    - `fr.xpdustry.javelin.socket.mode` to `CLIENT`.

    - `fr.xpdustry.javelin.client.address` to the main javelin server address such
      as `ws://example.org:port` (or `ws://localhost:port` if the client is running in the same
      machine as the server and that `always-allow-local-connections` is enabled).

    - `fr.xpdustry.javelin.socket.workers` : The number of threads handling the incoming and
      outgoing events (optional).

   If a password is required for the server :

    - `fr.xpdustry.javelin.client.username` to the username you assigned for this server.

    - `fr.xpdustry.javelin.client.password` to the password you assigned for this server.

6. Restart all "client" servers and enjoy the wonders of simple networking.

> Having problems ? Don't mind asking help to the maintainers in the **#support** channel of
> the [Xpdustry Discord server](https://discord.xpdustry.com).

## Usage

### Java

First, add this in your `build.gradle` :

```groovy
repositories {
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
    // If you want to use the snapshots, replace the uri with "https://maven.xpdustry.com/snapshots"
    maven { url = uri("https://maven.xpdustry.com/releases") }
}

dependencies {
    // Don't forget to suffix the version with "-SNAPSHOT" if using the snapshots
    compileOnly("fr.xpdustry:javelin-mindustry:1.2.0")
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

In your code, get the socket instance with `JavelinPlugin.getJavelinSocket()` (**do not call it
before `init`**).

> If you use `ExtendedPlugin` of Distributor, do not call before `onLoad()`.

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

More info in
the [Javadoc](https://maven.xpdustry.com/javadoc/releases/fr/xpdustry/javelin-core/latest).

### JavaScript

For the giga chad programmers making plugins in JavaScript,
Javelin is guaranteed to work in V7 (v127+).
It can be used like in the java example by grabbing the instance on `ServerLoadEvent`
with `Vars.mods.getMod("xpdustry-javelin").main`.

If you want to use java defined javelin events, here is an example :

```js
// If you use "ModLoader" for your plugin depencency resolution, replace the line below with
// Vars.mods.getMod("xpdustry-mod-loader-plugin").main.getSharedClassLoader()
const loader = Vars.mods.mainLoader()
// Here we obtain the event class
const SomeEvent = java.lang.Class.forName("org.example.plugin.SomeEvent", true, loader)
// Now we can subscribe to the event
const javelin = Vars.mods.getMod("xpdustry-javelin").main
javelin.getJavelinSocket().subscribe(SomeEvent, event => {
  // Example usage
  Log.info(event.getSomeData())
})
// If you want to send the event, you will need to do it with a relfective operation
// because "new" doesn't work with classes that are obtained with "Class.forName" and not "Packages"
let event = SomeEvent.getConstructor(java.lang.String).newInstance("some-name")
javelin.getJavelinSocket().sendEvent(event)
```

Now, if you want to define your own events in JavaScript, you can use the
provided `JavelinJsonEvent` with some wrapper code :

```js
// see comment above
const loader = Vars.mods.mainLoader()
const JavelinJsonEvent = java.lang.Class.forName("fr.xpdustry.javelin.JavelinJsonEvent", true,
    loader)
const javelin = Vars.mods.getMod("xpdustry-javelin").main

function sendEvent(name, event) {
  const json = JavelinJsonEvent.getConstructors()[0].newInstance(name, JSON.stringify(event))
  javelin.getJavelinSocket().sendEvent(json)
}

function subscribe(name, subscriber) {
  javelin.getJavelinSocket().subscribe(JavelinJsonEvent, event => {
    if (event.getName().equals(name)) subscriber(JSON.parse(event.getJson()))
  })
}

subscribe("event-name", event => {
  // Example usage
  Log.info(event.data)
})

sendEvent("event-name", {
  data: "Hello"
})
```

## Notes

- The Javelin socket isn't reusable, **do not start nor close it yourself** !!!

- If you manage your servers in bulk, you can force them to wait for the javelin server
  to open with the `fr.xpdustry.javelin.socket.initial-connection-timeout` property (in seconds).

- Javelin does not currently have a way to customize the serialization of events, so make sure to
  only use simple java objects in your event classes.

- You can add `wss` support on javelin with a reverse proxy like nginx.
  Example with [certbot](https://certbot.eff.org/) :

  ```nginx
  # This is required to upgrade the websocket connection
  map $http_upgrade $connection_upgrade {
      default upgrade;
      '' close;
  }

  upstream javelin {
      # The address and port of your javelin server
      server example.org:8080;
      keepalive 64;
  }

  server {
      # The name of your server (example: javelin.example.org)
      server_name javelin.example.org;

      listen 443 ssl;
      listen [::]:443 ssl;

      # Don't forget to change that
      access_log /var/log/nginx/javelin.example.org-access.log;
      error_log /var/log/nginx/javelin.example.org-error.log;

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
      ssl_certificate /etc/letsencrypt/live/javelin.example.org/fullchain.pem; # managed by Certbot
      ssl_certificate_key /etc/letsencrypt/live/javelin.example.org/privkey.pem; # managed by Certbot
      include /etc/letsencrypt/options-ssl-nginx.conf;
      ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;
  }
  ```

  Now, your javelin server is accessible with `wss://javelin.example.org/`.

## Building

- `./gradlew :javelin-mindustry:jar` for a simple jar that contains only the plugin code.

- `./gradlew :javelin-mindustry:shadowJar` for a fatJar that contains the plugin and its
  dependencies (use this for your server).

## Testing

- `./gradlew :javelin-mindustry:runMindustryClient`: Run the plugin in Mindustry desktop.

- `./gradlew :javelin-mindustry:runMindustryServer`: Run the plugin in Mindustry server.
