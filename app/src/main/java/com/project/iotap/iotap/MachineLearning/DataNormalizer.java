package com.project.iotap.iotap.MachineLearning;

import android.util.Log;
import android.util.Pair;

import java.util.LinkedList;
import java.util.List;

/**
 * A class for filling out missing values and smoothing the data.
 *
 * @author Christoffer Nilsson.
 */
public class DataNormalizer {

    private static final String TAG = "DataNormalizer";
    private static final int SMOOTH_N = 3;
    private int min = 0;
    private int max = 100;

    /**
     * Fills in missing values using the overall average for that value,
     * e.g. if AccX7 is 0 then it will replace that value with the average for
     * all the other AccX.
     * Then normalizes all values between 0-100.
     *
     * @param rawGestureData the raw sensor data
     */
    public void processData(int[][] rawGestureData) {
        Log.d(TAG, "\n\nBefore processing data\n\n");
        printData(rawGestureData);
       // fillMissingData(rawGestureData);
       // Log.d(TAG, "\n\nAfter filling in missing data\n\n");
       // printData(rawGestureData);
        rawGestureData = normalizeData(rawGestureData);
        Log.d(TAG, "\n\nAfter Normalizing data\n\n");
        printData(rawGestureData);
    }

    /**
     * Using moving window to smooth the data by averaging.
     *
     * @param rawGestureData
     */
    private int[][] normalizeData(int[][] rawGestureData) {
        int[][] tmpArray = new int[22][6]; // Temporary array with two extra rows at the end to help with the smoothing.
        int[][] smoothedGestureData = new int[20][6];

        //Copies raw gesture data to tmpArray.
        for (int i = 0; i < rawGestureData.length; i++) {
            System.arraycopy(rawGestureData[i], 0, tmpArray[i], 0, rawGestureData[i].length);
        }

        //Adds 5 extra rows to the tmpArray.
        System.arraycopy(rawGestureData, 18, tmpArray, 20, smoothedGestureData.length - 18);

        //For each column...
        for (int col = 0; col < 6; col++) {
            int sum = 0;
            int modulusCounter = 1;
            int smoothedRowCounter = 0;

            //For each row...
            for (int row = 0; row < tmpArray.length; row++) {
                sum += tmpArray[row][col];
                Log.d(TAG, "rawVal: " + String.valueOf(tmpArray[row][col]));

                //For every third row...
                if (modulusCounter % SMOOTH_N == 0) {
                    int average = Math.round(sum / SMOOTH_N);
                    Log.d(TAG, "Ave " + average);
                    smoothedGestureData[smoothedRowCounter++][col] = average;
                    sum = 0;
                    Log.d(TAG, "SmoothArray");
                    printData(smoothedGestureData);

                    if(row<21){ //Fixes the trouble at the end
                        row -= 2; //Reset the row one step back.
                    }

                    if(row == 13){
                        Log.d(TAG, "PUT BREAKPOINT ON THIS LINE!!!");
                    }

                    Log.d(TAG, "ROW: " + String.valueOf(row));
                }
                modulusCounter++;
            }
        }
        printData(smoothedGestureData);
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
     *
     * @param data the smoothed data
     */
    private void normalizeDataOld(int[][] data) {
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                data[i][j] = normalize(data[i][j]);
            }
        }
    }

    /**
     * Checks to see if the current value is min or max.
     *
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
     *
     * @param value the value to normalize
     * @return the normalized value
     */
    private int normalize(int value) {
        return ((value - min) / (max - min)) * 100;
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

        Log.d(TAG, "Done printing array\n");
    }
}