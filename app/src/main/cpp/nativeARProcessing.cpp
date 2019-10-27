#include <jni.h>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/features2d.hpp>
#include <opencv2/calib3d.hpp>
#include <vector>


extern "C" {
JNIEXPORT void JNICALL Java_org_sample_opencvsample_1mixedprocessing_Tutorial2Activity_FindFeatures(JNIEnv*, jobject, jlong addrGray, jlong addrRgba);
enum Algo {
    ORB,
    SIFT
};

enum MatchingType {
    BF_MATCHING,
    KNN_MATCHING,
    FLANN_MATCHING
};

void orbDetector(cv::Mat& image, std::vector<cv::KeyPoint>& kp, cv::Mat& descriptor) {
    cv::Ptr<cv::ORB> orb = cv::ORB::create(); //1000);
    orb->detectAndCompute(image, cv::noArray(), kp, descriptor);
}

void siftDetector(cv::Mat& image, std::vector<cv::KeyPoint>& kp, cv::Mat& descriptor) {
        //Initiate SIFT detector

//		sift = cv2.xfeatures2d.SIFT_create();
//
//		//find the keypoints and descriptors with SIFT
//		sift.detectAndCompute(image, null, kp, desc);

}

void detector(cv::Mat& image, int algo, std::vector<cv::KeyPoint>& kp, cv::Mat& descriptor) {
    if (algo == Algo::ORB)
        orbDetector(image, kp, descriptor);
    else
        siftDetector(image, kp, descriptor);
}

bool comparator(cv::DMatch& left, cv::DMatch& right) {
    if (left.distance < left.distance)
        return true;

    return false;
}

std::vector<cv::DMatch> match(cv::Mat& desc1, cv::Mat& desc2,
                              MatchingType matchingType, int nMatch, float LOWE_RATIO) {
        if (matchingType == MatchingType::BF_MATCHING) {
            cv::Ptr<cv::BFMatcher> bf = cv::BFMatcher::create(cv::NORM_HAMMING, true);

            bf->train();
            std::vector<cv::DMatch> matches;

            //Match descriptors.
            bf->match(desc1, desc2, matches);

            //Sort them in the order of their distance.
            std::sort(matches.begin(), matches.end(), comparator);

            if(nMatch < matches.size()) {
                std::vector<cv::DMatch> nMatchArray(matches.begin(), matches.begin() + nMatch);
                return nMatchArray;
            } else
                return matches;

        } else if (matchingType == MatchingType::KNN_MATCHING) {
            std::vector<std::vector<cv::DMatch>> matches;
            cv::Ptr<cv::BFMatcher> bf = cv::BFMatcher::create();
            bf->train();
            bf->knnMatch(desc1, desc2, matches, 2);

            //store all the good matches as per Lowe's ratio test.
            std::vector<cv::DMatch> goodMatches;
            for (int i = 0; i < matches.size(); i++) {
                float dist1 = matches[i][0].distance;
                float dist2 = matches[i][1].distance;

                if (dist1 < LOWE_RATIO * dist2) {
                    goodMatches.push_back(matches[i][0]);
                }
            }

            return goodMatches;
        } else {
            //org.opencv.features2d.BFMatcher bf = FlannBasedMatcher.create();
            return std::vector<cv::DMatch>();
        }
    }

void matToVectorKeyPoint(cv::Mat& mat, std::vector<KeyPoint>& v_kp)
{
    v_kp.clear();
    assert(mat.type()==CV_32FC(7) && mat.cols==1);
    for(int i=0; i<mat.rows; i++)
    {
        cv::Vec<float, 7> v = mat.at< cv::Vec<float, 7> >(i, 0);
        cv::KeyPoint kp(v[0], v[1], v[2], v[3], v[4], (int)v[5], (int)v[6]);
        v_kp.push_back(kp);
    }
    return;
}

JNIEXPORT jboolean JNICALL Java_com_ar_core_Matcher_computeHomography(
         JNIEnv* env, jobject, jlong addrKpRef, jlong addrDescRef, jlong addrImage,
         jlong addrHomography, jint algo, jint matchingType, jint nMatch, jfloat loweRatio, jint MIN_MATCHES) {
    Mat& image  = *(Mat*)addrImage;
    Mat& tmp  = *(Mat*)addrKpRef;
    std::vector<KeyPoint> kpRef;
    matToVectorKeyPoint(tmp, kpRef);
    Mat& descRef  = *(Mat*)addrDescRef;
    Mat& homography = *(Mat*) addrHomography;

        std::vector<cv::KeyPoint> kpFrame;
        cv::Mat descFrame;
        detector(image, algo, kpFrame, descFrame);

        if(descFrame.rows < 2)
            return false;

        std::vector<DMatch> matches = match(descRef, descFrame, (MatchingType)matchingType, nMatch, loweRatio); //to check, copies ?
        //cv::Mat homography;

        //compute Homography if enough matches are found
        if (matches.size() > MIN_MATCHES) {
            //Find homography
            std::vector<cv::Point2f> srcPoints(matches.size());
            std::vector<cv::Point2f> destPoints(matches.size());
            int cpt = 0;
            for (DMatch match : matches) {
                srcPoints[cpt] = kpRef[match.queryIdx].pt;
                destPoints[cpt] = kpFrame[match.trainIdx].pt;
                cpt++;
            }

//            jclass _class = env->FindClass("org/opencv/core/Mat");
//
//            if (NULL == _class)
//                return false;
//            //PrintError ("class");
//
//            jmethodID cid = env->GetMethodID(_class, "<init>", "(DD)V");
//
//            if (NULL == cid)
//                return false;
//            //PrintError ("method");

            std::vector<uchar> mask;

            //jobject homography = env->NewObject(_class, cid);
            //cv::Mat tmpHomography;
            homography = cv::findHomography(srcPoints, destPoints, cv::RANSAC, 5.0, mask);
            //homography = tmpHomography;

            std::vector<DMatch> inliers;
            for (int i = 0; i < mask.size(); i++)
            {
                if (mask[i] != 0)
                    inliers.push_back(matches[i]);
            }

            matches.swap(inliers);
            //return (matches.size() > MIN_MATCHES) ? homography : false;
            return matches.size() > MIN_MATCHES ? true : false;
        }

        return false;
    }
}

//NB: Project (app) right click, link C++ with gradle
//Android.mk, change path to locate opencv.mk
//Application.mk: c++_shared for APP_STL



//public void computeReferenceImage(Mat reference, Algo algo) {
//        detector(reference, algo, myKpRef, myDescRef);
//    }
//
//    /**
//     * Matches descriptors from two different images
//     *
//     * @param desc1
//     * @param desc2
//     * @param algo
//     * @return
//     */

//
//    /**
//     * Computes the homography matrix between the given image and the reference
//     * image. If the given image does not contain the reference object/pattern
//     * of the reference image, or if the homography matrix could not be computed,
//     * a null matrix is returned. All the steps are performed here (detection,
//     * matching, and homography computation).
//     *
//     * @param image The given image
//     * @param algo  Algorithm to use
//     * @return The homography matrix
//     */
//public Mat computeHomography(Mat image, Algo algo) {
//        MatOfKeyPoint kpFrame = new MatOfKeyPoint();
//        Mat descFrame = new Mat();
//        detector(image, algo, kpFrame, descFrame);
//
//        if(descFrame.rows() < 2)
//            return null;
//        List<DMatch> matches = match(myDescRef, descFrame, algo);
//        Mat homography = null;
//
//        //compute Homography if enough matches are found
//        if (matches.size() > MIN_MATCHES) {
//            //Find homography
//            MatOfPoint2f srcPoints = new MatOfPoint2f();
//            MatOfPoint2f destPoints = new MatOfPoint2f();
//            //destPoints.alloc(matches.size());
//            KeyPoint[] tmpRef = myKpRef.toArray();
//            KeyPoint[] tmpFrame = kpFrame.toArray();
//            Point[] tmpSrc = new Point[matches.size()];
//            Point[] tmpDest = new Point[matches.size()];
//            int cpt = 0;
//            for (DMatch match : matches) {
//                tmpSrc[cpt] = tmpRef[match.queryIdx].pt;
//                tmpDest[cpt] = tmpFrame[match.trainIdx].pt;
//                cpt++;
//            }
//            srcPoints.fromArray(tmpSrc);
//            destPoints.fromArray(tmpDest);
//
//            Mat mask = new Mat();
//            homography = org.opencv.calib3d.Calib3d.findHomography(srcPoints, destPoints, org.opencv.calib3d.Calib3d.RANSAC,5.0, mask);
//
//            ArrayList<DMatch> inliers = new ArrayList<>();
//            for (int i = 0; i < mask.rows(); i++)
//            {
//                if (mask.get(i, 0)[0] != 0)
//                    inliers.add(matches.get(i));
//            }
//
//            matches = new ArrayList(inliers);
//            return (matches.size() > MIN_MATCHES) ? homography : null;
//        }
//
//        return null;
//    }