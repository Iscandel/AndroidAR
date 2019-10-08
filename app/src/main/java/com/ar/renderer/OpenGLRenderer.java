package com.ar.renderer;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.MotionEvent;

import com.arapp.R;

import org.rajawali3d.Object3D;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.loader.LoaderOBJ;
import org.rajawali3d.loader.ParsingException;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.materials.textures.TextureManager;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Line3D;
import org.rajawali3d.primitives.Sphere;
import org.rajawali3d.renderer.Renderer;

import java.util.Stack;

public class OpenGLRenderer extends Renderer {
    protected Matrix4 myProjMatrix;
    protected Object3D myMesh;
    int myFramerate = 10;

    Line3D myAxisX;
    Line3D myAxisY;
    Line3D myAxisZ;

    Line3D myLine1;
    Line3D myLine2;
    Line3D myLine3;

    private final String TAG = OpenGLRenderer.class.getSimpleName();

    //
    private DirectionalLight directionalLight;
    private Sphere earthSphere;

    public OpenGLRenderer(Context context) {
        super(context);
        setFrameRate(myFramerate);
    }

    public OpenGLRenderer(Context context, Matrix4 proj) {
        super(context);
        setFrameRate(myFramerate);
        //myProjMatrix = proj;
        //getCurrentCamera().setProjectionMatrix(myProjMatrix);
    }

    public OpenGLRenderer(Context context, double fov, int width, int height) {
        super(context);
        setFrameRate(myFramerate);
        setProjectionMatrix(fov, width, height);
    }

    public void setProjectionMatrix(double fov, int width, int height) {
        getCurrentCamera().setProjectionMatrix(fov, width, height);
        //getCurrentCamera().getProjectionMatrix().multiply(Matrix4.createScaleMatrix(-1, -1, -1));
        getCurrentCamera().setFarPlane(100000);
    }

    public void shouldDraw(boolean draw) {
        myMesh.setVisible(draw);
    }

    @Override
    protected void initScene() {
//        directionalLight = new DirectionalLight(1f, .2f, -1.0f);
//        directionalLight.setColor(1.0f, 1.0f, 1.0f);
//        directionalLight.setPower(2);
        //getCurrentScene().addLight(directionalLight);

        Material material = new Material();
       // material.enableLighting(false);
        //material.setDiffuseMethod(new DiffuseMethod.Lambert());
        material.setColor(new float[]{0, 0, 0, 0});
int a = getViewportWidth();

        Texture meshTexture = new Texture("modelTexture", R.drawable.annaleiva);
        try {
            material.addTexture(meshTexture);
        } catch(ATexture.TextureException ex) {
            Log.e(TAG, "Error while loading mesh texture");
        }
//        earthSphere = new Sphere(1, 24, 24);
//        //earthSphere.setColor(new Vector3(255, 0, 0));
//        earthSphere.setMaterial(material);
//        getCurrentScene().addChild(earthSphere);
//        getCurrentCamera().setZ(4.2f);

        //getCurrentCamera().setProjectionMatrix(myProjMatrix);
        LoaderOBJ objParser = new LoaderOBJ(mContext.getResources(),
                mTextureManager, R.raw.donateodora);

        try {
            objParser.parse();
        } catch (ParsingException e) {

            e.printStackTrace();
        }

        myMesh = objParser.getParsedObject();
        //myMesh.setColor(new Vector3(255, 0, 0));
        //myMesh.setPosition(0, 0, 0);
        myMesh.setMaterial(material);
        myMesh.setScale(180, -180, -180);
        myMesh.setRotation(Vector3.Axis.X, -90);
        //getCurrentCamera().setPosition(0, 0, 100);

        getCurrentScene().addChild(myMesh);

        addFrame();
    }

    public void drawFrame(boolean draw) {
        myAxisX.setVisible(draw);
        myAxisY.setVisible(draw);
        myAxisZ.setVisible(draw);

        if(myLine1 != null) {
            myLine1.setVisible(draw);
            myLine2.setVisible(draw);
            myLine3.setVisible(draw);
        }
    }

    public void addFrame() {
        double scale = 1000;
        Stack<Vector3> xPoints = new Stack();
        xPoints.push(new Vector3(0,0,0)); xPoints.push(new Vector3(scale,0,0));
        Stack<Vector3> yPoints = new Stack();
        yPoints.push(new Vector3(0,0,0)); yPoints.push(new Vector3(0,scale,0));
        Stack<Vector3> zPoints = new Stack();
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
        Stack<Vector3> points = new Stack();
        points.push(pointLeftT); points.push(pointLeftB);
        myLine1 = new Line3D(points, 10, Color.argb(255, 255, 255, 255));
        points = new Stack();
        points.push(pointLeftT); points.push(pointRightT);
        myLine2 = new Line3D(points, 10, Color.argb(255, 255, 255, 255));
        points = new Stack();
        points.push(pointLeftB); points.push(pointRightB);
        myLine3 = new Line3D(points, 10, Color.argb(255, 255, 255, 255));

        Material material = new Material();
        material.enableLighting(false);
        material.setDiffuseMethod(new DiffuseMethod.Lambert());
        //material.setColor(new float[]{255, 0, 0, 255});

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
        //myMesh.setPosition(-position.x, -position.y, -position.y);
        //System.out.println("ET DONC " + getCurrentCamera().getNearPlane() + " " + getCurrentCamera().getFarPlane());
        getCurrentCamera().setPosition(position);
        getCurrentCamera().setRotation(rotation);
        test();
    }

    @Override
    public void onTouchEvent(MotionEvent event) {

    }

    public void translateMesh(double x, double y) {
        myMesh.setPosition(x, y, 0);
    }
}
