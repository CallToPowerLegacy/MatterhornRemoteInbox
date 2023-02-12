/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.directorywatch.api;

import java.io.IOException;
import java.util.LinkedList;
import de.calltopower.mhri.directorywatch.api.events.FileCreatedEventListener;
import de.calltopower.mhri.directorywatch.api.events.FileDeletedEventListener;
import de.calltopower.mhri.directorywatch.api.events.FileModifiedEventListener;

/**
 * DirectoryWatch - Watches a directory via the Java Watch Service API
 *
 * @date 12.03.2012
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public interface DirectoryWatch {

    // getter
    /**
     * Returns the list of watched directories
     *
     * @return the list of watched directories
     */
    public LinkedList<String> getWatchedDirectories();

    // setter
    /**
     * Tries to set the main directory
     *
     * @param mainDir directory to set as the main directory
     * @return true if main directory has been successfully set, false else
     * @throws IOException
     */
    public boolean setMainDirectory(String mainDir) throws IOException;

    // misc
    public boolean addDirectory(String path) throws IOException;
    
    /**
     * Reset
     */
    public void reset();

    /**
     * Adds an file created event listener
     *
     * @param listener
     */
    public void addFileCreatedEventListener(FileCreatedEventListener listener);

    /**
     * Adds an file modified event listener
     *
     * @param listener
     */
    public void addFileModifiedEventListener(FileModifiedEventListener listener);

    /**
     * Adds an file deleted event listener
     *
     * @param listener
     */
    public void addFileDeletedEventListener(FileDeletedEventListener listener);

    /**
     * Removes an created event listener
     *
     * @param listener
     */
    public void removeFileCreatedEventListener(FileCreatedEventListener listener);

    /**
     * Removes an modified event listener
     *
     * @param listener
     */
    public void removeFileModifiedEventListener(FileModifiedEventListener listener);

    /**
     * Removes an deleted event listener
     *
     * @param listener
     */
    public void removeFileDeletedEventListener(FileDeletedEventListener listener);
}
