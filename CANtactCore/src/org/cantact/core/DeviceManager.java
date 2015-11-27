/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cantact.core;

import java.util.ArrayList;

/**
 *
 * @author eric
 */


public final class DeviceManager {
    private static CantactDevice device;
    private static ArrayList<CanListener> canListeners = new ArrayList<>();
    
    private static final Runnable readThread = new Runnable() {
        @Override
        public void run() {
            readTask();
        }
    };
    
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
        new Thread(readThread).start();
        
    }
    
    private static void readTask() {
        for(;;) {
            CanFrame f = device.readFrame();
            //System.out.println(f);
            for (CanListener l : canListeners) {
                l.canReceived(f);
            }
        }
    }
}

