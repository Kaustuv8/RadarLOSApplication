package helperfn;

import classes.tileClass.Tile;

public class Losfn {

    public static boolean isLOSBlocked(
        double lat1, double lon1, double height1,
        double lat2, double lon2, double height2,
        Tile tile
    ) {
        int samplePoints = 50;
        int hopSize = 10;

        for (int i = 1; i < samplePoints; i += hopSize) {
            double fraction = i / (double) samplePoints;

            double lat = lat1 + fraction * (lat2 - lat1);
            double lon = lon1 + fraction * (lon2 - lon1);
            double lineHeight = height1 + fraction * (height2 - height1);

            // Convert lat/lon to (x, y) in tile grid
            int x = (int) ((lon - tile.originLon) / 0.001);
            int y = (int) ((tile.originLat - lat) / 0.001);

            // Check bounds
            if (x < 0 || x >= tile.width || y < 0 || y >= tile.height) {
                continue; // Point outside tile
            }

            int index = y * tile.width + x;
            int terrainElevation = tile.data[index];

            if (terrainElevation > lineHeight) {
                return true; // LOS is blocked by terrain
            }
        }

        return false; // No obstruction found
    }
}
