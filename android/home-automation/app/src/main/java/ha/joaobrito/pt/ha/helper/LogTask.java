package ha.joaobrito.pt.ha.helper;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import ha.joaobrito.pt.ha.domain.ImagesDto;
import ha.joaobrito.pt.ha.domain.LogEvent;

public class LogTask extends AsyncTask<ImagesDto, String, Void> {

    // CONSTANTS
    private static final String NUMBER_NOT_FOUNDS = "NUMBER_NOT_FOUNDS";
    private static final String DESCRIPTION = "DESCRIPTION";
    private static final String ANDROID = "ANDROID";
    private static final String SERVER = "SERVER";
    private static final String MEASUREMENT_ANDROID = "android";
    private static final String MEASUREMENT_SERVER = "server";
    private static final String EVENT_TYPE = "event_type";
    private static final String FIELD_VAL = "val";
    private static final String SCENE_DESC = "SCENE_DESC";

    private final String serverHost;
    private final String database;

    public LogTask (Context context){
        this.serverHost = SingletonHelper.getInstance(context).getInfluxdbUrl();
        this.database = SingletonHelper.getInstance(context).getInfluxdbDatabase();
    }

    @Override
    protected Void doInBackground(ImagesDto... data) {
        String url = "http://" + serverHost;
        String database = this.database;

        InfluxDB influxDB = InfluxDBFactory.connect(url);
        influxDB.setDatabase(database);

        Pong response = influxDB.ping();
        if (response.getVersion().equalsIgnoreCase("unknown")) {
            Log.e("InfluxDB", "Error pinging server.");
            return null;
        }

        BatchPoints batchPoints = BatchPoints
                .database(database)
                .build();

        Map<String, String> envTags = new HashMap<>();
        envTags.put(NUMBER_NOT_FOUNDS, String.valueOf(data[0].getNotFounds()));
        envTags.put(DESCRIPTION, String.valueOf(data[0].getObjectDesc()));
        envTags.put(SCENE_DESC, String.valueOf(data[0].getSceneDesc()));

        for (LogEvent e : data[0].getEvents()) {
            Log.d("EVENTS", e.getEvent().name() + "@" + e.getTimestamp());
            Point point = null;
            if(e.getEvent().name().startsWith(ANDROID)){
                point = Point.measurement(MEASUREMENT_ANDROID)
                        .time(e.getTimestamp(), TimeUnit.MILLISECONDS)
                        .tag(EVENT_TYPE, e.getEvent().name())
                        .tag(envTags)
                        .addField(FIELD_VAL, 10)
                        .build();
            }else if(e.getEvent().name().startsWith(SERVER)){
                point = Point.measurement(MEASUREMENT_SERVER)
                        .time(e.getTimestamp(), TimeUnit.MILLISECONDS)
                        .tag(EVENT_TYPE, e.getEvent().name())
                        .tag(envTags)
                        .addField(FIELD_VAL, 20)
                        .build();
            }
            batchPoints.point(point);
        }

        influxDB.write(batchPoints);

        return null;
    }
}
