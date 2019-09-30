package pt.joaobrito.ha.homeautomation.opencv;

import java.util.Map;

import org.opencv.core.MatOfKeyPoint;

import pt.joaobrito.ha.homeautomation.collection.Image;

public interface SURFDetector {
    Map<String, MatOfKeyPoint> processObjects(byte[] obj) throws OpenCVException;

    MatOfKeyPoint processScene(byte[] scene) throws OpenCVException;

    SurfResult find(byte[] objBytes, byte[] scnBytes, MatOfKeyPoint obj, MatOfKeyPoint keyPoints, MatOfKeyPoint scn) throws OpenCVException;

    void print(Image img, String id);
}
