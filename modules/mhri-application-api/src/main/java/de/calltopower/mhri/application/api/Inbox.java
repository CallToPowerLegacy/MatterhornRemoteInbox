/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.application.api;

/**
 * Inbox
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public interface Inbox {

    /**
     * get the ID
     *
     * @return the ID
     */
    int getId();
    
    /**
     * update series information from a series file
     */
    void updateSeriesTitle();

    /**
     * get the name
     *
     * @return the name
     */
    String getName();

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
     * get the series title
     *
     * @return the series title
     */
    String getSeriesTitle();

    /**
     * get the workflow ID
     *
     * @return the workflow ID
     */
    String getWorkflowId();

    /**
     * get all recordings
     *
     * @return all recordings
     */
    Recording[] getRecordings();

    /**
     * set the ID
     *
     * @param id
     */
    void setId(int id);

    /**
     * set the name
     *
     * @param name
     */
    void setName(String name);

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
     * set the series title
     *
     * @param seriesTitle
     */
    void setSeriesTitle(String seriesTitle);

    /**
     * set the workflow ID
     *
     * @param workflowId
     */
    void setWorkflowId(String workflowId);

    /**
     * add a recording
     *
     * @param r recording to add
     */
    void addRecording(Recording r);

    /**
     * remove a recording
     *
     * @param r recording to remove
     */
    void removeRecording(Recording r);
}
