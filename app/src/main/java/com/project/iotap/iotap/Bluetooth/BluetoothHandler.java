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
 */

public class BluetoothHandler {

    private static final String TAG = "Bluetooth";
    private static final int nbrRowsToRead = 20;
    private int[][] rawGestureData = new int[nbrRowsToRead][6]; //20 rows, with 6 values on each row representing AccX, AccY, AccZ, GyX, GyY, GyZ values.

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

        private StringBuilder appendedBTMessage = new StringBuilder(20);

        private Boolean initiated = false;

        public void handleMessage(Message msg) {

            if (msg.what == 1) { //Check if this is really necessary.
                String readMessage = (String) msg.obj;
                Log.d("HANDLER", "readMessage: " + readMessage);

                if(!initiated){
                    appendedBTMessage.append(readMessage);
                    checkIfBeginningOfMessage();
                }

                Log.d("HANDLER", "appendedBTMessage: " + appendedBTMessage);

                if(readMessage.contains("h") && appendedBTMessage.length() >=13 ){
                    String subStrAppended = appendedBTMessage.subSequence(0, appendedBTMessage.lastIndexOf("h")).toString();

                    insertMeasurementValuesIntoArray(subStrAppended);

                    appendedBTMessage = new StringBuilder(20);
                    appendedBTMessage.append(readMessage);
                }

                //Just for debugging, prints the aquired data for the collected gesture.
                if(rowCounter == nbrRowsToRead){
                    for(int[] row: rawGestureData){
                        StringBuilder currentRow = new StringBuilder();
                        for(int measurementData : row){
                            currentRow.append(String.valueOf(measurementData)).append(",");
                        }
                        Log.d("gestureArray", currentRow +  "\n");
                    }

                    //Just some sleep for debugging purposes. To be deleted in final project.
                    try {
                        Thread.sleep(20000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    bluetoothCallback.rawGestureDataCB(rawGestureData);
                    resetForNewReading();
                }
            }
        }

        private void insertMeasurementValuesIntoArray(String formattedString) {
            String[] strArray = formattedString.split(",");

            int[] measurementData = new int[6];

            int columnIndex = 0;
            for (String aStrArray : strArray) {
                try {
                    int x = Integer.parseInt(aStrArray);
                    measurementData[columnIndex] = x;
                    columnIndex++;
                } catch (NumberFormatException ignored) {
                    //Do nothing, we only care about numbers.
                }
            }
            rawGestureData[rowCounter++] = measurementData;
        }

        private void checkIfBeginningOfMessage() {
            if(appendedBTMessage.toString().startsWith("window size = 20")){
                appendedBTMessage.delete(0,16);
                initiated = true;
            }
        }

        private void resetForNewReading() {
            rawGestureData = new int[30][6];
            appendedBTMessage = new StringBuilder(20);
            rowCounter = 0;
        }
    };

    /**
     * EXPERIMENTAL HANDLER!
     */
    @SuppressLint("HandlerLeak")
    private final Handler EXPERIMENTAL = new Handler() {

        private int rowCounter = 0;

        private StringBuilder appendedBTMessage = new StringBuilder(20);

        private Boolean initiated = false;

        private long startTime = 0L;

        public void handleMessage(Message msg) {
            if (msg.what == 1) { 
                String readMessage = (String) msg.obj;
                Log.d("HANDLER", "readMessage: " + readMessage);

                if(!initiated){
                    appendedBTMessage.append(readMessage);
                    checkIfBeginningOfMessage();
                }

                if(startTime == 0L){
                     startTime = System.currentTimeMillis();
                }

                Log.d("HANDLER", "appendedBTMessage: " + appendedBTMessage);

                if(readMessage.contains("h") && appendedBTMessage.length() >=13 ){
                    String subStrAppended = appendedBTMessage.subSequence(0, appendedBTMessage.lastIndexOf("h")).toString();

                    insertMeasurementValuesIntoArray(subStrAppended);

                    appendedBTMessage = new StringBuilder(20);
                    appendedBTMessage.append(readMessage);
                }

                //If less than 15 rows where measured in a timespan, tell the user to try again!
                if(timeout()){
                    if(rowCounter<15){
                        //Maybe a callback or something to mainActivity to notify the user.
                        Log.d("HANDLER", "TRY AGAIN, less than 15 readings within 1 sec");
                        printGestureData();
                        resetForNewReading();
                    }else if(rowCounter == nbrRowsToRead){
                        printGestureData();
                        bluetoothCallback.rawGestureDataCB(rawGestureData);
                        resetForNewReading();
                    }
                }
            }
        }

        private void printGestureData() {
            for(int[] row: rawGestureData){
                StringBuilder currentRow = new StringBuilder();
                for(int measurementData : row){
                    currentRow.append(String.valueOf(measurementData)).append(",");
                }
                Log.d("gestureArray", currentRow +  "\n");
            }
        }

        private boolean timeout() {
            long estimatedTime = System.currentTimeMillis() - startTime;

            if(estimatedTime > 1500){
                return true;
            }
            return false;
        }

        private void insertMeasurementValuesIntoArray(String formattedString) {
            String[] strArray = formattedString.split(",");

            int[] measurementData = new int[6];

            int columnIndex = 0;
            for (String aStrArray : strArray) {
                try {
                    int x = Integer.parseInt(aStrArray);
                    measurementData[columnIndex] = x;
                    columnIndex++;
                } catch (NumberFormatException ignored) {
                    //Do nothing, we only care about numbers.
                }
            }
            rawGestureData[rowCounter++] = measurementData;
        }

        private void checkIfBeginningOfMessage() {
            if(appendedBTMessage.toString().startsWith("window size = 20")){
                appendedBTMessage.delete(0,16);
                initiated = true;
            }
        }

        private void resetForNewReading() {
            rawGestureData = new int[30][6];
            appendedBTMessage = new StringBuilder(20);
            rowCounter = 0;
        }
    };

    /**
     * Constructor that enables bluetooth if off and starts the connect thread if the sensor is found.
     * @param bluetoothCallback
     */
    public BluetoothHandler(BTCallback bluetoothCallback) {
        Log.d(TAG, "BTHandler started...");
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
            Log.d(TAG, "Device  wasn't paired."); //Maybe should notify the user of this error!
        }else{
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
            write( "w20".getBytes()); //Configures sensor to only send 20 samples.

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
        if(connectThread!= null){
            connectThread.cancel();
            connectThread = null;
        }
        if(readAndWriteThread != null){
            readAndWriteThread.cancel();
            readAndWriteThread = null;
        }
    }
}
