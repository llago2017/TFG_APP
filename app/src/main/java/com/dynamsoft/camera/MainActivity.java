package com.dynamsoft.camera;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private Camera mCamera;
    private CameraPreview mPreview;
    MediaRecorder recorder;
    final String TAG = "MainActivity";

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_MICRO_PERMISSION = 2;
    private static final int REQUEST_WRITE_PERMISSION = 3;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main);


        // Create an instance of Camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            requestMicroPermission();
            requestWritePermission();
            return;
        } else {
            mCamera = getCameraInstance();
            CameraPreview.setCameraDisplayOrientation(this,0,mCamera);

        }

        // Mostrar ajustes
        Button settings = findViewById(R.id.settings_button);
        settings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG,"Preferencias");
                Intent activity2Intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(activity2Intent);
            }
        });

        // Notificación
        LayoutInflater inflater = getLayoutInflater();
        @SuppressLint("InflateParams") View layout = inflater.inflate(R.layout.toast1, null);
        final Toast record_toast = new Toast(getApplicationContext());
        record_toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        record_toast.setDuration(Toast.LENGTH_SHORT);
        record_toast.setView(layout);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout frameLayout = findViewById(R.id.camera_preview);
        frameLayout.addView(mPreview);


        RelativeLayout relativeLayoutControls = (RelativeLayout) findViewById(R.id.controls_layout);
        relativeLayoutControls.bringToFront();



        Button record_button = findViewById(R.id.record_button);

        record_button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.i(TAG, "Botón pulsado");
                        init_recorder(mCamera,mPreview);

                        break;
                    case MotionEvent.ACTION_UP:
                        // RELEASED
                        Log.i(TAG, "Botón soltado");
                        // Paro de grabar
                        stop_recorder(mCamera);

                        // Preparación del encriptado
                        //init_encrypt();

                        record_toast.show();
                        break;
                }

                return false;
            }
        });

        // SwitchPreference preference change listener
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isChecked = sharedPreferences.getBoolean("Guardar", false);
        Toast.makeText(this, "isChecked : " + isChecked, Toast.LENGTH_LONG).show();



    }

    void init_recorder(Camera mCamera, CameraPreview mPreview) {
        // BEGIN_INCLUDE (configure_media_recorder)
        recorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mCamera.stopPreview();
        recorder.setCamera(mCamera);
        recorder.setOrientationHint(90);

        // Step 2: Set sources
        recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

        /* Step 4: Encoders
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);*/

        CamcorderProfile cpHigh = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        recorder.setProfile(cpHigh);

        // Set 5: Output file
        // SwitchPreference preference change listener


        recorder.setPreviewDisplay(mPreview.getHolder().getSurface());
        recorder.setOutputFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/myrecording.mp4");

        // TEST GET ENABLE/DISABLE SAVE
       /* SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isChecked = sharedPreferences.getBoolean("Guardar", false);
        Toast.makeText(this, "isChecked : " + isChecked, Toast.LENGTH_LONG).show();

        if (isChecked) {
            recorder.setOutputFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/myrecording.mp4");
        } else {
            recorder.setOutputFile("/dev/null");
        } */


        // Step 5: Prepare configured MediaRecorder
        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
        }
        recorder.start();
    }

    void stop_recorder(Camera mCamera){
        if (recorder != null) {
            recorder.stop();
            recorder.reset();
            recorder.release();
            recorder = null;
            mCamera.lock();
        }
        //keepVideo();

    }


    @Override
    protected void onPause()
    {
        super.onPause();
        releaseCamera();

    }

    private void requestCameraPermission() {
        requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }

    private void requestMicroPermission() {
        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICRO_PERMISSION);
    }

    private void requestWritePermission() {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(this, "Permissions Denied show camera", Toast.LENGTH_LONG).show();
                }
                return;
            }
            case REQUEST_MICRO_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                } else {
                    // permission denied
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Permissions Denied to record audio", Toast.LENGTH_LONG).show();
                }
                return;
            }

            case REQUEST_WRITE_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                } else {
                    // permission denied
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Permissions Denied to write storage", Toast.LENGTH_LONG).show();
                }
                return;
            }

        }
    }



    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance() {

        Camera c = null;
        try {
            c = Camera.open();


        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    void keepVideo() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isChecked = sharedPreferences.getBoolean("Guardar", false);
        Toast.makeText(this, "isChecked : " + isChecked, Toast.LENGTH_LONG).show();

        // Si es true, solo eliminamos el original y me quedo solo con el encriptado
        if (isChecked) {
            // No hacemos nada, se mantiene el archivo encriptado
        } else {
            // Se borra el encriptado
        }
        // El original se borra en todos los casos
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File file = new File(dir,  "/myrecording.mp4");
        boolean deleted = file.delete();
    }

}