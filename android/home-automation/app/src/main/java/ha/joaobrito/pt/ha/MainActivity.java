package ha.joaobrito.pt.ha;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import org.eclipse.paho.android.service.MqttAndroidClient;

import ha.joaobrito.pt.ha.helper.MQTTHelper;
import ha.joaobrito.pt.ha.helper.SingletonHelper;
import ha.joaobrito.pt.ha.helper.TimeSyncTask;

import static ha.joaobrito.pt.ha.Findctivity.OFFSET_TIME;

public class MainActivity extends AppCompatActivity {

    public static final String SETTINGS = "SETTINGS";
    public static final String ANDROID_START_TIMESTAMP = "TIMESTAMP";
    private static final String RESET_EXTRA = "reset";
    private static final String CLEAR_EXTRA = "clear";
    public static final String SCENE_DESC = "scene_desc";

    private SharedPreferences prefs;
    private Long offsetTime;
    private MqttAndroidClient client;
    private ConnectivityManager connectionManager;
    private NetworkInfo wifiCheck;

    public void setOffsetTime(Long offsetTime) {
        this.offsetTime = offsetTime;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        prefs = SingletonHelper.getInstance(this).getPrefs();
        ((EditText)findViewById(R.id.scene)).setText(prefs.getString(SingletonHelper.SCENE_DESC, ""));


        connectionManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        wifiCheck = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);


        // save image: metadata activity -> camera activity
        findViewById(R.id.saveObj).setOnClickListener(v -> {
            if (!wifiCheck.isConnected()) {
                return;
            }
            Intent i = new Intent(getApplicationContext(), MetaDataActivity.class);
            startActivity(i);
        });

        // find images: video activity
        findViewById(R.id.findObj).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!wifiCheck.isConnected()) {
                    return;
                }
                Intent i = new Intent(getApplicationContext(), Findctivity.class);
                i.putExtra(ANDROID_START_TIMESTAMP, System.currentTimeMillis());
                i.putExtra(OFFSET_TIME, offsetTime);
                i.putExtra(SCENE_DESC, ((EditText)findViewById(R.id.scene)).getText().toString());
                startActivity(i);
            }
        });

        // print
        findViewById(R.id.printbtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!wifiCheck.isConnected()) {
                    return;
                }
                Intent i = new Intent(getApplicationContext(), PrintActivity.class);
                startActivity(i);
            }
        });

        this.client = MQTTHelper.getMqttClient(getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();

        wifiCheck = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiCheck.isConnected()) {
            new TimeSyncTask(this).execute();
        }else{
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("No wifi connection detected!")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //do things
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }

        if(!SingletonHelper.getInstance(this).getPrefs().contains(SingletonHelper.OPENCV_HOST)){
            Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(i);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(SingletonHelper.SCENE_DESC, ((EditText)findViewById(R.id.scene)).getText().toString());
        editor.apply();
        MQTTHelper.disconnect(client);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(i);
            return true;
        }else if(id == R.id.action_settings_reset) {
            Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
            i.putExtra(SETTINGS, RESET_EXTRA);
            startActivity(i);
            return true;
        }else if(id == R.id.action_settings_clear) {
            Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
            i.putExtra(SETTINGS, CLEAR_EXTRA);
            startActivity(i);
            return true;
        }
        else if(id == R.id.action_settings_reload) {
            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            finish();
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
