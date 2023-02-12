/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.application.api;

/**
 * RecordingFile
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public interface RecordingFile {

    /**
     * Types of RecordingFile
     */
    public enum Type {

        TRACK, CATALOG, ATTACHMENT;
    }

    /**
     * get the flavor
     *
     * @return the flavor
     */
    String getFlavor();

    /**
     * get the ID
     *
     * @return the ID
     */
    int getId();

    /**
     * get the path
     *
     * @return the path
     */
    String getPath();

    /**
     * get the recording
     *
     * @return the recording
     */
    Recording getRecording();

    /**
     * get the type
     *
     * @return the type
     */
    Type getType();

    /**
     * get the upload job
     *
     * @return the uploadjob
     */
    UploadJob getUploadJob();

    /**
     * set the flavor
     *
     * @param flavor
     */
    void setFlavor(String flavor);

    /**
     * set the id
     *
     * @param id
     */
    void setId(int id);

    /**
     * set the path
     *
     * @param path
     */
    void setPath(String path);

    /**
     * set the recording
     *
     * @param recording
     */
    void setRecording(Recording recording);

    /**
     * set the type
     *
     * @param type
     */
    void setType(Type type);

    // set the upload job
    void setUploadJob(UploadJob uploadJob);
}
