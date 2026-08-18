# Brute Force Culling

<a href='https://fabricmc.net'><img alt="fabric" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/fabric_vector.svg"></a>
<a href='https://neoforged.net/'><img alt="neoforge" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/neoforge_vector.svg"></a>

Brute Force Culling Revamped is a modern multiloader port of [Brute Force Rendering Culling Revived](https://www.curseforge.com/minecraft/mc-mods/brute-force-rendering-culling-revived).

### What does the original mod do?

Brute Force Render Culling is a performance mod that improves how Minecraft handles rendering.

It uses advanced occlusion culling techniques to skip drawing chunks, entities, and block entities that the player
cannot see. By cutting out this hidden work, the mod reduces the strain on chunk compilation and rendering, which helps
keep frame rates steadier in heavy modpacks or areas crowded with blocks and mobs.

**The biggest gains show up in enclosed spaces or builds with lots of occlusions**, while open landscapes see less
impact. Rendering tasks are canceled early at the CPU stage, saving resources before they ever reach the GPU.

### Be Aware!

If your graphics card is too old, the mod will automatically turn itself off to avoid crashes. Before installing, make
sure your GPU supports at least OpenGL 3.3.

Modpack creators don’t need to worry! if a player’s hardware isn’t compatible, the mod simply disables itself quietly in
the background.

Brute Force Culling Revived brings back the original Brute Force Rendering Culling mod with important fixes and updates
for modern Minecraft.

The original project introduced aggressive occlusion culling to skip rendering hidden chunks and entities, but it
struggled with crashes, outdated code, and limited compatibility.

### Credits:

Original mod created by Misanthropy (Brute Force Rendering Culling Revived).

_Original_ original mod created by Rogo (Brute Force Rendering Culling).

Logo by Nekomaster!

## License

[![Code license (LGPL-3.0)](https://img.shields.io/badge/code%20license-LGPL3.0-green.svg?style=flat-square)](https://github.com/evanbones/Brute-Force-Culling/blob/1.21.1/LICENSE)

---

[![github-plural](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/social/github-plural_vector.svg)](https://github.com/evanbones/Brute-Force-Culling)
