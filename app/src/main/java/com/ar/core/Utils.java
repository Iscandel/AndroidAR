package com.ar.core;

import com.ar.loader3d.ObjLoader;

import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

import org.opencv.core.*;

import com.ar.loader3d.ObjLoader.Face;
import com.ar.loader3d.ObjLoader.Vector2;
import com.ar.loader3d.ObjLoader.Vector3;

public class Utils {
    /**
     * Projects a 3D model onto the image
     *
     * @param image
     * @param obj
     * @param projection
     * @param model
     * @param color
     * @param texture
     * @return
     */
    static public Mat render(Mat image, ObjLoader obj, Mat projection, Mat model, boolean color, Mat texture) {
        ArrayList<ObjLoader.Vector3<Double>> vertices = obj.vertices;
        ArrayList<ObjLoader.Vector2<Double>> texcoords = obj.texcoords;
        float scaleFactor = 8;//180;
        float[] scaleMatrix = {scaleFactor, scaleFactor, scaleFactor};

        //Mat scaleMatrix = new Mat();
        //Core.multiply(Mat.eye(new Size(3, 1), CvType.CV_32SC1), 8, scaleMatrix);
        scaleMatrix[1] = -scaleMatrix[1];
        scaleMatrix[2] = -scaleMatrix[2];
        Size refSize = model.size();

        for (ObjLoader.Face face : obj.faces) {
            ArrayList<Integer> faceVertices = face.indices;
            MatOfPoint3f points = new MatOfPoint3f();
            ArrayList<Point3> pointArray = new ArrayList<Point3>();
            for (int ind : faceVertices) {
                ObjLoader.Vector3<Double> oldP = vertices.get(ind - 1);
                Point3 p = new Point3();

                // Scale the model and then render it in the middle of the reference surface.
                // Otherwise, the model would be rendered in the corner of the reference surface
                p.x = oldP.x * scaleMatrix[0] + (int) refSize.width / 2; //to remove : int !!!!!!!!!!!!!!!!!!!
                p.y = oldP.y * scaleMatrix[1] + (int) refSize.height / 2;
                p.z = oldP.z * scaleMatrix[2];
                pointArray.add(p);
            }
            points.fromList(pointArray);

            MatOfPoint2f dest = new MatOfPoint2f();
            Core.perspectiveTransform(points, dest, projection);
            MatOfPoint imagePts = new MatOfPoint();
            dest.convertTo(imagePts, CvType.CV_32S);
            if (color)
                Imgproc.fillConvexPoly(image, imagePts, new Scalar(250, 60, 15, 255));
                //System.out.println(imagePts.toList());
            else {
                //Following, ie texture mapping, is mainly inspired from
                //https://www.learnopencv.com/warp-one-triangle-to-another-using-opencv-c-python
                Size texSize = texture.size();
                //get the uv indices
                ArrayList<Integer> uvIndices = face.texcoords;
                ArrayList<Point> uvArray = new ArrayList<>();
                //get the uv values
                MatOfPoint uvList = new MatOfPoint();
                //ArrayList<Point3> uvIndices = new ArrayList<Point3>();
                for (int ind : uvIndices) {
                    Vector2<Double> oldUV = texcoords.get(ind - 1);
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
                texture.submat(r1).copyTo(img1Cropped);
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

    /**
     * Computes the projection matrix
     *
     * @param cameraParameters Camera matrix -intrinsic parameters-
     * @param homography       Computed homography
     * @return The computed 3D projection matrix
     */
    static public Mat projectionMatrix(Mat cameraParameters, Mat homography) {
        //From the camera calibration matrix and the estimated homography
        //compute the 3D projection matrix

        //Recover the extrinsic matrix M: M = K-1 * H in the 2D case
        Mat M = new Mat();
        Core.gemm(cameraParameters.inv(), homography, 1, new Mat(), 0, M);

        //Normalize the first two columns
        Mat M0 = M.submat(Range.all(), new Range(0, 1));
        Mat M1 = M.col(1);
        double _lambda1 = Core.norm(M0);
        double _lambda2 = Core.norm(M1);
        double _lambda = (_lambda1 + _lambda2) / 2.;

        Core.multiply(M0, new Scalar(1. / _lambda1), M0);
        Core.multiply(M1, new Scalar(1. / _lambda2), M1);

        Mat translation = new Mat();
        //System.out.println(M.col(2).dump());

        //Normalize the last column (translation) by the average lambda
        //Mat t2 = new Mat();
        Core.multiply(M.col(2), new Scalar(1. / _lambda), translation);
        //System.out.println(_lambda);
        //System.out.println(translation.dump());
        //System.out.println(t2.dump());

        //Construct the rotation matrix
        Mat R = new Mat(3, 3, cameraParameters.type());
        M0.copyTo(R.col(0));
        M1.copyTo(R.col(1));
        M0.cross(M1).copyTo(R.col(2));

        if (Core.determinant(R) < 0)
            Core.multiply(R.col(2), new Scalar(-1.), R.col(2));
        //R[:, 2] = R[:, 2] * (-1)

        //Apply SVD to ensure orthogonality of the rotation matrix
        Mat W = new Mat();
        Mat U = new Mat();
        Mat Vt = new Mat();
        Core.SVDecomp(R, W, U, Vt);
        Core.gemm(U, Vt, 1, new Mat(), 0, R);
        //R = U.mul(Vt);

        //Construct the extrinsic matrix [R t]
        Mat extr = new Mat(3, 4, CvType.CV_64FC1);
        R.col(0).copyTo(extr.col(0));
        R.col(1).copyTo(extr.col(1));
        R.col(2).copyTo(extr.col(2));
        translation.copyTo(extr.col(3));

        //Compute the projection matrix H = K * extr
        Mat res1 = new Mat();
        Core.gemm(cameraParameters, extr, 1, new Mat(), 0, res1);
//	    Mat res2 = new Mat(4, 4, CvType.CV_64FC1);
//	    res1.row(0).copyTo(res2.row(0));
//	    res1.row(1).copyTo(res2.row(1));
//	    res1.row(2).copyTo(res2.row(2));

//	    System.out.println(cameraParameters.dump());
//	    System.out.println(extr.dump());
//	    System.out.println(res1.dump());
        return res1;
//	    return np.dot(camera_parameters, extr)    
    }

    public void drawFrame() {
        //	    #
//	#    point = np.array([[0,0,0], [1,0,0]])
//	#    point = np.dot(point, scale_matrix)
//	#    point = np.array([[p[0] + w / 2, p[1] + h / 2, p[2]] for p in point])
//	#    dst = cv2.perspectiveTransform(point.reshape(-1, 1, 3), projection)
//	#    imgpts = np.int32(dst)
//	#    img = cv2.polylines(img, [np.int32(dst)], False, (0,0, 255), 3, cv2.LINE_AA)
//	#
//	#    point = np.array([[0,0,0], [0,1,0]])
//	#    point = np.dot(point, scale_matrix)
//	#    point = np.array([[p[0] + w / 2, p[1] + h / 2, p[2]] for p in point])
//	#    dst = cv2.perspectiveTransform(point.reshape(-1, 1, 3), projection)
//	#    imgpts = np.int32(dst)
//	#    img = cv2.polylines(img, [np.int32(dst)], False, (0,255, 0), 3, cv2.LINE_AA)
//	#
//	#    point = np.array([[0,0,0], [0,0,1]])
//	#    point = np.dot(point, scale_matrix)
//	#    point = np.array([[p[0] + w / 2, p[1] + h / 2, p[2]] for p in point])
//	#    dst = cv2.perspectiveTransform(point.reshape(-1, 1, 3), projection)
//	#    imgpts = np.int32(dst)
//	#    img = cv2.polylines(img, [np.int32(dst)], False, (255,0, 0), 3, cv2.LINE_AA)
//	#
//	#    return img
    }

}
