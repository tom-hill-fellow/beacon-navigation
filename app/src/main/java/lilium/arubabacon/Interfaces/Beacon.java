package lilium.arubabacon.Interfaces;


/**
 * Created by boylec on 1/29/17.
 */

public interface Beacon {
    String getMac();
    float getX();
    float getY();
    long getLastUpdate();
    int getRssi();

    void setMac(String mac);
    void setX(float x);
    void setY(float y);
    void setLastUpdate(long lastUpdate);
    void setRssi(int rssi);

    double distance();
    void addRssi(Integer Rssi);
    double smoothRssi();

    @Override
    boolean equals(Object obj);

    @Override
    String toString();
}
