package jsonic.model.tile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import jsonic.utils.GameConstants;
import jsonic.model.physics.IPhysicsWorld.SensorDirection;
import jsonic.model.physics.IPhysicsWorld.SensorResult;

/** Owns the level's tile grid (loaded from a CSV) and answers every terrain query Player needs: surface/ceiling/wall lookups and sensor casts, via IPhysicsWorld. */
public class TileMap {

    private final int[][] mapTileNum;
    private final String mapCsvPath;

    private boolean debugMode = false;

    public TileMap(String mapCsvPath) {
        this.mapCsvPath = mapCsvPath;
        this.mapTileNum = new int[GameConstants.MAX_WORLD_COL][GameConstants.MAX_WORLD_ROW];
        // int[][] defaults to 0 (TileID.FULL, solid) - fill with AIR first so a CSV smaller
        // than the full grid leaves the uncovered area open instead of silently solid
        for (int[] column : mapTileNum) java.util.Arrays.fill(column, TileID.AIR);
        loadMap(mapCsvPath);
    }

    // loading and reloading
    private void loadMap(String filePath) {
        try (InputStream is = getClass().getResourceAsStream(filePath)) {
            if (is == null) {
                System.err.println("TileMap: map resource not found: " + filePath);
                return;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            for (int row = 0; row < GameConstants.MAX_WORLD_ROW; row++) {
                String line = br.readLine();
                if (line == null)
                    break;

                if (line.trim().isEmpty()) {
                    // Ignore blank lines in CSV (tolerate trailing newline)
                    row--; // retry filling this row with the next non-empty line
                    continue;
                }

                String[] tokens = line.split(",");
                for (int col = 0; col < GameConstants.MAX_WORLD_COL; col++) {
                    if (col < tokens.length) {
                        String tok = tokens[col].trim();
                        if (tok.isEmpty())
                            continue; // skip empty cells
                        mapTileNum[col][row] = Integer.parseInt(tok);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("TileMap: failed to load map: " + filePath);
        }
    }

    public void reload() {
        loadMap(mapCsvPath);
    }

    // grid lookup

    public int getTileAt(int col, int row) {
        if (isOutOfBounds(col, row))
            return TileID.AIR;
        return mapTileNum[col][row];
    }

    public void setTileAt(int col, int row, int tileID) {
        if (isOutOfBounds(col, row))
            return;
        mapTileNum[col][row] = tileID;
    }

    public int getTileIdAt(int worldX, int worldY) {
        int col = worldX / GameConstants.TILE_SIZE;
        int row = worldY / GameConstants.TILE_SIZE;
        return getTileAt(col, row);
    }

    private boolean isOutOfBounds(int col, int row) {
        return col < 0 || col >= GameConstants.MAX_WORLD_COL ||
                row < 0 || row >= GameConstants.MAX_WORLD_ROW;
    }

    // physics api
    public int getSurfaceY(int worldX, int worldY) {
        int tileID = getTileIdAt(worldX, worldY);
        if (tileID == TileID.AIR)
            return TileID.NO_SURFACE;

        int tileY = (worldY / GameConstants.TILE_SIZE) * GameConstants.TILE_SIZE;

        switch (tileID) {
            case TileID.FULL:
                return tileY;
            case TileID.THREE_FOUR:
            case TileID.HALF:
            case TileID.ONE_FOUR:
                return tileY + TileType.getSolidTopOffset(tileID);
        }

        int[] hm = TileType.getHeightmap(tileID);
        if (hm == null)
            return TileID.NO_SURFACE;

        return tileY + hm[resolveLocalX(tileID, worldX)];
    }

    // Column within the tile's own heightmap for a given world X. 22.5° slopes share their
    // heightmap across a pair of tiles (see TileType), so worldX must first be halved into
    // that pair's own local space; every other sloped tile just wraps to its own width.
    private int resolveLocalX(int tileID, int worldX) {
        int localX;
        if (tileID == TileID.SLOPE_R_22 || tileID == TileID.SLOPE_L_22) {
            localX = (worldX % (GameConstants.TILE_SIZE * 2)) / 2;
        } else {
            localX = worldX % GameConstants.TILE_SIZE;
        }
        return Math.max(0, Math.min(GameConstants.TILE_SIZE - 1, localX));
    }

    // true if a FULL tile is a lone one-row platform (air above AND below) - checking only "air
    // above" would also match the top tile of a taller stack, which is still solid underneath
    private boolean isOneRowThickFull(int worldX, int tileY) {
        return getTileIdAt(worldX, tileY - GameConstants.TILE_SIZE) == TileID.AIR
            && getTileIdAt(worldX, tileY + GameConstants.TILE_SIZE) == TileID.AIR;
    }

    public int getCeilingSurfaceY(int worldX, int worldY) {
        int tileID = getTileIdAt(worldX, worldY);
        if (tileID == TileID.AIR)
            return TileID.NO_SURFACE;

        int tileY = (worldY / GameConstants.TILE_SIZE) * GameConstants.TILE_SIZE;

        // One-way platforms (THREE_FOUR/HALF/ONE_FOUR, or a one-row-thick FULL) are solid for
        // landing only, never as a ceiling from below.
        if (tileID == TileID.FULL) {
            if (isOneRowThickFull(worldX, tileY)) return TileID.NO_SURFACE;
            return tileY + GameConstants.TILE_SIZE - 1;
        }

        int[] hm = TileType.getHeightmap(tileID);
        if (hm == null)
            return TileID.NO_SURFACE;

        return tileY + hm[resolveLocalX(tileID, worldX)];
    }

    public SensorResult castSensor(
            int tipX, int tipY, SensorDirection dir,
            int lookBack, int lookFwd) {

        boolean vertical = (dir == SensorDirection.DOWN || dir == SensorDirection.UP);
        // sign: advancement direction of the sensor along the cast axis
        int sign = (dir == SensorDirection.DOWN || dir == SensorDirection.RIGHT) ? +1 : -1;

        int fixed = vertical ? tipX : tipY; // axis perpendicular to the cast
        int tip = vertical ? tipY : tipX;
        int winLo = Math.min(tip + sign * (-lookBack), tip + sign * lookFwd);
        int winHi = Math.max(tip + sign * (-lookBack), tip + sign * lookFwd);

        int bestSurface = TileID.NO_SURFACE;
        float bestAngle = 0f;
        int bestProj = Integer.MAX_VALUE; // min(sign*surface) = surface hit first

        int cellLo = winLo / GameConstants.TILE_SIZE;
        int cellHi = winHi / GameConstants.TILE_SIZE;

        for (int cell = cellLo; cell <= cellHi; cell++) {
            int probe = cell * GameConstants.TILE_SIZE;

            int surface;
            int tileID;
            if (vertical) {
                surface = (sign > 0) ? getSurfaceY(fixed, probe)
                        : getCeilingSurfaceY(fixed, probe);
                tileID = getTileIdAt(fixed, probe);
            } else {
                // castRight==true: look for the left face of the wall to the right
                surface = getSurfaceX(probe, fixed, sign > 0);
                tileID = getTileIdAt(probe, fixed);
            }

            if (surface == TileID.NO_SURFACE)
                continue;
            if (surface < winLo || surface > winHi)
                continue;

            int proj = sign * surface; // smaller = hit first along dir
            if (proj < bestProj) {
                bestProj = proj;
                bestSurface = surface;
                bestAngle = TileType.getAngle(tileID);
            }
        }

        if (bestSurface == TileID.NO_SURFACE)
            return SensorResult.NONE;
        return new SensorResult(true, bestSurface, bestAngle);
    }

    public int getSurfaceX(int worldX, int worldY, boolean castRight) {
        int tileID = getTileIdAt(worldX, worldY);
        if (tileID == TileID.AIR)
            return TileID.NO_SURFACE;

        int tileX = (worldX / GameConstants.TILE_SIZE) * GameConstants.TILE_SIZE;
        int tileY = (worldY / GameConstants.TILE_SIZE) * GameConstants.TILE_SIZE;
        int localY = worldY % GameConstants.TILE_SIZE;

        // A one-row-thick FULL tile has no side collision either, same jump-through reasoning.
        if (tileID == TileID.FULL) {
            if (isOneRowThickFull(worldX, tileY)) return TileID.NO_SURFACE;
            return castRight ? tileX : tileX + GameConstants.TILE_SIZE - 1;
        }

        // Solid on the side only in the bottom fraction (a step to climb); fully transparent if there's air below.
        if (tileID == TileID.THREE_FOUR || tileID == TileID.HALF || tileID == TileID.ONE_FOUR) {
            if (getTileIdAt(worldX, tileY + GameConstants.TILE_SIZE) == TileID.AIR) return TileID.NO_SURFACE;
            if (localY < TileType.getSolidTopOffset(tileID)) return TileID.NO_SURFACE;
            return castRight ? tileX : tileX + GameConstants.TILE_SIZE - 1;
        }

        int[] hm = TileType.getHeightmap(tileID);
        if (hm == null)
            return TileID.NO_SURFACE;

        if (castRight) {
            for (int lx = 0; lx < GameConstants.TILE_SIZE; lx++) {
                if (localY >= hm[lx])
                    return tileX + lx;
            }
        } else {
            for (int lx = GameConstants.TILE_SIZE - 1; lx >= 0; lx--) {
                if (localY >= hm[lx])
                    return tileX + lx;
            }
        }
        return TileID.NO_SURFACE;
    }

    // debug
    public void toggleDebugMode() {
        debugMode = !debugMode;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    // getters
    public int[][] getMapTileNum() {
        return mapTileNum;
    }
}
