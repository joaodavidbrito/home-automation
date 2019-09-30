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

import java.io.UnsupportedEncodingException;

import ha.joaobrito.pt.ha.domain.ImagesDto;
import ha.joaobrito.pt.ha.domain.LogEvent;
import ha.joaobrito.pt.ha.helper.LogTask;
import ha.joaobrito.pt.ha.helper.MQTTHelper;


public class SensorRotationActivity extends Activity implements SensorEventListener{

    public static final String TAG = SensorRotationActivity.class.getName();

    private String topic = "hue/lamp1";

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Sensor mSensorAccelerometer;
    private long lastTimestamp;
    private int command = 0;
    private int lastCommand = 0;

    // accelerometer
    private static final float SHAKE_THRESHOLD = 3.25f; // m/S**2
    private static final float TAP_THRESHOLD = 3.25f; // m/S**2
    private static final int MIN_TIME_BETWEEN_SHAKES_MILLISECS = 1000;
    private long mLastShakeTime;

    private MqttAndroidClient client = MQTTHelper.client;
    private Long diffTime;

    private double acceleration;
    private boolean proceed;
    private boolean close;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rotation_sensor);
        lastTimestamp = 0;

        mSensorManager = (SensorManager) getSystemService(getApplicationContext().SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        // Listen for shakes
        mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        this.topic = getIntent().getStringExtra(Findctivity.TOPIC);
        this.diffTime = getIntent().getLongExtra(Findctivity.OFFSET_TIME, 0L);

        Log.d(TAG, "difftime timestamp: " + diffTime);

        // image
        byte[] img = getIntent().getByteArrayExtra(Findctivity.IMAGE);
        Bitmap bMap = BitmapFactory.decodeByteArray(img, 0, img.length);
        ImageView imageView = findViewById(R.id.imageRot);
        imageView.setImageBitmap(bMap);
        imageView.setRotation(90f);
    }

    @Override
    protected void onResume() {
        // Register a listener for the sensor.
        super.onResume();

        if (mSensor != null) {
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (mSensorAccelerometer != null) {
            mSensorManager.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        // Be sure to unregister the sensor when the activity pauses.
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
                if ((curTime - mLastShakeTime) > MIN_TIME_BETWEEN_SHAKES_MILLISECS) {

                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    acceleration = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) /*+ Math.pow(z, 2)*/) - SensorManager.GRAVITY_EARTH;
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

                        }else {
                            Log.i(TAG, "Shake, Rattle, and Roll");
                            Intent i = new Intent(getApplicationContext(), MainActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(i);
                            finish();
                        }

                    }
                }
                break;
            case Sensor.TYPE_ROTATION_VECTOR:

                if (acceleration > SHAKE_THRESHOLD) {
                    return;
                }else{
                    if(!proceed){
                        return;
                    }
                }

                if(event.timestamp - lastTimestamp < 6e8){
                    return;
                }
                lastTimestamp = event.timestamp;

                float[] rotationMatrix = new float[16];
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

                // Remap coordinate system
                float[] remappedRotationMatrix = new float[16];
                SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, remappedRotationMatrix);

                // Convert to orientations
                float[] orientations = new float[3];
                SensorManager.getOrientation(remappedRotationMatrix, orientations);

                for(int i = 0; i < 3; i++) {
                    orientations[i] = (float)(Math.toDegrees(orientations[i]));
                }

                Log.i("rotation", String.valueOf(orientations[2]));
                command = getCommand(orientations[2]);

                if(Math.abs(lastCommand - command) < 5 && (command < 5 || command > 85)){
                    return;
                }
//                helper.connect(getApplicationContext(), topic, String.valueOf(command), diffTime);
                try {
                    MQTTHelper.publishMessage(getApplicationContext(), client, String.valueOf(command), 2, topic, diffTime);
                } catch (MqttException | UnsupportedEncodingException e) {
                    MQTTHelper.disconnect(client);
                    Log.e(TAG, e.getMessage(), e);
                }
                lastCommand = command;
                break;
        }
    }

    private int getCommand(float orientation) {
        if(Math.abs(orientation) < 5){
            command = 0;
        }else if(Math.abs(orientation) > 85){
            command = 100;
        }else{
            command = Math.abs((int) orientation * 100 / 90);
        }
        Log.i(TAG, "command sent: " + command);
        return command;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, sensor.getName() + ", accuracy changed to " + accuracy);
    }
}


