package org.cantact.proto;

import org.cantact.core.CanFrame;
import org.cantact.core.CanListener;
import org.cantact.core.DeviceManager;

enum IsotpState {

    IDLE, IN_TX, TX_WAIT_FC, IN_RX
}

public class IsotpInterface implements CanListener {

    private IsotpState state;

    private int txId;
    private int[] txData;
    private int txDataIndex;
    private int txSerialNumber;
    private int txBlockSize;

    private int rxId;
    private int[] rxData;
    private int rxDataIndex;
    private int rxSerialNumber;
    private int rxBlockSize = 4095;
    private int rxBlockIndex;

    private final IsotpCallback callback; 
    
    public IsotpInterface(int txCanId, int rxCanId, IsotpCallback cb) {
        state = IsotpState.IDLE;
        txId = txCanId;
        rxId = rxCanId;
        callback = cb;
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
            frameData[0] = (byte) data.length;
            // set data bytes
            for (int i = 1; i < 8; i++) {
                if (i <= data.length) {
                    // add data byte
                    frameData[i] = data[i - 1];
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
            frameData[0] = (byte) (((data.length & 0x700) >> 8) + (1 << 4));
            // byte 1: lower byte of data length
            frameData[1] = (byte) (data.length & 0xFF);

            // add first 6 data bytes
            for (int i = 2; i < 8; i++) {
                frameData[i] = data[i - 2];
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
        int framesToSend = Math.min(txBlockSize,
            ((txData.length - txDataIndex) / 7) + 1);
        for (int i = 0; i < framesToSend; i++) {
            CanFrame cf = new CanFrame();
            cf.setDlc(8);
            cf.setId(txId);
            // data bytes to send is maximum 7, but is less if less than 7 bytes 
            // remain in the transmit buffer
            int byteCount = Math.min(7, txData.length - txDataIndex);
            // set the frame data to all zeros
            frameData = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
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
    }

    private void sendFlowControl() {
        CanFrame fc = new CanFrame();
        fc.setDlc(8);
        fc.setId(txId);

        // TODO: real values
        int[] frameData = new int[]{0x30, 0, 0, 0, 0, 0, 0, 0};
        fc.setData(frameData);
        DeviceManager.transmit(fc);
    }

    @Override
    public void canReceived(CanFrame f) {
        // check if this message has the correct id, if not, ignore it
        if (f.getId() != rxId) {
            return;
        }

        // upper nybble of byte 0 is frame type
        int frameType = (f.getData()[0] >> 4);

        if (state == IsotpState.TX_WAIT_FC) {
            // currently transmitting, waiting on a flow control frame
            if (frameType == 3) {
                // received a flow control frame, send the next block
                // TODO: (stmin and blocksize)
                txBlockSize = 4095;
                state = IsotpState.IN_TX;
                sendBlock();
                
                // determine next state
                if (txDataIndex >= txData.length) {
                    // all data sent, go to idle
                    state = IsotpState.IDLE;
                } else {
                    // with this block sent, we are waiting on another FC frame
                    state = IsotpState.TX_WAIT_FC;
                }
            } else {
                // this isn't the right frame, we'll ignore it
            }
        } else if (state == IsotpState.IDLE) {
            // currently not receiving or transmitting
            if (frameType == 0) {
                // received a single frame
                // data length is lower nybble of first byte
                int rxDataLength = f.getData()[0] & 0x0F;
                rxData = new int[rxDataLength];
                // copy data from single frame into rxData
                System.arraycopy(f.getData(), 1, rxData, 0, rxDataLength);
                rxDataIndex = rxDataLength - 1;
                callback.onIsotpReceived(rxData);
            } else if (frameType == 1) {
                // received a first frame
                // data length is lower nybble of first byte and second byte
                int rxDataLength = (((f.getData()[0] & 0x0F) << 8)
                        + f.getData()[1]);
                rxData = new int[rxDataLength];
                // copy data from first frame (6 bytes) into data buffer
                System.arraycopy(f.getData(), 2, rxData, 0, 6);
                rxDataIndex = 6;
                // next frame should have serial number 1
                rxSerialNumber = 1;
                // we're ready for the data, send a flow control frame
                state = IsotpState.IN_RX;
                sendFlowControl();
            }
        } else if (state == IsotpState.IN_RX) {
            // currently part way through receiving data
            if (frameType == 2) {
                // received a consecutive frame
                // check the serial number (lower nybble of first byte)
                if ((f.getData()[0] & 0x0F) != rxSerialNumber) {
                    // bad serial number!
                    // TODO: throw something
                    return;
                }

                // determine how many bytes should be read
                int numBytes = Math.min(7, rxData.length - rxDataIndex);

                // copy the data into the receive buffer
                System.arraycopy(f.getData(), 1, rxData, rxDataIndex, numBytes);
                rxDataIndex += numBytes;

                // increment the number of frames received in this block
                rxBlockIndex++;
                    
                // determine next state
                if (rxDataIndex >= rxData.length) {
                    // we've received all the data
                    callback.onIsotpReceived(rxData);
                    state = IsotpState.IDLE;
                } else if (rxBlockIndex >= rxBlockSize) {
                    // we have reached the end of this block, but have more data 
                    // to receive, send a frame control frame now
                    sendFlowControl();
                    rxBlockIndex = 0;
                    rxSerialNumber = 1;
                } else {
                    // we need more frames in for this block
                    // increment the expected serial number, wrapping at 0xF
                    rxSerialNumber++;
                    rxSerialNumber %= 0x0F;
                }
            } else if(frameType == 3) {
                // we have received our own flow control frame, do nothing
            } else {
                state = IsotpState.IDLE;
            }
        }
    }

    public void setRxId(Integer id) {
        rxId = id;
    }
    public int getRxId() {
        return rxId;
    }

    public void setTxId(Integer id) {
        txId = id;
    }
   public int getTxId() {
        return txId;
    }
   
    public void reset() {
        state = IsotpState.IDLE;
    }
    
    public interface IsotpCallback {
        public abstract void onIsotpReceived(int[] data);
    }
}
