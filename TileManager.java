package helperfn;
import java.io.*;
import java.util.*;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;

import classes.tileClass.Tile;

public class TileManager {
   

    // Cache loaded tiles to avoid repeated disk reads (optional)
    private final Map<String, Tile> tileCache = new HashMap<>();

    public TileManager(String tileDirectory) {
        
    }

    /**
     * Get tile containing the given lat, lon.
     * @param lat Latitude in decimal degrees
     * @param lon Longitude in decimal degrees
     * @return Tile object or null if not found
     */


    public static Tile TileFromFile(File F){
        // Register GDAL drivers
        gdal.AllRegister();
        gdal.PushErrorHandler("CPLQuietErrorHandler");  
        Dataset dataset = gdal.Open(F.getAbsolutePath(), gdalconstConstants.GA_ReadOnly);
        if(dataset == null){
            System.err.println("Failed to open " + F.getAbsolutePath());
            return null;
        }
        double[] geoTransform = dataset.GetGeoTransform();
        // Get raster band (assuming single band for DT2)
        Band band = dataset.GetRasterBand(1);
        int width = band.getXSize();
        int height = band.getYSize();
        

        // Read raster data
        int[] rasterData = new int[width * height];
        band.ReadRaster(0, 0, width, height, rasterData);
        double originLon = geoTransform[0];
        double originLat = geoTransform[3];
        //Create Tile
        Tile toBeReturned =  new Tile(F.getName(), rasterData, originLat, originLon, width, height);
        // Close dataset
        dataset.delete();
        //Return the tile
        return toBeReturned;
    }

    public List<Tile> getSurroundingTiles(double lat, double lon) {
        List<Tile> tiles = new ArrayList<>();

        int centerLat = (int) Math.floor(lat);
        int centerLon = (int) Math.floor(lon);

        // Loop over -1, 0, +1 offset in lat and lon to get 3x3 grid
        for (int dLat = -1; dLat <= 1; dLat++) {
            for (int dLon = -1; dLon <= 1; dLon++) {
                int neighborLat = centerLat + dLat;
                int neighborLon = centerLon + dLon;
                Tile neighborTile = getTile(neighborLat+0.5, neighborLon+0.5); // getTile works with center of tile
                if (neighborTile != null) {
                    tiles.add(neighborTile);
                }
            }
        }

        return tiles;
    }




    public Tile getTile(double lat, double lon) {
        // Determine tile origin coordinates (integer degree)
        int tileLat = (int) Math.floor(lat);
        int tileLon = (int) Math.floor(lon);

        // Build filename based on tile origin
        String filename = buildTileFileName(tileLat, tileLon);
        filename = "data\\terrainData\\".concat(filename);
        // Check cache first
        if (tileCache.containsKey(filename)) {
            return tileCache.get(filename);
        }

        // Load tile from disk
        Tile tile = TileFromFile(new File(filename));
        if (tile != null) {
            tileCache.put(filename, tile);
        }
        return tile;
    }

    /**
     * Construct filename from tile origin.
     * Example: lat=9, lon=76 => "n09_e076_1arc_v3.pkl"
     */
    private String buildTileFileName(int lat, int lon) {
        // Latitude prefix
        String latPrefix = (lat >= 0) ? "n" : "s";
        int latAbs = Math.abs(lat);

        // Longitude prefix
        String lonPrefix = (lon >= 0) ? "e" : "w";
        int lonAbs = Math.abs(lon);

        // Format with leading zeros to 2 or 3 digits as needed
        String latStr = String.format("%s%02d", latPrefix, latAbs);
        String lonStr = String.format("%s%03d", lonPrefix, lonAbs);

        // Adjust filename pattern if different
        String filename = latStr + "_" + lonStr + "_1arc_v3.dt2";

        return filename;
    }

    
    


    public int getElevation(double lat, double lon) {
        Tile tile = getTile(lat, lon);
        if (tile == null) {
            return Integer.MIN_VALUE; // or some nodata value
        }

        // Calculate pixel indices inside tile
        // Tile origin (tile.originLat, tile.originLon) is bottom-left or top-left? 
        // Assuming originLat/originLon are bottom-left corner of tile
        double latDiff = lat - tile.originLat; // degrees difference in latitude
        double lonDiff = lon - tile.originLon; // degrees difference in longitude

        // Each tile covers 1 degree with 3601 pixels
        double pixelSize = 1.0 / (tile.width - 1); // degrees per pixel

        int row = (int) ((1.0 - latDiff) / pixelSize); // Flip latitude: top-left origin
        int col = (int) (lonDiff / pixelSize);

        // Check bounds
        if (row < 0 || row >= tile.height || col < 0 || col >= tile.width) {
            return Integer.MIN_VALUE; // outside tile
        }

        // Index in data array (row-major order)
        int idx = row * tile.width + col;

        return tile.data[idx];
    }

    



    // Test example
    public static void main(String[] args) {
        String tileFolder = "data\\terrainData";
        TileManager manager = new TileManager(tileFolder);

        double lat = 9.5;
        double lon = 76.3;

        List<Tile> tiles = manager.getSurroundingTiles(lat, lon);
        System.out.println("Loaded " + tiles.size() + " surrounding tiles:");
        for (Tile tile : tiles) {
            System.out.println(" - " + tile.fileName);
        }
    }
}
