/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.gui;

import java.awt.Component;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import de.calltopower.mhri.application.api.Recording;

/**
 * RecordingListCellRenderer
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class RecordingListCellRenderer implements ListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        return new RecordingListItemPanel((value != null) ? ((Recording) value) : null, isSelected);
    }
}
