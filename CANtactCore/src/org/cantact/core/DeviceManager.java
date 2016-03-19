package org.cantact.core;

import java.util.ArrayList;

public class DeviceManager {

    private static CantactDevice device;
    private final static ArrayList<CanListener> canListeners = new ArrayList<>();

    public static boolean isDeviceOpen(String devName) {
        if (device != null && devName != null) {
            return (device.getDeviceName().
                    equals(devName) && device.isOpened());
        } else {
            return false;
        }
    }

    public static void addListener(CanListener l) {
        canListeners.add(l);
    }

    public static void removeListener(CanListener l) {
        canListeners.remove(l);
    }

    public static String[] getDeviceList() {
        return CantactDevice.getDeviceList();
    }

    public static void openDevice(String deviceName, int speed) {
        device = new CantactDevice(deviceName);
        device.setSpeedMode(speed);
        device.start();
    }
    
    public static void transmit(CanFrame txFrame) {
        if (device != null) {
            device.sendFrame(txFrame);
            for (CanListener l : canListeners) {
                l.canReceived(txFrame);
            }
        }
    }

    public static void closeDevice(String portName) {
        device.stop();
    }

    static void giveFrame(CanFrame f) {
        for (CanListener l : canListeners) {
            l.canReceived(f);
        }
    }
}
