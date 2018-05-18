package org.esa.s3tbx.olci.o2corr;

/**
 * Constants for OLCO O2 correction
 *
 * @author olafd
 */
public class O2CorrOlciConstants {

    public static int OLCI_INVALID_BIT = 25;

    // for bands 13-15, taken from O2_tra2recti_model_coeff_*.json:
    public static double[] cwvl = {761.726, 764.825, 767.917};
    public static double[] cbdw = {2.65, 3.7, 2.65};

    //
    public static double[][] DWL_CORR_OFFSET = {
            {
                    0.0,-0.1,-0.15,-0.15,-0.25
            },
            {
                    -0.05,-0.1,-0.15,-0.15,-0.25
            },
            {
                    0.0,-0.1,-0.15,-0.15,-0.25
            }
    };
    public static double[][] DWL_CORR_D_OFFSET = {
            {
                    -0.05,-0.1,-0.15,-0.15,-0.25
            },
            {
                    -0.05,-0.1,-0.15,-0.15,-0.25
            },
            {
                    -0.05,-0.1,-0.15,-0.15,-0.25
            }
    };

    // for bands 13-15, taken from O2_tra2recti_model_coeff_*.json:
    public static double[][] pCoeffsRectification = {
            {
                    1.9281088925186658,
                    0.8611327564752951,
                    0.2580503194681209,
                    -0.045993280418357915,
                    0.005358643551587891,
                    0.12659773028733481,
                    0.0,
                    -0.9116617176995471,
                    0.0,
                    0.0
            },
            {
                    2.512290463762116,
                    1.4511538250539846,
                    0.4940964173193233,
                    -0.022254534016777284,
                    0.002646811571508564,
                    0.15054629162744312,
                    0.0,
                    -1.502810421190935,
                    0.0,
                    0.0
            },
            {
                    27.74755282728319,
                    26.596747414918887,
                    11.689654182833268,
                    -0.001539147433752177,
                    0.0002400425859420771,
                    0.1852299228319541,
                    0.0,
                    -26.749581014760352,
                    0.0,
                    0.0
            }
    };
}
