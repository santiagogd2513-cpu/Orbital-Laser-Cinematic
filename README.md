# Chunk loading improvements — Minecraft Java 1.20.1 / Forge 47.4.5

This branch adds chunk-loading experiments to the existing Forge project.

## Current implementation

- Client render-distance slider ceiling is raised to 128 chunks. This is only a selectable ceiling; practical performance depends on the PC, memory, GPU, world generation and server view-distance.
- The client requests vanilla's nearby-first chunk rebuild priority.
- On the server, a short-lived region ticket requests the chunk roughly 24 blocks ahead of each moving player once per second. The normal Minecraft/Forge chunk pipeline handles generation and loading asynchronously, so work can begin before the player reaches that area.
- GitHub Actions compiles and verifies the project on pushes to this branch and uploads the built JAR as a 30-day artifact.

## Limits and safety

Prefetching does not bypass a remote server's view-distance or force it to send chunks outside the allowed range. It can increase server CPU, memory and disk activity, especially with multiple players. The ticket expires automatically; this is deliberately a small, bounded prefetch rather than unbounded world generation.

A true performance result still needs an in-game test on the target Forge installation. No fixed speed multiplier is promised.

## Download

Open the repository's **Actions** tab, select the latest successful **Build ChunkBoost Mod** run, and download the **ChunkBoost-Forge-1.20.1** artifact.
