package org.esa.s3tbx.olci.o2corr;

import smile.neighbor.KDTree;
import smile.neighbor.Neighbor;

/**
 * Class providing the algorithm for OLCO O2 correction
 *
 * @author olafd
 */
public class O2CorrOlciAlgorithm {

    /**
     * Provides config data for OLCI O2 correction
     */
    public static O2CorrConfigData getO2CorrConfigData() {
        // todo
        return null;
    }

    /**
     * This calculates the 1to1 transmission  ('rectified') (Zenith Sun --> Nadir observation: amf=2) at
     * given band, which would be measured without scattering. Usefull only for comparison.
     *
     * @param bandIndex  - index of given band
     * @param press      - input pressure
     * @param configData - configuration data
     * @return rectifiedTrans - the rectified transmission
     */
    public static double press2RectifiedTrans(int bandIndex, double press, O2CorrConfigData configData) {
        // todo
        double rectifiedTrans = 0.0;
        return rectifiedTrans;
    }

    /**
     * This calculates the pressure given a 1to1 transmission ('rectified') (Zenith Sun --> Nadir
     * observation: amf=2), which would be measured in given band without scattering. Usefull for a
     * first object height estimation (but *not* for dark targets (ocean!!!!))
     *
     * @param bandIndex      - index of given band
     * @param rectifiedTrans - input rectified transmission
     * @param configData     - configuration data
     * @return press - the pressure
     */
    public static double trans2Press(int bandIndex, double rectifiedTrans, O2CorrConfigData configData) {
        // todo
        double press = 0.0;
        return press;
    }

    /**
     * Provides a simple estimate for pressure from given height.
     *
     * @param height - height in m
     * @return pressure in hPa
     */
    public static double height2press(double height) {
        return Math.pow(1013.25 * (1.0 - (height * 0.0065 / 288.15)), 5.2555);
    }

    public static float overcorrectLambda(float cam, double[] dwvl) {
        double delta = 0.0;
        for (int i = 0; i < 5; i++) {
            delta = (cam == i + 1) ? dwvl[i] : 0.0;
        }

        return (float) delta;
    }

    /**
     * Desmile input transmission using interpolation of Desmile LUT, using KD search.
     * Java version (simplified) of 'lut2func_internal' in kd_interpolator.py of RP Python breadboard.
     *
     * @param cwl   - central wavelength
     * @param fwhm  - band width (full width at half maximum)
     * @param amf   - air mass factor
     * @param trans - original transmission
     * @param tree  - the KD Tree. Should have been once initialized at earlier stage.
     * @param lut   - the desmile LUT held in DesmileLut object. Should have been once initialized at earlier stage.
     * @return trans_desmiled
     */
    public static double desmileTransmission(double cwl, double fwhm, double amf, double trans,
                                      KDTree<double[]> tree, DesmileLut lut) {

        double[] x = new double[]{cwl, fwhm, amf, trans};
        final double[] wo = new double[lut.getVARI().length];
        for (int i = 0; i < lut.getVARI().length; i++) {
            wo[i] = x[i] - lut.getMEAN()[i] / lut.getVARI()[i];
        }

        final int nNearest = (int) Math.pow(2.0, lut.getN());   // should be 16
        final Neighbor<double[], double[]>[] neighbors = tree.knn(wo, nNearest);
        final double[] distances = new double[neighbors.length];
        final int[] indices = new int[neighbors.length];
        for (int i = 0; i < distances.length; i++) {
            distances[i] = neighbors[i].distance;
            indices[distances.length - i - 1] = neighbors[i].index;
        }

        final double small = 1.E-7;
        double[] weight = new double[distances.length];
        double norm = 0.0;
        for (int i = 0; i < weight.length; i++) {
            weight[i] = Double.isInfinite(distances[i]) ? small : distances[i] + small;
            norm += weight[i];
        }

        double temp = 0.0;
        for (int j = 0; j < nNearest; j++) {
            final boolean valid = !Double.isInfinite(distances[j]);
            if (valid) {
                for (int k = 0; k < wo.length; k++) {
                    final double dx = (wo[k] - lut.getX()[indices[j]][k]) * lut.getVARI()[k];
                    temp += (lut.getY()[indices[j]][k] + dx * lut.getJACO()[0][indices[j]][k]) * weight[j];
                }
            }
        }
        return temp / norm;
    }

    /**
     * Rectifies input desmiled transmission.
     * Java version of 'generate_tra2recti' in o2corr__io_v3.py of RP Python breadboard.
     *
     * @param trans_desmiled - desmiled transmission
     * @param amf            - air mass factor
     * @param bandIndex      - band index
     * @return trans_rectified
     */
    public static double rectifyDesmiledTransmission(double trans_desmiled, double amf, int bandIndex) {
        double[] x = new double[]{trans_desmiled, amf};

        final double tau = Math.log(x[0]);
        final double amfNew = amf - 2.0;
        final double[] p = O2CorrOlciConstants.pCoeffsRectification[bandIndex - 13];

        return p[0] + p[1] * tau + p[2] * tau * tau + p[3] * amf + p[4] * amf * amf + p[5] * tau * Math.sqrt(amf) + p[7] * x[0];
    }
}
