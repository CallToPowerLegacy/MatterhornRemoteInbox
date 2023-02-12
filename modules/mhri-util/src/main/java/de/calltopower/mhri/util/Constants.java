/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;

/**
 * Constants
 *
 * @date 09.08.2012
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class Constants {

    public final static String MHRI = "MHRI";
    public final static String MHRI_VERSION = "1.8.0"; // nothing, "alpha", "beta" or "release candidate"
    public final static String MHRI_BUILD = "20.03.2015 1";
    public final static String MHRI_LOCK_FILE_NAME = "__MHRI_lock-file";
    public final static String MHRI_LOCK_FILE_LOCATION = "user.home"; // has to be a system property
    public final static String MHRI_LOCK_FILE_SUFFIX = ".tmp";
    // split file
    public final static String MHRI_SPLIT_FILE_LOCATION = "java.io.tmpdir"; // has to be a system property
    public final static String MHRI_SPLIT_FILE_SUFFIX = ".splt";
    public final static String MHRI_TMP_FILE_DIR_NAME_WO_PATH = "MHRI";
    public final static String MHRI_TMP_FILE_NAME_WO_PATH = "MHRI_TMP";
    // config
    public static final String DEFAULT_CONFIG_PATH = "/config/default.properties";
    public static final String CONFIG_PATH = "./conf/mhri.properties";
    public static final String ENDPOINTS_PATH = "./conf/endpoints.properties";
    public static final String LOG_FILE_PATH = "logs/mhri.log";
    // misc variables
    public static final String FILENAME_DC_EPISODE = "episode.xml";
    public static final String FILENAME_DC_SERIES = "series.xml";
    public static final String FLAVOR_PRESENTER = "presenter/source";
    public static final String FLAVOR_PRESENTATION = "presentation/source";
    public static final String FLAVOR_DC_EPISODE = "dublincore/episode";
    public static final String FLAVOR_DC_SERIES = "dublincore/series";
    public static final String FLAVOR_CATALOG_UNKNOWN = "catalog/unknown";
    public final static String FLAVOR_ATTACHMENT_UNKNOWN = "attachment/unknown";
    public final static String SERIES_LIST_FORMAT = ".xml";
    public final static String SERIES_CATALOG_FORMAT = ".xml";
    // database requests
    public static final String SQL_LOADALL_INBOXES = "SELECT * FROM Inbox WHERE 1;";
    public static final String SQL_LOADALL_RECORDINGS = "SELECT * FROM Recording WHERE 1;";
    public static final String SQL_LOADALL_FILES = "SELECT * FROM File WHERE 1;";
    public static final String SQL_LOADALL_UPLOADJOBS = "SELECT * FROM UploadJob WHERE 1;";
    public static final String SQL_INSERT_INBOX = "INSERT INTO Inbox (path,name,seriesId,workflowId) VALUES (?,?,?,?);";
    public static final String SQL_INSERT_RECORDING = "INSERT INTO Recording (inbox,state,path,mediapackage,seriesId,workflowId,trimFlag) VALUES (?,?,?,?,?,?,?);";
    public static final String SQL_INSERT_FILE = "INSERT INTO File (recording,uploadJob,path,type,flavor) VALUES (?,?,?,?,?);";
    public static final String SQL_INSERT_UPLOADJOB = "INSERT INTO UploadJob (file,jobId,state,chunkSize,chunksTotal,currentChunk) VALUES (?,?,?,?,?,?);";
    public static final String SQL_UPDATE_INBOX = "UPDATE Inbox SET path=?,name=?,seriesId=?,workflowId=? WHERE id=?;";
    public static final String SQL_UPDATE_RECORDING = "UPDATE Recording SET inbox=?,state=?,path=?,mediapackage=?,seriesId=?,workflowId=?,trimFlag=? WHERE id=?;";
    public static final String SQL_UPDATE_FILE = "UPDATE File SET recording=?,uploadJob=?,path=?,type=?,flavor=? WHERE id=?;";
    public static final String SQL_UPDATE_UPLOADJOB = "UPDATE UploadJob SET file=?,jobId=?,state=?,chunkSize=?,chunksTotal=?,currentChunk=? WHERE id=?;";
    public static final String SQL_DELETE_INBOX = "DELETE FROM Inbox WHERE id=?;";
    public static final String SQL_DELETE_RECORDING = "DELETE FROM Recording WHERE id=?;";
    public static final String SQL_DELETE_FILE = "DELETE FROM File WHERE id=?;";
    public static final String SQL_DELETE_UPLOADJOB = "DELETE FROM UploadJob WHERE id=?;";
    public static final String SQL_DROP_TABLE_INBOX = "DROP TABLE IF EXISTS Inbox;";
    public static final String SQL_DROP_TABLE_RECORDING = "DROP TABLE IF EXISTS Recording;";
    public static final String SQL_DROP_TABLE_FILE = "DROP TABLE IF EXISTS File;";
    public static final String SQL_DROP_TABLE_UPLOADJOB = "DROP TABLE IF EXISTS UploadJob;";
    // misc
    public static final String PROPKEY_FIRSTSTART = "de.calltopower.mhri.firstStart";
    public static final String PROPKEY_START_AS_SERVICE = "de.calltopower.mhri.startAsService";
    public static final String PROPKEY_CHECK_FOR_UPDATES = "de.calltopower.mhri.checkForUpdates";
    public static final String PROPKEY_SHOW_TOOLTIPS = "de.calltopower.mhri.showTooltips";
    public static final String PROPKEY_CHUNKED_UPLOAD = "de.calltopower.mhri.chunkedUpload";
    public static final String PROPKEY_CHUNKED_UPLOAD_FALLBACK = "de.calltopower.mhri.chunkedUploadFallback";
    public static final String PROPKEY_RETRY_INGESTING_FAILED_RECORDINGS = "de.calltopower.mhri.retryIngestingFailedRecordings";
    public final static String PROPKEY_CHUNKSIZE = "de.calltopower.mhri.upload.chunksize";
    public static final String PROPKEY_MAXCONCURRENTUPLOADS = "de.calltopower.mhri.upload.maxconcurrent";
    public static final String PROPKEY_UPLOADNEXTCHUNKWAITMS = "de.calltopower.mhri.upload.nextChunkWaitMS";
    public static final String PROPKEY_UPLOADFINALIZINGWAITMS = "de.calltopower.mhri.upload.finalizingWaitMS";
    public static final String PROPKEY_INGESTFAILEDRECORDINGSTRIES = "de.calltopower.mhri.ingestFailedRecordingsTries";
    public static final String PROPKEY_POLLTIME = "de.calltopower.mhri.polltime";
    public static final String PROPKEY_TMPFILECHECKRATE = "de.calltopower.mhri.tmpfilecheckrate";
    public static final String PROPKEY_RECORDINGSTATECHECKERRATE = "de.calltopower.mhri.recordingStateCheckerRate";
    public static final String PROPKEY_CONDUCTORRATE = "de.calltopower.mhri.conductorRate";
    public static final String PROPKEY_RENAMINGTIMEOUT = "de.calltopower.mhri.renamingTimeout";
    public static final String PROPKEY_UPDATETRIES = "de.calltopower.mhri.update.tries";
    // server settings
    public static final String PROPKEY_HOST = "de.calltopower.mhri.server.host";
    public static final String PROPKEY_PORT = "de.calltopower.mhri.server.port";
    public static final String PROPKEY_USERNAME = "de.calltopower.mhri.server.auth.username";
    public static final String PROPKEY_PASSWORD = "de.calltopower.mhri.server.auth.password";
    // version server
    public final static String PROPKEY_JVERSIONSERVER_URL = "de.calltopower.mhri.versionserver.url";
    public final static String PROPKEY_JVERSIONSERVER_PORT = "de.calltopower.mhri.versionserver.port";
    public final static String PROPKEY_JVERSIONSERVER_PATH = "de.calltopower.mhri.versionserver.path";
    public final static String PROPKEY_JVERSIONSERVER_API_VERSION = "de.calltopower.mhri.versionserver.api.version";
    public final static String PROPKEY_POSTPARAM_JVERSIONSERVER_NAME = "de.calltopower.mhri.versionserver.post.name";
    // log server
    public final static String PROPKEY_MHRI_KEY_FILE = "de.calltopower.mhri.keyfile";
    public final static String PROPKEY_JLOGSERVER_URL = "de.calltopower.mhri.logserver.url";
    public final static String PROPKEY_JLOGSERVER_PORT = "de.calltopower.mhri.logserver.port";
    public final static String PROPKEY_JLOGSERVER_PATH = "de.calltopower.mhri.logserver.path";
    public final static String PROPKEY_JLOGSERVER_API_VERSION = "de.calltopower.mhri.logserver.api.version";
    public final static String PROPKEY_POSTPARAM_JLOGSERVER_KEY = "de.calltopower.mhri.logserver.post.key";
    public final static String PROPKEY_POSTPARAM_JLOGSERVER_NAME = "de.calltopower.mhri.logserver.post.name";
    public final static String PROPKEY_POSTPARAM_JLOGSERVER_VERSION = "de.calltopower.mhri.logserver.post.version";
    public final static String PROPKEY_POSTPARAM_JLOGSERVER_BUILD = "de.calltopower.mhri.logserver.post.build";
    public final static String PROPKEY_POSTPARAM_JLOGSERVER_LOGLEVEL = "de.calltopower.mhri.logserver.post.loglevel";
    public final static String PROPKEY_POSTPARAM_JLOGSERVER_AUTOGENERATED = "de.calltopower.mhri.logserver.post.autogenerated";
    public final static String PROPKEY_POSTPARAM_JLOGSERVER_MANUALDESCRIPTION = "de.calltopower.mhri.logserver.post.manualdescription";
    public final static String PROPKEY_POSTPARAM_JLOGSERVER_LOGFILE = "de.calltopower.mhri.logserver.post.logfile";
    // inbox settings
    public static final String PROPKEY_MAINDIR = "de.calltopower.mhri.maindir";
    public static final String PROPKEY_DEFAULTINBOX = "de.calltopower.mhri.default.inbox";
    // media settings
    public static final String PROPKEY_PRESENTATION_SUFFIX = "de.calltopower.mhri.presentation.suffix";
    public static final String PROPKEY_PRESENTER_SUFFIX = "de.calltopower.mhri.presenter.suffix";
    public static final String PROPKEY_MEDIAFILE_SUFFIXES = "de.calltopower.mhri.mediafile.suffixes";
    public static final String PROPKEY_DEFAULT_WORKFLOW = "de.calltopower.mhri.default.workflow";
    // misc REST
    public static final String PROPKEY_MEDIAPACKAGE_ADD_TRACK_URL = "de.calltopower.mhri.mediapackage.addtrack";
    public static final String PROPKEY_SERIES_STARTPAGE = "de.calltopower.mhri.series.startpage";
    public static final String PROPKEY_SERIES_COUNT = "de.calltopower.mhri.series.count";
    public static final String PROPKEY_SERIES = "de.calltopower.mhri.series";
    public static final String PROPKEY_SERIES_CREATE = "de.calltopower.mhri.series.create";
    public static final String PROPKEY_SERIES_LIST_URL = "de.calltopower.mhri.series.list";
    public static final String PROPKEY_INGEST_ADD_TRACK_URL_13 = "de.calltopower.mhri.ingest.addtrack13";
    public static final String PROPKEY_INGEST_ADD_CATALOG_URL_13 = "de.calltopower.mhri.ingest.addcatalog13";
    public static final String PROPKEY_INGEST_JOB_URL = "de.calltopower.mhri.ingest.job";
    public static final String PROPKEY_INGEST_NEWJOB_URL = "de.calltopower.mhri.ingest.newjob";
    public static final String PROPKEY_INGEST_ADD_CATALOG_URL = "de.calltopower.mhri.ingest.addcatalog";
    public static final String PROPKEY_INGEST_INGEST_URL = "de.calltopower.mhri.ingest.ingest";
    public static final String PROPKEY_INGEST_CREATE_MP_URL = "de.calltopower.mhri.ingest.createmediapackage";
    // GET REST
    public static final String PROPKEY_GETPARAM_SERIES_COUNT = "de.calltopower.mhri.get.series.count";
    public static final String PROPKEY_GETPARAM_SERIES_STARTPAGE = "de.calltopower.mhri.get.series.startpage";
    public static final String PROPKEY_GETPARAM_WORKFLOW = "de.calltopower.mhri.get.workflow";
    public static final String PROPKEY_GETPARAM_WORKFLOW_DEFINITIONS = "de.calltopower.mhri.get.workflow.definitions";
    public static final String PROPKEY_GETPARAM_WORKFLOW_INSTANCES = "de.calltopower.mhri.get.workflow.instances";
    public static final String PROPKEY_GETPARAM_WORKFLOW_INSTANCES_MEDIAPACKAGE = "de.calltopower.mhri.get.workflow.instances.mediapackage";
    // POST REST
    public static final String PROPKEY_POSTPARAM_STARTPROCESSING_MEDIAPACKAGE_13 = "de.calltopower.mhri.post.startprocessing.mediapackage13";
    public static final String PROPKEY_POSTPARAM_STARTPROCESSING_MEDIAPACKAGE = "de.calltopower.mhri.post.startprocessing.mediapackage";
    public static final String PROPKEY_POSTPARAM_INGEST_FILE_13 = "de.calltopower.mhri.post.addtrack.file13";
    public static final String PROPKEY_POSTPARAM_INGEST_FLAVOR_13 = "de.calltopower.mhri.post.addtrack.flavor13";
    public static final String PROPKEY_POSTPARAM_INGEST_MEDIAPACKAGE_13 = "de.calltopower.mhri.post.addtrack.mediapackage13";
    public static final String PROPKEY_POSTPARAM_ADDTRACK_TRACKURI = "de.calltopower.mhri.post.addtrack.trackuri";
    public static final String PROPKEY_POSTPARAM_ADDTRACK_FLAVOR = "de.calltopower.mhri.post.addtrack.flavor";
    public static final String PROPKEY_POSTPARAM_ADDTRACK_MEDIAPACKAGE = "de.calltopower.mhri.post.addtrack.mediapackage";
    public static final String PROPKEY_POSTPARAM_UPLOADNEXTCHUNK_FILEDATA = "de.calltopower.mhri.post.uploadnextchunk.filedata";
    public static final String PROPKEY_POSTPARAM_UPLOADNEXTCHUNK_JOBID = "de.calltopower.mhri.post.uploadnextchunk.jobid";
    public static final String PROPKEY_POSTPARAM_UPLOADNEXTCHUNK_CHUNKNUMBER = "de.calltopower.mhri.post.uploadnextchunk.chunknumber";
    public static final String PROPKEY_POSTPARAM_ADDCATALOG_MEDIAPACKAGE = "de.calltopower.mhri.post.addcatalog.mediapackage";
    public static final String PROPKEY_POSTPARAM_ADDCATALOG_DUBLINECORE = "de.calltopower.mhri.post.addcatalog.dublincore";
    public static final String PROPKEY_POSTPARAM_ADDCATALOG_FLAVOR = "de.calltopower.mhri.post.addcatalog.flavor";
    public static final String PROPKEY_POSTPARAM_NEWJOB_MEDIAPACKAGE = "de.calltopower.mhri.post.newjob.mediapackage";
    public static final String PROPKEY_POSTPARAM_NEWJOB_CHUNKSIZE = "de.calltopower.mhri.post.newjob.chunksize";
    public static final String PROPKEY_POSTPARAM_NEWJOB_FLAVOR = "de.calltopower.mhri.post.newjob.flavor";
    public static final String PROPKEY_POSTPARAM_NEWJOB_FILESIZE = "de.calltopower.mhri.post.newjob.filesize";
    public static final String PROPKEY_POSTPARAM_NEWJOB_FILENAME = "de.calltopower.mhri.post.newjob.filename";
    public static final String PROPKEY_POSTPARAM_SERIES_CREATE_SERIES = "de.calltopower.mhri.post.series.create.series";
    public static final String PROPKEY_POSTPARAM_SERIES_CREATE_ACL = "de.calltopower.mhri.post.series.create.acl";
    /**
     * Constants class
     */
    private static final Logger logger = Logger.getLogger(Constants.class);
    private static Constants instance = null;
    private static boolean localeSet = true;
    private static ResourceBundle strings = null;

    private Constants() {
        Locale defaultLocale = Locale.getDefault();
        try {
            strings = ResourceBundle.getBundle("/localization/strings", defaultLocale);
        } catch (NullPointerException | MissingResourceException | IllegalArgumentException ex) {
            localeSet = false;
            logger.error("Constants::Constants - No localization found");
        }
    }

    public static Constants getInstance() {
        if (instance == null) {
            instance = new Constants();
        }
        return instance;
    }

    public String getLocalizedString(String str) {
        if (localeSet) {
            String loc_str = str;
            try {
                loc_str = strings.getString(str);
            } catch (NullPointerException | MissingResourceException | ClassCastException ex) {
                logger.error("Constants::getLocalizedString - String not found: " + str);
            }
            return loc_str;
        } else {
            return str;
        }
    }
}
