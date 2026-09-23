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
