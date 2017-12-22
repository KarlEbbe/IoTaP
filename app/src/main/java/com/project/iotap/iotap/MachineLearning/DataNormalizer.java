package com.project.iotap.iotap.MachineLearning;

import android.util.Log;
import android.util.Pair;

import java.util.LinkedList;
import java.util.List;

/**
 * A class for smoothing (filling out missing) data, as well as normalizing it.
 *
 * @author Christoffer Nilsson.
 */
public class DataNormalizer {

    private static final String TAG = "DataNormalizer";
    private int min = 0;
    private int max = 100;

    /**
     * Fills in missing values using the overall average for that value,
     * e.g. if AccX7 is 0 then it will replace that value with the average for
     * all the other AccX.
     * <p>
     * Then normalizes all values between 0-100.
     *
     * @param rawGestureData the raw sensor data
     */
    public void processData(int[][] rawGestureData) {
        Log.d(TAG, "\n\nBefore processing data\n\n");
        printData(rawGestureData);
        fillMissingData(rawGestureData);
        Log.d(TAG, "\n\nAfter filling in missing data\n\n");
        printData(rawGestureData);
        normalizeData2(rawGestureData);
        Log.d(TAG, "\n\nAfter Normalizing data\n\n");
        printData(rawGestureData);
    }

    private void normalizeData2(int[][] rawGestureData) { //Is 20X6. but needs to be 20x6 + 1x2
        int[][] calcArray = new int[25][6];
        int[][] smoothedGestureData = new int[20][6];

        for (int i = 0; i < rawGestureData.length; i++) {
            for (int j = 0; j < rawGestureData[i].length; j++) {
                calcArray[i][j] = rawGestureData[i][j];
            }
        }

        for (int i = 15; i < smoothedGestureData.length; i++) {
            for (int j = 0; j < calcArray[i].length; j++) {
                calcArray[i + 5][j] = smoothedGestureData[i][j];
            }
        }

        for (int col = 0; col < 6; col++) {
            int sum = 0;
            int rowCounter = 1;
            int outerRowCounter = 0;

            for (int row = 0; row < calcArray.length; row++) {
                sum += calcArray[row][col];
                Log.d(TAG, "Value in arr: " + String.valueOf(calcArray[row][col]));


                if ( rowCounter % 5 == 0 ) {
                    int average =  Math.round(sum/5);
                    Log.d(TAG, "Average " + average);
                    smoothedGestureData[outerRowCounter++][col] = average;
                    sum = 0;
                    Log.d(TAG, "SmoothArray" );
                    printData(smoothedGestureData);
                    row-=3;
                }
                rowCounter++;
            }
        }

        printData(smoothedGestureData);

        rawGestureData = smoothedGestureData;
        try {
            Thread.sleep(1000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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
    private void normalizeData(int[][] data) {
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
    }
}