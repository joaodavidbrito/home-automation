package pt.joaobrito.ha.homeautomation.dao;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.Morphia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import pt.joaobrito.ha.homeautomation.collection.Image;

import javax.annotation.PostConstruct;
import java.util.List;

@Repository
public class ImageDao {

    private static final Logger LOG = LoggerFactory.getLogger(ImageDao.class);

    private static final String DB_NAME = "iot";

    @Value(value = "${mongodb.url}")
    private String mongodbUrl;

    private Datastore datastore;

    @PostConstruct
    public void init() {
        LOG.debug("starting init in service: {}", LOG.getName());
        Morphia morphia = new Morphia();
        morphia.mapPackage("pt.joaobrito.ha.homeautomation.collection");
        datastore = morphia.createDatastore(new MongoClient(new MongoClientURI(mongodbUrl)), DB_NAME);
        datastore.ensureIndexes();
    }

    public String save(Image image) {
        Image img = new Image();
        img.setEncodedImage(image.getEncodedImage());
        img.setName(image.getName());
        img.setAction(image.getAction());
        img.setDescription(image.getDescription());
        Key<Image> r = datastore.save(img);
        return r.getId().toString();
    }

    public List<Image> fetchAllMlabImages() {
        return datastore.find(Image.class).asList();
    }


}
