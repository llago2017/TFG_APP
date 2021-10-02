package com.dynamsoft.camera;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
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

public class MainActivity extends Activity {

    private Camera mCamera;
    private CameraPreview mPreview;
    final String TAG = "MainActivity";


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main);


        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.camera_preview);
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
                        break;
                    case MotionEvent.ACTION_UP:
                        // RELEASED
                        Log.i(TAG, "Botón soltado");
                        // Paro de grabar
                        // recorder.stop();
                        // recorder.reset();
                        // recorder.release();
                        // mCamera.lock();
                        //stop_recorder(recorder,mCamera);

                        // Preparación del encriptado
                        //init_encrypt();

                        //toast.show();
                        break;
                }

                return false;
            }
        });

    }


    @Override
    protected void onPause()
    {
        super.onPause();
        releaseCamera();

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

}