package com.example.owncameraapp;

import com.example.owncameraapp.tflitemodel.Classifier;
import com.example.owncameraapp.tflitemodel.TFLiteObjectDetectionAPIModel;
import com.example.owncameraapp.tflitemodel.MLResult;
import com.example.owncameraapp.fitbitserver.FitBitSocketServer;
import com.example.owncameraapp.utils.BoxBorderText;
import com.example.owncameraapp.utils.ImageUtils;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;

import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.json.JSONException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int NUM_THREADS = 3;
    private static final int NEW_PHOTO_SEC_PERIOD = 3;
    private static final int DEFAULT_IMG_SIZE_X = 3024;
    private static final int DEFAULT_IMG_SIZE_Y = 4032;
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    private static final float MINIMUM_CONFIDENCE_GENERAL_OBSTACLE_TF_OD_API = 0.35f;
    private static final int CAMERA_ORIENTATION = ExifInterface.ORIENTATION_ROTATE_90;
    private Button captureButton;
    private Button captureButton2;
    private ImageView imageView;

    //ML variables
    private static final int MODEL_INPUT_SIZE = 300;
    private static final String MODEL_FILE = "detect.tflite";
    private static final String LABELS_FILE = "file:///android_asset/labelmap.txt";
    private Classifier detector;
    private BoxBorderText borderedText;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    //Server variable
    private FitBitSocketServer server = null;
    private long lastProcessingTimeMs;  // = SystemClock.uptimeMillis() - startTime;
    private MLResult mlResultValue = MLResult.RESULT_UNDEFINED;
    private String mlResultTitle = "";

    //Camera variables
    protected Camera mCamera;
    protected CameraPreview mPreview;
    protected FrameLayout preview;
    private MediaRecorder mediaRecorder;

    //Thread variables
    private Runnable periodicTask;
    private ScheduledThreadPoolExecutor sch;
    private ScheduledFuture<?> periodicFuture;
    private boolean isRunning;

    //Text to speech
    TextToSpeech mTextToSpeech;
    private boolean isTextToSpeechInit = false;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.imageView);
        isRunning =false;

        //Conficure ML Model
        SetUpTFLiteModel();
        // Init TextToSpeech & Start Server
        mTextToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                Log.i(TAG, "onInit IN");
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTextToSpeech.setLanguage(Locale.ENGLISH);
                    if ( result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "This language is not supported!");
                    } else {
                        Log.i(TAG, "onInit success!");
                        isTextToSpeechInit = true;
                        mTextToSpeech.setPitch(0.6f);
                        mTextToSpeech.setSpeechRate(1.0f);
                    }
                }

                //Start Server from here because AsyncTask affects onInit.
                new StartServerTask().execute();
            }
        });

        //Set camera
        getCameraPermissionsAndSetPreview();
        //Initialise ScheduledThreadPoolExecutor and periodicTask code
        InitThreadPool();

        //Set onClick events for each button
        captureButton = findViewById(R.id.button_capture);
        captureButton2 = findViewById(R.id.button_capture2);
        captureButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ManageThreadPool(true);
                }
            }
        );
        captureButton2.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ManageThreadPool(false);
                }
            }
        );
    }

    /** Pause application */
    @Override
    protected void onPause() {
        Log.i(TAG, "onPause IN");
        try {
            //I end periodic Task
            ManageThreadPool(false);
            //II release camera
            releaseMediaRecorder();       // if you are using MediaRecorder, release it first
            releaseCamera();              // release the camera immediately on pause event
        }
        catch (Exception e) {
            Log.e(TAG, "onPause Exception: " + e.getMessage());
        }
        super.onPause();
    }

    /** Resume application */
    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume IN");
        try{
            //I get camera
            if(mCamera == null) {
                if(checkCameraHardware(this)) {
                    Log.i(TAG, "-> create camera instance");
                    mCamera = getCameraInstance();
                }
                mPreview = new CameraPreview(this, mCamera);
                preview.removeAllViews();
                preview.addView(mPreview);
            }
            //II start periodic Task
            ManageThreadPool(true);
        }catch (Exception ex){
            Log.e(TAG, "onResume Exception: " + ex.getMessage());
        }
    }

    /** Destroy application */
    @Override
    protected void onDestroy(){
        Log.i(TAG, "onDestroy IN");
        //I end periodic Task
        ManageThreadPool(false);
        //II release camera
        releaseCamera();
        releasePreview();
        //III destroy speech
        releaseTextToSpeech();
        //IV stop server
        releaseServer();

        super.onDestroy();
    }

    /** Set camera permissions an preview */
    private void getCameraPermissionsAndSetPreview() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 2);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},  2);
        }
        if(checkCameraHardware(this)){
            mCamera = getCameraInstance();
        }

        mPreview = new CameraPreview(this, mCamera);
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        Log.i(TAG, "checkCameraHardware IN");
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
            return true;
        } else {
            return false;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Log.i(TAG, "getCameraInstance IN");
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            Log.e(TAG, "Error getCameraInstance: " + e.getMessage());
        }
        return c; // returns null if camera is unavailable
    }

    /** Called in onPause */
    private void releaseMediaRecorder(){
        Log.i(TAG, "releaseMediaRecorder IN");
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    /** A safe way to release an instance of the Camera object. */
    private void releaseCamera(){
        Log.i(TAG, "releaseCamera IN");
        if (mCamera != null){
            mCamera.setPreviewCallback(null);
            mPreview.getHolder().removeCallback(mPreview);
            mCamera.release();  // release camera for other apps
            mCamera = null;
        }
    }

    /** Used when releasing the camera */
    private void releasePreview() {
        if(mPreview != null){
            mPreview.destroyDrawingCache();
            mPreview = null;
        }
    }

    /** A safe way to release text to speech object. */
    private void releaseTextToSpeech(){
        Log.i(TAG, "releaseTextToSpeech IN");
        if (mTextToSpeech != null) {
            isTextToSpeechInit = false;
            mTextToSpeech.stop();
            mTextToSpeech.shutdown();
            mTextToSpeech = null;
        }
    }

    /** A safe way to release FitBit server object. */
    private void releaseServer(){
        Log.i(TAG, "releaseServer IN");
        if(server != null) {
            server.endConnection();
            server = null;
        }
    }

    /** Initialise ScheduledThreadPoolExecutor and periodicTask code. */
    private void InitThreadPool() {
        sch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(NUM_THREADS);

        periodicTask = new Runnable(){
            @Override
            public void run() {
                try{
                    if(mCamera != null) {
                        mCamera.takePicture(null, null, mPicture);
                    }
                    else {
                        Log.e(TAG, "-> camera is NULL in thread pool, try create!");
                        mCamera = getCameraInstance();
                    }
                }catch(Exception e){
                    Log.e("PhotoThread", "Take photo error: " + e);
                }
            }
        };
    }

    /**
     * Start or stop thread activity considering present state.
     */
    private void ManageThreadPool(final boolean futureRunState) {
        if(futureRunState && !isRunning)
        {
            periodicFuture = sch.scheduleAtFixedRate(periodicTask, NEW_PHOTO_SEC_PERIOD, NEW_PHOTO_SEC_PERIOD, TimeUnit.SECONDS);
            isRunning = true;
        }
        else if(!futureRunState && isRunning)
        {
            periodicFuture.cancel(true);
            isRunning = false;
        }
    }

    /**
     * Instantiate the DetectorModel Class and set up all parameters.
     */
    private void SetUpTFLiteModel() {
        Log.i(TAG, "SetUpTFLiteModel IN");

        int cropSize = MODEL_INPUT_SIZE;
        int previewWidth = DEFAULT_IMG_SIZE_X;
        int previewHeight = DEFAULT_IMG_SIZE_Y;
        borderedText = new BoxBorderText();

        try {
            detector = TFLiteObjectDetectionAPIModel.create(
                            getAssets(), MODEL_FILE,
                            LABELS_FILE, MODEL_INPUT_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);
        frameToCropTransform = ImageUtils.getTransformationMatrix(
                                    previewWidth, previewHeight,
                                    cropSize, cropSize,false);
        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);
    }

    /**
     * Callback return with data after picture was taken.
     */
    private PictureCallback mPicture = new PictureCallback() {

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.i(TAG, "-> onPictureTaken IN");

            //INFO: Photo conversion steps
            Bitmap imageBitmap = BitmapFactory.decodeByteArray(data , 0, data.length);
            Bitmap rotatedBitmap = ImageUtils.RotateBitmap(imageBitmap, CAMERA_ORIENTATION);
            Canvas canvas = new Canvas(croppedBitmap);
            canvas.drawBitmap(rotatedBitmap, frameToCropTransform, null);

            //INFO: Call ML model
            final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);

            //INFO: Prepare Paint and Canvas fot rectangle draw
            cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
            final Canvas paintCanvas = new Canvas(cropCopyBitmap);
            final Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.0f);

            boolean recognitionFlag =false;
            float highestConfidence = 0;
            String messageTextToSpeak = "";
            for (final Classifier.Recognition result : results) {
                final RectF location = result.getLocation();
                final float confidence = result.getConfidence();
                String title = result.getTitle();

                if (location != null && confidence >= MINIMUM_CONFIDENCE_TF_OD_API) {
                    Log.i(TAG, "-> onPictureTaken, title: " + title + ", confidence: " + confidence + ", location: " + location);

                    //INFO: Consider boxy objects as obstacles
                    if(title.equals("refrigerator") || title.equals("tv") || title.equals("suitcase") ||
                       title.equals("laptop") || title.equals("book") || title.equals("bed") ||
                       title.equals("dining table") ) {
                        title = "obstacle";
                    }

                    if(highestConfidence < confidence) {
                        switch (title) {
                            case "traffic light": {
                                highestConfidence = confidence;
                                mlResultTitle = title;
                                if(ImageUtils.checkGreenVsRedPixels(croppedBitmap)) {
                                    mlResultValue = MLResult.RESULT_GREEN_TRAFFIC_LIGHT;
                                    messageTextToSpeak =  String.format("Green %s.", mlResultTitle);
                                }
                                else {
                                    mlResultValue = MLResult.RESULT_RED_TRAFFIC_LIGHT;
                                    messageTextToSpeak =  String.format("Red %s.", mlResultTitle);
                                }

                                //INFO: place here because mapRect alters location needed for checkAvoidOnObjectLocation
                                recognitionFlag = true;
                                final float cornerSize = Math.min(location.width(), location.height()) / 8.0f;
                                paintCanvas.drawRoundRect(location, cornerSize, cornerSize, paint);
                                borderedText.drawText(paintCanvas, location.left + cornerSize, location.top, title, paint);
                                cropToFrameTransform.mapRect(location);

                                break;
                            }
                            case "bicycle":
                            case "car":
                            case "motorcycle":
                            case "bus":
                            case "fire hydrant":
                            case "bench":
                            case "chair":
                            case "stop sign":
                            case "truck":
                            case "train":
                            case "obstacle": {
                                highestConfidence = confidence;
                                mlResultValue = ImageUtils.checkAvoidOnObjectLocation(location);
                                mlResultTitle = title;
                                switch (mlResultValue) {
                                    case RESULT_OBSTACLE_AVOID_LEFT:
                                        messageTextToSpeak = String.format("%s ahead, avoid left.", mlResultTitle); break;
                                    case RESULT_OBSTACLE_AVOID_RIGHT:
                                        messageTextToSpeak = String.format("%s ahead, avoid right.", mlResultTitle); break;
                                    case RESULT_OBSTACLE_UNAVOIDABLE:
                                        messageTextToSpeak = String.format("%s ahead, can't avoid.", mlResultTitle); break;
                                }

                                //INFO: place here because mapRect alters location needed for checkAvoidOnObjectLocation
                                recognitionFlag = true;
                                final float cornerSize = Math.min(location.width(), location.height()) / 8.0f;
                                paintCanvas.drawRoundRect(location, cornerSize, cornerSize, paint);
                                borderedText.drawText(paintCanvas, location.left + cornerSize, location.top, title, paint);
                                cropToFrameTransform.mapRect(location);

                                break;
                            }
                            default:
                                Log.i(TAG,"onPictureTaken: default branch!");
                                mlResultValue = MLResult.RESULT_UNDEFINED;
                                break;
                        }
                    }
                }
            }
            //INFO: If recognition was done put bitmap with rectangle on View
            if(recognitionFlag) {
                imageView.setImageBitmap(cropCopyBitmap);
            }
            else {
                imageView.setImageBitmap(croppedBitmap);
            }

            //INFO: If all is ok send data to server
            if(mlResultValue != MLResult.RESULT_UNDEFINED) {
                if(server != null) {
                    Log.i(TAG, "server != null try sendMessage");
                    try {
                        server.sendMessage(mlResultValue,mlResultTitle);
                    } catch (JSONException e) {
                        Log.e(TAG, "onPictureTaken JSONException: " + e);
                    }
                }

                speak(messageTextToSpeak);
                mlResultValue = MLResult.RESULT_UNDEFINED;
            }
        }
    };

    /**
     * Use TextToSpeech to send audio message.
     */
    public void speak(String text) {
        if(isTextToSpeechInit) {
            Log.i(TAG, "speak: " + text);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mTextToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                mTextToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
        else {
            Log.e(TAG, "speack: TextToSpeech is not initialised!");
        }
    }

/**
 * AsyncTask to run the WebSocket Local Server. (communication between Android and FitBit)
 */
class StartServerTask extends AsyncTask<Void, Void, Void> {
    private Exception exception;
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8887;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected Void doInBackground(Void... params) {
        try {
            Log.d(TAG, "CreateSSLServer IN");
            server = new FitBitSocketServer(new InetSocketAddress(HOST, PORT));

            Log.d(TAG, "CreateSSLServer run");
            server.run();
        } catch (Exception e) {
            this.exception = e;
            Log.d(TAG, "StartServerTask doInBackground ex: " + exception);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        Log.d(TAG, "onPostExecute doInBackground ex: " + exception);
        super.onPostExecute(result);
    }
}

}


