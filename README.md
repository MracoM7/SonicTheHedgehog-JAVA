# Sonic the Hedgehog — Java

Un livello giocabile ispirato a Green Hill Zone (Sonic the Hedgehog, Sega Genesis, 1991),
scritto da zero in Java puro: fisica a 360° su pendenze e loop in stile
[Sonic Physics Guide](https://info.sonicretro.org/Sonic_Physics_Guide), rendering 2D con Swing,
nessun motore grafico esterno.

## Avvio

Nessuna dipendenza esterna, nessun build tool — compilazione diretta con `javac`.

- Entry point: `src/main/java/jsonic/controller/Main.java`
- Sorgenti sotto `src/main/java/jsonic/`
- Risorse (sprite, mappe, audio) sotto `src/main/resources/res/`, lette dal classpath

```
javac -d out $(find src/main/java -name "*.java")
cp -r src/main/resources/res out/res
java -cp out jsonic.controller.Main
```

(un IDE che riconosce la convenzione Maven, come l'estensione Java di VS Code, compila e avvia
il progetto senza bisogno di questi comandi)

### Jar eseguibile

`SonicJava.jar`, nella root del repository, è già pronto all'uso: `java -jar SonicJava.jar`.
Compilato con l'overlay di debug disattivato (`DEBUG_ENABLED = false`, si veda sotto).

Nessuna dipendenza nativa (solo Swing/AWT standard): un unico jar gira invariato su Linux,
Windows e macOS, senza bisogno di ricompilarlo per ciascun sistema — basta un JRE/JDK 17+
installato su chi lo esegue. Per ricostruirlo da sorgente:

```
javac --release 17 -d out $(find src/main/java -name "*.java")
cp -r src/main/resources/res out/res
echo "Main-Class: jsonic.controller.Main" > manifest.txt
jar cfm SonicJava.jar manifest.txt -C out .
```

Poi, su qualunque sistema operativo: `java -jar SonicJava.jar`

(`--release 17` fissa la versione minima di Java richiesta per l'esecuzione a una LTS diffusa,
indipendentemente dalla versione di JDK usata per compilare)

Per una build senza l'overlay di debug (tasto T, vedi tabella comandi), imposta
`DEBUG_ENABLED = false` in `jsonic.utils.GameConstants` prima di compilare.

## Comandi

| Tasto | Azione |
|---|---|
| Frecce / WASD | Movimento |
| Spazio | Salto |
| Invio | Conferma / avvia |
| Esc | Pausa |
| F11 | Schermo intero |
| Su / Giù (nel menu) | Seleziona livello |
| M (nel menu) | Attiva/disattiva l'audio |
| T (in gioco) | Overlay di debug fisica (hitbox, sensori, angoli) |

## Struttura

Architettura MVC, package radice `jsonic` (`src/main/java/jsonic/`):

- `model/` — stato di gioco, nessuna dipendenza da Swing
  - `entity/` — `Player` (fisica, stati)
  - `enemy/` — badnik (`Motobug`, `BuzzBomber`, `Chopper`)
  - `item/` — oggetti raccoglibili/interattivi (`Ring`, `Spike`, `Goal`, ponti, fiori, proiettili)
  - `tile/` — griglia di collisione (`TileMap`), tipi di tile e loro fisica (`TileType`)
  - `physics/` — `IPhysicsWorld` (contratto tra `Player`/nemici e il mondo), `LoopRegion`
  - `world/` — `Level`: carica la mappa, orchestra collisioni/spawn/punteggio/stato del livello
- `view/` — rendering (Swing/`Graphics2D`), un renderer per responsabilità
  (`PlayerRenderer`, `EnemyRenderer`, `ItemRenderer`, `TileRenderer`, `ParallaxRenderer`,
  `HUDRenderer`, `TitleRenderer`, ...)
  - `itemview/` / `enemyview/` — sprite e animazione per ogni tipo di oggetto/nemico
  - `audio/` — `AudioManager`
  - `snapshot/` — uno snapshot immutabile per stato di gioco, costruito dal Controller e disegnato dalla View
- `controller/` — `GameEngine` (entry point/game loop), facciate Singleton verso Model e View
  (`ControllerForModel`/`ControllerForView`), macchina a stati (`MenuState`/`PlayState`/...), input, camera
- `utils/` — costanti di gioco e registro dei livelli (`GameConstants`, `LevelConfig`, `LevelRegistry`)

## Livelli

Ogni livello ha una sua cartella sotto `res/levels/<nome>/` con tutto il necessario: mappa
(CSV + progetto Tiled), sfondo, cartella `backgrounds/` per il parallax, splash card d'ingresso,
card di fine livello e musica. Aggiungerne uno nuovo richiede solo una cartella e una voce in
`LevelRegistry`, nessuna modifica al motore.

Le mappe sono editate in [Tiled](https://www.mapeditor.org/) ed esportate in CSV; ogni valore
della griglia è un `TileID` (pendenza, loop, spawn di anelli/nemici/goal, ...) — vedi
`jsonic/model/tile/TileID.java`. `LEVEL_TEST` è un livello puramente di verifica fisica
(ogni pendenza, un loop completo, un semi-loop), non un livello di gioco vero e proprio.

## Crediti

Sprite e font tratti da [The Spriters Resource](https://www.spriters-resource.com/) e
[The Sounds Resource](https://www.sounds-resource.com/) — credito a Sonic Team/SEGA e ai
rispettivi ripper. Fisica del personaggio basata sulla
[Sonic Physics Guide](https://info.sonicretro.org/Sonic_Physics_Guide). Progetto realizzato a
scopo didattico, non commerciale.
