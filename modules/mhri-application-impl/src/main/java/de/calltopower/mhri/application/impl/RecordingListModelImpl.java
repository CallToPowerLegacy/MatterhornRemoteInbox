/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.application.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import de.calltopower.mhri.application.api.Inbox;
import de.calltopower.mhri.application.api.Recording;
import de.calltopower.mhri.application.api.Recording.State;
import de.calltopower.mhri.application.api.RecordingListModel;

/**
 * RecordingListModelImpl
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class RecordingListModelImpl implements RecordingListModel {

    private final DatabaseAdapter db;
    private final List<Recording> recordings = new LinkedList<>();
    private final List<Recording> recordingsFiltered = new LinkedList<>();
    private final List<ListDataListener> listeners = new LinkedList<>();
    private Inbox inbox;
    private State filter = null;

    public RecordingListModelImpl(DatabaseAdapter db) {
        this.db = db;
    }

    private void filter() {
        recordingsFiltered.clear();
        for (Recording r : recordings) {
            if (filter != null) {
                if (filter == r.getState()) {
                    recordingsFiltered.add(r);
                }
            } else {
                recordingsFiltered.add(r);
            }
        }
        Collections.sort(recordingsFiltered, new RecordingsComparator());
        int isize = recordings.size() > 0 ? recordings.size() - 1 : 0;
        ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, isize);
        for (ListDataListener listener : listeners) {
            listener.intervalRemoved(event);
        }
    }

    public void filter(State filter) {
        this.filter = filter;
        filter();
    }

    public void filterAlphabetically() {
        setFilter(null);
    }

    public void reset() {
        for (Recording r : recordings) {
            int i = recordings.indexOf(r);
            recordings.remove(r);
            ListDataEvent evt = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, i, i);
            for (ListDataListener it : listeners) {
                it.intervalRemoved(evt);
            }
        }
        recordings.clear();
        filter(null);
    }

    @Override
    public final void setInbox(Inbox inbox) {
        this.inbox = inbox;
        int isize = recordings.size() > 0 ? recordings.size() - 1 : 0;
        ListDataEvent removedEvent = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, 0, isize);
        recordings.clear();
        for (int it : db.getRecordings().keySet()) {
            RecordingImpl recording = db.getRecordings().get(it);
            if (recording.inbox == inbox) {
                recordings.add((Recording) recording);
            }
        }
        ListDataEvent addedEvent = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, 0, isize);
        for (ListDataListener listener : listeners) {
            listener.intervalRemoved(removedEvent);
            listener.intervalAdded(addedEvent);
        }
        filter();
    }

    @Override
    public int getSize() {
        return recordingsFiltered.size();
    }

    @Override
    public Object getElementAt(int index) {
        if ((index >= 0) && (index < recordingsFiltered.size())) {
            return recordingsFiltered.get(index);
        } else {
            return null;
        }
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        listeners.add(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        listeners.remove(l);
    }

    public List<Recording> getRecordings() {
        return recordings;
    }

    protected void setFilter(State filter) {
        this.filter = filter;
        filter();
    }

    protected void recordingAdded(Recording recording) {
        if (inbox == recording.getInbox()) {
            recordings.add(recording);
            int i = recordings.indexOf(recording);
            ListDataEvent evt = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, i, i);
            for (ListDataListener it : listeners) {
                it.intervalAdded(evt);
            }
        }
        filter();
    }

    protected void recordingModified(Recording recording) {
        if (recordings.contains(recording)) {
            int i = recordings.indexOf(recording);
            ListDataEvent evt = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, i, i);
            for (ListDataListener it : listeners) {
                it.contentsChanged(evt);
            }
        }
        filter();
    }

    protected void recordingRemoved(Recording recording) {
        if (recordings.contains(recording)) {
            int i = recordings.indexOf(recording);
            recordings.remove(recording);
            ListDataEvent evt = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, i, i);
            for (ListDataListener it : listeners) {
                it.intervalRemoved(evt);
            }
        }
        filter();
    }
}
