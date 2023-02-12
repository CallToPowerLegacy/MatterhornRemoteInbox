/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.ingestclient.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
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
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import de.calltopower.mhri.application.api.RecordingFile;
import de.calltopower.mhri.application.impl.RecordingImpl;
import de.calltopower.mhri.ingestclient.api.IngestClientException;
import de.calltopower.mhri.ingestclient.api.UploadableFile;
import de.calltopower.mhri.util.Constants;
import de.calltopower.mhri.util.MHRIFileUtils;
import de.calltopower.mhri.util.ParseUtils;
import de.calltopower.mhri.util.UploadableFileState;

/**
 * UploadableFileImpl - Implements UploadableFile
 *
 * @date 18.05.2012
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public final class UploadableFileImpl implements UploadableFile {

    private static final Logger logger = Logger.getLogger(UploadableFileImpl.class);
    // POST parameters
    private String postparam_uploadNextChunk_chunknumber = "chunknumber";
    private String postparam_uploadNextChunk_jobId = "jobID";
    private String postparam_uploadNextChunk_fileData = "filedata";
    private String postparam_newJob_fileName = "filename";
    private String postparam_newJob_fileSize = "filesize";
    private String postparam_newJob_chunkSize = "chunksize";
    private String postparam_newJob_flavor = "flavor";
    private String postparam_newJob_mediaPackage = "mediapackage";
    // misc 'global'
    private String host = "";
    private int port = 8080;
    private String username = "";
    private String password = "";
    private String newjob_path = "";
    private String job_path = "";
    // misc 'local'
    private String uploadJobID = "";
    private String trackURL = "";
    private String uploadTmpFileName = "";
    private int chunkSize = 1024 * 1024 * 100; // in Byte
    private int numberOfChunks = 1;
    private int numberOfChunksUploaded = 0;
    private long numberOfBytesUploaded = 0;
    private boolean jobIdGenerated = false;
    private boolean jobIDCurrentlyBeingGenerated = false;
    private final int outputFormat = 1; // xml: 1, json: 2 -- json not working right now!
    // misc
    private RecordingFile file = null;
    private File uploadFile = null;
    private final int generateJobIDTries = 50;
    private int currentGenerateJobIDTries = 0;
    private boolean isUploading = false;
    // Apache Commons
    private DefaultHttpClient httpClient = null;
    private HttpContext client_context = null;
    private CookieStore cookieStore = null;
    private HttpPost httppost = null;
    private HttpGet httpget = null;

    public UploadableFileImpl(
            RecordingFile file,
            int chunkSize) throws IOException {
        this.file = file;
        this.uploadFile = new File(this.file.getPath());

        this.uploadTmpFileName = MHRIFileUtils.getInstance().getNewTmpFileName(
                Constants.MHRI_TMP_FILE_DIR_NAME_WO_PATH,
                Constants.MHRI_TMP_FILE_NAME_WO_PATH);
        if (chunkSize > 0) {
            this.chunkSize = chunkSize;
        }
        if (this.uploadFile != null) {
            this.numberOfChunks = MHRIFileUtils.getInstance().calculateNumberOfSplitFiles(this.uploadFile.length(), chunkSize);
        }
    }

    public void setup(
            String host,
            int port,
            String username,
            String password,
            String newJobPath,
            String jobPath) throws IngestClientException {
        this.host = host.startsWith("http://") ? host.substring(7) : host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.newjob_path = newJobPath;
        this.job_path = jobPath;

        try {
            this.uploadJobID = generateNewJobID();
        } catch (IOException ex) {
            logger.error("UploadableFileImpl::setup - IOException: " + ex.getMessage());
        } catch (RuntimeException ex) {
            logger.error("UploadableFileImpl::setup - RuntimeException: " + ex.getMessage());
        } catch (URISyntaxException ex) {
            logger.error("UploadableFileImpl::setup - URISyntaxException: could not generate new job ID: " + ex.getMessage());
        } catch (IngestClientException ex) {
            logger.error("UploadableFileImpl::setup - IngestClientException: could not generate new job ID: " + ex.getMessage());
            throw ex;
        }
    }

    public void setPostParameters(
            String postparam_uploadNextChunk_chunknumber,
            String postparam_uploadNextChunk_jobId,
            String postparam_uploadNextChunk_fileData,
            String postparam_newJob_fileName,
            String postparam_newJob_fileSize,
            String postparam_newJob_chunkSize,
            String postparam_newJob_flavor,
            String postparam_newJob_mediaPackage) {
        this.postparam_uploadNextChunk_chunknumber = postparam_uploadNextChunk_chunknumber;
        this.postparam_uploadNextChunk_jobId = postparam_uploadNextChunk_jobId;
        this.postparam_uploadNextChunk_fileData = postparam_uploadNextChunk_fileData;
        this.postparam_newJob_fileName = postparam_newJob_fileName;
        this.postparam_newJob_fileSize = postparam_newJob_fileSize;
        this.postparam_newJob_chunkSize = postparam_newJob_chunkSize;
        this.postparam_newJob_flavor = postparam_newJob_flavor;
        this.postparam_newJob_mediaPackage = postparam_newJob_mediaPackage;
    }

    private void releaseConnections() {
        if (cookieStore != null) {
            cookieStore.clear();
            cookieStore = null;
        }
        if (httpClient != null) {
            httpClient.getConnectionManager().shutdown();
        }
        if (httppost != null) {
            httppost.releaseConnection();
        }
        if (httpget != null) {
            httpget.releaseConnection();
        }
    }

    private HttpContext setCreds(DefaultHttpClient httpClient) throws IngestClientException, URISyntaxException {
        try {
            this.httpClient = httpClient;
            this.httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
            cookieStore = new BasicCookieStore();
            client_context = new BasicHttpContext();
            client_context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

            // POST /j_spring_security_check
            // j_username: Username
            // j_password: Password
            // Return value description: Cookie
            URI uri = new URI("http", null, this.host, this.port, "/j_spring_security_check", null, null);
            if (logger.isInfoEnabled()) {
                logger.info("UploadableFileImpl::setCreds - About to send POST request: " + uri.toURL().toString());
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
                logger.info("UploadableFileImpl::setCreds - Status code: " + response.getStatusLine().getStatusCode() + ", Location: " + location);
            }
            if ((response.getStatusLine().getStatusCode() == 200)
                    || (response.getStatusLine().getStatusCode() == 302)
                    && !location.endsWith("error")) {
            } else {
                logger.error("UploadableFileImpl::setCreds - Could not authenticate: IngestClientException(CLIENT_ERROR), Status code: " + response.getStatusLine().getStatusCode());
                httppost.releaseConnection();
                throw new IngestClientException("Could not authenticate", IngestClientException.Type.CLIENT_ERROR);
            }
            httppost.releaseConnection();
            return client_context;
        } catch (ConnectionClosedException ex) {
            logger.error("UploadableFileImpl::setCreds - IngestClientException(NETWORK_ERROR)"
                    + "-- connection closed");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
        } catch (NoHttpResponseException ex) {
            logger.error("UploadableFileImpl::setCreds - IngestClientException(SERVER_ERROR)"
                    + "-- dropped connection without any response. Maybe the server is under heavy load.");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
        } catch (SocketException ex) {
            logger.error("UploadableFileImpl::setCreds - IngestClientException(SERVER_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
        } catch (IOException ex) {
            logger.error("UploadableFileImpl::setCreds - IngestClientException(NETWORK_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
        } catch (URISyntaxException ex) {
            logger.error("UploadableFileImpl::setCreds - URISyntaxException");
            throw ex;
        } finally {
            httppost.releaseConnection();
        }
    }

    // getter
    @Override
    public int getChunkSize() {
        return this.chunkSize;
    }

    @Override
    public int getNumberOfChunks() {
        return this.numberOfChunks;
    }

    @Override
    public int getNumberOfChunksUploaded() {
        return this.numberOfChunksUploaded;
    }

    @Override
    public long getNumberOfBytesUploaded() {
        return this.numberOfBytesUploaded;
    }

    @Override
    public long getNumberOfRemainingBytes() {
        long i = -1;
        if (this.file != null) {
            i = this.uploadFile.length() - this.numberOfBytesUploaded;
        }

        return i;
    }

    @Override
    public String getTrackURL() throws URISyntaxException, IngestClientException {
        try {
            getState();
        } catch (URISyntaxException ex) {
            logger.error("UploadableFileImpl::getTrackURL - Caught URISyntaxException: " + ex.getMessage());
            throw ex;
        } catch (IngestClientException ex) {
            logger.error("UploadableFileImpl::getTrackURL - Caught IngestClientException: " + ex.getMessage());
            throw ex;
        }
        return trackURL;
    }

    @Override
    public String getState() throws URISyntaxException, IngestClientException {
        String ret = "";
        if (this.jobIdGenerated && !this.uploadJobID.isEmpty()) {
            // GET /job/{jobID}.{format:xml|json}
            httpClient = new DefaultHttpClient();
            client_context = setCreds(httpClient);
            try {
                String path = this.job_path + "/" + this.uploadJobID + "." + ((this.outputFormat == 1) ? "xml" : "json");
                URI uri = new URI("http", null, this.host, this.port, path, null, null);

                if (logger.isInfoEnabled()) {
                    logger.info("UploadableFileImpl::getState - About to send GET request: " + uri.toURL().toString());
                }
                httpget = new HttpGet(uri.toURL().toString());
                HttpResponse response = httpClient.execute(httpget, client_context);

                if (logger.isInfoEnabled()) {
                    logger.info("UploadableFileImpl::getState - Status code: " + response.getStatusLine().getStatusCode());
                }
                if (response.getStatusLine().getStatusCode() == 200) {
                    HttpEntity entity = response.getEntity();

                    InputStream instream = null;
                    BufferedReader ir = null;
                    if (entity != null) {
                        try {
                            instream = entity.getContent();
                            ir = new BufferedReader(new InputStreamReader(instream, "UTF-8"));
                            String line = "";
                            String resp = "";
                            while (line != null) {
                                resp += line;
                                line = ir.readLine();
                            }
                            this.trackURL = ParseUtils.getInstance().parseTrackURL(resp);
                            ret = ParseUtils.getInstance().parseState(this.outputFormat, resp);
                        } catch (IOException ex) {
                            logger.error("UploadableFileImpl::getState - Caught IOException 1: " + ex.getMessage());
                        } catch (RuntimeException ex) {
                            httpget.abort();
                            logger.error("UploadableFileImpl::getState - Caught RuntimeException 1: " + ex.getMessage());
                        } finally {
                            EntityUtils.consume(entity);
                            if (instream != null) {
                                instream.close();
                            }
                            if (ir != null) {
                                ir.close();
                            }
                        }
                    }
                } else {
                    logger.error("UploadableFileImpl::getState - Could not authenticate: IngestClientException(NETWORK_ERROR), Status code: " + response.getStatusLine().getStatusCode());
                    throw new IngestClientException("Something went wrong with the connection. Status code: " + response.getStatusLine().getStatusCode(), IngestClientException.Type.NETWORK_ERROR);
                }
            } catch (URISyntaxException ex) {
                logger.error("UploadableFileImpl::getState - Caught URISyntaxException: " + ex.getMessage());
                throw ex;
            } catch (IllegalArgumentException ex) {
                logger.error("UploadableFileImpl::getState - Caught IllegalArgumentException 1: " + ex.getMessage());
                throw ex;
            } catch (ConnectionClosedException ex) {
                logger.error("IngestClientImpl::getState - IngestClientException(NETWORK_ERROR)"
                        + "-- connection closed");
                throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
            } catch (NoHttpResponseException ex) {
                logger.error("UploadableFileImpl::getState - IngestClientException(SERVER_ERROR)"
                        + "-- dropped connection without any response. Maybe the server is under heavy load.");
                return UploadableFileState.State.FINALIZING.toString();
            } catch (SocketException ex) {
                logger.error("UploadableFileImpl::getState - IngestClientException(SERVER_ERROR)");
                throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
            } catch (IOException ex) {
                logger.error("UploadableFileImpl::getState - IngestClientException(NETWORK_ERROR)");
                throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
            } finally {
                releaseConnections();
            }
        } else {
            if (currentGenerateJobIDTries < generateJobIDTries) {
                try {
                    ++currentGenerateJobIDTries;
                    generateNewJobID();
                    return getState();
                } catch (IOException ex) {
                    logger.error("UploadableFileImpl::getState - Caught IOException 3: " + ex.getMessage());
                } catch (RuntimeException ex) {
                    logger.error("UploadableFileImpl::getState - Caught RuntimeException 2: " + ex.getMessage());
                }
            }
        }

        return ret;
    }

    // setter
    @Override
    public void setCurrentChunk(int newCurrentChunk) {
        if ((newCurrentChunk > 0) && (newCurrentChunk <= getNumberOfChunks())) {
            this.numberOfChunksUploaded = newCurrentChunk - 1;
            this.numberOfBytesUploaded = this.chunkSize * (newCurrentChunk - 1);
        }
    }

    // misc
    @Override
    public boolean isUploading() {
        /*
         boolean b = false;
         try {
         String state = getState();
         b = state.equals(UploadableFileState.State.INPROGRESS.toString());
         return b;
         } catch (URISyntaxException ex) {
         logger.error("UploadableFileImpl::isUploading - URISyntaxException: " + ex.getMessage());
         }
         return b;
         */
        return isUploading;
    }

    @Override
    public boolean isFullyUploaded() {
        boolean b = this.numberOfChunks == this.numberOfChunksUploaded;

        return b;
    }

    @Override
    public String generateNewJobID() throws IOException, URISyntaxException, RuntimeException, IngestClientException {
        String ret = this.uploadJobID;
        if (!this.jobIdGenerated && !jobIDCurrentlyBeingGenerated) {
            jobIDCurrentlyBeingGenerated = true;
            if ((this.file != null) && this.uploadFile.isFile()) {
                httpClient = new DefaultHttpClient();
                client_context = setCreds(httpClient);
                try {
                    // POST /newjob
                    // filename: The name of the file that will be uploaded
                    // filesize: The size of the file that will be uploaded
                    // chunksize: The size of the chunks that will be uploaded
                    // flavor: The flavor of this track
                    // mediapackage: The mediapackage the file should belong to
                    URI uri = new URI("http", null, this.host, this.port, this.newjob_path, null, null);
                    if (logger.isInfoEnabled()) {
                        logger.info("UploadableFileImpl::generateNewJobID - About to send POST request: " + uri.toURL().toString());
                    }
                    httppost = new HttpPost(uri.toURL().toString());

                    StringBody filename = new StringBody(this.uploadFile.getName().replaceAll("[?&=]", "_"), Charset.forName("UTF-8"));
                    StringBody fileSize = new StringBody(String.valueOf(this.uploadFile.length()));
                    StringBody chunksize = new StringBody(String.valueOf(this.chunkSize));
                    StringBody flav = new StringBody(this.file.getFlavor(), Charset.forName("UTF-8"));
                    StringBody mediapackage = new StringBody(this.file.getRecording().getMediaPackage(), Charset.forName("UTF-8"));

                    MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                    reqEntity.addPart(postparam_newJob_fileName, filename);
                    reqEntity.addPart(postparam_newJob_fileSize, fileSize);
                    reqEntity.addPart(postparam_newJob_chunkSize, chunksize);
                    reqEntity.addPart(postparam_newJob_flavor, flav);
                    reqEntity.addPart(postparam_newJob_mediaPackage, mediapackage);

                    httppost.setEntity(reqEntity);

                    HttpResponse response = httpClient.execute(httppost, client_context);
                    HttpEntity resEntity = response.getEntity();
                    if (logger.isInfoEnabled()) {
                        logger.info("UploadableFileImpl::generateNewJobID - Status code: " + response.getStatusLine().getStatusCode());
                    }
                    if ((resEntity != null) && response.getStatusLine().getStatusCode() == 200) {
                        InputStream instream = null;
                        BufferedReader ir = null;
                        try {
                            instream = resEntity.getContent();
                            ir = new BufferedReader(new InputStreamReader(instream, "UTF-8"));
                            String line = "";
                            String resp = "";
                            while (line != null) {
                                resp += line;
                                line = ir.readLine();
                            }
                            ret = resp;
                            this.jobIdGenerated = true;
                            jobIDCurrentlyBeingGenerated = false;
                        } catch (IOException ex) {
                            logger.error("UploadableFileImpl::generateNewJobID - Caught IOException generateNewJobID 1: " + ex.getMessage());
                            throw ex;
                        } catch (RuntimeException ex) {
                            httppost.abort();
                            logger.error("UploadableFileImpl::generateNewJobID - Caught RuntimeException generateNewJobID 1: " + ex.getMessage());
                            throw ex;
                        } finally {
                            EntityUtils.consume(resEntity);
                            EntityUtils.consume(reqEntity);
                            if (instream != null) {
                                instream.close();
                            }
                            if (ir != null) {
                                ir.close();
                            }
                        }
                    } else {
                        logger.error("IngestClientImpl::generateNewJobID - IngestClientException(NETWORK_ERROR), Status code: " + response.getStatusLine().getStatusCode());
                        throw new IngestClientException("Something went wrong with the connection. Status code: " + response.getStatusLine().getStatusCode(), IngestClientException.Type.NETWORK_ERROR);
                    }
                } catch (URISyntaxException ex) {
                    logger.error("UploadableFileImpl::generateNewJobID - Caught URISyntaxException: " + ex.getMessage());
                    throw ex;
                } catch (IllegalArgumentException ex) {
                    logger.error("UploadableFileImpl::generateNewJobID - Caught IllegalArgumentException 1: " + ex.getMessage());
                    throw ex;
                } catch (ConnectionClosedException ex) {
                    logger.error("IngestClientImpl::generateNewJobID - IngestClientException(NETWORK_ERROR)"
                            + "-- connection closed");
                    throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
                } catch (NoHttpResponseException ex) {
                    logger.error("UploadableFileImpl::generateNewJobID - IngestClientException(SERVER_ERROR)"
                            + "-- dropped connection without any response. Maybe the server is under heavy load.");
                    throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
                } catch (SocketException ex) {
                    logger.error("UploadableFileImpl::generateNewJobID - IngestClientException(SERVER_ERROR)");
                    throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
                } catch (IOException ex) {
                    logger.error("UploadableFileImpl::generateNewJobID - IngestClientException(NETWORK_ERROR)");
                    throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
                } finally {
                    releaseConnections();
                }
            }
        }
        // if an error occurred reset return value
        if (ret.contains("DOCTYPE")) {
            this.jobIdGenerated = false;
            ret = "";
        }

        return ret;
    }

    @Override
    public void stopUpload() {
        if (logger.isInfoEnabled()) {
            logger.info("UploadableFileImpl::stopUpload: Stopping upload");
        }
        releaseConnections();
        if (cookieStore != null) {
            cookieStore = null;
        }
        if (httpget != null) {
            httpget.abort();
            httpget = null;
        }
        if (httppost != null) {
            httppost.abort();
            httppost = null;
        }
        if (httpClient != null) {
            httpClient = null;
        }
        if (client_context != null) {
            client_context = null;
        }
    }

    @Override
    public void uploadNextChunk() throws FileNotFoundException, IOException, URISyntaxException, IngestClientException {
        if ((this.file != null) && this.uploadFile.isFile()
                && !isFullyUploaded()
                && !isUploading()
                && !this.host.isEmpty()
                && !this.job_path.isEmpty()
                && !this.uploadJobID.isEmpty()) {
            isUploading = true;
            String tmpFileName = this.uploadTmpFileName + "_" + getNumberOfChunksUploaded() + Constants.MHRI_SPLIT_FILE_SUFFIX;
            File f_toUpload = new File(tmpFileName);
            File parentDir = new File(f_toUpload.getParent());
            if (parentDir.canWrite()) {
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(f_toUpload);
                    int numberOfChunksUploaded_tmp = getNumberOfChunksUploaded();
                    long numberOfRemainingBytes_tmp = getNumberOfRemainingBytes();
                    long numberOfBytedUploaded_tmp = getNumberOfBytesUploaded();

                    double chunkProgReal = (double) ((double) ((RecordingImpl) this.file.getRecording()).getCurrentChunkProgSize() / (double) this.getNumberOfChunks());
                    int chunkProg = (int) Math.floor(chunkProgReal);
                    if (chunkProg == 0) {
                        int t = (int) Math.floor((1.0 / chunkProgReal));
                        if (numberOfChunksUploaded_tmp % t == 0) {
                            chunkProg = 1;
                        }
                    }
                    this.file.getRecording().setUploadProgress(this.file.getRecording().getUploadProgress() + chunkProg);

                    byte[] data;
                    if (numberOfRemainingBytes_tmp >= this.chunkSize) {
                        data = new byte[this.chunkSize];
                        ++this.numberOfChunksUploaded;
                        this.numberOfBytesUploaded += this.chunkSize;
                    } else {
                        data = new byte[(int) numberOfRemainingBytes_tmp];
                        this.numberOfChunksUploaded = this.numberOfChunks;
                        this.numberOfBytesUploaded = this.uploadFile.length();
                    }
                    FileInputStream fis = null;
                    int fis_read;
                    try {
                        fis = new FileInputStream(this.uploadFile);
                        fis.skip(numberOfBytedUploaded_tmp);
                        fis_read = fis.read(data);
                        if (fis_read != -1) {
                            fos.write(data);
                        }
                    } catch (IOException ex) {
                        logger.error("UploadableFileImpl::uploadNextChunk - Caught IOException uploadNextChunk 1: " + ex.getMessage());
                        throw ex;
                    } finally {
                        if (fis != null) {
                            fis.close();
                        }
                    }

                    // POST /job/{jobID}
                    // chunknumber: The number of the current chunk
                    // filedata: The payload
                    httpClient = new DefaultHttpClient();
                    client_context = setCreds(httpClient);
                    if (client_context == null) {
                        throw new IngestClientException("No client context", IngestClientException.Type.NETWORK_ERROR);
                    }
                    MultipartEntity reqEntity = null;
                    try {
                        URI uri = new URI("http", null, this.host, this.port, this.job_path + "/" + this.uploadJobID, null, null);

                        httppost = new HttpPost(uri.toURL().toString());

                        FileBody bin = new FileBody(f_toUpload);
                        StringBody chunkNumber = new StringBody(String.valueOf(numberOfChunksUploaded_tmp));
                        StringBody jobId = new StringBody(this.uploadJobID, Charset.forName("UTF-8"));

                        if (logger.isInfoEnabled()) {
                            logger.info("UploadableFileImpl::uploadNextChunk - About to send POST request: " + uri.toURL().toString());
                        }

                        reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                        reqEntity.addPart(postparam_uploadNextChunk_chunknumber, chunkNumber);
                        reqEntity.addPart(postparam_uploadNextChunk_jobId, jobId);
                        reqEntity.addPart(postparam_uploadNextChunk_fileData, bin);

                        httppost.setEntity(reqEntity);

                        HttpResponse response = httpClient.execute(httppost, client_context);
                        HttpEntity resEntity = response.getEntity();

                        if (logger.isInfoEnabled()) {
                            logger.info("UploadableFileImpl::uploadNextChunk - Status code: " + response.getStatusLine().getStatusCode());
                        }
                        EntityUtils.consume(resEntity);

                        if (response.getStatusLine().getStatusCode() != 200) {
                            logger.error("UploadableFileImpl::uploadNextChunk - Status Code: " + response.getStatusLine().getStatusCode());
                            throw new IngestClientException("Status Code: " + response.getStatusLine().getStatusCode(), IngestClientException.Type.SERVER_ERROR);
                        }
                    } catch (URISyntaxException ex) {
                        logger.error("UploadableFileImpl::uploadNextChunk - Caught URISyntaxException: " + ex.getMessage());
                        throw ex;
                    } catch (ConnectionClosedException ex) {
                        logger.error("IngestClientImpl::uploadNextChunk - IngestClientException(NETWORK_ERROR)"
                                + "-- connection closed");
                        throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
                    } catch (NoHttpResponseException ex) {
                        logger.error("UploadableFileImpl::uploadNextChunk - IngestClientException(SERVER_ERROR)"
                                + "-- dropped connection without any response. Maybe the server is under heavy load.");
                        // throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
                    } catch (SocketException ex) {
                        logger.error("UploadableFileImpl::uploadNextChunk - IngestClientException(SERVER_ERROR)");
                        throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
                    } catch (IOException ex) {
                        logger.error("UploadableFileImpl::uploadNextChunk - IngestClientException(NETWORK_ERROR)");
                        throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
                    } finally {
                        if (logger.isInfoEnabled()) {
                            logger.info("UploadableFileImpl::uploadNextChunk - Deleting tmp split file '" + tmpFileName + "'");
                        }
                        try {
                            fos.close();
                        } catch (Exception ex) {
                            logger.error("UploadableFileImpl::uploadNextChunk - Could not close tmp split file output stream");
                        }
                        try {
                            f_toUpload.delete();
                        } catch (Exception ex) {
                            logger.error("UploadableFileImpl::uploadNextChunk - Could not delete tmp split file '" + tmpFileName + "'");
                        }
                        if (reqEntity != null) {
                            EntityUtils.consume(reqEntity);
                        }
                        releaseConnections();
                        isUploading = false;
                    }
                } catch (IOException ex) {
                    logger.error("UploadableFileImpl::uploadNextChunk - IOException: " + ex.getMessage());
                } finally {
                    if (fos != null) {
                        fos.close();
                    }
                    try {
                        f_toUpload.delete();
                    } catch (Exception ex) {
                    }
                }
            } else {
                logger.error("UploadableFileImpl::uploadNextChunk - Cannot write to directory: " + f_toUpload.getParent().toString());
                throw new IOException("Cannot write to directory: " + f_toUpload.getParent().toString());
            }
        }
    }
}
