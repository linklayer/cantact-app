/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cantact.core;

import org.cantact.core.CanFrame;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

/**
 *
 * @author eric
 */
public class CantactDevice {
    private SerialPort serialPort;
    private int speedMode = 0;
    
    public CantactDevice(String deviceName) {
        this.serialPort = new SerialPort(deviceName);
    }
    
    public void setSpeedMode(int speedMode) {
        this.speedMode = speedMode;
    }
    
    public boolean isOpened() {
        if (this.serialPort == null) {
            return false;
        } else {
            return this.serialPort.isOpened();
        }
    }
    
    public String getDeviceName() {
        if (this.serialPort == null) {
            return "none";
        }
        return this.serialPort.getPortName();
    }
    
    public static String[] getDeviceList() {
        return SerialPortList.getPortNames();
    }
    
    public void start() {
        try {
            this.serialPort.openPort();
            if (!this.serialPort.isOpened()) {
                // TODO: throw error
                return;
            }
            this.serialPort.setParams(115200, 8, 1, 0);
            
            // close the device first, to ensure we can set bitrate
            this.serialPort.writeBytes("C\r".getBytes());
            // set the bitrate
            this.serialPort.writeString("S" + this.speedMode + "\r");
            // open the device
            this.serialPort.writeBytes("O\r".getBytes());
        } catch (SerialPortException ex) {
            // TODO: error handling
            System.out.println(ex);
        }
    }
    
    public void stop() {
        if (this.serialPort == null) {
            return;
        }
        
        try {
            this.serialPort.writeBytes("C\r".getBytes());
            this.serialPort.closePort();
        } catch (SerialPortException ex) {
            System.out.println(ex);
        }     
    }
    
    public CanFrame readFrame() {
        boolean frameReceived = false;
        
        List<Byte> frameBytes = new ArrayList<>();
        while(!frameReceived) {
            try {
                // read single byte from serial port
                Byte b = (Byte)this.serialPort.readBytes(1)[0];
                
                if (b == '\r') {
                    // end of frame data received
                    frameReceived = true;
                } else {
                    // byte received, add to buffer
                    frameBytes.add(b);
                }

            } catch (SerialPortException ex) {
                System.out.println(ex);
            }
        }
        return this.slcanToFrame(frameBytes.toArray(new Byte[frameBytes.size()]));
        
        
    }
    
    private CanFrame slcanToFrame(Byte[] slcanData) {
        CanFrame result = new CanFrame();
        
        Byte type = slcanData[0];
        
        if (type == 't') {
            int id;
            int dlc;
            
            Byte[] idBytes = Arrays.copyOfRange(slcanData, 1, 4);
            String idString = this.byteArrayToString(idBytes);
            id = Integer.valueOf(idString, 16);
            result.setId(id);
            
            Byte[] dlcBytes = Arrays.copyOfRange(slcanData, 4, 5);
            dlc = Integer.valueOf(this.byteArrayToString(dlcBytes));
            result.setDlc(dlc);
            
            Byte[] dataBytes = Arrays.copyOfRange(slcanData, 5, 
                                                  slcanData.length);
            byte[] data = {0,0,0,0,0,0,0,0};
            for(int i=0; i < dlc; i++) {    
                String byteString;
                byteString = this.byteArrayToString(Arrays.copyOfRange(dataBytes, i*2, i*2+2));
                data[i] = Integer.valueOf(byteString, 16).byteValue();
            }        
            result.setData(data);
        }
        
        return result;
    }
    
    public void sendFrame(CanFrame frame) {
        String slcanString = this.frameToSlcan(frame);
        try {
            this.serialPort.writeString(slcanString);
        } catch (SerialPortException ex) {
            Logger.getLogger(CantactDevice.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private String frameToSlcan(CanFrame frame) {
        String result = "";
        
        result += "t";
        result += String.format("%03X", frame.getId());
        result += Integer.toString(frame.getDlc());
        
        for (int i : frame.getData()) {
            result += String.format("%02X", i);
        }
        
        result += "\r";
        
        return result;
    }
    
    private String byteArrayToString(Byte[] array) {
        byte[] bytes = new byte[array.length];
        int i = 0;
        for (byte b : array) {
            bytes[i++] = b;
        }
        
        return new String(bytes);
    }
}
