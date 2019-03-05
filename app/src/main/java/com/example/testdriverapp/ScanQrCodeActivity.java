package com.example.testdriverapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import java.util.Collections;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ScanQrCodeActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    private static final String HUAWEI = "HUAWEI";
    private static final int MY_CAMERA_REQUEST_CODE = 6515;
    ZXingScannerView qrCodeScanner;
    ImageView flashOnOffImageView;
    boolean isDriver;
    private static final int VERIFICATION_REQUEST_CODE = 1010;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Get rid of the app's title
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Set app to full-screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_scan_qr_code);

        qrCodeScanner = findViewById(R.id.qrCodeScanner);

        // Set the scanner's basic properties
        setScannerProperties();

        ImageView barcodeBackImageView = findViewById(R.id.barcodeBackImageView);

        flashOnOffImageView = findViewById(R.id.flashOnOffImageView);

        barcodeBackImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        flashOnOffImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (qrCodeScanner.getFlash())
                {
                    qrCodeScanner.setFlash(false);
                    flashOnOffImageView
                            .setBackground(ContextCompat.getDrawable(getApplicationContext()
                            , R.drawable.flash_off_vector_icon));
                }
                else
                {
                    qrCodeScanner.setFlash(true);
                    flashOnOffImageView
                            .setBackground(ContextCompat.getDrawable(getApplicationContext()
                            , R.drawable.flash_on_vector_icon));
                }
            }
        });
    }

    /**
     * Set bar code scanner basic properties.
     */
    private void setScannerProperties() {
        qrCodeScanner.setFormats(Collections.singletonList(BarcodeFormat.QR_CODE));
        qrCodeScanner.setAutoFocus(true);
        qrCodeScanner.setLaserColor(R.color.colorAccent);
        qrCodeScanner.setMaskColor(R.color.colorAccent);
        if (Build.MANUFACTURER.equalsIgnoreCase(HUAWEI))
            qrCodeScanner.setAspectTolerance(0.5f);
    }


    /**
     * resume the qr code camera when activity is in onResume state.
     */

    @Override
    protected void onResume()
    {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat
                        .requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                        MY_CAMERA_REQUEST_CODE);
                return;
            }
        }
        qrCodeScanner.startCamera();
        qrCodeScanner.setResultHandler(this);
    }

    @Override
    public void handleResult(Result result)
    {
        String resultText;

        if (result != null)
        {
            resultText = result.getText();

            isDriver = resultText.contains("BasnaApp");

            Intent intent = new Intent();
            intent.putExtra("isDriver", isDriver);

            setResult(VERIFICATION_REQUEST_CODE, intent);

            finish();
        }
    }


    /**
     * stop the qr code camera scanner when activity is in onPause state.
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        qrCodeScanner.stopCamera();
    }

    /**
     * To check if user grant camera permission then called openCamera function.If not then show not granted
     * permission snack bar.
     *
     * @param requestCode  specify which request result came from operating system.
     * @param permissions  to specify which permission result is came.
     * @param grantResults to check if user granted the specific permission or not.
     */

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_CAMERA_REQUEST_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                openCamera();
            else if (grantResults[0] == PackageManager.PERMISSION_DENIED)
                showCameraToast();
        }
    }

    private void openCamera()
    {
        qrCodeScanner.startCamera();
        qrCodeScanner.setResultHandler(this);
    }

    private void showCameraToast()
    {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Toast.makeText(this
                    ,R.string.app_needs_your_camera_permission_in_order_to_scan_qr_code
                    , Toast.LENGTH_LONG).show();
        }
    }

    public static Intent getScanQrCodeActivity(Context callingClassContext)
    {
        return new Intent(callingClassContext, ScanQrCodeActivity.class);
    }

}