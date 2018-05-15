package org.esa.s3tbx.olci.o2corr;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import java.io.FileReader;

import static org.junit.Assert.*;

public class O2CorrOlciIOTest {

    @Test
    public void testParseJsonFile_correctionModelCoeff() throws Exception {
        final String jsonFile = getClass().getResource("O2_correction_model_coeff_13_amf.json").getFile();

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(new FileReader(jsonFile));

        final double cwvl = O2CorrOlciIO.parseJSONDouble(jsonObject, "cwvl");
        final double expectedCwvl = 761.726;
        assertEquals(expectedCwvl, cwvl, 1e-8);

        final double[] coefs = O2CorrOlciIO.parseJSON1DimDoubleArray(jsonObject, "coef");
        final double[] expectedCoefs = {
                0.7389559161849003,
                1.0031293314088998,
                0.07322228867490567,
                -0.11330928826087217,
                0.09548570369830006,
                0.008328890679651264,
                -0.3125364083301735,
                0.1275950228285915,
                -0.007912068831936118
        };
        assertNotNull(coefs);
        assertEquals(9, coefs.length);
        assertArrayEquals(expectedCoefs, coefs, 1e-8);
    }

    @Test
    public void testParseJsonFile_desmileLut() throws Exception {
        final String jsonFile = getClass().getResource("O2_desmile_lut_TEST.json").getFile();

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(new FileReader(jsonFile));

        final long L = O2CorrOlciIO.parseJSONInt(jsonObject, "L");
        final long expectedL = 3;
        assertEquals(expectedL, L);

        final long M = O2CorrOlciIO.parseJSONInt(jsonObject, "M");
        final long expectedM = 1;
        assertEquals(expectedM, M);

        final long N = O2CorrOlciIO.parseJSONInt(jsonObject, "N");
        final long expectedN = 4;
        assertEquals(expectedN, N);

        final double[][][] jacobians = O2CorrOlciIO.parseJSON3DimDoubleArray(jsonObject, "JACO");
        final double[][][] expectedJacobians = {
                {
                        {
                                -0.197668526954583,
                                0.0,
                                -0.0395649820755298,
                                0.0
                        }
                },
                {
                        {
                                -2.007899810240903138,
                                3.0026280660476688256,
                                4.5873694823399553,
                                5.02119888053260749
                        }
                },
                {
                        {
                                -0.007899810240903138,
                                0.0026280660476688256,
                                1.5873694823399553,
                                0.02119888053260749
                        }
                }
        };
        assertNotNull(jacobians);
        assertEquals(3, jacobians.length);
        assertEquals(1, jacobians[1].length);
        assertEquals(4, jacobians[2][0].length);
        assertArrayEquals(expectedJacobians, jacobians);

        final double[][] X = O2CorrOlciIO.parseJSON2DimDoubleArray(jsonObject, "X");
        final double[][] expectedX = {
                {
                        -1.6514456476894992,
                        -1.5811388300810618,
                        -0.267609642092725,
                        -1.5491933384829668
                },
                {
                        -5.6514456476894992,
                        -6.5811388300810618,
                        -7.267609642092725,
                        -8.5491933384829668
                },
                {
                        1.6514456476894992,
                        1.5811388300869134,
                        2.088818413372202,
                        1.5491933384829668
                }
        };
        assertNotNull(X);
        assertEquals(3, X.length);
        assertEquals(4, X[1].length);
        assertArrayEquals(expectedX, X);

        final double[][] Y = O2CorrOlciIO.parseJSON2DimDoubleArray(jsonObject, "Y");
        final double[][] expectedY = {
                {
                        0.9293298058188657
                },
                {
                        0.9088407606665322
                },
                {
                        1.0582987928525662
                }
        };
        assertNotNull(Y);
        assertEquals(3, Y.length);
        assertEquals(1, Y[1].length);
        assertArrayEquals(expectedY, Y);

        final double[] VARI = O2CorrOlciIO.parseJSON1DimDoubleArray(jsonObject, "VARI");
        final double[] expectedVARI = {
                0.6055300708195136,
                0.09486832980506345,
                0.19662785879946731,
                2.581988897471611
        };
        assertNotNull(VARI);
        assertEquals(4, VARI.length);
        assertArrayEquals(expectedVARI, VARI, 1e-8);

        final double cbwd = O2CorrOlciIO.parseJSONDouble(jsonObject, "cbwd");
        final double expectedCbwd = 2.65;
        assertEquals(expectedCbwd, cbwd, 1e-8);

        final double cwvl = O2CorrOlciIO.parseJSONDouble(jsonObject, "cwvl");
        final double expectedCwvl = 761.726;
        assertEquals(expectedCwvl, cwvl, 1e-8);

        final long leafsize = O2CorrOlciIO.parseJSONInt(jsonObject, "leafsize");
        final long expectedLeafsize = 4;
        assertEquals(expectedLeafsize, leafsize);

        final String[] sequ = O2CorrOlciIO.parseJSON1DimStringArray(jsonObject, "sequ");
        final String[] expectedSequ = {
                "dwvl,bwd,tra,amf",
                "tra/zero"
        };
        assertNotNull(sequ);
        assertEquals(2, sequ.length);
        assertArrayEquals(expectedSequ, sequ);

        final double[] MEAN = O2CorrOlciIO.parseJSON1DimDoubleArray(jsonObject, "MEAN");
        final double[] expectedMEAN = {
                1.5257651681785976e-20,
                2.6499999999997224,
                0.35496667905858464,
                6.0
        };
        assertNotNull(MEAN);
        assertEquals(4, MEAN.length);
        assertArrayEquals(expectedMEAN, MEAN, 1e-8);
    }
}
