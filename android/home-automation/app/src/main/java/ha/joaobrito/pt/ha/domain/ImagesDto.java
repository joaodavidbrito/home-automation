package ha.joaobrito.pt.ha.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ImagesDto implements Serializable {
    private static final long serialVersionUID = -992159325084386848L;

    private String id;
    private byte[] encodedImage;
    private String name;
    private String action;
    private int notFounds;
    private String objectDesc;
    private String sceneDesc;
    private List<LogEvent> events = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public List<LogEvent> getEvents() {
        return events;
    }

    public void setEvents(List<LogEvent> events) {
        this.events = events;
    }

    public int getNotFounds() {
        return notFounds;
    }

    public void incrementNotFounds() {
        this.notFounds++;
    }

    public String getObjectDesc() {
        return objectDesc;
    }

    public void setObjectDesc(String objectDesc) {
        this.objectDesc = objectDesc;
    }

    public String getSceneDesc() {
        return sceneDesc;
    }

    public void setSceneDesc(String sceneDesc) {
        this.sceneDesc = sceneDesc;
    }
}

