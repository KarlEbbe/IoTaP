package com.project.iotap.iotap.Bluetooth;

import android.bluetooth.BluetoothAdapter;

import com.project.iotap.iotap.Activities.MainActivity;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Anton on 2017-12-08.
 *
 */


public class BluetoothHandler {

    private static final String TAG = "Bluetooth";

    private BluetoothAdapter btAdapter = null;

    private ConnectThread connectThread = null;
    private ReadAndWriteThread readAndWriteThread = null;


    private static Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            byte[] writeBuf = (byte[]) msg.obj;
            int begin = (int) msg.arg1;
            int end = (int) msg.arg2;

            int[] intArray = convertToIntArray((byte[])msg.obj);
            Log.d(TAG, Arrays.toString(intArray));

            switch (msg.what) {
                case 1:
                    String writeMessage = new String(writeBuf);
                    writeMessage = writeMessage.substring(begin, end);
                    break;
            }
        }
    };


    public static int[] convertToIntArray(byte[] input)
    {
        int[] ret = new int[input.length];
        for (int i = 0; i < input.length; i++)
        {
            if(input[i]== 0){
                ret[i] = -1111111111;
            }else{
                ret[i] = input[i];

            }
        }
        return ret;
    }

    public BluetoothHandler(MainActivity activityContext) {
        Log.d(TAG, "BTHandler started...");
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Log.d(TAG, "Device does not support BT");
            return;
        }

        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            (activityContext).startActivityForResult(enableBtIntent, 0);
        }

        BluetoothDevice btDevice = null;
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();


        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if(device.getName().equals("G6")){
                    btDevice = device;
                }
            }
        }

        if(btDevice == null){
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
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.d(TAG, "Now connected and ready to read:");
            byte[] buffer = new byte[1024];
            int begin = 0;
            int bytes = 0;
            while (true) {
                try {
                    bytes += mmInStream.read(buffer, bytes, buffer.length - bytes);
                    for (int i = begin; i < bytes; i++) {

                        if (buffer[i] == "h".getBytes()[0]) {
                            Log.d(TAG, "small 'h' detected!");

                            mHandler.obtainMessage(1, begin, i, buffer).sendToTarget();
                            begin = i + 1;
                            if (i == bytes - 1) {
                                bytes = 0;
                                begin = 0;
                                Log.d(TAG,"Old reset");
                            }
                            if(i > 0){

                            }
                        }

                        //Log.d(TAG, String.valueOf(buffer[i]));
                    }
                    //buffer = new byte[1024];
                } catch (IOException e) {
                    Log.d(TAG, "Error when reading from inputStream" + e.getMessage());
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
