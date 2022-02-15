package com.myapp.SafeCamera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Camera;
import android.location.LocationManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
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
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
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
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class MainActivity extends FragmentActivity implements ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        DialogoPass.EditNameDialogListener {

    private static final Uri CONTENT_URI = Uri.parse("content://com.myapp.SafeCamera/users");
    private final static String TAG = "MainActivity";
    private static final float LOCATION_REFRESH_DISTANCE = 1000;
    private static final long LOCATION_REFRESH_TIME = 1000;
    private Camera mCamera;
    private CameraPreview mPreview;
    MediaRecorder recorder;
    //final String TAG = "MainActivity";
    private byte[] MY_PUBLICKEY;
    static SecureRandom srandom = new SecureRandom();
    private Boolean checkDb = false;

    float mDist;
    String filename;
    String enc_filename;
    File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

    private static final int REQUEST_CODE = 1001;
    private static final int RC_SIGN_IN = 9001;

    // Drive
    private DriveServiceHelper mDriveServiceHelper;

    private GoogleSignInClient mGoogleSignInClient;
    private String mOpenFileId;


    boolean anon = false;
    boolean fileio = false;

    // Localización
    double longitude;
    double latitude;
    LocationManager locationManager;

    //Claves
    byte[] priv = null;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint({"ClickableViewAccessibility", "MissingPermission"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1002);
        } else {
            getCurrentLocation();
        }
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
            
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    REQUEST_CODE);
            CameraPreview.surfaceCreated();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            mCamera = getCameraInstance();
            CameraPreview.setCameraDisplayOrientation(this, 0, mCamera);

            // Create our Preview view and set it as the content of our activity.
            mPreview = new CameraPreview(this, mCamera);
            FrameLayout frameLayout = findViewById(R.id.camera_preview);
            frameLayout.addView(mPreview);
        }



        // Mostrar ajustes
        Button settings = findViewById(R.id.settings_button);
        settings.setOnClickListener(v -> {
            Log.i(TAG, "Preferencias");
            Intent activity2Intent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(activity2Intent);
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
        anon = sharedPreferences.getBoolean("anon", false);
        fileio = sharedPreferences.getBoolean("fileio", false);
        Toast.makeText(this, "Drive : " + Drive, Toast.LENGTH_LONG).show();
        Toast.makeText(this, "AnonFiles : " + anon, Toast.LENGTH_LONG).show();
        if (Drive) {
            Button login = findViewById(R.id.login);
            login.setOnClickListener(v -> {
                Log.i(TAG, "Inicio de sesión");
                signIn();
            });
            Button logout = findViewById(R.id.logout);
            logout.setOnClickListener(v -> {
                Log.i(TAG, "Inicio de sesión");
                signOut();
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

       
        RelativeLayout relativeLayoutControls = (RelativeLayout) findViewById(R.id.controls_layout);
        relativeLayoutControls.bringToFront();


        Button record_button = findViewById(R.id.record_button);

        record_button.setOnTouchListener((v, event) -> {
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
        recorder.setLocation(Float.parseFloat(String.valueOf(latitude)),Float.parseFloat(String.valueOf(longitude)));

        // Set 5: Output file
        // SwitchPreference preference change listener


        recorder.setPreviewDisplay(mPreview.getHolder().getSurface());


        SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHHmm");
        filename = dateFormat.format(new Date()) + "_SafeCamera.mp4";
        recorder.setOutputFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/SafeCamera/" + "." + filename);


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

        init_encrypt(filename);
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            createVideo(enc_filename);

        } else {
            keepVideo(filename, enc_filename);
        }

        if (anon){
            AsyncTask.execute(() -> request_test(enc_filename, "anon"));

        }

        if (fileio) {
            AsyncTask.execute(() -> {
                // Get file from file name
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)+ "/SafeCamera/"+ enc_filename);
                // Get length of file in bytes
                long fileSizeInBytes = file.length();
                // Convert the bytes to Kilobytes (1 KB = 1024 Bytes)
                long fileSizeInKB = fileSizeInBytes / 1024;
                // Convert the KB to MegaBytes (1 MB = 1024 KBytes)
                long fileSizeInMB = fileSizeInKB / 1024;

                if (fileSizeInMB >= 100) {
                    Log.i(TAG, "Tamaño mayor de 100MB, no se puede subir");
                    showMessage("Tamaño mayor de 100MB, no se puede subir");
                } else {
                    Log.i(TAG, "Menor de 100MB: " + fileSizeInMB);
                    request_test(enc_filename, "fileio");
                }

            });

        }
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
        //releaseCamera();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {// When request is cancelled, the results array are empty
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
        }
        if (requestCode == 1002 && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                showMessage("Permission denied!");
            }
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
        checkDb();
        try {

            // Original
            FileInputStream fis = new FileInputStream((Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) +"/SafeCamera/" +"." +  filename));
            //Encriptado
            enc_filename = "enc_"+ filename;
            FileOutputStream fos = new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)+ "/SafeCamera/"+"enc_"+ filename);


            PublicKey pub = null;
            if (!checkDb) {
                // Se genera una clave
                //Obtención de claves
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                KeyPair keyPair = kpg.generateKeyPair();
                //key_out.write(keyPair.getPrivate().getEncoded());
                pub = keyPair.getPublic();
                priv = keyPair.getPrivate().getEncoded();

                MY_PUBLICKEY = keyPair.getPublic().getEncoded();
            } else {
                // La obtengo de la base de datos
                byte[] test = searchPK();
                X509EncodedKeySpec ks = new X509EncodedKeySpec(test);
                try {
                    KeyFactory kf = KeyFactory.getInstance("RSA");
                    pub = kf.generatePublic(ks);
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    e.printStackTrace();
                }
            }

            storeDb();

            // Genero clave AES (ka)
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            SecretKey skey = keyGen.generateKey();

            // Vector de inicialización
            byte[] iv = new byte[128/8];
            srandom.nextBytes(iv);
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            // Cifro la clave con RSA
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, pub);
            byte[] b = cipher.doFinal(skey.getEncoded());
            fos.write(b);
            fos.write(iv);

            // Cifro con AES
            Cipher encipher = Cipher.getInstance("AES/GCM/NoPadding");
            encipher.init(Cipher.ENCRYPT_MODE, skey, ivspec);
            processFile(encipher, fis, fos);

            //

            if (priv != null) {
                Log.i(TAG, "Se ha creado una clave privada");
                DialogoPass dialogoPass = new DialogoPass();
                dialogoPass.show(getSupportFragmentManager(), "pass");

            }

            // Añadir en la galería
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)+ "/SafeCamera/"+enc_filename);
            Uri contentUri = Uri.fromFile(f);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);


        } catch(GeneralSecurityException e) {
            throw new IllegalStateException("Could not retrieve AES cipher", e);
        } catch (FileNotFoundException e){
            throw new IllegalStateException("File not found",e);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onFinishEditDialog(String inputText) {
        Toast.makeText(this, "Hi, " + inputText, Toast.LENGTH_SHORT).show();

        try {
            FileOutputStream key_out = new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)+"/SafeCamera.key");
            String password = inputText;

            /*byte[] salt = {
                    (byte)0xc7, (byte)0x73, (byte)0x21, (byte)0x8c,
                    (byte)0x7e, (byte)0xc8, (byte)0xee, (byte)0x99
            };*/

            int count = 20;// hash iteration count

            byte[] salt = new byte[8];
            srandom.nextBytes(salt);
            Log.i(TAG, "SALT: " + Arrays.toString(salt));
            key_out.write(salt);

            Log.i(TAG, "Pass: " + Arrays.toString(password.toCharArray()));
            // Create PBE parameter set
            PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, count);
            PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray());//,salt, count, 128);
            SecretKeyFactory keyFac = SecretKeyFactory.getInstance("PBEWithSHAAnd3KeyTripleDES");
            SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);

            Cipher pbeCipher = Cipher.getInstance("PBEWithSHAAnd3KeyTripleDES");

            // Initialize PBE Cipher with key and parameters
            pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);

            // Encrypt the encoded Private Key with the PBE key
            byte[] ciphertext = pbeCipher.doFinal(priv);
            key_out.write(ciphertext);

            // After use, remove password
            inputText = null;
            password = null;
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }


    }

    // Llamar después de encriptar
    void keepVideo(String filename, String enc_filename) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isChecked = sharedPreferences.getBoolean("Guardar", false);
        //Toast.makeText(this, "isChecked : " + isChecked, Toast.LENGTH_LONG).show();
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

        // Si es true, solo eliminamos el original y me quedo solo con el encriptado
        if (!isChecked) {
            // Se borra el encriptado
            File file = new File(dir,  "/SafeCamera/" + enc_filename);
            file.delete();
        }
        
        // El original se borra en todos los casos
        File file = new File(dir,  "/SafeCamera/" + "." + filename);
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

        List<String> supportedFocusModes = params.getSupportedFocusModes();
        if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            mCamera.autoFocus((b, camera) -> {
                // auto enfoca un toque
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
        assert completedTask != null;
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
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // [START_EXCLUDE]
            updateUI(null);
            // [END_EXCLUDE]
        });
    }
    // [END signOut]
    
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

  
    private void readFile(String fileId) {
        if (mDriveServiceHelper != null) {
            Log.i(TAG, "Reading file " + fileId);

            mDriveServiceHelper.readFile(fileId)
                    .addOnSuccessListener(nameAndContent -> {

                        setReadWriteMode(fileId);
                        keepVideo(filename, enc_filename);
                        signOut();
                    })
                    .addOnFailureListener(exception ->
                            Log.e(TAG, "Couldn't read file.", exception));
        }
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

    private byte[] searchPK(){
        String[] projection = {"name"};
        String selection = null;
        String[] selectionArgs = null;
        String sort = null;
        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(CONTENT_URI, projection, selection, selectionArgs, sort);
        byte[] test = new byte[0];
        if(cursor.moveToFirst()) {
            do {
                Log.i("Resultado", "key");
                test = cursor.getBlob(0);

            }while(cursor.moveToNext());
        }  else {
            System.out.println("No Records Found");
        }
        return test;
    }

    private void storeDb(){

        if (!checkDb) {
            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(MyContentProvider.name,MY_PUBLICKEY);

            getContentResolver().insert(MyContentProvider.CONTENT_URI, values);

            // displaying a toast message
            Toast.makeText(getBaseContext(), "New Record Inserted", Toast.LENGTH_LONG).show();
        }
    }

    private void checkDb(){
        String[] projection = {"name"};
        String selection = null;
        String[] selectionArgs = null;
        String sort = null;
        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(CONTENT_URI, projection, selection, selectionArgs, sort);
        if(cursor.moveToFirst()) {
            do {
                if (cursor.getCount() > 0) {
                    Log.i(TAG, "Ya existe una clave");
                    checkDb = true;
                }
            }while(cursor.moveToNext());
        }  else {
            System.out.println("No Records Found");
        }
    }

    private void setReadWriteMode(String fileId) {
        mOpenFileId = fileId;
    }

    static void request_test(String enc_filename, String to) {
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httpost = null;
            switch (to) {
                case "anon":
                    httpost = new HttpPost("https://api.anonfiles.com/upload");
                    Log.i(TAG, "Subiendo a AnonFiles");
                    break;
                case "fileio":
                    httpost = new HttpPost("https://file.io");
                    Log.i(TAG, "Subiendo a File.io");
                    break;
            }

            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)+ "/SafeCamera/"+ enc_filename);

            // Adding data
            FileBody fileBody = new FileBody(file);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .addPart("file", fileBody);
            HttpEntity multiPartEntity = builder.build();
            if (httpost != null) {
                httpost.setEntity(multiPartEntity);
            }

            HttpResponse response = httpclient.execute(httpost);

            if (response.getStatusLine().getStatusCode() == 200) {
                String json = EntityUtils.toString(response.getEntity(), "UTF-8");
                Log.i(TAG, "respuesta: " + json);
                JSONObject obj = new JSONObject(json);

                switch (to) {
                    case "anon":
                        saveAnon(obj);
                        break;
                    case "fileio":
                        json_fileio(obj);
                        break;
                }
                String status = obj.getString("status");
                Log.i(TAG, "Estado: " + status);
            } else {
                String json = EntityUtils.toString(response.getEntity(), "UTF-8");
                Log.i(TAG, "respuesta: " + json);
                JSONObject obj = new JSONObject(json);
                String status = obj.getString("status");
                Log.i(TAG, "Estado: " + status);

            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    static void saveAnon(JSONObject obj) {
        //String json = "{\"status\":true,\"data\":{\"file\":{\"url\":{\"full\":\"https://anonfiles.com/p06dR0p1va/img_jpg\",\"short\":\"https://anonfiles.com/p06dR0p1va\"},\"metadata\":{\"id\":\"p06dR0p1va\",\"name\":\"img.jpg\",\"size\":{\"bytes\":2192881,\"readable\":\"2.19MB\"}}}}}";

        try {
            String status = obj.getString("status");
            String data = obj.getString("data");
            JSONObject newobj = new JSONObject(data);
            String file = newobj.getString("file");
            JSONObject newobj2 = new JSONObject(file);
            String url = new JSONObject(newobj2.getString("url")).getString("full");
            String filename = new JSONObject(newobj2.getString("metadata")).getString("name");

            totxt(filename, url);
            System.out.println(status);
            System.out.println(url);
        } catch(org.json.JSONException e) {
            e.printStackTrace();
        }
    }


    static void json_fileio(JSONObject obj) {
        //String json = "{\"success\":true,\"status\":200,\"id\":\"279e3360-5b85-11ec-b896-9b143cda26df\",\"key\":\"uNLkv4LYk5ed\",\"name\":\"img.jpg\",\"link\":\"https://file.io/uNLkv4LYk5ed\",\"private\":false,\"expires\":\"2021-12-26T19:53:19.510Z\",\"downloads\":0,\"maxDownloads\":1,\"autoDelete\":true,\"size\":2192881,\"mimeType\":\"application/octet-stream\",\"created\":\"2021-12-12T19:53:19.510Z\",\"modified\":\"2021-12-12T19:53:19.510Z\"}";

        try {
            //JSONObject obj = new JSONObject(json);
            String status = obj.getString("status");
            String filename = obj.getString("name");
            String url = obj.getString("link");

            totxt(filename, url);
            System.out.println(status);
        } catch(org.json.JSONException e) {
            e.printStackTrace();
        }
    }

    static void totxt(String filename, String url) {
        try{
            File doc = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)+ "/SafeCamera/"+ "SafeCamera.txt");
            if(doc.exists()) {
                //Do something
                FileWriter fw = new FileWriter(doc.getAbsolutePath(),true); //the true will append the new data
                fw.write(filename + ": " + url + "\n");//appends the string to the file
                fw.close();
            } else {
                // Do something else.
                FileOutputStream fos = new FileOutputStream(doc);
                fos.write((filename + ": " + url + "\n").getBytes());
                fos.flush();
                fos.close();
            }
            //System.out.println(status);
            System.out.println(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    void getCurrentLocation() {

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.getFusedLocationProviderClient(MainActivity.this)
                .requestLocationUpdates(locationRequest, new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                LocationServices.getFusedLocationProviderClient(MainActivity.this).removeLocationUpdates(this);

                if(locationResult != null && locationResult.getLocations().size() > 0){
                    int lastestLocatioIndex = locationResult.getLocations().size() - 1;
                    latitude = locationResult.getLocations().get(lastestLocatioIndex).getLatitude();
                    longitude = locationResult.getLocations().get(lastestLocatioIndex).getLongitude();
                    showMessage(String.valueOf(latitude));
                }

            }
        }, Looper.getMainLooper());
    }


}