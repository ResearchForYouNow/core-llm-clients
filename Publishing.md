# PUBLISHING.md

## Prerequisites (one-time)

* **Central Portal token** in `~/.gradle/gradle.properties`:

  ```
  sonatypeUsername=YOUR_PORTAL_TOKEN_USERNAME
  sonatypePassword=YOUR_PORTAL_TOKEN_PASSWORD
  ```
* **GPG** installed and configured for Gradle signing:

  ```
  signing.gnupg.executable=/opt/homebrew/bin/gpg   # check with: which gpg
  signing.gnupg.keyName=YOUR_KEY_FINGERPRINT       # or last 8 chars
  signing.gnupg.passphrase=                         # blank if none
  ```
* **Public key published** to keys.openpgp.org (email verified).

## Release (Maven Central)

1. **Bump version** in root `build.gradle.kts`:

   ```kotlin
   allprojects {
       version = "x.y.z"
   }
   ```
2. (Optional) commit & tag:

   ```bash
   git commit -am "release x.y.z" && git tag vx.y.z && git push && git push --tags
   ```
3. **Publish + close & release**:

   ```bash
   ./gradlew --no-daemon publishToSonatype closeAndReleaseSonatypeStagingRepository
   ```
4. Check status in Central Portal → **Publishing → Deployments**.

## Snapshot (nightlies)

1. Set version to `x.y.(z+1)-SNAPSHOT`.
2. Publish:

   ```bash
   ./gradlew --no-daemon publishToSonatype
   ```

   (No close/release step for snapshots.)

## Coordinates

```groovy
implementation "io.github.researchforyounow:core-api:x.y.z"
implementation "io.github.researchforyounow:llm-provider-openai:x.y.z"
implementation "io.github.researchforyounow:llm-provider-gemini:x.y.z"
implementation "io.github.researchforyounow:llm-clients:x.y.z"
```

## Quick Troubleshooting

* **“Artifact already exists”** → bump version and republish.
* **“Invalid signature”** → ensure Gradle uses the right GPG key (`signing.gnupg.keyName`) and your **public** key is visible on keys.openpgp.org.
* **“No staging repository created”** → run both tasks in one command (as above).
* **401** → using old OSSRH creds; use **Central Portal** token.

That’s it.
