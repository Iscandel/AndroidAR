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

public class MainActivity extends AppCompatActivity implements Camera.PreviewCallback {

    private enum Algo {ORB, SIFT};

    private CameraManager cameraManager;
    private CameraDevice myCamera;
    private CameraCaptureSession myCaptureSession;
    //private SurfaceView mySurface;
    private ImageReader myImageReader;
    private Algo myAlgo;
    private int myImageWidth;
    private int myImageHeight;
    String myCameraId;

    Surface myPreviewSurface;

    final int CAMERA_PERMISSION_CODE = 1;
    //int CAMERA_BACK;

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

        //mySurface = new SurfaceView(this);
        //Mat mRgba = new Mat(400, 800, CvType.CV_8UC4);

        //SessionConfiguration sessionConfig = new SessionConfiguration()
        //myCaptureSession = myCamera.createCaptureSession();

        myImageWidth = 400;
        myImageHeight = 400;

        this.setupCamera();

        TextureView view = findViewById(R.id.textureView);
        view.setSurfaceTextureListener(textureListener);

//        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
//        else
//            openCamera();

        //cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        //cameraFacing = CameraCharacteristics.LENS_FACING_BACK;
        //this.setupCamera();
        //this.openCamera();
        //createCameraPreviewSession();
        myAlgo = Algo.ORB;
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

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

    }

    private void createCameraPreviewSession() {
        try {
            //SurfaceView view = this.findViewById(R.id.surfaceView3);
            //myPreviewSurface = view.getHolder().getSurface();
            TextureView view = findViewById(R.id.textureView);
            SurfaceTexture texture = view.getSurfaceTexture();
            assert texture != null;
            myPreviewSurface = new Surface(texture);

            // We configure the size of default buffer to be the size of camera preview we want.
            //texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            //Surface surface = new Surface(texture);

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
                            //showToast("Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }



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
                }

                return;
        }
    }

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
}

    /*public void detector(Mat image, Algo algo, MatOfKeyPoint kp, Mat desc) {
        if (algo == Algo.ORB)
            orbDetector(image, kp, desc);
        else
            siftDetector(image, kp, desc);
    }

    public void siftDetector(Mat image, MatOfKeyPoint kp, Mat desc) {
        //Initiate SIFT detector
        org.opencv.xfeatures2d.SIFT
                sift = cv2.xfeatures2d.SIFT_create();

        //find the keypoints and descriptors with SIFT
        sift.detectAndCompute(image, null, kp, desc);

    }

    public void orbDetector(Mat image, MatOfKeyPoint kp, Mat desc) {
        //Initiate ORB detector
        org.opencv.features2d.ORB orb = org.opencv.features2d.ORB.create();

        orb.detectAndCompute(image, null, kp, desc);
    }

    public MatOfDMatch  match(Mat desc1, Mat desc2, Algo algo) {


            if(algo == Algo.ORB) {
                org.opencv.features2d.BFMatcher bf = new org.opencv.features2d.BFMatcher(org.opencv.core.Core.NORM_HAMMING, true);

                MatOfDMatch matches = new MatOfDMatch ();

            //Match descriptors.
                bf.match(desc1, desc2, matches);
            //matches = bf.knnMatch(np.asarray(des1, np.uint8), np.asarray(des2, np.uint8), k = 2)

            //Sort them in the order of their distance.
                matches = sorted(matches, key = lambda x:x.distance)

                return matches;
            }
    else {
                List<MatOfDMatch> matches = new ArrayList<>();
                BFMatcher bf = new BFMatcher();
                bf.knnMatch(desc1, desc2, matches, 2);

                //store all the good matches as per Lowe's ratio test.
                vector<KeyPoint> matched1, matched2, inliers1, inliers2;
                MatOfDMatch goodMatches = new MatOfDMatch();
                for (int i = 0; i < matches.size(); i++) {
                    MatOfDMatch match = matches.get(i);

                    DMatch first = match..get(0);//[i][0];
                    float dist1 = nn_matches[i][0].distance;
                    float dist2 = nn_matches[i][1].distance;

                    if (dist1 < nn_match_ratio * dist2) {
                        goodMatches.push_back(first);
                    }
                        matched1.push_back(kpts1[first.queryIdx]);
                        matched2.push_back(kpts2[first.trainIdx]);
                    }
                }
            }
            good = []
            for m,n in matches:
            if m.distance < 0.7*n.distance:
            good.append(m)

            return good

    public Mat androidImageToCv(Image image){
        Mat res = null;
        return res;
    }

    public void processFrame(Mat image) {
        //Image image = reader.acquireNextImage();
        //org.opencv.android.Utils.bitmapToMat();
        MatOfKeyPoint kpFrame = new MatOfKeyPoint();
        Mat descFrame = new Mat();
        detector(image, myAlgo, kpFrame, descFrame);
        matches = match(descRef, descFrame, algo)

        # compute Homography if enough matches are found
        if len(matches) > MIN_MATCHES:
        #Find homography
        src_pts = np.float32([kpRef[m.queryIdx].pt for m in matches]).reshape(-1, 1, 2)
        dst_pts = np.float32([kpFrame[m.trainIdx].pt for m in matches]).reshape(-1, 1, 2)

        h, status = cv2.findHomography(src_pts, dst_pts, cv2.RANSAC)#,5.0)

        if h is not None:
            # Draw a rectangle that marks the found model in the frame
                height, w, channel = imgRef.shape
        pts = np.float32([[0, 0], [0, height - 1], [w - 1, height - 1], [w - 1, 0]]).reshape(-1, 1, 2)

        if drawBorder:
        dst = cv2.perspectiveTransform(pts, h)
                # connect them with lines
        frame = cv2.polylines(frame, [np.int32(dst)], True, 255, 3, cv2.LINE_AA)
                #cv2.imshow('frame', frame)
                #cv2.waitKey(5)

        if drawMatches:
                # Draw first 10 matches.
                frame = cv2.drawMatches(imgRef,kpRef,frame,kpFrame,matches[:50], None, flags=2)
        frame = cv2.resize(frame,(1280,720))
                #img3 = cv.drawMatchesKnn(img1,kp1,img2,kp2,good,flags=2)
                #img3 = cv2.resize(img3,(800, 600), interpolation = cv2.INTER_CUBIC)
                #cv2.imshow('Matches',img3)
                #plt.figure('Matches test.')
                #plt.imshow(img3), plt.show()

        h2 = projection_matrix(K, h)
            #h2 = np.float32([[h[0,0], h[0,1], 0, h[0,2]], [h[1,0], h[1,1], 0, h[1,2]], [h[2,0], h[2,1], 0, h[2,2]]])

        print(h2)

            #print obj
        frame = render(frame, obj, h2, imgRef, False)
        plt.imshow(frame[:, :, ::-1])
        plt.pause(0.05)#, plt.show()
            #cv2.waitKey(5)

        videoWriter.write(frame)
    }
}
*/








/*
private void startPreview(CameraDevice camera) throws CameraAccessException {
    SurfaceTexture texture = mPreviewView.getSurfaceTexture();

    // to set PREVIEW size
    texture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
    surface = new Surface(texture);
    try {
        // to set request for PREVIEW
        mPreviewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
    } catch (CameraAccessException e) {
        e.printStackTrace();
    }

    mImageReader = ImageReader.newInstance(mImageWidth, mImageHeight, ImageFormat.YUV_420_888, 2);

    mImageReader.setOnImageAvailableListener(mOnImageAvailableListener,mHandler);

    mPreviewBuilder.addTarget(mImageReader.getSurface());

    //output Surface
    List<Surface> outputSurfaces = new ArrayList<>();
    outputSurfaces.add(mImageReader.getSurface());

    camera.createCaptureSession(outputSurfaces, mSessionStateCallback, mHandler);
            }


private CameraCaptureSession.StateCallback mSessionStateCallback = new CameraCaptureSession.StateCallback() {

@Override
public void onConfigured(CameraCaptureSession session) {
        try {
        updatePreview(session);
        } catch (CameraAccessException e) {
        e.printStackTrace();
        }
        }

@Override
public void onConfigureFailed(CameraCaptureSession session) {

        }
        };

private void updatePreview(CameraCaptureSession session)
        throws CameraAccessException {
        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);

        session.setRepeatingRequest(mPreviewBuilder.build(), null, mHandler);
        }


private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

@Override
public void onImageAvailable(ImageReader reader) {
        // get the newest frame
        Image image = reader.acquireNextImage();

        if (image == null) {
        return;
        }

        // print image format
        int format = reader.getImageFormat();
        Log.d(TAG, "the format of captured frame: " + format);

        // HERE to call jni methods
        JNIUtils.display(image.getWidth(), image.getHeight(), image.getPlanes()[0].getBuffer(), surface);


        //ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        //byte[] bytes = new byte[buffer.remaining()];


        image.close();
        }
        };
 */


/* @Override
        public void onOpened(CameraDevice camera) {
            Toast.makeText(getApplicationContext(), "onOpened", Toast.LENGTH_SHORT).show();

            //requesting permission
            int permissionCheck = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {

                } else {
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
                    Toast.makeText(getApplicationContext(), "request permission", Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(getApplicationContext(), "PERMISSION_ALREADY_GRANTED", Toast.LENGTH_SHORT).show();
            }
            */