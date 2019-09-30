package ha.joaobrito.pt.ha;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import ha.joaobrito.pt.ha.helper.SingletonHelper;

public class MetaDataActivity extends AppCompatActivity {

    public static final String NAME_EXTRA = "name";
    public static final String TYPE_EXTRA = "type";
    public static final String DESC_EXTRA = "desc";

    private static final String IMAGE_NAME = "image_name";
    private static final String SPINNER = "spinner";
    private static final String DESCRIPTION = "desc";
    private static final String DEFAULT_TOPIC = "tabua/lamp";


    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_meta_data);

        this.prefs = this.getPreferences(MetaDataActivity.MODE_PRIVATE);

        Spinner spinner = findViewById(R.id.spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.sensors, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        // load preferences
        ((EditText) findViewById(R.id.image_name)).setText(prefs.getString(IMAGE_NAME, DEFAULT_TOPIC));
        ((Spinner) findViewById(R.id.spinner)).setSelection(prefs.getInt(SPINNER, 0));
        ((EditText) findViewById(R.id.desc)).setText(prefs.getString(DESCRIPTION, ""));

        // scan: camera activity
        findViewById(R.id.scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // save preferences
                SharedPreferences.Editor editor = prefs.edit();
                String imageName = ((EditText) findViewById(R.id.image_name)).getText().toString();
                int spinner = ((Spinner) findViewById(R.id.spinner)).getSelectedItemPosition();
                String desc = ((EditText) findViewById(R.id.desc)).getText().toString();

                editor.putString(IMAGE_NAME, imageName);
                editor.putInt(SPINNER, spinner);
                editor.putString(DESCRIPTION, desc);
                editor.apply();

                Intent i = new Intent(getApplicationContext(), SaveActivity.class);
                i.putExtra(NAME_EXTRA, imageName);
                i.putExtra(TYPE_EXTRA, ((Spinner) findViewById(R.id.spinner)).getSelectedItem().toString());
                i.putExtra(DESC_EXTRA, desc);
                startActivity(i);
                finish();
            }
        });

        new NetworkTask().execute();
    }

    private final class NetworkTask extends AsyncTask<Void, Void, String> {



        @Override
        protected String doInBackground(Void... v) {
            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, 200);
            HttpConnectionParams.setSoTimeout(params, 200);
            HttpClient httpclient = new DefaultHttpClient(params);

            HttpOptions options = new HttpOptions("http://" + SingletonHelper.getInstance(MetaDataActivity.this).getOpencvHost() + SingletonHelper.HELLO_ENDPOINT);
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
                Toast.makeText(MetaDataActivity.this, "The server can not be reached. Please retry...", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }
    }


}
