package org.esa.s3tbx.olci.o2corr;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import smile.neighbor.KDTree;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * The O2corr GPF operator for OLCI L1b (Version v3).
 * Authors: R.Preusker (Python breadboard), O.Danne (Java onversion), May 2018
 * <p/>
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "O2CorrOlci", version = "0.8",
        authors = "R.Preusker, O.Danne",
        category = "Optical/Preprocessing",
        copyright = "Copyright (C) 2018 by Brockmann Consult",
        description = "Performs O2 correction on OLCI L1b product.")
public class O2CorrOlciOp extends Operator {

    @SourceProduct(description = "OLCI L1b product",
            label = "OLCI L1b product")
    private Product l1bProduct;

    @SourceProduct(description = "DEM product",
            optional = true,
            label = "Optional DEM product")
    private Product demProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "Name of altitude band in optional DEM file. Altitude is expected in meters.",
            label="Name of DEM altitude band")
    private String demAltitudeBandName;

    @Parameter(defaultValue = "true",
            description = "If set to true, only band 13 will be processed, otherwise bands 13-15.",
            label = "Only process OLCI band 13 (761.25 nm)")
    private boolean processOnlyBand13;

    @Parameter(defaultValue = "false",
            label = "Write corrected radiances",
            description = "If set to true, corrected radiances of processed band(s) will be written to target product.")
    private boolean writeCorrectedRadiances;

    private int lastBandToProcess;
    private int numBandsToProcess;

    private Band szaBand;
    private Band ozaBand;
    private Band altitudeBand;
    private Band slpBand;
    private Band detectorIndexBand;

    private Band[] radianceBands;
    private Band[] lambda0Bands;
    private Band[] fwhmBands;
    private Band[] solarFluxBands;

    private KDTree<double[]>[] desmileKdTrees;
    private DesmileLut[] desmileLuts;

    @Override
    public void initialize() throws OperatorException {
        validateSourceProduct();

        lastBandToProcess = processOnlyBand13 ? 13 : 15;
        numBandsToProcess = lastBandToProcess - 13;

        szaBand = l1bProduct.getBand("SZA");
        ozaBand = l1bProduct.getBand("OZA");
        slpBand = l1bProduct.getBand("sea_level_pressure");
        detectorIndexBand = l1bProduct.getBand("detector_index");

        if (demProduct != null) {
            altitudeBand = demProduct.getBand(demAltitudeBandName);
        } else {
            altitudeBand = l1bProduct.getBand("altitude");
        }

        radianceBands = new Band[5];
        lambda0Bands = new Band[5];
        fwhmBands = new Band[5];
        solarFluxBands = new Band[5];
        for (int i = 12; i < 17; i++) {
            radianceBands[i-12] = l1bProduct.getBand("Oa_" + i + "_radiance");
            lambda0Bands[i-12] = l1bProduct.getBand("lambda0_band_" + i);
            fwhmBands[i-12] = l1bProduct.getBand("FWHM_band_" + i);
            solarFluxBands[i-12] = l1bProduct.getBand("solar_flux_band_" + i);
        }

        createTargetProduct();

        try {
            initDesmileAuxdata();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            throw new OperatorException("Cannit initialize auxdata for desmile of transmissions - exiting.");
        }

    }

    private void initDesmileAuxdata() throws IOException, ParseException {
        desmileLuts = new DesmileLut[numBandsToProcess];
        desmileKdTrees = new KDTree[numBandsToProcess];
        for (int i = 13; i < lastBandToProcess; i++) {
            desmileLuts[i] =  O2CorrOlciIO.createDesmileLut(i);
            desmileKdTrees[i] =  O2CorrOlciIO.createKDTreeForDesmileInterpolation(desmileLuts[i]);
        }
    }

    private void validateSourceProduct() {
        // todo
    }

    private void createTargetProduct() {
        targetProduct = new Product("O2CORR", "O2CORR",
                                    l1bProduct.getSceneRasterWidth(), l1bProduct.getSceneRasterHeight());
        targetProduct.setDescription("O2 correction product");
        targetProduct.setStartTime(l1bProduct.getStartTime());
        targetProduct.setEndTime(l1bProduct.getEndTime());

        for (int i = 13; i < lastBandToProcess; i++) {
            targetProduct.addBand("trans_" + i, ProductData.TYPE_FLOAT32);
            // todo: add pressure, surface, corrected radiance
        }

        for (int i = 0; i < targetProduct.getNumBands(); i++) {
            targetProduct.getBandAt(i).setNoDataValue(Float.NaN);
            targetProduct.getBandAt(i).setNoDataValueUsed(true);
        }
        ProductUtils.copyTiePointGrids(l1bProduct, targetProduct);
        ProductUtils.copyGeoCoding(l1bProduct, targetProduct);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle targetRectangle = targetTile.getRectangle();
        String bandName = targetBand.getName();

        Tile szaTile = getSourceTile(szaBand, targetRectangle);
        Tile ozaTile = getSourceTile(ozaBand, targetRectangle);
        Tile altitudeTile = getSourceTile(altitudeBand, targetRectangle);
        Tile slpTile = getSourceTile(slpBand, targetRectangle);
        Tile detectorIndexTile = getSourceTile(detectorIndexBand, targetRectangle);
        Tile l1FlagsTile = getSourceTile(l1bProduct.getRasterDataNode("quality_flags"), targetRectangle);

        Tile[] radianceTiles = new Tile[5];
        Tile[] lambda0Tiles = new Tile[5];
        Tile[] fwhmTiles = new Tile[5];
        Tile[] solarFluxTiles = new Tile[5];
        for (int i = 0; i < 5; i++) {
            radianceTiles[i] = getSourceTile(radianceBands[i], targetRectangle);
            lambda0Tiles[i] = getSourceTile(lambda0Bands[i], targetRectangle);
            fwhmTiles[i] = getSourceTile(fwhmBands[i], targetRectangle);
            solarFluxTiles[i] = getSourceTile(solarFluxBands[i], targetRectangle);
        }

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                final boolean pixelIsValid = !l1FlagsTile.getSampleBit(x, y, O2CorrOlciConstants.OLCI_INVALID_BIT);
                if (pixelIsValid) {
                    final float sza = szaTile.getSampleFloat(x, y);
                    final float oza = ozaTile.getSampleFloat(x, y);
                    final float altitude = altitudeTile.getSampleFloat(x, y);
                    final float slp = slpTile.getSampleFloat(x, y);
                    final float detectorIndex = detectorIndexTile.getSampleFloat(x, y);

                    float[] radiance = new float[5];
                    float[] lambda0 = new float[5];
                    float[] fwhm = new float[5];
                    float[] solarFlux = new float[5];
                    for (int i = 12; i < 17; i++) {
                        radiance[i] = radianceTiles[i].getSampleFloat(x, y);
                        lambda0[i] = lambda0Tiles[i].getSampleFloat(x, y);
                        fwhm[i] = fwhmTiles[i].getSampleFloat(x, y);
                        solarFlux[i] = solarFluxTiles[i].getSampleFloat(x, y);
                    }
                }
            }
        }

    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(O2CorrOlciOp.class);
        }
    }
}
