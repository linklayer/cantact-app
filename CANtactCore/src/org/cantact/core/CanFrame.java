package org.cantact.core;

public class CanFrame {
    private int id;
    private int dlc;
    private int[] data;
    private boolean isFD;
    private boolean isBRS;
    private boolean isRTR;
    private boolean hasExtendedID;

    public boolean isIsRTR() {
        return isRTR;
    }

    public boolean isIsBRS() {
        return isBRS;
    }

    public void setIsBRS(boolean isBRS) {
        this.isBRS = isBRS;
    }

    public void setIsRTR(boolean isRTR) {
        this.isRTR = isRTR;
    }

    public boolean isHasExtendedID() {
        return hasExtendedID;
    }

    public void setHasExtendedID(boolean hasExtendedID) {
        this.hasExtendedID = hasExtendedID;
    }
    
    public int getId() {
        return this.id;
    }
    public void setId(int new_id) {
        this.id = new_id;
    }
    
    public int getDlc() {
        return this.dlc;
    }
    public void setDlc(int new_dlc) {
        this.dlc = new_dlc;
    }
    
    public int[] getData() {
        return this.data;
    }
    public void setData(int[] new_data) {
        int[] result = {
            0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0};
        // copy into empty array to ensure length is 8
        System.arraycopy(new_data, 0, result, 0, new_data.length);
        this.data = result;
    }
    
    public boolean isIsFD() {
        return this.isFD;
    }
    public void setIsFD(boolean isFD) {
        this.isFD = isFD;
    }
}
