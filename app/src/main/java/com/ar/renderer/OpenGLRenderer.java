package com.ar.renderer;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.MotionEvent;

import com.arapp.R;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.rajawali3d.Object3D;
import org.rajawali3d.loader.LoaderOBJ;
import org.rajawali3d.loader.ParsingException;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Line3D;
import org.rajawali3d.renderer.Renderer;

import java.util.Arrays;
import java.util.Stack;

/**
 * OpenGL based renderer
 */
public class OpenGLRenderer extends Renderer implements ARRenderer{

    protected Object3D myMesh;
    protected int myFramerate = 10;

    protected Line3D myAxisX;
    protected Line3D myAxisY;
    protected Line3D myAxisZ;

    protected Line3D myLine1;
    protected Line3D myLine2;
    protected Line3D myLine3;

    protected boolean myShouldDrawBorder;
    protected boolean myShouldDrawFrame;
    protected boolean myShouldDrawModel;

    protected boolean myIsAskDrawing;

    protected boolean myIsInit;

    protected ModelType myModelType;

    private final String TAG = OpenGLRenderer.class.getSimpleName();

    public OpenGLRenderer(Context context) {
        super(context);
        setFrameRate(myFramerate);
        myShouldDrawBorder = true;
        myShouldDrawFrame = true;
        myShouldDrawModel = true;
        myIsInit = false;
    }

    public OpenGLRenderer(Context context, Matrix4 proj) {
        super(context);
        setFrameRate(myFramerate);
        myShouldDrawBorder = true;
        myShouldDrawFrame = true;
        myShouldDrawModel = true;
        myIsInit = false;
    }

    public OpenGLRenderer(Context context, double fov, int width, int height) {
        super(context);
        setFrameRate(myFramerate);
        setProjectionMatrix(fov, width, height);
        myShouldDrawBorder = true;
        myShouldDrawFrame = true;
        myShouldDrawModel = true;
        myIsInit = false;
    }

    public void setProjectionMatrix(double fov, int width, int height) {
        getCurrentCamera().setProjectionMatrix(fov, width, height);
        //getCurrentCamera().getProjectionMatrix().multiply(Matrix4.createScaleMatrix(-1, -1, -1));
        getCurrentCamera().setFarPlane(100000);
    }

    @Override
    protected void initScene() {
        setModel(ModelType.DONA);
        addFrame();

        myIsInit = true;
        askDrawing(false);
    }

    protected void addFrame() {
        double scale = 1000;
        Stack<Vector3> xPoints = new Stack<>();
        xPoints.push(new Vector3(0,0,0)); xPoints.push(new Vector3(scale,0,0));
        Stack<Vector3> yPoints = new Stack<>();
        yPoints.push(new Vector3(0,0,0)); yPoints.push(new Vector3(0,scale,0));
        Stack<Vector3> zPoints = new Stack<>();
        zPoints.push(new Vector3(0,0,0)); zPoints.push(new Vector3(0,0,scale));
        myAxisX = new Line3D(xPoints, 10, Color.argb(255, 255, 0, 0));
        myAxisY = new Line3D(yPoints, 10, Color.argb(255, 0, 255, 0));
        myAxisZ = new Line3D(zPoints, 10, Color.argb(255, 0, 0, 255));

        Material material = new Material();
        material.enableLighting(false);
        material.setDiffuseMethod(new DiffuseMethod.Lambert());
        //material.setColor(new float[]{255, 0, 0, 255});

        myAxisX.setMaterial(material);
        myAxisY.setMaterial(material);
        myAxisZ.setMaterial(material);
        getCurrentScene().addChild(myAxisX);
        getCurrentScene().addChild(myAxisY);
        getCurrentScene().addChild(myAxisZ);
    }

    void test() {
        getCurrentScene().removeChild(myLine1);
        getCurrentScene().removeChild(myLine2);
        getCurrentScene().removeChild(myLine3);
        double dist = 0.5;
        Vector3 pointLeftT = unProject(0, 0, dist);
        Vector3 pointLeftB = unProject(0, getViewportHeight(), dist);
        Vector3 pointRightT = unProject(getViewportWidth(), 0, dist);
        Vector3 pointRightB = unProject(getViewportWidth(), getViewportHeight(), dist);
        Stack<Vector3> points = new Stack<>();
        points.push(pointLeftT); points.push(pointLeftB);
        myLine1 = new Line3D(points, 10, Color.argb(255, 255, 255, 255));
        points = new Stack<>();
        points.push(pointLeftT); points.push(pointRightT);
        myLine2 = new Line3D(points, 10, Color.argb(255, 255, 255, 255));
        points = new Stack<>();
        points.push(pointLeftB); points.push(pointRightB);
        myLine3 = new Line3D(points, 10, Color.argb(255, 255, 255, 255));

        Material material = new Material();
        material.enableLighting(false);
        material.setDiffuseMethod(new DiffuseMethod.Lambert());

        myLine1.setMaterial(material);
        myLine2.setMaterial(material);
        myLine3.setMaterial(material);
        getCurrentScene().addChild(myLine1);
        getCurrentScene().addChild(myLine2);
        getCurrentScene().addChild(myLine3);
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {
    }

    public void setOrientationMatrix(Vector3 position, Matrix4 rotation)
    {
        getCurrentCamera().setPosition(position);
        getCurrentCamera().setRotation(rotation);
        //test();
    }

    @Override
    public void onTouchEvent(MotionEvent event) {
    }

    @Override
    public void setIntrinsicParamsMatrix(Mat K) {
        double fpixel = K.get(0,0)[0];
        double width = K.get(0, 2)[0] * 2;
        double height =  K.get(1, 2)[0] * 2;
        double zfar = 10000;
        double znear = 1;
        //nb : row major array !
        Matrix4 p = new Matrix4(new double[]
                {2.*fpixel / width,                   0,                                      0,                                 0,
                 0,                                   2. * fpixel / height,                   0,                                 0,
                 (width  - 2. * (width / 2)) / width, (-height  + 2. * (height / 2)) / height,(-zfar - znear) / (zfar - znear), -1,
                 0,                                   0,                                      -2*zfar*znear / (zfar - znear),    0});
        getCurrentCamera().setFarPlane(10000);
        getCurrentCamera().setProjectionMatrix(p);
        //Scale matrix to get the same axis as opencv
        getCurrentCamera().getProjectionMatrix().multiply(Matrix4.createScaleMatrix(1, -1, -1));

        //double fovy = Math.toDegrees(2 * Math.atan((frame.height() * pixelSizeMm) / (2 * focalLengthMm)));
        //myRenderer.setProjectionMatrix(fovy, frame.width(), frame.height());
        //myRenderer.setViewPort(frame.width(), frame.height());
        //myRenderer.getCurrentCamera().getProjectionMatrix().scale(1, -1, -1);
    }

    @Override
    public void setExtrinsicMatrix(Mat translation, Mat rotation) {//Mat M) {
        Vector3 t = new Vector3(translation.get(0, 0)[0], translation.get(1, 0)[0], translation.get(2, 0)[0]);
        System.out.println(t);
        double[] tmp = new double[16]; for(int i = 0; i < 16; i++) tmp[i] = 0;
        tmp[15] = 1.;
        Matrix4 r = new Matrix4();
        for(int i = 0; i < 3; i++)
            for(int j = 0; j < 3; j++)
                tmp[i + j * 4] = rotation.get(i, j)[0];

        r.setAll(tmp);//r.inverse();
        Log.i("INFOS translation", t.toString());
        Log.i("INFOS rotation", r.toString());
        Matrix4 res = new Matrix4();
        res.setAll(t, new Vector3(1,1,1), new Quaternion().fromMatrix(r));
        res.inverse();
        t = res.getTranslation();
        res.setTranslation(0,0,0);
        setOrientationMatrix(t, res);
    }

    @Override
    public Mat render(Mat frame, Mat homography, Mat proj, int imgRefWidth, int imgRefHeight) {
        if(!myIsAskDrawing)
            return frame;

        if(myShouldDrawBorder) {
            MatOfPoint2f pts = new MatOfPoint2f(new Point(0f, 0f),
                    new Point(0f, imgRefHeight - 1),
                    new Point(imgRefWidth - 1, imgRefHeight - 1),
                    new Point(imgRefWidth - 1, 0));
            MatOfPoint2f dst = new MatOfPoint2f();

            //Log.i("h=", h.dump());
            Core.perspectiveTransform(pts, dst, homography);
            //Log.i("dst=", dst.dump());
            MatOfPoint intDst = new MatOfPoint();
            dst.convertTo(intDst, CvType.CV_32S);

            //connect them with lines
            Imgproc.polylines(frame, Arrays.asList(intDst), true, new Scalar(255, 0, 0, 255), 3, Imgproc.LINE_AA);
        }

        return frame;
    }

    @Override
    public void shouldDrawModel(boolean draw) {
        myShouldDrawModel = draw;
    }

    @Override
    public void shouldDrawFrame(boolean draw) {
        myShouldDrawFrame = draw;

        if(myLine1 != null) {
            myLine1.setVisible(draw);
            myLine2.setVisible(draw);
            myLine3.setVisible(draw);
        }
    }

    @Override
    public void shouldDrawBorder(boolean draw) {
        myShouldDrawBorder = draw;
    }

    @Override
    public void askDrawing(boolean ask){
        if(!myIsInit)
            return;

        myIsAskDrawing = ask;

        boolean draw = myIsAskDrawing ? myShouldDrawFrame : false;
        myAxisX.setVisible(draw);
        myAxisY.setVisible(draw);
        myAxisZ.setVisible(draw);

        draw = myIsAskDrawing & myShouldDrawModel;
        myMesh.setVisible(draw);
    }

    @Override
    public boolean isDrawBorder() {
        return myShouldDrawBorder;
    }

    @Override
    public boolean isDrawFrame() {
        return myShouldDrawFrame;
    }

    @Override
    public boolean isDrawModel() {
        return myShouldDrawModel;
    }

    @Override
    public void setModel(ModelType type) {
        if(myMesh != null)
            getCurrentScene().removeChild(myMesh);

        Material material = new Material();
        material.setColor(new float[]{0, 0, 0, 0});

        LoaderOBJ objParser = null;

        try {
            if (type == ModelType.DONA) {
                Texture meshTexture = new Texture("modelTexture", R.drawable.annaleiva);
                material.addTexture(meshTexture);

                objParser = new LoaderOBJ(mContext.getResources(), mTextureManager, R.raw.donateodora);
                objParser.parse();
                myMesh = objParser.getParsedObject();
                myMesh.setScale(180, -180, -180);
                myMesh.setRotation(Vector3.Axis.X, -90);
            } else {
                Texture meshTexture = new Texture("modelTexture", R.drawable.texture);
                material.addTexture(meshTexture);

                objParser = new LoaderOBJ(mContext.getResources(), mTextureManager, R.raw.fox2);
                objParser.parse();
                myMesh = objParser.getParsedObject();
                myMesh.setScale(8, -8, -8);
            }
        } catch (ATexture.TextureException ex) {
            Log.e(TAG, "Error while loading mesh texture");
        } catch (ParsingException e) {
            e.printStackTrace();
            return;
        }

        myMesh.setMaterial(material);

        getCurrentScene().addChild(myMesh);

        myModelType = type;
    }

    @Override
    public void setModelScale(double x, double y, double z) {
        if(myMesh != null)
            myMesh.setScale(x, y, z);
    }

    @Override
    public void setModelPosition(double x, double y, double z) {
        if(myMesh != null)
            myMesh.setPosition(x, y, z);
    }

    @Override
    public void setModelRotation(double axisX, double axisY, double axisZ, double angleDegrees) {
        if(myMesh != null) {
            myMesh.setRotation(axisX, axisY, axisZ, angleDegrees);
        }
    }

    @Override
    public ModelType getCurrentModelType() {
        return myModelType;
    }
}
