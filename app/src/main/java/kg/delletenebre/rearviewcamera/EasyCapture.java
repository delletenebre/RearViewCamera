package kg.delletenebre.rearviewcamera;

import android.view.Surface;

public interface EasyCapture {
	void getFrame(Surface mySuface);
    void stop();
    boolean isAttached();
    boolean isDeviceConnected();

    EasycapSettings getSettings();
}
