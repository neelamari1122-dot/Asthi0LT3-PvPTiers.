# Asthi0LT3 PvP Tiers — Minecraft Java 1.21.11 (Fabric client mod)

## IMPORTANT STATUS

**Ported project source, NOT a compiled or Minecraft-tested mod JAR.** To get a working installable file, a real Gradle/Fabric Loom build needs to succeed, and the built JAR should be tested in-game. The ZIP is not installable into `mods/` by itself. No actual Fabric compilation has run in the present workspace (Gradle and network access to dependency repositories are unavailable).

## Build prerequisites

- Minecraft Java **1.21.11**
- **JDK 21**
- Fabric **Loader 0.19.3 or compatible newer loader**
- Fabric API **0.141.4+1.21.11**, compatible version
- Gradle **9.6.1** with internet connection
- Loom **1.17.13** with `net.fabricmc.fabric-loom-remap` and official Mojang mappings

## Obtain an actual JAR (easiest: GitHub Actions)

1. Create your GitHub repository, upload the **contents** of this ZIP into the repository root, including the `.github` directory. (Don't upload the ZIP as a single file.)
2. On GitHub, select **Actions -> Build Asthi0LT3 PvP Tiers for 1.21.11 -> Run workflow**. The workflow will also run on push.
3. After the job succeeds, download the artifact `Asthi0LT3-PvPTiers-1.21.11-JAR`. It should contain `Asthi0LT3-PvPTiers-1.21.11-1.1.0.jar`.
4. Place only that compiled mod JAR into `%appdata%\.minecraft\mods` alongside Fabric API for 1.21.11. Run the 1.21.11 Fabric profile. Do **not** install the sources jar.
5. If the build fails, inspect **Actions -> failed job -> Compile and remap** logs; source has not been verified against live libraries.

Or locally with JDK 21 + Gradle 9.6.1 installed: `gradle build`. The build output (if successful) is in `build/libs`.

## Tier owner & how players see tags

Only the name `Asthi0LT3` may use the assignment command inside the mod. To **publish** official shared tiers, the owner must set a **private GitHub Gist access token** on the owner's own computer. GitHub enforces write authorization, not a username typed into local mod files. Never give anyone the token and never include it in a public repository or JAR.

The mod is **client-side only** (no server plugin required). All viewers must install the client mod and configure the same **public Gist ID**; then updates are loaded on their computers. Players without the mod cannot see custom tags. Without Gist configuration, your changes remain local and won't automatically appear to others. The original owner name is a convenient local UI restriction, not a secure Minecraft-account identity check. Public rankings are visible to everyone with the Gist URL.

### Gist configuration steps

1. Owner: Create a public GitHub Gist at https://gist.github.com/ with a file named `tiers.json` initially containing `{"players":{}}`.
2. Owner: Start Minecraft with mod once, close the game, edit `%appdata%\.minecraft\config\pvptiers-client.json` and set `gistId` to the Gist ID and `ownerGistToken` to a private GitHub token that can edit the Gist.
3. Viewer: Install the mod, start and close game, then configure **only the same `gistId`**; leave `ownerGistToken` empty.
4. Restart the clients. They automatically fetch the tier list periodically (roughly every minute). The owner can call `/pvptier sync` to refresh immediately.

## Modes (22)

CPvP, Sword, Axe, Mace, SMP, UHC, NetPot, DiaPot, Pot, Gapple, Bow, Trident, Shield, Bed, Anchor, Cart, Crossbow, Elytra, TNT, Diamond, Netherite, Card.

## Ranks

LT5, HT5, LT4, HT4, LT3, HT3, LT2, HT2, LT1, HT1.

## Client-side chat commands (while inside any world)

```
/pvptier modes
/pvptier tiers
/pvptier view Steve
/pvptier whoami
/pvptier sync
/pvptier set Steve cpvp HT3
/pvptier set Alex sword LT4
/pvptier remove Steve cpvp
```

Tier assignments are per mode; only the most recently assigned mode displays beside the player's name, and other modes remain viewable with `/pvptier view`. Commands work client side without server mod support if the mod successfully loads. This code does not change the actual Minecraft username or server tags.

## Limitations

- This project has **not** been built against Fabric's official Minecraft 1.21.11 libraries or launched inside the game here. It may require additional code changes to compile or work. Do not present the ZIP or a renamed copy as a finished JAR.
- A public Gist ID must be configured on every player's client for shared tiers. A future change could embed a public read-only Gist ID in the distributed mod (the ID is not supplied yet).
- Tier visibility is only for users with this mod installed; the server cannot force vanilla clients to show custom tags.
- The current code uses username matching for its owner-only UI. GitHub authentication, not just the username, determines whether official online data can be changed.
