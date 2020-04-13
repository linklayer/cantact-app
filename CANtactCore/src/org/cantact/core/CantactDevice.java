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
    
    enum CanFrameDlc {
        Dlc00(0),
        Dlc01(1),
        Dlc02(2),
        Dlc03(3),
        Dlc04(4),
        Dlc05(5),
        Dlc06(6),
        Dlc07(7),
        Dlc08(8),
        Dlc12(9),
        Dlc16(10),
        Dlc20(11),
        Dlc24(12),
        Dlc32(13),
        Dlc48(14),
        Dlc64(15);        

        private final int value;
        private CanFrameDlc(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

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
        
        if (null == type) {
            // this isn't a valid frame
            return null;
        } else switch (type) {
            case 'r':
                // standard ID RTR
                idBytes = Arrays.copyOfRange(slcanData, 1, 4);
                dlcBytes = Arrays.copyOfRange(slcanData, 4, 5);
                result.setIsRTR(true);
                dataBytes = null;
                break;
            case 'R':
                // extended ID RTR
                idBytes = Arrays.copyOfRange(slcanData, 1, 9);
                dlcBytes = Arrays.copyOfRange(slcanData, 9, 10);
                result.setIsRTR(true);
                result.setHasExtendedID(true);
                dataBytes = null;
                break;
            case 't':
                // standard ID
                idBytes = Arrays.copyOfRange(slcanData, 1, 4);
                dlcBytes = Arrays.copyOfRange(slcanData, 4, 5);
                dataBytes = Arrays.copyOfRange(slcanData, 5, slcanData.length);
                break;
            case 'T':
                // extended ID
                idBytes = Arrays.copyOfRange(slcanData, 1, 9);
                dlcBytes = Arrays.copyOfRange(slcanData, 9, 10);
                dataBytes = Arrays.copyOfRange(slcanData, 10, slcanData.length);
                result.setHasExtendedID(true);
                break;
            case 'd':
                // standard ID FD
                idBytes = Arrays.copyOfRange(slcanData, 1, 9);
                dlcBytes = Arrays.copyOfRange(slcanData, 9, 10);
                dataBytes = Arrays.copyOfRange(slcanData, 10, slcanData.length);
                result.setIsFD(true);
                break;
            case 'D':
                // extended ID FD
                idBytes = Arrays.copyOfRange(slcanData, 1, 9);
                dlcBytes = Arrays.copyOfRange(slcanData, 9, 10);
                dataBytes = Arrays.copyOfRange(slcanData, 10, slcanData.length);
                result.setHasExtendedID(true);
                result.setIsFD(true);
                break;
            case 'b':
                // standard ID FD
                idBytes = Arrays.copyOfRange(slcanData, 1, 9);
                dlcBytes = Arrays.copyOfRange(slcanData, 9, 10);
                dataBytes = Arrays.copyOfRange(slcanData, 10, slcanData.length);
                result.setIsFD(true);
                result.setIsBRS(true);
                break;
            case 'B':
                // extended ID FD
                idBytes = Arrays.copyOfRange(slcanData, 1, 9);
                dlcBytes = Arrays.copyOfRange(slcanData, 9, 10);
                dataBytes = Arrays.copyOfRange(slcanData, 10, slcanData.length);
                result.setHasExtendedID(true);
                result.setIsFD(true);
                result.setIsBRS(true);
                break;
            default:
                // this isn't a valid frame
                return null;
        }
        String idString = byteArrayToString(idBytes);
        id = Integer.valueOf(idString, 16);
        result.setId(id);
        

        dlc = Integer.valueOf(byteArrayToString(dlcBytes));
        dlc = dlcToSize(CanFrameDlc.values()[dlc]);
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
        int dlc = frame.getDlc();
        boolean isFd = frame.isIsFD();
        boolean isBRS = frame.isIsBRS();
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
                    if (isBRS)
                        result += "B";
                    else
                        result += "D";
                }else {
                    result += "T";
                }
                
                result += String.format("%08X", frame.getId());
                
            } else {
                if (isFd){
                    if (isBRS)
                        result += "b";
                    else
                        result += "d";
                }else {
                    result += "t";
                }
                
                result += String.format("%03X", frame.getId());
            }    
            
        }

        result += Integer.toString(sizeToDlc(dlc).getValue());
        
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
    
    private static CanFrameDlc sizeToDlc(int size) {
        switch (size) {
            case 0:
                return CanFrameDlc.Dlc00;
            case 1:
                return CanFrameDlc.Dlc01;
            case 2:
                return CanFrameDlc.Dlc02;
            case 3:
                return CanFrameDlc.Dlc03;
            case 4:
                return CanFrameDlc.Dlc04;
            case 5:
                return CanFrameDlc.Dlc05;
            case 6:
                return CanFrameDlc.Dlc06;
            case 7:
                return CanFrameDlc.Dlc07;
            case 8:
                return CanFrameDlc.Dlc08;
            case 12:
                return CanFrameDlc.Dlc12;
            case 16:
                return CanFrameDlc.Dlc16;
            case 20:
                return CanFrameDlc.Dlc20;
            case 24:
                return CanFrameDlc.Dlc24;
            case 32:
                return CanFrameDlc.Dlc32;
            case 48:
                return CanFrameDlc.Dlc48;
            case 64:
                return CanFrameDlc.Dlc64;
            default:
                return CanFrameDlc.Dlc00;
        }
    }

    static int dlcToSize(CanFrameDlc dlc) {
        switch (dlc) {
            case Dlc00:
                return 0;
            case Dlc01:
                return 1;
            case Dlc02:
                return 2;
            case Dlc03:
                return 3;
            case Dlc04:
                return 4;
            case Dlc05:
                return 5;
            case Dlc06:
                return 6;
            case Dlc07:
                return 7;
            case Dlc08:
                return 8;
            case Dlc12:
                return 12;
            case Dlc16:
                return 16;
            case Dlc20:
                return 20;
            case Dlc24:
                return 24;
            case Dlc32:
                return 32;
            case Dlc48:
                return 48;
            case Dlc64:
                return 64;
        }
        return 0;
    }
}
