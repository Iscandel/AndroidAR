package com.arapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.Menu;
import android.view.MenuItem;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraCaptureSession;
import android.widget.ImageView;
import android.widget.Toast;

import com.ar.core.Matcher;
import com.ar.core.Utils;
import com.ar.loader3d.ObjLoader;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.CvType;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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
    private ObjLoader myMesh;

    final int CAMERA_PERMISSION_CODE = 1;

    //
    int MIN_MATCHES = 10;

    Matcher.Algo algo = Matcher.Algo.ORB;
    boolean firstFrame = true;
    boolean drawMatches = false;
    boolean drawBorder = true;

    //Camera parameters
    int imageRefSizeX = 5312;//5312 x 2988 #nb imgref was redim, we cannot use it
    int imageRefSizeY = 2988;
    double pixelSizeMm = 0.00112;
    double focalLengthMm = 4.3;
    double fpixel;// = focalLengthMm / pixelSizeMm
    Mat K = null;
    Matcher matcher = new Matcher();
    Mat myImgRef;
    //


    Bitmap testref;
    Handler uiHandler;
    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
            Log.e(MainActivity.class.getName(), "OpenCV could not be initialized");
        }
        //System.loadLibrary("opencv_java3");
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

    //Preview texture listener
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

    //processing-purpose texture listener
    protected ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            // get the newest frame
            Image image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            //for jpeg image
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length,null);
            Mat frame = new Mat();//
            org.opencv.android.Utils.bitmapToMat(bitmap, frame);

            //yuv ?
//           Mat buf = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC1);
//           org.opencv.android.Utils.
//           ByteBuffer buffer = image.getPlanes()[0].getBuffer();
//           byte[] bytes = new byte[buffer.remaining()];
//           buffer.get(bytes);
//           buf.put(0, 0, bytes);
//
//            Mat mat = Imgcodecs.imdecode(buf, Imgcodecs.IMREAD_COLOR);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inScaled = false;

            //Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.frametest, o);


            //org.opencv.android.Utils.bitmapToMat(b, frame);
            //b.recycle();

            if (firstFrame) {
                //InputStream stream = getResources().openRawResource(R.raw.ref2corrigee);

                Bitmap ref = BitmapFactory.decodeResource(getResources(), R.drawable.ref2corrigee, o);

                myImgRef = new Mat();
                org.opencv.android.Utils.bitmapToMat(testref, myImgRef);

                matcher.computeReferenceImage(myImgRef, algo);
                firstFrame = false;

                //For video, resize pixel size accordingly
                pixelSizeMm = pixelSizeMm * (imageRefSizeX / (double) frame.width());
                fpixel = focalLengthMm / pixelSizeMm;
                K = new Mat(3, 3, CvType.CV_64FC1);
                K.put(0, 0, fpixel);
                K.put(0, 1, 0.);
                K.put(0, 2, frame.width() / 2.);
                K.put(1, 0, 0);
                K.put(1, 1, fpixel);
                K.put(1, 2, frame.height() / 2.);
                K.put(2, 0, 0);
                K.put(2, 1, 0);
                K.put(2, 2, 1);
            }

            Mat h = matcher.computeHomography(frame, algo);
            //h.convertTo(h, CvType.CV_32F);
            if (h != null && !h.empty()) {
                if (drawBorder) {
                    Size size = myImgRef.size();
                    MatOfPoint2f pts = new MatOfPoint2f(new Point(0f, 0f),
                            new Point(0f, size.height - 1),
                            new Point(size.width - 1, size.height - 1),
                            new Point(size.width - 1, 0));
                    MatOfPoint2f dst = new MatOfPoint2f();

                    Log.i("h=", h.dump());
                    Core.perspectiveTransform(pts, dst, h);
                    Log.i("dst=", dst.dump());
                    MatOfPoint intDst = new MatOfPoint();
                    dst.convertTo(intDst, CvType.CV_32S);

                    //connect them with lines
                    Imgproc.polylines(frame, Arrays.asList(intDst), true, new Scalar(255, 0, 0, 255), 3, Imgproc.LINE_AA);
                }

                Mat proj = Utils.projectionMatrix(K, h);
                frame = Utils.render(frame, myMesh, proj, myImgRef, true, null);
                frame.convertTo(frame, CvType.CV_8UC3);

                //res.recycle();
            }

            ImageView view2 = MainActivity.this.findViewById(R.id.imageView2);
            Bitmap res = Bitmap.createBitmap(frame.width(), frame.height(), Bitmap.Config.ARGB_8888);
            org.opencv.android.Utils.matToBitmap(frame, res);

            view2.setImageBitmap(null);
            view2.setImageBitmap(res);
            view2.invalidate();
            //res.recycle();

            // When a bitmap is downloaded you do:
//            uiHandler.post(new Runnable() {
//                public void run() {
//                ImageView view = MainActivity.this.findViewById(R.id.imageView);
//                System.out.println(testref);
//                view.setImageBitmap(testref);
//                view.invalidate();
//                //    ref.recycle();
//            }
//            });


//            Canvas canvas = view.getHolder().lockCanvas();
//
//            if (canvas != null)
//            {
//                //Bitmap toDisplay = Bitmap.createBitmap(frame.width(), frame.height(), Bitmap.Config.ARGB_8888);
//                //org.opencv.android.Utils.matToBitmap(frame, bitmap);
//
//                if(bitmap != null)
//                {
//                    Paint paint = new Paint();
//
//                    //MemoryStream ms = new MemoryStream();
//                    //currentBitmap.Compress(Bitmap.CompressFormat.Jpeg, 100, ms);
//                    //byte[] bitmapData = ms.toArray();
//                    //Bitmap bitmap = BitmapFactory.DecodeByteArray(bitmapData, 0, bitmapData.Length);
//                    //Bitmap scaledBitmap = Bitmap.CreateScaledBitmap(bitmap, mPreview2.Width, mPreview2.Height, true);
//
//                    canvas.drawBitmap(ref, 0, 0, paint);
//                    ref.recycle();
//                    //scaledBitmap.Recycle();
//                    //currentBitmap.Recycle();
//                }
//                view.getHolder().unlockCanvasAndPost(canvas);
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
              image.close();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        //ImageView view = findViewById(R.id.imageView);
        //view.invalidate();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_new);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        InputStream stream = getResources().openRawResource(R.raw.fox2);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        myMesh = new ObjLoader(reader, false);

        //ImageView view2 = MainActivity.this.findViewById(R.id.imageView2);

        myImageWidth = 640;//1328;//5312;//view2.getMaxWidth();
        myImageHeight = 480;//747;//2988;//view2.getMaxHeight();
        //Log.i("here", view2.getMaxWidth() + " " + view2.getMaxWidth());

        uiHandler = new Handler();

        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;

        testref = BitmapFactory.decodeResource(getResources(), R.drawable.ref2corrigee, o);
        this.setupCamera();

        TextureView view = findViewById(R.id.textureView2);
        view.setSurfaceTextureListener(textureListener);


        //


        //

        //
        ImageView view2 = this.findViewById(R.id.imageView2);
        Log.i("here", ""+testref);
        view2.setImageBitmap(testref);
        view2.invalidate();

        //Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.frametest, o);







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
            TextureView view = findViewById(R.id.textureView2);
            SurfaceTexture texture = view.getSurfaceTexture();
            // We configure the size of default buffer to be the size of camera preview we want.
            //texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            assert texture != null;
            //Output surface for the preview
            myPreviewSurface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            final CaptureRequest.Builder previewRequestBuilder
                    = myCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //previewRequestBuilder.addTarget(myPreviewSurface);//surface);

			//StreamConfigurationMap map = characteristics.get(
            //            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            //    if (map == null) {
            //        continue;
            //    }
			//
            //    // For still image captures, we use the largest available size.
            //    Size largest = Collections.max(
            //            Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
            //            new CompareSizesByArea());

            myImageReader = ImageReader.newInstance(myImageWidth, myImageHeight, ImageFormat.JPEG, 2);//ImageFormat.YUV_420_888, 2);
            myImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);
            previewRequestBuilder.addTarget(myImageReader.getSurface());

            // Here, we create a CameraCaptureSession for camera preview.
            myCamera.createCaptureSession(Arrays.asList(/*myPreviewSurface,*/ myImageReader.getSurface()),
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

//https://stackoverflow.com/questions/12695232/using-native-functions-in-android-with-opencv/12699835#12699835
//https://stackoverflow.com/questions/53277911/how-to-import-3d-model-obj-into-android-studio

/*
JNIEXPORT jstring JNICALL Java_com_neza_myrobot_JNIUtils_detectLane(
            JNIEnv *env, jobject obj, jint srcWidth, jint srcHeight,
            jobject srcBuffer, jobject dstSurface, jstring path, jint saveFile) {
    char outStr[2000];

    const char *str = env->GetStringUTFChars(path, NULL);
    LOGE("bob path:%s saveFile=%d", str, saveFile);

    uint8_t *srcLumaPtr = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(srcBuffer));

    if (srcLumaPtr == nullptr) {
        LOGE("blit NULL pointer ERROR");
        return NULL;
    }

    int dstWidth;
    int dstHeight;

    cv::Mat mYuv(srcHeight + srcHeight / 2, srcWidth, CV_8UC1, srcLumaPtr);

    uint8_t *srcChromaUVInterleavedPtr = nullptr;
    bool swapDstUV;

    ANativeWindow *win = ANativeWindow_fromSurface(env, dstSurface);
    ANativeWindow_acquire(win);

    ANativeWindow_Buffer buf;

    dstWidth = srcHeight;
    dstHeight = srcWidth;

    ANativeWindow_setBuffersGeometry(win, dstWidth, dstHeight, 0 );

        if (int32_t err = ANativeWindow_lock(win, &buf, NULL)) {
        LOGE("ANativeWindow_lock failed with error code %d\n", err);
        ANativeWindow_release(win);
        return NULL;
        }

        uint8_t *dstLumaPtr = reinterpret_cast<uint8_t *>(buf.bits);
        Mat dstRgba(dstHeight, buf.stride, CV_8UC4,
        dstLumaPtr);        // TextureView buffer, use stride as width
        Mat srcRgba(srcHeight, srcWidth, CV_8UC4);
        Mat flipRgba(dstHeight, dstWidth, CV_8UC4);

        // convert YUV -> RGBA
        cv::cvtColor(mYuv, srcRgba, CV_YUV2RGBA_NV21);

        // Rotate 90 degree
        cv::transpose(srcRgba, flipRgba);
        cv::flip(flipRgba, flipRgba, 1);

        #if 0
        int ball_x;
        int ball_y;
        int ball_r;

        ball_r = 0;

        BallDetect(flipRgba, ball_x, ball_y, ball_r);
        if( ball_r > 0)
        LOGE("ball x:%d y:%d r:%d", ball_x, ball_y, ball_r);
        else
        LOGE("ball not detected");
        #endif

        LaneDetect(flipRgba, str, saveFile, outStr);

        // copy to TextureView surface
        uchar *dbuf;
        uchar *sbuf;
        dbuf = dstRgba.data;
        sbuf = flipRgba.data;
        int i;
        for (i = 0; i < flipRgba.rows; i++) {
        dbuf = dstRgba.data + i * buf.stride * 4;
        memcpy(dbuf, sbuf, flipRgba.cols * 4);
        sbuf += flipRgba.cols * 4;
        }

        // Draw some rectangles
        Point p1(100, 100);
        Point p2(300, 300);
        cv::line(dstRgba, Point(dstWidth/2, 0), Point(dstWidth/2, dstHeight-1),Scalar(255, 255, 255));
        cv::line(dstRgba, Point(0,dstHeight-1), Point(dstWidth-1, dstHeight-1),Scalar(255,255,255 ));

        LOGE("bob dstWidth=%d height=%d", dstWidth, dstHeight);
        ANativeWindow_unlockAndPost(win);
        ANativeWindow_release(win);

        return env->NewStringUTF(outStr);
        }
        }
 */