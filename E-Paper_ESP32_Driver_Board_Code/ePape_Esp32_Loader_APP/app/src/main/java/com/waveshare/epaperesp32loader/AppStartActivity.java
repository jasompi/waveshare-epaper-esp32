package com.waveshare.epaperesp32loader;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
//import android.icu.text.StringPrepParseException;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;


import com.waveshare.epaperesp32loader.communication.PermissionHelper;
import com.waveshare.epaperesp32loader.image_processing.EPaperDisplay;

/*
    * <h1>Main activity</h1>
    * The main activity. It leads the steps of e-Paper image uploading:
    * 1. Open Wi-fi or Bluetooth adapter
    * 2. Select the image file
    * 3. Select the type of the display
    * 4. Select the method of pixel format converting
    * 5. Start uploading
    *
    * @author  Waveshare team
    * @version 1.0
    * @since   8/16/2018
*/

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AppStartActivity extends AppCompatActivity
{
    // Request codes
    //-----------------------------
    public static final int REQ_BLUETOOTH_CONNECTION = 2;
    public static final int REQ_OPEN_FILE            = 3;
    public static final int REQ_DISPLAY_SELECTION    = 4;
    public static final int REQ_PALETTE_SELECTION    = 5;
    public static final int REQ_UPLOADING            = 6;

    // Image file name and path
    //-----------------------------
    public static String fileName;
    public static String filePath;

    // Views
    //-----------------------------
    public TextView textBlue;
    public TextView textLoad;
    public TextView textDisp;
    public TextView textFilt;
    public TextView textSend;
//    public TextView textAddr;
    public Button button_file;

    public ImageView pictFile; // View of loaded image
    public ImageView pictFilt; // View of filtered image
    Log log ;
    // Data
    //-----------------------------
    public static Bitmap originalImage; // Loaded image with original pixel format
    public static Bitmap indTableImage; // Filtered image with indexed colors

    // Device
    //-----------------------------
    public static BluetoothDevice btDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_start_activity);

        // Image file name (null by default)
        //-----------------------------------------------------
        fileName = null;



        // Image file path (external storage root by default)
        //-----------------------------------------------------
        filePath = Environment.getExternalStorageDirectory().getAbsolutePath();

        // Views
        //-----------------------------------------------------
        textBlue = findViewById(R.id.text_blue);
        textLoad = findViewById(R.id.text_file);
        textDisp = findViewById(R.id.text_disp);
        textFilt = findViewById(R.id.text_filt);
        textSend = findViewById(R.id.text_send);

        pictFile = findViewById(R.id.pict_file);
        pictFilt = findViewById(R.id.pict_filt);
        button_file = findViewById(R.id.Button_file);
        // Data
        //-----------------------------
        originalImage = null;
        indTableImage = null;
        button_file.setEnabled(false);

    }

    public void onScan(View view)
    {
        // Open bluetooth devices scanning activity
        //-----------------------------------------------------
        startActivityForResult(
            new Intent(this, ScanningActivity.class),
            REQ_BLUETOOTH_CONNECTION);
    }

    public void onLoad(View view)
    {
        // Opening file browser in recently opened folder
        //-----------------------------------------------------
        /*
        startActivityForResult(
                new Intent(this, OpenFileActivity.class),
                REQ_OPEN_FILE);
        */

        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setFixAspectRatio(true)
                .setAspectRatio(EPaperDisplay.getDisplays()[EPaperDisplay.epdInd].width, EPaperDisplay.getDisplays()[EPaperDisplay.epdInd].height)
                .start(this);

    }

    public void onDisplay(View view)
    {
        // Open display selection activity
        //-----------------------------------------------------
        startActivityForResult(
                new Intent(this, DisplaysActivity.class),
                REQ_DISPLAY_SELECTION);


    }

    public void onFilter(View view)
    {
        // Check if any image is loaded
        //-----------------------------------------------------
        if (originalImage == null) PermissionHelper.note(this, R.string.no_pict);

        // Check if any display is selected
        //-----------------------------------------------------
        else if (EPaperDisplay.epdInd == -1) PermissionHelper.note(this, R.string.no_disp);

        // Open palette selection activity
        //-----------------------------------------------------
        else startActivityForResult(
                new Intent(this, FilteringActivity.class),
                REQ_PALETTE_SELECTION);
    }

    public void onUpload(View view)
    {
        // Check if any devices is found
        //-----------------------------------------------------
        if (btDevice == null) PermissionHelper.note(this, R.string.no_blue);

        // Check if any palette is selected
        //-----------------------------------------------------
        else if (indTableImage == null) PermissionHelper.note(this, R.string.no_filt);

        // Open uploading activity
        //-----------------------------------------------------
        else startActivityForResult(
            new Intent(this, UploadActivity.class),
            REQ_UPLOADING);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        //-----------------------------------------------------
        //  Messages form ScanningActivity
        //-----------------------------------------------------
        if (requestCode == REQ_BLUETOOTH_CONNECTION)
        {
            // Bluetooth device was found and selected
            //-------------------------------------------------
            if (resultCode == RESULT_OK)
            {
                // Get selected bluetooth device
                //---------------------------------------------
                btDevice = data.getParcelableExtra("DEVICE");

                // Show name and address of the device
                //---------------------------------------------
                textBlue.setText(btDevice.getName() + " (" + btDevice.getAddress() + ")");
            }
        }

        //-----------------------------------------------------
        //  Message form open file activity
        //-----------------------------------------------------
        else if (requestCode == REQ_OPEN_FILE)
        {
            if (resultCode == RESULT_OK)
            {
                // Getting file name and file path
                //---------------------------------------------
                textLoad.setText(fileName);

                try
                {
                    // Loading of the selected file
                    //---------------------------------------------
                    InputStream is = new FileInputStream(filePath + "/" + fileName);
                    originalImage  = (new BitmapDrawable(is)).getBitmap();

                    int pictSize = textLoad.getWidth();
                    pictFile.setMaxHeight(pictSize);
                    pictFile.setMinimumHeight(pictSize / 2);
                    pictFile.setImageBitmap(originalImage);
                }
                catch(Exception e)
                {
                    textFilt.setText(R.string.failed_file);
                }
            }
        }

        //-----------------------------------------------------
        //  Message form display selection activity
        //-----------------------------------------------------
        else if (requestCode == REQ_DISPLAY_SELECTION)
        {
            if (resultCode == RESULT_OK) {
                textDisp.setText(EPaperDisplay.getDisplays()[EPaperDisplay.epdInd].title);
                button_file.setEnabled(true);
            }
        }

        //-----------------------------------------------------
        //  Message form palette selection activity
        //-----------------------------------------------------
        else if (requestCode == REQ_PALETTE_SELECTION)
        {
            if (resultCode == RESULT_OK)
            {
                textFilt.setText(data.getStringExtra("NAME"));

                try
                {
                    int size = textLoad.getWidth();
                    pictFilt.setMaxHeight(size);
                    pictFilt.setMinimumHeight(size / 2);
                    pictFilt.setImageBitmap(indTableImage);
                }
                catch(Exception e)
                {
                    textFilt.setText(R.string.failed_filt);
                }
            }
        }else if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            File temp = new File(Environment.getExternalStorageDirectory() .getAbsolutePath()+ "/Android/data/" + getPackageName()
                    + "/" + System.currentTimeMillis() + ".png");
            String lastBipmapName;
            lastBipmapName=temp.getName();
            //log.e(" "," " +lastBipmapName);
            if (resultCode == RESULT_OK) {
                Uri contentURI = result.getUri();
                log.e(" ", " "+contentURI);
                try {
                    bmp_raw = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentURI);
                    FileOutputStream fos = new FileOutputStream(temp);
                    bmp_raw.compress(Bitmap.CompressFormat.PNG, 30, fos);
                    fos.flush();
                    fos.close();



                    log.e("getHeight "," "+ bmp_raw.getHeight());
                    log.e("getWidth "," "+ bmp_raw.getWidth());

                    log.e("getHeight 1"," "+ EPaperDisplay.getDisplays()[EPaperDisplay.epdInd].width);
                    log.e("getWidth 1"," "+ EPaperDisplay.getDisplays()[EPaperDisplay.epdInd].height);
                    Matrix matrix = new Matrix();

                    bmp_raw = Bitmap.createScaledBitmap(bmp_raw, EPaperDisplay.getDisplays()[EPaperDisplay.epdInd].width, EPaperDisplay.getDisplays()[EPaperDisplay.epdInd].height, false);
                    originalImage = bmp_raw;
                    textLoad.setText(lastBipmapName);
                    int pictSize = bmp_raw.getWidth();
                    pictFile.setMaxHeight(pictSize);
                    pictFile.setMinimumHeight(pictSize / 2);
                    pictFile.setImageBitmap(bmp_raw);
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {

                Exception error = result.getError();
            }

            //SetOrGetInfo.getInstance().setLastPicName(this,"lastBipmapName",lastBipmapName);
        }
    }
    public  Bitmap bmp_raw;
}
