/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.ingestclient.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
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
import de.calltopower.mhri.ingestclient.api.IngestClientException;
import de.calltopower.mhri.ingestclient.api.UploadableFile13;

/**
 * UploadableFile13Impl - Implements UploadableFile for MH 1.3
 *
 * @date 24.09.2012
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public final class UploadableFile13Impl implements UploadableFile13 {

    private static final Logger logger = Logger.getLogger(UploadableFile13Impl.class);
    // URLs
    private String addTrackURL = "/ingest/addTrack";
    // POST parameters
    private String postparam_ingest_flavor = "flavor";
    private String postparam_ingest_mediapackage = "mediapackage";
    private String postparam_ingest_file = "file";
    // misc 'global'
    private String host = "";
    private int port = 8080;
    private String username = "";
    private String password = "";
    // misc 'local'
    private boolean isUploading = false;
    // misc
    private RecordingFile file = null;
    private File uploadFile = null;
    // Apache Commons
    private DefaultHttpClient httpClient = null;
    private HttpContext client_context = null;
    private CookieStore cookieStore = null;
    private HttpPost httppost = null;

    public UploadableFile13Impl(
            RecordingFile file) throws IOException {
        this.file = file;
        this.uploadFile = new File(this.file.getPath());
    }

    public void setup(
            String host,
            int port,
            String username,
            String password,
            String addTrackURL) throws IngestClientException {
        this.host = host.startsWith("http://") ? host.substring(7) : host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.addTrackURL = addTrackURL;
    }

    public void setPostParameters(
            String postparam_ingest_flavor,
            String postparam_ingest_mediapackage,
            String postparam_ingest_file) {
        this.postparam_ingest_flavor = postparam_ingest_flavor;
        this.postparam_ingest_mediapackage = postparam_ingest_mediapackage;
        this.postparam_ingest_file = postparam_ingest_file;
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
                logger.info("UploadableFile13Impl::setCreds - About to send POST request: " + uri.toURL().toString());
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
                logger.info("UploadableFile13Impl::setCreds - Status code: " + response.getStatusLine().getStatusCode() + ", Location: " + location);
            }
            if ((response.getStatusLine().getStatusCode() == 200)
                    || (response.getStatusLine().getStatusCode() == 302)
                    && !location.endsWith("error")) {
            } else {
                logger.error("UploadableFile13Impl::setCreds - Could not authenticate: IngestClientException(CLIENT_ERROR), Status Code: " + response.getStatusLine().getStatusCode());
                httppost.releaseConnection();
                throw new IngestClientException("Could not authenticate", IngestClientException.Type.CLIENT_ERROR);
            }
            httppost.releaseConnection();
            return client_context;
        } catch (ConnectionClosedException ex) {
            logger.error("UploadableFile13Impl::setCreds - IngestClientException(NETWORK_ERROR)"
                    + "-- connection closed");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
        } catch (NoHttpResponseException ex) {
            logger.error("UploadableFile13Impl::setCreds - IngestClientException(SERVER_ERROR)"
                    + "-- dropped connection without any response. Maybe the server is under heavy load.");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
        } catch (SocketException ex) {
            logger.error("UploadableFile13Impl::setCreds - IngestClientException(SERVER_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
        } catch (IOException ex) {
            logger.error("UploadableFile13Impl::setCreds - IngestClientException(NETWORK_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
        } catch (URISyntaxException ex) {
            logger.error("UploadableFile13Impl::setCreds - URISyntaxException");
            throw ex;
        } finally {
            httppost.releaseConnection();
        }
    }

    // getter
    @Override
    public String getState() throws URISyntaxException, IngestClientException {
        String ret = "";

        return ret;
    }

    // misc
    @Override
    public boolean isUploading() throws IngestClientException {
        return isUploading;
    }

    @Override
    public void stopUpload() {
        if (logger.isInfoEnabled()) {
            logger.info("UploadableFile13Impl::stopUpload: Stopping upload");
        }
        releaseConnections();
        if (cookieStore != null) {
            cookieStore = null;
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
    public String upload() throws FileNotFoundException, IOException, URISyntaxException, IngestClientException {
        String mediaPackageXML_new = "";
        if ((this.file != null) && this.uploadFile.isFile()
                && !isUploading()
                && !this.host.isEmpty()) {
            isUploading = true;
            mediaPackageXML_new = this.file.getRecording().getMediaPackage();

            this.file.getRecording().setUploadProgress(this.file.getRecording().getUploadProgress() + 10);

            // POST /addTrack
            // flavor: The kind of media track 
            // mediaPackage: The media package as XML
            // file: The media track file
            httpClient = new DefaultHttpClient();
            client_context = setCreds(httpClient);
            try {
                URI uri = new URI("http", null, this.host, this.port, this.addTrackURL, null, null);

                httppost = new HttpPost(uri.toURL().toString());

                FileBody bin = new FileBody(this.uploadFile);
                StringBody flavor = new StringBody(this.file.getFlavor(), Charset.forName("UTF-8"));
                StringBody mediapackage = new StringBody(this.file.getRecording().getMediaPackage(), Charset.forName("UTF-8"));

                if (logger.isInfoEnabled()) {
                    logger.info("UploadableFile13Impl::upload - About to send POST request: " + uri.toURL().toString());
                }

                MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                reqEntity.addPart(postparam_ingest_flavor, flavor);
                reqEntity.addPart(postparam_ingest_mediapackage, mediapackage);
                reqEntity.addPart(postparam_ingest_file, bin);

                httppost.setEntity(reqEntity);

                HttpResponse response = httpClient.execute(httppost, client_context);

                if (logger.isInfoEnabled()) {
                    logger.info("UploadableFile13Impl::upload - Status code: " + response.getStatusLine().getStatusCode());
                }
                if (response.getStatusLine().getStatusCode() == 200) {
                    HttpEntity resEntity = response.getEntity();
                    int r = resEntity.getContent().read();
                    InputStream stream = null;
                    if (r != -1) {
                        try {
                            stream = resEntity.getContent();
                            mediaPackageXML_new = IOUtils.toString(stream);

                            // small hack for correct parsing
                            if (!mediaPackageXML_new.startsWith("<")) {
                                mediaPackageXML_new = "<" + mediaPackageXML_new;
                            }
                        } catch (IOException ex) {
                            logger.error("UploadableFile13Impl::upload - IOException: " + ex.getMessage());
                        } finally {
                            if (stream != null) {
                                IOUtils.closeQuietly(stream);
                            }
                        }
                    }
                    EntityUtils.consume(resEntity);
                } else {
                    logger.error("UploadableFile13Impl::upload - IngestClientException(SERVER_ERROR), Status Code: " + response.getStatusLine().getStatusCode());
                    throw new IngestClientException("500 Internal Server Error", IngestClientException.Type.SERVER_ERROR);
                }
                return mediaPackageXML_new;
            } catch (URISyntaxException ex) {
                logger.error("UploadableFile13Impl::upload - Caught URISyntaxException: " + ex.getMessage());
                throw ex;
            } catch (ConnectionClosedException ex) {
                logger.error("UploadableFile13Impl::upload - IngestClientException(NETWORK_ERROR)"
                        + "-- connection closed");
                throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
            } catch (NoHttpResponseException ex) {
                logger.error("UploadableFile13Impl::upload - IngestClientException(SERVER_ERROR)"
                        + "-- dropped connection without any response. Maybe the server is under heavy load.");
                throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
            } catch (SocketException ex) {
                logger.error("UploadableFile13Impl::upload - IngestClientException(SERVER_ERROR)");
                throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
            } catch (IOException ex) {
                logger.error("UploadableFile13Impl::upload - IngestClientException(NETWORK_ERROR)");
                throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
            } finally {
                isUploading = false;
                releaseConnections();
            }
        }
        return mediaPackageXML_new;
    }
}
