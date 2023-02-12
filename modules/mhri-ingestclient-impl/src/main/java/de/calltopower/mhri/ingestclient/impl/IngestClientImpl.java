/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.ingestclient.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.RoundingMode;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import org.apache.commons.io.IOUtils;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import de.calltopower.mhri.application.api.Recording;
import de.calltopower.mhri.application.api.RecordingFile;
import de.calltopower.mhri.application.api.UploadJob;
import de.calltopower.mhri.ingestclient.api.IngestClient;
import de.calltopower.mhri.ingestclient.api.IngestClientException;
import de.calltopower.mhri.util.Constants;
import de.calltopower.mhri.util.UploadableFileState;
import de.calltopower.mhri.util.conf.Configuration;

/**
 * IngestClientImpl - Implements IngestClient
 *
 * @date 28.06.2013
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class IngestClientImpl implements IngestClient {

    private static final Logger logger = Logger.getLogger(IngestClientImpl.class);
    // authentication
    private String username = "";
    private String password = "";
    // REST endpoints URLs
    private static String ingest_createMP_URL = "/ingest/createMediaPackage";
    private static String ingest_newJob_URL = "/upload/newjob";
    private static String ingest_job_URL = "/upload/job";
    private static String ingest_ingest_URL = "/ingest/ingest";
    private static String ingest_addCatalog_URL = "/ingest/addDCCatalog";
    private static String mediapackage_addTrack_URL = "/mediapackage/addTrack";
    private static String ingest_addTrack_URL_13 = "/ingest/addTrack";
    private static String ingest_addCatalog_URL_13 = "/ingest/addDCCatalog";
    // GET parameters
    public static String getparam_workflow = "/workflow";
    public static String getparam_workflow_instances = "/instances";
    public static String getparam_workflow_instances_mediapackage = "mp";
    // POST parameters
    private static String postparam_addTrack_mediaPackage = "mediapackage";
    private static String postparam_addTrack_trackUri = "trackUri";
    private static String postparam_addTrack_flavor = "flavor";
    private static String postparam_addCatalog_mediaPackage = "mediapackage";
    private static String postparam_addCatalog_dublineCore = "dublincore";
    private static String postparam_addCatalog_flavor = "flavor";
    private static String postparam_startProcessing_mediaPackage = "mediaPackage";
    // POST parameters for UploadableFileImpl
    private String postparam_uploadNextChunk_chunknumber = "chunknumber";
    private String postparam_uploadNextChunk_jobId = "jobID";
    private String postparam_uploadNextChunk_fileData = "filedata";
    private String postparam_newJob_fileName = "filename";
    private String postparam_newJob_fileSize = "filesize";
    private String postparam_newJob_chunkSize = "chunksize";
    private String postparam_newJob_flavor = "flavor";
    private String postparam_newJob_mediaPackage = "mediapackage";
    // POST parameters for UploadableFileImpl for MH 1.3
    private static String postparam_ingest_mediaPackage_13 = "mediapackage";
    private static String postparam_ingest_flavor_13 = "flavor";
    private static String postparam_ingest_file_13 = "file";
    private static String postparam_startProcessing_mediaPackage_13 = "MEDIAPACKAGE";
    // misc
    private final Configuration config;
    private String matterhornHost = "localhost";
    private int serverPort = 8080;
    private int chunkSize = 1024 * 1024 * 100; // in byte
    private final ExecutorService threadPool = Executors.newCachedThreadPool(); // create new threads as needed, reuse previously constructed threads
    // Apache Commons
    private CookieStore cookieStore = null;
    private DefaultHttpClient httpClient = null;
    private HttpContext client_context = null;
    private HttpPost httppost = null;
    private HttpGet httpget = null;
    private UploadableFileImpl uploadableFile = null;
    private UploadableFile13Impl uploadableFile13 = null;
    // ID
    private String id = "";

    public IngestClientImpl(Configuration config, String recordingPath) {
        this.config = config;
        id = recordingPath;
        readProperties();

        String uName = config.get(Constants.PROPKEY_USERNAME);
        String pWord = config.get(Constants.PROPKEY_PASSWORD);
        setCredentials(uName, pWord);
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public String getInstanceXML(String mediapackageID) {
        if (logger.isInfoEnabled()) {
            logger.info("IngestClientImpl::getInstanceXML - checking network connection");
        }
        DefaultHttpClient _httpClient = new DefaultHttpClient();
        HttpPost _httppost = null;
        HttpGet _httpget = null;
        try {
            _httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
            CookieStore _cookieStore = new BasicCookieStore();
            HttpContext _client_context = new BasicHttpContext();
            _client_context.setAttribute(ClientContext.COOKIE_STORE, _cookieStore);

            // POST /j_spring_security_check
            // j_username: Username
            // j_password: Password
            // Return value description: Cookie
            URI uri_login = new URI("http", null, getBaseURL(), getPort(), "/j_spring_security_check", null, null);
            if (logger.isInfoEnabled()) {
                logger.info("IngestClientImpl::getInstanceXML - About to send POST request: " + uri_login.toURL().toString());
            }
            _httppost = new HttpPost(uri_login.toURL().toString());

            List<NameValuePair> nameValuePairs = new ArrayList<>(3);
            nameValuePairs.add(new BasicNameValuePair("j_username", this.username));
            nameValuePairs.add(new BasicNameValuePair("j_password", this.password));
            nameValuePairs.add(new BasicNameValuePair("submit", "Login"));
            _httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));

            HttpResponse response = _httpClient.execute(_httppost, _client_context);

            String location = response.getHeaders("Location")[0].getValue();

            if (logger.isInfoEnabled()) {
                logger.info("IngestClientImpl::getInstanceXML - Status code: " + response.getStatusLine().getStatusCode() + ", Location: " + location);
            }
            int statCode = response.getStatusLine().getStatusCode();
            if (((statCode != 200) && (statCode != 302)) || location.endsWith("error")) {
                logger.error("IngestClientImpl::getInstanceXML - Status Code: " + response.getStatusLine().getStatusCode());
                return null;
            }
            _httppost.releaseConnection();
            try {
                String path = IngestClientImpl.getparam_workflow + IngestClientImpl.getparam_workflow_instances;
                String query = IngestClientImpl.getparam_workflow_instances_mediapackage + "=" + mediapackageID;
                URI uri = new URI("http", null, getBaseURL(), getPort(), path, query, null);
                _httpget = new HttpGet(uri.toURL().toString());
                if (logger.isInfoEnabled()) {
                    logger.info("IngestClientImpl::createNewMediaPackage - About to send GET request: " + uri.toURL().toString());
                }

                HttpResponse response_get = _httpClient.execute(_httpget, _client_context);

                if (logger.isInfoEnabled()) {
                    logger.info("IngestClientImpl::getInstanceXML - Status code: " + response_get.getStatusLine().getStatusCode());
                }
                String instanceJSON = "";
                if (response_get.getStatusLine().getStatusCode() == 200) {
                    HttpEntity entity = response_get.getEntity();
                    if (entity != null) {
                        InputStream stream = entity.getContent();
                        instanceJSON = IOUtils.toString(stream);
                        IOUtils.closeQuietly(stream);
                        EntityUtils.consume(entity);
                    } else {
                        logger.error("IngestClientImpl::getInstanceXML - Entity is null");
                    }
                } else {
                    logger.error("IngestClientImpl::createNewMediaPackage - IngestClientException(NETWORK_ERROR) 1, Status Code: " + response.getStatusLine().getStatusCode());
                    throw new IngestClientException(
                            "Got "
                            + response.getStatusLine().getStatusCode()
                            + " when attempting to create MediaPackage.", IngestClientException.Type.NETWORK_ERROR);
                }
                return instanceJSON;
            } catch (URISyntaxException | IOException | IllegalStateException | IngestClientException ex) {
                logger.error("IngestClientImpl::getInstanceXML - Did not get any State: " + ex.getMessage());
            } finally {
                if (_httpget != null) {
                    _httpget.releaseConnection();
                }
            }
            return null;
        } catch (URISyntaxException | IOException ex) {
            logger.error("IngestClientImpl::getInstanceXML - Exception: " + ex.getMessage());
        } finally {
            if (_httppost != null) {
                _httppost.releaseConnection();
            }
            if (_httpget != null) {
                _httpget.releaseConnection();
            }
            _httpClient.getConnectionManager().shutdown();
        }
        return null;
    }

    private void readProperties() {
        // read
        matterhornHost = config.get(Constants.PROPKEY_HOST).toLowerCase();
        matterhornHost = getBaseURL();
        serverPort = Integer.parseInt(config.get(Constants.PROPKEY_PORT));
        chunkSize = 1024 * 1024 * Integer.parseInt(config.get(Constants.PROPKEY_CHUNKSIZE));

        ingest_createMP_URL = config.get(Constants.PROPKEY_INGEST_CREATE_MP_URL);
        ingest_newJob_URL = config.get(Constants.PROPKEY_INGEST_NEWJOB_URL);
        ingest_job_URL = config.get(Constants.PROPKEY_INGEST_JOB_URL);
        ingest_ingest_URL = config.get(Constants.PROPKEY_INGEST_INGEST_URL);
        ingest_addCatalog_URL = config.get(Constants.PROPKEY_INGEST_ADD_CATALOG_URL);
        mediapackage_addTrack_URL = config.get(Constants.PROPKEY_MEDIAPACKAGE_ADD_TRACK_URL);

        ingest_addTrack_URL_13 = config.get(Constants.PROPKEY_INGEST_ADD_TRACK_URL_13);
        ingest_addCatalog_URL_13 = config.get(Constants.PROPKEY_INGEST_ADD_CATALOG_URL_13);

        getparam_workflow = config.get(Constants.PROPKEY_GETPARAM_WORKFLOW);
        getparam_workflow_instances = config.get(Constants.PROPKEY_GETPARAM_WORKFLOW_INSTANCES);
        getparam_workflow_instances_mediapackage = config.get(Constants.PROPKEY_GETPARAM_WORKFLOW_INSTANCES_MEDIAPACKAGE);

        postparam_addTrack_mediaPackage = config.get(Constants.PROPKEY_POSTPARAM_ADDTRACK_MEDIAPACKAGE);
        postparam_addTrack_trackUri = config.get(Constants.PROPKEY_POSTPARAM_ADDTRACK_TRACKURI);
        postparam_addTrack_flavor = config.get(Constants.PROPKEY_POSTPARAM_ADDTRACK_FLAVOR);
        postparam_addCatalog_mediaPackage = config.get(Constants.PROPKEY_POSTPARAM_ADDCATALOG_MEDIAPACKAGE);
        postparam_addCatalog_dublineCore = config.get(Constants.PROPKEY_POSTPARAM_ADDCATALOG_DUBLINECORE);
        postparam_addCatalog_flavor = config.get(Constants.PROPKEY_POSTPARAM_ADDCATALOG_FLAVOR);
        postparam_startProcessing_mediaPackage = config.get(Constants.PROPKEY_POSTPARAM_STARTPROCESSING_MEDIAPACKAGE);

        // read
        postparam_uploadNextChunk_chunknumber = config.get(Constants.PROPKEY_POSTPARAM_UPLOADNEXTCHUNK_CHUNKNUMBER);
        postparam_uploadNextChunk_jobId = config.get(Constants.PROPKEY_POSTPARAM_UPLOADNEXTCHUNK_JOBID);
        postparam_uploadNextChunk_fileData = config.get(Constants.PROPKEY_POSTPARAM_UPLOADNEXTCHUNK_FILEDATA);
        postparam_newJob_fileName = config.get(Constants.PROPKEY_POSTPARAM_NEWJOB_FILENAME);
        postparam_newJob_fileSize = config.get(Constants.PROPKEY_POSTPARAM_NEWJOB_FILESIZE);
        postparam_newJob_chunkSize = config.get(Constants.PROPKEY_POSTPARAM_NEWJOB_CHUNKSIZE);
        postparam_newJob_flavor = config.get(Constants.PROPKEY_POSTPARAM_NEWJOB_FLAVOR);
        postparam_newJob_mediaPackage = config.get(Constants.PROPKEY_POSTPARAM_NEWJOB_MEDIAPACKAGE);

        postparam_ingest_mediaPackage_13 = config.get(Constants.PROPKEY_POSTPARAM_INGEST_MEDIAPACKAGE_13);
        postparam_ingest_flavor_13 = config.get(Constants.PROPKEY_POSTPARAM_INGEST_FLAVOR_13);
        postparam_ingest_file_13 = config.get(Constants.PROPKEY_POSTPARAM_INGEST_FILE_13);
        postparam_startProcessing_mediaPackage_13 = config.get(Constants.PROPKEY_POSTPARAM_STARTPROCESSING_MEDIAPACKAGE_13);
    }

    private void releaseConnections() {
        if (cookieStore != null) {
            cookieStore.clear();
            cookieStore = null;
        }
        if (httpClient != null) {
            httpClient.getConnectionManager().shutdown();
        }
        if (httpget != null) {
            httpget.releaseConnection();
        }
        if (httppost != null) {
            httppost.releaseConnection();
        }
    }

    private void setCredentials(String _username, String _password) {
        this.username = _username;
        this.password = _password;
    }

    private HttpContext initHttpClient(DefaultHttpClient httpClient) throws IngestClientException, URISyntaxException {
        try {
            httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
            cookieStore = new BasicCookieStore();
            client_context = new BasicHttpContext();
            client_context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

            // POST /j_spring_security_check
            // j_username: Username
            // j_password: Password
            // Return value description: Cookie
            URI uri = new URI("http", null, getBaseURL(), getPort(), "/j_spring_security_check", null, null);
            if (logger.isInfoEnabled()) {
                logger.info("IngestClientImpl::initHttpClient - About to send POST request: " + uri.toURL().toString());
            }
            httppost = new HttpPost(uri.toURL().toString());

            List<NameValuePair> nameValuePairs = new ArrayList<>(3);
            nameValuePairs.add(new BasicNameValuePair("j_username", this.username));
            nameValuePairs.add(new BasicNameValuePair("j_password", this.password));
            nameValuePairs.add(new BasicNameValuePair("submit", "Login"));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));

            HttpResponse response = httpClient.execute(httppost, client_context);

            String location = response.getHeaders("Location")[0].getValue();

            if (logger.isInfoEnabled()) {
                logger.info("IngestClientImpl::initHttpClient - Status code: " + response.getStatusLine().getStatusCode() + ", Location: " + location);
            }
            int statCode = response.getStatusLine().getStatusCode();
            if (((statCode != 200) && (statCode != 302)) || location.endsWith("error")) {
                logger.error("IngestClientImpl::initHttpClient - Could not authenticate: IngestClientException(CLIENT_ERROR), Status Code: " + response.getStatusLine().getStatusCode());
                throw new IngestClientException("Could not authenticate", IngestClientException.Type.CLIENT_ERROR);
            }
            return client_context;
        } catch (ConnectionClosedException ex) {
            logger.error("IngestClientImpl::initHttpClient - IngestClientException(NETWORK_ERROR)"
                    + "-- connection closed");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
        } catch (NoHttpResponseException ex) {
            logger.error("IngestClientImpl::initHttpClient - IngestClientException(SERVER_ERROR)"
                    + "-- dropped connection without any response. Maybe the server is under heavy load.");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
        } catch (SocketException ex) {
            logger.error("IngestClientImpl::initHttpClient - IngestClientException(SERVER_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
        } catch (IOException ex) {
            logger.error("IngestClientImpl::initHttpClient - IngestClientException(NETWORK_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
        } catch (URISyntaxException ex) {
            logger.error("IngestClientImpl::initHttpClient - URISyntaxException");
            throw ex;
        } finally {
            httppost.releaseConnection();
        }
    }

    private String getBaseURL() {
        String s = this.matterhornHost.startsWith("http://") ? this.matterhornHost.substring(7) : this.matterhornHost;
        return s;
    }

    private int getPort() {
        return this.serverPort;
    }

    // misc
    @Override
    public FutureTask createNewMediaPackage() throws IngestClientException {
        FutureTask<String> task = new FutureTask<>(new Callable<String>() {
            @Override
            public String call() throws IngestClientException, URISyntaxException {
                String mediapackageXml = "";
                httpClient = new DefaultHttpClient();
                client_context = initHttpClient(httpClient);
                try {
                    URI uri = new URI("http", null, getBaseURL(), getPort(), IngestClientImpl.ingest_createMP_URL, null, null);
                    httpget = new HttpGet(uri.toURL().toString());
                    if (logger.isInfoEnabled()) {
                        logger.info("IngestClientImpl::createNewMediaPackage - About to send GET request: " + uri.toURL().toString());
                    }

                    HttpResponse response = httpClient.execute(httpget, client_context);

                    if (logger.isInfoEnabled()) {
                        logger.info("IngestClientImpl::createNewMediaPackage - Status code: " + response.getStatusLine().getStatusCode());
                    }
                    if (response.getStatusLine().getStatusCode() == 200) {
                        HttpEntity entity = response.getEntity();
                        if (entity != null) {
                            InputStream stream = entity.getContent();
                            mediapackageXml = IOUtils.toString(stream);
                            IOUtils.closeQuietly(stream);
                            EntityUtils.consume(entity);
                        } else {
                            logger.error("IngestClientImpl::createNewMediaPackage - IngestClientException(CLIENT_ERROR)");
                            throw new IngestClientException("Entity is null", IngestClientException.Type.CLIENT_ERROR);
                        }
                    } else {
                        logger.error("IngestClientImpl::createNewMediaPackage - IngestClientException(NETWORK_ERROR) 1, Status Code: " + response.getStatusLine().getStatusCode());
                        throw new IngestClientException(
                                "Got "
                                + response.getStatusLine().getStatusCode()
                                + " when attempting to create MediaPackage.", IngestClientException.Type.NETWORK_ERROR);
                    }
                    if (mediapackageXml.isEmpty()) {
                        logger.error("IngestClientImpl::createNewMediaPackage - IngestClientException(NETWORK_ERROR) 3");
                        throw new IngestClientException("No Mediapackage XML could be retrieved.", IngestClientException.Type.NETWORK_ERROR);
                    }
                    return mediapackageXml;
                } catch (IngestClientException ex) {
                    logger.error("IngestClientImpl::createNewMediaPackage - IngestClientException(NETWORK_ERROR) 3");
                    throw ex;
                } catch (URISyntaxException ex) {
                    logger.error("IngestClientImpl::createNewMediaPackage - IngestClientException(CLIENT_ERROR)");
                    throw new IngestClientException(ex.getMessage(), IngestClientException.Type.CLIENT_ERROR);
                } catch (ConnectionClosedException ex) {
                    logger.error("IngestClientImpl::createNewMediaPackage - IngestClientException(NETWORK_ERROR)"
                            + "-- connection closed");
                    throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
                } catch (NoHttpResponseException ex) {
                    logger.error("IngestClientImpl::createNewMediaPackage - IngestClientException(SERVER_ERROR)"
                            + "-- dropped connection without any response. Maybe the server is under heavy load.");
                    throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
                } catch (SocketException ex) {
                    logger.error("IngestClientImpl::createNewMediaPackage - IngestClientException(SERVER_ERROR)");
                    throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
                } catch (IOException ex) {
                    logger.error("IngestClientImpl::createNewMediaPackage - IngestClientException(NETWORK_ERROR)");
                    throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
                } finally {
                    releaseConnections();
                }
            }
        });
        threadPool.execute(task);
        return task;
    }

    private String getFormattedSize(File f) {
        String sizeStr = "";
        // size in bytes
        double fileSize = f.length();
        boolean inKB;
        boolean inMB = false;
        boolean inGB = false;
        if (fileSize > 0.0) {
            // convert to kb
            fileSize = fileSize / 1024;
            inKB = true;
            if (fileSize > 1024.0) {
                // convert to mb
                fileSize = fileSize / 1024;
                inKB = false;
                inMB = true;
                if (fileSize > 1024.0) {
                    // convert to gb
                    fileSize = fileSize / 1024;
                    inKB = false;
                    inMB = false;
                    inGB = true;
                }
            }
            DecimalFormat df = null;
            if (inKB) {
                df = new DecimalFormat("0.00##");
                sizeStr = df.format(fileSize) + " KB";
            } else if (inMB) {
                df = new DecimalFormat("0.00");
                sizeStr = df.format(fileSize) + " MB";
            } else if (inGB) {
                df = new DecimalFormat("0.00##");
                sizeStr = df.format(fileSize) + " GB";
            }
            if (df != null) {
                df.setRoundingMode(RoundingMode.UP);
            }
        }
        return sizeStr;
    }

    private String upload14(RecordingFile file) throws IOException, IngestClientException, URISyntaxException {
        if (logger.isInfoEnabled()) {
            logger.info("IngestClientImpl::upload14 - Upload method: 1.4");
        }

        final String _host = this.getBaseURL();
        final int _port = this.getPort();
        final String _username = this.username;
        final String _password = this.password;
        final int _chunksize = this.chunkSize;

        File pfile = new File(file.getPath());
        String filename = pfile.getName();
        Recording recording = file.getRecording();
        recording.setIngestStatus(Constants.getInstance().getLocalizedString("InitializingUploadOf") + ": '" + filename + "'");
        recording.setIngestDetails(Constants.getInstance().getLocalizedString("Size") + ": " + getFormattedSize(pfile));
        // initialize upload job
        UploadJob job = file.getUploadJob();
        job.setChunkSize(_chunksize);
        job.setJobId(UUID.randomUUID().toString());
        job.setState(UploadJob.State.INPROGRESS);

        // upload chunks
        recording.setIngestStatus(Constants.getInstance().getLocalizedString("Uploading") + ": '" + filename + "'");

        uploadableFile = new UploadableFileImpl(
                file,
                _chunksize);
        uploadableFile.setPostParameters(
                postparam_uploadNextChunk_chunknumber,
                postparam_uploadNextChunk_jobId,
                postparam_uploadNextChunk_fileData,
                postparam_newJob_fileName,
                postparam_newJob_fileSize,
                postparam_newJob_chunkSize,
                postparam_newJob_flavor,
                postparam_newJob_mediaPackage);
        uploadableFile.setup(
                _host,
                _port,
                _username,
                _password,
                IngestClientImpl.ingest_newJob_URL,
                IngestClientImpl.ingest_job_URL);
        uploadableFile.setCurrentChunk((int) job.getCurrentChunk());
        job.setTotalChunks(uploadableFile.getNumberOfChunks());
        if (logger.isInfoEnabled()) {
            logger.info("IngestClientImpl::upload14 - Starting upload at chunk " + ((int) uploadableFile.getNumberOfChunksUploaded()));
        }
        while (!uploadableFile.isFullyUploaded() && job.getState().equals(UploadJob.State.INPROGRESS)) {
            if (!uploadableFile.isUploading()) {
                recording.setIngestDetails(Constants.getInstance().getLocalizedString("Size") + ": " + getFormattedSize(pfile) + ", " + Constants.getInstance().getLocalizedString("Chunk") + ": " + (uploadableFile.getNumberOfChunksUploaded() + 1) + "/" + (uploadableFile.getNumberOfChunks()));
                if (logger.isInfoEnabled()) {
                    logger.info("IngestClientImpl::upload14 - Uploading chunk " + (uploadableFile.getNumberOfChunksUploaded()));
                }
                try {
                    uploadableFile.uploadNextChunk();
                } catch (IOException ex) {
                    logger.error("IngestClientImpl::upload14 - Uploading chunk: IOException: " + ex.getMessage());
                    throw ex;
                } catch (IngestClientException ex) {
                    logger.error("IngestClientImpl::upload14 - Uploading chunk: IngestClientException: " + ex.getMessage());
                    throw ex;
                } catch (URISyntaxException ex) {
                    logger.error("IngestClientImpl::upload14 - Uploading chunk: URISyntaxException: " + ex.getMessage());
                    throw ex;
                }
                job.setCurrentChunk(uploadableFile.getNumberOfChunksUploaded());
            } else if (!uploadableFile.isFullyUploaded()) {
                try {
                    Thread.sleep(Integer.parseInt(config.get(Constants.PROPKEY_UPLOADNEXTCHUNKWAITMS)));
                } catch (InterruptedException ex) {
                    logger.error("IngestClientImpl::upload14 - IngestClientException(GENERAL) 1");
                    throw new IngestClientException(ex.getMessage(), IngestClientException.Type.GENERAL);
                }
            }
        }
        // wait for upload job finalizing
        recording.setIngestStatus(Constants.getInstance().getLocalizedString("AddingToMediaCollection") + ": '" + filename + "'");
        job.setState(UploadJob.State.FINALIZING);
        while (!uploadableFile.getState().equals(UploadableFileState.State.COMPLETE.toString())) {
            try {
                Thread.sleep(Integer.parseInt(config.get(Constants.PROPKEY_UPLOADFINALIZINGWAITMS)));
            } catch (InterruptedException ex) {
                logger.error("IngestClientImpl::upload14 - IngestClientException(GENERAL) 2");
                throw new IngestClientException(ex.getMessage(), IngestClientException.Type.GENERAL);
            }
        }

        uploadableFile.stopUpload();

        // POST /addTrack
        // mediapackage: the mediapackage to change
        // trackUri: the URI to the new track
        // flavor: the flavor of the track
        // Return value description: Returns the new Mediapackage
        httpClient = new DefaultHttpClient();
        client_context = initHttpClient(httpClient);

        URI uri = new URI("http", null, getBaseURL(), getPort(), IngestClientImpl.mediapackage_addTrack_URL, null, null);
        if (logger.isInfoEnabled()) {
            logger.info("IngestClientImpl::upload14 - About to send POST request: " + uri.toURL().toString());
        }
        httppost = new HttpPost(uri.toURL().toString());

        StringBody mediapackage = new StringBody(recording.getMediaPackage(), Charset.forName("UTF-8"));
        StringBody trackUri = new StringBody(uploadableFile.getTrackURL(), Charset.forName("UTF-8"));
        StringBody flavor = new StringBody(file.getFlavor(), Charset.forName("UTF-8"));

        MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        reqEntity.addPart(postparam_addTrack_mediaPackage, mediapackage);
        reqEntity.addPart(postparam_addTrack_trackUri, trackUri);
        reqEntity.addPart(postparam_addTrack_flavor, flavor);

        httppost.setEntity(reqEntity);

        HttpResponse response = httpClient.execute(httppost, client_context);

        String mediaPackageXML_new = recording.getMediaPackage();
        if (logger.isInfoEnabled()) {
            logger.info("IngestClientImpl::upload14 - Status code: " + response.getStatusLine().getStatusCode());
        }
        if (response.getStatusLine().getStatusCode() == 200) {
            HttpEntity resEntity = response.getEntity();
            int r = resEntity.getContent().read();
            if (r != -1) {
                InputStream stream = resEntity.getContent();
                mediaPackageXML_new = IOUtils.toString(stream);

                // small hack for correct parsing
                if (!mediaPackageXML_new.startsWith("<")) {
                    mediaPackageXML_new = "<" + mediaPackageXML_new;
                }
                IOUtils.closeQuietly(stream);
            }
            EntityUtils.consume(resEntity);
        } else {
            logger.error("IngestClientImpl::upload14 - IngestClientException(SERVER_ERROR), Status Code: " + response.getStatusLine().getStatusCode());
            releaseConnections();
            throw new IngestClientException("500 Internal Server Error", IngestClientException.Type.SERVER_ERROR);
        }

        releaseConnections();

        recording.setIngestStatus(Constants.getInstance().getLocalizedString("Uploading") + ": '" + filename + "' - " + Constants.getInstance().getLocalizedString("Done"));
        job.setState(UploadJob.State.COMPLETE);

        // return resulting mediaPackage
        return mediaPackageXML_new;
    }

    private String upload13(RecordingFile file) throws IOException, IngestClientException, URISyntaxException {
        if (logger.isInfoEnabled()) {
            logger.info("IngestClientImpl::upload13 - Upload method: 1.3");
        }

        File pfile = new File(file.getPath());
        String filename = pfile.getName();
        Recording recording = file.getRecording();
        recording.setIngestStatus(Constants.getInstance().getLocalizedString("InitializingUploadOf") + ": '" + filename + "'");
        recording.setIngestDetails(Constants.getInstance().getLocalizedString("Size") + ": " + getFormattedSize(pfile));

        uploadableFile13 = new UploadableFile13Impl(file);
        recording.setIngestStatus(Constants.getInstance().getLocalizedString("Uploading") + ": '" + filename + "'");

        uploadableFile13.setPostParameters(
                postparam_ingest_flavor_13,
                postparam_ingest_mediaPackage_13,
                postparam_ingest_file_13);
        uploadableFile13.setup(
                this.getBaseURL(),
                this.getPort(),
                username,
                password,
                ingest_addTrack_URL_13);

        String mediaPackageXML_new = recording.getMediaPackage();

        UploadJob job = file.getUploadJob();

        if (!uploadableFile13.isUploading() && !job.getState().equals(UploadJob.State.INPROGRESS)) {
            try {
                mediaPackageXML_new = uploadableFile13.upload();
            } catch (FileNotFoundException ex1) {
                logger.error("IngestClientImpl::upload13 - FileNotFoundException");
                throw new IngestClientException(ex1.getMessage(), IngestClientException.Type.GENERAL);
            } catch (IOException ex1) {
                logger.error("IngestClientImpl::upload13 - FileNotFoundException");
                throw new IngestClientException(ex1.getMessage(), IngestClientException.Type.GENERAL);
            }
        }

        uploadableFile13.stopUpload();

        recording.setIngestStatus(Constants.getInstance().getLocalizedString("Uploading") + ": '" + filename + "'");
        job.setState(UploadJob.State.COMPLETE);

        return mediaPackageXML_new;
    }

    @Override
    public FutureTask addTrack(final RecordingFile file) {
        FutureTask<String> task = new FutureTask<>(new Callable() {
            @Override
            public Object call() throws IngestClientException {
                try {
                    File pfile = new File(file.getPath());
                    String filename = pfile.getName();
                    Recording recording = file.getRecording();
                    recording.setIngestStatus(Constants.getInstance().getLocalizedString("InitializingUploadOf") + ": '" + filename + "'");
                    recording.setIngestDetails(Constants.getInstance().getLocalizedString("Size") + ": " + getFormattedSize(pfile));

                    // upload chunks
                    recording.setIngestStatus(Constants.getInstance().getLocalizedString("Uploading") + ": '" + filename + "'");

                    boolean chunkedUpload = Boolean.parseBoolean(config.get(Constants.PROPKEY_CHUNKED_UPLOAD)) || false;

                    if (chunkedUpload) {
                        try {
                            return upload14(file);
                        } catch (IngestClientException ex) {
                            try {
                                // Try MH 1.3 addTrack
                                boolean chunkedUploadFallback = Boolean.parseBoolean(config.get(Constants.PROPKEY_CHUNKED_UPLOAD_FALLBACK));
                                if (chunkedUploadFallback) {
                                    logger.error("IngestClientImpl::addTrack - IngestClientException (NETWORK_ERROR) -- trying MH 1.3 method");
                                    return upload13(file);
                                } else {
                                    logger.error("IngestClientImpl::addTrack - IngestClientException (NETWORK_ERROR)");
                                    throw new IngestClientException(ex.getMessage(), IngestClientException.Type.GENERAL);
                                }
                            } catch (IOException ex1) {
                                logger.error("IngestClientImpl::addTrack - IOException: " + ex1.getMessage());
                                throw new IngestClientException(ex1.getMessage(), IngestClientException.Type.GENERAL);
                            }
                        } catch (ConnectionClosedException ex) {
                            logger.error("IngestClientImpl::addTrack - IngestClientException(NETWORK_ERROR)"
                                    + "-- connection closed");
                            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
                        } catch (NoHttpResponseException ex) {
                            logger.error("IngestClientImpl::addTrack - IngestClientException(SERVER_ERROR)"
                                    + "-- dropped connection without any response. Maybe the server is under heavy load.");
                            return "";
                            // throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
                        } catch (SocketException ex) {
                            logger.error("IngestClientImpl::addTrack - IngestClientException(SERVER_ERROR)");
                            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
                        } catch (IOException ex) {
                            logger.error("IngestClientImpl::addTrack - IngestClientException(NETWORK_ERROR)");
                            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
                        } finally {
                            releaseConnections();
                        }
                    } else {
                        try {
                            return upload13(file);
                        } catch (IOException ex1) {
                            logger.error("IngestClientImpl::addTrack - IOException: " + ex1.getMessage());
                            throw new IngestClientException(ex1.getMessage(), IngestClientException.Type.GENERAL);
                        }
                    }
                } catch (URISyntaxException ex) {
                    logger.error("IngestClientImpl::addTrack - IngestClientException(CLIENT_ERROR)");
                    throw new IngestClientException(ex.getMessage(), IngestClientException.Type.CLIENT_ERROR);
                }
            }
        });
        threadPool.execute(task);
        return task;
    }

    private String addCatalog_helper(final RecordingFile file, String ingest_addCatalogURL) throws IngestClientException {
        try {
            File pfile = new File(file.getPath());
            String filename = pfile.getName();
            Recording recording = file.getRecording();
            recording.setIngestStatus(Constants.getInstance().getLocalizedString("InitializingUploadOf") + ": '" + filename + "'");
            recording.setIngestDetails(Constants.getInstance().getLocalizedString("Size") + ": " + getFormattedSize(pfile));
            // initialize upload job
            UploadJob job = file.getUploadJob();
            job.setChunkSize(1000);
            job.setTotalChunks(10);
            job.setJobId(UUID.randomUUID().toString());
            job.setState(UploadJob.State.INPROGRESS);

            // upload chunks
            recording.setIngestStatus(Constants.getInstance().getLocalizedString("Uploading") + ": '" + filename + "'");

            httpClient = new DefaultHttpClient();
            client_context = initHttpClient(httpClient);
            try {
                job.setState(UploadJob.State.FINALIZING);

                // POST /addDCCatalog
                // mediaPackage: The media package as XML
                // dublinCore: DublinCore catalog as XML
                // flavor(Default value=dublincore/episode): DublinCore Flavor [optional]
                // Returns agmented media package
                URI uri = new URI("http", null, getBaseURL(), getPort(), ingest_addCatalogURL, null, null);
                if (logger.isInfoEnabled()) {
                    logger.info("IngestClientImpl::addCatalog_helper - About to send POST request: " + uri.toURL().toString());
                }
                httppost = new HttpPost(uri.toURL().toString());

                String pfileContent = "";

                FileInputStream fis = null;
                BufferedReader ir = null;
                // read out file
                try {
                    fis = new FileInputStream(pfile);
                    ir = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
                    String s = ir.readLine();
                    while (s != null) {
                        pfileContent += s;
                        s = ir.readLine();
                    }
                } catch (IOException e) {
                    logger.error("IngestClientImpl::addCatalog_helper - IOException (Could not read '" + pfile.getName() + "'): " + e.getMessage());
                    throw new RuntimeException("Could not read '" + pfile.getName() + "'.", e);
                } finally {
                    if (fis != null) {
                        fis.close();
                    }
                    if (ir != null) {
                        ir.close();
                    }
                }

                byte[] utf8 = pfileContent.getBytes(Charset.forName("UTF-8"));
                pfileContent = new String(utf8, Charset.forName("UTF-8"));

                StringBody mediapackage = new StringBody(recording.getMediaPackage(), Charset.forName("UTF-8"));
                StringBody dublinCore = new StringBody(pfileContent, Charset.forName("UTF-8"));
                StringBody flavor = new StringBody(file.getFlavor(), Charset.forName("UTF-8"));

                MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                reqEntity.addPart(postparam_addCatalog_mediaPackage, mediapackage);
                reqEntity.addPart(postparam_addCatalog_dublineCore, dublinCore);
                reqEntity.addPart(postparam_addCatalog_flavor, flavor);

                httppost.setEntity(reqEntity);

                HttpResponse response = httpClient.execute(httppost, client_context);

                String mediaPackageXML_new = recording.getMediaPackage();
                if (logger.isInfoEnabled()) {
                    logger.info("IngestClientImpl::addCatalog_helper - Status code: " + response.getStatusLine().getStatusCode());
                }
                if (response.getStatusLine().getStatusCode() == 200) {
                    HttpEntity resEntity = response.getEntity();
                    int r = resEntity.getContent().read();
                    if (r != -1) {
                        InputStream stream = resEntity.getContent();
                        mediaPackageXML_new = IOUtils.toString(stream);

                        // small hack for correct parsing
                        if (!mediaPackageXML_new.startsWith("<")) {
                            mediaPackageXML_new = "<" + mediaPackageXML_new;
                        }
                        IOUtils.closeQuietly(stream);
                    }
                    EntityUtils.consume(resEntity);
                } else {
                    logger.error("IngestClientImpl::addCatalog_helper - IngestClientException(GENERAL): Status Code: " + response.getStatusLine().getStatusCode());
                    EntityUtils.consume(reqEntity);
                    throw new IngestClientException("Status Code: " + response.getStatusLine().getStatusCode(), IngestClientException.Type.GENERAL);
                }

                recording.setIngestStatus(Constants.getInstance().getLocalizedString("Uploading") + ": '" + filename + "' - " + Constants.getInstance().getLocalizedString("Done"));
                job.setState(UploadJob.State.COMPLETE);

                // return resulting mediaPackage
                return mediaPackageXML_new;
            } catch (ConnectionClosedException ex) {
                logger.error("IngestClientImpl::addCatalog_helper - IngestClientException(NETWORK_ERROR)"
                        + "-- connection closed");
                throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
            } catch (NoHttpResponseException ex) {
                logger.error("IngestClientImpl::addCatalog_helper - IngestClientException(SERVER_ERROR)"
                        + "-- dropped connection without any response. Maybe the server is under heavy load.");
                throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
            } catch (SocketException ex) {
                logger.error("IngestClientImpl::addCatalog_helper - IngestClientException(SERVER_ERROR)");
                throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
            } catch (IOException ex) {
                logger.error("IngestClientImpl::addCatalog_helper - IngestClientException(NETWORK_ERROR)");
                throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
            } finally {
                releaseConnections();
            }
        } catch (URISyntaxException ex) {
            logger.error("IngestClientImpl::addCatalog_helper - IngestClientException(CLIENT_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.CLIENT_ERROR);
        }
    }

    @Override
    public FutureTask addCatalog(final RecordingFile file) {

        FutureTask<String> task = new FutureTask<>(new Callable() {
            @Override
            public Object call() throws IngestClientException {
                String ret = "";
                try {
                    ret = addCatalog_helper(file, IngestClientImpl.ingest_addCatalog_URL);
                } catch (IngestClientException ex) {
                    if (ex.getType() == IngestClientException.Type.GENERAL) {
                        if (logger.isInfoEnabled()) {
                            logger.info("IngestClientImpl::addCatalog - Recognized old version of Matterhorn, trying again.");
                        }
                        try {
                            ret = addCatalog_helper(file, IngestClientImpl.ingest_addCatalog_URL_13);
                        } catch (IngestClientException ex1) {
                            logger.error("IngestClientImpl::addCatalog - IngestClientException:" + ex1.getMessage());
                        }
                    }
                }
                return ret;
            }
        });
        threadPool.execute(task);
        return task;
    }

    @Override
    public FutureTask stopProcessing() {
        FutureTask<String> task = new FutureTask<>(new Callable() {
            @Override
            public Object call() throws IngestClientException {
                if (logger.isInfoEnabled()) {
                    logger.info("IngestClientImpl::stopProcessing - Stopping processing");
                }
                releaseConnections();
                if (cookieStore != null) {
                    cookieStore = null;
                }
                if (httpget != null) {
                    httpget.abort();
                    httpget = null;
                }
                if (httpget != null) {
                    httppost.abort();
                    httppost = null;
                }
                if (httpClient != null) {
                    httpClient = null;
                }
                if (client_context != null) {
                    client_context = null;
                }
                if (uploadableFile13 != null) {
                    uploadableFile13.stopUpload();
                }
                if (uploadableFile != null) {
                    uploadableFile.stopUpload();
                }
                return null;
            }
        });

        threadPool.execute(task);
        return task;
    }

    private String startProcessing_helper(
            String mediaPackageXML,
            String workflowId,
            Map<String, String> workflowParams,
            String ingest_ingest_URL,
            String postparam_startProcessing_mediaPackage) throws IngestClientException, URISyntaxException {
        String ret = "";

        DefaultHttpClient _httpClient = new DefaultHttpClient();
        HttpContext _client_context = initHttpClient(_httpClient);
        HttpPost _httppost = null;
        try {
            String path = ingest_ingest_URL + "/" + workflowId;
            URI uri = new URI("http", null, getBaseURL(), getPort(), path, null, null);
            if (logger.isInfoEnabled()) {
                logger.info("IngestClientImpl::startProcessing_helper - About to send POST request: " + uri.toURL().toString());
            }
            _httppost = new HttpPost(uri.toURL().toString());

            HashMap<String, StringBody> stringBodies = new HashMap<>();
            StringBody mediapackage = new StringBody(mediaPackageXML, Charset.forName("UTF-8"));
            if (!workflowParams.isEmpty()) {
                for (String s : workflowParams.keySet()) {
                    stringBodies.put(s, new StringBody(workflowParams.get(s), Charset.forName("UTF-8")));
                    if (logger.isInfoEnabled()) {
                        logger.info("IngestClientImpl::startProcessing_helper - Additional string body: '" + s + " - " + workflowParams.get(s) + "'");
                    }
                }
            }

            MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            reqEntity.addPart(postparam_startProcessing_mediaPackage, mediapackage);
            for (String s : stringBodies.keySet()) {
                reqEntity.addPart(s, stringBodies.get(s));
            }

            _httppost.setEntity(reqEntity);

            HttpResponse response = _httpClient.execute(_httppost, _client_context);

            if (logger.isInfoEnabled()) {
                logger.info("IngestClientImpl::startProcessing_helper - Status code: " + response.getStatusLine().getStatusCode());
            }
            if (response.getStatusLine().getStatusCode() != 200) {
                logger.error("IngestClientImpl::startProcessing_helper - IngestClientException(GENERAL) - Status Code: " + response.getStatusLine().getStatusCode());
                EntityUtils.consume(reqEntity);
                throw new IngestClientException("Status Code: " + response.getStatusLine().getStatusCode(), IngestClientException.Type.GENERAL);
            }
        } catch (URISyntaxException ex) {
            logger.error("IngestClientImpl::startProcessing_helper - IngestClientException(CLIENT_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.CLIENT_ERROR);
        } catch (ConnectionClosedException ex) {
            logger.error("IngestClientImpl::startProcessing_helper - IngestClientException(NETWORK_ERROR)"
                    + "-- connection closed");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
        } catch (NoHttpResponseException ex) {
            logger.error("IngestClientImpl::startProcessing_helper - IngestClientException(SERVER_ERROR)"
                    + "-- dropped connection without any response. Maybe the server is under heavy load.");
            // throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
        } catch (SocketException ex) {
            logger.error("IngestClientImpl::startProcessing_helper - IngestClientException(SERVER_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
        } catch (IOException ex) {
            logger.error("IngestClientImpl::startProcessing_helper - IngestClientException(NETWORK_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
        } finally {
            if (_httppost != null) {
                _httppost.releaseConnection();
            }
            _httpClient.getConnectionManager().shutdown();
        }
        return ret;
    }

    @Override
    public FutureTask startProcessing(
            String mediaPackageXML,
            String workflowId,
            Map<String, String> workflowParams) {
        final String _workflowId = workflowId;
        final String _mediaPackageXML = mediaPackageXML;
        final Map<String, String> _workflowParams = workflowParams;

        FutureTask<String> task = new FutureTask<>(new Callable() {
            @Override
            public Object call() throws IngestClientException, URISyntaxException {
                String ret = "";
                try {
                    // POST /ingest/ingest/{wdID}, wdID = Workflow definition id
                    // mediaPackage: The media package XML
                    ret = startProcessing_helper(
                            _mediaPackageXML,
                            _workflowId,
                            _workflowParams,
                            IngestClientImpl.ingest_ingest_URL,
                            postparam_startProcessing_mediaPackage);
                } catch (IngestClientException ex) {
                    // POST /ingest/ingest/{wdID}, wdID = Workflow definition id
                    // MEDIAPACKAGE: The media package XML
                    if (ex.getType() == IngestClientException.Type.GENERAL) {
                        if (logger.isInfoEnabled()) {
                            logger.info("IngestClientImpl::startProcessing - Recognized old version of Matterhorn, trying again.");
                        }
                        try {
                            ret = startProcessing_helper(
                                    _mediaPackageXML,
                                    _workflowId,
                                    _workflowParams,
                                    IngestClientImpl.ingest_ingest_URL,
                                    postparam_startProcessing_mediaPackage_13);
                        } catch (IngestClientException ex1) {
                            logger.error("IngestClientImpl::startProcessing - IngestClientException: " + ex1.getMessage());
                            throw ex1;
                        }
                    }
                }
                return ret;
            }
        });

        threadPool.execute(task);
        return task;
    }

    @Override
    public void reload() {
        try {
            threadPool.shutdown();
        } catch (Exception e) {
        }
        readProperties();

        String uName = config.get(Constants.PROPKEY_USERNAME);
        String pWord = config.get(Constants.PROPKEY_PASSWORD);
        setCredentials(uName, pWord);
    }
}
