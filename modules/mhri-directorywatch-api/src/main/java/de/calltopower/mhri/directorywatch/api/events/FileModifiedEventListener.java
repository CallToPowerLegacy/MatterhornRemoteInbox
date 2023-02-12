/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.directorywatch.api.events;

import java.util.EventListener;

/**
 * FileModifiedEventListener - Listener for FileModifiedEvent
 *
 * @date 12.03.2012
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public interface FileModifiedEventListener extends EventListener {

    /**
     * When a FileModifiedEvent has been thrown
     *
     * @param evt
     * @param file
     */
    public void fileModifiedEventOccurred(FileModifiedEvent evt, String file);
}
