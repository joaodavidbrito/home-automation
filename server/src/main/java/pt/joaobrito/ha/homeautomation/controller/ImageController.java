package pt.joaobrito.ha.homeautomation.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ha.joaobrito.pt.ha.domain.ImagesDto;
import ha.joaobrito.pt.ha.domain.LogEvent;
import ha.joaobrito.pt.ha.domain.LogEvent.EventType;
import pt.joaobrito.ha.homeautomation.collection.Image;
import pt.joaobrito.ha.homeautomation.opencv.OpenCVException;
import pt.joaobrito.ha.homeautomation.service.ImageService;
import pt.joaobrito.ha.homeautomation.service.TimeSyncService;

@RestController
@RequestMapping("/images")
@PropertySource(value = "classpath:application.yml")
public class ImageController {

    private static final Logger LOG = LoggerFactory.getLogger(ImageController.class);
    private static final String TAG = ImageController.class.getName();
    private Long offsetTime = 0L;

    private ImageService findService;
    private TimeSyncService timeSyncService;

    @Autowired
    public ImageController(ImageService findService, TimeSyncService timeSyncService) {
        this.findService = findService;
        this.timeSyncService = timeSyncService;
    }

    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody byte[] data) {
        LOG.debug("Just enter save method @: {}", Instant.now().toEpochMilli());

        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {

            ImagesDto image = (ImagesDto) ois.readObject();
            Image img = new Image();
            img.setEncodedImage(image.getEncodedImage());
            img.setName(image.getName());
            img.setAction(image.getAction());
            img.setDescription(image.getObjectDesc());

            String res = findService.save(img);
            return new ResponseEntity<>(res, HttpStatus.CREATED);
        } catch (Exception e) {
            String erroMessage = e.getMessage();
            LOG.error(erroMessage, e);
            return new ResponseEntity<>(erroMessage, HttpStatus.BAD_REQUEST);
        } finally {
            LOG.debug("Exit save method @: {}", Instant.now().toEpochMilli());
        }
    }

    @PostMapping("/find")
    public ResponseEntity<?> find(@RequestBody byte[] data) {
        LOG.debug("Just enter in find method @: {}", System.currentTimeMillis());
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis);
             ByteArrayOutputStream out = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(out)) {

            ImagesDto dto = (ImagesDto) ois.readObject();

            // matching
            long serverStartMatching = System.currentTimeMillis() + offsetTime;
            if (dto.getNotFounds() == 0) {
                dto.getEvents().add(new LogEvent(serverStartMatching, EventType.SERVER_START_MATCHING));
            } else {
                dto.getEvents().add(new LogEvent(serverStartMatching, EventType.SERVER_RESTART_MATCHING));
            }

            ImagesDto res;
            try {
                res = findService.find(dto.getEncodedImage());
            } catch (OpenCVException e) {
                long serverError = System.currentTimeMillis() + offsetTime;
                dto.getEvents().add(new LogEvent(serverError, EventType.SERVER_ERROR));
                dto.setEncodedImage(null);
                dto.incrementNotFounds();
                oos.writeObject(dto);
                return new ResponseEntity<>(out.toByteArray(), HttpStatus.INTERNAL_SERVER_ERROR);
            }

            long serverEndMatching = System.currentTimeMillis() + offsetTime;
            if (dto.getNotFounds() == 0) {
                dto.getEvents().add(new LogEvent(serverEndMatching, EventType.SERVER_END_MATCHING));
            } else {
                dto.getEvents().add(new LogEvent(serverEndMatching, EventType.SERVER_REEND_MATCHING));
            }

            // not found
            if (res == null) {
                long serverNotFound = System.currentTimeMillis() + offsetTime;
                dto.getEvents().add(new LogEvent(serverNotFound, EventType.SERVER_NOT_FOUND));
                dto.setEncodedImage(null);
                dto.incrementNotFounds();
                oos.writeObject(dto);
                return new ResponseEntity<>(out.toByteArray(), HttpStatus.NOT_FOUND);
            } else {
                long serverFound = System.currentTimeMillis() + offsetTime;
                dto.getEvents().add(new LogEvent(serverFound, EventType.SERVER_FOUND));
            }

            // OK
            dto.setEncodedImage(res.getEncodedImage());
            dto.setName(res.getName());
            dto.setAction(res.getAction());
            dto.setObjectDesc(res.getObjectDesc());

            long serverEnd = System.currentTimeMillis() + offsetTime;
            dto.getEvents().add(new LogEvent(serverEnd, EventType.SERVER_END));

            if (LOG.isDebugEnabled()) {
                dto.getEvents().forEach(e -> LOG.debug("event name: {}, timestamp: {}", e.getEvent().name(), e.getTimestamp()));
            }

            oos.writeObject(dto);
            LOG.debug("timestamp before send OK: {}", System.currentTimeMillis());
            return new ResponseEntity<>(out.toByteArray(), HttpStatus.OK);

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } finally {
            LOG.debug("Exit find method @: {}", Instant.now().toEpochMilli());
        }
    }

    @GetMapping("/objects")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity<?> getObjects() {
        LOG.debug("Just enter in getObjects method @: {}", Instant.now().toEpochMilli());
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(out)) {

            Comparator<Image> comparator = Comparator.comparing(i -> i.getId().getTimestamp());

            List<ImagesDto> resp = findService.fetchAllMlabImages().stream().sorted(comparator).map(i -> {
                ImagesDto dto = new ImagesDto();
                dto.setId(i.getId().toString());
                dto.setEncodedImage(i.getEncodedImage());
                dto.setObjectDesc(i.getDescription());
                dto.setAction(i.getAction());
                return dto;
            }).collect(Collectors.toList());

            oos.writeObject(resp);
            return new ResponseEntity<>(out.toByteArray(), HttpStatus.OK);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } finally {
            LOG.debug("Exit getObjects method: {}", Instant.now().toEpochMilli());
        }
    }


    @GetMapping("/hello")
    public ResponseEntity<?> hello() {
        LOG.debug("{} enter hello method: {}", TAG, Instant.now().toEpochMilli());
        return new ResponseEntity<>("Hello openCV", HttpStatus.OK);
    }

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        LOG.debug("{} enter ping method: {}", TAG, Instant.now().toEpochMilli());
        ResponseEntity<?> pong = ResponseEntity.noContent().build();
        return pong;
    }

    @Scheduled(fixedDelay = 3600 * 1000, initialDelay = 0L)
    private void updateTime() {
        this.offsetTime = timeSyncService.update();
    }
}
