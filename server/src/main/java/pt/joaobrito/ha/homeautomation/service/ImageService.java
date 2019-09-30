package pt.joaobrito.ha.homeautomation.service;

import java.util.List;
import java.util.Map;

import org.opencv.core.Mat;

import ha.joaobrito.pt.ha.domain.ImagesDto;
import pt.joaobrito.ha.homeautomation.collection.Image;
import pt.joaobrito.ha.homeautomation.opencv.OpenCVException;

public interface ImageService {
    /**
     * save image in DB
     *
     * @param image
     * @return
     */
    String save(Image image);

    /**
     * find image/object match
     *
     * @param image
     * @return
     */
    ImagesDto find(byte[] scene) throws OpenCVException;

    /**
     * print all messages in database
     */
    void print(List<Image> images);

    /**
     * fetch all images from DB
     *
     * @param key
     * @return
     */
    List<Image> fetchAllMlabImages();
}
