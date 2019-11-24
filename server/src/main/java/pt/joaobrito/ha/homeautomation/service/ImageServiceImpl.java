package pt.joaobrito.ha.homeautomation.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.opencv.core.MatOfKeyPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ha.joaobrito.pt.ha.domain.ImagesDto;
import pt.joaobrito.ha.homeautomation.collection.Image;
import pt.joaobrito.ha.homeautomation.dao.ImageDao;
import pt.joaobrito.ha.homeautomation.opencv.OpenCVException;
import pt.joaobrito.ha.homeautomation.opencv.SURFDetector;
import pt.joaobrito.ha.homeautomation.opencv.SurfResult;

@Service
public class ImageServiceImpl implements ImageService {

    private static final Logger LOG = LoggerFactory.getLogger(ImageServiceImpl.class);

    private static final Object TAG = ImageServiceImpl.class.getName();

    @Autowired
    private SURFDetector surfDetector;

    @Autowired
    private ImageDao dao;


    private Future<List<Image>> futureImageObjects;
    private ExecutorService executor = Executors.newCachedThreadPool();


    @PostConstruct
    public void init() {
        this.futureImageObjects = updateImages(executor);
    }

    private Future<List<Image>> updateImages(ExecutorService executor) {
        Future<List<Image>> future = executor.submit(new Callable<List<Image>>() {
            @Override
            public List<Image> call() throws Exception {
                List<Image> images = dao.fetchAllMlabImages();

                List<Image> resultList = images.parallelStream().map(img -> {
                    try {
                        Map<String, MatOfKeyPoint> m = surfDetector.processObjects(img.getEncodedImage());
                        img.setDescriptor(m.get("desc"));
                        img.setKeyPoints(m.get("kp"));
                        return img;
                    } catch (OpenCVException e) {
                        return null;
                    }
                }).collect(Collectors.toList());

                LOG.info("Finished executor of updating the images. Total processed images is {}", resultList.size());
                return resultList;
            }
        });
        executor.shutdown();
        return future;
    }

    @Override
    public String save(Image image) {
        String result = dao.save(image);
        if (!executor.isTerminated()) {
            try {
                executor.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        executor = Executors.newCachedThreadPool();
        this.futureImageObjects = updateImages(executor);
        return result;
    }

    public List<Image> fetchAllMlabImages() {
        return getObjectImagesFromFuture();
    }

    @Override
    public ImagesDto find(byte[] scene) throws OpenCVException {

        MatOfKeyPoint scnDescriptor = surfDetector.processScene(scene);

        // alternative 1
        Comparator<Image> comparator = Comparator.comparing(Image::getMatches);

        Optional<Image> match = Optional.empty();
        Stream<Image> imageStream = null;
        if (!LOG.isDebugEnabled()) {
            LOG.info("{} - Executing non-debug block", TAG);
            imageStream = getObjectImagesFromFuture().parallelStream();
        } else {
            LOG.debug("{} - Executing debug block", TAG);
            imageStream = this.getObjectImagesFromFuture().stream();

        }
        match = imageStream.filter(o -> {
            SurfResult res;
            try {
                res = surfDetector.find(o.getEncodedImage(), scene, o.getDescriptor(), o.getKeyPoints(), scnDescriptor);
            } catch (OpenCVException e) {
                LOG.error(e.getMessage());
                return false;
            }
            return res.isMatch();
        }).max(comparator);

        if (match.isPresent()) {
            Image img = match.get();

            ImagesDto res = new ImagesDto();
            res.setEncodedImage(img.getEncodedImage());
            res.setName(img.getName());
            res.setAction(img.getAction());
            res.setObjectDesc(img.getDescription());

            LOG.debug("image id " + img.getId().toHexString() + " - " + img.getName() + ", with " + img.getMatches()
                    + " matches, action " + img.getAction());
            return res;
        }
        LOG.debug("no match");
        return null;
    }

    @Override
    public void print(List<Image> images) {
        if (executor.isTerminated()) {
            LOG.debug("Started method print @ {}", new Date());
            images.parallelStream().forEach(img -> {
                if (img != null && img.getId() != null) {
                    surfDetector.print(img, img.getId().toString());
                }
            });
            LOG.debug("Ended method print @ {}", new Date());
        }
    }

    private List<Image> getObjectImagesFromFuture() {
        try {
            if (!executor.isTerminated()) {
                executor.awaitTermination(60, TimeUnit.SECONDS);
            }
            return this.futureImageObjects.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
