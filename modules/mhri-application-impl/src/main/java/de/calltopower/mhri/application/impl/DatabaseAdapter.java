/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.application.impl;

import de.calltopower.mhri.application.api.RemoteInboxModelListener;
import de.calltopower.mhri.application.api.RecordingFile;
import de.calltopower.mhri.application.api.Recording;
import de.calltopower.mhri.application.api.UploadJob;
import de.calltopower.mhri.application.api.Inbox;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import de.calltopower.mhri.application.api.Recording.State;
import de.calltopower.mhri.application.api.RecordingFile.Type;
import de.calltopower.mhri.util.Constants;

/**
 * DatabaseAdapter
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public final class DatabaseAdapter {

    private static final Logger logger = Logger.getLogger(DatabaseAdapter.class);
    private Connection dbconn;
    private final Map<Integer, Inbox> inboxes = new HashMap<>();
    private final Map<Integer, RecordingImpl> recordings = new HashMap<>();
    private final Map<Integer, RecordingFileImpl> files = new HashMap<>();
    private final Map<Integer, UploadJobImpl> uploadJobs = new HashMap<>();
    private final Map<String, Object> itemsByPath = new TreeMap<>();
    private PreparedStatement insertInbox, insertRecording, insertFile, insertUploadJob;
    private PreparedStatement updateInbox, updateRecording, updateFile, updateUploadJob;
    private PreparedStatement deleteInbox, deleteRecording, deleteFile, deleteUploadJob;
    private PreparedStatement dropInbox, dropRecording, dropFile, dropUploadJob;
    private final List<RemoteInboxModelListener> modelListeners = new LinkedList<>();

    private void prepareStatements() throws SQLException {
        if (logger.isInfoEnabled()) {
            logger.info("DatabaseAdapter::prepareStatements - Preparing statements");
        }
        // INSERT
        insertInbox = dbconn.prepareStatement(Constants.SQL_INSERT_INBOX);
        insertRecording = dbconn.prepareStatement(Constants.SQL_INSERT_RECORDING);
        insertFile = dbconn.prepareStatement(Constants.SQL_INSERT_FILE);
        insertUploadJob = dbconn.prepareStatement(Constants.SQL_INSERT_UPLOADJOB);

        // UPDATE
        updateInbox = dbconn.prepareStatement(Constants.SQL_UPDATE_INBOX);
        updateRecording = dbconn.prepareCall(Constants.SQL_UPDATE_RECORDING);
        updateFile = dbconn.prepareStatement(Constants.SQL_UPDATE_FILE);
        updateUploadJob = dbconn.prepareStatement(Constants.SQL_UPDATE_UPLOADJOB);

        // DELETE
        deleteInbox = dbconn.prepareStatement(Constants.SQL_DELETE_INBOX);
        deleteRecording = dbconn.prepareStatement(Constants.SQL_DELETE_RECORDING);
        deleteFile = dbconn.prepareStatement(Constants.SQL_DELETE_FILE);
        deleteUploadJob = dbconn.prepareStatement(Constants.SQL_DELETE_UPLOADJOB);

        // DROP
        dropInbox = dbconn.prepareStatement(Constants.SQL_DROP_TABLE_INBOX);
        dropRecording = dbconn.prepareStatement(Constants.SQL_DROP_TABLE_RECORDING);
        dropFile = dbconn.prepareStatement(Constants.SQL_DROP_TABLE_FILE);
        dropUploadJob = dbconn.prepareStatement(Constants.SQL_DROP_TABLE_UPLOADJOB);
    }

    private List<String> loadSchema() {
        if (logger.isInfoEnabled()) {
            logger.info("DatabaseAdapter::loadSchema - Loading schema");
        }
        List<String> statements = new LinkedList<>();
        BufferedReader ir = null;
        try {
            ir = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/db/dbschema.sql"), "UTF-8"));
            for (;;) {
                String s = ir.readLine();
                if (s != null) {
                    statements.add(s);
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("DatabaseAdapter::loadSchema - Could not read database schema.");
            throw new RuntimeException("Could not read database schema.", e);
        } finally {
            if (ir != null) {
                try {
                    ir.close();
                } catch (IOException ex) {
                    logger.error("IOException: " + ex.getMessage());
                }
            }
        }
        return statements;
    }

    public void dropTables() {
        if (logger.isInfoEnabled()) {
            logger.info("DatabaseAdapter::dropTables - Dropping tables");
        }
        try {
            dropInbox.execute();
            dropRecording.execute();
            dropFile.execute();
            dropUploadJob.execute();
            dbconn.commit();

            inboxes.clear();
            recordings.clear();
            files.clear();
            uploadJobs.clear();
            itemsByPath.clear();
        } catch (SQLException ex) {
            logger.error("DatabaseAdapter::dropTables - SQLException when dropping tables: " + ex.getMessage());
        }
    }

    public DatabaseAdapter() throws Exception {
        construct();
    }

    public void construct() throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("DatabaseAdapter::construct - Initializing");
        }
        Class.forName("org.h2.Driver");
        dbconn = DriverManager.getConnection("jdbc:h2:./database/mhri", "sa", "");
        dbconn.setAutoCommit(false);
        ensureSchema();
        prepareStatements();
    }

    public void shutdown() throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("DatabaseAdapter::shutdown - Shutting down");
        }
        dbconn.close();
    }

    public Map<Integer, RecordingImpl> getRecordings() {
        return recordings;
    }

    public Map<String, Object> getItemsByPath() {
        return itemsByPath;
    }

    public final void ensureSchema() {
        if (logger.isInfoEnabled()) {
            logger.info("DatabaseAdapter::ensureSchema - Checking the current schema");
        }
        List<String> statements = loadSchema();
        for (String sql : statements) {
            try {
                Statement s = dbconn.createStatement();
                s.execute(sql);
            } catch (SQLException e) {
                logger.error("DatabaseAdapter::ensureSchema - Error executing SQL statement: " + sql + "\n" + e.getMessage());
            }
        }
    }

    public void loadState() {
        if (logger.isInfoEnabled()) {
            logger.info("DatabaseAdapter::loadState - Loading the state");
        }
        try (Statement stat = dbconn.createStatement()) {
            // load Inboxes
            if (logger.isInfoEnabled()) {
                logger.info("DatabaseAdapter::loadState - Loading inboxes");
            }
            ResultSet rs = stat.executeQuery(Constants.SQL_LOADALL_INBOXES);
            while (rs.next()) {
                int id = rs.getInt("id");
                String path = rs.getString("path");
                String name = rs.getString("name");
                String sid = rs.getString("seriesId");
                String wid = rs.getString("workflowId");
                if (!itemsByPath.containsKey(path)) {
                    InboxImpl inbox = new InboxImpl(id, path, name, sid, wid);
                    inboxes.put(rs.getInt("id"), inbox);
                    itemsByPath.put(inbox.getPath(), inbox);
                    inbox.setDatabaseAdapter(this);
                }
            }

            // load Recordings
            if (logger.isInfoEnabled()) {
                logger.info("DatabaseAdapter::loadState - Loading recordings");
            }
            rs = stat.executeQuery(Constants.SQL_LOADALL_RECORDINGS);
            while (rs.next()) {
                int parentId = rs.getInt("inbox");
                if (inboxes.containsKey(parentId)) {
                    InboxImpl parent = (InboxImpl) inboxes.get(parentId);
                    int id = rs.getInt("id");
                    String path = rs.getString("path");
                    Recording.State state = Recording.State.valueOf(rs.getString("state"));
                    String mp = rs.getString("mediapackage");
                    String sid = rs.getString("seriesId");
                    String wid = rs.getString("workflowId");
                    boolean trim = (rs.getInt("trimFlag")) == 1;
                    if (!itemsByPath.containsKey(path)) {
                        RecordingImpl r = new RecordingImpl(id, parent, state, path, mp, sid, wid, trim);
                        parent.addRecording(r);
                        recordings.put(id, r);
                        itemsByPath.put(r.getPath(), r);
                        r.master = this;
                    } else {
                        if ((state == State.COMPLETE)
                                || (state == State.FAILED)) {
                            RecordingImpl rimpl = (RecordingImpl) itemsByPath.get(path);
                            rimpl.setState(state);
                        }
                    }
                } else {
                    int id = rs.getInt("id");
                    if (logger.isInfoEnabled()) {
                        logger.info("DatabaseAdapter::loadState - Deleting dangeling Recording: " + id);
                    }
                    deleteRecording.setInt(1, id);
                    deleteRecording.execute();
                }
            }

            // load RecordingFiles
            if (logger.isInfoEnabled()) {
                logger.info("DatabaseAdapter::loadState - Loading recording files");
            }
            rs = stat.executeQuery(Constants.SQL_LOADALL_FILES);
            while (rs.next()) {
                int parentId = rs.getInt("recording");
                if (recordings.containsKey(parentId)) {
                    RecordingImpl parent = recordings.get(parentId);
                    int id = rs.getInt("id");
                    String path = rs.getString("path");
                    RecordingFile.Type type = RecordingFile.Type.valueOf(rs.getString("type"));
                    String flavor = rs.getString("flavor");
                    if (!itemsByPath.containsKey(path)) {
                        RecordingFileImpl f = new RecordingFileImpl(id, parent, path, type, flavor);
                        parent.files.add(f);
                        files.put(id, f);
                        itemsByPath.remove(f.getPath());
                        itemsByPath.put(f.getPath(), f);
                        f.setDatabaseAdapter(this);
                    }
                } else {
                    int id = rs.getInt("id");
                    if (logger.isInfoEnabled()) {
                        logger.info("DatabaseAdapter::loadState - Deleting dangeling RecordingFile: " + id);
                    }
                    deleteFile.setInt(1, id);
                    deleteFile.execute();
                }
            }

            // load UploadJobs
            rs = stat.executeQuery(Constants.SQL_LOADALL_UPLOADJOBS);
            if (logger.isInfoEnabled()) {
                logger.info("DatabaseAdapter::loadState - Loading upload jobs");
            }
            while (rs.next()) {
                int parentId = rs.getInt("file");
                if (files.containsKey(parentId)) {
                    RecordingFileImpl parent = files.get(parentId);
                    int id = rs.getInt("id");
                    String jid = rs.getString("jobId");
                    long chunkSize = rs.getLong("chunkSize");
                    long chunksTotal = rs.getLong("chunksTotal");
                    long currChunk = rs.getLong("currentChunk");
                    UploadJob.State state = UploadJob.State.valueOf(rs.getString("state"));
                    UploadJobImpl j = new UploadJobImpl(id, jid, chunkSize, chunksTotal, currChunk, parent);
                    j.saveStateWOSave(state);
                    parent.setUploadJob(j);
                    uploadJobs.put(id, j);
                    j.setDatabaseAdapter(this);
                } else {
                    int id = rs.getInt("id");
                    if (logger.isInfoEnabled()) {
                        logger.info("DatabaseAdapter::loadState - Deleting dangeling UploadJob: " + id);
                    }
                    deleteUploadJob.setInt(1, id);
                    deleteUploadJob.execute();
                }
            }

        } catch (SQLException e) {
            logger.error("DatabaseAdapter::loadState - SQL ERROR: " + e.getMessage());
        }
    }

    public Map<Integer, Inbox> getInboxes() {
        return inboxes;
    }

    public boolean hasInbox(String path) {
        return itemsByPath.containsKey(path) && (itemsByPath.get(path) instanceof InboxImpl);
    }

    public Inbox getInboxByPath(String path) {
        if (itemsByPath.get(path) instanceof Inbox) {
            return (Inbox) itemsByPath.get(path);
        } else {
            return null;
        }
    }

    public Inbox createInbox(String path, String name) throws SQLException {
        if (logger.isInfoEnabled()) {
            logger.info("DatabaseAdapter::createInbox - Creating inbox " + name + " at " + path);
        }
        InboxImpl inbox = null;
        try {
            insertInbox.setString(1, path);
            insertInbox.setString(2, name);
            insertInbox.setString(3, "");
            insertInbox.setString(4, "");
            insertInbox.executeUpdate();
            ResultSet rs = insertInbox.getGeneratedKeys();
            rs.next();
            int id = rs.getInt(1);
            inbox = new InboxImpl(id, path, name, "", "");
            inboxes.put(id, inbox);
            itemsByPath.put(inbox.getPath(), inbox);
            inbox.setDatabaseAdapter(this);
            dbconn.commit();
            for (RemoteInboxModelListener it : modelListeners) {
                it.inboxCreated(inbox);
            }
            if (logger.isInfoEnabled()) {
                logger.info("DatabaseAdapter::createInbox - Successfully added inbox " + path + " as " + inbox.getPath());
            }
        } catch (SQLException e) {
            logger.error("Failed to insert Inbox: " + e.getMessage());
            throw e;
        }
        return inbox;
    }

    public void updateInboxes(boolean removed) {
        if (logger.isInfoEnabled()) {
            logger.info("DatabaseAdapter::updateInboxes - Updating inboxes");
        }
        try {
            for (Inbox inbox : inboxes.values()) {
                try {
                    for (RemoteInboxModelListener it : modelListeners) {
                        for (Recording r : inbox.getRecordings()) {
                            try {
                                if (removed) {
                                    it.recordingRemoved(r);
                                } else {
                                    it.recordingModified(r);
                                }
                            } catch (Exception e) {
                                logger.error("Exception occured while updating the recording '" + r.getTitle() + "': " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Exception occured while updating the inbox '" + inbox.getName() + "': " + e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Exception occured while updating inboxes: " + e.getMessage());
        }
    }

    public void updateInbox(InboxImpl inbox) {
        if (logger.isInfoEnabled()) {
            logger.info("DatabaseAdapter::updateInboxes - Updating inbox " + inbox.getName());
        }
        try {
            updateInbox.setString(1, inbox.getPath());
            updateInbox.setString(2, inbox.getName());
            updateInbox.setString(3, inbox.getSeriesId());
            updateInbox.setString(4, inbox.getWorkflowId());
            updateInbox.setInt(5, inbox.getId());
            updateInbox.executeUpdate();
            dbconn.commit();
            for (RemoteInboxModelListener it : modelListeners) {
                it.inboxModified(inbox);
            }
        } catch (SQLException e) {
            logger.error("DatabaseAdapter::updateInbox - Failed to upadte Inbox: " + e.getMessage());
        }
    }

    public void removeInbox(Inbox inbox) {
        if (logger.isInfoEnabled()) {
            logger.info("DatabaseAdapter::removeInbox - Removing inbox " + inbox.getName());
        }
        try {
            Recording[] rs = inbox.getRecordings();
            for (Recording r : rs) {
                removeRecording((RecordingImpl) r);
            }
            deleteInbox.setInt(1, inbox.getId());
            deleteInbox.execute();
            dbconn.commit();
            inboxes.remove(inbox.getId());
            itemsByPath.remove(inbox.getPath());
            for (RemoteInboxModelListener it : modelListeners) {
                it.inboxRemoved(inbox);
            }
        } catch (SQLException e) {
            logger.error("DatabaseAdapter::removeInbox - Failed to delete Inbox: " + e.getMessage());
        }
    }

    public boolean hasRecording(String path) {
        return itemsByPath.containsKey(path) && (itemsByPath.get(path) instanceof RecordingImpl);
    }

    public Recording getRecordingByPath(String path) {
        if (itemsByPath.get(path) instanceof Recording) {
            return (Recording) itemsByPath.get(path);
        } else {
            return null;
        }
    }

    public Recording createRecording(Inbox parent, String path, String mediaPackage, String seriesId, String workflowId, boolean trim) {
        if (logger.isInfoEnabled()) {
            logger.info("DatabaseAdapter::createRecording");
        }
        RecordingImpl recording = null;
        if (!this.hasRecording(path)) {
            if (logger.isInfoEnabled()) {
                logger.info("DatabaseAdapter::createRecording - Creating recording in inbox " + parent.getName() + " at " + path);
            }
            try {
                insertRecording.setInt(1, parent.getId());
                insertRecording.setString(2, State.RECIEVING.name());
                insertRecording.setString(3, path);
                insertRecording.setString(4, mediaPackage);
                insertRecording.setString(5, seriesId);
                insertRecording.setString(6, workflowId);
                insertRecording.setInt(7, trim ? 1 : 0);
                insertRecording.executeUpdate();
                ResultSet rs = insertRecording.getGeneratedKeys();
                rs.next();
                int id = rs.getInt(1);
                recording = new RecordingImpl(id, (InboxImpl) parent, State.RECIEVING, path, mediaPackage, seriesId, workflowId, trim);
                if (!recordings.containsKey(id)) {
                    parent.addRecording(recording);
                    recordings.put(id, recording);
                    itemsByPath.put(recording.getPath(), recording);
                    recording.master = this;
                    dbconn.commit();
                    for (RemoteInboxModelListener it : modelListeners) {
                        it.recordingCreated(recording);
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to insert Recording: " + e.getMessage());
                throw new RuntimeException("Failed to insert Recording: " + e.getMessage());
            }
        }
        return recording;
    }

    public void updateRecording(RecordingImpl recording) {
        if (logger.isInfoEnabled()) {
            logger.info("DatabaseAdapter::updateRecording - Updating recording " + recording.getTitle());
        }
        try {
            updateRecording.setInt(1, recording.inbox.getId());
            updateRecording.setString(2, recording.state.name());
            updateRecording.setString(3, recording.getPath());
            updateRecording.setString(4, recording.getMediaPackage());
            updateRecording.setString(5, recording.getSeriesId());
            updateRecording.setString(6, recording.getWorkflowId());
            updateRecording.setInt(7, recording.getTrim() ? 1 : 0);
            updateRecording.setInt(8, recording.getId());
            updateRecording.executeUpdate();
            dbconn.commit();
            for (RemoteInboxModelListener it : modelListeners) {
                it.recordingModified(recording);
            }
        } catch (SQLException e) {
            logger.error("DatabaseAdapter::updateRecording - Failed to update Recording: " + e.getMessage());
        }
    }

    public void removeRecording(Recording recording) {
        if (logger.isInfoEnabled()) {
            logger.info("DatabaseAdapter::removeRecording - Removing recording " + recording.getTitle());
        }
        try {
            RecordingFile[] fs = recording.getFiles();
            for (RecordingFile f : fs) {
                removeRecordingFile((RecordingFileImpl) f);
            }
            deleteRecording.setInt(1, recording.getId());
            deleteRecording.execute();
            dbconn.commit();
            recording.getInbox().removeRecording(recording);
            recordings.remove(recording.getId());
            itemsByPath.remove(recording.getPath());
            for (RemoteInboxModelListener it : modelListeners) {
                it.recordingRemoved(recording);
            }
        } catch (SQLException e) {
            logger.error("DatabaseAdapter::removeRecording - Failed to delete Recording: " + e.getMessage());
        }
    }

    public boolean hasRecordingFile(String path) {
        return itemsByPath.containsKey(path) && (itemsByPath.get(path) instanceof RecordingFileImpl);
    }

    public RecordingFile getRecordingFileByPath(String path) {
        if (itemsByPath.get(path) instanceof RecordingFile) {
            return (RecordingFile) itemsByPath.get(path);
        } else {
            return null;
        }
    }

    public RecordingFile createRecordingFile(Recording parent, UploadJob job, String path, Type type, String flavor) {
        if (logger.isInfoEnabled()) {
            logger.info("DatabaseAdapter::createRecordingFile - Creating recording file in recording " + parent.getTitle());
        }
        RecordingFileImpl fo = null;
        try {
            insertFile.setInt(1, parent.getId());
            insertFile.setInt(2, (job == null) ? 0 : job.getId());
            insertFile.setString(3, path);
            insertFile.setString(4, type.name());
            insertFile.setString(5, flavor);
            insertFile.executeUpdate();
            ResultSet rs = insertFile.getGeneratedKeys();
            rs.next();
            int id = rs.getInt(1);
            fo = new RecordingFileImpl(id, (RecordingImpl) parent, path, type, flavor);
            if (job != null) {
                fo.setUploadJob(job);
            }
            parent.addFile(fo);
            files.put(id, fo);
            itemsByPath.put(path, fo);
            fo.setDatabaseAdapter(this);
            dbconn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert RecordingFile: " + e.getMessage());
        }
        return fo;
    }

    public void updateRecordingFile(RecordingFileImpl file) {
        if (logger.isInfoEnabled()) {
            logger.info("DatabaseAdapter::updateRecordingFile - Updating recording file " + file.getPath());
        }
        try {
            updateFile.setInt(1, file.getRecording().getId());
            if (file.getUploadJob() != null) {
                updateFile.setInt(2, file.getUploadJob().getId());
            } else {
                updateFile.setInt(2, 0);
            }
            updateFile.setString(3, file.getPath());
            updateFile.setString(4, file.getType().name());
            updateFile.setString(5, file.getFlavor());
            updateFile.setInt(6, file.getId());
            updateFile.executeUpdate();
            dbconn.commit();
        } catch (SQLException e) {
            logger.error("DatabaseAdapter::updateRecordingFile - Failed to upadte Recording: " + e.getMessage());
        }
    }

    public void removeRecordingFile(RecordingFile file) {
        if (logger.isInfoEnabled()) {
            logger.info("DatabaseAdapter::removeRecordingFile - Removing recording file " + file.getPath());
        }
        try {
            if (file.getUploadJob() != null) {
                removeUploadJob((UploadJobImpl) file.getUploadJob());
            }
            deleteFile.setInt(1, file.getId());
            deleteFile.execute();
            dbconn.commit();
            file.getRecording().removeFile(file);
            files.remove(file.getId());
            itemsByPath.remove(file.getPath());
        } catch (SQLException e) {
            logger.error("DatabaseAdapter::removeRecordingFile - Failed to delete RecordingFile: " + e.getMessage());
        }
    }

    public UploadJob createUploadJob(RecordingFile parent, String jobId, long chunksize) {
        if (logger.isInfoEnabled()) {
            logger.info("DatabaseAdapter::createUploadJob");
        }
        UploadJobImpl job = null;
        if ((parent.getId() >= 0)
                && !jobId.isEmpty()
                && (chunksize >= 0)) {
            if (logger.isInfoEnabled()) {
                logger.info("DatabaseAdapter::createUploadJob - Creating upload job of recording file " + parent.getPath());
            }
            try {
                insertUploadJob.setInt(1, parent.getId());
                insertUploadJob.setString(2, jobId);
                insertUploadJob.setString(3, UploadJob.State.UNINITIALIZED.name());
                insertUploadJob.setLong(4, chunksize);
                insertUploadJob.setLong(5, 0l);
                insertUploadJob.setLong(6, 0l);
                insertUploadJob.executeUpdate();
                ResultSet rs = insertUploadJob.getGeneratedKeys();
                if (rs.next()) {
                    int id = rs.getInt(1);
                    job = new UploadJobImpl(id, jobId, chunksize, 0, 0, parent);
                    job.saveStateWOSave(UploadJob.State.UNINITIALIZED);
                    uploadJobs.put(id, job);
                    job.setDatabaseAdapter(this);
                    dbconn.commit();
                }
            } catch (SQLException e) {
                logger.error("Failed to insert UploadJob (1): " + e.getMessage());
                logger.error(
                        "Parent ID: " + parent.getId()
                        + ", Job ID: " + jobId
                        + ", UploadJob state: " + UploadJob.State.UNINITIALIZED.name()
                        + ", Chunk size: " + chunksize);
                job = null;
                // throw new RuntimeException("Failed to insert UploadJob: " + e.getMessage());
            }
        } else {
            logger.error("Failed to insert UploadJob (2): ");
            logger.error("Parent ID: " + parent.getId() + ", Job ID: " + jobId + ", Chunk size: " + chunksize);
        }
        return job;
    }

    public void updateUploadJob(UploadJobImpl job) {
        if (logger.isInfoEnabled()) {
            logger.info("DatabaseAdapter::updateUploadJob");
        }
        try {
            updateUploadJob.setInt(1, job.getFile().getId());
            updateUploadJob.setString(2, job.getJobId());
            updateUploadJob.setString(3, job.getState().name());
            updateUploadJob.setLong(4, job.getChunkSize());
            updateUploadJob.setLong(5, job.getTotalChunks());
            updateUploadJob.setLong(6, job.getCurrentChunk());
            updateUploadJob.setInt(7, job.getId());
            updateUploadJob.executeUpdate();
            dbconn.commit();
            // for (RemoteInboxModelListener it : modelListeners) {
            // it..recordingModified(job.file.getRecording());
            // }
        } catch (SQLException e) {
            logger.error("DatabaseAdapter::updateUploadJob - Failed to upadte Recording: " + e.getMessage());
        }
    }

    public void removeUploadJob(UploadJobImpl job) {
        if (logger.isInfoEnabled()) {
            logger.info("DatabaseAdapter::removeUploadJob");
        }
        try {
            deleteUploadJob.setInt(1, job.getId());
            deleteUploadJob.execute();
            uploadJobs.remove(job.getId());
            dbconn.commit();
        } catch (SQLException e) {
            logger.error("DatabaseAdapter::removeUploadJob - Failed to delete UploadJob: " + e.getMessage());
        }
    }

    public void addModelListener(RemoteInboxModelListener listener) {
        modelListeners.add(listener);
    }

    public void removeModelListener(RemoteInboxModelListener listener) {
        modelListeners.remove(listener);
    }
}
