# TameAll

[![Build](https://github.com/riqvip/tameall/actions/workflows/build.yml/badge.svg)](https://github.com/riqvip/tameall/actions/workflows/build.yml)

TameAll is a Fabric mod for Minecraft Java 26.2 that turns eligible living
creatures into persistent, configurable companions while keeping their original
entity, equipment, variant, and name.

## Screenshots

| Companion controls | Movement and combat |
|:--:|:--:|
| ![Companion overview](docs/images/companion-overview.png) | ![Movement controls](docs/images/companion-movement.png) |

| Collection | Abilities |
|:--:|:--:|
| ![Collection controls](docs/images/companion-collection.png) | ![Ability controls](docs/images/companion-abilities.png) |

| Companion inventory | Target editor |
|:--:|:--:|
| ![Companion inventory](docs/images/companion-inventory.png) | ![Target editor](docs/images/target-editor.png) |

| Companion roster | Item textures |
|:--:|:--:|
| ![Companion roster](docs/images/companion-roster.png) | ![TameAll item textures](docs/images/items.png) |

## Features

- Bond eligible creatures with Golden Wheat or the chance-based Gilded Wheat.
- Manage movement, combat stance, operating area, protection abilities, item
  collection, equipment, cargo, and nametags from dark-themed screens.
- Use Passive, Assist, or Defend Area behavior with include and exclude target
  groups, entity types, and tags.
- Recall existing companions with the Companion Whistle, including companions
  that are currently unloaded when they can be found.
- Preserve bond state, equipment, cargo, target selections, and roster records
  across saves and entity conversions.
- Keep the original entity and its behavior identity while making its actions
  owner-aware and configurable.

## Development status

TameAll is an early `0.1.0-alpha.1` release. Features can still change during
the alpha series. The mod has been exercised through ongoing play in a real
Minecraft client, the common and client source sets compile, the full Gradle
build passes, and the headless server GameTest suite passes all 18 required
tests.

The interactive client GameTest needs a graphical display and is not available
in the headless WSL development environment. Manual client playtesting remains
part of development; focused regression coverage is recorded in
[PLAN.md](PLAN.md).

## Requirements

- Minecraft Java 26.2
- Fabric Loader 0.19.5 or newer
- Fabric API for Minecraft 26.2

## Building and testing

Use Java 25 and run Gradle through the included wrapper:

```text
./gradlew compileJava compileClientJava
./gradlew runGameTestServer
./gradlew build
```

The release JAR is written to `build/libs/`.

## Release track

The project uses a staged SemVer release track:

- `0.1.0-alpha.N` — early testing while features and behavior can change.
- `0.1.0-beta.N` — feature set is mostly complete and testing focuses on
  stability.
- `0.1.0-rc.N` — release candidate pending final verification.
- `0.1.0` — first stable release.
- `0.1.1` — compatible bugfix release after `0.1.0`.

The first public package is planned as the GitHub pre-release
`v0.1.0-alpha.1`, including the matching JAR.

## License

TameAll is licensed under the [Mozilla Public License 2.0](LICENSE).
