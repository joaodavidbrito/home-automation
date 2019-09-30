package ha.joaobrito.pt.ha;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.SerializableEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import ha.joaobrito.pt.ha.domain.ImagesDto;
import ha.joaobrito.pt.ha.helper.SingletonHelper;

import static ha.joaobrito.pt.ha.MetaDataActivity.DESC_EXTRA;
import static ha.joaobrito.pt.ha.MetaDataActivity.NAME_EXTRA;
import static ha.joaobrito.pt.ha.MetaDataActivity.TYPE_EXTRA;


public class SaveActivity extends Activity {

    private Camera mCamera;
    private CameraPreview mPreview;
    private Button bt;
    private String imageName;
    private String type;
    private String desc;

    private Camera.PictureCallback mPicture = (byte[] data, Camera camera) -> {

        // send pic to the server
        ImagesDto image = new ImagesDto();
        image.setEncodedImage(data);
        image.setName(imageName);
        image.setAction(type);
        image.setObjectDesc(desc);
        new SaveTask().execute(image);
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_preview_layout);

        Intent i = getIntent();
        this.imageName = i.getStringExtra(NAME_EXTRA);
        this.type = i.getStringExtra(TYPE_EXTRA);
        this.desc = i.getStringExtra(DESC_EXTRA);

        // Create an instance of Camera
        mCamera = getCameraInstance();

        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        // auto-focus
        Camera.Parameters params = mCamera.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        params.setPictureFormat(ImageFormat.JPEG);

        params.setPreviewSize(640, 480);
        params.setPictureSize(640, 480);
        mCamera.setParameters(params);

        // Create our Preview view and set it as the content of our activity.

        bt = findViewById(R.id.button_capture);
        bt.setOnClickListener(v -> {
            mCamera.takePicture(null, null, mPicture);
            finish();
        });
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    private Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            Log.d(null, "Error setting camera preview: " + e.getMessage());
        }
        return c; // returns null if camera is unavailable
    }

    private final class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;
        private Context ctx;

        public CameraPreview(Context context, Camera camera) {
            super(context);
            this.ctx = context;
            mCamera = camera;

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.setDisplayOrientation(90);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.d(ContentValues.TAG, "Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            if (mHolder.getSurface() == null) {
                return;
            }

            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                Log.e("", e.getMessage());
            }

            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();

            } catch (Exception e) {
                Log.d(ContentValues.TAG, "Error starting camera preview: " + e.getMessage());
            }
        }
    }

    private final class SaveTask extends AsyncTask<ImagesDto, String, String> {
        private static final String SAVE_ENDPOINT = "/images/save";

        @Override
        protected String doInBackground(ImagesDto... image) {

            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, 2000);
            HttpConnectionParams.setSoTimeout(params, 500);
            HttpClient httpclient = new DefaultHttpClient(params);

            HttpResponse response;
            String responseString = null;
            try {
                String serverHost = SingletonHelper.getInstance(SaveActivity.this).getOpencvHost();
                HttpPost httpPost = new HttpPost("http://" + serverHost + SAVE_ENDPOINT);

                //add data
                httpPost.setEntity(new SerializableEntity(image[0], true));

                response = httpclient.execute(httpPost);

                StatusLine statusLine = response.getStatusLine();
                Log.i("status", statusLine.toString());
                if (statusLine.getStatusCode() == HttpStatus.SC_CREATED) {
                    try(ByteArrayOutputStream out = new ByteArrayOutputStream();){
                        response.getEntity().writeTo(out);
                        responseString = out.toString();
                    }
                } else {
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            } catch (ClientProtocolException e) {
                Log.d(null, e.getMessage());
            } catch (IOException e) {
                Log.d(null, e.getMessage());
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                Log.d(null, s);
                if (s.startsWith("updated rows")) {
                    return;
                }
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
