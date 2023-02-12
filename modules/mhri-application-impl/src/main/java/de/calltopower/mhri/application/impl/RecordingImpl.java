/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.application.impl;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import de.calltopower.mhri.application.api.Inbox;
import de.calltopower.mhri.application.api.Recording;
import de.calltopower.mhri.application.api.RecordingFile;
import de.calltopower.mhri.application.api.UploadJob;
import de.calltopower.mhri.util.ParseUtils;

/**
 * RecordingImpl
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class RecordingImpl implements Recording {

    public List<RecordingFile> files = new LinkedList<>();
    public List<RecordingFile> ignoredFiles = new LinkedList<>();
    public DatabaseAdapter master;
    public Inbox inbox;
    public State state;
    public String linkToEngageUI = "";
    private String path;
    private String mediaPackage;
    private String seriesId;
    private int id;
    private final String originalTitle;
    private String title;
    private String workflowId;
    private InboxTask currentTask = null; // the task that is currently conducted for this recording
    private String ingestStatus = "";
    private String ingestDetails = "";
    private int uploadProgress = 0;
    private int currentChunkProgSize = 0;
    private String currentErrorMsg = "";
    private boolean trim = false;

    public RecordingImpl(int id, InboxImpl inbox, State state, String path, String mediapackage, String seriesId, String workflowId, boolean trimFlag) {
        this.id = id;
        this.inbox = inbox;
        this.state = state;
        this.path = path;
        this.mediaPackage = mediapackage;
        this.seriesId = seriesId;
        this.workflowId = workflowId;
        this.trim = trimFlag;
        File f = new File(path);
        this.originalTitle = f.getName();
        this.title = this.originalTitle;
        checkForEpisodeName();
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public String getOriginalTitle() {
        return this.originalTitle;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public void resetTitle() {
        title = originalTitle;
    }

    private String parseTitle(String episode) {
        String str_titleStart = "<dcterms:title>";
        String str_titleEnd = "</dcterms:title>";
        return ParseUtils.getInstance().getFirstAppearanceOf(episode, str_titleStart, str_titleEnd);
    }

    private void checkForEpisodeName() {
        String episode = SpecificFileUtils.getEpisode(this);
        if (!episode.trim().isEmpty()) {
            String _title = parseTitle(episode);
            if (!_title.trim().isEmpty()) {
                this.title = _title;
            }
        }
    }

    public int getCurrentChunkProgSize() {
        return currentChunkProgSize;
    }

    public void setCurrentChunkProgSize(int currentChunkProgSize) {
        this.currentChunkProgSize = currentChunkProgSize;
    }

    public void setAllFilesToUnfinished() {
        for (RecordingFile file : files) {
            UploadJob fileJob = file.getUploadJob();
            fileJob.setState(UploadJob.State.UNINITIALIZED);
            fileJob.setCurrentChunk(0);
        }
    }

    public List<RecordingFile> getUnfinishedFiles() {
        List<RecordingFile> out = new LinkedList<>();
        for (RecordingFile file : files) {
            UploadJob fileJob = file.getUploadJob();
            if (!ignoredFiles.contains(file)) {
                if (fileJob == null) {
                    out.add(file);
                } else if (fileJob.getState().equals(UploadJob.State.UNINITIALIZED)) {
                    out.add(file);
                }
            }
        }
        return out;
    }

    public boolean ignorePendingFile(RecordingFile rfile) {
        for (RecordingFile file : files) {
            if(rfile.equals(file)) {
                ignoredFiles.add(file);
                return true;
            }
        }
        return false;
    }

    private void save() {
        master.updateRecording(this);
    }

    public boolean hasCurrentTask() {
        return currentTask != null;
    }

    public InboxTask getCurrentTask() {
        return currentTask;
    }

    public void setCurrentTask(InboxTask currentTask) {
        this.currentTask = currentTask;
    }

    // getter
    @Override
    public int getId() {
        return id;
    }

    @Override
    public Inbox getInbox() {
        return inbox;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void setSeriesId(String seriesId) {
        this.seriesId = seriesId;
        save();
    }

    @Override
    public String getWorkflowId() {
        return workflowId;
    }

    @Override
    public String getMediaPackage() {
        return mediaPackage;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public boolean getTrim() {
        return trim;
    }

    @Override
    public String getErrorMessage() {
        return currentErrorMsg;
    }

    @Override
    public String getIngestStatus() {
        return this.ingestStatus;
    }

    @Override
    public String getIngestDetails() {
        return this.ingestDetails;
    }

    @Override
    public int getUploadProgress() {
        return uploadProgress;
    }

    @Override
    public RecordingFile[] getFiles() {
        RecordingFile[] a = new RecordingFile[files.size()];
        int i = 0;
        for (RecordingFile it : files) {
            a[i] = it;
            ++i;
        }
        return a;
    }

    // setter
    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public void setInbox(Inbox inbox) {
        this.inbox = inbox;
        save();
    }

    @Override
    public void setPath(String path) {
        this.path = path;
        save();
    }

    @Override
    public String getSeriesId() {
        return seriesId;
    }

    @Override
    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
        save();
    }

    @Override
    public void setMediaPackage(String mediaPackage) {
        this.mediaPackage = mediaPackage;
        save();
    }

    @Override
    public void setState(State state) {
        this.state = state;
        save();
    }

    @Override
    public void setTrim(boolean trim) {
        this.trim = trim;
        save();
    }

    @Override
    public void setErrorMessage(String msg) {
        this.currentErrorMsg = msg;
    }

    @Override
    public void setIngestStatus(String status) {
        this.ingestStatus = status;
        save();
    }

    @Override
    public void setIngestDetails(String status) {
        this.ingestDetails = status;
    }

    @Override
    public void setUploadProgress(int i) {
        if (i >= 0) {
            this.uploadProgress = i;
            save();
        }
    }

    @Override
    public void addFile(RecordingFile file) {
        if (!files.contains(file)) {
            files.add(file);
        }
    }

    @Override
    public void removeFile(RecordingFile file) {
        if (files.contains(file)) {
            files.remove(file);
        }
    }
}
