/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.application.api;

/**
 * Recording
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public interface Recording {

    /**
     * States of Recording
     */
    public enum State {

        IDLE, RECIEVING, SCHEDULED, INPROGRESS, PAUSED, COMPLETE, FAILED
    }

    /**
     * get the ID
     *
     * @return the ID
     */
    int getId();

    /**
     * get the title
     *
     * @return the title
     */
    String getTitle();

    /**
     * get the original title
     *
     * @return the original title
     */
    String getOriginalTitle();

    /**
     * get the inbox
     *
     * @return the inbox
     */
    Inbox getInbox();

    /**
     * get the path
     *
     * @return the path
     */
    String getPath();

    /**
     * get the series ID
     *
     * @return the series ID
     */
    String getSeriesId();

    /**
     * get the workflow ID
     *
     * @return the workflow ID
     */
    String getWorkflowId();

    /**
     * get the mediapackage
     *
     * @return the mediapackage
     */
    String getMediaPackage();

    /**
     * get the state
     *
     * @return the state
     */
    State getState();

    /**
     * get flag whether to trim
     *
     * @return true when recording should be trimmed, false else
     */
    boolean getTrim();

    /**
     * get the error message
     *
     * @return the error message
     */
    String getErrorMessage();

    /**
     * get the ingest status
     *
     * @return the ingest status
     */
    String getIngestStatus();

    /**
     * get the ingest details
     *
     * @return the ingest details
     */
    String getIngestDetails();

    /**
     * get the upload process
     *
     * @return the upload process
     */
    int getUploadProgress();

    /**
     * get all files
     *
     * @return all files
     */
    RecordingFile[] getFiles();

    /**
     * set the id
     *
     * @param id
     */
    void setId(int id);

    /**
     * reset the title
     */
    void resetTitle();

    /**
     * set the title
     *
     * @param title
     */
    void setTitle(String name);

    /**
     * set the inbox
     *
     * @param inbox
     */
    void setInbox(Inbox inbox);

    /**
     * set the path
     *
     * @param path
     */
    void setPath(String path);

    /**
     * set the series ID
     *
     * @param seriesId
     */
    void setSeriesId(String seriesId);

    /**
     * set the workflow ID
     *
     * @param workflowId
     */
    void setWorkflowId(String workflowId);

    /**
     * set the mediapackage
     *
     * @param mediaPackage
     */
    void setMediaPackage(String mediaPackage);

    /**
     * set the state
     *
     * @param state
     */
    void setState(State state);

    /**
     * set flag whether to trim
     *
     * @return true when recording should be trimmed, false else
     */
    void setTrim(boolean trim);

    /**
     * set the error message
     *
     * @param msg
     */
    void setErrorMessage(String msg);

    /**
     * set the ingest status
     *
     * @param status
     */
    void setIngestStatus(String status);

    /**
     * set the ingest details
     *
     * @param status
     */
    void setIngestDetails(String status);

    /**
     * set the upload progress
     *
     * @param i the upload progress
     */
    void setUploadProgress(int i);

    /**
     * add a file
     *
     * @param file file to add
     */
    void addFile(RecordingFile file);

    /**
     * remove a file
     *
     * @param file file to remove
     */
    void removeFile(RecordingFile file);
}
