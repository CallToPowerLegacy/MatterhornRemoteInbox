/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.application.api;

import javax.swing.ListModel;

/**
 * RecordingListModel
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public interface RecordingListModel extends ListModel {

    /**
     * set the inbox
     * @param inbox 
     */
    void setInbox(Inbox inbox);
}
