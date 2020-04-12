/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cantact.core;

import javax.swing.text.PlainDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

/**
 *
 * @author Alexander
 */
public class FixedSizeDocument extends PlainDocument{
    private int limit;

    public FixedSizeDocument(int limit) {
        super();
        this.limit = limit;
    }

    public void insertString( int offset, String  str, AttributeSet attr ) throws BadLocationException {
        if (str == null) return;

        if (((getLength() + str.length()) <= limit) && (isInt(str))) {
            super.insertString(offset, str, attr);
        }
    }
    
   private boolean isInt(String text) {
      try {
         Integer.parseInt(text);
         return true;
      } catch (NumberFormatException e) {
         return false;
      }
   }

}
