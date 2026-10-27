# Inmersive Backpack (Fabric 1.20.1, Java 17)

Mochila de viajero con dos acabados (cuero de vaca y piel de conejo):

- 54 casillas (cofre doble) + 1 casilla azul solo para faroles + 1 casilla roja para la manta.
- Se ve en 3D en la espalda del jugador; el farol cuelga de una cuerda corta y da luz (15 farol, 10 farol de almas).
- Sin durabilidad, a prueba de fuego y lava, sin penalizacion de peso o velocidad.
- Tecla B: abrir la mochila (con ella en cualquier parte del inventario). Clic derecho tambien.
- Shift + clic derecho en el suelo con manta: dormir sobre ella (camara en tercera persona, manta que se despliega y se recoge).
- Tecla H o boton "?" en la mochila: guia de controles.
- Recetas en la mesa de crafteo (aparecen en el libro de recetas al buscar "mochila"/"backpack").

## Compilar

Con GitHub Actions (ver .github/workflows/build.yml) o a mano: JDK 17 + Gradle 8.x

    gradle wrapper --gradle-version 8.6
    ./gradlew build

El .jar queda en `build/libs/inmersivebackpack-1.1.0.jar` (no uses el `-sources`).

## Ajustes

- Giro de la manta visible: `BLANKET_YAW_OFFSET` en `sleep/BedrollManager.java`.
- Posicion del farol colgado: `renderLantern` en `client/BackpackFeatureRenderer.java`.
- Items aceptados como manta: `data/inmersivebackpack/tags/items/bedrolls.json`.
