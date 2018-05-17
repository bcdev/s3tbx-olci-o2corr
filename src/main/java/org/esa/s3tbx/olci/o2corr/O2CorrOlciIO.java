package org.esa.s3tbx.olci.o2corr;

import com.google.common.primitives.Doubles;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import smile.neighbor.KDTree;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class providing I/O related methods for OLCO O2 correction.
 *
 * @author olafd
 */
public class O2CorrOlciIO {

    public static double parseJSONDouble(JSONObject jsonObject, String variableName) {
        return (double) jsonObject.get(variableName);
    }

    public static long parseJSONInt(JSONObject jsonObject, String variableName) {
        return (Long) jsonObject.get(variableName);
    }

    public static double[] parseJSON1DimDoubleArray(JSONObject jsonObject, String variableName) {
        JSONArray jsonArray = (JSONArray) jsonObject.get(variableName);
        List<Double> doubleList = (List<Double>) jsonArray.stream().collect(Collectors.toList());
        return Doubles.toArray(doubleList);
    }

    public static String[] parseJSON1DimStringArray(JSONObject jsonObject, String variableName) {
        JSONArray jsonArray = (JSONArray) jsonObject.get(variableName);
        List<String> stringList = (List<String>) jsonArray.stream().collect(Collectors.toList());
        return stringList.toArray(new String[stringList.size()]);
    }

    public static double[][] parseJSON2DimDoubleArray(JSONObject jsonObject, String variableName) {
        final JSONArray jsonArray1 = (JSONArray) jsonObject.get(variableName);

        final int dim1 = jsonArray1.size();
        final int dim2 = ((JSONArray) jsonArray1.get(0)).size();

        JSONArray[] jsonArray2 = new JSONArray[dim1];

        double[][] doubleArr = new double[dim1][dim2];

        for (int i = 0; i < dim1; i++) {
            jsonArray2[i] = (JSONArray) jsonArray1.get(i);
            for (int j = 0; j < dim2; j++) {
                doubleArr[i][j] = (Double) jsonArray2[i].get(j);
            }
        }

        return doubleArr;
    }

    public static double[][][] parseJSON3DimDoubleArray(JSONObject jsonObject, String variableName) {
        final JSONArray jsonArray1 = (JSONArray) jsonObject.get(variableName);

        final int dim1 = jsonArray1.size();
        final int dim2 = ((JSONArray) jsonArray1.get(0)).size();
        final int dim3 = ((JSONArray) ((JSONArray) jsonArray1.get(0)).get(0)).size();

        JSONArray[] jsonArray2 = new JSONArray[dim1];
        JSONArray[][] jsonArray3 = new JSONArray[dim1][dim2];

        double[][][] doubleArr = new double[dim1][dim2][dim3];

        for (int i = 0; i < dim1; i++) {
            jsonArray2[i] = (JSONArray) jsonArray1.get(i);
            for (int j = 0; j < dim2; j++) {
                jsonArray3[i][j] = (JSONArray) jsonArray2[i].get(j);
                for (int k = 0; k < dim3; k++) {
                    doubleArr[i][j][k] = (Double) jsonArray3[i][j].get(k);
                }
            }
        }

        return doubleArr;
    }

    public static KDTree<double[]> createKDTreeForDesmileInterpolation(DesmileLut desmileLut) {
        return new KDTree<>(desmileLut.getX(), desmileLut.getX());
    }

    public static DesmileLut createDesmileLut(int bandIndex) throws IOException, ParseException {
        final String jsonFile = O2CorrOlciOp.class.getResource("O2_desmile_lut_" + bandIndex + ".json").getFile();

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(new FileReader(jsonFile));

        // parse JSON file...
        final long L = O2CorrOlciIO.parseJSONInt(jsonObject, "L");
        final long M = O2CorrOlciIO.parseJSONInt(jsonObject, "M");
        final long N = O2CorrOlciIO.parseJSONInt(jsonObject, "N");
        final double[][][] jacobians = O2CorrOlciIO.parseJSON3DimDoubleArray(jsonObject, "JACO");
        final double[][] X = O2CorrOlciIO.parseJSON2DimDoubleArray(jsonObject, "X");
        final double[][] Y = O2CorrOlciIO.parseJSON2DimDoubleArray(jsonObject, "Y");
        final double[] VARI = O2CorrOlciIO.parseJSON1DimDoubleArray(jsonObject, "VARI");
        final double cbwd = O2CorrOlciIO.parseJSONDouble(jsonObject, "cbwd");
        final double cwvl = O2CorrOlciIO.parseJSONDouble(jsonObject, "cwvl");
        final long leafsize = O2CorrOlciIO.parseJSONInt(jsonObject, "leafsize");
        final String[] sequ = O2CorrOlciIO.parseJSON1DimStringArray(jsonObject, "sequ");
        final double[] MEAN = O2CorrOlciIO.parseJSON1DimDoubleArray(jsonObject, "MEAN");

        return new DesmileLut(L, M, N, X, Y, jacobians, MEAN, VARI, cwvl, cbwd, leafsize, sequ);
    }


}
