# TemplatePlugin

[![Build status](https://github.com/Xpdustry/TemplatePlugin/actions/workflows/build.yml/badge.svg?branch=master&event=push)](https://github.com/Xpdustry/TemplatePlugin/actions/workflows/build.yml)
[![Mindustry 6.0 | 7.0 ](https://img.shields.io/badge/Mindustry-6.0%20%7C%207.0-ffd37f)](https://github.com/Anuken/Mindustry/releases)
[![Xpdustry latest](https://repo.xpdustry.fr/api/badge/latest/snapshots/fr/xpdustry/template-plugin?color=00FFFF&name=TemplatePlugin&prefix=v)](https://github.com/Xpdustry/TemplatePlugin/releases)

## Description

**Xpdustry variation for publishing packages to our repo.**

This template features some cool stuff such as:

- [Jitpack](https://jitpack.io/) support.

- GitHub action for easier release and Jitpack usage:

  - To create a new release, edit `CHANGELOG.md` and then run `./gradlew createRelease`, it will automatically create
    a release tag and push it to trigger the release workflow. If you pushed your release by mistake, simply run this
    in your terminal:

    ```batch
    # https://stackoverflow.com/a/5480292/15861283
    git push --delete origin v{release-version}
    git tag -d v{release-version}
    ```

## Building

- `./gradlew jar` for a simple jar that contains only the plugin code.

- `./gradlew shadowJar` for a fatJar that contains the plugin and its dependencies (use this for your server).

## Testing

- `./gradlew runMindustryClient`: Run Mindustry in desktop with the plugin.

- `./gradlew runMindustryServer`: Run Mindustry in a server with the plugin.

## Running

This plugin is compatible with V6 and V7.

## Nice tips

- When using this template, don't forget to change `plugin.json` and `gradle.properties`.

- This template targets V6 by default, you can change it by editing `minGameVersion` in `plugin.json`.

- Don't forget to edit `props.root-package` in `gradle.properties` to enable NullAway.

- To make sure gradle is always executable do :

```batch
# https://stackoverflow.com/a/54048315/15861283
git update-index --chmod=+x gradlew
git add .
git commit -m "Changing permission of gradlew"
git push
```

- Don't forget to bump your dependencies with the `dependencyUpdates` task.

- If you want to use the Xpdustry template, before doing anything, run this in your terminal to set the `xpdustry-master` branch as master :

```batch
git fetch origin xpdustry-master
git checkout xpdustry-master
git branch -m master old-master
git branch -m xpdustry-master master
git branch -rD origin/master
git push origin master -f
git branch -D old-master
git push origin --delete xpdustry-master
```
