# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/),
and this project adheres to [Semantic Versioning](http://semver.org/).

## v1.3.0 - 2023-03-21

### Features

- Added an option in `javelin-mindustry` to block the server for a given time while connecting to the Javelin server.
- Added an option for sockets to be able to broadcast their events in their own event bus (https://github.com/Xpdustry/Javelin/pull/4).

### Chores

- Improved tutorial in the README.

## v1.2.1 - 2022-12-11

### Bugfix

- Fix a bug where disabling auto-restart in `javelin-mindustry` causes the socket to not start.

## v1.2.0 - 2022-11-05

Major release with very nice stuff :)

### Changes

- `UserAuthenticator` is now part of `javelin-core`.
- Sockets no longer show errors when an unknown event class is received.
- Better documentation.

### Features

- Added simple JavaScript support.
- Added new socket state for unusable sockets.
- In `javelin-mindustry`, added command to restart the socket (`javelin-restart`)

### Breaking

- The hashing format of the default `UserAuthenticator` has changed to Bcrypt. All existing users in `javelin-mindustry` are invalid.

## v1.1.0 - 2022-11-02

### Features

- Added credential-less connections for Javelin servers and sockets on the same network.
- Javelin clients can now be restarted.

### Changes

- Internal improvements of the sockets.

## v1.0.0 - 2022-10-01

- Hello world!
