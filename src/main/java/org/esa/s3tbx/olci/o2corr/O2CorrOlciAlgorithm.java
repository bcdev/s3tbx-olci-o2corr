package org.esa.s3tbx.olci.o2corr;

/**
 * Class providing the algorithm for OLCO O2 correction
 *
 * @author olafd
 */
public class O2CorrOlciAlgorithm {

    /**
     * Provides config data for OLCI O2 correction
     *
     */
    public static O2CorrConfigData getO2CorrConfigData() {
        // todo
        return null;
    }

    /**
     * This calculates the 1to1 transmission  ('rectified') (Zenith Sun --> Nadir observation: amf=2) at
     * given band, which would be measured without scattering. Usefull only for comparison.
     *
     * @param bandIndex - index of given band
     * @param press - input pressure
     * @param configData - configuration data
     *
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
     * @param bandIndex - index of given band
     * @param rectifiedTrans - input rectified transmission
     * @param configData - configuration data
     *
     * @return press - the pressure
     */
    public static double trans2Press(int bandIndex, double rectifiedTrans, O2CorrConfigData configData) {
        // todo
        double press = 0.0;
        return press;
    }

    /**
     * This calculates the 'rectified' transmission in given band (Zenith Sun --> Nadir observation: amf=2), given
     * a transmission in given band and the corresponding geometry.
     *
     * @param bandIndex - index of given band
     * @param desmiledTrans - input desmiled transmission
     * @param configData - configuration data
     *
     * @return rectifiedDesmiledTrans - the rectified desmiled transmission
     */
    public static double rectifyDesmiledTrans(int bandIndex, double desmiledTrans, O2CorrConfigData configData) {
        // todo
        double rectifiedDesmiledTrans = 0.0;
        return rectifiedDesmiledTrans;
    }

    /**
     * This calculates smile corrected transmission given the transmission in given band and additionally
     the wavelength, bandwidth and the amf.
     *
     * @param bandIndex - index of given band
     * @param trans - input transmission
     * @param amf - air mass factor
     * @param configData - configuration data
     *
     * @return  desmiledTrans - the desmiled transmission
     */
    public static double desmileTrans(int bandIndex, double trans, double amf, O2CorrConfigData configData) {
        // todo
        double desmiledTrans = 0.0;
        return desmiledTrans;
    }

    /**
     * Provides a simple estimate for pressure from given height.
     *
     * @param height - height in m
     *
     * @return pressure in hPa
     */
    public static double height2press(double height) {
        return Math.pow(1013.25*(1.0 - (height*0.0065/288.15)), 5.2555);
    }

    public static double overcorrectLambda(double inputWvl, int cam, double dwvl) {
        // todo
        double outputWvl = 0.0;
        return outputWvl;
    }
}
