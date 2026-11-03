package org.plovdev.audioengine.devices;

public interface AudioDeviceListener {
    void onDeviceConnected(AudioDeviceInfo info);

    void onDeviceDisconnected(AudioDeviceInfo info);
}