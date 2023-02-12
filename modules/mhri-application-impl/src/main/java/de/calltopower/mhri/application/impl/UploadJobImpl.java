/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.application.impl;

import de.calltopower.mhri.application.api.RecordingFile;
import de.calltopower.mhri.application.api.UploadJob;

/**
 * UploadJobImpl
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class UploadJobImpl implements UploadJob {

    private int id;
    private State state;
    private String jobId;
    private long chunkSize;
    private long chunksTotal;
    private long currentChunk;
    private RecordingFile file;
    private DatabaseAdapter master;

    public UploadJobImpl(int id, String jobId, long chunkSize, long chunksTotal, long currentChunk, RecordingFile file) {
        this.id = id;
        this.state = State.UNINITIALIZED;
        this.jobId = jobId;
        this.chunkSize = chunkSize;
        this.chunksTotal = chunksTotal;
        this.currentChunk = currentChunk;
        this.file = file;
    }

    private void save() {
        master.updateUploadJob(this);
    }

    public DatabaseAdapter getDatabaseAdapter() {
        return master;
    }

    public void setDatabaseAdapter(DatabaseAdapter dbadapter) {
        master = dbadapter;
    }

    public RecordingFile getFile() {
        return file;
    }

    public void setFile(RecordingFile rfile) {
        file = rfile;
    }

    // getter
    @Override
    public State getState() {
        return state;
    }

    @Override
    public long getChunkSize() {
        return chunkSize;
    }

    @Override
    public long getTotalChunks() {
        return chunksTotal;
    }

    @Override
    public long getCurrentChunk() {
        return currentChunk;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getJobId() {
        return jobId;
    }

    public void saveStateWOSave(State state) {
        this.state = state;
    }

    // setter
    @Override
    public void setState(State state) {
        this.state = state;
        save();
    }

    @Override
    public void setChunkSize(long chunkSize) {
        this.chunkSize = chunkSize;
        save();
    }

    @Override
    public void setTotalChunks(long totalChunks) {
        this.chunksTotal = totalChunks;
        save();
    }

    @Override
    public void setCurrentChunk(long currentChunk) {
        this.currentChunk = currentChunk;
        save();
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public void setJobId(String jobId) {
        this.jobId = jobId;
        save();
    }
}
