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
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.project.iotap.iotap.Activities.MainActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Anton on 2017-11-29.
 * Class for handling bluetooth connection.
 *
 *
 *
 *
 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<DOESN'T WORK!!!!
 *
 */

public class OldBTNotUsed {
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private ConnectedThread mConnectedThread = null;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //"name" of this device.
    private static final String TAG = "BluetoothTag";

    //Receives messages and prints them to the logcat. 
    private static Handler messageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            StringBuilder sb = new StringBuilder();
            switch (msg.what) {
                case 1:
                    byte[] readBuf = (byte[]) msg.obj;
                    String strIncom = new String(readBuf, 0, msg.arg1);
                    sb.append(strIncom);
                    int endOfLineIndex = sb.indexOf("\r\n");
                    if (endOfLineIndex > 0) {
                        sb.delete(0, sb.length());
                    }
                    Log.d(TAG, "...String:" + sb.toString() + "Byte:" + msg.arg1 + "..."); //Should output data from the connected device.
                    break;
            }
        }
    };

    /**
     * Constructor that checks if there is a bluetooth adapter and asks the user to enable if it is off.
     * TODO: Decide on how to handle BT connection if it's already on or off, left some alternatives in the code.
     */
    public OldBTNotUsed(Context activityContext) {
        Log.d(TAG, "Created OldBTNotUsed...");
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
            /*
            //Not sure if we actually need this, depends on how the motionsensor work.
            btAdapter.disable();
            Toast.makeText(activityContext, "BT was on, resetting it...", Toast.LENGTH_LONG).show();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            btAdapter.enable();
*/
        } else {
            Toast.makeText(activityContext, "BT was off, starting...", Toast.LENGTH_LONG).show();
            btAdapter.enable();
        }
    }

    //The BroadcastReceiver that listens for bluetooth broadcasts. When a device is connected, this will execute. Here we could check for name, id etc.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Toast.makeText(context, device.getName() + " is now Connected!", Toast.LENGTH_LONG).show();
                if (device.getName().equals("MotionSensor")) {
                    //Do stuff. For now I will just ignore this.
                }

                try {
                    btSocket = createBluetoothSocket(device);
                    Log.d(TAG, "...Socket created...");
                } catch (IOException e) {
                    Log.d(TAG, "Socket create failed: " + e.getMessage());
                }

                Log.d(TAG, "...Connecting...");
                try {
                    btSocket.connect();
                    Log.d(TAG, "...OK...\n...Creating data stream...");
                    mConnectedThread = new ConnectedThread(btSocket);
                    mConnectedThread.start();
                } catch (IOException e) {
                    try {
                        btSocket.close();
                        Log.d(TAG, "Socket couldn't connect!"); //<----------------------------------------------------PROBLEM!!!!!!!!!!!!!!!!!!!!!
                    } catch (IOException e2) {
                        Log.d(TAG, " unable to close socket during connection failure" + e2.getMessage());
                    }
                }
            }
        }
    };

    //Not sure what this actually does.
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[]{UUID.class});
            return (BluetoothSocket) m.invoke(device, MY_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection", e);
        }
        return device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    //Opens a data stream to the bluetooth connected device.
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occured when establishing streams " + e.getMessage());
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);        // Get number of bytes and message in "buffer"
                    messageHandler.obtainMessage(1, bytes, -1, buffer).sendToTarget();     // Send to message queue Handler
                    Log.e(TAG, "Got some data...");
                } catch (IOException e) {
                    Log.e(TAG, "Error occured when reading from input stream: " + e.getMessage());
                    break;
                }
            }
        }
    }
}
