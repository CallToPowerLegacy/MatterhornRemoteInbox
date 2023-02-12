/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.application.api;

import javax.swing.ListModel;

/**
 * ListModelAdapter
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public interface ListModelAdapter {

    /**
     * get the inbox list model
     *
     * @return the inbox list model
     */
    ListModel getInboxListModel();

    /**
     * get the recording list model
     *
     * @return the recording list model
     */
    RecordingListModel getRecordingListModel();

    /**
     * Resets the model
     */
    void resetBefore();

    /**
     * Resets the model
     */
    void resetAfter();
}
