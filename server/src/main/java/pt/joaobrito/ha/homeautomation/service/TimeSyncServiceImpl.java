package pt.joaobrito.ha.homeautomation.service;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;

@Service
public class TimeSyncServiceImpl implements TimeSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(TimeSyncServiceImpl.class);

    @Value(value = "${time.ntpServerHost}")
    private String ntpServerHost;

    @Override
    public Long update() {
        Long offsetTime = null;
        LOG.info("Time sync with NTP server: {}", ntpServerHost);
        String ntpHost = ntpServerHost; //ctx.getResources().getString(R.string.ntp_server_host);
        NTPUDPClient ntpUdpClient = new NTPUDPClient();

        // We want to timeout if a response takes longer than 2 seconds
        int ntpTimeout = 2000;
        ntpUdpClient.setDefaultTimeout(ntpTimeout);
        try {
            ntpUdpClient.open();
            try {
                InetAddress hostAddr = InetAddress.getByName(ntpHost);
                TimeInfo timeInfo = ntpUdpClient.getTime(hostAddr);
                timeInfo.computeDetails();
                Long offsetValue = timeInfo.getOffset();
                if (offsetValue == null) {
                    LOG.error("We couldn't connect to NTP server. Timeout was {}millis", ntpTimeout);
                    offsetTime = 0L;
                } else {
                    offsetTime = offsetValue;
                }
            } catch (IOException ioe) {
                LOG.error(ioe.getMessage(), ioe);
            }
        } catch (SocketException e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.info("Offset time is: {}", offsetTime);
        return offsetTime;
    }
}
