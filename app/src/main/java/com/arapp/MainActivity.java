package com.arapp;

import android.Manifest;
import android.app.ActionBar;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
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
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.Menu;
import android.view.MenuItem;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraCaptureSession;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.ar.core.Matcher;
import com.ar.core.Utils;
import com.ar.loader3d.ObjLoader;
import com.ar.renderer.ARRenderer;
import com.ar.renderer.OpenCVRenderer;
import com.ar.renderer.OpenGLRenderer;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.rajawali3d.view.ISurface;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;


public class MainActivity extends AppCompatActivity implements
                            CameraBridgeViewBase.CvCameraViewListener2, DialogResultListener {
    private static final String TAG = "OCVSample::Activity";

    protected boolean myIsFabExpanded;

    protected ARRenderer myRenderer;

    public enum RendererType {
        OPENCV,
        OPENGL
    }

    final int CAMERA_PERMISSION_CODE = 1;

    Matcher.Algo myAlgo = Matcher.Algo.ORB;
    boolean myIsFirstFrame = true;

    boolean myShouldDrawBorder;
    boolean myShouldDrawFrame;
    boolean myShouldDrawModel;

    RendererType myRendererType;

    public final String RENDERING_SETTINGS_DLG = "RenderingSettingsDlg";
    public final String MODEL_SETTINGS_DLG = "ModelSettingsDlg";

    //Camera parameters
    final int imageRefSizeX = 5312;//5312 x 2988 #nb imgref was redim, we cannot use it
    int imageRefSizeY = 2988;
    double pixelSizeMm = 0.00112;
    double focalLengthMm = 4.3;
    double fpixel;// = focalLengthMm / pixelSizeMm
    Mat myK = null;
    Matcher matcher;
    Mat myImgRef;
    //

    private CameraBridgeViewBase mOpenCvCameraView;

//    static {
//        if (!OpenCVLoader.initDebug()) {
//            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
//            //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
//            System.loadLibrary("opencv_java4");
//        } else {
//            Log.d(TAG, "OpenCV library found inside package. Using it!");
//            //mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
//        }
//    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    //System.loadLibrary("mixed_sample");
                    matcher = new Matcher();
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.enableFpsMeter();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };



    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main_new);

        //myRendererType = RendererType.OPENCV;

        initializeRenderer(RendererType.OPENGL);

        myIsFabExpanded = false;

        myShouldDrawBorder = true;
        myShouldDrawFrame = true;
        myShouldDrawModel = true;

        FloatingActionButton fabSettings = this.findViewById(R.id.fabSettings);
        fabSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (myIsFabExpanded == true){
                    closeSubMenusFab();
                } else {
                    openSubMenusFab();
                }
            }
        });

        closeSubMenusFab();

        //Configure the rendering settings dialog
        FloatingActionButton fabRenderingSettings = this.findViewById(R.id.fabRenderingSettings);
        fabRenderingSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               RenderingSettingsDlg dlg = new RenderingSettingsDlg();
               Bundle bundle = new Bundle();
               bundle.putBoolean("drawBorder", myRenderer.isDrawBorder());
               bundle.putBoolean("drawFrame", myRenderer.isDrawFrame());
               bundle.putBoolean("drawModel", myRenderer.isDrawModel());
                bundle.putBoolean("openGL", myRendererType == RendererType.OPENGL);
               dlg.setArguments(bundle);
               dlg.show(getSupportFragmentManager(), RENDERING_SETTINGS_DLG);
            }
        });

        //Configure the model settings dialog
        FloatingActionButton fabModelSettings = this.findViewById(R.id.fabModelSettings);
        fabModelSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ModelSettingsDlg dlg = new ModelSettingsDlg();
                Bundle bundle = new Bundle();
                bundle.putInt("modelType", myRenderer.getCurrentModelType().ordinal());
                dlg.setArguments(bundle);
                dlg.show(getSupportFragmentManager(), MODEL_SETTINGS_DLG);
            }
        });

//        org.rajawali3d.view.SurfaceView rajawaliView = new org.rajawali3d.view.SurfaceView(this);
//        rajawaliView.setZOrderMediaOverlay(true);
//        rajawaliView.setTransparent(true);
//        myRenderer = new OpenGLRenderer(this);
//        //myRenderer.setViewPort(1920, 1080);
//        rajawaliView.setSurfaceRenderer(myRenderer);
//        rajawaliView.setRenderMode(ISurface.RENDERMODE_WHEN_DIRTY);
//
//        //myRenderer.setRenderSurface(rawajaliView);
//
//        FrameLayout frameLayout = findViewById(R.id.rootLayout);
//        //FrameLayout v = new FrameLayout(this);
//        frameLayout.addView(rajawaliView, 0);
//        myRenderer.setViewPort(1920, 1080);
//        ViewGroup.LayoutParams params = rajawaliView.getLayoutParams();
//        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
//        rajawaliView.setLayoutParams(params);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.imageView2);
        //mOpenCvCameraView = new JavaCameraView(this, -1);
        //v.addView(mOpenCvCameraView);
        //setContentView(v);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        askForCameraPermission();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        Log.i(TAG, "called onCreateOptionsMenu");
//        mItemPreviewRGBA = menu.add("Preview RGBA");
//        mItemPreviewGray = menu.add("Preview GRAY");
//        mItemPreviewCanny = menu.add("Canny");
//        mItemPreviewFeatures = menu.add("Find features");
        return true;
    }

    /**
     * Called when an open dialog returns, following a click on "OK" button
     * @param dialog Concerned dialog
     */
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        if(Objects.equals(dialog.getTag(), RENDERING_SETTINGS_DLG)) {
            RenderingSettingsDlg dlg = (RenderingSettingsDlg) dialog;
            myRenderer.shouldDrawBorder(dlg.isDrawBorder());
            myRenderer.shouldDrawFrame(dlg.isDrawFrame());
            myRenderer.shouldDrawModel(dlg.isDrawModel());
            if(dlg.isOpenGLRenderer() && myRendererType != RendererType.OPENGL ||
                    !dlg.isOpenGLRenderer() && myRendererType != RendererType.OPENCV ) {

                initializeRenderer(dlg.isOpenGLRenderer() ? RendererType.OPENGL : RendererType.OPENCV);

                FloatingActionButton fabRenderingSettings = this.findViewById(R.id.fabRenderingSettings);
                Snackbar.make(fabRenderingSettings,
                          "Switch to " + myRendererType.toString(), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
//            CheckBox check = dialog.getDialog().findViewById(R.id.checkDrawBorder);
//            CheckBox check = findViewById(R.id.checkDrawFrame);
//            CheckBox check = findViewById(R.id.checkDrawModel);
        } else if (Objects.equals(dialog.getTag(), MODEL_SETTINGS_DLG)) {
            ModelSettingsDlg dlg = (ModelSettingsDlg) dialog;
            myRenderer.setModel(dlg.getSelectedModel());
        }
    }

    /**
     * Called when an open dialog returns, following a click on "Cancel" button
     * @param dialog
     */
    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {

    }

    /**
     * Closes the sub menu of floating action buttons
     */
    private void closeSubMenusFab(){
        LinearLayout layoutFabRendering = this.findViewById(R.id.layoutRenderingSettings);
        LinearLayout layoutFabModel = this.findViewById(R.id.layoutFabModelSettings);

        layoutFabRendering.setVisibility(View.INVISIBLE);
        layoutFabModel.setVisibility(View.INVISIBLE);

        FloatingActionButton fabSettings = this.findViewById(R.id.fabSettings);
        fabSettings.setImageResource(R.drawable.ic_settings_black_24dp);
        myIsFabExpanded = false;
    }

    /**
     * Opens the sub menus of floating action buttons
     */
    private void openSubMenusFab(){
        LinearLayout layoutFabRendering = this.findViewById(R.id.layoutRenderingSettings);
        LinearLayout layoutFabModel = this.findViewById(R.id.layoutFabModelSettings);
        layoutFabRendering.setVisibility(View.VISIBLE);
        layoutFabModel.setVisibility(View.VISIBLE);

        //Change settings icon to 'X' icon
        FloatingActionButton fabSettings = this.findViewById(R.id.fabSettings);
        fabSettings.setImageResource(R.drawable.ic_close_black_24dp);
        myIsFabExpanded = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        matcher = new Matcher();
        //mOpenCvCameraView.enableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    /**
     * Called by OpenCV on each camera frame
     * @param inputFrame Given frame
     * @return Processed frame
     */
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat frame = inputFrame.rgba();

        if (myIsFirstFrame) {
            //InputStream stream = getResources().openRawResource(R.raw.ref2corrigee);
//            myRenderer.setViewPort(frame.width(), frame.height());
//            final FrameLayout frameLayout = (FrameLayout) findViewById(R.id.rootLayout);
//            new Handler().post(new Runnable() {public void run() {frameLayout.invalidate();}});
//            frameLayout.getChildAt(0);
//            //FrameLayout v = new FrameLayout(this);
//            ViewGroup.LayoutParams params = rajawaliView.getLayoutParams();
//            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
//            rajawaliView.setLayoutParams(params);

            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inScaled = false;
            Bitmap ref = BitmapFactory.decodeResource(getResources(), R.drawable.ref2corrigee, o);

            myImgRef = new Mat();
            org.opencv.android.Utils.bitmapToMat(ref, myImgRef);
            //org.opencv.imgproc.Imgproc.cvtColor(myImgRef, myImgRef, Imgproc.COLOR_BGR2GRAY);
            matcher.computeReferenceImage(myImgRef, myAlgo);
            myIsFirstFrame = false;


            //For video, resize pixel size accordingly
            pixelSizeMm = pixelSizeMm * (imageRefSizeY / (double) frame.height());
            fpixel = focalLengthMm / pixelSizeMm;
            myK = Utils.computeIntrinsicParamsMatrix(pixelSizeMm, focalLengthMm,
                                frame.width() / 2., frame.height() / 2.);

            myRenderer.setIntrinsicParamsMatrix(myK);
            //myRenderer.setModelPosition(myImgRef.width() / 2., -myImgRef.height() / 2.); //with opengl axis
            myRenderer.setModelPosition(myImgRef.width() / 2., myImgRef.height() / 2., 0); //with opencv axis
        }

        Mat h = matcher.computeHomography(frame, myAlgo);
        //h.convertTo(h, CvType.CV_32F);
        if (h != null && !h.empty()) {
            myRenderer.askDrawing(true);

            Mat translation = new Mat();
            Mat rotation = new Mat(3, 3, myK.type());
            Mat proj = Utils.projectionMatrix(myK, h, translation, rotation);

            myRenderer.setModelPosition(myImgRef.width() / 2., myImgRef.height() / 2., 0); //with opencv axis
            myRenderer.setExtrinsicMatrix(translation, rotation);
            myRenderer.render(frame, h, proj, myImgRef.width(), myImgRef.height());

            translation.release();
            rotation.release();
        } else {
            myRenderer.askDrawing(false);
        }

        return frame;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

//        if (item == mItemPreviewRGBA) {
//        mViewMode = VIEW_MODE_RGBA;
//        } else if (item == mItemPreviewGray) {
//        mViewMode = VIEW_MODE_GRAY;
//        } else if (item == mItemPreviewCanny) {
//        mViewMode = VIEW_MODE_CANNY;
//        } else if (item == mItemPreviewFeatures) {
//        mViewMode = VIEW_MODE_FEATURES;
//        }

        return true;
    }

    /**
     * Prompts the user for camera permission if not already allowed
     */
    public void askForCameraPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
    }

    /**
     * Initializes the given renderer
     * @param type renderer to instanciate
     */
    // TODO: 09/10/2019 Should use app state to initalize renderers
    public void initializeRenderer(RendererType type) {

        //Clean
        FrameLayout frameLayout = findViewById(R.id.rootLayout);
        View view = frameLayout.getChildAt(0);
        //Ugly
        if(view instanceof org.rajawali3d.view.SurfaceView) {
            frameLayout.removeView(view);
            org.rajawali3d.view.SurfaceView rview = (org.rajawali3d.view.SurfaceView) view;
        }

        if(myRendererType == RendererType.OPENGL) {
            ((OpenGLRenderer) myRenderer).removeScene(((OpenGLRenderer) myRenderer).getCurrentScene());
            ((OpenGLRenderer) myRenderer).stopRendering();
        }

        if(type == RendererType.OPENCV) {
            myRenderer = new OpenCVRenderer(this, 250,60,10);
            if(myK != null) {
                myRenderer.setIntrinsicParamsMatrix(myK);
                myRenderer.setModelPosition(myImgRef.width() / 2., myImgRef.height() / 2., 0); //with opencv axis
            }
        } else {
            org.rajawali3d.view.SurfaceView rajawaliView = new org.rajawali3d.view.SurfaceView(this);
            rajawaliView.setZOrderMediaOverlay(true);
            rajawaliView.setTransparent(true);
            OpenGLRenderer renderer = new OpenGLRenderer(this);
            myRenderer = renderer;
            //myRenderer.setViewPort(1920, 1080);
            rajawaliView.setSurfaceRenderer(renderer);
            rajawaliView.setRenderMode(ISurface.RENDERMODE_WHEN_DIRTY);

            renderer.setViewPort(1920, 1080);
//            myRenderer.setRenderSurface(rawajaliView);
            if(myK != null) {
                myRenderer.setIntrinsicParamsMatrix(myK);
                myRenderer.setModelPosition(myImgRef.width() / 2., myImgRef.height() / 2., 0); //with opencv axis
            }

            //FrameLayout v = new FrameLayout(this);
            frameLayout.addView(rajawaliView, 0);

            ViewGroup.LayoutParams params = rajawaliView.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            rajawaliView.setLayoutParams(params);
        }

        myRendererType = type;
    }

}





















//public class MainActivity extends AppCompatActivity { //implements Camera.PreviewCallback {
//
//    //Manager for all available cameras of the device
//    private CameraManager cameraManager;
//    //Camera device
//    private CameraDevice myCamera;
//    //Capture session reference
//    private CameraCaptureSession myCaptureSession;
//    //private SurfaceView mySurface;
//    private ImageReader myImageReader;
//    private int myImageWidth;
//    private int myImageHeight;
//    //Id of the used camera
//    String myCameraId;
//    //Surface used for the preview process
//    Surface myPreviewSurface;
//    private ObjLoader myMesh;
//
//    final int CAMERA_PERMISSION_CODE = 1;
//
//    //
//    int MIN_MATCHES = 10;
//
//    Matcher.Algo algo = Matcher.Algo.ORB;
//    boolean firstFrame = true;
//    boolean drawMatches = false;
//    boolean drawBorder = true;
//
//    //Camera parameters
//    int imageRefSizeX = 5312;//5312 x 2988 #nb imgref was redim, we cannot use it
//    int imageRefSizeY = 2988;
//    double pixelSizeMm = 0.00112;
//    double focalLengthMm = 4.3;
//    double fpixel;// = focalLengthMm / pixelSizeMm
//    Mat K = null;
//    Matcher matcher = new Matcher();
//    Mat myImgRef;
//    //
//
//
//    Bitmap testref;
//    Handler uiHandler;
//    static {
//        if (!OpenCVLoader.initDebug()) {
//            // Handle initialization error
//            Log.e(MainActivity.class.getName(), "OpenCV could not be initialized");
//        }
//        //System.loadLibrary("opencv_java3");
//    }
//
//    private final CameraDevice.StateCallback myStateCallback = new CameraDevice.StateCallback() {
//        @Override
//        public void onOpened(@NonNull CameraDevice cameraDevice) {
//            // This method is called when the camera is opened.  We start camera preview here.
//            //mCameraOpenCloseLock.release();
//            myCamera = cameraDevice;
//            createCameraPreviewSession();
//        }
//
//        @Override
//        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
//            //mCameraOpenCloseLock.release();
//            cameraDevice.close();
//            myCamera = null;
//        }
//
//        @Override
//        public void onError(@NonNull CameraDevice cameraDevice, int error) {
//            //mCameraOpenCloseLock.release();
//            cameraDevice.close();
//            myCamera = null;
//            MainActivity.this.finish();
//            //Activity activity = getActivity();
//            //if (null != activity) {
//            //    this.finish();
//            //}
//        }
//
//    };
//
//    //Preview texture listener
//    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
//        @Override
//        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//            askForPermissionAndOpenCamera();
//        }
//        @Override
//        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
//            // Transform you image captured size according to the surface width and height
//        }
//        @Override
//        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//            return false;
//        }
//        @Override
//        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//        }
//    };
//
//    //processing-purpose texture listener
//    protected ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
//
//        @Override
//        public void onImageAvailable(ImageReader reader) {
//            // get the newest frame
//            Image image = reader.acquireLatestImage();
//
//            if (image == null) {
//                return;
//            }
//
//            //for jpeg image
//            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
//            byte[] bytes = new byte[buffer.remaining()];
//            buffer.get(bytes);
//            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length,null);
//
//
//
//            Mat frame = new Mat();//
//            org.opencv.android.Utils.bitmapToMat(bitmap, frame);
//            //org.opencv.imgproc.Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
//
//            //yuv ?
////           Mat buf = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC1);
////           org.opencv.android.Utils.
////           ByteBuffer buffer = image.getPlanes()[0].getBuffer();
////           byte[] bytes = new byte[buffer.remaining()];
////           buffer.get(bytes);
////           buf.put(0, 0, bytes);
////
////            Mat mat = Imgcodecs.imdecode(buf, Imgcodecs.IMREAD_COLOR);
//            BitmapFactory.Options o = new BitmapFactory.Options();
//            o.inScaled = false;
//
//            //Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.frametest, o);
//
//
//            //org.opencv.android.Utils.bitmapToMat(b, frame);
//            //b.recycle();
//
//            if (firstFrame) {
//                //InputStream stream = getResources().openRawResource(R.raw.ref2corrigee);
//
//                Bitmap ref = BitmapFactory.decodeResource(getResources(), R.drawable.ref2corrigee, o);
//
//                myImgRef = new Mat();
//                org.opencv.android.Utils.bitmapToMat(testref, myImgRef);
//                //org.opencv.imgproc.Imgproc.cvtColor(myImgRef, myImgRef, Imgproc.COLOR_BGR2GRAY);
//                matcher.computeReferenceImage(myImgRef, algo);
//                firstFrame = false;
//
//                //For video, resize pixel size accordingly
//                pixelSizeMm = pixelSizeMm * (imageRefSizeX / (double) frame.width());
//                fpixel = focalLengthMm / pixelSizeMm;
//                K = new Mat(3, 3, CvType.CV_64FC1);
//                K.put(0, 0, fpixel);
//                K.put(0, 1, 0.);
//                K.put(0, 2, frame.width() / 2.);
//                K.put(1, 0, 0);
//                K.put(1, 1, fpixel);
//                K.put(1, 2, frame.height() / 2.);
//                K.put(2, 0, 0);
//                K.put(2, 1, 0);
//                K.put(2, 2, 1);
//            }
//
//            Mat h = matcher.computeHomography(frame, algo);
//            //h.convertTo(h, CvType.CV_32F);
//            if (h != null && !h.empty()) {
//                if (drawBorder) {
//                    Size size = myImgRef.size();
//                    MatOfPoint2f pts = new MatOfPoint2f(new Point(0f, 0f),
//                            new Point(0f, size.height - 1),
//                            new Point(size.width - 1, size.height - 1),
//                            new Point(size.width - 1, 0));
//                    MatOfPoint2f dst = new MatOfPoint2f();
//
//                    //Log.i("h=", h.dump());
//                    Core.perspectiveTransform(pts, dst, h);
//                    //Log.i("dst=", dst.dump());
//                    MatOfPoint intDst = new MatOfPoint();
//                    dst.convertTo(intDst, CvType.CV_32S);
//
//                    //connect them with lines
//                    Imgproc.polylines(frame, Arrays.asList(intDst), true, new Scalar(255, 0, 0, 255), 3, Imgproc.LINE_AA);
//                }
//
//                Mat proj = Utils.projectionMatrix(K, h);
//                frame = Utils.render(frame, myMesh, proj, myImgRef, true, null);
//                frame.convertTo(frame, CvType.CV_8UC3);
//
//                //res.recycle();
//            }
//
//            //ImageView view2 = MainActivity.this.findViewById(R.id.imageView2);
//            Bitmap res = Bitmap.createBitmap(frame.width(), frame.height(), Bitmap.Config.ARGB_8888);
//            org.opencv.android.Utils.matToBitmap(frame, res);
//
//            SurfaceView view2 = MainActivity.this.findViewById(R.id.imageView2);
//            Canvas canvas = view2.getHolder().lockCanvas();
//            if (canvas != null) {
//                canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
//
//
//                canvas.drawBitmap(res, new Rect(0,0,res.getWidth(), res.getHeight()),
//                        new Rect(0,
//                                0,
//                                canvas.getWidth(),
//                                canvas.getHeight()), null);
//            }
//
//            view2.getHolder().unlockCanvasAndPost(canvas);
//            frame.release();
//            image.close();
//
////            view2.setImageBitmap(null);
////            view2.setImageBitmap(res);
////            view2.invalidate();
//            //res.recycle();
//
//            // When a bitmap is downloaded you do:
////            uiHandler.post(new Runnable() {
////                public void run() {
////                ImageView view = MainActivity.this.findViewById(R.id.imageView);
////                System.out.println(testref);
////                view.setImageBitmap(testref);
////                view.invalidate();
////                //    ref.recycle();
////            }
////            });
//
//
////            Canvas canvas = view.getHolder().lockCanvas();
////
////            if (canvas != null)
////            {
////                //Bitmap toDisplay = Bitmap.createBitmap(frame.width(), frame.height(), Bitmap.Config.ARGB_8888);
////                //org.opencv.android.Utils.matToBitmap(frame, bitmap);
////
////                if(bitmap != null)
////                {
////                    Paint paint = new Paint();
////
////                    //MemoryStream ms = new MemoryStream();
////                    //currentBitmap.Compress(Bitmap.CompressFormat.Jpeg, 100, ms);
////                    //byte[] bitmapData = ms.toArray();
////                    //Bitmap bitmap = BitmapFactory.DecodeByteArray(bitmapData, 0, bitmapData.Length);
////                    //Bitmap scaledBitmap = Bitmap.CreateScaledBitmap(bitmap, mPreview2.Width, mPreview2.Height, true);
////
////                    canvas.drawBitmap(ref, 0, 0, paint);
////                    ref.recycle();
////                    //scaledBitmap.Recycle();
////                    //currentBitmap.Recycle();
////                }
////                view.getHolder().unlockCanvasAndPost(canvas);
////            }
//
////
////            // print image format
////            int format = reader.getImageFormat();
////            Log.d(TAG, "the format of captured frame: " + format);
////
////            // HERE to call jni methods
////            JNIUtils.display(image.getWidth(), image.getHeight(), image.getPlanes()[0].getBuffer(), surface);
////
////
////            //ByteBuffer buffer = image.getPlanes()[0].getBuffer();
////            //byte[] bytes = new byte[buffer.remaining()];
////
////
////              image.close();
//        }
//    };
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        //ImageView view = findViewById(R.id.imageView);
//        //view.invalidate();
//    }
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main_new);
//        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        //setSupportActionBar(toolbar);
//
////        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
////        fab.setOnClickListener(new View.OnClickListener() {
////            @Override
////            public void onClick(View view) {
////                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
////                        .setAction("Action", null).show();
////            }
////        });
//
//        InputStream stream = getResources().openRawResource(R.raw.fox2);
//        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
//        myMesh = new ObjLoader(reader, false);
//
//        //ImageView view2 = MainActivity.this.findViewById(R.id.imageView2);
//
//        myImageWidth = 1280;//640;//1328;//5312;//view2.getMaxWidth();
//        myImageHeight = 720;//480;//747;//2988;//view2.getMaxHeight();
//        //Log.i("here", view2.getMaxWidth() + " " + view2.getMaxWidth());
//
//        uiHandler = new Handler();
//
//        BitmapFactory.Options o = new BitmapFactory.Options();
//        o.inScaled = false;
//
//        testref = BitmapFactory.decodeResource(getResources(), R.drawable.ref2corrigee, o);
//        this.setupCamera();
//
//        //TextureView view = findViewById(R.id.textureView2);
//        //view.setSurfaceTextureListener(textureListener);
//        askForPermissionAndOpenCamera();
//
//        //
//
//
//        //
//
//        //
////        ImageView view2 = this.findViewById(R.id.imageView2);
////        Log.i("here", ""+testref);
////        view2.setImageBitmap(testref);
////        view2.invalidate();
//
//        //Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.frametest, o);
//
//
//
//
//
//
//
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
//
////    @Override
////    public void onPreviewFrame(byte[] data, Camera camera) {
////
////    }
//
//    public void askForPermissionAndOpenCamera() {
//        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
//            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
//        else
//            openCamera();
//    }
//
//    //=========================================================================
//    ///////////////////////////////////////////////////////////////////////////
//    private void createCameraPreviewSession() {
//        try {
//            //SurfaceView view = this.findViewById(R.id.surfaceView3);
//            //myPreviewSurface = view.getHolder().getSurface();
//
//            //TextureView view = findViewById(R.id.textureView2);
//            //SurfaceTexture texture = view.getSurfaceTexture();
//            // We configure the size of default buffer to be the size of camera preview we want.
//            //texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
//            //assert texture != null;
//            //Output surface for the preview
//            //myPreviewSurface = new Surface(texture);
//
//            // We set up a CaptureRequest.Builder with the output Surface.
//            final CaptureRequest.Builder previewRequestBuilder
//                    = myCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
//            //previewRequestBuilder.addTarget(myPreviewSurface);//surface);
//
//            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
//            CameraCharacteristics characteristics
//                    = manager.getCameraCharacteristics(myCameraId);
//
//            // We don't use a front facing camera in this sample.
//            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
//            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
//                return;
//            }
//
//            StreamConfigurationMap map = characteristics.get(
//                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//            if (map == null) {
//                return;
//            }
//
//            // For still image captures, we use the largest available size.
//            android.util.Size largest = Collections.max(
//                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
//                    new CompareSizesByArea());
//
//            //myImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, 1);//myImageWidth, myImageHeight, ImageFormat.JPEG, 1);//ImageFormat.YUV_420_888, 2);
//            myImageReader = ImageReader.newInstance(myImageWidth, myImageHeight, ImageFormat.JPEG, 2);
//            myImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);
//            previewRequestBuilder.addTarget(myImageReader.getSurface());
//
//            // Here, we create a CameraCaptureSession for camera preview.
//            myCamera.createCaptureSession(Arrays.asList(/*myPreviewSurface,*/ myImageReader.getSurface()),
//                    new CameraCaptureSession.StateCallback() {
//
//                        @Override
//                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
//                            // The camera is already closed
//                            if (myCamera == null) {
//                                return;
//                            }
//
//                            // When the session is ready, we start displaying the preview.
//                            myCaptureSession = cameraCaptureSession;
//                            try {
//                                // Auto focus should be continuous for camera preview.
//                                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
//                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//                                // Flash is automatically enabled when necessary.
//                                //setAutoFlash(mPreviewRequestBuilder);
//
//                                // Finally, we start displaying the camera preview.
//                                CaptureRequest previewRequest = previewRequestBuilder.build();
//                                myCaptureSession.setRepeatingRequest(previewRequest, null, null);
//                                //myCaptureCallback, myBackgroundHandler);
//                            } catch (CameraAccessException e) {
//                                e.printStackTrace();
//                            }
//                        }
//
//                        @Override
//                        public void onConfigureFailed(
//                                @NonNull CameraCaptureSession cameraCaptureSession) {
//                            Toast.makeText(getApplicationContext(), "Failed to configure the camera", Toast.LENGTH_SHORT).show();
//                        }
//                    }, null
//            );
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
//
//    //=========================================================================
//    ///////////////////////////////////////////////////////////////////////////
//    private void setupCamera() {
//        try {
//            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
//
//            for (String cameraId : cameraManager.getCameraIdList()) {
//                CameraCharacteristics cameraCharacteristics =
//                        cameraManager.getCameraCharacteristics(cameraId);
//                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
//                        CameraCharacteristics.LENS_FACING_BACK) {
//                    StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(
//                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//                    //previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
//                    this.myCameraId = cameraId;
//                }
//            }
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
//
//    //=========================================================================
//    ///////////////////////////////////////////////////////////////////////////
//    private void openCamera() {
//        try {
//            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
//                    == PackageManager.PERMISSION_GRANTED) {
//                cameraManager.openCamera(myCameraId, myStateCallback, null);// backgroundHandler);
//            }
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
//
//    //=========================================================================
//    ///////////////////////////////////////////////////////////////////////////
//    @Override
//    public void onRequestPermissionsResult(int requestCode,
//                                           String permissions[], int[] results) {
//        switch (requestCode) {
//            case CAMERA_PERMISSION_CODE:
//                if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
//                    // permission granted
//                    openCamera();
//                } else {
//                    // permission denied
//                    Toast.makeText(getApplicationContext(), "Camera permission must be enabled", Toast.LENGTH_SHORT).show();
//                    finish();
//                }
//        }
//    }
//
//    static class CompareSizesByArea implements Comparator<android.util.Size> {
//
//        @Override
//        public int compare(android.util.Size lhs, android.util.Size rhs) {
//            // We cast here to ensure the multiplications won't overflow
//            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
//                    (long) rhs.getWidth() * rhs.getHeight());
//        }
//
//    }
//}

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