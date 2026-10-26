# Inmersive Backpack (Fabric 1.20.1, Java 17)

Mochila Medieval de Viajero:

- 54 casillas (cofre doble) + 2 correas para herramientas + 1 casilla de manta.
- Sin durabilidad, a prueba de fuego y lava, sin ninguna penalizacion de peso o velocidad.
- Clic derecho: abre la mochila.
- Shift + clic derecho sobre la cara superior de un bloque, con una manta (cama) equipada: el jugador se tumba a dormir sin necesitar cama.
- La mochila no se puede meter dentro de si misma ni dentro de otra mochila.

## Compilar

Requisitos: JDK 17 y Gradle 8.x.

    gradle wrapper --gradle-version 8.6
    ./gradlew build

El .jar queda en `build/libs/inmersivebackpack-1.0.0.jar` (no uses el `-sources`).
Instalalo junto con Fabric Loader y Fabric API 0.92.2+1.20.1.

## Personalizar

- Items aceptados como manta: `data/inmersivebackpack/tags/items/bedrolls.json` (por defecto, camas y alfombras de lana).
- Giro de la manta visible: constante `BLANKET_YAW_OFFSET` en `sleep/BedrollManager.java`.
- Receta: `data/inmersivebackpack/recipes/smart_backpack.json` (borrala si prefieres solo /give).
- Prueba rapida: `/give @s inmersivebackpack:smart_backpack` y `/time set night`.
