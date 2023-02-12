/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.application.impl;

import java.util.LinkedList;
import java.util.List;
import de.calltopower.mhri.application.api.Inbox;
import de.calltopower.mhri.application.api.Recording;
import de.calltopower.mhri.util.ParseUtils;

/**
 * InboxImpl
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class InboxImpl implements Inbox {

    private int id;             // (DB) ID of this Inbox
    private String path;        // directory of this Inbox
    private String name;        // friendly name of this Inbox
    private String seriesId;    // Matterhorn ID of default series for recordings from this Inbox
    private String seriesTitle;
    private String workflowId;  // ID of Matterhorn workflow that is applied by default to recordings from this Inbox
    private final List<Recording> recordings = new LinkedList<>();
    private DatabaseAdapter master;
    private final String str_titleStart = "<dcterms:title>";
    private final String str_titleEnd = "</dcterms:title>";

    private void save() {
        master.updateInbox(this);
    }

    public InboxImpl(int id, String path, String name, String seriesId, String workflowId) {
        this.id = id;
        this.path = path;
        this.name = name;
        this.seriesId = seriesId;
        this.seriesTitle = "";
        this.workflowId = workflowId;
    }

    public DatabaseAdapter getDatabaseAdapter() {
        return master;
    }

    public void setDatabaseAdapter(DatabaseAdapter dba) {
        this.master = dba;
    }
    
    @Override
    public void updateSeriesTitle() {
        if(seriesTitle.isEmpty()) {
            String s = ParseUtils.getInstance().getFirstAppearanceOf(SpecificFileUtils.getSeries(this), str_titleStart, str_titleEnd);
            setSeriesTitle(s);
        }
    }

    @Override
    public String toString() {
        return name;
    }

    // getter
    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getSeriesId() {
        return seriesId;
    }

    @Override
    public String getSeriesTitle() {
        return seriesTitle;
    }

    @Override
    public String getWorkflowId() {
        return workflowId;
    }

    @Override
    public Recording[] getRecordings() {
        Recording[] a = new Recording[recordings.size()];
        int i = 0;
        for (Recording it : recordings) {
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
    public void setName(String name) {
        this.name = name;
        save();
    }

    @Override
    public void setPath(String path) {
        this.path = path;
        save();
    }

    @Override
    public void setSeriesId(String seriesId) {
        this.seriesId = seriesId;
        save();
    }

    @Override
    public void setSeriesTitle(String seriesTitle) {
        this.seriesTitle = seriesTitle;
    }

    @Override
    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
        save();
    }

    // misc
    @Override
    public void addRecording(Recording r) {
        recordings.add(r);
    }

    @Override
    public void removeRecording(Recording r) {
        recordings.remove(r);
    }
}
