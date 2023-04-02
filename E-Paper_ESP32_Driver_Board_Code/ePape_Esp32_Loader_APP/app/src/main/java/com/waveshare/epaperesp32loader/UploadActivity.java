package com.waveshare.epaperesp32loader;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.waveshare.epaperesp32loader.communication.BluetoothHelper;
import com.waveshare.epaperesp32loader.image_processing.EPaperDisplay;

/**
 * <h1>Upload activity</h1>
 * The activity shows the progress of image uploading into display
 * of the selected bluetooth device.
 *
 * @author  Waveshare team
 * @version 1.0
 * @since   8/20/2018
 */

public class UploadActivity extends AppCompatActivity
{
    private TextView textView;
    private SocketHandler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_activity);
        getSupportActionBar().setTitle(R.string.dlg_send);

        // View
        //--------------------------------------
        textView = findViewById(R.id.upload_text);
        textView.setText("Uploading: 0%");

        // Bluetooth helper and its handler
        //--------------------------------------
        BluetoothHelper.initialize(AppStartActivity.btDevice, handler = new SocketHandler());
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // Bluetooth socket connection
        //--------------------------------------
        if (!BluetoothHelper.connect() || !handler.init(AppStartActivity.indTableImage))
        {
            setResult(RESULT_CANCELED);
            finish();
        }
        else textView.setText("Uploading 0 %");
    }

    @Override
    protected void onPause()
    {
        BluetoothHelper.close();
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        BluetoothHelper.close();
        super.onDestroy();
    }

    @Override
    public void onBackPressed()
    {
        BluetoothHelper.close();
        setResult(RESULT_OK);
        finish();
    }

    public void onCancel(View view)
    {
        onBackPressed();
    }

    // Uploaded data buffer
    //---------------------------------------------------------
    private static final int BUFF_SIZE = 256;
    private static byte[]    buffArr = new byte[BUFF_SIZE];
    private static int       buffInd;
    private static int       xLine;
    //---------------------------------------------------------
    //  Socket Handler
    //---------------------------------------------------------
    class SocketHandler extends Handler
    {
        private int   pxInd; // Pixel index in picture
        private int   stInd; // Stage index of uploading
        private int   dSize; // Size of uploaded data by LOAD command
        private int[] array; // Values of picture pixels

        public SocketHandler()
        {
            super();
        }

        // Converts picture pixels into selected pixel format
        // and sends EPDx command
        //-----------------------------------------------------
        private boolean init(Bitmap bmp)
        {
            int w = bmp.getWidth(); // Picture with
            int h = bmp.getHeight();// Picture height
            int epdInd = EPaperDisplay.epdInd;
            array = new int[w*h]; // Array of pixels
            int i = 0;            // Index of pixel in the array of pixels

            // Loading pixels into array
            //-------------------------------------------------

            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++, i++)
                    if(epdInd == 25 || epdInd ==37)
                        array[i] = getVal_7color(bmp.getPixel(x, y));
                    else
                        array[i] = getVal(bmp.getPixel(x, y));

            pxInd = 0;
            xLine = 0;  //2.13inch
            stInd = 0;
            dSize = 0;

            buffInd = 2;                             // Size of command in bytes
            buffArr[0] = (byte)'I';                  // Name of command (Initialize)
            buffArr[1] = (byte)EPaperDisplay.epdInd; // Index of display

            return u_send(false);
        }

        // The function is executed after every "Ok!" response
        // obtained from esp32, which means a previous command
        // is complete and esp32 is ready to get the new one.
        //-----------------------------------------------------
        private boolean handleUploadingStage()
        {
            int epdInd = EPaperDisplay.epdInd;

            // 2.13 e-Paper display
            if ((epdInd == 3) || (epdInd == 39))
            {
                if(stInd == 0) return u_line(0, 0, 100);
                //-------------------------------------------------
                if(stInd == 1) return u_show();
            }

            // 2.13 b V4
            if ((epdInd == 40))
            {
                if(stInd == 0) return u_line(0, 0, 50);
                if(stInd == 1) return u_next();
                if(stInd == 2) return u_line(3, 50, 50);
                if(stInd == 3) return u_show();
            }

            // White-black e-Paper displays
            //-------------------------------------------------
            else if ((epdInd==0)||(epdInd==3)||(epdInd==6)||(epdInd==7)||(epdInd==9)||(epdInd==12)||
                    (epdInd==16)||(epdInd==19)||(epdInd==22)||(epdInd==26)||(epdInd==27)||(epdInd==28))
            {
                if(stInd == 0) return u_data(0,0,100);
                if(stInd == 1) return u_show();
            }

            // 7.5 colored e-Paper displays
            //-------------------------------------------------
            else if (epdInd>15 && epdInd < 22)
            {
                if(stInd == 0) return u_data(-1,0,100);
                if(stInd == 1) return u_show();
            }

            // 5.65f colored e-Paper displays
            //-------------------------------------------------
            else if (epdInd == 25 || epdInd == 37)
            {
                if(stInd == 0) return u_data(-2,0,100);
                if(stInd == 1) return u_show();
            }

            // Other colored e-Paper displays
            //-------------------------------------------------
            else
            {
//                if(stInd==0 && epdInd==23)return u_data(0,0,100);
                if(stInd == 0)return u_data((epdInd == 1)? -1 : 0,0,50);
                if(stInd == 1)return u_next();
                if(stInd == 2)return u_data(3,50,50);
                if(stInd == 3)return u_show();
            }

            return true;
        }

        // Returns the index of color in palette
        //-----------------------------------------------------
        public int getVal(int color)
        {
            int r = Color.red(color);
            int b = Color.blue(color);

            if((r == 0xFF) && (b == 0xFF)) return 1;
            if((r == 0x7F) && (b == 0x7F)) return 2;
            if((r == 0xFF) && (b == 0x00)) return 3;

            return 0;
        }

        // Returns the index of color in palette just for 5.65f e-Paper
        //-----------------------------------------------------
        public int getVal_7color(int color)
        {
            int r = Color.red(color);
            int g = Color.green(color);
            int b = Color.blue(color);

            if((r == 0x00) && (g == 0x00) && (b == 0x00)) return 0;
            if((r == 0xFF) && (g == 0xFF) && (b == 0xFF)) return 1;
            if((r == 0x00) && (g == 0xFF) && (b == 0x00)) return 2;
            if((r == 0x00) && (g == 0x00) && (b == 0xFF)) return 3;
            if((r == 0xFF) && (g == 0x00) && (b == 0x00)) return 4;
            if((r == 0xFF) && (g == 0xFF) && (b == 0x00)) return 5;
            if((r == 0xFF) && (g == 0x80) && (b == 0x00)) return 6;

            return 7;
        }

        // Sends command cmd
        //-----------------------------------------------------
        private boolean u_send(boolean next)
        {
            if (!BluetoothHelper.btThread.write(buffArr, buffInd))
                return false; // Command sending is failed

            if(next) stInd++; // Go to next stage if it is needed
            return true;      // Command is sent successful
        }

        // Next stage command
        //-----------------------------------------------------
        private boolean u_next()
        {
            buffInd = 1;           // Size of command in bytes
            buffArr[0] = (byte)'N';// Name of command (Next)

            pxInd = 0;
            return u_send(true);
        }

        // The finishing command
        //-----------------------------------------------------
        private boolean u_show()
        {
            buffInd = 1;           // Size of command in bytes
            buffArr[0] = (byte)'S';// Name of command (Show picture)

            // Return false if the SHOW command is not sent
            //-------------------------------------------------
            if (!u_send(true)) return false;

            // Otherwise exit the uploading activity.
            //-------------------------------------------------
            return true;
        }

        // Sends pixels of picture and shows uploading progress
        //-----------------------------------------------------
        private boolean u_load(int k1, int k2)
        {
            // Uploading progress message
            //-------------------------------------------------
            String x = "" + (k1 + k2*pxInd/array.length);
            if (x.length() > 5) x = x.substring(0, 5);
            handleUserInterfaceMessage(x);

            // Size of uploaded data
            //-------------------------------------------------
            dSize += buffInd;

            // Request message contains:
            //     data (maximum BUFF_SIZE bytes),
            //     size of uploaded data (4 bytes),
            //     length of data
            //     command name "LOAD"
            //-------------------------------------------------
            buffArr[0] = (byte)'L';

            // Size of packet
            //-------------------------------------------------
            buffArr[1] = (byte)(buffInd     );
            buffArr[2] = (byte)(buffInd >> 8);

            // Data size
            //-------------------------------------------------
            buffArr[3] = (byte)(dSize      );
            buffArr[4] = (byte)(dSize >>  8);
            buffArr[5] = (byte)(dSize >> 16);

            return u_send(pxInd >= array.length);
        }

        // Pixel format converting
        //-----------------------------------------------------
        private boolean u_data(int c, int k1, int k2)
        {
            buffInd = 6; // pixels' data offset

            if(c == -1)
            {
                while ((pxInd < array.length) && (buffInd + 1 < BUFF_SIZE))
                {
                    int v = 0;

                    for(int i = 0; i < 16; i += 2)
                    {
                        if (pxInd < array.length) v |= (array[pxInd] << i);
                        pxInd++;
                    }

                    buffArr[buffInd++] = (byte)(v     );
                    buffArr[buffInd++] = (byte)(v >> 8);
                }
            }
            else if(c == -2)
            {
                while ((pxInd < array.length) && (buffInd + 1 < BUFF_SIZE))
                {
                    int v = 0;

                    for(int i = 0; i < 16; i += 4)
                    {
                        if (pxInd < array.length) v |= (array[pxInd] << i);
                        pxInd++;
                    }

                    buffArr[buffInd++] = (byte)(v     );
                    buffArr[buffInd++] = (byte)(v >> 8);
                }
            }
            else
            {
                while ((pxInd < array.length) && (buffInd < BUFF_SIZE))
                {
                    int v = 0;

                    for (int i = 0; i < 8; i++)
                    {
                        if ((pxInd < array.length) && (array[pxInd] != c)) v |= (128 >> i);
                        pxInd++;
                    }

                    buffArr[buffInd++] = (byte)v;
                }
            }

            return u_load(k1, k2);
        }

        // Pixel format converting (2.13 e-Paper display)
        //-----------------------------------------------------
        private boolean u_line(int c, int k1, int k2)
        {
            buffInd = 6; // pixels' data offset
            while ((pxInd < array.length) && (buffInd < 246))     // 15*16+6 ，16*8 = 128
            {
                int v = 0;

                for (int i = 0; (i < 8) && (xLine < 122); i++, xLine++){
                    if (array[pxInd++] != c) v |= (128 >> i);
                }
                if(xLine >= 122 )xLine = 0;
                buffArr[buffInd++] = (byte)v;
            }
            return u_load(k1, k2);
        }

        //-------------------------------------------
        //  Handles socket message
        //-------------------------------------------
        public void handleMessage(android.os.Message msg)
        {
            // "Fatal error" event
            //-------------------------------------------------
            if (msg.what == BluetoothHelper.BT_FATAL_ERROR)
            {
                setResult(RESULT_CANCELED);
                finish();
            }

            // "Data is received" event
            //-------------------------------------------------
            else if (msg.what == BluetoothHelper.BT_RECEIVE_DATA)
            {
                // Convert data to string
                //---------------------------------------------
                String line = new String((byte[]) msg.obj, 0, msg.arg1);

                // If esp32 is ready for new command
                //---------------------------------------------
                if (line.contains("Ok!"))
                {
                    // Try to handle received data.
                    // If it's failed, restart the uploading
                    //-----------------------------------------
                    if (handleUploadingStage()) return;
                }

                // Exit is the message is unknown
                //---------------------------------------------
                else if (!line.contains("Error!")) return;

                // Otherwise restart the uploading
                //-----------------------------------------
                BluetoothHelper.close();
                BluetoothHelper.connect();
                handler.init(AppStartActivity.indTableImage);
            }
        }
    }

    //---------------------------------------------------------
    //  User Interface Handler
    //---------------------------------------------------------
    public void handleUserInterfaceMessage(String msg)
    {
        runOnUiThread(new UserInterfaceHandler(msg));
    }

    private class UserInterfaceHandler implements Runnable
    {
        public String msg;

        public UserInterfaceHandler(String msg)
        {
            this.msg = "Uploading: " + msg + "%";
        }

        @Override
        public void run()
        {
            textView.setText(msg);
        }
    }
}