package org.cantact.core;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 *
 * @author eric
 */
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
        this.data = new_data;
    }
}
