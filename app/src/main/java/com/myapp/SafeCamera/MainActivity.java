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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback,
                                                      GoogleApiClient.OnConnectionFailedListener,
                                                      GoogleApiClient.ConnectionCallbacks {

    private Camera mCamera;
    private CameraPreview mPreview;
    MediaRecorder recorder;
    final String TAG = "MainActivity";
    static SecureRandom srandom = new SecureRandom();

    float mDist;
    String filename;
    File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_MICRO_PERMISSION = 2;
    private static final int REQUEST_WRITE_PERMISSION = 3;
    private static final int REQUEST_CODE = 1001;
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

        File file = new File(dir + "/SafeCamera/");

        if (!file.isDirectory()) {
            file.mkdirs();
        }
        dir = file;

        // Create an instance of Camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                + ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                + ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //requestCameraPermission();
            //requestMicroPermission();
            //requestWritePermission();
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    REQUEST_CODE);
        }

        mCamera = getCameraInstance();
        CameraPreview.setCameraDisplayOrientation(this, 0, mCamera);

        // Mostrar ajustes
        Button settings = findViewById(R.id.settings_button);
        settings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "Preferencias");
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
                    Log.i(TAG, "Inicio de sesión");
                    signIn();
                }
            });
            Button logout = findViewById(R.id.logout);
            logout.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Log.i(TAG, "Inicio de sesión");
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
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.i(TAG, "Botón pulsado");
                        init_recorder(mCamera, mPreview);


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


        SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHHmm");
        filename = dateFormat.format(new Date()) + "_SafeCamera.mp4";
        recorder.setOutputFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/SafeCamera/" + filename);

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

    @RequiresApi(api = Build.VERSION_CODES.M)
    void stop_recorder(Camera mCamera) {
        if (recorder != null) {
            recorder.stop();
            recorder.reset();
            recorder.release();
            recorder = null;
            mCamera.lock();
        }

        //createFile();
        //createDriveFile();
        //saveFile();
        createVideo(filename);
        init_encrypt(filename);
        Log.i(TAG, "Archivo guardado");
        //keepVideo(filename);
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
    protected void onPause() {
        super.onPause();
        releaseCamera();

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

            case REQUEST_CODE:
                // When request is cancelled, the results array are empty
                if (
                        (grantResults.length > 0) &&
                                (grantResults[0]
                                        + grantResults[1]
                                        + grantResults[2]
                                        == PackageManager.PERMISSION_GRANTED
                                )
                ) {
                    // Permissions are granted
                    Toast.makeText(getApplicationContext(), "Permissions granted.", Toast.LENGTH_SHORT).show();

                } else {
                    // Permissions are denied
                    Toast.makeText(getApplicationContext(), "Permissions denied.", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }


    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {

        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.M)
    void init_encrypt(String filename){
        try {
            Cipher encipher = Cipher.getInstance("AES/GCM/NoPadding");
            // Original
            FileInputStream fis = new FileInputStream((Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) +"/SafeCamera/" + filename));
            //Encriptado
            FileOutputStream fos = new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)+"/encrypted.mp4");
            FileOutputStream key_out = new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)+"/privatekey.key");

            // Genero clave AES (ka)
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            SecretKey skey = keyGen.generateKey();

            // Genero par de claves RSA
            //KeyPairGenerator kpg = KeyPairGenerator.getInstance(
             //       KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");

            String alias = "SafeCamera";
            /*kpg.initialize(new KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_DECRYPT)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                    .setKeySize(2048)
                    .build());*/
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);

            //Obtención de claves
            //kpg.initialize(2048);
            KeyPair keyPair = kpg.generateKeyPair();

            key_out.write(keyPair.getPrivate().getEncoded());

            /* The key pair can also be obtained from the Android Keystore any time as follows:
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, null);
            PublicKey publicKey = keyStore.getCertificate(alias).getPublicKey();*/

            // Vector de inicialización
            byte[] iv = new byte[128/8];
            srandom.nextBytes(iv);
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            // Cifro la clave con RSA
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
            byte[] b = cipher.doFinal(skey.getEncoded());
            fos.write(b);
            fos.write(iv);

            // Cifro con AES
            encipher.init(Cipher.ENCRYPT_MODE, skey, ivspec);

            processFile(encipher, fis, fos);



            /*CipherOutputStream cos = new CipherOutputStream(fos, encipher);
            //FileInputStream fis2 = new FileInputStream((Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) +"/SafeCamera/" + filename));

            try (FileInputStream in = new FileInputStream(String.valueOf(fis))) {
                processFile(encipher, in, out);
            }
            out.close();*/

            /*
            // Write bytes
            int b;
            byte[] d = new byte[8];
            while((b = fis.read(d)) != -1) {
                cos.write(d, 0, b);
            }
            // Flush and close streams.
            cos.flush();
            cos.close();

            fis.close();*/

        } catch(GeneralSecurityException e) {
            throw new IllegalStateException("Could not retrieve AES cipher", e);
        } catch (FileNotFoundException e){
            throw new IllegalStateException("File not found",e);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // Llamar después de encriptar
    void keepVideo(String filename) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isChecked = sharedPreferences.getBoolean("Guardar", false);
        //Toast.makeText(this, "isChecked : " + isChecked, Toast.LENGTH_LONG).show();

        // Si es true, solo eliminamos el original y me quedo solo con el encriptado
        if (isChecked) {
            // No hacemos nada, se mantiene el archivo encriptado
        } else {
            // Se borra el encriptado
        }
        // El original se borra en todos los casos
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File file = new File(dir,  "/SafeCamera/" + filename);
        file.delete();
    }

    static private void processFile(Cipher ci, InputStream in, OutputStream out)
            throws javax.crypto.IllegalBlockSizeException,
            javax.crypto.BadPaddingException,
            java.io.IOException
            {
                byte[] ibuf = new byte[1024];
                int len;
                while ((len = in.read(ibuf)) != -1) {
                    byte[] obuf = ci.update(ibuf, 0, len);
                    if ( obuf != null ) out.write(obuf);
                }
                byte[] obuf = ci.doFinal();
                if ( obuf != null ) out.write(obuf);
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

            CameraPreview.surfaceCreated();
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

    private void createVideo(String filename) {
        if (mDriveServiceHelper != null) {
            Log.i(TAG, "Creating a video.");

            mDriveServiceHelper.createVideo(filename)
                    .addOnSuccessListener(fileId -> readFile(fileId))
                    .addOnFailureListener(exception ->
                            Log.e(TAG, "Couldn't create file.", exception));
        }
        Log.i(TAG, "mDriveServiceHelper es null");
        //
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