/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.directorywatch.api.exceptions;

/**
 * MainDirectoryDeletedException - Thrown whenever the main directory has been
 * deleted
 *
 * @date 12.03.2012
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class MainDirectoryDeletedException extends Exception {

    public MainDirectoryDeletedException() {
    }

    public MainDirectoryDeletedException(String s) {
        super(s);
    }
}
