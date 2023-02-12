/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.ingestclient.api;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * UploadableFile
 *
 * @date 18.05.2012
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public interface UploadableFile {

    /**
     * get the chunk size
     *
     * @return chunk size
     */
    public int getChunkSize();

    /**
     * get the number of chunks
     *
     * @return number of chunks
     */
    public int getNumberOfChunks();

    /**
     * get the number of chunks already uploaded
     *
     * @return the number of chunks already uploaded
     */
    public int getNumberOfChunksUploaded();

    /**
     * get the number of bytes already uploaded
     *
     * @return the number of bytes already uploaded
     */
    public long getNumberOfBytesUploaded();

    /**
     * get the number of remaining bytes to upload
     *
     * @return the number of remaining bytes to upload
     */
    public long getNumberOfRemainingBytes();

    /**
     * get the track URL
     *
     * @return the track URL
     * @throws URISyntaxException
     * @throws IngestClientException
     */
    public String getTrackURL() throws URISyntaxException, IngestClientException;

    /**
     * get the current state
     *
     * @return the current state
     * @throws URISyntaxException
     * @throws IngestClientException
     */
    public String getState() throws URISyntaxException, IngestClientException;

    /**
     * set the current chunk
     *
     * @param newCurrentChunk
     */
    public void setCurrentChunk(int newCurrentChunk);

    /**
     * flag whether UploadableFile currently uploads
     *
     * @return true when UploadableFile currently uploads, false else
     * @throws IngestClientException
     */
    public boolean isUploading() throws IngestClientException;

    /**
     * flag whether UploadableFile has been fully uploaded
     *
     * @return true when UploadableFile has been fully uploaded, false else
     */
    public boolean isFullyUploaded();

    /**
     * generates a new job ID
     *
     * @return a new job ID
     * @throws IOException
     * @throws URISyntaxException
     * @throws RuntimeException
     * @throws IngestClientException
     */
    public String generateNewJobID() throws IOException, URISyntaxException, RuntimeException, IngestClientException;

    /**
     * uploads the next chunk
     *
     * @throws FileNotFoundException
     * @throws IOException
     * @throws URISyntaxException
     * @throws IngestClientException
     */
    public void uploadNextChunk() throws FileNotFoundException, IOException, URISyntaxException, IngestClientException;

    /**
     * stop uploading
     */
    public void stopUpload();
}
