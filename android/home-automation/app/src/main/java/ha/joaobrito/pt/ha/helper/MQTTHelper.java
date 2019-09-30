package ha.joaobrito.pt.ha.helper;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

import ha.joaobrito.pt.ha.domain.ImagesDto;
import ha.joaobrito.pt.ha.domain.LogEvent;

public class MQTTHelper {

    public static final String TAG = MqttAndroidClient.class.getName();

    public static MqttAndroidClient client;

    public static MqttAndroidClient getMqttClient(Context context) {
        String url = SingletonHelper.getInstance(context).getMosquitoUrl();

        MqttAndroidClient mqttAndroidClient = new MqttAndroidClient(context, "tcp://" + url, MqttClient.generateClientId());
        try {
            IMqttToken token = mqttAndroidClient.connect(getMqttConnectionOption(context));
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    mqttAndroidClient.setBufferOpts(getDisconnectedBufferOptions());
                    Log.d(TAG, "Success");
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable e) {
                    Log.d(TAG, "Failure " + e.toString());
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        client = mqttAndroidClient;
        return mqttAndroidClient;
    }

    private static MqttConnectOptions getMqttConnectionOption(Context context){
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(false);
        options.setAutomaticReconnect(true);

        String username = SingletonHelper.getInstance(context).getMosquitoUsername();
        String password = SingletonHelper.getInstance(context).getMosquitoPassword();

        options.setUserName(username);
        options.setPassword(password.toCharArray());
        return options;
    }

    private static DisconnectedBufferOptions getDisconnectedBufferOptions(){
        DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(true);
        bufferOptions.setBufferSize(100);
        bufferOptions.setPersistBuffer(true);
        bufferOptions.setDeleteOldestMessages(false);
        return bufferOptions;
    }

    public static void publishMessage(Context context, @NonNull MqttAndroidClient client, @NonNull String msg, int qos, @NonNull String topic, Long diffTime) throws MqttException, UnsupportedEncodingException {
        byte[] encodedPayload;
        encodedPayload = msg.getBytes("UTF-8");
        MqttMessage message = new MqttMessage(encodedPayload);
        message.setRetained(true);
        message.setQos(qos);
        client.publish(topic, message);
        saveLog(context, diffTime);
    }

    private static void saveLog(Context context, Long diffTime) {
        ImagesDto dto = new ImagesDto();
        LogEvent e = null;
        e = new LogEvent(System.currentTimeMillis() + diffTime, LogEvent.EventType.ANDROID_MQTT_PUBLISH);
        dto.getEvents().add(e);
        new LogTask(context).execute(dto);
    }

    public static void disconnect(MqttAndroidClient mqttAndroidClient) {
        try {
            mqttAndroidClient.disconnect().setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are disconnected
                    Log.i(TAG, "We are disconnected");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.e(TAG, "Something went wrong e.g. connection timeout or firewall problems");
                }
            });
        } catch (MqttException ex) {
            Log.e(TAG, ex.getMessage());
        }
    }
}
