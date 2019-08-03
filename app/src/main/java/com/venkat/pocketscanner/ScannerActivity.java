package com.venkat.pocketscanner;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.intsig.csopen.sdk.CSOpenAPI;
import com.intsig.csopen.sdk.CSOpenAPIParam;
import com.intsig.csopen.sdk.CSOpenApiFactory;
import com.intsig.csopen.sdk.CSOpenApiHandler;
import com.intsig.csopen.sdk.ReturnCode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class ScannerActivity extends Activity implements View.OnClickListener {

    private static final String Tag = "MainActivity";

    private final int REQ_CODE_PICK_IMAGE = 1;
    private final int REQ_CODE_CALL_CAMSCANNER = 2;
    private final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    private static final String APP_KEY = "Your App Key Goes Here";

    private static final String DIR_IMAGE = Environment.getExternalStorageDirectory()
            + File.separator
            + "PocketScanner";

    // three values for save instance;
    private static final String SCANNED_IMAGE = "scanned_img";
    private static final String SCANNED_PDF = "scanned_pdf";
    private static final String ORIGINAL_IMG = "ori_img";


    private String mSourceImagePath;
    private String mOutputImagePath;
    private String mOutputPdfPath;
    private String mOutputOrgPath;

    private ImageView mImageView;
    private Bitmap mBitmap;

    private CSOpenAPI mApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(ScannerActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                /*showExplanation("Permission Needed", "Rationale",
                        Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_WRITE_EXTERNAL_STORAGE);*/
            } else {
                requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }


        mImageView = (ImageView) findViewById(R.id.image);

        findViewById(R.id.pick_and_send).setOnClickListener(this);
        // show debug info of CSOpenAPI SDK, remove this sentence if you build release version
//        Log.set(Log.LEVEL_DEBUG);
        mApi = CSOpenApiFactory.createCSOpenApi(this, APP_KEY, null);
    }

    private void showExplanation(String title,
                                 String message,
                                 final String permission,
                                 final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermission(permission, permissionRequestCode);
                    }
                });
        builder.create().show();
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(this,
                new String[]{permissionName}, permissionRequestCode);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mImageView.setImageBitmap(mBitmap);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.pick_and_send) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Util.checkDir(DIR_IMAGE);
                go2Gallery();
            } else {
                Toast.makeText(ScannerActivity.this, getString(R.string.a_msg_permission_declined), Toast.LENGTH_LONG).show();
            }
        }
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mOutputImagePath = savedInstanceState.getString(SCANNED_IMAGE);
        mOutputPdfPath = savedInstanceState.getString(SCANNED_PDF);
        mOutputOrgPath = savedInstanceState.getString(ORIGINAL_IMG);
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(SCANNED_IMAGE, mOutputImagePath);
        outState.putString(SCANNED_PDF, mOutputPdfPath);
        outState.putString(ORIGINAL_IMG, mOutputOrgPath);
        super.onSaveInstanceState(outState);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(Tag, "requestCode:" + requestCode + " resultCode:" + resultCode);
        if (requestCode == REQ_CODE_CALL_CAMSCANNER) {
            mApi.handleResult(requestCode, resultCode, data, new CSOpenApiHandler() {

                @Override
                public void onSuccess() {
                    new AlertDialog.Builder(ScannerActivity.this)
                            .setTitle(R.string.a_title_success)
                            .setMessage(R.string.a_msg_api_success)
                            .setPositiveButton(android.R.string.ok, null)
                            .create().show();
                    mBitmap = Util.loadBitmap(mOutputImagePath);
                }

                @Override
                public void onError(int errorCode) {
                    String msg = handleResponse(errorCode);
                    new AlertDialog.Builder(ScannerActivity.this)
                            .setTitle(R.string.a_title_reject)
                            .setMessage(msg)
                            .setPositiveButton(android.R.string.ok, null)
                            .create().show();
                }

                @Override
                public void onCancel() {
                    new AlertDialog.Builder(ScannerActivity.this)
                            .setMessage(R.string.a_msg_cancel)
                            .setPositiveButton(android.R.string.ok, null)
                            .create().show();
                }
            });
        } else if (requestCode == REQ_CODE_PICK_IMAGE && resultCode == RESULT_OK) {    // result of go2Gallery
            if (data != null) {
                Uri u = data.getData();
                Cursor c = getContentResolver().query(u, new String[]{"_data"}, null, null, null);
                if (c == null || c.moveToFirst() == false) {
                    return;
                }
                mSourceImagePath = c.getString(0);
                c.close();
                go2CamScanner();
            }
        }
    }

    private void go2Gallery() {
        Intent i = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        i.setType("image/*");
        try {
            startActivityForResult(i, REQ_CODE_PICK_IMAGE);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void go2CamScanner() {
        mOutputImagePath = DIR_IMAGE + "/scanned.jpg";
        mOutputPdfPath = DIR_IMAGE + "/scanned.pdf";
        mOutputOrgPath = DIR_IMAGE + "/org.jpg";
        try {
            FileOutputStream fos = new FileOutputStream(mOutputOrgPath);
            fos.write(3);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        CSOpenAPIParam param = new CSOpenAPIParam(mSourceImagePath,
                mOutputImagePath, mOutputPdfPath, mOutputOrgPath, 1.0f);
        boolean res = mApi.scanImage(this, REQ_CODE_CALL_CAMSCANNER, param);
        android.util.Log.d(Tag, "send to CamScanner result: " + res);
    }

    private String handleResponse(int code) {
        switch (code) {
            case ReturnCode.OK:
                return getString(R.string.a_msg_api_success);
            case ReturnCode.INVALID_APP:
                return getString(R.string.a_msg_invalid_app);
            case ReturnCode.INVALID_SOURCE:
                return getString(R.string.a_msg_invalid_source);
            case ReturnCode.AUTH_EXPIRED:
                return getString(R.string.a_msg_auth_expired);
            case ReturnCode.MODE_UNAVAILABLE:
                return getString(R.string.a_msg_mode_unavailable);
            case ReturnCode.NUM_LIMITED:
                return getString(R.string.a_msg_num_limit);
            case ReturnCode.STORE_JPG_ERROR:
                return getString(R.string.a_msg_store_jpg_error);
            case ReturnCode.STORE_PDF_ERROR:
                return getString(R.string.a_msg_store_pdf_error);
            case ReturnCode.STORE_ORG_ERROR:
                return getString(R.string.a_msg_store_org_error);
            case ReturnCode.APP_UNREGISTERED:
                return getString(R.string.a_msg_app_unregistered);
            case ReturnCode.API_VERSION_ILLEGAL:
                return getString(R.string.a_msg_api_version_illegal);
            case ReturnCode.DEVICE_LIMITED:
                return getString(R.string.a_msg_device_limited);
            case ReturnCode.NOT_LOGIN:
                return getString(R.string.a_msg_not_login);
            default:
                return "Return code = " + code;
        }
    }
}
