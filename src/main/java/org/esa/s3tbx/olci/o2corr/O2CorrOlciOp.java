package org.esa.s3tbx.olci.o2corr;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;

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
    private String demAltitude;

    @Parameter(defaultValue = "true",
            description = "If set to true, only band 13 will be processed, otherwise bands 13-15.",
            label = "Only process OLCI band 13 (761.25 nm)")
    private boolean processOnlyBand13;

    @Parameter(defaultValue = "false",
            label = "Write corrected radiances",
            description = "If set to true, corrected radiances of processed band(s) will be written to target product.")
    private boolean writeCorrectedRadiances;


    @Override
    public void initialize() throws OperatorException {

    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(O2CorrOlciOp.class);
        }
    }
}
