package pt.joaobrito.ha.homeautomation.collection;

import java.io.Serializable;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;
import org.opencv.core.MatOfKeyPoint;


@Entity("images")
public class Image implements Serializable {
    @Id
    private ObjectId id;
    private byte[] encodedImage;
    private String name;
    private String action;
    private String description;
    private MatOfKeyPoint descriptor;
    private MatOfKeyPoint keyPoints;

    @Transient
    private int matches;

    public ObjectId getId() {
        return id;
    }

    public byte[] getEncodedImage() {
        return encodedImage;
    }

    public void setEncodedImage(byte[] encodedImage) {
        this.encodedImage = encodedImage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMatches() {
        return matches;
    }

    public MatOfKeyPoint getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(MatOfKeyPoint descriptor) {
        this.descriptor = descriptor;
    }

    public MatOfKeyPoint getKeyPoints() {
        return keyPoints;
    }

    public void setKeyPoints(MatOfKeyPoint keyPoints) {
        this.keyPoints = keyPoints;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
