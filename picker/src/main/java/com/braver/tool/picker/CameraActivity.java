/*
 *
 *  * Created by https://github.com/braver-tool on 12/10/21, 08:30 PM
 *  * Copyright (c) 2021 . All rights reserved.
 *  * Last modified 23/03/22, 09:45 AM
 *
 */

package com.braver.tool.picker;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
import static android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;

@SuppressWarnings("ALL")
public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnClickListener {
    private static final String PROCESSING_VIDEO = "Processing a video...";
    private final String TAG = CameraActivity.class.getSimpleName();
    private final Handler customHandler = new Handler();
    //private OrientationEventListener myOrientationEventListener;
    //private final int iOrientation = 0;
    private int flag = 0;
    private int flashType = 1;
    private int mOrientation = 90;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private Camera.PictureCallback jpegCallback;
    private MediaRecorder mediaRecorder;
    private SurfaceView imgSurface;
    private ImageView imgCapture;
    private ImageView imgFlashOnOff;
    private ImageView imgSwipeCamera;
    private TextView textCounter;
    private TextView hintTextView;
    private BackgroundTaskForSavingVideo saveVideoTask = null;
    private File recordedVideoFilePath = null;
    private File recordedImageFilePath = null;
    private long startTime = SystemClock.uptimeMillis();
    private final Runnable updateTimerThread = new Runnable() {
        @SuppressLint("SetTextI18n")
        public void run() {
            long timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            long timeSwapBuff = 0L;
            long updatedTime = timeSwapBuff + timeInMilliseconds;
            int seconds = (int) (updatedTime / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            textCounter.setText(String.format(Locale.getDefault(), "%02d", minutes) + ":" + String.format(Locale.getDefault(), "%02d", seconds));
            customHandler.postDelayed(this, 0);

        }

    };
    private BackgroundTaskForSavingImage savePicTask;
    private String mediaFileName = null;
    private Context context;
    private String bVideoName = "";

    /**
     * this method used to initiate declare and initiate for Message attachment Camera and video activity
     *
     * @param savedInstanceState - this object is responsible to create the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity);
        initializeViews();
    }

    /**
     * Handling to mapping the xml view from{@link R.layout#activity_camera}
     */
    private void initializeViews() {
        context = getApplicationContext();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        initControls();
        initCameraActivity();
    }

    /**
     * Method used to call Camera activity's lifecycle after check permission
     */
    private void initCameraActivity() {
        File f = BraveFileUtils.getRandomRecordedVideoFileName(CameraActivity.this);
        //create a folder to get image
        recordedVideoFilePath = new File(f.getPath());
        if (!recordedVideoFilePath.exists()) {
            recordedVideoFilePath.mkdirs();
        }
        //capture image on callback
        captureImageCallback();
        //
        if (camera != null) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                imgFlashOnOff.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Called when Activity content is not visible because user resume previous activity
     */
    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (customHandler != null) customHandler.removeCallbacksAndMessages(null);
            releaseMediaRecorder();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when back navigation
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        cancelSavePicTaskIfNeed();
    }

    /**
     * This is called immediately after the surface is first created.
     *
     * @param arg0- The SurfaceHolder whose surface is being created
     */
    @Override
    public void surfaceCreated(SurfaceHolder arg0) {
        try {
            if (flag == 0) {
                camera = Camera.open(0);
            } else {
                camera = Camera.open(1);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            return;
        }
        try {
            Camera.Parameters param;
            param = camera.getParameters();
            final List<String> modes = param.getSupportedFocusModes();
            if (modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_MACRO)) {
                param.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                param.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                param.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
            }
            List<Camera.Size> sizes = param.getSupportedPreviewSizes();
            //get diff to get perfect preview sizes
            DisplayMetrics displaymetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            int height = displaymetrics.heightPixels;
            int width = displaymetrics.widthPixels;
            long diff = (height * 1000 / width);
            long cdistance = Integer.MAX_VALUE;
            int idx = 0;
            for (int i = 0; i < sizes.size(); i++) {
                long value = (long) (sizes.get(i).width * 1000) / sizes.get(i).height;
                if (value > diff && value < cdistance) {
                    idx = i;
                    cdistance = value;
                }
            }
            Camera.Size cs = sizes.get(idx);
            param.setPreviewSize(cs.width, cs.height);
            param.setPictureSize(cs.width, cs.height);
            camera.setParameters(param);
            setCameraDisplayOrientation(0);
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
            if (flashType == 1) {
                param.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                imgFlashOnOff.setImageResource(R.drawable.ic_automatic_flash);
            } else if (flashType == 2) {
                param.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                Camera.Parameters params;
                if (camera != null) {
                    params = camera.getParameters();

                    if (params != null) {
                        List<String> supportedFlashModes = params.getSupportedFlashModes();
                        if (supportedFlashModes != null) {
                            if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                                param.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                            } else if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
                                param.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                            }
                        }
                    }
                }
                imgFlashOnOff.setImageResource(R.drawable.ic_flash_on);
            } else if (flashType == 3) {
                param.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                imgFlashOnOff.setImageResource(R.drawable.ic_flash_off);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This is called immediately before a surface is being destroyed.
     *
     * @param arg0-The SurfaceHolder whose surface is being destroyed.
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
        try {
            camera.stopPreview();
            camera.release();
            camera = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This is called immediately after any structural changes (format or size) have been made to the surface.
     * You should at this point update the imagery in the surface.
     * This method is always called at least once, after surfaceCreated(SurfaceHolder).
     *
     * @param surfaceHolder -The SurfaceHolder whose surface has changed.
     * @param i             -int: The new PixelFormat of the surface.
     * @param i1            -int: The new width of the surface. Value is 0 or greater
     * @param i2            -int:The new height of the surface. Value is 0 or greater
     */
    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        refreshCamera();
        Camera.Parameters parameters = camera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        camera.setParameters(parameters);
        camera.startPreview();
    }

    /**
     * onClick is used to define the function to be invoked in the activity when the button is clicked.
     *
     * @param v - The view that was clicked. It is the view that was clicked.
     */
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.imgFlashOnOff) {
            flashToggle();
        } else if (id == R.id.imgChangeCamera) {
            switchCameraType();
        }
    }

    /**
     * This Method is used to initialize all views
     * Method call from {@link #onCreate(Bundle)}
     */
    private void initControls() {
        mediaRecorder = new MediaRecorder();
        imgSurface = findViewById(R.id.imgSurface);
        textCounter = findViewById(R.id.textCounter);
        imgCapture = findViewById(R.id.imgCapture);
        imgFlashOnOff = findViewById(R.id.imgFlashOnOff);
        imgSwipeCamera = findViewById(R.id.imgChangeCamera);
        textCounter.setVisibility(View.GONE);
        hintTextView = findViewById(R.id.hintTextView);
        surfaceHolder = imgSurface.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        imgSwipeCamera.setOnClickListener(this);
        activeCameraCapture();
        imgFlashOnOff.setOnClickListener(this);
    }

    /**
     * This method is used for handle camera capture and video recording
     * Method call from    {@link #initControls()},{@link #savePicTask}
     */
    @SuppressLint("ClickableViewAccessibility")
    private void activeCameraCapture() {
        if (imgCapture != null) {
            imgCapture.setAlpha(1.0f);
            imgCapture.setOnLongClickListener(v -> {
                hintTextView.setVisibility(View.INVISIBLE);
                try {
                    if (prepareMediaRecorder()) {
                        //myOrientationEventListener.disable();
                        mediaRecorder.start();
                        startTime = SystemClock.uptimeMillis();
                        customHandler.postDelayed(updateTimerThread, 0);
                    } else {
                        return false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                textCounter.setVisibility(View.VISIBLE);
                imgSwipeCamera.setVisibility(View.GONE);
                imgFlashOnOff.setVisibility(View.GONE);
                imgCapture.setOnTouchListener((v1, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_BUTTON_PRESS) {
                        return true;
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        hintTextView.setVisibility(View.VISIBLE);
                        cancelSaveVideoTaskIfNeed();
                        saveVideoTask = new BackgroundTaskForSavingVideo();
                        saveVideoTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
                        return true;
                    }
                    return true;
                });
                return true;
            });
            imgCapture.setOnClickListener(v -> captureImage());
        }
    }

    /**
     * This method is used for handle front and back camera
     * Method call from    {@link #onClick(View)} ()}
     */
    private void switchCameraType() {
        camera.stopPreview();
        camera.release();
        if (flag == Camera.CameraInfo.CAMERA_FACING_BACK) {
            flag = Camera.CameraInfo.CAMERA_FACING_FRONT;
            imgFlashOnOff.setVisibility(View.GONE);
        } else {
            flag = Camera.CameraInfo.CAMERA_FACING_BACK;
            imgFlashOnOff.setVisibility(View.VISIBLE);
        }
        camera = Camera.open(flag);
        try {
            camera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        camera.startPreview();
        Camera.Parameters cameraParameters = camera.getParameters();
        refreshCameraPreview(cameraParameters);
    }

    /**
     * switch image to on and off state just to indicate flashlight status.
     * Method call from    {@link #onClick(View)}
     */
    private void flashToggle() {
        if (flashType == 1) {
            flashType = 2;
        } else if (flashType == 2) {
            flashType = 3;
        } else if (flashType == 3) {
            flashType = 1;
        }
        refreshCamera();
    }

    /**
     * this method is used for get an image from the camera
     * Method call from    {@link #activeCameraCapture()}
     */
    private void captureImage() {
        camera.takePicture(null, null, jpegCallback);
        inActiveCameraCapture();
    }

    /**
     * This method is used for set alpha  of the click actions
     * Method call from {@link #captureImage()}
     */
    private void inActiveCameraCapture() {
        if (imgCapture != null) {
            imgCapture.setAlpha(0.5f);
            imgCapture.setOnClickListener(null);
        }
    }

    /**
     * this method is used to get image,validate image rotation rotation and save local path as bitmap
     * Method call from    {@link #onCreate(Bundle)}
     */
    private void captureImageCallback() {
        surfaceHolder = imgSurface.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        jpegCallback = (data, camera) -> {
            if (data != null) {
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                Matrix mtx = new Matrix();
                Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
                int w = bm.getWidth();
                int h = bm.getHeight();
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT && flag == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    mtx.postRotate(90);
                    // Rotating Bitmap
                    bm = Bitmap.createBitmap(bm, 0, 0, w, h, mtx, true);
                } else if ((getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT && flag == Camera.CameraInfo.CAMERA_FACING_FRONT)) {
                    mtx.postRotate(-90);
                    mtx.postScale(-1, 1);
                    // Rotating Bitmap
                    bm = Bitmap.createBitmap(bm, 0, 0, w, h, mtx, true);
                } else {// LANDSCAPE MODE
                    //No need to reverse width and height
                    bm = Bitmap.createScaledBitmap(bm, screenWidth, screenHeight, true);
                }
                refreshCamera();
                cancelSavePicTaskIfNeed();
                savePicTask = new BackgroundTaskForSavingImage(bm);
                savePicTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
            }
        };
    }

    /**
     * This method is used to validate surface ,flash toggle and taken image surface refresh actions
     * Method call from    {@link #surfaceChanged(SurfaceHolder, int, int, int)},{@link #flashToggle()},{@link #captureImageCallback()}
     */
    public void refreshCamera() {
        if (surfaceHolder.getSurface() == null) {
            return;
        }
        try {
            camera.stopPreview();
            Camera.Parameters param = camera.getParameters();
            if (flag == 0) {
                if (flashType == 1) {
                    param.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                    imgFlashOnOff.setImageResource(R.drawable.ic_automatic_flash);
                } else if (flashType == 2) {
                    param.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                    Camera.Parameters params;
                    if (camera != null) {
                        params = camera.getParameters();
                        if (params != null) {
                            List<String> supportedFlashModes = params.getSupportedFlashModes();
                            if (supportedFlashModes != null) {
                                if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                                    param.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                                } else if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
                                    param.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                                }
                            }
                        }
                    }
                    imgFlashOnOff.setImageResource(R.drawable.ic_flash_on);
                } else if (flashType == 3) {
                    param.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    imgFlashOnOff.setImageResource(R.drawable.ic_flash_off);
                }
            }
            refreshCameraPreview(param);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * this method is used to refresh camera preview view
     *
     * @param param - It is a camera parameter
     *              Method call from  {@link #refreshCamera()}
     */
    private void refreshCameraPreview(Camera.Parameters param) {
        try {
            camera.setParameters(param);
            setCameraDisplayOrientation(0);
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is used to handle camera rotation orientation
     *
     * @param cameraId - It is captured image id
     *                 Method call from   {@link #surfaceCreated(SurfaceHolder)},{@link #refreshCamera()}
     */
    public void setCameraDisplayOrientation(int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        Camera.Parameters p = null;
        try {
            p = camera.getParameters();
        } catch (Exception e) {
            Log.d(TAG, e.getMessage() != null ? e.getMessage() : "");

        }
        camera.setDisplayOrientation(result);
        List<String> supportedFocusModes = null;
        if (p != null) {
            supportedFocusModes = p.getSupportedFocusModes();
        }
        Camera.Parameters param = camera.getParameters();
        if (supportedFocusModes != null) {
            if (supportedFocusModes.contains(FOCUS_MODE_CONTINUOUS_PICTURE)) {
                param.setFocusMode(FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (supportedFocusModes.contains(FOCUS_MODE_CONTINUOUS_VIDEO)) {
                param.setFocusMode(FOCUS_MODE_CONTINUOUS_VIDEO);
            }
        }
        camera.setParameters(param);
    }

    /**
     * @param bitmap -It is a captured image
     * @return -Returns saved image local path
     */
    private String saveCapturedImageToLocalPath(Bitmap bitmap) {
        String imagePath = "";
        try {
            if (bitmap != null) {
                File tempFilePath = BraveFileUtils.getRandomImageFileName(this);
                BraveFileUtils.saveBitmapToExternalStorage(context, bitmap, tempFilePath);
                Bitmap imageBitMap = BraveFileUtils.checkIsBitMapRotated(context, true, tempFilePath);
                if (imageBitMap != null) {
                    recordedImageFilePath = BraveFileUtils.getRandomImageFileName(this);
                    BraveFileUtils.saveBitmapToExternalStorage(context, imageBitMap, recordedImageFilePath);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage() != null ? e.getMessage() : "");
        }
        return imagePath;
    }

    /**
     * This method is used to cancel the camera actions
     * Method call from    {@link #onBackPressed()}, {@link #captureImageCallback()}
     */
    private void cancelSavePicTaskIfNeed() {
        if (savePicTask != null && savePicTask.getStatus() == AsyncTask.Status.RUNNING) {
            savePicTask.cancel(true);
        }
    }


    /**
     * This method is used for recording video, validate orientation view and validate video size
     *
     * @return -It returns boolean value
     */
    @SuppressLint("SimpleDateFormat")
    protected boolean prepareMediaRecorder() {
        mediaRecorder = new MediaRecorder();
        camera.stopPreview();
        camera.unlock();
        mediaRecorder.setCamera(camera);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        if (flag == 1) {
            mediaRecorder.setProfile(CamcorderProfile.get(1, CamcorderProfile.QUALITY_480P));
        } else {
            mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));
        }
        mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
        mediaRecorder.setOrientationHint(mOrientation);
        if (Build.MODEL.equalsIgnoreCase("Nexus 6") && flag == 1) {
            mediaRecorder.setOrientationHint(mOrientation);
        } else if (mOrientation == 90 && flag == 1) {
            mOrientation = 270;
            mediaRecorder.setOrientationHint(mOrientation);
        } else if (flag == 1) {
            mediaRecorder.setOrientationHint(mOrientation);
        }
        int random = new Random().nextInt(6997);
        bVideoName = "Braver_VID".concat("_") + random + ".mp4";
        mediaRecorder.setOutputFile(recordedVideoFilePath.getAbsolutePath() + "/" + bVideoName);
        mediaRecorder.setOnInfoListener((mr, what, extra) -> {
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                long downTime = 0;
                long eventTime = 0;
                //float x = 0.0f;
                //float y = 0.0f;
                int metaState = 0;
                MotionEvent motionEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, 0, 0, metaState);
                imgCapture.dispatchTouchEvent(motionEvent);
                Toast.makeText(CameraActivity.this, "You reached to Maximum(17MB) video size.", Toast.LENGTH_SHORT).show();
            }
        });
        mediaRecorder.setMaxFileSize(1000 * 17 * 1000);
        try {
            mediaRecorder.prepare();
        } catch (Exception e) {
            releaseMediaRecorder();
            Log.d(TAG, e.getMessage() != null ? e.getMessage() : "");
            return false;
        }
        return true;

    }

    /**
     * This method is used to navigates saved video saved path in Message fragment
     *
     * @param videoPath Method call from {@link #saveVideoTask}
     */
    public void onVideoSendDialog() {
        Intent intent = new Intent();
        intent.putExtra(BraveFileUtils.IS_IMAGE_FILE, false);
        intent.putExtra(BraveFileUtils.VIDEO_PATH, recordedVideoFilePath.getAbsolutePath() + "/" + bVideoName);
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * This method is used for cancel video recording
     * Method call from {@link #activeCameraCapture()} ()}
     */
    private void cancelSaveVideoTaskIfNeed() {
        if (saveVideoTask != null && saveVideoTask.getStatus() == AsyncTask.Status.RUNNING) {
            saveVideoTask.cancel(true);
        }
    }

    /**
     * This method is used for clear recorder configuration and release the recorder object
     * Method call from {@link #onPause()}, {@link #prepareMediaRecorder(), {@link #saveVideoTask}}
     */
    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = new MediaRecorder();
        }
    }

    //------------------SURFACE OVERRIDE METHODS END--------------------//

    private void sendImagePath() {
        Intent intent = new Intent();
        intent.putExtra(BraveFileUtils.IS_IMAGE_FILE, true);
        intent.putExtra(BraveFileUtils.IMAGE_PATH, recordedImageFilePath.getAbsolutePath());
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * This method is used to save picture in local path
     * * Method call from    {@link #captureImageCallback()}
     */
    @SuppressLint("StaticFieldLeak")
    private class BackgroundTaskForSavingImage extends AsyncTask<Void, Void, String> {
        private final Bitmap data;

        public BackgroundTaskForSavingImage(Bitmap data) {
            this.data = data;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                return saveCapturedImageToLocalPath(data);
            } catch (Exception e) {
                Log.d(TAG, e.getMessage() != null ? e.getMessage() : "");
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            activeCameraCapture();
            sendImagePath();
        }
    }

    /**
     * This method is used for save video in local path
     * Method call from {@link #activeCameraCapture()}
     */

    @SuppressLint("StaticFieldLeak")
    private class BackgroundTaskForSavingVideo extends AsyncTask<Void, Void, Void> {
        ProgressDialog progressDialog = null;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(CameraActivity.this);
            progressDialog.setMessage(PROCESSING_VIDEO);
            progressDialog.show();
            imgCapture.setOnTouchListener(null);
            textCounter.setVisibility(View.GONE);
            imgSwipeCamera.setVisibility(View.VISIBLE);
            imgFlashOnOff.setVisibility(View.VISIBLE);
            super.onPreExecute();

        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                try {
                    customHandler.removeCallbacksAndMessages(null);
                    mediaRecorder.stop();
                    releaseMediaRecorder();
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage() != null ? e.getMessage() : "");
                }
            } catch (Exception e) {
                Log.d(TAG, e.getMessage() != null ? e.getMessage() : "");

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (progressDialog != null) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }
            if (recordedVideoFilePath != null && recordedVideoFilePath.getAbsolutePath() != null) {
                onVideoSendDialog();
            }
        }
    }
}