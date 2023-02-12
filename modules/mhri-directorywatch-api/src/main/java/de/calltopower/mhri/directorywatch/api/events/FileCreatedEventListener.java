/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.directorywatch.api.events;

import java.util.EventListener;

/**
 * FileCreatedEventListener - Listener for FileCreatedEvent
 *
 * @date 12.03.2012
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public interface FileCreatedEventListener extends EventListener {

    /**
     * When a FileCreatedEvent has been thrown
     *
     * @param evt
     * @param file
     */
    public void fileCreatedEventOccurred(FileCreatedEvent evt, String file);
}
