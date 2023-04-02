package com.waveshare.epaperesp32loader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.waveshare.epaperesp32loader.communication.PermissionHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * <h1>Open file activity</h1>
 * The activity if a simple file browser.
 * It provides the file choosing within the external storage.
 *
 * @author  Waveshare team
 * @version 1.0
 * @since   8/17/2018
 */

public class OpenFileActivity extends AppCompatActivity implements AdapterView.OnItemClickListener
{
    // Permission
    //--------------------------------------
    private PermissionHelper       permissionHelper;
    private ReadPermissionResponse permissionResponse;

    // Views
    //--------------------------------------
    public TextView textView;
    public ListView listView;

    // Files
    //--------------------------------------
    public File   thisFolder;
    public String externalStorage;

    // File list
    //--------------------------------------
    public ArrayList<File> fileArrayList;
    public FileListAdapter fileListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.open_file_activity);
        getSupportActionBar().setTitle(R.string.file);

        // Permission (READ_EXTERNAL_STORAGE)
        //--------------------------------------
        permissionHelper = new PermissionHelper(this);
        permissionResponse = new ReadPermissionResponse();
        permissionHelper.setResponse(PermissionHelper.REQ_READ, permissionResponse);

        // Views
        //--------------------------------------
        textView = findViewById(R.id.open_file_text);
        listView = findViewById(R.id.open_file_list);

        // File list adapter
        //--------------------------------------
        fileArrayList   = new ArrayList<>();
        fileListAdapter = new FileListAdapter(this, fileArrayList);

        listView.setAdapter(fileListAdapter);
        listView.setOnItemClickListener(this);

        // Set current folder
        //--------------------------------------
        externalStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = AppStartActivity.filePath;

        thisFolder = (fileName == null) || !fileName.startsWith(externalStorage)
                ? Environment.getExternalStorageDirectory()
                : new File(fileName);

        select(thisFolder);
    }

    public void select(File folder)
    {
        // Set folder's file into the permission's response
        //-----------------------------------------------------
        permissionResponse.file = folder;

        // Check permission and run response if it's granted
        //-----------------------------------------------------
        if (permissionHelper.sendRequestPermission(PermissionHelper.REQ_READ))
            permissionResponse.invoke();
    }

    // On coming to parent folder event handler
    //---------------------------------------------------------
    public void onBack(View view)
    {
        // Exit if the external storage root is current folder
        //-----------------------------------------------------
        if (thisFolder.getAbsolutePath().equals(
                Environment.getExternalStorageDirectory().getAbsolutePath())) return;

        // Setup parent folder
        //-----------------------------------------------------
        thisFolder = thisFolder.getParentFile();
        textView.setText(thisFolder.getPath());

        // Update list of files
        //-----------------------------------------------------
        fileArrayList.clear();
        fileArrayList.addAll(Arrays.asList(thisFolder.listFiles()));
        fileListAdapter.notifyDataSetChanged();
    }

    public void onCancel(View view)
    {
        onBackPressed();
    }

    @Override
    public void onBackPressed()
    {
        setResult(RESULT_CANCELED, new Intent());
        finish();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
    {
        select(fileListAdapter.getItem(i));
    }

    //---------------------------------------------------------
    //  File list adapter
    //---------------------------------------------------------
    private class FileListAdapter extends ArrayAdapter<File>
    {
        public FileListAdapter(Context context, ArrayList<File> files)
        {
            super(context, android.R.layout.simple_list_item_1, files);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            // Get list item view and its file
            //-------------------------------------------------
            TextView view = (TextView) super.getView(position, convertView, parent);
            File file = getItem(position);

            // Put the name of the file into its view
            //-------------------------------------------------
            if(view != null) view.setText(file.getName());
            return view;
        }
    }

    //------------------------------------------
    //  Result of permission request
    //------------------------------------------
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        permissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //---------------------------------------------------------
    //  Permission responses
    //---------------------------------------------------------
    class ReadPermissionResponse implements PermissionHelper.PermissionResponse
    {
        public File file;

        @Override
        public void invoke()
        {
            // Check if the file is located in external storage
            //-------------------------------------------------
            if ((file == null) || !file.getAbsolutePath().startsWith(externalStorage))
                file = Environment.getExternalStorageDirectory();

            // Case of file: return its name and path
            //-------------------------------------------------
            if (file.isFile())
            {
                AppStartActivity.fileName = file.getName();
                AppStartActivity.filePath = thisFolder.getAbsolutePath();
                setResult(RESULT_OK, new Intent());
                finish();
            }

            // Case of folder: return its name and path
            //-------------------------------------------------
            else
            {
                String message = "Current location: ";
                message += file.getAbsolutePath().substring(externalStorage.length());
                textView.setText(message);

                // Get items of the folder
                //---------------------------------------------
                File[] files = file.listFiles();
                if (files == null) return;

                // Mark the folder as current one
                //---------------------------------------------
                thisFolder = file;

                // Update file list view
                //---------------------------------------------
                fileArrayList.clear();
                fileArrayList.addAll(Arrays.asList(files));
                fileListAdapter.notifyDataSetChanged();
            }
        }
    }
}