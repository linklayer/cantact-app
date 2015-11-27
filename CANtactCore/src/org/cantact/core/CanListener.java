/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cantact.core;
import java.util.EventListener;
/**
 *
 * @author eric
 */
public interface CanListener extends EventListener {
    public void canReceived(CanFrame f);
}
