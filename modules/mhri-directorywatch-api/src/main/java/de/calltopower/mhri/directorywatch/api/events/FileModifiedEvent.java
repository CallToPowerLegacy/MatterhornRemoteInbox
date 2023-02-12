/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.directorywatch.api.events;

import java.util.EventObject;

/**
 * FileModifiedEvent - Thrown whenever an file has been modified
 *
 * @date 12.03.2012
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class FileModifiedEvent extends EventObject {

    /**
     * Constructor
     *
     * @param source
     */
    public FileModifiedEvent(Object source) {
        super(source);
    }
}
