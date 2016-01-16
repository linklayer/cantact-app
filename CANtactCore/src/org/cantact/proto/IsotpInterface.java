/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cantact.proto;

import org.cantact.core.CanFrame;
import org.cantact.core.CanListener;
import org.cantact.core.DeviceManager;
/**
 *
 * @author eric
 */
enum IsotpState {
    IDLE, IN_TX, TX_WAIT_FC, IN_RX
}

public class IsotpInterface implements CanListener {
    private int[] txData;
    private int txDataIndex;
    private IsotpState state;
    private int blockSize;
    private int txId;
    private int rxId;
    private int[] rxData;
    private int rxDataIndex;
    private int rxDataLength;
    private int txSerialNumber;
    
    public IsotpInterface(int txCanId, int rxCanId) {
        DeviceManager.addListener(this);
        state = IsotpState.IDLE;
        txId = txCanId;
        rxId = rxCanId;
    }
    
    public void send(int[] data) {
        if (data.length <= 7) {
            // data fits into single frame, send SF
            CanFrame sf = new CanFrame();
            sf.setDlc(8);
            sf.setId(txId);
            
            int[] frameData = new int[8];
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
            DeviceManager.transmit(sf);
        } else {
            // data is longer than single frame, need to send multiple
            // assemble first frame
            CanFrame ff = new CanFrame();
            ff.setDlc(8);
            ff.setId(txId);
            int[] frameData = new int[8];
            
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
            ff.setData(frameData);
            
            // set the data so it is available for consecutive frames
            txData = data;
            txDataIndex = 6;
            
            // the next message sent will have serial number 1
            txSerialNumber = 1;
            
            // now we must wait for a flow control message before continuing
            state = IsotpState.TX_WAIT_FC;
            
            // finally, send the FF to the device
            DeviceManager.transmit(ff);
            
        }
    }
    
    private void sendBlock() {

        int[] frameData;
        // how many frames should be sent?
        int framesToSend = Math.min(blockSize, 
                                    ((txData.length - txDataIndex)/7)+1);
        for (int i = 0; i < framesToSend; i++) {
            CanFrame cf = new CanFrame();
            cf.setDlc(8);
            cf.setId(txId);
            // data bytes to send is maximum 7, but is less if less than 7 bytes 
            // remain in the transmit buffer
            int byteCount = Math.min(7, txData.length - txDataIndex);
            // set the frame data to all zeros
            frameData = new int[] {0,0,0,0,0,0,0,0};
            // first byte of CF, upper nybble is 2, lower nybble is serial
            // number
            frameData[0] = (2 << 4) + txSerialNumber;
            // copy the transmit data into the frame data
            System.arraycopy(txData, txDataIndex, frameData, 1, byteCount);
            cf.setData(frameData);
            // send the frame
            DeviceManager.transmit(cf);
            // increment the serial number, wrapping at 0xF
            txSerialNumber++;
            txSerialNumber %= 0x0F;
            // increment the data index to point at the next byte to send
            txDataIndex += byteCount;
        }
        
        if (txDataIndex >= txData.length) {
            // all data sent, go to idle
            state = IsotpState.IDLE;
            txSerialNumber = 0;
        } else {
            // with this block sent, we are waiting on another FC frame
            state = IsotpState.TX_WAIT_FC;
        }
    }
    
    @Override
    public void canReceived(CanFrame f) {
        // check if this message has the correct id, if not, ignore it
        if (f.getId() != rxId) return;
        
        // upper nybble of byte 0 is frame type
        int frameType = (f.getData()[0] & 0xF0) >> 4;
        
        if (state == IsotpState.TX_WAIT_FC) {
            if (frameType == 3) {
                // this is a flow control frame
                // TODO: (stmin and blocksize)
                blockSize = 10;
                state = IsotpState.IN_TX;
                sendBlock();
            } else {
                // this isn't the right frame, we'll ignore it
            }
        } else if (state == IsotpState.IDLE) {
            // TODO: handle SF and FF
            if (frameType == 0) {
                // this is a single frame
                // data length is lower nybble of first byte
                rxDataLength = f.getData()[0] & 0x0F;
                // TODO: make the CF and the SFs work!!!
            }
        } else if (state == IsotpState.IN_RX) {
            // TODO: handle CF
        }
    }
}
