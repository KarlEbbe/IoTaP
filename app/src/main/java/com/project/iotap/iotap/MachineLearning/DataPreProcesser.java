package com.project.iotap.iotap.MachineLearning;

import android.util.Log;
import android.util.Pair;

import java.util.LinkedList;
import java.util.List;

/**
 * A class for filling out missing values and smoothing the data.
 *
 * @author Anton Gustafsson, Christoffer Nilsson.
 */
public class DataPreProcesser {

    private static final String TAG = "DataPreProcesser";
    private static final int SMOOTH_N = 3;
    private final int oldAccMin = -445;
    private final int oldAccMax = 422;
    private final int oldGyrMin = -6523;
    private final int oldGyrMax = 6272;
    private final int min = 0;
    private final int max = 200;

    /**
     * Fills in missing values using the overall average for that value,
     * e.g. if AccX7 is 0 then it will replace that value with the average for
     * all the other AccX.
     * Then normalizes all values between 0-100.
     *
     * @param rawGestureData the raw sensor data
     */
    public void processData(int[][] rawGestureData) {
        Log.d(TAG, "\n\ndata before processed: ");
        printData(rawGestureData);

        fillMissingData(rawGestureData);
        Log.d(TAG, "\n\ndata filled.: ");
        printData(rawGestureData);

        rawGestureData = smoothData(rawGestureData);
        Log.d(TAG, "\n\ndata smoothed: ");
        printData(rawGestureData);

        normalizeData(rawGestureData);
        Log.d(TAG, "\n\ndata normalized: ");
        printData(rawGestureData);
    }

    /**
     * Using moving window to smooth the data by averaging.
     *
     * @param rawGestureData
     */
    private int[][] smoothData(int[][] rawGestureData) {
        int[][] smoothedGestureData = new int[rawGestureData.length][rawGestureData[0].length];
        for (int row = 0; row < smoothedGestureData.length; row++) { // 0-19
            for (int col = 0; col < smoothedGestureData[row].length; col++) { // 0-5
                if (row >= 2) { // Third row and onward we take average of previous two plus current.
                    smoothedGestureData[row][col] = (rawGestureData[row][col] + rawGestureData[row - 1][col] + rawGestureData[row - 2][col]) / 3;
                } else if (row == 0) { // row == 0, i.e. first row. Smoothed valued = current value.
                    smoothedGestureData[row][col] = rawGestureData[row][col];
                } else { // row == 1, i.e. second row. Smoothed value = current value + previous value / 2.
                    smoothedGestureData[row][col] = (rawGestureData[row][col] + rawGestureData[row - 1][col]) / 2;
                }
            }
        }
        return smoothedGestureData;
    }

    /**
     * Fills in missing values using the overall average for that value,
     * e.g. if AccX7 is 50000 then it will replace that value with the average for
     * all the other AccX.
     *
     * @param data the raw sensor data
     */
    private void fillMissingData(int[][] data) {
        double[] average = new double[6]; // 0 = AccX, 1 = AccY etc.
        List<Pair<Integer, Integer>> missing = new LinkedList<>();
        for (int row = 0; row < data.length; row++) { // 0-19
            for (int col = 0; col < data[row].length; col++) { // 0-5
                if (data[row][col] == 50000) { // Remember the missing values.
                    missing.add(new Pair<>(row, col));
                } else if (row != 0) { // Don't divide by 0.
                    average[col] = (average[col] * row + data[row][col]) / (row + 1);
                } else { // If i = 0, we just set average to the value.
                    average[col] = data[row][col];
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
     * Normalizes data between 0-200.
     *
     * @param data the smoothed data
     */
    private void normalizeData(int[][] data) {
        // First normalize first three values, i.e. AccX, AccY and AccZ, then take gyro.
        for (int row = 0; row < data.length; row++) {
            for (int col = 0; col < data[0].length; col++) {
                if (col < 3) {
                    data[row][col] = (int) normalize(data[row][col], oldAccMin, oldAccMax);
                } else {
                    data[row][col] = (int) normalize(data[row][col], oldGyrMin, oldGyrMax);
                }
            }
        }
    }

    /**
     * Normalizes the value to between 0-200.
     *
     * @param value the value to normalize
     * @return the normalized value
     */
    private double normalize(int value, int oldMin, int oldMax) {
        return ((double) (value - oldMin) / (oldMax - oldMin)) * (max - min) + min;
    }

    //Print the gesture array.
    private void printData(int[][] rawGestureData) {
        for (int[] row : rawGestureData) {
            StringBuilder currentRow = new StringBuilder();
            for (int measurementData : row) {
                currentRow.append(String.valueOf(measurementData)).append(",");
            }
            Log.d(TAG, currentRow + "\n");
        }
    }
}