package ha.joaobrito.pt.ha;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import ha.joaobrito.pt.ha.helper.SingletonHelper;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = SettingsActivity.class.getName();

    private static final String RESET = "reset";
    private static final String CLEAR = "clear";

    private String option;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = SingletonHelper.getInstance(this).getPrefs();

        Intent i = getIntent();
        this.option = i.getStringExtra(MainActivity.SETTINGS);


        Log.d("PREFS", prefs.getString(SingletonHelper.OPENCV_HOST, "null"));
        ((EditText) findViewById(R.id.settings_opencv_host)).setText(prefs.getString(SingletonHelper.OPENCV_HOST, ""));
        ((EditText) findViewById(R.id.settings_mosquito_url)).setText(prefs.getString(SingletonHelper.MOSQUITO_URL, ""));
        ((EditText) findViewById(R.id.settings_mosquito_username)).setText(prefs.getString(SingletonHelper.MOSQUITO_USERNAME, ""));
        ((EditText) findViewById(R.id.settings_mosquito_password)).setText(prefs.getString(SingletonHelper.MOSQUITO_PASSWORD, ""));
        ((EditText) findViewById(R.id.settings_influxdb_url)).setText(prefs.getString(SingletonHelper.INFLUXDB_URL, ""));
        ((EditText) findViewById(R.id.settings_influxdb_database)).setText(prefs.getString(SingletonHelper.INFLUXDB_DATABASE, ""));
        ((EditText) findViewById(R.id.settings_ntp_server_host)).setText(prefs.getString(SingletonHelper.NTP_SERVER_HOST, ""));

        findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // save preferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(SingletonHelper.OPENCV_HOST, ((EditText) findViewById(R.id.settings_opencv_host)).getText().toString());
                editor.putString(SingletonHelper.MOSQUITO_URL, ((EditText) findViewById(R.id.settings_mosquito_url)).getText().toString());
                editor.putString(SingletonHelper.MOSQUITO_USERNAME, ((EditText) findViewById(R.id.settings_mosquito_username)).getText().toString());
                editor.putString(SingletonHelper.MOSQUITO_PASSWORD, ((EditText) findViewById(R.id.settings_mosquito_password)).getText().toString());
                editor.putString(SingletonHelper.INFLUXDB_URL, ((EditText) findViewById(R.id.settings_influxdb_url)).getText().toString());
                editor.putString(SingletonHelper.INFLUXDB_DATABASE, ((EditText) findViewById(R.id.settings_influxdb_database)).getText().toString());
                editor.putString(SingletonHelper.NTP_SERVER_HOST, ((EditText) findViewById(R.id.settings_ntp_server_host)).getText().toString());
                editor.apply();

                new AsyncTask<Void, Void, Boolean>() {

                    @Override
                    protected Boolean doInBackground(Void... voids) {
                        return true; // FIXME: TODO: check host and ports settings
                    }

                    @Override
                    protected void onPostExecute(Boolean aBoolean) {
                        if (aBoolean) {
                            Intent i = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(i);
                            finish();
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                            builder.setMessage("Please fix the wrong host names and ports")
                                    .setCancelable(true)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            SharedPreferences.Editor editor = prefs.edit();
                                            editor.clear();
                                            editor.apply();
                                        }
                                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // do nothing
                                }
                            });
                            AlertDialog alert = builder.create();
                            alert.show();

                        }
                    }
                }.execute();

            }
        });

        findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.load).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("PREFS", prefs.getString(SingletonHelper.OPENCV_HOST, "null"));
                ((EditText) findViewById(R.id.settings_opencv_host)).setText(prefs.getString(SingletonHelper.OPENCV_HOST, getApplicationContext().getResources().getString(R.string.opencv_server_host)));
                ((EditText) findViewById(R.id.settings_mosquito_url)).setText(prefs.getString(SingletonHelper.MOSQUITO_URL, getApplicationContext().getResources().getString(R.string.mosquito_server_url)));
                ((EditText) findViewById(R.id.settings_mosquito_username)).setText(prefs.getString(SingletonHelper.MOSQUITO_USERNAME, getApplicationContext().getResources().getString(R.string.mosquito_username)));
                ((EditText) findViewById(R.id.settings_mosquito_password)).setText(prefs.getString(SingletonHelper.MOSQUITO_PASSWORD, getApplicationContext().getResources().getString(R.string.mosquito_password)));
                ((EditText) findViewById(R.id.settings_influxdb_url)).setText(prefs.getString(SingletonHelper.INFLUXDB_URL, getApplicationContext().getResources().getString(R.string.influxdb_url)));
                ((EditText) findViewById(R.id.settings_influxdb_database)).setText(prefs.getString(SingletonHelper.INFLUXDB_DATABASE, getApplicationContext().getResources().getString(R.string.influxdb_database)));
                ((EditText) findViewById(R.id.settings_ntp_server_host)).setText(prefs.getString(SingletonHelper.NTP_SERVER_HOST, getApplicationContext().getResources().getString(R.string.ntp_server_host)));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (option == null) {
            return;
        }

        switch (option) {
            case RESET:
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                editor.apply();

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
                break;
            case CLEAR:
                // save preferences
                ((EditText) findViewById(R.id.settings_opencv_host)).setText("");
                ((EditText) findViewById(R.id.settings_mosquito_url)).setText("");
                ((EditText) findViewById(R.id.settings_mosquito_username)).setText("");
                ((EditText) findViewById(R.id.settings_mosquito_password)).setText("");
                ((EditText) findViewById(R.id.settings_influxdb_url)).setText("");
                ((EditText) findViewById(R.id.settings_influxdb_database)).setText("");
                ((EditText) findViewById(R.id.settings_ntp_server_host)).setText("");
                break;
            default:
                Log.e(TAG, "No option recognized");
        }
    }
}
