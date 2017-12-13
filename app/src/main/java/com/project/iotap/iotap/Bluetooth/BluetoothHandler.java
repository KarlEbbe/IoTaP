package com.project.iotap.iotap.Bluetooth;

import android.bluetooth.BluetoothAdapter;

import com.project.iotap.iotap.Activities.MainActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
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

    private BluetoothAdapter btAdapter = null;

    private ConnectThread connectThread = null;
    private ReadAndWriteThread readAndWriteThread = null;

    private BTCallback bluetoothCallback;

    //30x6 values for each row.
    //So we get 30 rows, with 6 values on each row representing AccX, AccY, AccZ, GyX, GyY, GyZ
    private int[][] rawGestureData = new int[20][6];

    /**
     * Handler that parses data from bluetooth motion sensor.
     */
    private final Handler bluetoothIn = new Handler() {

        private int rowCounter = 0;

        private StringBuilder appendedBTMessage = new StringBuilder(20);

        private Boolean initiated = false;

        public void handleMessage(android.os.Message msg) {

            if (msg.what == 1) {
                String readMessage = (String) msg.obj;
                Log.d("HANDLER", "readMessage: " + readMessage);

                if(!initiated){
                    appendedBTMessage.append(readMessage);
                    if(appendedBTMessage.toString().startsWith("window size = 20")){
                        appendedBTMessage.delete(0,16);
                        initiated = true;
                    }
                }

                Log.d("HANDLER", "appendedBTMessage: " + appendedBTMessage);

                if(readMessage.contains("h") && appendedBTMessage.length() >=13 ){
                    String formattedString = appendedBTMessage.subSequence(0, appendedBTMessage.lastIndexOf("h")).toString();

                    //Log.d("We are here", "lel last index of h is" + appendedBTMessage.lastIndexOf("h"));
                    //Save the current appendedBTMessage and clear it and append
                    String[] strArray = formattedString.split(",");
                    int[] intArray = new int[6];

                    int columnIndex = 0;
                    for (String aStrArray : strArray) {
                        try {
                            int x = Integer.parseInt(aStrArray);
                            intArray[columnIndex] = x;
                            //Log.d("Array", String.valueOf(x));
                            columnIndex++;
                        } catch (NumberFormatException e) {
                            //Do nothing, we only care about numbers.
                        }
                    }
                    rawGestureData[rowCounter++] = intArray;

                    appendedBTMessage = new StringBuilder(20);
                    appendedBTMessage.append(readMessage);
                }

                if(rowCounter == 20){ //Number of rows to read.

                    for(int[] iArray: rawGestureData){
                        StringBuilder currentRow = new StringBuilder();
                        for(int j : iArray){
                            currentRow.append(String.valueOf(j)).append(",");
                        }
                        Log.d("gestureArray", currentRow +  "\n");
                    }
                    //Now we are done. The gesture data for one gesture is now put into the rawGestureData array. Now maybe we should do a callback or somehing.
                    try {
                        Thread.sleep(20000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    bluetoothCallback.rawGestureDataCB(rawGestureData);
                    //Clear all the data:
                    rawGestureData = new int[30][6];
                    appendedBTMessage = new StringBuilder(20);
                    rowCounter = 0;
                }
            }
        }
    };

    public BluetoothHandler(MainActivity activityContext, BTCallback bluetoothCallback) {
        Log.d(TAG, "BTHandler started...");
        this.bluetoothCallback = bluetoothCallback;
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Log.d(TAG, "Device does not support BT");
            return;
        }

        if (!btAdapter.isEnabled()) {
            //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //(activityContext).startActivityForResult(enableBtIntent, 0);
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
            Log.d(TAG, "Device G6 wasn't paired.");
            return;
        }
        connectThread = new ConnectThread(btDevice);
        connectThread.start();
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        public ConnectThread(BluetoothDevice device) {
            Log.d(TAG, "Starting ConnectThread...");
            BluetoothSocket tmp = null;
            mmDevice = device;
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

    private class ReadAndWriteThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ReadAndWriteThread(BluetoothSocket socket) {
            Log.d(TAG, "ReadAndWriteThread started...");
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
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    bluetoothIn.obtainMessage(1, bytes, -1, readMessage).sendToTarget();
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

}
