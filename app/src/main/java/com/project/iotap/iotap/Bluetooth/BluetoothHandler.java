package com.project.iotap.iotap.Bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * Class that handles the connection and writing and reading data with motion sensor over Bluetooth.
 * @author Anton
 */
public class BluetoothHandler {

    private static final String TAG = "BluetoothHandler";
    private static final int nbrRowsToRead = 20;
    private final int[][] rawGestureData = new int[nbrRowsToRead][6]; //20 rows, with 6 values on each row representing AccX, AccY, AccZ, GyX, GyY, GyZ values.
    private BluetoothAdapter btAdapter = null;
    private ConnectThread connectThread = null;
    private ReadAndWriteThread readAndWriteThread = null;
    private final BTCallback bluetoothCallback;

    /**
     * Handler that parses data from bluetooth motion sensor.
     */
    @SuppressLint("HandlerLeak")
    private final Handler btMessageHandler = new Handler() {

        private StringBuilder wholeMessage = new StringBuilder(125);
        private long startTime = System.currentTimeMillis();
        private int hCounter;

        public void handleMessage(Message msg) {
            String latestMessage = (String) msg.obj;
            if (latestMessage.contains("h")) {
                hCounter++;
            }
            wholeMessage.append(latestMessage);
            if (timeout()) {
                Log.d(TAG, "Time is up!");
                if (hCounter >= 15) { // We require 15 readings.
                    Log.d(TAG, "h limit reached or surpassed: " + hCounter);
                    motionSensorValuesToArray(wholeMessage.toString());
                    bluetoothCallback.rawGestureDataCB(rawGestureData);
                    resetForNewReading();
                } else {
                    Log.e(TAG, "Not enough h: " + hCounter);
                }
                hCounter = 0;
                wholeMessage = new StringBuilder(120);
            }
        }

        /**
         * Returns true if it has been more than one second since last move, false if not.
         * @return true if the move took too long or false otherwise
         */
        private boolean timeout() {
            long estimatedTime = System.currentTimeMillis() - startTime;
            if (estimatedTime >= 1000) {
                startTime = System.currentTimeMillis();
                return true;
            }
            return false;
        }

        /**
         * Goes through the message received from bluetooth and converts it into a 2d array.
         * @param wholeMessage
         */
        private void motionSensorValuesToArray(String wholeMessage) {
            String[] splitWholeMessage = wholeMessage.split(",");
            int rowIndex = 0;
            int columnIndex = 0;
            for (String s : splitWholeMessage) {
                try {
                    int measurement = Integer.parseInt(s);
                    rawGestureData[rowIndex][columnIndex++] = measurement;
                    if (columnIndex == 6) {
                        columnIndex = 0;
                        rowIndex++;
                        if (rowIndex == 20) {
                            return;
                        }
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore exception, we only care about numbers.
                }
            }
        }

        /**
         * Resets values to prepare for a new reading.
         */
        private void resetForNewReading() {
            initiateDefaultValueForArray();
            wholeMessage = new StringBuilder(120);
        }
    };

    /**
     * Constructor that enables bluetooth if it's off and starts the connect thread if the sensor is found.
     * @param bluetoothCallback
     */
    public BluetoothHandler(BTCallback bluetoothCallback) {
        initiateDefaultValueForArray();
        this.bluetoothCallback = bluetoothCallback;
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            return;
        }
        if (!btAdapter.isEnabled()) {
            btAdapter.enable();
        }
        BluetoothDevice btDevice = null;
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("G6")) {
                    btDevice = device;
                }
            }
        }
        connectThread = new ConnectThread(btDevice);
        connectThread.start();
    }

    /**
     * Thread that handles socket creation and connection with the sensor.
     */
    private class ConnectThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket error: " + Log.getStackTraceString(e));
            }
            mmSocket = tmp;
        }

        public void run() {
            btAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException e) {
                Log.e(TAG, "Socket connection error: " + Log.getStackTraceString(e));
                try {
                    mmSocket.close();
                } catch (IOException e1) {
                    Log.e(TAG, "Error when closing the socket: " + Log.getStackTraceString(e1));
                }
                return;
            }
            initiateDefaultValueForArray();
            readAndWriteThread = new ReadAndWriteThread(mmSocket);
            readAndWriteThread.start();
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Error when closing the socket: " + closeException.getMessage());
            }
        }
    }

    /**
     * Thread that handles the in and output streams from and to the sensor.
     */
    private class ReadAndWriteThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ReadAndWriteThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error when opening streams" + Log.getStackTraceString(e));
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            write(("w" + String.valueOf(nbrRowsToRead)).getBytes()); //Configures sensor to only send 20 samples.
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    btMessageHandler.obtainMessage(1, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "Error when reading from stream: " + Log.getStackTraceString(e));
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Error when writing to stream: " + Log.getStackTraceString(e));
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error when closing socket: " + Log.getStackTraceString(e));
            }
        }
    }

    /**
     * Stops the threads.
     */
    public void cancel() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (readAndWriteThread != null) {
            readAndWriteThread.cancel();
            readAndWriteThread = null;
        }
    }

    /**
     * Initializes the rawGestureData with a default value of 50000 (empty).
     */
    private void initiateDefaultValueForArray() {
        for (int i = 0; i < rawGestureData.length; i++) {
            for (int j = 0; j < rawGestureData[i].length; j++) {
                rawGestureData[i][j] = 50000;
            }
        }
    }
}