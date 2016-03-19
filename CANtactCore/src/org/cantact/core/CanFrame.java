package org.cantact.core;

public class CanFrame {
    private int id;
    private int dlc;
    private int[] data;
    
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
        int[] result = {0,0,0,0,0,0,0,0};
        // copy into empty array to ensure length is 8
        System.arraycopy(new_data, 0, result, 0, new_data.length);
        this.data = result;
    }
}
