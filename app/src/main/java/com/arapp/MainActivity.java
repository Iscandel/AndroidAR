package com.arapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraCaptureSession;
import android.widget.Toast;

import org.opencv.core.DMatch;
import org.opencv.features2d.BFMatcher;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.features2d.BFMatcher;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity { //implements Camera.PreviewCallback {

    //Manager for all available cameras of the device
    private CameraManager cameraManager;
    //Camera device
    private CameraDevice myCamera;
    //Capture session reference
    private CameraCaptureSession myCaptureSession;
    //private SurfaceView mySurface;
    private ImageReader myImageReader;
    private int myImageWidth;
    private int myImageHeight;
    //Id of the used camera
    String myCameraId;
    //Surface used for the preview process
    Surface myPreviewSurface;

    final int CAMERA_PERMISSION_CODE = 1;

    private final CameraDevice.StateCallback myStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            //mCameraOpenCloseLock.release();
            myCamera = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            //mCameraOpenCloseLock.release();
            cameraDevice.close();
            myCamera = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            //mCameraOpenCloseLock.release();
            cameraDevice.close();
            myCamera = null;
            MainActivity.this.finish();
            //Activity activity = getActivity();
            //if (null != activity) {
            //    this.finish();
            //}
        }

    };

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            else
                openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    protected ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            // get the newest frame
//            Image image = reader.acquireNextImage();
//
//            if (image == null) {
//                return;
//            }
//
//            // print image format
//            int format = reader.getImageFormat();
//            Log.d(TAG, "the format of captured frame: " + format);
//
//            // HERE to call jni methods
//            JNIUtils.display(image.getWidth(), image.getHeight(), image.getPlanes()[0].getBuffer(), surface);
//
//
//            //ByteBuffer buffer = image.getPlanes()[0].getBuffer();
//            //byte[] bytes = new byte[buffer.remaining()];
//
//
//            image.close();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        myImageWidth = 400;
        myImageHeight = 400;

        this.setupCamera();

        TextureView view = findViewById(R.id.textureView);
        view.setSurfaceTextureListener(textureListener);

//        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
//        else
//            openCamera();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

//    @Override
//    public void onPreviewFrame(byte[] data, Camera camera) {
//
//    }

    //=========================================================================
    ///////////////////////////////////////////////////////////////////////////
    private void createCameraPreviewSession() {
        try {
            //SurfaceView view = this.findViewById(R.id.surfaceView3);
            //myPreviewSurface = view.getHolder().getSurface();
            TextureView view = findViewById(R.id.textureView);
            SurfaceTexture texture = view.getSurfaceTexture();
            // We configure the size of default buffer to be the size of camera preview we want.
            //texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            assert texture != null;
            //Output surface for the preview
            myPreviewSurface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            final CaptureRequest.Builder previewRequestBuilder
                    = myCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(myPreviewSurface);//surface);
            //myImageReader = ImageReader.newInstance(myImageWidth, myImageHeight, ImageFormat.YUV_420_888, 2);

            // Here, we create a CameraCaptureSession for camera preview.
            myCamera.createCaptureSession(Arrays.asList(myPreviewSurface),//, myImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (myCamera == null) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            myCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                //setAutoFlash(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.
                                CaptureRequest previewRequest = previewRequestBuilder.build();
                                myCaptureSession.setRepeatingRequest(previewRequest, null, null);
                                        //myCaptureCallback, myBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(getApplicationContext(), "Failed to configure the camera", Toast.LENGTH_SHORT).show();
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //=========================================================================
    ///////////////////////////////////////////////////////////////////////////
    private void setupCamera() {
        try {
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics =
                        cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_BACK) {
                    StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    //previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
                    this.myCameraId = cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //=========================================================================
    ///////////////////////////////////////////////////////////////////////////
    private void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(myCameraId, myStateCallback, null);// backgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //=========================================================================
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] results) {
        switch (requestCode) {
            case CAMERA_PERMISSION_CODE:
                if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted
                    openCamera();
                } else {
                    // permission denied
                    Toast.makeText(getApplicationContext(), "Camera permission must be enabled", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }
}

