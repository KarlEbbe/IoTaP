package com.project.iotap.iotap.Bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
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

    private final String BT = "Bluetooth";
    private final BluetoothAdapter btAdapter;

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
    public BluetoothHandler(Context applicationContext) {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Toast.makeText(applicationContext, "This device is missing BT adapter", Toast.LENGTH_LONG).show();
        } else if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ((Activity) applicationContext).startActivityForResult(enableBtIntent, 0);
        } else {
            startTransfer();
        }
    }

    /**
     * Starts a thread for connecting via bt.
     */
    public void startTransfer() {
        Log.d(BT, "STARTED TRANSFER!");
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        String name = btAdapter.getName();
        Log.d(BT, name);

        BluetoothDevice device = null;
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice pairedDevice : pairedDevices) {
                device = pairedDevice;
                Log.d(BT, device.getName());
            }
        }
        ConnectThread mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }

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
            while (true) {
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
}
