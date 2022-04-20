# Javelin

[![Build status](https://github.com/Xpdustry/Javelin/actions/workflows/build.yml/badge.svg?branch=master&event=push)](https://github.com/Xpdustry/Javelin/actions/workflows/build.yml)
[![Mindustry 6.0 | 7.0 ](https://img.shields.io/badge/Mindustry-6.0%20%7C%207.0-ffd37f)](https://github.com/Anuken/Mindustry/releases)
[![Xpdustry latest](https://repo.xpdustry.fr/api/badge/latest/releases/fr/xpdustry/javelin?color=00FFFF&name=Javelin&prefix=v)](https://github.com/Xpdustry/Javelin/releases)

## Description

A simple and fast communication system for Mindustry servers, enabling powerful features such as global chats and synced moderation.

## Usage

### First set up

1. Install the plugin on all the servers you wish to link and start them, it will create the necessary config files in the `./distributor/plugins/xpdustry-javelin-plugin` directory.

2. Choose a main server and generate a secret key with the `javelin server generate-secret` command. Once generated, stop the main server and set the following values in the `server-config.properties` file :

   - `javelin.server.enabled` to `true` to enable the server.

   - `javelin.server.secret` to the generated secret key.

3. Start the main server again, and add the servers you wish to link with the `javelin server add <name>` command. It will generate a token for each server so copy them and set the following values in the `client-config.properties` file of the servers :

  - `javelin.client.enabled` to `true` to enable the client.

  - `javelin.client.token` to the generated token.

  > You also need to create a client for the main server.

4. Restart all the servers and now, they all should be linked. If you have errors, turn on debug with the `config debug true` command and try again. If you can't figure out the problem, I'll be happy to help you in the **#support** channel of the [Xpdustry Discord server](https://discord.xpdustry.fr).

The plugin offers a global chat command by default with `/global <message>` or `/g <message>`.

### Managing service access

Javelin communicates using endpoints, where messages are handled. You can regulate the access of specific endpoints with blacklists and whitelists.

To do so, use the commands `javelin server blacklist/whitelist add/remove <name> <namespace> <subject>` where :

- `name` is the server name.

- `namespace` is the internal name of the plugin responsible for the endpoint (example: `xpdustry-javelin`).

- `subject` is the name of the specific handler that handles the message (example: `global-chat`).

So if you want, for example to disable the built-in global chat command for a specific server, do `javelin server blacklist add <server> xpdustry-javelin global-chat`.

### Api

It's very simple.

First, add this in your `build.gradle` :

```gradle
repositories {
    // Replace with "https://repo.xpdustry.fr/snapshots" if you want to use the snapshots
    maven { url = uri("https://repo.xpdustry.fr/releases") }
}

dependencies {
    // Add "-SNAPSHOT" after the version if you are using the snapshot repository
    compileOnly("fr.xpdustry:javelin:0.3.1" )
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

Finally, in your code, get the client with `Javelin.getClient()`, check if it's connected with `isConnected()` and enjoy.

You can `send` or `broadcast` any message you like, as long as it is serializable by `Gson`.

### Tips

- In the client config, if you set `javelin.client.timeout` to `0`, Your client will always reconnect to your main server, even if it has been down for a long time. 

- You can explicitly reconnect a Javelin client with the `javelin client reconnect` command.

- You can reset the token of a specific server with `javelin server token reset <name>`.

- List all the registered servers with `javelin server list`

- If you have https enabled on your server domain with a reverse proxy (nginx, Apache web server, ...), enable the secure websocket protocol by setting :

  - `javelin.server.wss` to `true` in the `server-config.properties` file in the main server.

  - `javelin.client.wss` to `true` in the `client-config.properties` file of each client.

   Then change the config of your reverse proxy. Example with nginx + let's encrypt / certbot :

   ```nginx
   # This is required to upgrade the websocket connection
   map $http_upgrade $connection_upgrade {
       default upgrade;
       '' close;
   }

   upstream javelin {
       # I use pterodactyl pannel so I use the node url, if you don't, use 127.0.0.1 or localhost
       # 12000 is the port of my javelin main server
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

   Your server will also refuse any normal websocket connection now.

## Building

- `./gradlew jar` for a simple jar that contains only the plugin code.

- `./gradlew shadowJar` for a fatJar that contains the plugin and its dependencies (use this for your server).

## Testing

- `./gradlew runMindustryClient`: Run Mindustry in desktop with the plugin.

- `./gradlew runMindustryServer`: Run Mindustry in a server with the plugin.

## Running

This plugin is compatible with V6 and V7, but it requires the following dependencies :

- [xpdustry-distributor-core](https://github.com/Xpdustry/Distributor)

- [xpdustry-kotlin-stdlib](https://github.com/Xpdustry/KotlinRuntimePlugin)

If you run on v135 or lower, you will need [mod-loader](https://github.com/Xpdustry/ModLoaderPlugin) for the dependency resolution.
