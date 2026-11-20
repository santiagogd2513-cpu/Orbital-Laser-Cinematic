# ChunkBoost — Forge 1.20.1

Mod cliente para Minecraft Java 1.20.1 + Forge 47.4.5.

## Funciones iniciales

- Eleva el límite del control de distancia de renderizado de 32 a 128 chunks mediante un mixin de cliente.
- Activa la prioridad de actualización **NEARBY** de Minecraft para que los chunks cercanos al jugador se reconstruyan antes que los lejanos.
- GitHub Actions compila automáticamente al hacer push a esta rama o a `main`, y publica el JAR como artefacto descargable durante 30 días.

## Importante sobre 128 chunks

128 es el máximo que permite seleccionar el mod, no una garantía de que cualquier equipo pueda renderizar esa distancia. El consumo de RAM/VRAM y el tiempo de generación de chunks crecen mucho; el servidor también puede limitar la distancia de visualización. Se recomienda empezar con valores bajos y subir gradualmente.

La prioridad NEARBY mejora el orden en que se atienden las actualizaciones visibles; no elimina los límites de CPU, GPU, almacenamiento, red ni generación del mundo. No se promete un multiplicador fijo de velocidad.

## Compilar

En **Actions**, abre la ejecución y descarga el artefacto **ChunkBoost-Forge-1.20.1**.

Requisitos: Java 17, Minecraft Java 1.20.1 y Forge 47.4.5.
