/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.application.impl;

import de.calltopower.mhri.application.api.RemoteInboxModelListener;
import de.calltopower.mhri.application.api.RecordingListModel;
import de.calltopower.mhri.application.api.ListModelAdapter;
import de.calltopower.mhri.application.api.Recording;
import de.calltopower.mhri.application.api.Inbox;
import java.util.LinkedList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import de.calltopower.mhri.application.api.Recording.State;

/**
 * ListModelAdapterImpl
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class ListModelAdapterImpl implements ListModelAdapter, RemoteInboxModelListener {

    private final DatabaseAdapter db;
    private final DefaultListModel inboxListModel = new DefaultListModel();
    private final List<RecordingListModelImpl> recordingListModels = new LinkedList<>();
    private State filter = null;

    ListModelAdapterImpl(DatabaseAdapter db) {
        this.db = db;
        for (Integer key : db.getInboxes().keySet()) {
            Inbox inbox = db.getInboxes().get(key);
            inboxListModel.addElement(inbox);
        }
    }

    @Override
    public void resetBefore() {
        setFilter(null);
        db.updateInboxes(true);
        for (RecordingListModelImpl l : recordingListModels) {
            l.reset();
        }
        recordingListModels.clear();
        for (Integer key : db.getInboxes().keySet()) {
            Inbox inbox = db.getInboxes().get(key);
            inboxRemoved(inbox);
        }
        inboxListModel.clear();
    }

    @Override
    public void resetAfter() {
        setFilter(null);
        inboxListModel.clear();
        recordingListModels.clear();
        for (Integer key : db.getInboxes().keySet()) {
            Inbox inbox = db.getInboxes().get(key);
            inboxListModel.addElement(inbox);
        }
        db.updateInboxes(false);
    }

    // getter
    @Override
    public ListModel getInboxListModel() {
        return inboxListModel;
    }

    @Override
    public RecordingListModel getRecordingListModel() {
        RecordingListModelImpl listModel = new RecordingListModelImpl(db);
        recordingListModels.add(listModel);
        return listModel;
    }

    // misc
    @Override
    public void inboxCreated(Inbox inbox) {
        if (inbox != null) {
            inboxListModel.addElement(inbox);
        }
    }

    @Override
    public void inboxModified(Inbox inbox) {
        // if(inbox != null) {
        // TODO exchange DefaultListModel?
        // }
    }

    @Override
    public void inboxRemoved(Inbox inbox) {
        if (inbox != null) {
            inboxListModel.removeElement(inbox);
        }
    }

    @Override
    public void recordingCreated(Recording recording) {
        if (recording != null) {
            for (RecordingListModelImpl it : recordingListModels) {
                it.recordingAdded(recording);
            }
        }
    }

    @Override
    public void recordingModified(Recording recording) {
        if (recording != null) {
            for (RecordingListModelImpl it : recordingListModels) {
                it.recordingModified(recording);
            }
        }
    }

    @Override
    public void recordingRemoved(Recording recording) {
        if (recording != null) {
            for (RecordingListModelImpl it : recordingListModels) {
                it.recordingRemoved(recording);
            }
        }
    }

    protected void setFilter(State filter) {
        this.filter = filter;
        for (RecordingListModelImpl it : recordingListModels) {
            it.setFilter(filter);
        }
    }
}
