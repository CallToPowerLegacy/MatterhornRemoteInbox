/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.ingestclient.api;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;

/**
 * IngestClientController - Controls IngestClients
 *
 * @date 28.06.2013
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public interface IngestClientController {

    /**
     * States of the network connection
     */
    public enum NetworkConnectionState {

        CLIENT_OFFLINE, SERVER_OFFLINE, WRONG_CREDENTIALS, ONLINE, ERROR
    }

    /**
     * Returns the status of the connection to the server
     *
     * @return the status of the connection to the server
     */
    public NetworkConnectionState getNetworkConnectionState();

    /**
     * Creates a new series
     *
     * @return the created series
     */
    public String createNewSeries(String document) throws IngestClientException, URISyntaxException;

    /**
     * Returns a client
     *
     * @param id id of the client, e.g. the path of the recording
     * @return a client
     */
    public IngestClient getClient(String id);

    /**
     * Removes a client
     *
     * @param id id of the client, e.g. the path of the recording
     * @return true when client has been removed, false else
     */
    public boolean removeClient(String id);

    /**
     * get the series catalog
     *
     * @param seriesID
     * @return the series catalog
     * @throws IngestClientException
     */
    public String getSeriesCatalog(String seriesID) throws IngestClientException, URISyntaxException;

    /**
     * get the series list
     *
     * @return the series list as a HashMap
     * @throws IOException
     * @throws IngestClientException
     */
    public HashMap<String, String> getSeriesList() throws IOException, IngestClientException, URISyntaxException;

    /**
     * get the workflow list
     *
     * @return the workflow list as a HashMap
     * @throws IOException
     * @throws IngestClientException
     */
    public List<String> getWorkflowList() throws IOException, IngestClientException, URISyntaxException;

    /**
     * reload the service
     */
    public void reload();
}
