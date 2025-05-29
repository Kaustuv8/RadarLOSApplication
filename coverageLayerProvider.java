package helperfn;
import classes.tileClass.Tile;
import org.geotools.api.style.Style;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.GridCoverageLayer;
import org.geotools.api.style.ColorMapEntry;
import org.geotools.api.style.ColorMap;
import org.geotools.api.style.RasterSymbolizer;
import org.geotools.styling.ColorMapEntryImpl;
import org.geotools.styling.ColorMapImpl;
import org.geotools.styling.StyleBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import javax.media.jai.RasterFactory;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;

import java.awt.Color;

public class coverageLayerProvider {

   public GridCoverageLayer giveCoverageLayer(
    Tile tile, 
    double radarLat, 
    double radarLong, 
    double radarHeight, 
    Tile tileForLOS // Only uses single tile
) {
    float[] radarData = new float[tile.width * tile.height];

    int centerX = tile.width / 2;
    int centerY = tile.height / 2;
    int radius = Math.min(tile.width, tile.height) / 3;

    for (int y = 0; y < tile.height; y++) {
        for (int x = 0; x < tile.width; x++) {
            int dx = x - centerX;
            int dy = y - centerY;

            // Only within circle
            if (dx * dx + dy * dy <= radius * radius) {
                double targetLat = tile.originLat - y * 0.001;
                double targetLon = tile.originLon + x * 0.001;

                int index = y * tile.width + x;
                int targetHeight = tile.data[index];

                boolean blocked = Losfn.isLOSBlocked(
                    radarLat, radarLong, radarHeight,
                    targetLat, targetLon, targetHeight,
                    tileForLOS
                );

                radarData[y * tile.width + x] = blocked ? 0.5f : 1f; // 0.5 = blocked, 1.0 = visible
            } else {
                radarData[y * tile.width + x] = 0f; // outside range
            }
        }
    }

    // Raster + Image
    WritableRaster radarRaster = RasterFactory.createBandedRaster(
        DataBuffer.TYPE_FLOAT, tile.width, tile.height, 1, null
    );
    for (int y = 0; y < tile.height; y++) {
        for (int x = 0; x < tile.width; x++) {
            radarRaster.setSample(x, y, 0, radarData[y * tile.width + x]);
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
    GridCoverage2D radarCoverage = factory.create("RadarCoverage", radarRaster, envelope);

    // STYLE
    StyleBuilder sb = new StyleBuilder();
    RasterSymbolizer radarSym = sb.createRasterSymbolizer();

    ColorMapEntry noCoverage = new ColorMapEntryImpl();
    noCoverage.setColor(sb.literalExpression(Color.BLACK));
    noCoverage.setQuantity(sb.literalExpression(0.0));
    noCoverage.setOpacity(sb.literalExpression(0.0f));
    noCoverage.setLabel("No Coverage");

    ColorMapEntry losBlocked = new ColorMapEntryImpl();
    losBlocked.setColor(sb.literalExpression(Color.RED)); // Red
    losBlocked.setQuantity(sb.literalExpression(0.5));
    losBlocked.setOpacity(sb.literalExpression(0.8f));
    losBlocked.setLabel("LOS Blocked");

    ColorMapEntry losVisible = new ColorMapEntryImpl();
    losVisible.setColor(sb.literalExpression(Color.GREEN)); // Green
    losVisible.setQuantity(sb.literalExpression(1.0));
    losVisible.setOpacity(sb.literalExpression(0.8f));
    losVisible.setLabel("LOS Clear");

    ColorMapImpl radarColorMap = new ColorMapImpl();
    radarColorMap.addColorMapEntry(noCoverage);
    radarColorMap.addColorMapEntry(losBlocked);
    radarColorMap.addColorMapEntry(losVisible);
    
    radarColorMap.setType(ColorMap.TYPE_VALUES);

    radarSym.setColorMap(radarColorMap);
    Style radarStyle = sb.createStyle(radarSym);

    return new GridCoverageLayer(radarCoverage, radarStyle);
}

}
