package ha.joaobrito.pt.ha;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import ha.joaobrito.pt.ha.domain.ImagesDto;
import ha.joaobrito.pt.ha.domain.LogEvent;
import ha.joaobrito.pt.ha.helper.LogTask;
import ha.joaobrito.pt.ha.helper.SingletonHelper;
import ha.joaobrito.pt.ha.helper.TimeSyncTask;


public class Findctivity extends Activity implements SensorEventListener {

    private static final String TAG = Findctivity.class.getName();
    public static final String TOPIC = "topic";
    public static final String IMAGE = "image";
    public static final String OFFSET_TIME = "offsetTime";
    private static final String PROXIMITY = "PROXIMITY";
    private static final String ROTATION = "ROTATION";
    private static final int LIMIT = 50;

    private long firstShot = 0L;

    private Camera mCamera;
    private CameraPreview mPreview;
    private SurfaceHolder mHolder;
    private boolean safe = true;
    private ImagesDto img;

    private long offsetTime = 0L;

    public void setOffsetTime(long offsetTime) {
        this.offsetTime = offsetTime;
    }

    private boolean userFinishLoop;

    private SensorManager mSensorManager;
    private Sensor mSensorAccelerometer;

    // accelerometer
    private static final float SHAKE_THRESHOLD = 3.25f; // m/S**2
    private static final int MIN_TIME_BETWEEN_SHAKES_MILLI_SECS = 1000;
    private long mLastShakeTime;
    private String sceneDesc;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_preview_layout);

        new NetworkTask().execute();

        sceneDesc = getIntent().getStringExtra(MainActivity.SCENE_DESC);
        if (sceneDesc == null || sceneDesc.isEmpty()) {
            sceneDesc = "NA";
        }
        firstShot = getIntent().getLongExtra(MainActivity.ANDROID_START_TIMESTAMP, System.currentTimeMillis());
        offsetTime = getIntent().getLongExtra(OFFSET_TIME, 0L);
        img = new ImagesDto();
        img.setSceneDesc(sceneDesc);
        img.getEvents().add(new LogEvent(firstShot + offsetTime, LogEvent.EventType.ANDROID_START));

        // Create an instance of Camera
        mCamera = getCameraInstance();

        Camera.Parameters params = mCamera.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        params.setPreviewSize(640, 480);
        params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        params.setAutoExposureLock(false);
        params.setAutoWhiteBalanceLock(false);
        mCamera.setParameters(params);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout layoutPreview = findViewById(R.id.camera_preview);
        layoutPreview.addView(mPreview);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
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
        this.img = null;
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    private Camera getCameraInstance() {
        Camera cam = null;
        try {
            cam = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK); // attempt to get a Camera instance
        } catch (Exception e) {
            Log.e(TAG, "Error setting camera preview: " + e.getMessage());
        }
        return cam; // returns null if camera is unavailable
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (userFinishLoop) {
            return;
        }

        long curTime = System.currentTimeMillis();
        if ((curTime - mLastShakeTime) > MIN_TIME_BETWEEN_SHAKES_MILLI_SECS) {

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            double acceleration = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) /*+ Math.pow(z, 2)*/) - SensorManager.GRAVITY_EARTH;

            if (acceleration > SHAKE_THRESHOLD) {
                this.userFinishLoop = true;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "Sensor Accuracy changed");
    }

    private class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
        private final static String TAG = "CameraPreview";

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.setPreviewCallback(this);
                mCamera.setDisplayOrientation(90);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.d(ContentValues.TAG, "Error setting camera preview: " + e.getMessage());
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            if (mHolder.getSurface() == null) {
                return;
            }

            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.setDisplayOrientation(90);
                mCamera.startPreview();
            } catch (Exception e) {
                Log.d(ContentValues.TAG, "Error starting camera preview: " + e.getMessage());
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (!safe) {
                return;
            }
            safe = false;
            mPreview.invalidate();

            if ((System.currentTimeMillis()) - firstShot < 1000) {
                safe = true;
                return;
            }
            firstShot = 0;

            Camera.Size size = mCamera.getParameters().getPreviewSize();
            YuvImage im = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
            Rect rect = new Rect(0, 0, im.getWidth(), im.getHeight());
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();) {
                im.compressToJpeg(rect, 100, baos);
                new FindTask().execute(baos.toByteArray()); // send data do server
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    private final class FindTask extends AsyncTask<byte[], String, ImagesDto> {

        private static final String FIND_ENDPOINT = "/images/find";
        private DefaultHttpClient httpclient;

        @Override
        protected ImagesDto doInBackground(byte[]... data) {

            // http params
            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, 4000);
            HttpConnectionParams.setSoTimeout(params, 500);
            HttpConnectionParams.setTcpNoDelay(params, true);
            this.httpclient = new DefaultHttpClient(params);

            HttpResponse response = null;
            ObjectInputStream ois = null;
            try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(out)) {

                HttpPost httpPost = new HttpPost("http://" + SingletonHelper.getInstance(Findctivity.this).getOpencvHost() + FIND_ENDPOINT);

                //add data
                img.setEncodedImage(data[0]);
                if (img.getNotFounds() == 0) {
                    img.getEvents().add(new LogEvent(System.currentTimeMillis() + Findctivity.this.offsetTime, LogEvent.EventType.ANDROID_SEND));
                } else {
                    img.getEvents().add(new LogEvent(System.currentTimeMillis() + Findctivity.this.offsetTime, LogEvent.EventType.ANDROID_RESEND));
                }
                oos.writeObject(img);
                httpPost.setEntity(new ByteArrayEntity(out.toByteArray()));

                response = httpclient.execute(httpPost);

                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    Log.d(TAG, "200 OK - Image was Found");
                    ois = new ObjectInputStream(response.getEntity().getContent());
                    ImagesDto dto = (ImagesDto) ois.readObject();
                    return dto;

                } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                    Log.d(TAG, "404 Image not Found");
                    ois = new ObjectInputStream(response.getEntity().getContent());
                    ImagesDto dto = (ImagesDto) ois.readObject();
                    return dto;
                } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                    Log.e(TAG, "500 Internal Server Error occurred");
                    ois = new ObjectInputStream(response.getEntity().getContent());
                    ImagesDto dto = (ImagesDto) ois.readObject();
                    return dto;
                } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
                    Log.e(TAG, "400 Bad Request with cause: " + response.getEntity().getContent().toString());
                    ImagesDto i = new ImagesDto();
                    i.setSceneDesc(sceneDesc);
                    return i;
                } else {
                    Log.e(TAG, "Unknown error occurred");
                    ImagesDto i = new ImagesDto();
                    i.setSceneDesc(sceneDesc);
                    return i;
                }
            } catch (ClientProtocolException e) {
                Log.e(TAG, e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } catch (ClassNotFoundException e) {
                Log.e(TAG, e.getMessage());
            } finally {
                try {
                    ois.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
            return null;
        }


        @Override
        protected void onPostExecute(ImagesDto logDto) {
            super.onPostExecute(logDto);

            if (logDto != null && (logDto.getNotFounds() >= LIMIT || userFinishLoop)) {
                if (logDto.getNotFounds() >= LIMIT) {
                    logDto.getEvents().add(new LogEvent(System.currentTimeMillis() + Findctivity.this.offsetTime, LogEvent.EventType.ANDROID_LIMIT_REACHED));
                    Toast toast = Toast.makeText(getApplicationContext(), "Unable to find the object", Toast.LENGTH_SHORT);
                    toast.show();
                } else if (userFinishLoop) {
                    logDto.getEvents().add(new LogEvent(System.currentTimeMillis() + Findctivity.this.offsetTime, LogEvent.EventType.ANDROID_USER_ABORTED));
                    Toast toast = Toast.makeText(getApplicationContext(), "User aborted", Toast.LENGTH_SHORT);
                    toast.show();
                }
                logDto.getEvents().add(new LogEvent(System.currentTimeMillis() + Findctivity.this.offsetTime, LogEvent.EventType.ANDROID_END));
                // save logs into influxdb
                new LogTask(Findctivity.this).execute(logDto);

                // go back to main activity
                finish();
            } else if (logDto != null && logDto.getEncodedImage() != null) {
                Toast toast = Toast.makeText(getApplicationContext(), logDto.getName(), Toast.LENGTH_SHORT);
                toast.show();
                String type = logDto.getAction();

                Intent i;
                switch (type) {
                    case Findctivity.PROXIMITY:
                        i = new Intent(getApplicationContext(), SensorProximityActivity.class);
                        break;
                    case Findctivity.ROTATION:
                        i = new Intent(getApplicationContext(), SensorRotationActivity.class);
                        break;
                    default:
                        Log.e("Findctivity", "No sensor type detected");
                        mCamera.startPreview();
                        safe = true;
                        img = logDto;
                        return;
                }
                logDto.getEvents().add(new LogEvent(System.currentTimeMillis() + Findctivity.this.offsetTime, LogEvent.EventType.ANDROID_END));

                Log.d("timestamp after AE", "" + (System.currentTimeMillis() + offsetTime));
                // save logs into influxdb
                new LogTask(Findctivity.this).execute(logDto);

                i.putExtra(TOPIC, logDto.getName());
                i.putExtra(IMAGE, logDto.getEncodedImage());
                i.putExtra(OFFSET_TIME, Findctivity.this.offsetTime);
                startActivity(i);
                finish();
                Log.d("timestamp before sensor", "" + (System.currentTimeMillis() + offsetTime));
            } else {
                new TimeSyncTask(Findctivity.this).execute();
                if (mCamera == null) {
                    return;
                }
                mCamera.startPreview();
                logDto.getEvents().add(new LogEvent(System.currentTimeMillis() + Findctivity.this.offsetTime, LogEvent.EventType.ANDROID_USER_NOT_ABORTED));
                img = logDto;
                safe = true;
            }
        }
    }

    private final class NetworkTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... v) {
            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, 200);
            HttpConnectionParams.setSoTimeout(params, 200);
            HttpClient httpclient = new DefaultHttpClient(params);

            HttpOptions options = new HttpOptions("http://" + SingletonHelper.getInstance(Findctivity.this).getOpencvHost() + SingletonHelper.HELLO_ENDPOINT);
            try {
                httpclient.execute(options);
            } catch (Exception e) {
                return null;
            }
            return "hello";
        }

        @Override
        protected void onPostExecute(String res) {
            super.onPostExecute(res);
            if (res == null) {
                Toast.makeText(Findctivity.this, "The server can not be reached. Please retry...", Toast.LENGTH_SHORT).show();
                Findctivity.this.finish();
            }
        }
    }
}


