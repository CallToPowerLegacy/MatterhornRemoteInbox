/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.directorywatch.api.events;

import java.util.EventObject;

/**
 * FileCreatedEvent - Thrown whenever an file has been created (added)
 *
 * @date 12.03.2012
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class FileCreatedEvent extends EventObject {

    /**
     * Constructor
     *
     * @param source
     */
    public FileCreatedEvent(Object source) {
        super(source);
    }
}
