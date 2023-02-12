/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.updater;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import de.calltopower.mhri.util.Constants;
import de.calltopower.mhri.util.conf.Configuration;

/**
 * Updater - Checks for version updates
 *
 * @date 28.06.2013
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class Updater {

    private static final Logger logger = Logger.getLogger(Updater.class);
    // REST endpoints URLs
    private String version_URL = "YOUR.SERVER.TLD";
    private String version_PORT = "8088";
    private String version_PATH = "/version";
    private String api_version = "1.0";
    // POST parameters
    private String postparam_name = "name";
    // misc
    private final Configuration config;

    public Updater(Configuration config) {
        this.config = config;
        readProperties();
    }

    private void readProperties() {
        // read
        version_URL = config.get(Constants.PROPKEY_JVERSIONSERVER_URL);
        version_PORT = config.get(Constants.PROPKEY_JVERSIONSERVER_PORT);
        version_PATH = config.get(Constants.PROPKEY_JVERSIONSERVER_PATH);
        api_version = config.get(Constants.PROPKEY_JVERSIONSERVER_API_VERSION);

        postparam_name = config.get(Constants.PROPKEY_POSTPARAM_JVERSIONSERVER_NAME);
    }

    public String getCurrentlyAvailableVersion() throws IOException {
        if (logger.isInfoEnabled()) {
            logger.info("Updater::getCurrentlyAvailableVersion - Checking for newer version");
        }
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httppost = null;
        try {
            httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);

            httpClient.getParams().setParameter("http.connection.timeout", 2000);
            httpClient.getParams().setParameter("http.socket.timeout", 2000);

            URI uri = new URI("http", null, getBaseURL(), getPort(), version_PATH + "/" + api_version, null, null);
            if (logger.isInfoEnabled()) {
                logger.info("Updater::getCurrentlyAvailableVersion - About to send POST request: " + uri.toURL().toString());
            }
            httppost = new HttpPost(uri.toURL().toString());

            StringBody name = new StringBody("mhri", Charset.forName("UTF-8"));

            MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            reqEntity.addPart(postparam_name, name);

            httppost.setEntity(reqEntity);

            HttpResponse response = httpClient.execute(httppost);

            if (response != null) {
                if (logger.isInfoEnabled()) {
                    logger.info("Updater::getCurrentlyAvailableVersion - Status code: " + response.getStatusLine().getStatusCode());
                }
                if (response.getStatusLine().getStatusCode() != 200) {
                    logger.error("Updater::getCurrentlyAvailableVersion - Bad status code: " + response.getStatusLine().getStatusCode());
                    throw new IOException("Bad status code");
                }
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    InputStream stream = null;
                    try {
                        stream = entity.getContent();
                        String tmp = IOUtils.toString(stream);
                        IOUtils.closeQuietly(stream);
                        EntityUtils.consume(entity);
                        if (logger.isInfoEnabled()) {
                            String currVersion = "v" + Constants.MHRI_VERSION + " build " + Constants.MHRI_BUILD;
                            logger.info("Updater::getCurrentlyAvailableVersion - This version: " + currVersion.trim());
                            logger.info("Updater::getCurrentlyAvailableVersion - Latest available version: " + tmp.trim());
                        }
                        return tmp.trim();
                    } catch (IOException ex) {
                        logger.error("Updater::getCurrentlyAvailableVersion - IOException: " + ex.getMessage());
                        throw new IOException("Response entity is null");
                    } finally {
                        if (stream != null) {
                            IOUtils.closeQuietly(stream);
                        }
                    }
                } else {
                    logger.error("Updater::getCurrentlyAvailableVersion - Response entity is null");
                    throw new IOException("Response entity is null");
                }
            } else {
                logger.error("Updater::getCurrentlyAvailableVersion - Response is null");
                throw new IOException("Response is null");
            }
        } catch (ConnectionClosedException ex) {
            logger.error("Updater::getCurrentlyAvailableVersion - ConnectionClosedException - Connection closed: " + ex.getMessage());
            throw new IOException(ex.getMessage());
        } catch (NoHttpResponseException ex) {
            logger.error("Updater::getCurrentlyAvailableVersion - NoHttpResponseException - Dropped connection without any response. Maybe the server is under heavy load: " + ex.getMessage());
            throw new IOException(ex.getMessage());
        } catch (SocketException ex) {
            logger.error("Updater::getCurrentlyAvailableVersion - SocketException: " + ex.getMessage());
            throw new IOException(ex.getMessage());
        } catch (URISyntaxException ex) {
            logger.error("Updater::getCurrentlyAvailableVersion - URISyntaxException: " + ex.getMessage());
            throw new IOException(ex.getMessage());
        } catch (IOException ex) {
            logger.error("Updater::getCurrentlyAvailableVersion - IOException: " + ex.getMessage());
            throw new IOException(ex.getMessage());
        } finally {
            if (httppost != null) {
                httppost.releaseConnection();
            }
            httpClient.getConnectionManager().shutdown();
        }
    }

    private String getBaseURL() {
        String s = this.version_URL.startsWith("http://") ? this.version_URL.substring(7) : this.version_URL;
        return s;
    }

    private int getPort() {
        int p = 8088;
        try {
            p = Integer.parseInt(version_PORT);
        } catch (NumberFormatException ex) {
            logger.error("Updater::getPort - NumberFormatException, returning default Port 8088");
        }
        return p;
    }

    public void reload() {
        readProperties();
    }
}
