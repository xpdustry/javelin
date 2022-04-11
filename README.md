# TemplatePlugin

[![Build status](https://github.com/Xpdustry/Javelin/actions/workflows/build.yml/badge.svg?branch=master&event=push)](https://github.com/Xpdustry/Javelin/actions/workflows/build.yml)
[![Mindustry 6.0 | 7.0 ](https://img.shields.io/badge/Mindustry-6.0%20%7C%207.0-ffd37f)](https://github.com/Anuken/Mindustry/releases)
[![Xpdustry latest](https://repo.xpdustry.fr/api/badge/latest/releases/fr/xpdustry/javelin?color=00FFFF&name=Javelin&prefix=v)](https://github.com/Xpdustry/Javelin/releases)

## Description

A simple and fast cross communication system for Mindustry servers.

## Usage

### First set up

First, install the plugin on all the servers you wish to link and start them, it will create the necessary config files in the `./distributor/plugins/xpdustry-javelin-plugin` directory.

Now choose a main server and generate a secret key with the `javelin server generate-secret` command, it is really important for the security.

Then, set the `javelin.server.enabled` property to `true` and set the `javelin.server.secret` property to the generated secret in the main server in the `server-config.properties` file.

Now that the main server is ready, add new linked servers with the `javelin server add <name>` command. It will generate a token for each so copy and set the `javelin.client.token` property with it in the `client-config.properties` file in the destination server. 

> You also need to create a client for the main server.

Now, you may start all the servers to see if they link correctly. After that, Javelin is ready to be used.

### Registering services

Javelin network security is handled with endpoints, they allow external servers to access your Javelin network without leaking sensitive data.

To add an endpoint to a server, do the command `javelin server endpoint add <name> <namespace> <subject>` where :

- `name` is the server name.

- `namespace` is the internal name of the plugin responsible for the endpoint (example: `xpdustry-javelin`).

- `subject` is the name of the specific handler that will handle the message (example: `whisper`).

So if you want, for example to enable the built-in global whisper command of Javelin on a server, do the `javelin server endpoint add <name> xpdustry-javelin whisper` in the main server.

### Api usage

Its terribly simple, just get the client with `Javelin.getClient()`, check if it's not null, and enjoy.

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

## Support

Strange error codes ? Crashes ? Report them in the [Xpdustry discord server](https://discord.xpdustry.fr) in the **#support** channel.

## TODO

- [ ] Implement auto reconnection logic.

- [ ] Improve logging.
