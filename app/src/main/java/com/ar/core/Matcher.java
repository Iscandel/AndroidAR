package com.ar.core;

import android.util.Log;

import com.ar.loader3d.ObjLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.BFMatcher;
//import org.opencv.highgui.HighGui;
import org.opencv.features2d.FlannBasedMatcher;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import com.ar.loader3d.ObjLoader;

public class Matcher {
    public enum Algo {ORB, SIFT};

    public enum MatchingType {BF_MATCHING, KNN_MATCHING, FLANN_MATCHING};

    public final float LOWE_RATIO = 0.8f;
    public final int MIN_MATCHES = 10;

    protected int myORBnMatch = 100;
    protected MatOfKeyPoint myKpRef;
    protected Mat myDescRef;

    protected MatchingType myMatchingType = MatchingType.BF_MATCHING;

    public Matcher() {
        myKpRef = new MatOfKeyPoint();
        myDescRef = new Mat();
    }

    /**
     * Detects interest points on a given image
     *
     * @param image Image to detect the features on
     * @param algo  Keypoint algorithm to use
     * @param kp    Matrix of keypoints, with their location on the image
     * @param desc  Associated descriptors (to the keypoints)
     */
    public void detector(Mat image, Algo algo, MatOfKeyPoint kp, Mat desc) {
        if (algo == Algo.ORB)
            orbDetector(image, kp, desc);
        else
            siftDetector(image, kp, desc);
    }

    /**
     * Sift detector -currently not implemented as it is not always available depending
     * on the version of opencv-
     *
     * @param image
     * @param kp
     * @param desc
     */
    public void siftDetector(Mat image, MatOfKeyPoint kp, Mat desc) {
        //Initiate SIFT detector
//		org.opencv..xfeatures2d.SIFT
//		sift = cv2.xfeatures2d.SIFT_create();
//
//		//find the keypoints and descriptors with SIFT
//		sift.detectAndCompute(image, null, kp, desc);

    }

    /**
     * Uses ORB detector to compute interest points
     *
     * @param image
     * @param kp
     * @param desc
     */
    public void orbDetector(Mat image, MatOfKeyPoint kp, Mat desc) {
        //Initiate ORB detector
        org.opencv.features2d.ORB orb = org.opencv.features2d.ORB.create();//5000);

        orb.detectAndCompute(image, new Mat(), kp, desc);
    }

    /**
     * Computes interest points on the reference image
     *
     * @param reference The reference image
     * @param algo      Algorithm to use
     */
    public void computeReferenceImage(Mat reference, Algo algo) {
        detector(reference, algo, myKpRef, myDescRef);
    }

    /**
     * Matches descriptors from two different images
     *
     * @param desc1
     * @param desc2
     * @param algo
     * @return
     */
    public List<DMatch> match(Mat desc1, Mat desc2, Algo algo) {
        if (myMatchingType == MatchingType.BF_MATCHING) {
            org.opencv.features2d.BFMatcher bf = BFMatcher.create(org.opencv.core.Core.NORM_HAMMING, true);
            bf.train();
            MatOfDMatch matches = new MatOfDMatch();

            //Match descriptors.
            bf.match(desc1, desc2, matches);

            List<DMatch> matchArray = matches.toList();
            //Sort them in the order of their distance.
            Collections.sort(matchArray, new Comparator<DMatch>() {
                @Override
                public int compare(DMatch o1, DMatch o2) {
                    if (o1.distance < o2.distance)
                        return -1;
                    if (o1.distance > o2.distance)
                        return 1;
                    return 0;
                }
            });

            ArrayList<DMatch> nMatchArray = new ArrayList<>(myORBnMatch);
            if(myORBnMatch < matchArray.size()) {
                for(int i = 0; i< myORBnMatch; i++)
                    nMatchArray.add(matchArray.get(i));
                return nMatchArray;
            } else
                return matchArray;
        } else if (myMatchingType == MatchingType.KNN_MATCHING) {
            List<MatOfDMatch> matches = new ArrayList<>();
            org.opencv.features2d.BFMatcher bf = BFMatcher.create();
            bf.train();
            bf.knnMatch(desc1, desc2, matches, 2);

            //store all the good matches as per Lowe's ratio test.
            List<DMatch> goodMatches = new ArrayList<DMatch>();
            for (int i = 0; i < matches.size(); i++) {
                MatOfDMatch match = matches.get(i);
                DMatch[] matchArray = match.toArray();
                DMatch first = matchArray[0];//[i][0];
                float dist1 = matchArray[0].distance;
                float dist2 = matchArray[1].distance;

                if (dist1 < LOWE_RATIO * dist2) {
                    goodMatches.add(first);
                }
            }

            return goodMatches;
        } else {
            //org.opencv.features2d.BFMatcher bf = FlannBasedMatcher.create();
            return null;
        }
    }

    /**
     * Computes the homography matrix between the given image and the reference
     * image. If the given image does not contain the reference object/pattern
     * of the reference image, or if the homography matrix could not be computed,
     * a null matrix is returned. All the steps are performed here (detection,
     * matching, and homography computation).
     *
     * @param image The given image
     * @param algo  Algorithm to use
     * @return The homography matrix
     */
    public Mat computeHomography(Mat image, Algo algo) {
        //Image image = reader.acquireNextImage();
        //org.opencv.android.Utils.bitmapToMat();
        MatOfKeyPoint kpFrame = new MatOfKeyPoint();
        Mat descFrame = new Mat();
        detector(image, algo, kpFrame, descFrame);
        //Log.i("lol", ""+descFrame);
        if(descFrame.rows() < 2)
            return null;
        List<DMatch> matches = match(myDescRef, descFrame, algo);
        Mat homography = null;

        //compute Homography if enough matches are found
        if (matches.size() > MIN_MATCHES) {
            //Find homography
            MatOfPoint2f srcPoints = new MatOfPoint2f();
            MatOfPoint2f destPoints = new MatOfPoint2f();
            //destPoints.alloc(matches.size());
            KeyPoint[] tmpRef = myKpRef.toArray();
            KeyPoint[] tmpFrame = kpFrame.toArray();
            Point[] tmpSrc = new Point[matches.size()];
            Point[] tmpDest = new Point[matches.size()];
            int cpt = 0;
            for (DMatch match : matches) {
                tmpSrc[cpt] = tmpRef[match.queryIdx].pt;
                tmpDest[cpt] = tmpFrame[match.trainIdx].pt;
                cpt++;
            }
            srcPoints.fromArray(tmpSrc);
            destPoints.fromArray(tmpDest);

            homography = org.opencv.calib3d.Calib3d.findHomography(srcPoints, destPoints, org.opencv.calib3d.Calib3d.RANSAC,5.0);
            return homography;
        }

        return null;
    }

    public static void main(String[] args) {
        System.out.println(Core.NATIVE_LIBRARY_NAME);
        //System.load( );
        //System.load( );
        Mat imgRef = Imgcodecs.imread("./data/ref2corrigee.jpg", Imgcodecs.IMREAD_UNCHANGED);//, Imgcodecs.IMREAD_GRAYSCALE);//org.opencv.imgcodecs.Imgcodecs.IMREAD_UNCHANGED );
        VideoCapture vid = new VideoCapture("./data/20181009_140438.mp4");
//		fourcc = cv2.VideoWriter_fourcc('M', 'J', 'P', 'G');
        Mat texture = Imgcodecs.imread("./data/Annaleiva.jpg", Imgcodecs.IMREAD_UNCHANGED);
        ObjLoader obj = new ObjLoader("./data/dona-teodora.obj", true);

        int MIN_MATCHES = 10;

        Algo algo = Algo.ORB;
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

        while (true) {
            //read the current frame
            Mat frame = new Mat();
            if (!vid.read(frame)) {
                System.out.println("Unable to capture video");
                break;
            }

            if (firstFrame) {
                matcher.computeReferenceImage(imgRef, algo);
                firstFrame = false;

                //For video, resize pixel size accordingly
                pixelSizeMm = pixelSizeMm * (imageRefSizeX / (double) frame.cols());
                fpixel = focalLengthMm / pixelSizeMm;
                K = new Mat(3, 3, CvType.CV_64FC1);
                K.put(0, 0, fpixel);
                K.put(0, 1, 0.);
                K.put(0, 2, frame.cols() / 2.);
                K.put(1, 0, 0);
                K.put(1, 1, fpixel);
                K.put(1, 2, frame.rows() / 2.);
                K.put(2, 0, 0);
                K.put(2, 1, 0);
                K.put(2, 2, 1);
            }

            Mat h = matcher.computeHomography(frame, algo);
            //h.convertTo(h, CvType.CV_32F);

            if (drawBorder) {
                Size size = imgRef.size();
                MatOfPoint2f pts = new MatOfPoint2f(new Point(0f, 0f),
                        new Point(0f, size.height - 1),
                        new Point(size.width - 1, size.height - 1),
                        new Point(size.width - 1, 0));
                MatOfPoint2f dst = new MatOfPoint2f();
                Core.perspectiveTransform(pts, dst, h);
                //System.out.println(pts.toList());
                //System.out.println(dst.toList());
                MatOfPoint intDst = new MatOfPoint();
                dst.convertTo(intDst, CvType.CV_32S);

                //connect them with lines
                Imgproc.polylines(frame, Arrays.asList(intDst), true, new Scalar(255), 3, Imgproc.LINE_AA);

//			    HighGui.imshow("frame", frame);
//				HighGui.waitKey(50);
            }

            if (drawMatches) {
                //Draw first 10 matches.
//		        frame = HighGui..drawMatches(imgRef,myKpRef,frame,kpFrame,matches[:50], None, flags=2)
//		        Imgproc.resize(frame, frame, new Size(1280,720));
//              HighGui.imshow("frame", frame);
//			    HighGui.waitKey(50);
            }
//		            
            if (h != null) {
                Mat proj = Utils.projectionMatrix(K, h);
                frame = Utils.render(frame, obj, proj, imgRef, true, texture);
                frame.convertTo(frame, CvType.CV_8UC3);
                //HighGui.imshow("frame", frame);
                //HighGui.waitKey(50);
            }
        }
    }
}
