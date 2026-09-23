# Looking for contributors
I currently don't have enough time to actively maintain ReTerminal.
If you're interested in keeping the project alive, contributions are very welcome!



# ReTerminal
**ReTerminal** is a sleek, Material 3-inspired terminal emulator designed as a modern alternative to the legacy [Jackpal Terminal](https://github.com/jackpal/Android-Terminal-Emulator). Built on [Termux's](https://github.com/termux/termux-app) robust TerminalView

Download the latest APK from the [Releases Section](https://github.com/RohitKushvaha01/ReTerminal/releases/latest).

# Features
- [x] Basic Terminal
- [x] Virtual Keys
- [x] Multiple Sessions
- [x] Ubuntu 22.04 LTS support (bundled Ubuntu Base 22.04.5, ARM64 only)
- [x] Always-dark theme
- [x] Simplified Chinese by default
- [x] Configurable Keyboard Shortcuts (Paste, Session Management)

## Offline Ubuntu CA certificates

The bundled Ubuntu 22.04 Base image does not include `ca-certificates`. On the first Ubuntu session, ReTerminal installs the following official Jammy packages offline from its APK assets (not from Android's certificate store):

- `ca-certificates_20260601~22.04.1_all.deb` — SHA-256 `6e8cdcc8c86103acd4fc14649eac62ff2037108389074a7b167567af33c32245`
- `openssl_3.0.2-0ubuntu1.29_arm64.deb` — SHA-256 `57ac2c1bc874531c81387d8842e73499203f11e997b4342540629de474c43366`

These files came from Ubuntu's `jammy-updates` repository. Their checksums were verified against `Packages.xz` from an `InRelease` file signed by the Ubuntu Archive Automatic Signing Key.

# Screenshots
<div>
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/01.png" width="32%" />
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/02.jpg" width="32%" />
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/03.jpg" width="32%" />
</div>

## Build on macOS

See [macOS APK build instructions](docs/macos-build.md) for prerequisites, debug APKs, release signing, and bundled Ubuntu image details.

## GitHub Releases

Push a `v*` tag to build its APK and publish it to GitHub Releases. You can also run **Android CI** manually with an existing `release_tag`. Optional `KEYSTORE` and `PROP` repository secrets configure release signing; without them the build uses the bundled testkey. See the [build instructions](docs/macos-build.md) for setup.

## Community
> [!TIP]
Join the reTerminal community to stay updated and engage with other users:
- [Telegram](https://t.me/reTerminal)
