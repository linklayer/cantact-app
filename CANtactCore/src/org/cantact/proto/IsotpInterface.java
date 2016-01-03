/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cantact.proto;

import org.cantact.core.CanFrame;
import org.cantact.core.CantactDevice;
import org.cantact.core.CanListener;
import org.cantact.core.DeviceManager;
/**
 *
 * @author eric
 */
public class IsotpInterface implements CanListener {
    public IsotpInterface() {
        DeviceManager.addListener(this);
    }
    
    public void send(int id, byte[] data) {
        if (data.length <= 7) {
            // data fits into single frame, send SF
            CanFrame sf = new CanFrame();
            sf.setDlc(data.length + 1);
            sf.setId(id);
            byte[] frameData = new byte[8];
            // set first byte as SF with length
            // upper nybble = 0 for SF, lower nybble = data length
            frameData[0] = (byte)data.length;
            // set data bytes
            for (int i = 1; i < 8; i++) {
                if (i <= data.length) {
                    // add data byte
                    frameData[i] = data[i-1];
                } else {
                    // add padding byte
                    frameData[i] = 0x55;
                }
            }
            sf.setData(frameData);
            DeviceManager.sendFrame(sf);
        } else {
            // data is longer than single frame, need to send multiple
            // assemble first frame
            CanFrame ff = new CanFrame();
            ff.setDlc(data.length + 1);
            ff.setId(id);
            byte[] frameData = new byte[8];
            
            // set first two bytes for FF
            // byte 0: upper nybble = 1 for FF,
            // lower byte = data length high nybble 
            frameData[0] = (byte)(((data.length & 0x700) >> 8) + (1 << 4));
            // byte 1: lower byte of data length
            frameData[1] = (byte)(data.length & 0xFF);
            
            // add first 6 data bytes
            for (int i = 2; i < 8; i++) {
                frameData[i] = data[i-2];
            }
            
            // now we must wait for a frame control message before continuing
            // TODO: CF stuff
        }
    }
    
    public void canReceived(CanFrame f) {
        
    }
}
