# rssh

Run SSH commands on your servers with one tap — from Quick Settings tiles or home-screen shortcuts.

Configure hosts and commands in the app, then:

- **Quick Settings tiles** — five tiles (`rssh 1`–`rssh 5`) that each run one command. Assign
  commands under the *Tiles* tab, then add the tiles from the Quick Settings editor.
- **Home-screen shortcuts** — tap the home icon next to a command on the *Commands* tab and confirm
  the launcher's pin dialog. Renaming the command relabels the shortcut; deleting it disables the
  shortcut.

Each run ends with a toast: `✓ label: first line of output` or `✗ label (exit N): error`. While
the Quick Settings panel is open, the tile itself also shows *Running…* (greyed out, so it can't
be tapped twice) and then a short result for a few seconds before returning to the host name.

## Setup

1. Open the app and tap the lock icon to generate an Ed25519 key pair.
2. Copy the public key line into `~/.ssh/authorized_keys` on each host.
3. Add the host, tap **Test connection**, and confirm the fingerprint it shows against
   `ssh-keygen -lf /etc/ssh/ssh_host_*_key.pub` on the server.
4. Add a command, pick the host, then assign it to a tile or pin it to the home screen.

## Security model

- The private key is generated on the device and stored encrypted with a non-exportable AES key in
  the Android Keystore. Backups are disabled; the key and database never leave the device.
- Host keys are trust-on-first-use: an unknown key is only accepted from the *Test connection*
  dialog, never from a tile or shortcut. A changed host key refuses the connection until you forget
  the saved key in the host's edit screen.
- Tiles require the device to be unlocked before running anything. Shortcuts live on the launcher,
  which is only reachable when unlocked, and their target activity is not exported, so no other
  app can trigger a command.

## Building

Requires JDK 17+ and an Android SDK with platform 37 (the build downloads matching build-tools).

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk   # or any JDK 17+
export ANDROID_HOME=~/Android/Sdk               # or set sdk.dir in local.properties
./gradlew assembleDebug testDebugUnitTest
adb install app/build/outputs/apk/debug/app-debug.apk
```

Unit tests cover the host-key verifier and the SSH runner against an in-process Apache MINA
server, plus the tile state mapping, run-state store and result formatting, so no device is
needed for them.

GitHub Actions runs the same build on every push and pull request and uploads the debug APK as
the `rssh-debug-apk` artifact. Pushing a `v*` tag also attaches it to a GitHub release. Debug
builds are signed with the committed `app/debug.keystore`, so CI and local builds install over
each other.

## Stack

Kotlin, Jetpack Compose (Material 3), Room, WorkManager, [sshj](https://github.com/hierynomus/sshj)
with BouncyCastle. minSdk 29.
