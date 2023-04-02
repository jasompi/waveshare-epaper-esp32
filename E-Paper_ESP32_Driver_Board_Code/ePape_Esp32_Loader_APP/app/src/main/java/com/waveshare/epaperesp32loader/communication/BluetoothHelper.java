package com.waveshare.epaperesp32loader.communication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * <h1>Bluetooth helper</h1>
 * The class provides functions for bluetooth device
 * input\output data transfer by BluetoothSocket class
 *
 * @author  Waveshare team
 * @version 1.0
 * @since   8/18/2018
 */

public class BluetoothHelper
{
    // This constant is used for creating bluetooth socket
    //---------------------------------------------------------
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Bluetooth
    //---------------------------------------------------------
    private static BluetoothAdapter btAdapter;
    private static BluetoothSocket  btSocket;
    private static BluetoothDevice  btDevice;
    private static String           btAddress;
    public  static ConnectedThread  btThread;
    private static Handler          btHandler;


    // Keys of events
    //---------------------------------------------------------
    public static final int BT_FATAL_ERROR  = 1;
    public static final int BT_STATE_IS_OFF = 2;
    public static final int BT_RECEIVE_DATA = 3;

    // Initialisation/restart the upload activity
    //---------------------------------------------------------
    public static boolean initialize(BluetoothDevice device, Handler handler)
    {
        btDevice  = device;
        btAddress = device.getAddress();
        btHandler = handler;
        return checkState();
    }

    // Initialisation bluetooth socket in its listening thread
    //---------------------------------------------------------
    public static boolean connect()
    {
        // If it is already connected
        //-----------------------------------------------------
        if ((btSocket != null) && (btSocket.isConnected())) return true;

        // Prepare the bluetooth device for connection
        //-----------------------------------------------------
        if (!checkState()) return false;
        BluetoothDevice device = btAdapter.getRemoteDevice(btAddress);

        // Trying to create bluetooth socket
        //-----------------------------------------------------
        try
        {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        }
        catch (IOException e)
        {
            btHandler.obtainMessage(BT_FATAL_ERROR);
            return false;
        }

        // Trying to make connection by the bluetooth socket
        //-----------------------------------------------------
        try
        {
            btSocket.connect();
        }
        catch (IOException e)
        {
            close();
            return false;
        }

        // Create and start a thread listening the socket
        //-----------------------------------------------------
        btThread = new ConnectedThread(btSocket, btHandler);
        btThread.start();

        return true;
    }

    // Trying to close the bluetooth socket
    //---------------------------------------------------------
    public static boolean close()
    {
        try
        {
            btSocket.close();
            return true;
        }
        catch (IOException e2)
        {
            btHandler.obtainMessage(BT_FATAL_ERROR);
            return false;
        }
    }

    // Checks the bluetooth adapter is on
    //---------------------------------------------------------
    public static boolean checkState()
    {
        if ((btAdapter = BluetoothAdapter.getDefaultAdapter()) == null)
        {
            btHandler.obtainMessage(BT_FATAL_ERROR);
            return false;
        }

        if (!btAdapter.isEnabled())
        {
            btHandler.obtainMessage(BT_STATE_IS_OFF);
            return false;
        }

        return true;
    }

    // Thread where input\output streams of socket are handled
    //---------------------------------------------------------
    public static class ConnectedThread extends Thread
    {
        private final Handler      mmHandler;
        private final InputStream  mmInStream;
        private final OutputStream mmOutStream;

        public static boolean keepConnection = false;


        public ConnectedThread(BluetoothSocket socket, Handler handler)
        {
            mmHandler = handler;

            InputStream  tmpIn  = null;
            OutputStream tmpOut = null;

            // Getting socket's streams
            //-------------------------------------------------
            try
            {
                tmpIn  = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                tmpOut.flush();
            }
            catch (IOException e) { }

            mmInStream  = tmpIn;
            mmOutStream = tmpOut;

        }

        // Reading data from the input stream into a buffer
        // and sending it to message handler
        //-----------------------------------------------------
        public void run()
        {
            byte[] buffer = new byte[256];

            while (true)
            {
                try
                {
                    mmHandler.obtainMessage(BluetoothHelper.BT_RECEIVE_DATA,
                        mmInStream.read(buffer), -1, buffer).sendToTarget();
                }
                catch (IOException e)
                {
                    if (keepConnection)
                    {
                        try
                        {
                            Thread.sleep(1000);
                            close();
                            connect();
                        }
                        catch (InterruptedException internetExplorer)
                        {
                        }
                    }
                    else break;
                }
            }
        }

        // Writing of string into the output stream
        //-----------------------------------------------------
        public boolean write(byte[] mass, int size)
        {
            try
            {
                //mmOutStream.flush();
                mmOutStream.write(mass, 0, size);
                return true;
            }
            catch (IOException e)
            {
                return false;
            }
        }
    }
}