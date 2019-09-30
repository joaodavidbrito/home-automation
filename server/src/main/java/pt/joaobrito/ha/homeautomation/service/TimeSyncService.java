package pt.joaobrito.ha.homeautomation.service;


public interface TimeSyncService {
    /**
     * updates the clock
     *
     * @return offsetTime in millis
     */
    Long update();
}
