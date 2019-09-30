package ha.joaobrito.pt.ha.helper;

import android.content.Context;
import android.content.SharedPreferences;

import ha.joaobrito.pt.ha.R;

public class SingletonHelper {

    public static final String HELLO_ENDPOINT = "/images/hello";
    public static final String SETTINGS = "SETTINGS";
    public static final String OPENCV_HOST = "OPENCV_HOST";
    public static final String OPENCV_PORT = "OPENCV_PORT";
    public static final String MOSQUITO_URL = "MOSQUITO_URL";
    public static final String MOSQUITO_USERNAME = "MOSQUITO_USERNAME";
    public static final String MOSQUITO_PASSWORD = "MOSQUITO_PASSWORD";
    public static final String INFLUXDB_URL = "INFLUXDB_URL";
    public static final String INFLUXDB_DATABASE = "INFLUXDB_DATABASE";
    public static final String NTP_SERVER_HOST = "NTP_SERVER_HOST";
    public static final String SCENE_DESC = "SCENE_DESC";
    private static SingletonHelper instance = null;

    private final String opencvHost;
    private final String mosquitoUrl;
    private final String mosquitoUsername;
    private final String mosquitoPassword;
    private final String influxdbUrl;
    private final String influxdbDatabase;
    private final String ntpServerHost;
    private final SharedPreferences prefs;
    private final Context ctx;


    private SingletonHelper(Context ctx) {
        this.ctx = ctx;
        SharedPreferences prefs = ctx.getSharedPreferences(SETTINGS, ctx.MODE_PRIVATE);
        this.prefs = prefs;
        this.opencvHost = prefs.getString(OPENCV_HOST, ctx.getResources().getString(R.string.opencv_server_host));
        this.mosquitoUrl = prefs.getString(MOSQUITO_URL, ctx.getResources().getString(R.string.mosquito_server_url));
        this.mosquitoUsername = prefs.getString(MOSQUITO_USERNAME, ctx.getResources().getString(R.string.mosquito_username));
        this.mosquitoPassword = prefs.getString(MOSQUITO_PASSWORD, ctx.getResources().getString(R.string.mosquito_password));
        this.influxdbUrl = prefs.getString(INFLUXDB_URL, ctx.getResources().getString(R.string.influxdb_url));
        this.influxdbDatabase = prefs.getString(INFLUXDB_DATABASE, ctx.getResources().getString(R.string.influxdb_database));
        this.ntpServerHost = prefs.getString(NTP_SERVER_HOST, ctx.getResources().getString(R.string.ntp_server_host));
    }

    public static SingletonHelper getInstance(Context context) {
        if (instance == null) {
            instance = new SingletonHelper(context);
        }
        return instance;
    }

    public String getOpencvHost() {
        return opencvHost;
    }

    public String getMosquitoUrl() {
        return mosquitoUrl;
    }

    public String getMosquitoUsername() {
        return mosquitoUsername;
    }

    public String getMosquitoPassword() {
        return mosquitoPassword;
    }

    public String getInfluxdbUrl() {
        return influxdbUrl;
    }

    public String getInfluxdbDatabase() {
        return influxdbDatabase;
    }

    public String getNtpServerHost() {
        return ntpServerHost;
    }

    public SharedPreferences getPrefs() {
        return prefs;
    }
}
