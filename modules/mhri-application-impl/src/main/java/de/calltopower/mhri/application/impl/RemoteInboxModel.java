/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.application.impl;

import java.io.File;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import de.calltopower.mhri.application.api.Inbox;
import de.calltopower.mhri.application.api.Recording;
import de.calltopower.mhri.application.api.RecordingFile;
import de.calltopower.mhri.application.api.RecordingFile.Type;
import de.calltopower.mhri.application.api.UploadJob;
import de.calltopower.mhri.util.Constants;
import de.calltopower.mhri.util.MHRIFileUtils;

/**
 * RemoteInboxModel
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class RemoteInboxModel {

    private static final Logger logger = Logger.getLogger(RemoteInboxModel.class);
    private final DatabaseAdapter db;
    private final RemoteInboxApplicationImpl application;
    private final String[] mediafileSuffixes;
    private File mainDir;

    public RemoteInboxModel(DatabaseAdapter db, RemoteInboxApplicationImpl application) {
        this.db = db;
        this.application = application;
        mediafileSuffixes = application.getConfig().get(Constants.PROPKEY_MEDIAFILE_SUFFIXES).split(",");
        this.db.loadState();
        for (int it : db.getRecordings().keySet()) {
            Recording recording = db.getRecordings().get(it);
            if (logger.isInfoEnabled()) {
                logger.info(recording.getPath() + " " + recording.getState());
            }
        }
    }

    // getter
    public File getMainDir() {
        return mainDir;
    }

    // setter
    public void setMainDir(File mainDir) {
        this.mainDir = mainDir.getAbsoluteFile();
    }

    // misc
    /*
     * Ensures (at startup) that the model loaded from the DB is still
     * consistent with what is going on in the file system.
     */
    public void ensureConsistency() {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxModel::ensureConsistency - Ensuring consistency");
        }
        List<String> fsFiles = MHRIFileUtils.getInstance().scanDir(getMainDir(), 2);
        Set<String> dbFiles = new HashSet(db.getItemsByPath().keySet());
        for (String path : fsFiles) {
            File f = new File(path).getAbsoluteFile();
            if (f != null) {
                if (dbFiles.contains(path)) {
                    if (logger.isInfoEnabled()) {
                        logger.info("RemoteInboxModel::ensureConsistency - [KNOWN] '" + path + "'");
                    }
                    if (f.isDirectory()) {
                        application.registerDirectory(path);
                    }
                    dbFiles.remove(path);
                } else {
                    if (logger.isInfoEnabled()) {
                        logger.info("RemoteInboxModel::ensureConsistency - [NEW] '" + path + "'");
                    }
                    if (isInbox(f)) {
                        addInbox(f);
                        application.registerDirectory(path);
                    } else if (f.getName().startsWith(".")) {
                        // ignore hidden files
                    } else if (isRecording(f)) {
                        try {
                            Recording rec = addRecording(f);
                            if (rec != null) {
                                if (logger.isInfoEnabled()) {
                                    logger.info("RemoteInboxModel::ensureConsistency - Recording '" + rec.getPath() + "' state: " + rec.getState());
                                }
                            }
                        } catch (Exception e) {
                            logger.error("RemoteInboxModel::ensureConsistency - Exception (recording): " + e.getMessage());
                        }
                        application.registerDirectory(path);
                    } else {
                        try {
                            if (!addRecordingFile(f)) {
                                logger.error("Failed to add recording: " + f.getAbsolutePath());
                            }
                        } catch (Exception e) {
                            logger.error("RemoteInboxModel::ensureConsistency - Exception (recordingfile): " + e.getMessage());
                        }
                    }
                }
            } else {
                logger.error("RemoteInboxModel::ensureConsistency - File is null");
            }
        }
        for (String path : dbFiles) {
            if (logger.isInfoEnabled()) {
                logger.info("RemoteInboxModel::ensureConsistency - [DANGELING]  '" + path + "'");
            }
            try {
                Object item = db.getItemsByPath().get(path);
                if (item instanceof Inbox) {
                    db.removeInbox((Inbox) item);
                } else if (item instanceof Recording) {
                    db.removeRecording((Recording) item);
                } else if (item instanceof RecordingFile) {
                    db.removeRecordingFile((RecordingFile) item);
                }
            } catch (NullPointerException e) {
                logger.error("RemoteInboxModel::ensureConsistency - NullPointerException while removing '" + path + "'");
            }
        }
    }

    public boolean isInbox(File f) {
        return f.getAbsoluteFile().getParentFile().equals(mainDir);
    }

    public boolean isRecording(File f) {
        String parentPath = f.getAbsoluteFile().getParent();
        return db.hasInbox(parentPath);
    }

    public boolean isRecordingFile(File f) {
        String path = f.getAbsoluteFile().getPath();
        return db.hasRecording(path);
    }

    public void addInbox(File f) {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxModel::addInbox - Adding inbox " + f.getAbsoluteFile().getAbsolutePath());
        }
        try {
            db.createInbox(f.getAbsoluteFile().getAbsolutePath(), f.getAbsoluteFile().getName());
        } catch (SQLException ex) {
            logger.error("RemoteInboxModel::addInbox - SQLException " + ex.getMessage());
            try {
                db.dropTables();
                db.shutdown();
                db.construct();
            } catch (SQLException e) {
                logger.error("RemoteInboxModel::addInbox - SQL exception: " + e.getMessage());
                Runtime.getRuntime().halt(0);
            } catch (Exception e) {
                logger.error("RemoteInboxModel::addInbox - Exception: " + e.getMessage());
                Runtime.getRuntime().halt(0);
            }
        }
    }

    public Recording addRecording(File f) {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxModel::addRecording - Adding recording " + f.getAbsoluteFile().getAbsolutePath());
        }
        Inbox parent = db.getInboxByPath(f.getAbsoluteFile().getParent());
        return db.createRecording(parent, f.getAbsoluteFile().getAbsolutePath(), "", parent.getSeriesId(), parent.getWorkflowId(), false);
    }

    public boolean addRecordingFile(File f) {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxModel::addRecordingFile - Adding recording file " + f.getAbsoluteFile().getAbsolutePath());
        }
        String name = f.getName();
        Type type = guessFileType(name);
        String flavor = Constants.FLAVOR_ATTACHMENT_UNKNOWN;
        if (type == Type.TRACK) {
            flavor = guessTrackFlavor(name);
        } else if (type == Type.CATALOG) {
            flavor = guessCatalogFlavor(name);
        }
        Recording parent = db.getRecordingByPath(f.getAbsoluteFile().getParent());
        if (parent != null) {
            RecordingFile rf = db.createRecordingFile(parent, null, f.getAbsoluteFile().getAbsolutePath(), type, flavor);
            if (rf != null) {
                UploadJob job = db.createUploadJob(rf, Integer.toString(rf.getId()), 0);
                if (job != null) {
                    rf.setUploadJob(job);
                    return true;
                }
            }
        }
        return false;
    }

    public void removeInbox(File f) {
        try {
            if (logger.isInfoEnabled()) {
                logger.info("Removing Inbox '" + f.getAbsoluteFile().getAbsolutePath() + "'");
            }
            Inbox inbox = db.getInboxByPath(f.getAbsoluteFile().getAbsolutePath());
            db.removeInbox(inbox);
        } catch (Exception e) {
            logger.error("RemoteInboxModel::removeInbox - Failed to remove Inbox " + f.getAbsoluteFile().getAbsolutePath() + ": " + e.getMessage());
        }
    }

    public void removeRecording(File f) {
        try {
            if (logger.isInfoEnabled()) {
                logger.info("RemoteInboxModel::removeRecording - Removing Recording '" + f.getAbsoluteFile().getAbsolutePath() + "'");
            }
            Recording recording = db.getRecordingByPath(f.getAbsoluteFile().getAbsolutePath());
            db.removeRecording(recording);
        } catch (Exception e) {
            logger.error("RemoteInboxModel::removeRecording - Failed to remove Recording " + f.getAbsoluteFile().getAbsolutePath() + ": " + e.getMessage());
        }
    }

    public void removeRecordingFile(File f) {
        try {
            if (logger.isInfoEnabled()) {
                logger.info("RemoteInboxModel::removeRecordingFile - Removing File '" + f.getAbsoluteFile().getAbsolutePath() + "'");
            }
            db.removeRecordingFile(db.getRecordingFileByPath(f.getAbsoluteFile().getAbsolutePath()));
        } catch (Exception e) {
            logger.error("RemoteInboxModel::removeRecordingFile - Failed to remove File " + f.getAbsoluteFile().getAbsolutePath() + ": " + e.getMessage());
        }
    }

    private Type guessFileType(String name) {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxModel::guessFileType - Guessing type of: " + name);
        }
        if (name.equalsIgnoreCase(Constants.FILENAME_DC_EPISODE)
                || name.equalsIgnoreCase(Constants.FILENAME_DC_SERIES)) {
            if (logger.isInfoEnabled()) {
                logger.info("RemoteInboxModel::guessFileType - Guessed file type: " + Type.CATALOG.toString());
            }
            return Type.CATALOG;
        } else if (MHRIFileUtils.getInstance().endsWithOne(name, mediafileSuffixes)) {
            if (logger.isInfoEnabled()) {
                logger.info("RemoteInboxModel::guessFileType - Guessed file type: " + Type.TRACK.toString());
            }
            return Type.TRACK;
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("RemoteInboxModel::guessFileType - Guessed file type: " + Type.ATTACHMENT.toString());
            }
            return Type.ATTACHMENT;
        }
    }

    private String guessTrackFlavor(String name) {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxModel::guessTrackFlavor - Guessing flavor of: " + name);
        }
        String[] pieces = name.split("\\.");
        if (pieces.length < 2) {
            if (logger.isInfoEnabled()) {
                logger.info("RemoteInboxModel::guessTrackFlavor - Guessed flavor: " + Constants.FLAVOR_PRESENTER);
            }
            return Constants.FLAVOR_PRESENTER;
        }
        String s = pieces[pieces.length - 2]; // get the part before the file extension
        if (s.endsWith(application.getConfig().get(Constants.PROPKEY_PRESENTER_SUFFIX))) {
            if (logger.isInfoEnabled()) {
                logger.info("RemoteInboxModel::guessTrackFlavor - Guessed flavor: " + Constants.FLAVOR_PRESENTER);
            }
            return Constants.FLAVOR_PRESENTER;
        } else if (s.endsWith(application.getConfig().get(Constants.PROPKEY_PRESENTATION_SUFFIX))) {
            if (logger.isInfoEnabled()) {
                logger.info("RemoteInboxModel::guessTrackFlavor - Guessed flavor: " + Constants.FLAVOR_PRESENTATION);
            }
            return Constants.FLAVOR_PRESENTATION;
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("RemoteInboxModel::guessTrackFlavor - Guessed flavor: " + Constants.FLAVOR_ATTACHMENT_UNKNOWN);
            }
            return Constants.FLAVOR_ATTACHMENT_UNKNOWN;
        }
    }

    private String guessCatalogFlavor(String name) {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxModel::guessCatalogFlavor - Guessing catalog flavor of: " + name);
        }
        if (name.equalsIgnoreCase(Constants.FILENAME_DC_EPISODE)) {
            if (logger.isInfoEnabled()) {
                logger.info("RemoteInboxModel::guessCatalogFlavor - Guessed catalog flavor: " + Constants.FLAVOR_DC_EPISODE);
            }
            return Constants.FLAVOR_DC_EPISODE;
        } else if (name.equalsIgnoreCase(Constants.FILENAME_DC_SERIES)) {
            if (logger.isInfoEnabled()) {
                logger.info("RemoteInboxModel::guessCatalogFlavor - Guessed catalog flavor: " + Constants.FLAVOR_DC_SERIES);
            }
            return Constants.FLAVOR_DC_SERIES;
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("RemoteInboxModel::guessCatalogFlavor - Guessed catalog flavor: " + Constants.FLAVOR_CATALOG_UNKNOWN);
            }
            return Constants.FLAVOR_CATALOG_UNKNOWN;
        }
    }
}
