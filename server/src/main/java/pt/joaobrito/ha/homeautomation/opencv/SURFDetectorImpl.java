package pt.joaobrito.ha.homeautomation.opencv;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.highgui.Highgui;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import pt.joaobrito.ha.homeautomation.collection.Image;

@Component
public class SURFDetectorImpl implements SURFDetector {

    private static final Logger LOG = LoggerFactory.getLogger(SURFDetectorImpl.class);
    private static final float NNDR_RATIO = 0.3f; //0.7f
    private static final int THRESHOLD = 10;

    private Mat sceneImage;
    private MatOfKeyPoint sceneKeyPoints;

    public Map<String, MatOfKeyPoint> processObjects(byte[] obj) throws OpenCVException {
        Mat objectImage = Highgui.imdecode(new MatOfByte(obj), Highgui.CV_LOAD_IMAGE_COLOR);

        if (LOG.isDebugEnabled()) {
            Highgui.imwrite("output/object.jpg", objectImage);
        }

        LOG.debug("Detecting key points...");
        FeatureDetector featureDetector = FeatureDetector.create(FeatureDetector.SURF);
        MatOfKeyPoint objectKeyPoints = new MatOfKeyPoint();
        featureDetector.detect(objectImage, objectKeyPoints);

        LOG.debug("Computing descriptors...");
        MatOfKeyPoint objectDescriptors = new MatOfKeyPoint();
        DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.SURF);
        descriptorExtractor.compute(objectImage, objectKeyPoints, objectDescriptors);
        if (objectDescriptors.empty()) {
            throw new OpenCVException("Object Descriptor is null");
        }

        Map<String, MatOfKeyPoint> map = new HashMap<>();
        map.put("desc", objectDescriptors);
        map.put("kp", objectKeyPoints);

        return map;
    }

    public MatOfKeyPoint processScene(byte[] scene) throws OpenCVException {
        LOG.debug("Start processing scene @ {}", new Date());

        this.sceneImage = Highgui.imdecode(new MatOfByte(scene), Highgui.CV_LOAD_IMAGE_COLOR);

        if (LOG.isDebugEnabled()) {
            Highgui.imwrite("output/scene.jpg", this.sceneImage);
        }

        LOG.debug("Detecting key points...");
        FeatureDetector featureDetector = FeatureDetector.create(FeatureDetector.SURF);
        this.sceneKeyPoints = new MatOfKeyPoint();
        featureDetector.detect(this.sceneImage, this.sceneKeyPoints);

        LOG.debug("Computing descriptors...");
        MatOfKeyPoint sceneDescriptors = new MatOfKeyPoint();
        DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.SURF);
        descriptorExtractor.compute(this.sceneImage, this.sceneKeyPoints, sceneDescriptors);

        if (sceneDescriptors.empty()) {
            throw new OpenCVException("Scene Descriptor is null");
        }

        LOG.debug("End processing scene @ {}", new Date());

        return sceneDescriptors;
    }

    public SurfResult find(byte[] objBytes, byte[] scnBytes, MatOfKeyPoint obj, MatOfKeyPoint keyPoints, MatOfKeyPoint scn) throws OpenCVException {
        try {
            LOG.info("Started matching.... {}", new Date());
            SurfResult result = new SurfResult();

            // common part
            List<MatOfDMatch> matches = new LinkedList<>();
            DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);

            LOG.debug("Matching object and scene images...");
            descriptorMatcher.knnMatch(obj, scn, matches, 2);

            LOG.debug("Calculating good match list...");
            LinkedList<DMatch> goodMatchesList = new LinkedList<>();

            matches.forEach(m -> {
                DMatch[] dmatchArray = m.toArray();
                DMatch m1 = dmatchArray[0];
                DMatch m2 = dmatchArray[1];

                if (m1.distance <= m2.distance * NNDR_RATIO) {
                    goodMatchesList.addLast(m1);
                }
            });

            LOG.debug("Matches found: {}.", goodMatchesList.size());

            if (!LOG.isDebugEnabled()) {
                if (goodMatchesList.size() > THRESHOLD) {
                    result.setMatch(true);
                    result.setMatches(goodMatchesList.size());
                    LOG.info("Object found! Ended matching successfuly.... {}", new Date());
                    return result;
                }
            } else {
                if (goodMatchesList.size() > THRESHOLD) {
                    result.setMatch(true);
                    result.setMatches(goodMatchesList.size());

                    List<KeyPoint> objKeypointlist = keyPoints.toList();
                    List<KeyPoint> scnKeypointlist = sceneKeyPoints.toList();

                    LinkedList<Point> objectPoints = new LinkedList<>();
                    LinkedList<Point> scenePoints = new LinkedList<>();
                    goodMatchesList.forEach(gm -> {
                        objectPoints.addLast(objKeypointlist.get(gm.queryIdx).pt);
                        scenePoints.addLast(scnKeypointlist.get(gm.trainIdx).pt);
                    });

                    MatOfPoint2f objMatOfPoint2f = new MatOfPoint2f();
                    objMatOfPoint2f.fromList(objectPoints);
                    MatOfPoint2f scnMatOfPoint2f = new MatOfPoint2f();
                    scnMatOfPoint2f.fromList(scenePoints);

                    Mat homography = Calib3d.findHomography(objMatOfPoint2f, scnMatOfPoint2f, Calib3d.RANSAC, 3);

                    Mat obj_corners = new Mat(4, 1, CvType.CV_32FC2);
                    Mat scene_corners = new Mat(4, 1, CvType.CV_32FC2);

                    Mat objectImage = Highgui.imdecode(new MatOfByte(objBytes), Highgui.CV_LOAD_IMAGE_COLOR);
                    obj_corners.put(0, 0, 0, 0);
                    obj_corners.put(1, 0, objectImage.cols(), 0);
                    obj_corners.put(2, 0, objectImage.cols(), objectImage.rows());
                    obj_corners.put(3, 0, 0, objectImage.rows());

                    LOG.debug("Transforming object corners to scene corners...");
                    Core.perspectiveTransform(obj_corners, scene_corners, homography);

                    Mat img = Highgui.imdecode(new MatOfByte(scnBytes), Highgui.CV_LOAD_IMAGE_COLOR);
                    Core.line(img, new Point(scene_corners.get(0, 0)), new Point(scene_corners.get(1, 0)), new Scalar(0, 255, 0), 4);
                    Core.line(img, new Point(scene_corners.get(1, 0)), new Point(scene_corners.get(2, 0)), new Scalar(0, 255, 0), 4);
                    Core.line(img, new Point(scene_corners.get(2, 0)), new Point(scene_corners.get(3, 0)), new Scalar(0, 255, 0), 4);
                    Core.line(img, new Point(scene_corners.get(3, 0)), new Point(scene_corners.get(0, 0)), new Scalar(0, 255, 0), 4);

                    LOG.debug("Drawing matches image...");
                    MatOfDMatch goodMatches = new MatOfDMatch();
                    goodMatches.fromList(goodMatchesList);

                    LOG.debug("Creating the matrix for output image...");
                    Mat outputImage = new Mat(objectImage.rows(), objectImage.cols(), Highgui.CV_LOAD_IMAGE_COLOR);
                    Scalar newKeypointColor = new Scalar(255, 0, 0);

                    LOG.debug("Drawing key points on object image...");
                    Features2d.drawKeypoints(objectImage, keyPoints, outputImage, newKeypointColor, 0);

                    Mat matchoutput = new Mat(sceneImage.rows() * 2, sceneImage.cols() * 2, Highgui.CV_LOAD_IMAGE_COLOR);
                    Scalar matchestColor = new Scalar(0, 255, 0);

                    Features2d.drawMatches(objectImage, keyPoints, sceneImage, sceneKeyPoints, goodMatches,
                            matchoutput, matchestColor, newKeypointColor, new MatOfByte(), 2);

                    Highgui.imwrite("output/outputImage.jpg", outputImage);

                    Highgui.imwrite("output/matchoutput.jpg", matchoutput);

                    Highgui.imwrite("output/img.jpg", img);
                    LOG.debug("Object found! Ended matching successfuly.... {}", new Date());
                    return result;
                }
            }

            LOG.info("Object Not Found. Ended matching - No success.... {}", new Date());
            return result;
        } catch (Exception e) {
            throw new OpenCVException("Something wrong happened");
        }
    }

    @Override
    public void print(Image img, String id) {
        Mat image = Highgui.imdecode(new MatOfByte(img.getEncodedImage()), Highgui.CV_LOAD_IMAGE_COLOR);
        try {
            Highgui.imwrite("print/img_" + id + ".jpg", image);
        } catch (Exception e) {
            LOG.error("Error printing the images: {}", e.getMessage(), e);
        }
    }
}