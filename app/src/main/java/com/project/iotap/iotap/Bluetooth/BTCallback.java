package com.project.iotap.iotap.Bluetooth;

/**
 * Created by anton on 12/13/17.
 * Callback for getting the gesture data from motion sensor to MainActivity
 */

public interface BTCallback {
    void rawGestureDataCB(int[][] rawGestureData);
}
