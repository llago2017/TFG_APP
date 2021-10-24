package com.myapp.SafeCamera;


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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;


import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback,
                                                      GoogleApiClient.OnConnectionFailedListener,
                                                      GoogleApiClient.ConnectionCallbacks {

    private Camera mCamera;
    private CameraPreview mPreview;
    MediaRecorder recorder;
    final String TAG = "MainActivity";

    float mDist;
    int fileId;

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_MICRO_PERMISSION = 2;
    private static final int REQUEST_WRITE_PERMISSION = 3;

    private static final int RC_SIGN_IN = 9001;

    // Drive
    private DriveServiceHelper mDriveServiceHelper;

    private GoogleSignInClient mGoogleSignInClient;
    private String mOpenFileId;


    private GoogleApiClient mGoogleApiClient;
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        // Hide the window title.
      //  requestWindowFeature(Window.FEATURE_NO_TITLE);
       // getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
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

        // Inicio de sesión de drive
        // [START configure_signin]
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(Scopes.DRIVE_FULL))
                .requestEmail()
                .build();
        // [END configure_signin]

        // [START build_client]
        // Build a GoogleSignInClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        // [END build_client]


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean Drive = sharedPreferences.getBoolean("Drive", false);
        Toast.makeText(this, "Drive : " + Drive, Toast.LENGTH_LONG).show();
        if (Drive) {
            Button login = findViewById(R.id.login);
            login.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Log.i(TAG,"Inicio de sesión");
                    signIn();
                }
            });
            Button logout = findViewById(R.id.logout);
            logout.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Log.i(TAG,"Inicio de sesión");
                    signOut();
                }
            });
        } else {

            findViewById(R.id.login).setVisibility(View.GONE);
            findViewById(R.id.logout).setVisibility(View.GONE);
        }




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

        createFile();
        //createDriveFile();
        saveFile();
        Log.i(TAG, "Archivo guardado");
        //keepVideo();
    }



    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // GoogleApiClient connection failed, most API calls will not work...
        Log.i(TAG, "Fallo al conectar");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // GoogleApiClient is connected, API calls should succeed...
    }

    @Override
    public void onConnectionSuspended(int i) {
        // ...
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

    // Llamar después de encriptar
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

    // Zoom
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Get the pointer ID
        Camera.Parameters params = mCamera.getParameters();
        int action = event.getAction();


        if (event.getPointerCount() > 1) {
            // handle multi-touch events
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                mDist = getFingerSpacing(event);
            } else if (action == MotionEvent.ACTION_MOVE && params.isZoomSupported()) {
                mCamera.cancelAutoFocus();
                handleZoom(event, params);
            }
        } else {
            // handle single touch events
            if (action == MotionEvent.ACTION_UP) {
                handleFocus(event, params);
            }
        }
        return true;
    }

    private void handleZoom(MotionEvent event, Camera.Parameters params) {
        int maxZoom = params.getMaxZoom();
        int zoom = params.getZoom();
        float newDist = getFingerSpacing(event);
        if (newDist > mDist) {
            //zoom in
            if (zoom < maxZoom)
                zoom++;
        } else if (newDist < mDist) {
            //zoom out
            if (zoom > 0)
                zoom--;
        }
        mDist = newDist;
        params.setZoom(zoom);
        mCamera.setParameters(params);
    }

    public void handleFocus(MotionEvent event, Camera.Parameters params) {
        int pointerId = event.getPointerId(0);
        int pointerIndex = event.findPointerIndex(pointerId);
        // Get the pointer's current position
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        List<String> supportedFocusModes = params.getSupportedFocusModes();
        if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {
                    // currently set to auto-focus on single touch
                }
            });
        }
    }

    /** Determine the space between the first two fingers */
    private float getFingerSpacing(MotionEvent event) {
        // ...
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }

    // DRIVE

    @Override
    public void onStart() {
        super.onStart();

        // Check if the user is already signed in and all required scopes are granted
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null && GoogleSignIn.hasPermissions(account, new Scope(Scopes.DRIVE_FULL))) {
            initializeDriveService(account);
            updateUI(account);
        } else {
            updateUI(null);
        }
    }

    // [START onActivityResult]
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }
    // [END onActivityResult]

    // [START handleSignInResult]
    private void handleSignInResult(@Nullable Task<GoogleSignInAccount> completedTask) {
        Log.d(TAG, "handleSignInResult:" + completedTask.isSuccessful());

        try {
            // Signed in successfully, show authenticated U
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            initializeDriveService(account);
            Log.d(TAG, "Signed in as " + account.getEmail());
            updateUI(account);
        } catch (ApiException e) {
            // Signed out, show unauthenticated UI.
            Log.w(TAG, "handleSignInResult:error", e);
            updateUI(null);
        }
    }
    // [END handleSignInResult]

    public void initializeDriveService(GoogleSignInAccount account) {
        // The DriveServiceHelper encapsulates all REST API and SAF functionality.
        // Its instantiation is required before handling any onClick actions.
        GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(
                        this, Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(account.getAccount());
        Drive googleDriveService =
                new Drive.Builder(
                        AndroidHttp.newCompatibleTransport(),
                        new GsonFactory(),
                        credential)
                        .setApplicationName("Safe Camera")
                        .build();
        mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
    }

    // [START signIn]
    public void signIn() {

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    // [END signIn]

    // [START signOut]
    private void signOut() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                // [START_EXCLUDE]
                updateUI(null);
                // [END_EXCLUDE]
            }
        });
    }
    // [END signOut]

    // [START revokeAccess]
    private void revokeAccess() {
        mGoogleSignInClient.revokeAccess().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // [START_EXCLUDE]
                        updateUI(null);
                        // [END_EXCLUDE]
                    }
                });
    }
    // [END revokeAccess]
    private void updateUI(@Nullable GoogleSignInAccount account) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean Drive = sharedPreferences.getBoolean("Drive", false);

        if (Drive) {
            if (account != null) {

                findViewById(R.id.login).setVisibility(View.GONE);
                findViewById(R.id.logout).setVisibility(View.VISIBLE);
            } else {

                findViewById(R.id.login).setVisibility(View.VISIBLE);
                findViewById(R.id.logout).setVisibility(View.GONE);
            }

        }

    }

    protected void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

  /*  @SuppressLint("StringFormatInvalid")
    private void createDriveFile() {
        // Get currently signed in account (or null)
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

        // Synchronously check for necessary permissions
        if (!GoogleSignIn.hasPermissions(account, Drive.SCOPE_FILE)) {
            // Note: this launches a sign-in flow, however the code to detect
            // the result of the sign-in flow and retry the API call is not
            // shown here.
            GoogleSignIn.requestPermissions(this, RC_SIGN_IN,
                    account, Drive.SCOPE_FILE);
            return;
        }

        DriveResourceClient client = Drive.getDriveResourceClient(this, account);
        client.createContents()
                .addOnCompleteListener(task ->
                        Log.i(TAG,"Parece que fiunciona"));
    }*/

    /**
     * Retrieves the title and content of a file identified by {@code fileId} and populates the UI.
     */
    private void readFile(String fileId) {
        if (mDriveServiceHelper != null) {
            Log.i(TAG, "Reading file " + fileId);

            mDriveServiceHelper.readFile(fileId)
                    .addOnSuccessListener(nameAndContent -> {

                        setReadWriteMode(fileId);
                    })
                    .addOnFailureListener(exception ->
                            Log.e(TAG, "Couldn't read file.", exception));
        }
    }

    private void createFile() {
        if (mDriveServiceHelper != null) {
            Log.i(TAG, "Creating a file.");

            mDriveServiceHelper.createFile()
                    .addOnSuccessListener(fileId -> readFile(fileId))
                    .addOnFailureListener(exception ->
                            Log.e(TAG, "Couldn't create file.", exception));
        }
        Log.i(TAG, "mDriveServiceHelper es null");
    }

    private void saveFile() {
        if (mDriveServiceHelper != null && mOpenFileId != null) {
            Log.i(TAG, "Saving " + mOpenFileId);

            String fileName = "prueba";
            String fileContent = "Hola mundo";

            mDriveServiceHelper.saveFile(mOpenFileId, fileName, fileContent)
                    .addOnFailureListener(exception ->
                            Log.e(TAG, "Unable to save file via REST.", exception));
        }
    }


    private void setReadWriteMode(String fileId) {
        mOpenFileId = fileId;
    }


}