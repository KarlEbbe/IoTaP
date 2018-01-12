package com.project.iotap.iotap.Bluetooth;

/**
 * Callback for getting the gesture data from motion sensor to MainActivity.
 * @author Anton
 */
public interface BTCallback {
    void rawGestureDataCB(int[][] rawGestureData);
}