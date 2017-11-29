package com.project.iotap.iotap.Bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Anton on 2017-11-29.
 * Class for handling bluetooth connection.
 */

public class BluetoothHandler {

    private final BluetoothAdapter btAdapter;
    private boolean running = false;

    private static final Handler messageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            byte[] writeBuffer = (byte[]) msg.obj;
            int begin = msg.arg1;
            int end = msg.arg2;
            switch (msg.what) {
                case 1:
                    String writeMessage = new String(writeBuffer);
                    writeMessage = writeMessage.substring(begin, end);
                    break;
            }
        }
    };

    /**
     * Constructor that checks if there is a bluetooth adapter and asks the user to enable if it is off.
     */
    public BluetoothHandler(Context activityContext) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        activityContext.registerReceiver(mReceiver, filter);
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        //Got some alternatives here, either we let the user turn it off or we do it in code.
       /*
        if (btAdapter == null) {
            Toast.makeText(activityContext, "This device is missing BT adapter", Toast.LENGTH_LONG).show();
        } else if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ((Activity) activityContext).startActivityForResult(enableBtIntent, 0);
        }
        */

        if (btAdapter == null) {
            Toast.makeText(activityContext, "This device is missing BT adapter", Toast.LENGTH_LONG).show();
        } else if (btAdapter.isEnabled()) {
            //Not sure if we actually need this, depends on how the motionsensor work.
            btAdapter.disable();
            Toast.makeText(activityContext, "BT was on, resetting it...", Toast.LENGTH_LONG).show();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            btAdapter.enable();
        } else {
            Toast.makeText(activityContext, "BT was off, starting...", Toast.LENGTH_LONG).show();
            btAdapter.enable();
        }
    }

    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //Device found
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //Device is now connected
                Toast.makeText(context, device.getName() + " is now Connected!", Toast.LENGTH_LONG).show();
                if (device.getName().equals("MotionSensor")) {
                    //Here we can check the name or address of the device.
                    //If it matches our sensor, we should start to try and receive data from it!
                    //For now, I will just start the connect thread outside this if statement for testing purposes:
                }
                ConnectThread mConnectThread = new ConnectThread(device);
                mConnectThread.start();

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //Done searching
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                //Device is about to disconnect
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //Device has disconnected
            }
        }
    };

    /**
     * Thread that handles connection to a device.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmpSocket = null;
            try {
                tmpSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException ignored) {
            }
            mmSocket = tmpSocket;
        }

        public void run() {
            btAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                }
                return;
            }

            running = true;
            TransferThread mTransferThread = new TransferThread(mmSocket);
            mTransferThread.start();
        }
    }

    /**
     * Thread that handles transfers to a device.
     */
    private class TransferThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public TransferThread(BluetoothSocket socket) {
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
            byte[] buffer = new byte[1024];
            int begin = 0;
            int bytes = 0;
            while (running) {
                try {
                    bytes += mmInStream.read(buffer, bytes, buffer.length - bytes);
                    for (int i = begin; i < bytes; i++) {
                        if (buffer[i] == "#".getBytes()[0]) {
                            messageHandler.obtainMessage(1, begin, i, buffer).sendToTarget();
                            begin = i + 1;
                            if (i == bytes - 1) {
                                bytes = 0;
                                begin = 0;
                            }
                        }
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    public void shutDown() {
        //Should terminate the threads, connection and exit gracefully.
        running = false;
        btAdapter.disable();
    }
}
