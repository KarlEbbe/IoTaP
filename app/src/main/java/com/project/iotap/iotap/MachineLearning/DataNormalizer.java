package com.project.iotap.iotap.MachineLearning;

import android.util.Pair;
import java.util.LinkedList;
import java.util.List;

/**
 * A class for smoothing (filling out missing) data, as well as normalizing it.
 * @author Christoffer Nilsson.
 */
public class DataNormalizer {

    private int min = 0;
    private int max = 100;

    /**
     * Fills in missing values using the overall average for that value,
     * e.g. if AccX7 is 0 then it will replace that value with the average for
     * all the other AccX.
     *
     * Then normalizes all values between 0-100.
     * @param data the raw sensor data
     */
    public void processData(int[][] data) {
        fillMissingData(data);
        normalizeData(data);
    }

    /**
     * Fills in missing values using the overall average for that value,
     * e.g. if AccX7 is 0 then it will replace that value with the average for
     * all the other AccX.
     * @param data the raw sensor data
     */
    private void fillMissingData(int[][] data) {
        double[] average = new double[6]; // 0 = AccX, 1 = AccY etc.
        List<Pair<Integer, Integer>> missing = new LinkedList<>();
        for (int i = 0; i < data.length; i++) { // 0-19
            for (int j = 0; j < data[i].length; j++) { // 0-5
                if (data[i][j] == 50000) { // Remember the missing values.
                    missing.add(new Pair<>(i, j));
                } else if (i != 0) { // Don't divide by 0.
                    average[j] = (average[j] * i + data[i][j]) / (i + 1);
                    checkMinMax(data[i][j]);
                } else { // If i = 0, we just set average to the value.
                    average[j] = data[i][j];
                    checkMinMax(data[i][j]);
                }
            }
        }
        // Add missing values.
        for (int i = 0; i < missing.size(); i++) {
            Pair pair = missing.get(i);
            data[(int) pair.first][(int) pair.second] = (int) average[(int) pair.second];
        }
    }

    /**
     * Normalizes data between 0-100.
     * @param data the smoothed data
     */
    private void normalizeData(int[][] data) {
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                data[i][j] = normalize(data[i][j]);
            }
        }
    }

    /**
     * Checks to see if the current value is min or max.
     * @param value the value to check
     */
    private void checkMinMax(int value) {
        if (value <= min) {
            min = value;
        }
        if (value >= max) {
            max = value;
        }
    }

    /**
     * Normalizes the value to between 0-100.
     * @param value the value to normalize
     * @return the normalized value
     */
    private int normalize(int value) {
        return ((value - min) / (max - min)) * 100;
    }
}