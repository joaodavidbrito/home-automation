package ha.joaobrito.pt.ha.helper;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;

import ha.joaobrito.pt.ha.MainActivity;
import ha.joaobrito.pt.ha.Findctivity;

public class TimeSyncTask extends AsyncTask<Void, Void, Long> {

    private static final String TAG = TimeSyncTask.class.getName();
    private final String host;
    private  MainActivity ctx;
    private Findctivity videoCtx;
    private Object lock1 = new Object();
    private Object lock2 = new Object();


    public TimeSyncTask(MainActivity context){
        this.ctx = context;
        this.host = SingletonHelper.getInstance(context).getNtpServerHost();
    }

    public TimeSyncTask(Findctivity context){
        this.videoCtx = context;
        this.host = SingletonHelper.getInstance(context).getNtpServerHost();
    }

    @Override
    protected Long doInBackground(Void... data) {
        synchronized (lock1) {
            String ntpHost = this.host;
            NTPUDPClient ntpUdpClient = new NTPUDPClient();
            // We want to timeout if a response takes longer than 2 seconds
            ntpUdpClient.setDefaultTimeout(2000);
            try {
                ntpUdpClient.open();
                try {
                    InetAddress hostAddr = InetAddress.getByName(ntpHost);
                    TimeInfo timeInfo = ntpUdpClient.getTime(hostAddr);
                    return processResponse(timeInfo);
                } catch (IOException ioe) {
                    Log.e(TAG, ioe.getMessage(), ioe);
                }
            } catch (SocketException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        return null;
    }

    private Long processResponse(TimeInfo info) {
        info.computeDetails(); // compute offset/delay if not already done
        Long offsetValue = info.getOffset();
        return offsetValue;
    }

    @Override
    protected void onPostExecute(Long offsetValue) {
        synchronized (lock2) {
            if (ctx != null) {
                if (offsetValue == null) {
                    ctx.setOffsetTime(0L);
                }
                Log.d("NTP offset", String.valueOf(offsetValue));
                ctx.setOffsetTime(offsetValue);

                Toast toast = Toast.makeText(ctx, "offset time is " + offsetValue + "ms", Toast.LENGTH_SHORT);
                toast.show();
            } else if (this.videoCtx != null) {

                if (offsetValue == null) {
                    videoCtx.setOffsetTime(0L);
                }
                Log.d("NTP offset", String.valueOf(offsetValue));
                try{
                    videoCtx.setOffsetTime(offsetValue);
                }catch (Exception e){
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        }
    }
}
