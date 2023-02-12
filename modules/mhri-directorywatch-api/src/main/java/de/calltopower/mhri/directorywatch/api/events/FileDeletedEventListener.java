/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.directorywatch.api.events;

import java.util.EventListener;

/**
 * FileDeletedEventListener - Listener for FileCreatedEvent
 *
 * @date 12.03.2012
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public interface FileDeletedEventListener extends EventListener {

    /**
     * When a FileDeletedEvent has been thrown
     *
     * @param evt
     * @param file
     */
    public void fileDeletedEventOccurred(FileDeletedEvent evt, String file);
}
