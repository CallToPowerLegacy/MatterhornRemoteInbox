/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.application.impl;

import de.calltopower.mhri.application.api.RecordingFile;
import de.calltopower.mhri.application.api.ListModelAdapter;
import de.calltopower.mhri.application.api.Recording;
import de.calltopower.mhri.application.api.UploadJob;
import de.calltopower.mhri.application.api.Inbox;
import de.calltopower.mhri.application.api.RemoteInboxApplication;
import de.calltopower.mhri.directorywatch.api.events.FileDeletedEventListener;
import de.calltopower.mhri.directorywatch.api.events.FileCreatedEvent;
import de.calltopower.mhri.directorywatch.api.events.FileDeletedEvent;
import de.calltopower.mhri.directorywatch.api.events.FileModifiedEvent;
import de.calltopower.mhri.directorywatch.api.events.FileModifiedEventListener;
import de.calltopower.mhri.directorywatch.api.events.FileCreatedEventListener;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import de.calltopower.mhri.util.VersionUtils;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.log4j.Logger;
import de.calltopower.mhri.application.api.Recording.State;
import de.calltopower.mhri.application.impl.InboxTask.Type;
import de.calltopower.mhri.directorywatch.api.DirectoryWatch;
import de.calltopower.mhri.ingestclient.api.IngestClient;
import de.calltopower.mhri.ingestclient.api.IngestClient.InstanceState;
import de.calltopower.mhri.ingestclient.api.IngestClientController;
import de.calltopower.mhri.ingestclient.api.IngestClientException;
import de.calltopower.mhri.updater.Updater;
import de.calltopower.mhri.util.Constants;
import de.calltopower.mhri.util.MHRIFileUtils;
import de.calltopower.mhri.util.ParseUtils;
import de.calltopower.mhri.util.conf.Configuration;
import org.osgi.service.component.ComponentContext;

/**
 * RemoteInboxApplicationImpl
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
@Component(name = "de.calltopower.mhri.application.impl", immediate = true)
@Service
public class RemoteInboxApplicationImpl implements
        RemoteInboxApplication,
        FileCreatedEventListener,
        FileModifiedEventListener,
        FileDeletedEventListener {

    private static final Logger logger = Logger.getLogger(RemoteInboxApplicationImpl.class);
    @Reference
    private DirectoryWatch dirWatcher;
    @Reference
    private Configuration config;
    @Reference
    private IngestClientController ingest;
    private VersionUtils vt;
    protected DatabaseAdapter db;
    protected RemoteInboxModel model;
    protected ListModelAdapterImpl listModelAdapter;
    private ExecutorService pool_stateChecker;
    protected ScheduledExecutorService pool_executor;
    protected Map<String, Long> recievingRecordings = new ConcurrentHashMap<>();
    protected List<String> deletedRecordings = Collections.synchronizedList(new LinkedList<String>());
    protected List<Recording> recordingsToCheck = Collections.synchronizedList(new LinkedList<Recording>());
    protected Queue<Recording> scheduledRecordings = new LinkedBlockingQueue<>();
    protected List<Recording> activeRecordings = Collections.synchronizedList(new LinkedList<Recording>());
    protected Map<String, Integer> failedRecordings = new HashMap<>();
    private String currentInboxName = "";
    private final int progressBarStartAt = 10;
    private final int progressBarDownloadStopAt = 90;
    private final int numberOfDeletionTries = 20;
    // from config
    private int pollTime = 5;
    private int tmpFileCheckRate = 2; // hours
    private int stateCheckerCompletedRate = 30;
    private final int stateCheckerActiveRate = 10;
    private int conductorRate = 3;
    private int ingestFailedRecordingsTries = 2;
    private int maxConcurrentUploads = 3;
    private int renamingTimeout = 1000;
    private int updateTries = 3;
    public boolean databaseError = false;

    protected void activate(ComponentContext cc) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::activate - Activating");
        }
        loadConfiguration();
        try {
            db = new DatabaseAdapter();
        } catch (Exception ex) {
            logger.error("RemoteInboxApplicationImpl::activate - Database error: " + ex.getMessage());
            try {
                databaseError = true;
                db.dropTables();
                db.shutdown();
                db.construct();
            } catch (SQLException e) {
                logger.error("RemoteInboxApplicationImpl::activate - SQL exception: " + e.getMessage());
                try {
                    db.shutdown();
                } catch (SQLException es) {
                    logger.error("RemoteInboxApplicationImpl::activate - SQLException 1.1: " + es.getMessage());
                } catch (Exception es) {
                    logger.error("RemoteInboxApplicationImpl::activate - Exception 1.1: " + es.getMessage());
                }
                try {
                    MHRIFileUtils.getInstance().delete(new File("database"));
                } catch (Exception es) {
                    logger.error("RemoteInboxApplicationImpl::activate - Exception 2.1: " + es.getMessage());
                }
                try {
                    db = new DatabaseAdapter();
                } catch (SQLException es) {
                    logger.error("RemoteInboxApplicationImpl::activate - SQLException 3.1: " + es.getMessage());
                } catch (Exception es) {
                    logger.error("RemoteInboxApplicationImpl::activate - Exception 3.1: " + es.getMessage());
                }
            } catch (Exception e) {
                logger.error("RemoteInboxApplicationImpl::activate - Exception: " + e.getMessage());
                try {
                    db.shutdown();
                } catch (SQLException es) {
                    logger.error("RemoteInboxApplicationImpl::activate - SQLException 4.1: " + es.getMessage());
                } catch (Exception es) {
                    logger.error("RemoteInboxApplicationImpl::activate - Exception 4.1: " + es.getMessage());
                }
                try {
                    MHRIFileUtils.getInstance().delete(new File("database"));
                } catch (Exception es) {
                    logger.error("RemoteInboxApplicationImpl::activate - Exception 5.1: " + es.getMessage());
                }
                try {
                    db = new DatabaseAdapter();
                } catch (SQLException es) {
                    logger.error("RemoteInboxApplicationImpl::activate - SQLException 6.1: " + es.getMessage());
                } catch (Exception es) {
                    logger.error("RemoteInboxApplicationImpl::activate - Exception 6.1: " + es.getMessage());
                }
            }
        }

        model = new RemoteInboxModel(db, this);

        File mainDir = ensureMainDirectory();
        model.setMainDir(mainDir);
        ensureDefaultInbox();

        model.ensureConsistency();
        listModelAdapter = new ListModelAdapterImpl(db);

        db.addModelListener(listModelAdapter);

        dirWatcher.addFileCreatedEventListener(this);
        dirWatcher.addFileModifiedEventListener(this);
        dirWatcher.addFileDeletedEventListener(this);
        String path = model.getMainDir().getPath();
        dirWatcher.setMainDirectory(path);
        initWorkers();

        resurrectRecordings();
        vt = new VersionUtils();

        for (int it : db.getRecordings().keySet()) {
            Recording recording = db.getRecordings().get(it);
            if (recording.getState().equals(State.COMPLETE)) {
                recordingsToCheck.add(recording);
            }
        }

        pool_stateChecker = Executors.newFixedThreadPool(5);
    }

    protected void deactivate(ComponentContext cc) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::deactivate - Deactivating");
        }
        dirWatcher.removeFileCreatedEventListener(this);
        dirWatcher.removeFileModifiedEventListener(this);
        dirWatcher.removeFileDeletedEventListener(this);
        pool_stateChecker.shutdown();
        pool_executor.shutdown();

        deletedRecordings.clear();
        recordingsToCheck.clear();
        scheduledRecordings.clear();
        activeRecordings.clear();
        failedRecordings.clear();

        db.shutdown();
    }

    public boolean inDatabase(Recording r) {
        return db.hasRecording(r.getPath());
    }

    public void registerDirectory(String path) {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::registerDirectory - Registering directory " + path);
        }
        try {
            dirWatcher.addDirectory(path);
        } catch (IOException e) {
            logger.error("RemoteInboxApplicationImpl::registerDirectory - Failed to register " + path + " with directory watcher: " + e.getMessage());
        }
    }

    public void restart(boolean resetRecordings, boolean inboxChanged) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::restart - Restarting. Reset recordings: " + resetRecordings + ", inbox changed: " + inboxChanged);
        }
        if (resetRecordings || inboxChanged) {
            try {
                pool_stateChecker.shutdown();
                pool_executor.shutdown();
            } catch (Exception ex) {
                logger.error("RemoteInboxApplicationImpl::restart (1): " + ex.getMessage());
            }
            for (int it : db.getRecordings().keySet()) {
                try {
                    Recording recording = db.getRecordings().get(it);
                    fullyStopRecordingTask(recording);
                } catch (Exception ex) {
                    logger.error("RemoteInboxApplicationImpl::restart (2): " + ex.getMessage());
                }
            }
        }

        loadConfiguration();

        if (inboxChanged) {
            try {
                dirWatcher.reset();
            } catch (Exception ex) {
                logger.error("RemoteInboxApplicationImpl::restart (3): " + ex.getMessage());
            }

            try {
                listModelAdapter.resetBefore();
            } catch (Exception ex) {
                logger.error("RemoteInboxApplicationImpl::restart (4): " + ex.getMessage());
            }

            try {
                db.dropTables();
                db.shutdown();
                db.construct();
            } catch (Exception ex) {
                logger.error("RemoteInboxApplicationImpl::restart (5): " + ex.getMessage());
            }

            model = new RemoteInboxModel(db, this);

            try {
                File mainDir = ensureMainDirectory();
                model.setMainDir(mainDir);
                ensureDefaultInbox();
            } catch (Exception ex) {
                logger.error("RemoteInboxApplicationImpl::restart (6): " + ex.getMessage());
            }

            try {
                model.ensureConsistency();
            } catch (Exception ex) {
                logger.error("RemoteInboxApplicationImpl::restart (7): " + ex.getMessage());
            }

            try {
                listModelAdapter.resetAfter();
            } catch (Exception ex) {
                logger.error("RemoteInboxApplicationImpl::restart (8): " + ex.getMessage());
            }

            try {
                dirWatcher.addFileCreatedEventListener(this);
                dirWatcher.addFileModifiedEventListener(this);
                dirWatcher.addFileDeletedEventListener(this);

                String path = model.getMainDir().getPath();
                dirWatcher.setMainDirectory(path);
            } catch (Exception ex) {
                logger.error("RemoteInboxApplicationImpl::restart (9): " + ex.getMessage());
            }
        }

        if (resetRecordings || inboxChanged) {
            try {
                ingest.reload();
            } catch (Exception ex) {
                logger.error("RemoteInboxApplicationImpl::restart (10): " + ex.getMessage());
            }

            deletedRecordings.clear();
            recordingsToCheck.clear();
            scheduledRecordings.clear();
            activeRecordings.clear();
            failedRecordings.clear();
            pool_stateChecker = Executors.newFixedThreadPool(5);
            initWorkers();
        }
    }

    private void loadConfiguration() throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::loadConfiguration - Loading configuration");
        }
        tmpFileCheckRate = Integer.parseInt(config.get(Constants.PROPKEY_TMPFILECHECKRATE));
        pollTime = Integer.parseInt(config.get(Constants.PROPKEY_POLLTIME));
        stateCheckerCompletedRate = Integer.parseInt(config.get(Constants.PROPKEY_RECORDINGSTATECHECKERRATE));
        conductorRate = Integer.parseInt(config.get(Constants.PROPKEY_CONDUCTORRATE));
        ingestFailedRecordingsTries = Integer.parseInt(config.get(Constants.PROPKEY_INGESTFAILEDRECORDINGSTRIES));
        maxConcurrentUploads = Integer.parseInt(config.get(Constants.PROPKEY_MAXCONCURRENTUPLOADS));
        renamingTimeout = Integer.parseInt(config.get(Constants.PROPKEY_RENAMINGTIMEOUT));
        updateTries = Integer.parseInt(config.get(Constants.PROPKEY_UPDATETRIES));
    }

    public File ensureMainDirectory() throws IllegalArgumentException {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::ensureMainDirectory - Ensuring main directory");
        }
        File mainDir;
        String path = config.get(Constants.PROPKEY_MAINDIR);
        if (path != null) {
            // ensure existence of main directory
            mainDir = new File(path);
            if (!mainDir.exists()) {
                if (logger.isInfoEnabled()) {
                    logger.info("RemoteInboxApplicationImpl::ensureMainDirectory - Main directory not existing, creating main directory: " + mainDir.getPath());
                }
                if (!mainDir.mkdirs()) {
                    logger.error("RemoteInboxApplicationImpl::ensureMainDirectory - Failed to create main directory: " + mainDir.getPath());
                    throw new IllegalStateException("Failed to create main directory");
                }
            }
        } else {
            logger.error("RemoteInboxApplicationImpl::ensureMainDirectory - No main dir configured");
            throw new IllegalArgumentException("No main directory configured");
        }
        return mainDir;
    }

    public File ensureDefaultInbox() throws IllegalArgumentException {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::ensureDefaultInbox - Ensuring defailt inbox");
        }
        File mainDir = null;
        try {
            mainDir = ensureMainDirectory();
        } catch (IllegalArgumentException e) {
            throw e;
        }

        if (mainDir != null) {
            // ensure existence of default inbox
            File defaultInbox = new File(mainDir.getPath()
                    + File.separator + config.get(Constants.PROPKEY_DEFAULTINBOX));
            if (!defaultInbox.exists()) {
                if (logger.isInfoEnabled()) {
                    logger.info("Default inbox " + config.get(Constants.PROPKEY_DEFAULTINBOX) + " not existing, creating default inbox: " + defaultInbox.getPath());
                }
                if (!defaultInbox.mkdir()) {
                    logger.error("RemoteInboxApplicationImpl::ensureDefaultInbox - Failed to create default inbox!");
                    throw new IllegalStateException("Failed to create default inbox directory");
                } else {
                    model.addInbox(defaultInbox);
                }
            }
        }
        return mainDir;
    }

    public DatabaseAdapter getDB() {
        return db;
    }

    private void resurrectRecordings() {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::resurrectRecordings - Resurrecting recordings");
        }
        for (int it : db.getRecordings().keySet()) {
            Recording recording = db.getRecordings().get(it);
            if (recording.getState().equals(State.SCHEDULED)) {
                recievingRecordings.put(recording.getPath(), 0l);
            } else if (recording.getState().equals(State.INPROGRESS)) {
                startIngest(recording);
            }
        }
    }

    private void automaticallyScheduleFailedRecordings() {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::automaticallyScheduleFailedRecordings - Scheduling failed recordings");
        }
        if (Boolean.parseBoolean(config.get(Constants.PROPKEY_RETRY_INGESTING_FAILED_RECORDINGS))) {
            for (int it : db.getRecordings().keySet()) {
                Recording recording = db.getRecordings().get(it);
                if (recording.getState().equals(State.FAILED)) {
                    int tries = 0;
                    if (failedRecordings.containsKey(recording.getPath())) {
                        tries = failedRecordings.get(recording.getPath());
                    } else {
                        failedRecordings.put(recording.getPath(), 0);
                    }
                    if (tries < ingestFailedRecordingsTries) {
                        ++tries;
                        failedRecordings.put(recording.getPath(), tries);
                        scheduleIngest(recording);
                    }
                } else if (recording.getState().equals(State.COMPLETE) || recording.getState().equals(State.IDLE)) {
                    if (failedRecordings.containsKey(recording.getPath())) {
                        failedRecordings.remove(recording.getPath());
                        logger.error(recording.getPath());
                    }
                }
            }
        }
    }

    private void updateRecordingFromFuture(FutureTask future, Recording recording) throws InterruptedException, ExecutionException {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::updateRecordingFromFuture - Updating recording " + recording.getTitle());
        }
        try {
            String mpXml = (String) future.get();
            if (!mpXml.isEmpty() && !mpXml.equals("")) {
                recording.setMediaPackage(mpXml);
            }
        } catch (NullPointerException e) {
        }
    }

    private void setRecordingFailed(RecordingImpl recording, String errorMsg) {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::setRecordingFailed - Recording failed: " + recording.getTitle() + " (" + errorMsg + ")");
        }
        stopIngest(recording);
        recording.setCurrentTask(null);
        recording.setErrorMessage(errorMsg);
        recording.setUploadProgress(0);
        recording.setState(State.FAILED);
        activeRecordings.remove(recording);
        scheduledRecordings.remove(recording);
    }

    private void setRecordingRecieving(File f) {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::setRecordingRecieving - Receiving recording " + f.getName());
        }
        String parentKey = f.getAbsoluteFile().getParent();
        Recording rec = db.getRecordingByPath(parentKey);
        if (rec != null) {
            if (rec.getState().equals(State.IDLE)) {
                recordingsToCheck.remove(rec);
                rec.setState(State.RECIEVING);
                recievingRecordings.put(parentKey, 0l);
            } else if (rec.getState().equals(State.RECIEVING)) {
                recievingRecordings.put(parentKey, 0l);
            }
        }
    }

    private void stopActiveTask(RecordingImpl recording) {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::stopActiveTask - Stopping active task " + recording.getTitle());
        }
        try {
            InboxTask task = recording.getCurrentTask();
            if (task != null) {
                recording.setCurrentTask(null);
                FutureTask ft = task.getFutureTask();
                if (ft != null) {
                    ft.cancel(true);
                }
                ingest.removeClient(recording.getPath());
            }
        } catch (Exception ex) {
            logger.error("RemoteInboxApplicationImpl::stopActiveTask - Exception occurred on stopping the active task: " + ex.getMessage());
        }
    }

    public IngestClientController.NetworkConnectionState getNetworkConnectionState() {
        IngestClientController.NetworkConnectionState state = ingest.getNetworkConnectionState();
        return state;
    }

    public String getCurrentVersion() {
        return vt.getCurrentVersion();
    }

    public String createNewSeries(String document) throws IngestClientException {
        try {
            return ingest.createNewSeries(document);
        } catch (URISyntaxException ex) {
        }
        return "";
    }

    public String getCurrentlyAvailableVersion() {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::getCurrentlyAvailableVersion");
        }
        Updater updater = new Updater(config);
        String currVersion = vt.getCurrentVersion();
        String availVersion = "unknown";
        boolean success = false;
        int tries = updateTries;
        while (!success && (tries > 0)) {
            if (logger.isInfoEnabled()) {
                logger.info("RemoteInboxApplicationImpl::getCurrentlyAvailableVersion - try #" + (updateTries - tries + 1));
            }
            --tries;
            try {
                availVersion = updater.getCurrentlyAvailableVersion();
                success = true;
            } catch (IOException ex) {
                logger.error("RemoteInboxApplicationImpl::getCurrentlyAvailableVersion - IOException: " + ex.getMessage());
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ex1) {
                    logger.error("RemoteInboxApplicationImpl::getCurrentlyAvailableVersion - InterruptedException: " + ex1.getMessage());
                }
            }
        }
        if (!success) {
            return "unknown";
        }
        int compVersions = vt.compareVersions(availVersion, currVersion);
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::getCurrentlyAvailableVersion - Compared versions: " + compVersions);
        }
        if (compVersions == 1) {
            return availVersion;
        }
        return currVersion;
    }

    private void fullyStopRecordingTask(Recording recording) {
        try {
            if (recording != null) {
                if (logger.isInfoEnabled()) {
                    logger.info("RemoteInboxApplicationImpl::fullyStopRecordingTask - fully stopping recording task for recording " + recording.getTitle());
                }
                if ((recording.getState() != State.IDLE)
                        && (recording.getState() != State.COMPLETE)
                        && (recording.getState() != State.FAILED)
                        && (recording.getState() != State.RECIEVING)) {
                    if (scheduledRecordings.contains(recording)) {
                        scheduledRecordings.remove(recording);
                    }
                    if (activeRecordings.contains(recording)) {
                        activeRecordings.remove(recording);
                    }
                    for (RecordingFile file : ((RecordingImpl) recording).files) {
                        UploadJob job = file.getUploadJob();
                        job.setState(UploadJob.State.UNINITIALIZED);
                        db.removeUploadJob((UploadJobImpl) job);
                        job = db.createUploadJob(file, Integer.toString(file.getRecording().getId()), 0);
                        if (job != null) {
                            job.setState(UploadJob.State.UNINITIALIZED);
                            file.setUploadJob(job);
                        }
                    }

                    stopActiveTask((RecordingImpl) recording);
                    ((RecordingImpl) recording).setCurrentTask(null);
                    recording.setMediaPackage("");
                    recording.setState(State.IDLE);
                    recording.setIngestStatus("");
                    recording.setUploadProgress(0);
                }
            } else {
                if (logger.isInfoEnabled()) {
                    logger.info("RemoteInboxApplicationImpl::fullyStopRecordingTask - recording is null");
                }
            }
        } catch (Exception e) {
            logger.error("RemoteInboxApplicationImpl::fullyStopRecordingTask - Exception: " + e.getMessage());
        }
    }

    private void checkActiveStates() {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::checkActiveStates - Checking active states");
        }
        if ((activeRecordings.size() < maxConcurrentUploads) && !scheduledRecordings.isEmpty()) {
            Recording recording = scheduledRecordings.poll();
            startIngest(recording);
        }

        int active = 0;
        for (int it : db.getRecordings().keySet()) {
            Recording recording = db.getRecordings().get(it);
            if (recording.getState().equals(State.INPROGRESS)) {
                if (active >= maxConcurrentUploads) {
                    fullyStopRecordingTask(recording);
                    scheduleIngest(recording);
                } else {
                    ++active;
                }
            }
        }
    }

    private void checkCompletedRecordingStates() {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::checkCompletedRecordingStates - Checking completed recording states");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Recordings to check (server status): " + recordingsToCheck.size());
        }
        for (final Recording r : recordingsToCheck) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (logger.isInfoEnabled()) {
                        logger.info("Next path to check (server status): " + r.getPath());
                    }
                    try {
                        if (r.getState().equals(State.COMPLETE)) {
                            String mpID = ParseUtils.getInstance().getMediaPackageID(r.getMediaPackage());
                            if (!mpID.isEmpty() && !mpID.equals("")) {
                                IngestClient ic = ingest.getClient(r.getPath());
                                String instanceXML = ic.getInstanceXML(mpID);
                                if (instanceXML != null) {
                                    String state_str = ParseUtils.getInstance().parseState(instanceXML);
                                    // server URL
                                    String serverURL = "";
                                    // server status
                                    InstanceState is;
                                    if (state_str.equalsIgnoreCase(InstanceState.FAILING.toString()) || state_str.equalsIgnoreCase("FAILED")) {
                                        is = InstanceState.FAILING;
                                    } else if (state_str.equalsIgnoreCase(InstanceState.PAUSED.toString())) {
                                        is = InstanceState.PAUSED;
                                    } else if (state_str.equalsIgnoreCase(InstanceState.INSTANTIATED.toString())) {
                                        is = InstanceState.INSTANTIATED;
                                    } else if (state_str.equalsIgnoreCase(InstanceState.RUNNING.toString())) {
                                        is = InstanceState.RUNNING;
                                    } else if (state_str.equalsIgnoreCase(InstanceState.SUCCEEDED.toString())) {
                                        is = InstanceState.SUCCEEDED;
                                        serverURL = ParseUtils.getInstance().parseEngageURL(instanceXML);
                                    } else {
                                        is = null;
                                    }
                                    if (is != null) {
                                        ((RecordingImpl) r).linkToEngageUI = serverURL;
                                        ((RecordingImpl) r).setIngestDetails(Constants.getInstance().getLocalizedString("ServerStatus") + ": " + is.toString());
                                        r.setState(State.COMPLETE);
                                        if (is.equals(InstanceState.SUCCEEDED)) {
                                            recordingsToCheck.remove(r);
                                        }
                                    }
                                } else {
                                    ((RecordingImpl) r).linkToEngageUI = "";
                                    ((RecordingImpl) r).setIngestDetails("");
                                    r.setState(State.COMPLETE);
                                }
                            } else {
                                recordingsToCheck.remove(r);
                            }
                        }
                    } catch (Exception e) {
                        logger.error("RemoteInboxApplicationImpl::fullyStopRecordingTask - Exception: " + e.getMessage());
                    }
                }
            });
            pool_stateChecker.execute(t);
        }
    }

    private void resetRecordingQueue() {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::resetRecordingQueue - Resetting recording queue");
        }
        try {
            recordingsToCheck.clear();
            scheduledRecordings.clear();
            activeRecordings.clear();
            for (int it : db.getRecordings().keySet()) {
                Recording recording = db.getRecordings().get(it);
                if (recording.getState().equals(State.COMPLETE)) {
                    recordingsToCheck.add(recording);
                }
            }
        } catch (Exception e) {
            logger.error("RemoteInboxApplicationImpl::resetRecordingQueue - Exception: " + e.getMessage());
        }
    }

    private void initWorkers() {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::initWorkers - Initializing workers");
        }
        pool_executor = Executors.newScheduledThreadPool(6);

        Runnable tmpFileChecker = new Runnable() {
            @Override
            public void run() {
                try {
                    if (activeRecordings.isEmpty() && scheduledRecordings.isEmpty()) {
                        SpecificFileUtils.deleteTmpFiles();
                    }
                } catch (Exception ex) {
                    logger.error("RemoteInboxApplicationImpl::initWorkers (tmp file checker): " + ex.getMessage());
                }
            }
        };

        Runnable watcher = new Runnable() {
            @Override
            public void run() {
                try {
                    if (!recievingRecordings.isEmpty()) {
                        for (String key : recievingRecordings.keySet()) {
                            Recording recording = db.getRecordingByPath(key);
                            if (recording != null) {
                                boolean everythingOK = true;
                                for (RecordingFile rf : recording.getFiles()) {
                                    RandomAccessFile f = null;
                                    try {
                                        f = new RandomAccessFile(rf.getPath(), "rw");
                                    } catch (Exception ex) {
                                        logger.error("Exception: " + ex.getMessage());
                                        everythingOK = false;
                                        break;
                                    } finally {
                                        if (f != null) {
                                            try {
                                                f.close();
                                            } catch (IOException ex) {
                                                logger.error("RemoteInboxApplicationImpl::initWorkers (Watcher::run) - IOException: " + ex.getMessage());
                                            }
                                        }
                                    }
                                }

                                if (everythingOK) {
                                    // TODO: Activate automatic scheduling
                                    // scheduleIngest(recording);
                                    if (recording.getState() == State.RECIEVING) {
                                        recordingsToCheck.remove(recording);
                                        recording.setState(State.IDLE);
                                    }
                                    recievingRecordings.remove(key);
                                }
                            }
                        }
                    } else {
                        for (int it : db.getRecordings().keySet()) {
                            Recording recording = db.getRecordings().get(it);
                            if ((recording != null) && (recording.getState().equals(State.RECIEVING))) {
                                recordingsToCheck.remove(recording);
                                recording.setState(State.IDLE);
                            }
                        }
                        deletedRecordings.clear();
                        automaticallyScheduleFailedRecordings();
                    }
                } catch (Exception ex) {
                    logger.error("RemoteInboxApplicationImpl::initWorkers: (Watcher)" + ex.getMessage());
                }
            }
        };

        Runnable launcher = new Runnable() {
            @Override
            public void run() {
                try {
                    if ((activeRecordings.size() < maxConcurrentUploads) && !scheduledRecordings.isEmpty()) {
                        Recording recording = scheduledRecordings.poll();
                        startIngest(recording);
                    }
                } catch (Exception ex) {
                    logger.error("RemoteInboxApplicationImpl::initWorkers (Launcher): " + ex.getMessage());
                }
            }
        };

        Runnable stateCompletedChecker = new Runnable() {
            @Override
            public void run() {
                try {
                    checkCompletedRecordingStates();
                } catch (Exception ex) {
                    logger.error("RemoteInboxApplicationImpl::initWorkers: (StateCompletedChecker)" + ex.getMessage());
                }
            }
        };

        Runnable stateActiveChecker = new Runnable() {
            @Override
            public void run() {
                try {
                    checkActiveStates();
                } catch (Exception ex) {
                    logger.error("RemoteInboxApplicationImpl::initWorkers: (StateActiveChecker)" + ex.getMessage());
                }
            }
        };

        final RemoteInboxApplication ria = this;
        Runnable conductor = new Runnable() {
            @Override
            public void run() {
                RecordingImpl recording = null;
                try {
                    if (!activeRecordings.isEmpty()) {
                        for (Recording rIt : activeRecordings) {
                            recording = (RecordingImpl) rIt;
                            if ((recording != null) && recording.hasCurrentTask()) {
                                IngestClient ic = ingest.getClient(recording.getPath());
                                if (ic == null) {
                                    logger.error("RemoteInboxApplicationImpl::initWorkers (Conductor): Could not find recording");
                                    return;
                                }
                                InboxTask task = recording.getCurrentTask();
                                FutureTask future = task.getFutureTask();
                                try {
                                    switch (task.getType()) {
                                        // nothing done yet, we start with creating a mediaPackage on the server
                                        case STARTING:
                                            if (logger.isInfoEnabled()) {
                                                logger.info("RemoteInboxApplicationImpl::initWorkers (Conductor): Starting to create MediaPackage for " + recording.getPath());
                                            }
                                            recording.setUploadProgress(progressBarStartAt);

                                            recording.setIngestStatus(Constants.getInstance().getLocalizedString("CheckForEpisode"));
                                            SpecificFileUtils.checkForEpisodeXml(ria, recording);
                                            recording.setIngestStatus(Constants.getInstance().getLocalizedString("CheckForEpisode") + " - " + Constants.getInstance().getLocalizedString("Done"));
                                            recording.setUploadProgress(recording.getUploadProgress() + 5);

                                            recording.setIngestStatus(Constants.getInstance().getLocalizedString("CheckForSeries"));
                                            SpecificFileUtils.checkForSeriesXml(recording);
                                            recording.setIngestStatus(Constants.getInstance().getLocalizedString("CheckForSeries") + " - " + Constants.getInstance().getLocalizedString("Done"));
                                            recording.setUploadProgress(recording.getUploadProgress() + 5);
                                            try {
                                                recording.setIngestStatus(Constants.getInstance().getLocalizedString("CreatingMediaPackage"));
                                                recording.setCurrentTask(
                                                        new InboxTask(Type.CREATE_MEDIAPACKAGE, ic.createNewMediaPackage()));
                                            } catch (IngestClientException ex) {
                                                logger.error("RemoteInboxApplicationImpl::initWorkers (Conductor):  - Network Error in createMediaPackage: " + ex.getMessage());
                                                setRecordingFailed(recording, Constants.getInstance().getLocalizedString("CouldNotGetMediaPackage"));
                                            }
                                            break;

                                        // create a mediaPackage
                                        case CREATE_MEDIAPACKAGE:
                                            if (future.isDone()) {
                                                recording.setUploadProgress(recording.getUploadProgress() + 5);

                                                try {
                                                    if (logger.isInfoEnabled()) {
                                                        logger.info("RemoteInboxApplicationImpl::initWorkers (Conductor): Starting uploading of media files for " + recording.getPath());
                                                    }

                                                    updateRecordingFromFuture(future, recording);

                                                    recording.setIngestStatus(Constants.getInstance().getLocalizedString("CreatingMediaPackage") + " - " + Constants.getInstance().getLocalizedString("Done"));
                                                    recording.setUploadProgress(recording.getUploadProgress() + 5);

                                                    recording.setCurrentTask(
                                                            // set dummy InboxTask to indicate that we want to start uploading files
                                                            new InboxTask(Type.UPLOAD_FILE, null));

                                                } catch (InterruptedException e) {
                                                    logger.error("RemoteInboxApplicationImpl::initWorkers (Conductor): FutureTask (CreateMediaPackage) was interrupted");
                                                    setRecordingFailed(recording, "");

                                                } catch (ExecutionException e) {
                                                    Throwable cause = e.getCause();
                                                    if (cause instanceof IngestClientException) {
                                                        logger.error("RemoteInboxApplicationImpl::initWorkers (Conductor): " + e.getMessage());
                                                        IngestClientException ie = (IngestClientException) cause;
                                                        if (ie.getType().equals(IngestClientException.Type.NETWORK_ERROR)) {
                                                            logger.error("RemoteInboxApplicationImpl::initWorkers (Conductor): CREATE_MEDIAPACKAGE - IngestClientException(NETWORK_ERROR): " + e.getMessage());
                                                            setRecordingFailed(recording, Constants.getInstance().getLocalizedString("NetworkError") + ": " + Constants.getInstance().getLocalizedString("CouldNotGetMediaPackage"));
                                                            // scheduleIngest(recording);
                                                        } else {
                                                            logger.error("RemoteInboxApplicationImpl::initWorkers (Conductor): CREATE_MEDIAPACKAGE - Exception: " + e.getMessage());
                                                            setRecordingFailed(recording, Constants.getInstance().getLocalizedString("CouldNotGetMediaPackage"));
                                                        }
                                                    } else {
                                                        logger.error("RemoteInboxApplicationImpl::initWorkers (Conductor): CREATE_MEDIAPACKAGE - ExecutionException: " + e.getMessage());
                                                        setRecordingFailed(recording, Constants.getInstance().getLocalizedString("CouldNotGetMediaPackage"));
                                                    }
                                                }
                                            }
                                            break;

                                        case UPLOAD_FILE:
                                            if ((future == null) || future.isDone()) {
                                                try {
                                                    updateRecordingFromFuture(future, recording);
                                                } catch (InterruptedException e) {
                                                    logger.error("RemoteInboxApplicationImpl::initWorkers (Conductor): UPLOAD_FILE - FutureTask (UploadFile) was interrupted");
                                                    setRecordingFailed(recording, "");
                                                    break;
                                                } catch (ExecutionException e) {
                                                    Throwable cause = e.getCause();
                                                    logger.error("RemoteInboxApplicationImpl::initWorkers (Conductor): UPLOAD_FILE - Exception cause: " + e.getMessage());
                                                    if (cause instanceof IngestClientException) {
                                                        logger.error("RemoteInboxApplicationImpl::initWorkers (Conductor): UPLOAD_FILE (Exception generated by the ingest client) " + e.getMessage());
                                                        IngestClientException ie = (IngestClientException) cause;
                                                        if (ie.getType().equals(IngestClientException.Type.NETWORK_ERROR)) {
                                                            setRecordingFailed(recording, Constants.getInstance().getLocalizedString("NetworkError") + ": " + Constants.getInstance().getLocalizedString("CouldNotCompleteUpload2"));
                                                            // scheduleIngest(recording);
                                                            break;
                                                        } else {
                                                            setRecordingFailed(recording, Constants.getInstance().getLocalizedString("CouldNotCompleteUpload"));
                                                            break;
                                                        }
                                                    } else {
                                                        logger.error("RemoteInboxApplicationImpl::initWorkers (Conductor): UPLOAD_FILE - ExecutionException: " + e.getMessage());
                                                        setRecordingFailed(recording, Constants.getInstance().getLocalizedString("CouldNotCompleteUpload"));
                                                        break;
                                                    }
                                                }
                                                List<RecordingFile> pendingFiles = ((RecordingImpl) recording).getUnfinishedFiles();
                                                if (logger.isInfoEnabled()) {
                                                    logger.info("RemoteInboxApplicationImpl::initWorkers (Conductor): Number of pending files: " + pendingFiles.size());
                                                    logger.info("RemoteInboxApplicationImpl::initWorkers (Conductor): #############################################");
                                                    for (int i = 0; i < pendingFiles.size(); ++i) {
                                                        logger.info("RemoteInboxApplicationImpl::initWorkers (Conductor): Pending file number " + i + ":");
                                                        logger.info("RemoteInboxApplicationImpl::initWorkers (Conductor): Path: " + pendingFiles.get(i).getPath());
                                                        logger.info("RemoteInboxApplicationImpl::initWorkers (Conductor): Flavor: " + pendingFiles.get(i).getFlavor());
                                                        logger.info("RemoteInboxApplicationImpl::initWorkers (Conductor): Type: " + pendingFiles.get(i).getType());
                                                    }
                                                    logger.info("RemoteInboxApplicationImpl::initWorkers (Conductor): #############################################");
                                                }
                                                int s = recording.files.size();
                                                int chunkProg = 0;
                                                if (s != 0) {
                                                    chunkProg = (progressBarDownloadStopAt - (progressBarStartAt + 5 * 5)) / recording.files.size();
                                                }
                                                recording.setCurrentChunkProgSize(chunkProg);
                                                if (pendingFiles.size() > 0) {
                                                    RecordingFile file = pendingFiles.get(0);
                                                    if (logger.isInfoEnabled()) {
                                                        logger.info("RemoteInboxApplicationImpl::initWorkers (Conductor): ========================================");
                                                        logger.info("RemoteInboxApplicationImpl::initWorkers (Conductor): Next file to be uploaded:");
                                                        logger.info("RemoteInboxApplicationImpl::initWorkers (Conductor): Path: " + file.getPath());
                                                        logger.info("RemoteInboxApplicationImpl::initWorkers (Conductor): Flavor: " + file.getFlavor());
                                                        logger.info("RemoteInboxApplicationImpl::initWorkers (Conductor): Type: " + file.getType());
                                                        logger.info("RemoteInboxApplicationImpl::initWorkers (Conductor): ========================================");
                                                    }
                                                    if (file.getType().equals(RecordingFile.Type.TRACK)) {
                                                        recording.setCurrentTask(
                                                                new InboxTask(Type.UPLOAD_FILE, ic.addTrack(file)));
                                                    } else if (file.getType().equals(RecordingFile.Type.CATALOG)) {
                                                        recording.setUploadProgress(recording.getUploadProgress() + chunkProg);
                                                        recording.setCurrentTask(
                                                                new InboxTask(Type.UPLOAD_FILE, ic.addCatalog(file)));
                                                    } else {
                                                        logger.error("RemoteInboxApplicationImpl::initWorkers (Conductor): RemoteInboxApplicationImpl::run - Could not recognize track type of file " + file.getPath());
                                                        if (((RecordingImpl) recording).ignorePendingFile(file)) {
                                                            if (logger.isInfoEnabled()) {
                                                                logger.info("RemoteInboxApplicationImpl::initWorkers (Conductor): Ignoring file " + file.toString());
                                                            }
                                                        } else {
                                                            if (logger.isInfoEnabled()) {
                                                                logger.info("RemoteInboxApplicationImpl::initWorkers (Conductor): Could not ignore file " + file.toString());
                                                            }
                                                            setRecordingFailed(recording, Constants.getInstance().getLocalizedString("CouldNotRecognizeTrackType"));
                                                        }
                                                        break;
                                                    }
                                                    // all files uploaded
                                                } else {
                                                    recording.setUploadProgress(90);
                                                    Map<String, String> params = new HashMap<>();
                                                    if (recording.getTrim()) {
                                                        params.put("trimHold", "true");
                                                        params.put("videoPreview", "true");
                                                    }
                                                    String workflowId = recording.getWorkflowId();
                                                    if (workflowId.isEmpty() || workflowId.equals("")) {
                                                        workflowId = recording.getInbox().getWorkflowId();
                                                    }
                                                    if (workflowId.isEmpty() || workflowId.equals("")) {
                                                        workflowId = config.get(Constants.PROPKEY_DEFAULT_WORKFLOW);
                                                    }
                                                    recording.setIngestStatus(Constants.getInstance().getLocalizedString("StartingProcessing"));
                                                    recording.setCurrentTask(
                                                            new InboxTask(Type.START_INGEST,
                                                                    ingest.getClient(recording.getPath()).startProcessing(recording.getMediaPackage(), workflowId, params)));
                                                }
                                            }
                                            break;

                                        case START_INGEST:
                                            if (future.isDone()) {
                                                recording.setUploadProgress(recording.getUploadProgress() + 5);
                                                try {
                                                    updateRecordingFromFuture(future, recording);

                                                } catch (InterruptedException e) {
                                                    logger.error("RemoteInboxApplicationImpl::initWorkers (Conductor): FutureTask (StartIngest) was interrupted");
                                                    setRecordingFailed(recording, "");
                                                    break;

                                                } catch (ExecutionException e) {
                                                    Throwable cause = e.getCause();
                                                    if (cause instanceof IngestClientException) {
                                                        IngestClientException ie = (IngestClientException) cause;
                                                        if (ie.getType().equals(IngestClientException.Type.NETWORK_ERROR)) {
                                                            setRecordingFailed(recording, Constants.getInstance().getLocalizedString("NetworkError") + ": " + Constants.getInstance().getLocalizedString("CouldNotStartIngest2"));
                                                            // scheduleIngest(recording);
                                                            break;
                                                        } else {
                                                            setRecordingFailed(recording, Constants.getInstance().getLocalizedString("CouldNotStartIngest"));
                                                            break;
                                                        }
                                                    } else {
                                                        logger.error("RemoteInboxApplicationImpl::initWorkers (Conductor): START_INGEST - ExecutionException: " + e.getMessage());
                                                        setRecordingFailed(recording, Constants.getInstance().getLocalizedString("CouldNotStartIngest"));
                                                        break;
                                                    }
                                                }
                                                recording.setState(State.COMPLETE);
                                                recording.setUploadProgress(100);
                                                activeRecordings.remove(recording);
                                                recordingsToCheck.add(recording);
                                                ingest.removeClient(recording.getPath());
                                            }
                                            break;
                                    }
                                } catch (Exception ex) {
                                    setRecordingFailed(recording, ex.getMessage());
                                }
                            } else {
                                activeRecordings.remove(recording);
                                ingest.removeClient(recording.getPath());
                            }
                        }
                    }
                } catch (Exception ex) {
                    logger.error("RemoteInboxApplicationImpl::initWorkers (Conductor): Exception caught in Runnable conductor: " + ex.getMessage());
                    if (recording != null) {
                        logger.error("RemoteInboxApplicationImpl::initWorkers (Conductor): Setting recording '" + recording.getPath() + "/" + recording.getTitle() + "' to failed.");
                        setRecordingFailed(recording, Constants.getInstance().getLocalizedString("CouldNotStartIngest"));
                    } else {
                        logger.error("RemoteInboxApplicationImpl::initWorkers (Conductor): Could not set recording to failed, recording is null. Resetting the queue.");
                        resetRecordingQueue();
                    }
                }
            }
        };

        pool_executor.scheduleAtFixedRate(tmpFileChecker, 0, tmpFileCheckRate, TimeUnit.HOURS);
        pool_executor.scheduleAtFixedRate(watcher, 0, pollTime, TimeUnit.SECONDS);
        pool_executor.scheduleAtFixedRate(launcher, pollTime / 2, pollTime, TimeUnit.SECONDS);
        pool_executor.scheduleAtFixedRate(stateCompletedChecker, stateCheckerCompletedRate / 2, stateCheckerCompletedRate, TimeUnit.SECONDS);
        pool_executor.scheduleAtFixedRate(stateActiveChecker, stateCheckerActiveRate / 2, stateCheckerActiveRate, TimeUnit.SECONDS);
        pool_executor.scheduleAtFixedRate(conductor, 0, conductorRate, TimeUnit.SECONDS);
    }

    // getter
    @Override
    public ListModelAdapter getListModelAdapter() {
        return listModelAdapter;
    }

    public ListModelAdapter getListModelAdapter(State filter, boolean filterAlphabetically) {
        ListModelAdapterImpl lmai = (ListModelAdapterImpl) listModelAdapter;
        lmai.setFilter(filter);
        return lmai;
    }

    @Override
    public Configuration getConfig() {
        return this.config;
    }

    @Override
    public String getInboxPath() {
        String path = config.get(Constants.PROPKEY_MAINDIR);
        if (path != null) {
            path = path.endsWith(File.separator) ? path : (path + File.separator);
        }
        return path;
    }

    @Override
    public HashMap<String, String> getSeriesList() throws IOException {
        HashMap<String, String> seriesList = null;
        try {
            seriesList = ingest.getSeriesList();
        } catch (IOException ex) {
            logger.error("RemoteInboxApplicationImpl::getSeriesList - IOException: " + ex.getMessage());
            throw ex;
        } catch (IngestClientException ex) {
            logger.error("RemoteInboxApplicationImpl::getSeriesList - IngestClientException: " + ex.getMessage());
            throw new IOException(ex.getMessage());
        } catch (URISyntaxException ex) {
            logger.error("RemoteInboxApplicationImpl::getSeriesList - URISyntaxException: " + ex.getMessage());
            throw new IOException(ex.getMessage());
        }
        return seriesList;
    }

    @Override
    public List<String> getWorkflowList() throws IOException {
        List<String> workflowList = null;
        try {
            workflowList = ingest.getWorkflowList();
        } catch (IOException ex) {
            logger.error("RemoteInboxApplicationImpl::getWorkflowList - IOException: " + ex.getMessage());
            throw ex;
        } catch (IngestClientException ex) {
            logger.error("RemoteInboxApplicationImpl::getWorkflowList - IngestClientException: " + ex.getMessage());
            throw new IOException(ex.getMessage());
        } catch (URISyntaxException ex) {
            logger.error("RemoteInboxApplicationImpl::getWorkflowList - URISyntaxException: " + ex.getMessage());
            throw new IOException(ex.getMessage());
        }
        return workflowList;
    }

    // setter
    @Override
    public void setCurrentSelectedInbox(String currInboxName) {
        currentInboxName = currInboxName;
    }

    // misc
    @Override
    public boolean createInbox(String name) {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::createInbox - Creating inbox named " + name);
        }
        boolean mkd = false;
        try {
            File newDir = new File(model.getMainDir().getPath() + File.separator + name);
            mkd = newDir.mkdir();
        } catch (Exception e) {
            logger.error("RemoteInboxApplicationImpl::createInbox - Exception occurred while creating an inbox: " + e.getMessage());
        }
        return mkd;
    }

    @Override
    public void downloadSeries(String seriesID) throws IOException {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::downloadSeries - Downloading series " + seriesID);
        }
        String path = getInboxPath();
        if (!(path.equals("")
                || path.isEmpty()
                || currentInboxName.equals("")
                || currentInboxName.isEmpty())) {
            try {
                String pathStr = path + currentInboxName + File.separator + Constants.FILENAME_DC_SERIES;
                String series = ingest.getSeriesCatalog(seriesID);
                if (!series.equals("") && !series.isEmpty()) {
                    try {
                        MHRIFileUtils.getInstance().writeToFile(pathStr, series);
                    } catch (IOException ex) {
                        throw ex;
                    }
                }
            } catch (IngestClientException ex) {
                logger.error("RemoteInboxApplicationImpl::downloadSeries - IngestClientException: " + ex.getMessage());
                throw new IOException(ex.getMessage());
            } catch (URISyntaxException ex) {
                logger.error("RemoteInboxApplicationImpl::downloadSeries - URISyntaxException: " + ex.getMessage());
                throw new IOException(ex.getMessage());
            }
        }
    }

    @Override
    public void scheduleIngest(Recording recording) {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::scheduleIngest - Scheduling ingest for recording " + recording.getTitle());
        }
        if ((recording != null)
                && !activeRecordings.contains(recording)
                && !scheduledRecordings.contains(recording)) {
            recordingsToCheck.remove(recording);
            recording.setState(State.SCHEDULED);
            recording.setIngestStatus(Constants.getInstance().getLocalizedString("ScheduledForSubmission"));
            scheduledRecordings.add(recording);
        }
    }

    @Override
    public void startIngest(Recording recording) {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::startIngest - Starting ingest for recording " + recording.getTitle());
        }
        if ((recording != null)
                && !activeRecordings.contains(recording)) {
            if (recording.getMediaPackage().isEmpty()) {
                ((RecordingImpl) recording).setCurrentTask(
                        new InboxTask(Type.STARTING, null));
            } else {
                ((RecordingImpl) recording).setCurrentTask(
                        new InboxTask(Type.STARTING, null));
            }
            activeRecordings.add(recording);
            recording.setIngestStatus(Constants.getInstance().getLocalizedString("Starting"));
            recordingsToCheck.remove(recording);
            recording.setState(State.INPROGRESS);
        }
    }

    // TODO
    @Override
    public void pauseIngest(Recording recording) {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::pauseIngest - Pausing ingest for recording " + recording.getTitle());
        }
        if (recording != null) {
            if (scheduledRecordings.contains(recording)) { // recording scheduled?
                scheduledRecordings.remove(recording);
            } else if (activeRecordings.contains(recording)) { // recording active?
                stopActiveTask((RecordingImpl) recording);
                activeRecordings.remove(recording);
            }
            recordingsToCheck.remove(recording);
            recording.setState(State.PAUSED);
            recording.setIngestStatus(Constants.getInstance().getLocalizedString("Paused"));
            recording.setUploadProgress(0);
        }
    }

    @Override
    public void stopIngest(Recording recording) {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::stopIngest - Stopping ingest for recording " + recording.getTitle());
        }
        if (recording != null) {
            RecordingImpl r = ((RecordingImpl) recording);
            if (r.getState().equals(State.INPROGRESS)
                    || r.getState().equals(State.PAUSED)
                    || r.getState().equals(State.SCHEDULED)) {
                stopActiveTask((RecordingImpl) recording);
                if (logger.isInfoEnabled()) {
                    logger.info("RemoteInboxApplicationImpl::stopIngest - fully stopping recording task");
                }
                fullyStopRecordingTask(recording);
            }
        }
    }

    @Override
    public void retryIngest(Recording recording) {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::retryIngest - Retrying ingest for recording " + recording.getTitle());
        }
        if (recording != null) {
            try {
                stopIngest(recording);
                ((RecordingImpl) recording).setAllFilesToUnfinished();
                scheduleIngest(recording);
            } catch (Exception e) {
                logger.error("RemoteInboxApplicationImpl::retryIngest - Exception occurred on retrying ingesting: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean deleteInbox(Inbox inbox) {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::deleteInbox - Deleting inbox " + inbox.getName());
        }
        if (inbox != null) {
            try {
                for (Recording r : inbox.getRecordings()) {
                    stopIngest(r);
                    stopActiveTask((RecordingImpl) r);
                    ingest.removeClient(r.getPath());
                    inbox.removeRecording(r);
                    for (RecordingFile f1 : r.getFiles()) {
                        model.removeRecordingFile(new File(f1.getPath()));
                    }
                    db.removeRecording(r);
                }
                File f = new File(inbox.getPath());
                model.removeInbox(f);
                int times = numberOfDeletionTries;
                while (f.exists() && (times > 0)) {
                    MHRIFileUtils.getInstance().delete(f);
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException ex) {
                        logger.error("RemoteInboxApplicationImpl::deleteInbox - Trying again: " + times);
                    }
                    --times;
                }
                return (times > 0);
            } catch (Exception e) {
                logger.error("RemoteInboxApplicationImpl::deleteInbox - Exception occurred on deleting an inbox: " + e.getMessage());
            }
        }
        return false;
    }

    @Override
    public boolean deleteRecording(Recording recording) {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::deleteRecording - Deleting recording " + recording.getTitle());
        }
        if (recording != null) {
            try {
                for (RecordingFile f1 : recording.getFiles()) {
                    model.removeRecordingFile(new File(f1.getPath()));
                }
                ingest.removeClient(recording.getPath());
                stopIngest(recording);
                stopActiveTask((RecordingImpl) recording);
                File f = new File(recording.getPath());
                model.removeRecording(f);
                MHRIFileUtils.getInstance().delete(f);
                return true;
            } catch (Exception e) {
                logger.error("RemoteInboxApplicationImpl::deleteRecording - Exception occurred on deleting a recording: " + e.getMessage());
            }
        }
        return false;
    }

    private void moveFileTo(final String newDirComplete, final File f) {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::moveFileTo - Moving file " + f.getName() + " to " + newDirComplete);
        }
        final File newDir = new File(newDirComplete);
        if (newDir.exists() || (!newDir.exists() && newDir.mkdir())) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (logger.isInfoEnabled()) {
                        logger.info("RemoteInboxApplicationImpl::fileCreatedEventOccurred - Successfully created folder '" + newDirComplete + "' for found file. Moving found file.");
                    }
                    try {
                        String newDirCompleteChanged = newDirComplete.endsWith(File.separator) ? newDirComplete.substring(0, newDirComplete.length() - 1) : newDirComplete;
                        String newFileName = newDirCompleteChanged + File.separator + f.getName();
                        File f_mv = new File(newFileName);
                        while (!f.renameTo(f_mv)) {
                            if (deletedRecordings.contains(f.getAbsolutePath())) {
                                if (logger.isInfoEnabled()) {
                                    logger.info("RemoteInboxApplicationImpl::fileCreatedEventOccurred - File to copy has been deleted");
                                }
                                newDir.delete();
                                break;
                            }
                            if (logger.isInfoEnabled()) {
                                logger.info("RemoteInboxApplicationImpl::fileCreatedEventOccurred - Could not move file to '" + newFileName + "' -- waiting a few ms for another try.");
                            }
                            Thread.sleep(renamingTimeout);
                        }
                        if (model.addRecordingFile(f_mv.getAbsoluteFile())) {
                            if (logger.isInfoEnabled()) {
                                logger.info("RemoteInboxApplicationImpl::fileCreatedEventOccurred - Successfully moved file to '" + newFileName + "'");
                            }
                            recievingRecordings.put(f_mv.getAbsoluteFile().getParent(), 0l);
                        } else {
                            logger.error("RemoteInboxApplicationImpl::fileCreatedEventOccurred - Error registering moved file '" + newFileName + "'");
                        }
                    } catch (Exception ex) {
                        logger.error("RemoteInboxApplicationImpl::fileCreatedEventOccurred - Exception caught: " + ex.getMessage());
                    }
                }
            }).start();
        } else {
            logger.error("RemoteInboxApplicationImpl::fileCreatedEventOccurred - Could not create folder '" + newDirComplete + "' for found file. Ignoring file.");
        }
    }

    @Override
    public void fileCreatedEventOccurred(FileCreatedEvent evt, String path) {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxAppicationImpl::fileCreatedEventOccurred - File created: " + path);
        }
        final File f = new File(path).getAbsoluteFile();
        if (db.hasInbox(path) || db.hasRecording(path) || db.hasRecordingFile(path)) {
            if (logger.isInfoEnabled()) {
                logger.info("RemoteInboxApplicationImpl::fileCreatedEventOccurred - File is already in database");
            }
            return;
        }
        if (f.isDirectory()) {
            if (logger.isInfoEnabled()) {
                logger.info("RemoteInboxApplicationImpl::fileCreatedEventOccurred - Found directory");
            }
            if (model.isInbox(f)) {
                if (logger.isInfoEnabled()) {
                    logger.info("RemoteInboxApplicationImpl::fileCreatedEventOccurred - is inbox");
                }
                model.addInbox(f);
            } else if (model.isRecording(f)) {
                if (logger.isInfoEnabled()) {
                    logger.info("RemoteInboxApplicationImpl::fileCreatedEventOccurred - is recording");
                }
                model.addRecording(f);
            } else {
                logger.error("RemoteInboxApplicationImpl::fileCreatedEventOccurred - Can't handle " + path + ", ignoring");
            }
        } else if (f.isFile()) {
            if (logger.isInfoEnabled()) {
                logger.info("RemoteInboxApplicationImpl::fileCreatedEventOccurred - Found file");
            }
            if (f.isHidden() || f.getName().startsWith(".")) {
                // ignore hidden files
                if (logger.isInfoEnabled()) {
                    logger.info("RemoteInboxApplicationImpl::fileCreatedEventOccurred - is hidden file - ignoring");
                }
            } else {
                if (logger.isInfoEnabled()) {
                    logger.info("RemoteInboxApplicationImpl::fileCreatedEventOccurred - is unregistered file");
                }
                String newDirName = f.getName();
                boolean isSeries = newDirName.equals(Constants.FILENAME_DC_SERIES);
                boolean isEpisode = newDirName.equals(Constants.FILENAME_DC_EPISODE);
                if (isSeries || isEpisode) {
                    if (logger.isInfoEnabled()) {
                        logger.info("RemoteInboxApplicationImpl::fileCreatedEventOccurred - New file is a metadata file (" + (isSeries ? "Series" : "Episode") + ")");
                    }
                }
                String mainmodelDir = model.getMainDir().getPath();
                mainmodelDir = mainmodelDir.endsWith(File.separator) ? mainmodelDir.substring(0, mainmodelDir.length() - 1) : mainmodelDir;
                String filePath = f.getPath();
                filePath = filePath.endsWith(File.separator) ? filePath.substring(0, filePath.length() - 1) : filePath;
                int nrOfSepMainDir = mainmodelDir.split(Pattern.quote(File.separator)).length + 2;
                int nrOfSepMain = filePath.split(Pattern.quote(File.separator)).length;
                if (!isSeries && (nrOfSepMainDir == nrOfSepMain)) {
                    if (newDirName.contains(".")) {
                        newDirName = newDirName.substring(0, newDirName.lastIndexOf("."));
                    }
                    newDirName += "_" + UUID.randomUUID().toString();
                    String newDirPath = f.getPath();
                    newDirPath = newDirPath.substring(0, newDirPath.lastIndexOf(File.separator));
                    newDirPath = newDirPath.endsWith(File.separator) ? newDirPath.substring(0, newDirPath.length() - 1) : newDirPath;
                    String newDirComplete = newDirPath + File.separator + newDirName;
                    if (logger.isInfoEnabled()) {
                        logger.info("RemoteInboxApplicationImpl::fileCreatedEventOccurred - trying to create folder '" + newDirComplete + "' for found file");
                    }
                    moveFileTo(newDirComplete, f);
                } else {
                    if (model.addRecordingFile(f.getAbsoluteFile())) {
                        if (logger.isInfoEnabled()) {
                            logger.info("RemoteInboxApplicationImpl::fileCreatedEventOccurred - Successfully registered file '" + f.getAbsoluteFile().getAbsolutePath() + "'");
                        }
                        recievingRecordings.put(f.getAbsoluteFile().getParent(), 0l);
                    } else {
                        logger.error("RemoteInboxApplicationImpl::fileCreatedEventOccurred - Error registering file '" + f.getAbsoluteFile().getAbsolutePath() + "'");
                    }
                }
            }
        } else {
            logger.error("RemoteInboxApplicationImpl::fileCreatedEventOccurred - Can't handle " + path + ", ignoring");
        }
        db.updateInboxes(false);
    }

    @Override
    public void fileModifiedEventOccurred(FileModifiedEvent evt, String path) {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxAppicationImpl::fileModifiedEventOccurred - File modified: " + path);
        }
        File f = new File(path);
        if (model.isRecordingFile(f)) {
            setRecordingRecieving(f);
        }
        db.updateInboxes(false);
    }

    @Override
    public void fileDeletedEventOccurred(FileDeletedEvent evt, String path) {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxApplicationImpl::fileDeletedEventOccurred - File removed: " + path);
        }
        try {
            File f = new File(path).getAbsoluteFile();
            deletedRecordings.add(f.getAbsolutePath());

            if (model.isInbox(f)) {
                if (logger.isInfoEnabled()) {
                    logger.info("RemoteInboxApplicationImpl::fileDeletedEventOccurred - Is inbox");
                }
                Inbox inbox = db.getInboxByPath(path);
                for (Recording r : inbox.getRecordings()) {
                    stopIngest(r);
                    stopActiveTask((RecordingImpl) r);
                    inbox.removeRecording(r);
                    db.removeRecording(r);
                    r = null;
                }
                model.removeInbox(f);
            } else if (model.isRecording(f)) {
                if (logger.isInfoEnabled()) {
                    logger.info("RemoteInboxApplicationImpl::fileDeletedEventOccurred - Is recording");
                }
                Recording recording = db.getRecordingByPath(f.getPath());
                stopIngest(recording);
                stopActiveTask((RecordingImpl) recording);
                model.removeRecording(f);
            } else if (model.isRecordingFile(f)) {
                if (logger.isInfoEnabled()) {
                    logger.info("RemoteInboxApplicationImpl::fileDeletedEventOccurred - Is recording file");
                }
                RecordingImpl recording = (RecordingImpl) db.getRecordingByPath(f.getParent());
                stopActiveTask(recording);
                model.removeRecordingFile(f);
            } else {
                logger.error("RemoteInboxApplicationImpl::fileDeletedEventOccurred - Can't handle " + path + ", ignoring");
            }
            db.updateInboxes(false);
        } catch (Exception ex) {
            logger.error("RemoteInboxApplicationImpl::fileDeletedEventOccurred - Exception occurred on deleting a file: " + ex.getMessage());
        } finally {
            model.ensureConsistency();
        }
    }
}
