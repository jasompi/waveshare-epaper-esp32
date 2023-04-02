package com.waveshare.epaperesp32loader.communication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.NonNull;

import com.waveshare.epaperesp32loader.ScanningActivity;

/**
 * Created by YiWan on 8/19/2018.
 */

/**
 * <h1>Bluetooth scanner</h1>
 * The class provides functions for receiving bluetooth broadcast messages
 * (bluetooth is on\off, discovering is on\off, new device found),
 * requesting ACCESS_COARSE_LOCATION permission and devices scanning.
 *
 * @author  Waveshare team
 * @version 1.0
 * @since   8/19/2018
 */

public class BluetoothScanner
{
    // Bluetooth scanning objects
    //-----------------------------
    private static BluetoothAdapter           btAdapter;
    private static BluetoothBroadcastReceiver btReceiver;
    private static ScanningHandler            btHandler;

    // Keys of scanning events
    //-----------------------------
    public static final int BT_SCANNING_ON  = 2;
    public static final int BT_SCANNING_OFF = 3;
    public static final int BT_IS_CHANGED   = 4;
    public static final int BT_DEVICE_FOUND = 5;

    //--------------------------------------
    // Permission
    //--------------------------------------
    private static PermissionHelper                    permissionHelper;
    private static PermissionHelper.PermissionResponse permissionResponse;

    //---------------------------------------------------------
    //  Scanning message handler
    //---------------------------------------------------------
    public interface ScanningHandler
    {
        void handleMessage(int cmnd, Intent data);
    }

    //---------------------------------------------------------
    //  Broadcast receiver of bluetooth adapter events:
    //
    //    ACTION_STATE_CHANGED      - bluetooth on/off
    //    ACTION_DISCOVERY_STARTED  - scanning on
    //    ACTION_DISCOVERY_FINISHED - scanning off
    //    ACTION_FOUND              - some device is found
    //---------------------------------------------------------
    private static class BluetoothBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_FOUND))
                btHandler.handleMessage(BT_DEVICE_FOUND, intent);

            else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
                btHandler.handleMessage(BT_IS_CHANGED, intent);

            else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
                btHandler.handleMessage(BT_SCANNING_ON, null);

            else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
                btHandler.handleMessage(BT_SCANNING_OFF, null);
        }
    }

    //---------------------------------------------------------
    //  Tries to start scanning process
    //    Returns true if the starting is successful
    //    Returns false if the starting needs permission
    //---------------------------------------------------------
    public static boolean initialize(Activity activity)
    {
        // Permission required for scanning
        //-----------------------------------------------------
        permissionHelper = new PermissionHelper(activity);

        //-----------------------------------------------------
        //  Start the scanning if it is granted
        //
        //    This class exemplar is analog of C#'s delegate.
        //    It wraps invoke function and is stored in the
        //    permissionHelper, just because function invoke
        //    can't be stored as a variable in Java.
        //-----------------------------------------------------
        permissionResponse = new PermissionHelper.PermissionResponse()
        {
            // It must be executed if the permission is granted
            //-------------------------------------------------
            @Override
            public void invoke()
            {
                // Start the scanning of bluetooth devices
                //---------------------------------------------
                btAdapter.startDiscovery();
            }
        };

        // Permission of ACCESS_COARSE_LOCATION
        //--------------------------------------
        permissionHelper.setResponse(PermissionHelper.REQ_BLUE, permissionResponse);

        // Get available bluetooth adapter
        //-----------------------------------------------------
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // Exit, if the adapter is not available
        //-----------------------------------------------------
        if (btAdapter == null) return false;

        // Message handler
        //-----------------------------------------------------
        btHandler = (ScanningHandler)activity;

        // Broadcast receiver of bluetooth events
        //-----------------------------------------------------
        btReceiver = new BluetoothBroadcastReceiver();

        //-----------------------------------------------------
        //  Intent filter for bluetooth events:
        //
        //    ACTION_STATE_CHANGED      - bluetooth on/off
        //    ACTION_DISCOVERY_STARTED  - scanning on
        //    ACTION_DISCOVERY_FINISHED - scanning off
        //    ACTION_FOUND              - some device is found
        //-----------------------------------------------------
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        activity.registerReceiver(btReceiver, intentFilter);

        //-----------------------------------------------------
        //  Turn bluetooth on if it isn't on yet
        //
        //    In fact, if btAdapter is off then following
        //    command sends request ACTION_REQUEST_ENABLE
        //    to turn on btAdapter and the broadcast receiver
        //    gets STATE_TURNING_ON message, which means
        //    the adapter started the turning on process.
        //    The scanning routine is interesting in STATE_ON
        //    and STATE_OFF messages only and ignores others.
        //-----------------------------------------------------
        if (!btAdapter.isEnabled()) activity.startActivity(
                new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));

        return true;
    }

    //---------------------------------------------------------
    //  Checks if the device is scanning now
    //---------------------------------------------------------
    public static boolean isScanning()
    {
        return (btAdapter != null) && btAdapter.isDiscovering();
    }

    //---------------------------------------------------------
    //  Tries to start scanning process
    //    Returns true if the starting is successful
    //    Returns false if the scanning needs permission
    //---------------------------------------------------------
    public static boolean startScanning()
    {
        if (btAdapter.isDiscovering())
            return true;

        else if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.M) ||
                permissionHelper.sendRequestPermission(PermissionHelper.REQ_BLUE))
            permissionResponse.invoke();

        return false;
    }

    //---------------------------------------------------------
    //  Stops scanning process and unregisters receiver
    //---------------------------------------------------------
    public static void stopScanning(Activity activity, boolean unregisterReceiver)
    {
        try
        {
            // Stop getting messages from broadcast receiver
            //-------------------------------------------------
            if (unregisterReceiver)
                activity.unregisterReceiver(btReceiver);

            // Stop scanning bluetooth devices
            //-------------------------------------------------
            if ((btAdapter != null) && btAdapter.isDiscovering())
                btAdapter.cancelDiscovery();
        }
        catch (Exception e)
        {
        }
    }

    //---------------------------------------------------------
    //  Sends request of scanning permission
    //---------------------------------------------------------
    public static void sendRequestPermission()
    {
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.M) ||
            permissionHelper.sendRequestPermission(PermissionHelper.REQ_BLUE))
        permissionResponse.invoke();
    }

    //---------------------------------------------------------
    //  Request's result of the scanning permission
    //---------------------------------------------------------
    public static void onRequestPermissionsResult(
            // Expected arguments:
            int requestCode,               // PermissionHelper.REQ_BLUE
            @NonNull String[] permissions, // Manifest.permission.ACCESS_COARSE_LOCATION
            @NonNull int[] grantResults)   // PackageManager.PERMISSION_GRANTED
    {
        permissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
