package org.cantact.core;
import java.util.EventListener;

public interface CanListener extends EventListener {
    public void canReceived(CanFrame f);
}
