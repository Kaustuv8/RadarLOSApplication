package helperfn;

import classes.tileClass.Tile;
import org.geotools.api.style.Style;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.MapContent;
import org.geotools.map.GridCoverageLayer;
import org.geotools.swing.JMapPane;


import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.awt.Color;

import org.geotools.styling.StyleBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.api.style.ColorMap;
import org.geotools.api.style.RasterSymbolizer;

import javax.media.jai.RasterFactory;
import javax.swing.JFrame;

public class map {

    private static String tileFolder = "data\\terrainData";

    private static TileManager manager = new TileManager(tileFolder);

    private static Style giveStyle(double min, double max){
        StyleBuilder sb = new StyleBuilder();

        // Colors from low to high elevation
        Color[] colors = new Color[] {
        new Color(255, 255, 255),  // White (very low)
        new Color(192, 192, 192),  // Light Gray
        new Color(128, 128, 128),  // Medium Gray
        new Color(255, 165, 0),    // Orange
        new Color(160, 82, 45),    // SaddleBrown
        new Color(101, 67, 33)     // Dark Brown
    };

        double[] values = new double[] {
            min,
            min + (max - min) * 0.2,
            min + (max - min) * 0.4,
            min + (max - min) * 0.6,
            min + (max - min) * 0.8,
            max
        };

        String[] labels = new String[] {
            "Very Low", "Low", "Mid", "High", "Very High", "Peak"
        };

        ColorMap colorMap = sb.createColorMap(labels, values, colors, ColorMap.TYPE_INTERVALS);

        RasterSymbolizer sym = sb.createRasterSymbolizer();
        sym.setColorMap(colorMap);
        return sb.createStyle(sym);
    }

    public int[] giveElevationData(double lat, double lon, double height){
        int ans[] = new int[2];
        Tile tile = manager.getTile(lat, lon);
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int val : tile.data) {
            if (val < min) min = val;
            if (val > max) max = val;
        }
        ans[0] = min;
        ans[1] = max;
        return ans;
    }

    public JMapPane giveMap(double lat, double lon, double height) {
        Tile tile = manager.getTile(lat, lon);
        if (tile == null) {
            System.err.println("Failed to load tile for lat=" + lat + ", lon=" + lon);
            return null;
        }

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int val : tile.data) {
            if (val < min) min = val;
            if (val > max) max = val;
        }
        if (min == max) max = min + 1;
        System.out.println("Elevation min: " + min + ", max: " + max);

        WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT, tile.width, tile.height, 1, null);

        // Use raw elevation values here (float version)
        for (int y = 0; y < tile.height; y++) {
            for (int x = 0; x < tile.width; x++) {
                float value = tile.data[y * tile.width + x];
                raster.setSample(x, y, 0, value);
            }
        }

        ReferencedEnvelope envelope = new ReferencedEnvelope(
                tile.originLon,
                tile.originLon + tile.width * 0.001,
                tile.originLat - tile.height * 0.001,
                tile.originLat,
                DefaultGeographicCRS.WGS84
        );

        GridCoverageFactory factory = new GridCoverageFactory();
        GridCoverage2D coverage = factory.create("Elevation", raster, envelope);

        MapContent map = new MapContent();
        map.setTitle("Elevation Heatmap");

        map.addLayer(new GridCoverageLayer(coverage, giveStyle(min, max)));
        coverageLayerProvider help = new coverageLayerProvider();
        map.addLayer(help.giveCoverageLayer(tile, lat, lon, height, tile));
        JMapPane mapPane = new JMapPane();
        mapPane.setMapContent(map);
        return mapPane;
    }


    public static void main(String[] args) {
        map mapper = new map();

        // Example coordinates and height
        double lat = 31;   // example latitude
        double lon = 85;   // example longitude
        double height = 500;    // example height

        JMapPane mapPane = mapper.giveMap(lat, lon, height);
        if (mapPane == null) {
            System.err.println("Map generation failed.");
            System.exit(1);
        }

        JFrame frame = new JFrame("Radar LOS Map Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(mapPane);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
