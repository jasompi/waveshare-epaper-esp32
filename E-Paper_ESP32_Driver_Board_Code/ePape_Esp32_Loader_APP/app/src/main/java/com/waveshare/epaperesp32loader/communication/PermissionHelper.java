package com.waveshare.epaperesp32loader.communication;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import com.waveshare.epaperesp32loader.R;

/**
 * <h1>Permission helper</h1>
 * The class provides functions for delayed operations,
 * which require permissions and interaction with user.
 *
 * @author  Waveshare team
 * @version 1.0
 * @since   8/16/2018
 */

public class PermissionHelper implements DialogInterface.OnDismissListener
{
    // Permissions used in the application
    //---------------------------------------------------------
    public static final int REQ_BLUE = 0; // For bluetooth devices scanning
    public static final int REQ_READ = 1; // For reading from external storage

    // Permissions related messages
    //---------------------------------------------------------
    private static final String[] messages = new String[]
    {
        "Please accept the GPS Location permission",
        "Please accept the Storage reading permission",
        "The GPS Location permission is denied",
        "The Storage Reading permission is denied",
    };

    // Full names of permissions
    //---------------------------------------------------------
    private static final String[] permissions = new String[]
    {
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    // Wrapped names of permissions wrapped (used in requests)
    //---------------------------------------------------------
    private static final String[][] permissionArrays = new String[][]
    {
        new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
        new String[] { Manifest.permission.READ_EXTERNAL_STORAGE  },
    };

    // Granted permission event handler interface
    //---------------------------------------------------------
    public interface PermissionResponse
    {
        void invoke();
    }

    // Array of granted permission event handlers
    //---------------------------------------------------------
    private static PermissionResponse[] responses = new PermissionResponse[]
    {
        null,
        null,
    };

    private Activity context; // Activity which needs a permission
    private int      reqCode; //

    public PermissionHelper(Activity activity)
    {
        context = activity;
    }

    public AlertDialog show(int requestCode)
    {
        reqCode = requestCode;
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        dialog.setTitle(R.string.dlg_note);
        dialog.setOnDismissListener(this);
        dialog.setPositiveButton(R.string.btn_okey, null);
        dialog.setMessage(messages[reqCode]);

        return dialog.show();
    }

    public void onDismiss(DialogInterface dialog)
    {
        if (reqCode >= permissions.length) return;
        ActivityCompat.requestPermissions(context, permissionArrays[reqCode], reqCode);
    }

    // Interaction with user and sending the request permission
    //---------------------------------------------------------
    public boolean sendRequestPermission(int requestCode)
    {
        // Get permission if possible
        //-----------------------------------------------------
        if (requestCode >= permissions.length) return false;
        String permission = permissions[requestCode];

        // Return true if it is already granted
        //-----------------------------------------------------
        if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)
            return true;

        // Interact with user if it's needed
        //-----------------------------------------------------
        if (context.shouldShowRequestPermissionRationale(permission))
            show(requestCode);

        // Request the permission
        //-----------------------------------------------------
        else context.requestPermissions(permissionArrays[requestCode], requestCode);

        return false;
    }

    // Getting and handling the request permission result
    //---------------------------------------------------------
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        // Unknown result
        //-----------------------------------------------------
        if ((requestCode < 0) || (requestCode >= permissions.length))
            return;

        // Notify that permission is not granted yet
        //-----------------------------------------------------
        if (context.checkSelfPermission(permissions[requestCode]) != PackageManager.PERMISSION_GRANTED)
        {
            if (responses[requestCode] != null)
                show(requestCode + permissions.length);
        }

        // Perform granted permission's response function
        //-----------------------------------------------------
        else if (responses[requestCode] != null)
                responses[requestCode].invoke();

        // Clear corresponded cell in responses array
        //-----------------------------------------------------
        responses[requestCode] = null;
    }

    // Set response on
    //---------------------------------------------------------
    public void setResponse(int requestCode, PermissionResponse response)
    {
        if ((requestCode < 0) || (requestCode >= responses.length)) return;
        responses[requestCode] = response;
    }

    // Notification dialog
    //---------------------------------------------------------
    public static void note(Activity activity, int msg)
    {
        Resources resources = activity.getResources();
        AlertDialog.Builder dialog = new AlertDialog.Builder(activity);

        dialog.setTitle(resources.getString(R.string.dlg_note));
        dialog.setPositiveButton(resources.getString(R.string.btn_okey), null);
        dialog.setMessage(resources.getString(msg));
        dialog.show();
    }
}