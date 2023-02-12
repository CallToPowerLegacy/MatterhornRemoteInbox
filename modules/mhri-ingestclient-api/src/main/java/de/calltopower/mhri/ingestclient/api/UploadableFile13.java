/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.ingestclient.api;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * UploadableFile for MH 1.3
 *
 * @date 24.09.2012
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public interface UploadableFile13 {

    /**
     * get the current state
     *
     * @return the current state
     * @throws URISyntaxException
     * @throws IngestClientException
     */
    public String getState() throws URISyntaxException, IngestClientException;

    /**
     * flag whether UploadableFile currently uploads
     *
     * @return true when UploadableFile currently uploads, false else
     * @throws IngestClientException
     */
    public boolean isUploading() throws IngestClientException;

    /**
     * uploads
     *
     * @return the state
     * @throws FileNotFoundException
     * @throws IOException
     * @throws URISyntaxException
     * @throws IngestClientException
     */
    public String upload() throws FileNotFoundException, IOException, URISyntaxException, IngestClientException;

    /**
     * stop uploading
     */
    public void stopUpload();
}
