/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.application.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import de.calltopower.mhri.util.conf.Configuration;

/**
 * RemoteInboxApplication
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public interface RemoteInboxApplication {

    /**
     * get the list model adapter
     *
     * @return the list model adapter
     */
    ListModelAdapter getListModelAdapter();

    /**
     * get the configuration
     *
     * @return the configuration
     */
    Configuration getConfig();

    /**
     * get the inbox path
     *
     * @return the inbox path
     */
    String getInboxPath();

    /**
     * get the series list
     *
     * @return the series list
     * @throws IOException
     */
    HashMap<String, String> getSeriesList() throws IOException;

    /**
     * get the workflow list
     *
     * @return the workflow list
     * @throws IOException
     */
    List<String> getWorkflowList() throws IOException;

    /**
     * set the currently selected Inbox
     *
     * @param currInboxName
     */
    void setCurrentSelectedInbox(String currInboxName);

    /**
     * creates an inbox
     *
     * @param name name of the inbox
     * @return true when the inbox has been successfully created, false else
     */
    boolean createInbox(String name);

    /**
     * delete an inbox
     *
     * @param inbox inbox to delete
     * @return true when the inbox has been successfully deleted, false else
     */
    boolean deleteInbox(Inbox inbox);

    /**
     * downloads a series from the server
     *
     * @param seriesID series ID to the series to download
     * @throws IOException
     */
    void downloadSeries(String seriesID) throws IOException;

    /**
     * schedule ingest
     *
     * @param recording recording to be scheduled
     */
    void scheduleIngest(Recording recording);

    /**
     * start ingest
     *
     * @param recording recording to be ingested
     */
    void startIngest(Recording recording);

    /**
     * pause ingest
     *
     * @param recording recording to be pause
     */
    void pauseIngest(Recording recording);

    /**
     * stop ingest
     *
     * @param recording recording to be stopped
     */
    void stopIngest(Recording recording);

    /**
     * retry ingest
     *
     * @param recording recording to be retried
     */
    void retryIngest(Recording recording);

    /**
     * delete recording
     *
     * @param recording recording to be deleted
     * @return true when the recording has been successfully deleted, false else
     */
    boolean deleteRecording(Recording recording);
}
