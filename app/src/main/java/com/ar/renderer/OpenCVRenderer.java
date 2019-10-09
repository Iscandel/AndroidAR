package com.ar.renderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import com.ar.core.Utils;
import com.ar.loader3d.ObjLoader;
import com.arapp.R;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class OpenCVRenderer implements ARRenderer {
    protected boolean myShouldDrawBorder;
    protected boolean myShouldDrawFrame;
    protected boolean myShouldDrawModel;
    protected boolean myIsAskDrawing;
    protected Context myContext;
    protected ObjLoader myMesh;
    protected Mat myK;
    protected Mat myExtrinsic;

    protected int myModelColor;
    protected Point3 myModelScale;
    protected Point3 myModelPosition;
    protected Mat myTexture;
    protected ModelType myModelType;

    public OpenCVRenderer(Context context, int r, int g, int b) {
        this(context);

        setModelColor(Color.rgb(r, g, b));
    }

    public OpenCVRenderer(Context context, int textureId) {
        this(context);

        setTexture(textureId);
    }

    protected OpenCVRenderer(Context context) {
        myContext = context;

        myIsAskDrawing = false;
        myShouldDrawBorder = true;
        myShouldDrawFrame = true;
        myShouldDrawModel = true;

        myModelScale = new Point3();
        myModelPosition = new Point3();
        setModel(ModelType.FOX);
    }

    public void setModelColor(int modelColor) {
        if(myTexture != null)
            myTexture.release();

        myTexture = null;
        myModelColor = modelColor;
    }

    public void setTexture(int id) {
        myModelColor = -1;

        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        Bitmap ref = BitmapFactory.decodeResource(myContext.getResources(), id, o);

        myTexture = new Mat();
        org.opencv.android.Utils.bitmapToMat(ref, myTexture);
    }

    @Override
    public void setIntrinsicParamsMatrix(Mat K) {
        myK = K;
    }

    @Override
    public void setExtrinsicMatrix(Mat translation, Mat rotation) {

    }

    @Override
    public Mat render(Mat frame, Mat homography, Mat proj, int imgRefWidth, int imgRefHeight) {
        //If homography could not be found, return
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

        if(myShouldDrawFrame) {
            Utils.drawFrame(frame, proj, imgRefWidth / 2., imgRefHeight / 2., 1000.);
        }

        if(myShouldDrawModel) {
            frame = _render(frame, proj);
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
    }

    @Override
    public void shouldDrawBorder(boolean draw) {
        myShouldDrawBorder = draw;
    }

    @Override
    public void askDrawing(boolean ask){
        myIsAskDrawing = ask;
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
        if(type == ModelType.DONA) {
            InputStream stream = myContext.getResources().openRawResource(R.raw.donateodora);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            myMesh = new ObjLoader(reader, true);
            myModelScale = new Point3(180, -180, -180);
        } else {
            InputStream stream = myContext.getResources().openRawResource(R.raw.fox2);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            myMesh = new ObjLoader(reader, false);
            myModelScale = new Point3(8, -8, -8);
        }

        myModelType = type;
    }

    @Override
    public void setModelScale(double x, double y, double z) {
        myModelScale.x = x;
        myModelScale.y = y;
        myModelScale.z = z;
    }

    @Override
    public void setModelPosition(double x, double y, double z) {
        myModelPosition.x = x;
        myModelPosition.y = y;
        myModelPosition.z = z;
    }

    @Override
    public void setModelRotation(double axisX, double axisY, double axisZ, double angleDegrees) {

    }

    @Override
    public ModelType getCurrentModelType() {
        return myModelType;
    }

    /**
     * Projects a 3D model onto the image
     *
     * @param image
     * @param projection
     * @return
     */
    public Mat _render(Mat image, Mat projection) {
        ArrayList<ObjLoader.Vector3<Double>> vertices = myMesh.vertices;
        ArrayList<ObjLoader.Vector2<Double>> texcoords = myMesh.texcoords;

        for (ObjLoader.Face face : myMesh.faces) {
            ArrayList<Integer> faceVertices = face.indices;
            MatOfPoint3f points = new MatOfPoint3f();
            ArrayList<Point3> pointArray = new ArrayList<Point3>();
            for (int ind : faceVertices) {
                ObjLoader.Vector3<Double> oldP = vertices.get(ind - 1);
                Point3 p = new Point3();

                // Scale the model and then render it in the middle of the reference surface.
                // Otherwise, the model would be rendered in the corner of the reference surface
                p.x = oldP.x * myModelScale.x + (int) myModelPosition.x; //to remove : int !!!!!!!!!!!!!!!!!!!
                p.y = oldP.y * myModelScale.y + (int) myModelPosition.y;
                p.z = oldP.z * myModelScale.z;
                pointArray.add(p);
            }
            points.fromList(pointArray);

            MatOfPoint2f dest = new MatOfPoint2f();
            Core.perspectiveTransform(points, dest, projection);
            MatOfPoint imagePts = new MatOfPoint();
            dest.convertTo(imagePts, CvType.CV_32S);
            if (myTexture == null) {
                //int r = (int) (myModelColor.getComponent(0) * 255);
                Imgproc.fillConvexPoly(image, imagePts, new Scalar(Color.red(myModelColor), Color.green(myModelColor), Color.blue(myModelColor), 255));//new Scalar(250, 60, 15, 255));
                //System.out.println(imagePts.toList());
            } else {
                //Following, ie texture mapping, is mainly inspired from
                //https://www.learnopencv.com/warp-one-triangle-to-another-using-opencv-c-python
                Size texSize = myTexture.size();
                //get the uv indices
                ArrayList<Integer> uvIndices = face.texcoords;
                ArrayList<Point> uvArray = new ArrayList<>();
                //get the uv values
                MatOfPoint uvList = new MatOfPoint();
                //ArrayList<Point3> uvIndices = new ArrayList<Point3>();
                for (int ind : uvIndices) {
                    ObjLoader.Vector2<Double> oldUV = texcoords.get(ind - 1);
                    Point p = new Point();

                    // Scale the model, the render it in the middle of the reference surface. To do so,
                    // model points must be displaced
                    p.x = (int) (oldUV.x * texSize.width); //Cast int ????????????????????????????????????????????
                    p.y = (int) ((1.0 - oldUV.y) * texSize.height);

                    uvArray.add(p);
                }
                uvList.fromList(uvArray);

                //round...
                // Find bounding box.
                Rect r1 = Imgproc.boundingRect(uvList);
                Rect r2 = Imgproc.boundingRect(imagePts);

                if (r2.height >= image.cols() && r2.width >= image.rows())
                    continue;

                // Offset points by left top corner of the
                // respective rectangles

                ArrayList<Point> tri1CroppedArray = new ArrayList<Point>();
                ArrayList<Point> tri2CroppedArray = new ArrayList<>();

                for (int i = 0; i < 3; i++) {
                    tri1CroppedArray.add(new Point(uvArray.get(i).x - r1.x, uvArray.get(i).y - r1.y));
                    tri2CroppedArray.add(new Point(imagePts.toArray()[i].x - r2.x, imagePts.toArray()[i].y - r2.y));
                }
                MatOfPoint2f tri1Cropped = new MatOfPoint2f();
                tri1Cropped.fromList(tri1CroppedArray);
                MatOfPoint2f tri2Cropped = new MatOfPoint2f();
                tri2Cropped.fromList(tri2CroppedArray);

                // Apply warpImage to small rectangular patches
                Mat img1Cropped = new Mat();
                myTexture.submat(r1).copyTo(img1Cropped);
                // Given a pair of triangles, find the affine transform.
                Mat warpMat = Imgproc.getAffineTransform(tri1Cropped, tri2Cropped);

                // Apply the Affine Transform just found to the src image
                Mat img2Cropped = Mat.zeros(r2.height, r2.width, img1Cropped.type());
                Imgproc.warpAffine(img1Cropped, img2Cropped, warpMat, img2Cropped.size(), Imgproc.INTER_LINEAR, Core.BORDER_REFLECT_101);
                // Get mask by filling triangle
                Mat mask = Mat.zeros(r2.height, r2.width, CvType.CV_32FC3);

                // fillConvexPoly needs a vector of Point and not Point2f
                MatOfPoint tri2CroppedInt = new MatOfPoint();
                tri2Cropped.convertTo(tri2CroppedInt, CvType.CV_32S);
                Imgproc.fillConvexPoly(mask, tri2CroppedInt, new Scalar(1.0, 1.0, 1.0), 16, 0);

                // Apply mask to cropped region
                //System.out.println(img2Cropped.dump());
                //System.out.println(mask.dump());
                img2Cropped.convertTo(img2Cropped, CvType.CV_32FC3);
                Core.multiply(img2Cropped, mask, img2Cropped);
                //System.out.println(img2Cropped.dump());

                // Copy triangular region of the rectangular patch to the output image
                int[] minBound = {Math.max(0, r2.y), Math.max(0, r2.x)}; //line, col
                int[] maxBound = {Math.min(image.rows() - 1, r2.y + r2.height), Math.min(image.cols() - 1, r2.x + r2.width)};
                if (maxBound[0] > minBound[0] && maxBound[1] > minBound[1]) {
//	            	img2Cropped.convertTo(img2Cropped, CvType.CV_8UC3);
//	            	img2Cropped.copyTo(image.submat(minBound[0], maxBound[0], minBound[1], maxBound[1]));
                    Mat _1MinusMask = new Mat();
                    //-mask
                    Core.multiply(mask, new Scalar(-1., -1., -1.), mask);
                    Core.add(mask,
                            new Scalar(1.0, 1.0, 1.0),
                            _1MinusMask);
                    image.convertTo(image, CvType.CV_32FC3);
                    Core.multiply(image.submat(minBound[0], maxBound[0], minBound[1], maxBound[1]),
                            _1MinusMask.submat(minBound[0] - r2.y, maxBound[0] - r2.y, minBound[1] - r2.x, maxBound[1] - r2.x),
                            image.submat(minBound[0], maxBound[0], minBound[1], maxBound[1]));

                    Core.add(image.submat(minBound[0], maxBound[0], minBound[1], maxBound[1]),
                            img2Cropped.submat(minBound[0] - r2.y, maxBound[0] - r2.y, minBound[1] - r2.x, maxBound[1] - r2.x),
                            image.submat(minBound[0], maxBound[0], minBound[1], maxBound[1]));
                }
            }
        }
        return image;
    }

}
