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
 * Created by Anton on 2017-12-08.
 * Class that handles the connection and writing and reading data with motion sensor over Bluetooth
 */

public class BluetoothHandler {

    private static final String TAG = "Bluetooth";
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

        private int rowCounter = 0;
        private StringBuilder wholeMessage = new StringBuilder(nbrRowsToRead);
        private Boolean initiated = false;

        public void handleMessage(Message msg) {

            String latestMessage = (String) msg.obj;
            Log.d(TAG, "latestMessage: " + latestMessage);

            wholeMessage.append(latestMessage);
            Log.d(TAG, "wholeMessage: " + wholeMessage);

            if (!initiated) {
                checkIfBeginningOfMessage();
            }

            if (latestMessage.contains("h") && wholeMessage.length() >= 13) {
                String subWholeMessage = wholeMessage.subSequence(0, wholeMessage.lastIndexOf("h")).toString();

                motionSensorValuesToArray(subWholeMessage);

                wholeMessage.delete(0, wholeMessage.lastIndexOf("h"));
            }

            if (rowCounter == nbrRowsToRead) {
                printDebugToBeRemoved();

                bluetoothCallback.rawGestureDataCB(rawGestureData);

                resetForNewReading();
            }
        }

        //TODO: REMOVE!
        private void printDebugToBeRemoved() {
            for (int[] row : rawGestureData) {
                StringBuilder currentRow = new StringBuilder();
                for (int measurementData : row) {
                    currentRow.append(String.valueOf(measurementData)).append(",");
                }
                Log.d(TAG, currentRow + "\n");
            }
        }

        private void motionSensorValuesToArray(String subWholeMessage) {
            String[] strArray = subWholeMessage.split(",");
            int[] measurementData = new int[6];
            int columnIndex = 0;

            for (String s : strArray) {
                try {
                    int measurement = Integer.parseInt(s);
                    measurementData[columnIndex] = measurement; // TODO sometimes throws out of bounds. It seems to happen when the motion sensor is moved to fast. Maybe we should just tell the user to do it slower?
                    columnIndex++;
                } catch (NumberFormatException ignored) {
                    //Ignore exception, we only care about numbers.
                }
                catch (IndexOutOfBoundsException exception){
                    Log.d("XxXxXxXxXxXxXxXxXxXxX", "INDEX OUT OF BOUNDS; CONTINUING!jdsmbnfjdbahvgdvsghvfghvsdgfhvsvfghvsdhvgfgyjg");
                    break;
                }
            }
            rawGestureData[rowCounter++] = measurementData;
        }

        private void checkIfBeginningOfMessage() {
            if (wholeMessage.toString().startsWith("window size = 20")) {
                wholeMessage.delete(0, 16);
                initiated = true;
            }
        }

        private void resetForNewReading() {
            initiateDefaultValueForArray();
            wholeMessage = new StringBuilder(20);
            rowCounter = 0;
        }
    };

    /**
     * Constructor that enables bluetooth if it's off and starts the connect thread if the sensor is found.
     *
     * @param bluetoothCallback
     */
    public BluetoothHandler(BTCallback bluetoothCallback) {
        Log.d(TAG, "BTHandler started...");
        initiateDefaultValueForArray();
        this.bluetoothCallback = bluetoothCallback;
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Log.d(TAG, "Device does not support BT");
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

        if (btDevice == null) {
            Log.d(TAG, "Device wasn't paired.");
        } else {
            connectThread = new ConnectThread(btDevice);
            connectThread.start();
        }
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
                Log.d(TAG, "Socket error: " + e.getMessage());
            }
            mmSocket = tmp;
        }

        public void run() {
            btAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                Log.d(TAG, "Socket connection error: " + connectException.getMessage());
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.d(TAG, "Error when closing the socket: " + closeException.getMessage());
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
                Log.d(TAG, "Error when closing the socket: " + closeException.getMessage());
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
                Log.d(TAG, "Error when opening streams" + e.getMessage());
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.d(TAG, "Now connected and ready to read:");
            byte[] buffer = new byte[1024];
            int bytes;
            write(("w"+ String.valueOf(nbrRowsToRead)).getBytes()); //Configures sensor to only send 20 samples.

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    btMessageHandler.obtainMessage(1, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "Error when reading from stream" + e.getMessage());
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.d(TAG, "Error when writing to outputStream" + e.getMessage());
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "Error when closing socket" + e.getMessage());
            }
        }
    }

    /**
     * Stops the threads and deletes them.
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
     * Initializes the rawGestureData with a default value of 50 000
     */
    private void initiateDefaultValueForArray() {
        for (int i = 0; i < rawGestureData.length; i++) {
            for (int j = 0; j < rawGestureData[i].length; j++) {
                rawGestureData[i][j] = 50000;
            }
        }
    }
}
