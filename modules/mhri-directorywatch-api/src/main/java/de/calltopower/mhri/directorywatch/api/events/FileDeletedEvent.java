/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.directorywatch.api.events;

import java.util.EventObject;

/**
 * FileDeletedEvent - Thrown whenever an file has been deleted
 *
 * @date 12.03.2012
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class FileDeletedEvent extends EventObject {

    /**
     * Constructor
     *
     * @param source
     */
    public FileDeletedEvent(Object source) {
        super(source);
    }
}
