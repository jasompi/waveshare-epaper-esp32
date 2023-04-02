package com.waveshare.epaperesp32loader;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.waveshare.epaperesp32loader.communication.BluetoothHelper;
import com.waveshare.epaperesp32loader.communication.BluetoothScanner;
import com.waveshare.epaperesp32loader.communication.PermissionHelper;

import java.util.ArrayList;

/**
 * <h1>Scanning activity</h1>
 * The activity offers to select one of available bluetooth devices.
 *
 * @author  Waveshare team
 * @version 1.0
 * @since   8/11/2018
 */


public class ScanningActivity extends AppCompatActivity implements BluetoothScanner.ScanningHandler
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scanning_activity);
        getSupportActionBar().setTitle(R.string.scan);

        // Device list
        //-------------------------
        deviceList = findViewById(R.id.device_list);
        deviceList.setAdapter(btListAdapter = new ListAdapter());
        deviceList.setOnItemClickListener(new OnDevListItemClick());

        // Bluetooth scanner
        //-------------------------
        if (!BluetoothScanner.initialize(this))
        {
            Intent intent = new Intent();
            intent.putExtra("COMMENT", "Bluetooth is not supported on this device");

            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        BluetoothScanner.stopScanning(this, false);
    }

    @Override
    protected void onPause()
    {
        BluetoothScanner.stopScanning(this, false);
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        BluetoothScanner.stopScanning(this, true);
        super.onDestroy();
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        BluetoothScanner.stopScanning(this, true);
    }


    //------------------------------------------
    //  Result of permission request
    //------------------------------------------
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        BluetoothScanner.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //---------------------------------------------------------
    //  Main menu
    //---------------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.scanning_menu, menu);

        if (BluetoothScanner.isScanning())
        {
            menu.findItem(R.id.menu_item_ring).setActionView(R.layout.scanning_ring);
            menu.findItem(R.id.menu_item_scan).setVisible(false);
            menu.findItem(R.id.menu_item_stop).setVisible(true);
        }
        else
        {
            menu.findItem(R.id.menu_item_ring).setActionView(null);
            menu.findItem(R.id.menu_item_scan).setVisible(true);
            menu.findItem(R.id.menu_item_stop).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        switch (id)
        {
            // Start scanning
            //-------------------------------------------------
            case R.id.menu_item_scan:
                // If scanning is started
                //---------------------------------------------
                if (BluetoothScanner.startScanning())
                    // Just update option menu's view
                    //-----------------------------------------
                    invalidateOptionsMenu();

                // ... otherwise send request permission
                //---------------------------------------------
                else BluetoothScanner.sendRequestPermission();
            break;

            // Stop scanning and update menu
            //-------------------------------------------------
            case R.id.menu_item_stop:
                BluetoothScanner.stopScanning(this, false);
                invalidateOptionsMenu();
            break;
        }

        return super.onOptionsItemSelected(item);
    }

    //---------------------------------------------------------
    // Device list
    //---------------------------------------------------------
    private ListView deviceList;
    private ListAdapter btListAdapter;

    static class ViewHolder
    {
        public TextView text;
        public TextView addr;
    }

    private class ListAdapter extends BaseAdapter
    {
        private ArrayList<BluetoothDevice> btDevices;

        public ListAdapter()
        {
            super();
            btDevices = new ArrayList<>();
        }

        public void addDevice(BluetoothDevice device)
        {
            btDevices.add(device);
        }

        public BluetoothDevice getDevice(int position)
        {
            return btDevices.get(position);
        }

        public void clear()
        {
            btDevices.clear();
        }

        public boolean contains(BluetoothDevice device)
        {
            // The device is already in the list
            //-------------------------------------------------
            if (btDevices.contains(device))
                return true;

            // Every device must have unique MAC address
            //-------------------------------------------------
            for (int i = 0; i < btDevices.size(); i++)
                if (btDevices.get(i).getAddress().equals(device.getAddress()))
                    return true;

            return false;
        }

        @Override
        public int getCount()
        {
            return btDevices.size();
        }

        @Override
        public Object getItem(int i)
        {
            return btDevices.get(i);
        }

        @Override
        public long getItemId(int i)
        {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup)
        {
            ViewHolder viewHolder;

            if (view == null)
            {
                view = ScanningActivity.this.getLayoutInflater().inflate(R.layout.scanning_list_item, null);
                viewHolder = new ViewHolder();
                viewHolder.text = view.findViewById(R.id.bt_dev_name);
                viewHolder.addr = view.findViewById(R.id.bt_dev_addr);
                view.setTag(viewHolder);
            }
            else viewHolder = (ScanningActivity.ViewHolder) view.getTag();

            BluetoothDevice device = btDevices.get(i);
            String name = device.getName();
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) name += " (BONDED)";

            viewHolder.text.setText((name != null) && (name.length() > 0) ? name : "unknown");
            viewHolder.addr.setText(device.getAddress());
            return view;
        }
    }

    private class OnDevListItemClick implements AdapterView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            // Stop scanning and broadcast receiving
            //-------------------------------------------------
            BluetoothScanner.stopScanning(ScanningActivity.this, true);

            // Return name and address of the selected device
            //-------------------------------------------------
            Intent intent = new Intent();
            intent.putExtra("DEVICE", btListAdapter.getDevice(position));
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    //---------------------------------------------------------
    //  Scanning message handler
    //---------------------------------------------------------
    public void handleMessage(int cmd, Intent data)
    {
        runOnUiThread(new ScanHandler(cmd, data));
    }

    private class ScanHandler implements Runnable
    {
        public int cmd;
        public Intent data;

        public ScanHandler(int cmd, Intent data)
        {
            this.cmd = cmd;
            this.data = data;

        }
        @Override
        public void run()
        {
            BluetoothDevice device;
            int state;

            switch (cmd)
            {
                case BluetoothScanner.BT_DEVICE_FOUND:

                    // Found bluetooth device
                    //-----------------------------------------
                    device = data.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    // Check if the device is unique
                    //-----------------------------------------
                    if (btListAdapter.contains(device)) break;

                    // Add the device into devices list
                    //-----------------------------------------
                    btListAdapter.addDevice(device);

                    // Update devices list's view
                    //-----------------------------------------
                    btListAdapter.notifyDataSetChanged();
                    break;

                case BluetoothScanner.BT_SCANNING_ON:
                    // Update the options menu's view
                    //-----------------------------------------
                    invalidateOptionsMenu();

                    // Clear devices list
                    //-----------------------------------------
                    btListAdapter.clear();

                    // Update devices list's view
                    //-----------------------------------------
                    btListAdapter.notifyDataSetChanged();
                    break;

                case BluetoothScanner.BT_SCANNING_OFF:
                    // Just update the options menu's view
                    //-----------------------------------------
                    invalidateOptionsMenu();
                    break;

                case BluetoothScanner.BT_IS_CHANGED:
                    // Current state of the bluetooth adapter
                    //-----------------------------------------
                    state = data.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);

                    // Update the options menu's view
                    // if the adapter is turned off
                    //-----------------------------------------
                    if (state == BluetoothAdapter.STATE_OFF)
                        invalidateOptionsMenu();
                    break;

                default:
                    break;
            }
        }
    }
}