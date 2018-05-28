package org.esa.s3tbx.olci.o2corr;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.json.simple.parser.ParseException;
import smile.neighbor.KDTree;

import java.awt.*;
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
            label = "Name of DEM altitude band")
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

    private TiePointGrid szaBand;
    private TiePointGrid ozaBand;
    private TiePointGrid slpBand;
    
    private RasterDataNode altitudeBand;
    private Band detectorIndexBand;

    private Band[] radianceBands;
    private Band[] cwlBands;
    private Band[] fwhmBands;
    private Band[] solarFluxBands;

    private KDTree<double[]>[] desmileKdTrees;
    private DesmileLut[] desmileLuts;

    @Override
    public void initialize() throws OperatorException {
        validateSourceProduct();

        lastBandToProcess = processOnlyBand13 ? 13 : 15;
        numBandsToProcess = lastBandToProcess - 13 + 1;

        szaBand = l1bProduct.getTiePointGrid("SZA");
        ozaBand = l1bProduct.getTiePointGrid("OZA");
        slpBand = l1bProduct.getTiePointGrid("sea_level_pressure");
        detectorIndexBand = l1bProduct.getBand("detector_index");

        if (demProduct != null) {
            altitudeBand = demProduct.getRasterDataNode(demAltitudeBandName);
        } else {
            altitudeBand = l1bProduct.getBand("altitude");
        }

        radianceBands = new Band[5];
        cwlBands = new Band[5];
        fwhmBands = new Band[5];
        solarFluxBands = new Band[5];
        for (int i = 12; i < 17; i++) {    // todo: optimize, consider numBandsToProcess
            radianceBands[i - 12] = l1bProduct.getBand("Oa" + i + "_radiance");
            cwlBands[i - 12] = l1bProduct.getBand("lambda0_band_" + i);
            fwhmBands[i - 12] = l1bProduct.getBand("FWHM_band_" + i);
            solarFluxBands[i - 12] = l1bProduct.getBand("solar_flux_band_" + i);
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
        for (int i = 13; i <= lastBandToProcess; i++) {
            desmileLuts[i-13] = O2CorrOlciIO.createDesmileLut(i);
            desmileKdTrees[i-13] = O2CorrOlciIO.createKDTreeForDesmileInterpolation(desmileLuts[i-13]);
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

        for (int i = 13; i <= lastBandToProcess; i++) {
            targetProduct.addBand("trans_" + i, ProductData.TYPE_FLOAT32);
            // todo: add pressure, surface, corrected radiance
        }

        for (int i = 0; i < targetProduct.getNumBands(); i++) {
            targetProduct.getBandAt(i).setNoDataValue(Float.NaN);
            targetProduct.getBandAt(i).setNoDataValueUsed(true);
        }
        ProductUtils.copyTiePointGrids(l1bProduct, targetProduct);
        ProductUtils.copyGeoCoding(l1bProduct, targetProduct);

        setTargetProduct(targetProduct);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle targetRectangle = targetTile.getRectangle();
        String targetBandName = targetBand.getName();

        Tile szaTile = getSourceTile(szaBand, targetRectangle);
        Tile ozaTile = getSourceTile(ozaBand, targetRectangle);
        Tile altitudeTile = getSourceTile(altitudeBand, targetRectangle);
        Tile slpTile = getSourceTile(slpBand, targetRectangle);
        Tile detectorIndexTile = getSourceTile(detectorIndexBand, targetRectangle);
        Tile l1FlagsTile = getSourceTile(l1bProduct.getRasterDataNode("quality_flags"), targetRectangle);

        Tile[] radianceTiles = new Tile[5];
        Tile[] cwlTiles = new Tile[5];
        Tile[] fwhmTiles = new Tile[5];
        Tile[] solarFluxTiles = new Tile[5];
        for (int i = 0; i < 5; i++) {
            radianceTiles[i] = getSourceTile(radianceBands[i], targetRectangle);
            cwlTiles[i] = getSourceTile(cwlBands[i], targetRectangle);
            fwhmTiles[i] = getSourceTile(fwhmBands[i], targetRectangle);
            solarFluxTiles[i] = getSourceTile(solarFluxBands[i], targetRectangle);
        }

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                final boolean pixelIsValid = !l1FlagsTile.getSampleBit(x, y, O2CorrOlciConstants.OLCI_INVALID_BIT);
                if (pixelIsValid) {
                    // Preparing input data...

                    final float sza = szaTile.getSampleFloat(x, y);
                    final float oza = ozaTile.getSampleFloat(x, y);
                    final float altitude = altitudeTile.getSampleFloat(x, y);
                    final float slp = slpTile.getSampleFloat(x, y);
                    final float detectorIndex = detectorIndexTile.getSampleFloat(x, y);

                    final float amf = (float) (1.0 / Math.cos(sza * MathUtils.DTOR) + 1.0 / Math.cos(oza * MathUtils.DTOR));

                    float[] radiance = new float[5];
                    float[] r = new float[5];
                    float[] cwl = new float[5];
                    float[] fwhm = new float[5];
                    float[] solarFlux = new float[5];
                    for (int i = 0; i < 5; i++) {    // 12, 13, 14, 15, 16
                        radiance[i] = radianceTiles[i].getSampleFloat(x, y);
                        cwl[i] = cwlTiles[i].getSampleFloat(x, y);
                        fwhm[i] = fwhmTiles[i].getSampleFloat(x, y);
                        solarFlux[i] = solarFluxTiles[i].getSampleFloat(x, y);
                        r[i] = radiance[i] / solarFlux[i];
                    }

                    final float dlam = cwl[4] - cwl[0];
                    final float drad = r[4] - r[0];
                    float[] trans = new float[5];
                    float[] radianceAbsFree = new float[5];
//                    float[] tauAmf = new float[3];
                    for (int i = 0; i < 3; i++) {   // 13, 14, 15 !!
                        if (dlam > 0.0001) {
                            final float grad = drad / dlam;
                            radianceAbsFree[i+1] = r[0] + grad * (cwl[i+1] - cwl[0]);
                        } else {
                            radianceAbsFree[i+1] = Float.NaN;
                        }
                        trans[i+1] = r[i+1] / radianceAbsFree[i+1];
                        cwl[i+1] += O2CorrOlciAlgorithm.overcorrectLambda(detectorIndex,
                                                                        O2CorrOlciConstants.DWL_CORR_OFFSET[i]);
                    }

                    // Processing data...
                    for (int i = 0; i < numBandsToProcess; i++) {   // 13, 14, 15
                        final double dwl = cwl[i+1] - O2CorrOlciConstants.cwvl[i];
                        final float transDesmiled = (float) O2CorrOlciAlgorithm.desmileTransmission(dwl, fwhm[i+1],
                                                                                                    amf,
                                                                                                    trans[i+1],
                                                                                                    desmileKdTrees[i],
                                                                                                    desmileLuts[i]);
                        final float transDesmiledRectified =
                                (float) O2CorrOlciAlgorithm.rectifyDesmiledTransmission(transDesmiled, amf, i + 13);

                        if (targetBandName.startsWith("trans")) {
                            targetTile.setSample(x, y, transDesmiledRectified);
                        } else if (targetBandName.startsWith("press")) {
                            // todo
                        } else if (targetBandName.startsWith("surface")) {
                            // todo
                        } else {
                            // radiance
                            float correctedRadiance = radianceAbsFree[i+1] * solarFlux[i+1] * transDesmiledRectified;
                            targetTile.setSample(x, y, correctedRadiance);
                        }
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
