# OfflinePotionTimer

## Overview

---

Continues reducing active potion-effect durations while player/server is offline.

When a player leaves the server, the plugin stores the expiration time of every active potion effect. When the player joins again, the plugin calculates how much real time has passed and restores only the remaining duration. 
For example, if a player has 10 minutes of Speed remaining and stays offline for 4 minutes, they will have approximately 6 minutes of Speed remaining when they return.

Potion effects continue counting down while:

- The player is offline.
- The server is stopped.
- The server is restarted.

## Requirements

---

- **Spigot or any other fork of it**
- **1.8.8 or newer**

No commands. No configuration. Just install the plugin and it works automatically.
