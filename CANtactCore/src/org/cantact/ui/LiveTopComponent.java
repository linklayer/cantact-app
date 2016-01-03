/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cantact.ui;

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.cantact.core.CanFrame;
import org.cantact.core.CanListener;
import org.cantact.core.DeviceManager;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;

class LiveTableDataCell {
    private String current = "";
    private String previous = "";
    
    public String getCurrent() {
        return current;
    }
    public void setCurrent(String value) {
         current = value;
    }
    public String getPrevious() {
        return previous;
    }
    public void getCurrent(String value) {
        previous = value;
    }
    public void swap() {
        previous = current;
        current = "";
    }
    @Override
    public String toString() {
        return current;
    }
}

class LiveTableRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        Component c;
        if (value instanceof LiveTableDataCell) {
            LiveTableDataCell dataCell = (LiveTableDataCell)value;
            c = super.getTableCellRendererComponent(table, 
                dataCell.getCurrent(), isSelected, hasFocus, row, 
                column);
            
            // byte coloring
            // get the new byte values and old byte values
            String[] currentBytes = dataCell.getCurrent().split(" ");
            String[] prevBytes = dataCell.getPrevious().split(" ");
            String result = "<html>";
            
            for (int i = 0; i < currentBytes.length; i++) {
                // out of bytes in previous data, all other bytes are new
                if (i >= prevBytes.length) {
                    result = result + ("<font color='red'>" + 
                                       currentBytes[i] +
                                       "</font> ");
                    
                } else {
                    // check if the byte has changed
                    if (currentBytes[i].equals(prevBytes[i])) {
                        // byte has not changed
                        result = result + ("<font color='black'>" + 
                                           currentBytes[i] +
                                           "</font> ");
                    } else {
                        // byte changed
                        result = result + ("<font color='red'>" + 
                                           currentBytes[i] +
                                           "</font> ");
                    }
                }
            }
            result = result + "</html>";
            setText(result);
            /*
            if (dataCell.getCurrent().equals(dataCell.getPrevious()))
            {
                c.setForeground(Color.lightGray);
            } else {
                c.setForeground(Color.black);
            }*/

        } else {
            c = super.getTableCellRendererComponent(table, 
                value, isSelected, hasFocus, row, 
                column);
        }
        return c;
    }
}


/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.cantact.ui//Live//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "LiveTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "org.cantact.ui.LiveTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_LiveAction",
        preferredID = "LiveTopComponent"
)
@Messages({
    "CTL_LiveAction=Live",
    "CTL_LiveTopComponent=Live Window",
    "HINT_LiveTopComponent=This is a Live window"
})

public final class LiveTopComponent extends TopComponent implements CanListener {

    public LiveTopComponent() {
        initComponents();
        setName(Bundle.CTL_LiveTopComponent());
        setToolTipText(Bundle.HINT_LiveTopComponent());
        liveTable.setDefaultRenderer(Object.class, new LiveTableRenderer());
        DeviceManager.addListener(this);
    }
    class LiveUpdater implements Runnable {
        private CanFrame frame;
        public LiveUpdater(CanFrame f) {
            frame = f;
        }
        public void run() {
            String dataString = "";
            for (int i = 0; i < frame.getDlc(); i++) {
                dataString = dataString + String.format("%02X ", frame.getData()[i]);
            }
            
            DefaultTableModel liveModel = (DefaultTableModel) liveTable.getModel();
            boolean inserted = false;
                   
            for (int i = 0; i < liveModel.getRowCount(); i++) {
                if ((int)liveModel.getValueAt(i, 0) == frame.getId()) {
                    liveModel.setValueAt((Object)frame.getDlc(), i, 1);
                    // get the existing cell data
                    LiveTableDataCell dataCell = (LiveTableDataCell)liveModel.getValueAt(i, 2);
                    dataCell.swap();
                    // set current value to new data
                    dataCell.setCurrent(dataString);
                    // push to the table
                    liveModel.setValueAt(dataCell, i, 2);    
                    inserted = true;
                }
            }
            if (!inserted) {
                LiveTableDataCell dataCell = new LiveTableDataCell();
                dataCell.setCurrent(dataString);
                Object[] rowData = {(Object)frame.getId(), (Object)frame.getDlc(), dataCell};
                liveModel.addRow(rowData);
            }
        }
    }
    @Override
    public void canReceived(CanFrame f) {
        java.awt.EventQueue.invokeLater(new LiveUpdater(f));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane2 = new javax.swing.JScrollPane();
        liveTable = new javax.swing.JTable();
        jToolBar2 = new javax.swing.JToolBar();
        clearButton = new javax.swing.JButton();

        liveTable.setAutoCreateRowSorter(true);
        liveTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "DLC", "Data"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.Object.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane2.setViewportView(liveTable);

        jToolBar2.setRollover(true);

        org.openide.awt.Mnemonics.setLocalizedText(clearButton, org.openide.util.NbBundle.getMessage(LiveTopComponent.class, "LiveTopComponent.clearButton.text")); // NOI18N
        clearButton.setFocusable(false);
        clearButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        clearButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        clearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearButtonActionPerformed(evt);
            }
        });
        jToolBar2.add(clearButton);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 715, Short.MAX_VALUE)
            .addComponent(jToolBar2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jToolBar2, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void clearButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearButtonActionPerformed
        DefaultTableModel liveModel = (DefaultTableModel) liveTable.getModel();
        while (liveModel.getRowCount() > 0) {
            for (int i = 0; i < liveModel.getRowCount(); i++) {
                liveModel.removeRow(i);
            }
        }
    }//GEN-LAST:event_clearButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton clearButton;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JToolBar jToolBar2;
    private javax.swing.JTable liveTable;
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }
}
