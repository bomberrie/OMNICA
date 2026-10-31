# OMNICA

*The Vanilla-First Logistics & Supply Chain Network for Fabric*

OMNICA replaces legacy pipe-and-hopper automation with a unified, visually stunning node network. It acts as the ultimate extension of modern Minecraft mechanics, using zero custom ores and expanding strictly upon vanilla Crafters, Bundles, Copper, and Amethyst.

---

## Features

- **Nexus Node System**: Attach Nexus Nodes to vanilla containers (Chests, Barrels, Crafters) and wirelessly link them up to 32 blocks.
- **Zero-Tick Lag**: Item transfers are calculated mathematically via cached adjacency arrays and the Fabric Transfer API, bypassing block updates and entity tick loops.
- **Visual Laser Tethers**: Client-side instanced laser beams and floating item animations without server-side `ItemEntity` overhead.
- **Bundle Filtering**: Insert Bundles into nodes to act as multi-item whitelists directly using vanilla `BundleContentsComponent`.
- **Vanilla-First Recipes**: Crafted using Copper Ingots, Amethyst Shards, and Redstone Dust.

---

## Building and Running

### Prerequisites
- JDK 25+
- Fabric Loader >= 0.20.x
- Minecraft 26.3 ("Wilderness Bound")

### Build
```bash
./gradlew build
```

### Run Client
```bash
./gradlew runClient
```
