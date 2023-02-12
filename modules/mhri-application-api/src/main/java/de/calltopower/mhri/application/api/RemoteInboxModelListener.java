/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.application.api;

/**
 * RemoteInboxModelListener
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public interface RemoteInboxModelListener {

    /**
     * inbox has been created
     *
     * @param inbox
     */
    void inboxCreated(Inbox inbox);

    /**
     * inbox has been modified
     *
     * @param inbox
     */
    void inboxModified(Inbox inbox);

    /**
     * inbox has been removed
     *
     * @param inbox
     */
    void inboxRemoved(Inbox inbox);

    /**
     * recording has been created
     *
     * @param recording
     */
    void recordingCreated(Recording recording);

    /**
     * recording has been modified
     *
     * @param recording
     */
    void recordingModified(Recording recording);

    /**
     * recording has been removed
     *
     * @param recording
     */
    void recordingRemoved(Recording recording);
}
