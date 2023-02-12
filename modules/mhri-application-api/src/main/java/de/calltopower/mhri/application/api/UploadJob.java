/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.application.api;

/**
 * UploadJob
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public interface UploadJob {

    /**
     * States of UploadJob
     */
    public enum State {

        UNINITIALIZED, INPROGRESS, FINALIZING, COMPLETE
    }

    /**
     * get the state
     *
     * @return
     */
    State getState();

    /**
     * get the chunk size
     *
     * @return the chunk size
     */
    long getChunkSize();

    /**
     * get total number of chunks
     *
     * @return total number of chunks
     */
    long getTotalChunks();

    /**
     * get current chunk
     *
     * @return current chunk
     */
    long getCurrentChunk();

    /**
     * get the ID
     *
     * @return the ID
     */
    int getId();

    /**
     * get the job ID
     *
     * @return the job ID
     */
    String getJobId();

    /**
     * set the state
     *
     * @param state
     */
    void setState(State state);

    /**
     * set the chunk size
     *
     * @param chunkSize
     */
    void setChunkSize(long chunkSize);

    /**
     * set the total number of chunks
     *
     * @param totalChunks
     */
    void setTotalChunks(long totalChunks);

    /**
     * set the current chunk
     *
     * @param currentChunk
     */
    void setCurrentChunk(long currentChunk);

    /**
     * set the ID
     *
     * @param id
     */
    void setId(int id);

    /**
     * set the job ID
     *
     * @param jobId
     */
    void setJobId(String jobId);
}
