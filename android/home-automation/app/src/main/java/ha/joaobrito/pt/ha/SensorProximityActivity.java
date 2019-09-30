package ha.joaobrito.pt.ha;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import ha.joaobrito.pt.ha.domain.ImagesDto;
import ha.joaobrito.pt.ha.domain.LogEvent;
import ha.joaobrito.pt.ha.helper.LogTask;
import ha.joaobrito.pt.ha.helper.MQTTHelper;


public class SensorProximityActivity extends Activity implements SensorEventListener {

    public static final String TAG = SensorProximityActivity.class.getName();

    private static String topic = "tabua/lamp";

    private SensorManager mSensorManager;
    private Sensor mSensorProximity;
    private Sensor mSensorAccelerometer;

    private static final String FAR = "ON";
    private static final String CLOSE = "OFF";
    private String lastCommand = CLOSE;

    private static Map<LevelEnum, String> levelMap = new HashMap<LevelEnum, String>();

    // accelerometer
    private static final float SHAKE_THRESHOLD = 3.25f; // m/S**2
    private static final float TAP_THRESHOLD = 3.25f; // m/S**2
    private static final int MIN_TIME_BETWEEN_SHAKES_MILLI_SECS = 1000;
    private long mLastShakeTime;

    private MqttAndroidClient client = MQTTHelper.client;
    private Long diffTime;


    static {
        levelMap.put(LevelEnum.FAR, FAR);
        levelMap.put(LevelEnum.CLOSE, CLOSE);
    }

    private enum LevelEnum {
        CLOSE, FAR;
    }

    private boolean proceed;
    private boolean close;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_proximity_sensor);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Listen for shakes
        mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        this.topic = getIntent().getStringExtra(Findctivity.TOPIC);
        this.diffTime = getIntent().getLongExtra(Findctivity.OFFSET_TIME, 0L);
        Log.d("timestamp sensor create", "" + (System.currentTimeMillis() + diffTime));
        Log.d(TAG, "difftime timestamp: " + diffTime);

        // image
        byte[] img = getIntent().getByteArrayExtra(Findctivity.IMAGE);
        ByteArrayInputStream in = new ByteArrayInputStream(img);
        ObjectInputStream is = null;
        try {
            is = new ObjectInputStream(in);
            is.readObject();
        } catch (IOException | ClassNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        Bitmap bMap = BitmapFactory.decodeByteArray(img, 0, img.length);

        ImageView imageView = findViewById(R.id.imageProx);
        imageView.setImageBitmap(bMap);
        imageView.setRotation(90f);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSensorAccelerometer != null) {
            mSensorManager.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(client == null){
            this.client = MQTTHelper.client;
        }
        if(!client.isConnected()){
            Log.d(TAG, "timestamp client not connected: " + (System.currentTimeMillis() + diffTime));
            return;
        }
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                Log.d(TAG, "Detected accelerometer");
                long curTime = System.currentTimeMillis();
                if ((curTime - mLastShakeTime) > MIN_TIME_BETWEEN_SHAKES_MILLI_SECS) {

                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    double acceleration = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)) - SensorManager.GRAVITY_EARTH;
                    Log.d(TAG, "Acceleration is " + acceleration + "m/s^2");


                    double tap = Math.sqrt(Math.pow(z, 2)) - SensorManager.GRAVITY_EARTH;

                    if(tap > TAP_THRESHOLD){
                        if(!close) {
                            Log.d(TAG, "positive positive detected: " + tap);
                            LogEvent e = new LogEvent(System.currentTimeMillis() + diffTime, LogEvent.EventType.ANDROID_POSITIVE_DETECTION);
                            ImagesDto dto = new ImagesDto();
                            dto.getEvents().add(e);
                            new LogTask(this).execute(dto);

                            proceed = true;
                            close = true;

                            mSensorProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
                            if (mSensorProximity != null) {
                                mSensorManager.registerListener(this, mSensorProximity, SensorManager.SENSOR_DELAY_NORMAL);
                            }

                            Toast toast = Toast.makeText(getApplicationContext(), "Positive Shake Detected", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }

                    else if (acceleration > SHAKE_THRESHOLD) {
                        if(!close) {
                            Log.d(TAG, "false match detected: " + tap);
                            LogEvent e = new LogEvent(System.currentTimeMillis() + diffTime, LogEvent.EventType.ANDROID_FALSE_DETECTION);
                            ImagesDto dto = new ImagesDto();
                            dto.getEvents().add(e);
                            new LogTask(this).execute(dto);

                            mLastShakeTime = curTime;
                            Log.i(TAG, "Shake, Rattle, and Roll");

                            Intent i = new Intent(getApplicationContext(), MainActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(i);
                            finish();

                            Toast toast = Toast.makeText(getApplicationContext(), "False Shake Detected", Toast.LENGTH_SHORT);
                            toast.show();

                        }else{
                            Intent i = new Intent(getApplicationContext(), MainActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(i);
                            finish();
                        }
                    }
                }
                break;
            case Sensor.TYPE_PROXIMITY:
                if(!proceed){
                    return;
                }

                Log.d(TAG, "Detected proximity");
                LevelEnum level = getLevel(event.values[1]);

                if (lastCommand.equals(levelMap.get(level))) {
                    Log.d("SAME_COMMAND", "Same command detected: " + levelMap.get(level) + " with value: " + event.values[1]);
                    return;
                } else {
                    lastCommand = levelMap.get(level);
                }

                try {
                    MQTTHelper.publishMessage(getApplicationContext(), client, levelMap.get(level), 2, topic, diffTime);
                } catch (MqttException | UnsupportedEncodingException e) {
                    MQTTHelper.disconnect(client);
                    Log.e(TAG, e.getMessage(), e);
                }

                Log.d("COMMAND", "Command sent: " + levelMap.get(level));
                break;
        }
    }

    private LevelEnum getLevel(float value) {
        if (value > 50) {
            return LevelEnum.CLOSE;
        } else {
            return LevelEnum.FAR;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d("sensor", sensor.getName() + ", accuracy changed to " + accuracy);
    }
}


