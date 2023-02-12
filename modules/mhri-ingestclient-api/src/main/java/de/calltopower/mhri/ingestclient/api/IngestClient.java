/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.ingestclient.api;

import java.util.Map;
import java.util.concurrent.FutureTask;
import de.calltopower.mhri.application.api.RecordingFile;

/**
 * IngestClient - Instantiates a MediaPackage and uploads to a Matterhorn
 * instance
 *
 * 1. createNewMediaPackage --> get MediaPackage XML 2. addTrack, addCatalog 3.
 * startProcessing when everything is fully uploaded
 *
 * @date 28.06.2012
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public interface IngestClient {

    /**
     * States of Instances
     */
    public enum InstanceState {

        RUNNING, PAUSED, FAILING, SUCCEEDED, INSTANTIATED
    }

    public String getID();

    /**
     * create a new mediapackage
     *
     * @return futuretask creating a new mediapackage
     * @throws IngestClientException
     */
    public FutureTask createNewMediaPackage() throws IngestClientException;

    /**
     * add a track
     *
     * @param file
     * @return a futuretask adding a track
     */
    public FutureTask addTrack(RecordingFile file);

    /**
     * add a catalog
     *
     * @param file
     * @return a futuretask adding a catalog
     */
    public FutureTask addCatalog(RecordingFile file);

    /**
     * start processing
     *
     * @param mediaPackageXML
     * @param workflowId
     * @param workflowParams
     * @return a futuretask starting processing
     */
    public FutureTask startProcessing(String mediaPackageXML, String workflowId, Map<String, String> workflowParams);

    /**
     * stop processing
     *
     * @return a futuretask stopping processing
     */
    public FutureTask stopProcessing();

    /**
     * reload the service
     */
    public void reload();

    /**
     * get the instance xml file
     */
    public String getInstanceXML(String mediapackageID);
}
