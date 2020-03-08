package org.cantact.core;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;

public class CantactDevice {

    private SerialPort serialPort;
    private int speedMode = 0;

    public CantactDevice(String deviceName) {
        serialPort = new SerialPort(deviceName);
    }

    public void setSpeedMode(int speedMode) {
        this.speedMode = speedMode;
    }

    public boolean isOpened() {
        if (serialPort == null) {
            return false;
        } else {
            return serialPort.isOpened();
        }
    }

    public String getDeviceName() {
        if (serialPort == null) {
            return "none";
        }
        return serialPort.getPortName();
    }

    public static String[] getDeviceList() {
        return SerialPortList.getPortNames();
    }

    public void start() {
        try {
            serialPort.openPort();
            if (!serialPort.isOpened()) {
                // TODO: throw error
                return;
            }
            serialPort.setParams(115200, 8, 1, 0);

            // close the device first, to ensure we can set bitrate
            serialPort.writeBytes("C\r".getBytes());
            // set the bitrate
            serialPort.writeString("S" + speedMode + "\r");
            // open the device
            serialPort.writeBytes("O\r".getBytes());
            SerialPortEventListener listener = new CantactDeviceListener();
            serialPort.addEventListener(listener);
        } catch (SerialPortException ex) {
            // TODO: error handling
            System.out.println(ex);
        }
    }

    public void stop() {
        if (serialPort == null) {
            return;
        }

        try {
            serialPort.writeBytes("C\r".getBytes());
            serialPort.removeEventListener();
            serialPort.purgePort(SerialPort.PURGE_RXABORT | SerialPort.PURGE_TXCLEAR);
            serialPort.closePort();
        } catch (SerialPortException ex) {
            System.out.println(ex);
        }
    }

    private CanFrame slcanToFrame(Byte[] slcanData) {
        CanFrame result = new CanFrame();

        Byte type = slcanData[0];

        int id;
        int dlc;
        Byte[] idBytes;
        Byte[] dlcBytes;
        Byte[] dataBytes;
        
        if (type == 'r') {
            // standard ID RTR
            idBytes = Arrays.copyOfRange(slcanData, 1, 4);
            dlcBytes = Arrays.copyOfRange(slcanData, 4, 5);
            result.setIsRTR(true);
            dataBytes = null;
        } else if (type == 'R') {
            // extended ID RTR
            idBytes = Arrays.copyOfRange(slcanData, 1, 9);
            dlcBytes = Arrays.copyOfRange(slcanData, 9, 10);
            result.setIsRTR(true);
            result.setHasExtendedID(true);
            dataBytes = null;
        } else if (type == 't') {
            // standard ID
            idBytes = Arrays.copyOfRange(slcanData, 1, 4);
            dlcBytes = Arrays.copyOfRange(slcanData, 4, 5);
            dataBytes = Arrays.copyOfRange(slcanData, 5, slcanData.length);
        } else if (type == 'T') {
            // extended ID
            idBytes = Arrays.copyOfRange(slcanData, 1, 9);
            dlcBytes = Arrays.copyOfRange(slcanData, 9, 10);
            dataBytes = Arrays.copyOfRange(slcanData, 10, slcanData.length);
            result.setHasExtendedID(true);
        } else if (type == 'd') {
            // standard ID FD
            idBytes = Arrays.copyOfRange(slcanData, 1, 9);
            dlcBytes = Arrays.copyOfRange(slcanData, 9, 10);
            dataBytes = Arrays.copyOfRange(slcanData, 10, slcanData.length);
            result.setIsFD(true);
        } else if (type == 'D') {
            // extended ID FD
            idBytes = Arrays.copyOfRange(slcanData, 1, 9);
            dlcBytes = Arrays.copyOfRange(slcanData, 9, 10);
            dataBytes = Arrays.copyOfRange(slcanData, 10, slcanData.length);
            result.setHasExtendedID(true);
             result.setIsFD(true);
        }else {
            // this isn't a valid frame
            return null;
        }
        String idString = byteArrayToString(idBytes);
        id = Integer.valueOf(idString, 16);
        result.setId(id);
        

        dlc = Integer.valueOf(byteArrayToString(dlcBytes));
        result.setDlc(dlc);
         
        if (!result.isIsRTR()){
            int[] data = { 
                0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0};
            for (int i = 0; i < dlc; i++) {
                String byteString;
                byteString = byteArrayToString(Arrays.copyOfRange(dataBytes,
                        i * 2, i * 2 + 2));
                data[i] = Integer.valueOf(byteString, 16);
            }
            result.setData(data);
        }

        return result;
    }

    public void sendFrame(CanFrame frame) {
        String slcanString = frameToSlcan(frame);
        try {
            serialPort.writeString(slcanString);
        } catch (SerialPortException ex) {
            Logger.getLogger(CantactDevice.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String frameToSlcan(CanFrame frame) {
        String result = "";
        boolean isFd = frame.isIsFD();
        boolean hasExtendedID = frame.isHasExtendedID();
        
        if(frame.isIsRTR()){
            if(hasExtendedID){
                result += "R";
            } else{
                result += "r";
            }
        } else {   
            
            if (hasExtendedID){
                if (isFd){
                    result += "D";
                }else {
                    result += "T";
                }
                
                result += String.format("%08X", frame.getId());
                
            } else {
                if (isFd){
                    result += "d";
                }else {
                    result += "t";
                }
                
                result += String.format("%03X", frame.getId());
            }    
            
        }

        result += Integer.toString(frame.getDlc());
        
        if (!frame.isIsRTR()){
            for (int i : frame.getData()) {
                result += String.format("%02X", i);
            }
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

    private class CantactDeviceListener implements SerialPortEventListener {

        List<Byte> frameBytes = new ArrayList<>();

        @Override
        public void serialEvent(SerialPortEvent event) {
            if (event.isRXCHAR() && event.getEventValue() > 0) {
                try {
                    byte[] bs;
                    bs = serialPort.readBytes();

                    for (byte b : bs) {
                        if (b == '\r') {
                             if(frameBytes.size() > 4) {
                                // end of frame data received
                                CanFrame f = slcanToFrame(frameBytes.toArray(new Byte[frameBytes.size()]));
                                DeviceManager.giveFrame(f);
                             }
                            frameBytes.clear();
                        } else {
                            // byte received, add to buffer
                            frameBytes.add(b);
                        }
                    }
                } catch (SerialPortException ex) {
                    System.out.println(ex);
                }
            }
        }
    }
}
