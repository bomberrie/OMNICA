# OMNICA

[![Minecraft](https://img.shields.io/badge/Minecraft-26.3%20%28Wilderness%20Bound%29-blue?logo=minecraft)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/Mod%20Loader-Fabric-orange?logo=fabric)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-25-red?logo=openjdk)](https://adoptium.net/)
[![Author](https://img.shields.io/badge/Author-Bomberrie-purple?logo=github)](https://github.com/Bomberrie)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**The Vanilla-First Logistics & Storage Network for Minecraft.**

**OMNICA** replaces messy hopper lines, noisy dropper towers, and lag-heavy water streams with a clean, magical wireless item network. Connect your chests, barrels, and auto-crafters across your base using resonant amethyst crystals and copper conductors — with **zero custom ores**, **zero energy fuels**, and **zero lag**.

---

## ✨ Why OMNICA?

Traditional Minecraft sorting systems and pipe mods often feel either clunky or out of place:
- **No Hopper Lag**: Hoppers constantly check the world for items above them every tick. OMNICA uses a mathematical graph routing engine built on Fabric's native Transfer API — moving entire stacks smoothly without dropping server ticks per second (TPS).
- **100% Vanilla-First**: No new worldgen ores to hunt for, no RF/FE generators, and no oil refineries. Everything is crafted with items already in vanilla: **Copper, Amethyst, Redstone, and Crafters**.
- **Watch Your Items Fly**: Items don't disappear into invisible black boxes. You can see luminous amethyst laser tethers linking your chests, and watch your items physically float and glide along radiant arcs through the air!
- **Works with Modern Minecraft**: Native support for **Minecraft 26.3 ("Wilderness Bound")**, **Java 25**, and 1.21+ features like **Bundles** and the **Crafter**.

---

## 🛠️ How It Works: Player Guide

OMNICA is designed to be simple to pick up, yet deep enough to automate your entire survival base.

### 1. Nexus Nodes
The building block of your network. Attach a **Nexus Node** directly against any container:
- Regular Chests & Double Chests
- Trapped Chests & Barrels
- Vanilla Crafters & Droppers

#### Node Modes
Right-click or shift-right-click a placed node with an empty hand to switch its operating mode:
- 🔵 **Extractor (Cyan)**: Actively extracts items out of this container and sends them into the network.
- 🟣 **Inserter (Purple)**: Receives items from the network and stores them inside this container.
- 🟡 **Balanced (Gold)**: Can both extract and receive items, keeping connected inventories equalized.

#### Directional Filters
Want a chest to only store Iron Ingots, or an auto-smelter to only receive raw ores?
- **Single Item Filter**: Right-click the Nexus Node while holding any item to set it as an exclusive filter.
- **Bundle Multi-Filter (Up to 64 Items)**: Right-click the node with a filled vanilla **Bundle**! The node will read the contents of the bundle and whitelist all items inside it at once.
- **Clear Filter**: Sneak-right-click the node with an empty hand while already in Balanced mode to clear any active filter.

---

### 2. Nexus Linker
The **Nexus Linker** is your network tuning tool.
- **Link Nodes**: Right-click a starting Nexus Node, walk over to your target node (up to 32 blocks away by default), and right-click it. You will hear an amethyst chime and see a radiant laser tether establish between them!
- **Reset Selection**: Right-click the same node again or right-click into the air while sneaking to clear your current selection.
- **Multi-Node Networks**: A single node can connect to multiple neighbors, forming a unified web of chests across your base.

---

### 3. Request Terminal
The ultimate command center for your storage room.
- Place an **Omnica Request Terminal** anywhere adjacent to a Nexus Node.
- Right-click the terminal to open the search interface.
- **Instant Search**: Type the name of any item in the search bar.
- **One-Click Delivery**: Left-click any item in the terminal to pull a stack directly from your remote chests into your inventory!

---

## 🔨 Crafting Recipes

All items in OMNICA are crafted using 100% vanilla ingredients.

### Nexus Node (Yields 4x)
Crafted at a standard Crafting Table using Copper Ingots, Amethyst, and Redstone:

```
[       ] [ Amethyst Shard ] [       ]
[Redstone] [  Copper Ingot  ] [Redstone]
[ Copper ] [  Copper Ingot  ] [ Copper ]
```
- **Ingredients**: 1 Amethyst Shard, 2 Redstone Dust, 5 Copper Ingots.
- **Yield**: **4x Nexus Nodes**

---

### Nexus Linker (Yields 1x)
A handheld tuning tool crafted like a wand:

```
[       ] [       ] [ Amethyst Shard ]
[       ] [Redstone] [       ]
[ Copper ] [       ] [       ]
```
- **Ingredients**: 1 Amethyst Shard, 1 Redstone Dust, 1 Copper Ingot.
- **Yield**: **1x Nexus Linker**

---

### Omnica Request Terminal (Yields 1x)
The central logistics interface, powered by the vanilla Crafter:

```
[ Copper ] [ Amethyst Block ] [ Copper ]
[Redstone] [     Crafter    ] [Redstone]
[ Copper ] [  Copper Ingot  ] [ Copper ]
```
- **Ingredients**: 1 Amethyst Block, 1 Vanilla Crafter, 2 Redstone Dust, 5 Copper Ingots.
- **Yield**: **1x Omnica Request Terminal**

---

## ⚙️ Mod Menu & Configuration

If you have **Mod Menu** installed, you can configure OMNICA directly from the in-game settings screen! Click **Mods** → **OMNICA** → **Configure**.

All settings are also saved to `config/omnica.json`:

| Setting | Default | Range | Description |
| :--- | :---: | :---: | :--- |
| **Max Link Distance** | `32` | `8 - 128 blocks` | Maximum distance allowed between two linked nodes. Increase for mega-bases! |
| **Transfer Interval** | `8 ticks` | `1 - 40 ticks` | How often the network transfers items. 8 ticks = 0.4s. Lower is faster; higher is ultra-lightweight. |
| **Batch Size** | `64 items` | `1 - 64` | Maximum stack quantity moved in a single transfer pulse. |
| **Render Tether Beams** | `ON` | `ON / OFF` | Renders vibrant amethyst laser beams between connected nodes. |
| **Render Floating Items** | `ON` | `ON / OFF` | Shows traveling item stacks physically gliding through the air. |
| **Network Sound Effects** | `ON` | `ON / OFF` | Toggles the musical amethyst chime sounds when items transfer. |

---

## 💻 Developer & Build Options

OMNICA is built with modern Gradle and Fabric Loom for Minecraft 26.3 and Java 25.

### System Requirements
- **Java Development Kit (JDK) 25** (recommended: [Eclipse Adoptium Temurin 25](https://adoptium.net/))
- **Fabric Loader 0.20.1+**
- **Fabric API 0.165.0+26.3**

### Build Commands

```bash
# Clone the repository
git clone https://github.com/Bomberrie/omnica.git
cd omnica

# Compile and package the mod jar
./gradlew build
```
The compiled jar will be located at:
```
build/libs/omnica-0.1.0-alpha.jar
```

### Development Launch Options

```bash
# Launch test Minecraft client with hot-reloading
./gradlew runClient

# Launch headless dedicated test server
./gradlew runServer
```

---

## 👤 Credits & Author

- **Lead Developer & Creator**: **[Bomberrie](https://github.com/Bomberrie)**
- **Architecture**: Powered by the Fabric Transfer API and Loom toolchain.
- **Sound & Visual Design**: Built using vanilla Amethyst resonance and modern Minecraft 26.3 rendering.

---

## 📄 License

OMNICA is licensed under the open-source **[MIT License](LICENSE)**. Feel free to use it in any modpack, fork, or showcase!
