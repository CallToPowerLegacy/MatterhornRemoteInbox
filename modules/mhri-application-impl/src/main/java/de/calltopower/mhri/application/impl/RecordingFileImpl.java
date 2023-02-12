/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.application.impl;

import de.calltopower.mhri.application.api.Recording;
import de.calltopower.mhri.application.api.RecordingFile;
import de.calltopower.mhri.application.api.UploadJob;

/**
 * RecordingFileImpl
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class RecordingFileImpl implements RecordingFile {

    private int id;
    private Recording recording;
    private UploadJob uploadJob = null;
    private String path;
    private Type type;
    private String flavor;
    private DatabaseAdapter master;

    public RecordingFileImpl(int id, RecordingImpl recording, String path, Type type, String flavor) {
        this.id = id;
        this.recording = recording;
        this.path = path;
        this.type = type;
        this.flavor = flavor;
    }

    private void save() {
        master.updateRecordingFile(this);
    }

    public DatabaseAdapter getDatabaseAdapter() {
        return master;
    }

    public void setDatabaseAdapter(DatabaseAdapter dbadapter) {
        master = dbadapter;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public Recording getRecording() {
        return recording;
    }

    @Override
    public void setRecording(Recording recording) {
        this.recording = recording;
        save();
    }

    @Override
    public UploadJob getUploadJob() {
        return uploadJob;
    }

    @Override
    public void setUploadJob(UploadJob uploadJob) {
        this.uploadJob = uploadJob;
        save();
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void setPath(String path) {
        this.path = path;
        save();
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void setType(Type type) {
        this.type = type;
        save();
    }

    @Override
    public String getFlavor() {
        return flavor;
    }

    @Override
    public void setFlavor(String flavor) {
        this.flavor = flavor;
        save();
    }
}
