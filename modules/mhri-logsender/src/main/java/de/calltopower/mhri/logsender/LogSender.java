/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.logsender;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
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
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import de.calltopower.mhri.util.Constants;
import de.calltopower.mhri.util.conf.Configuration;

/**
 * LogSender - Sends log files
 *
 * @date 11.07.2013
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class LogSender {

    private static final Logger logger = Logger.getLogger(LogSender.class);
    // REST endpoints URLs
    private String log_URL = "YOUR.SERVER.TLD";
    private String log_PORT = "8088";
    private String log_PATH = "/log";
    private String api_version = "1.0";
    // POST parameters
    private String postparam_key = "key";
    private String postparam_name = "name";
    private String postparam_version = "version";
    private String postparam_build = "build";
    private String postparam_loglevel = "loglevel";
    private String postparam_autogenerated = "autogenerated";
    private String postparam_manualdescription = "manualdescription";
    private String postparam_logfile = "logfile";
    // misc
    private final Configuration config;
    HttpClient httpClient;
    CookieStore cookieStore;
    HttpContext client_context;
    HttpPost httppost;

    public LogSender(Configuration config) {
        this.config = config;
        readProperties();
    }

    private void readProperties() {
        // read
        log_URL = config.get(Constants.PROPKEY_JLOGSERVER_URL);
        log_PORT = config.get(Constants.PROPKEY_JLOGSERVER_PORT);
        log_PATH = config.get(Constants.PROPKEY_JLOGSERVER_PATH);
        api_version = config.get(Constants.PROPKEY_JLOGSERVER_API_VERSION);

        postparam_key = config.get(Constants.PROPKEY_POSTPARAM_JLOGSERVER_KEY);
        postparam_name = config.get(Constants.PROPKEY_POSTPARAM_JLOGSERVER_NAME);
        postparam_version = config.get(Constants.PROPKEY_POSTPARAM_JLOGSERVER_VERSION);
        postparam_build = config.get(Constants.PROPKEY_POSTPARAM_JLOGSERVER_BUILD);
        postparam_loglevel = config.get(Constants.PROPKEY_POSTPARAM_JLOGSERVER_LOGLEVEL);
        postparam_autogenerated = config.get(Constants.PROPKEY_POSTPARAM_JLOGSERVER_AUTOGENERATED);
        postparam_manualdescription = config.get(Constants.PROPKEY_POSTPARAM_JLOGSERVER_MANUALDESCRIPTION);
        postparam_logfile = config.get(Constants.PROPKEY_POSTPARAM_JLOGSERVER_LOGFILE);
    }

    public boolean sendLog(
            String p_key,
            String p_name,
            String p_version,
            String p_build,
            int p_loglevel,
            boolean p_autogenerated,
            String p_manualDescription,
            String p_pathToLogFile) throws IOException, FileNotFoundException {
        if (logger.isInfoEnabled()) {
            logger.info("LogSender::sendLog - Key: " + p_key);
            logger.info("LogSender::sendLog - Name: " + p_name);
            logger.info("LogSender::sendLog - Version: " + p_version);
            logger.info("LogSender::sendLog - Build: " + p_build);
            logger.info("LogSender::sendLog - Log level: " + p_loglevel);
            logger.info("LogSender::sendLog - Autogenerated: " + p_autogenerated);
            logger.info("LogSender::sendLog - Manual description: " + p_manualDescription);
            logger.info("LogSender::sendLog - Path to log file: " + p_pathToLogFile);
        }

        File f = new File(p_pathToLogFile);
        if (!f.exists()) {
            throw new FileNotFoundException("No log file found");
        } else if (!f.canRead()) {
            throw new IOException("Cannot read log file");
        }

        httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
        cookieStore = new BasicCookieStore();
        client_context = new BasicHttpContext();
        client_context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
        httpClient.getParams().setParameter("http.connection.timeout", 2000);
        httpClient.getParams().setParameter("http.socket.timeout", 2000);

        try {
            if (logger.isInfoEnabled()) {
                logger.info("LogSender::sendLog - Sending log");
            }
            // POST /log/x.y
            URI uri = new URI("http", null, getBaseURL(), getPort(), log_PATH + "/" + api_version, null, null);
            if (logger.isInfoEnabled()) {
                logger.info("LogSender::sendLog - About to send POST request: " + uri.toURL().toString());
            }
            httppost = new HttpPost(uri.toURL().toString());

            StringBody key = new StringBody(p_key, "text/plain", Charset.forName("UTF-8"));
            StringBody name = new StringBody(p_name, "text/plain", Charset.forName("UTF-8"));
            StringBody version = new StringBody(p_version, "text/plain", Charset.forName("UTF-8"));
            StringBody build = new StringBody(p_build, "text/plain", Charset.forName("UTF-8"));
            StringBody loglevel = new StringBody(String.valueOf(p_loglevel), "text/plain", Charset.forName("UTF-8"));
            StringBody autogenerated = new StringBody(String.valueOf(p_autogenerated), "text/plain", Charset.forName("UTF-8"));
            StringBody manualDescription = new StringBody(p_manualDescription, "text/plain", Charset.forName("UTF-8"));
            FileBody logFile = new FileBody(f.getAbsoluteFile(), "text/plain");

            MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            reqEntity.addPart(postparam_key, key);
            reqEntity.addPart(postparam_name, name);
            reqEntity.addPart(postparam_version, version);
            reqEntity.addPart(postparam_build, build);
            reqEntity.addPart(postparam_loglevel, loglevel);
            reqEntity.addPart(postparam_autogenerated, autogenerated);
            reqEntity.addPart(postparam_manualdescription, manualDescription);
            reqEntity.addPart(postparam_logfile, logFile);

            httppost.setEntity(reqEntity);

            HttpResponse response;
            try {
                response = httpClient.execute(httppost, client_context);
            } catch (Exception ex) {
                logger.error("LogSender::sendLog - (Protocol)Exception: " + ex.getMessage());
                throw ex;
            }
            HttpEntity resEntity = response.getEntity();
            String resp = EntityUtils.toString(resEntity, "UTF-8");
            if (logger.isInfoEnabled()) {
                logger.info("LogSender::sendLog - Response: " + resp);
            }

            EntityUtils.consume(resEntity);
            if (logger.isInfoEnabled()) {
                logger.info("LogSender::sendLog - Status code: " + response.getStatusLine().getStatusCode());
            }
            if (response.getStatusLine().getStatusCode() != 200) {
                logger.error("LogSender::sendLog - Status code: " + response.getStatusLine().getStatusCode());
                return false;
            }

            return true;
        } catch (URISyntaxException ex) {
            logger.error("LogSender::sendLog - URISyntaxException: " + ex.getMessage());
            throw new IOException("Could not send log");
        } catch (MalformedURLException ex) {
            logger.error("LogSender::sendLog - MalformedURLException: " + ex.getMessage());
            throw new IOException("Could not send log");
        } catch (ConnectionClosedException ex) {
            logger.error("LogSender::sendLog - ConnectionClosedException: " + ex.getMessage());
            throw new IOException("Could not send log: Connection closed");
        } catch (NoHttpResponseException ex) {
            logger.error("LogSender::sendLog - NoHttpResponseException: " + ex.getMessage());
            throw new IOException("Could not send log: No HTTP response");
        } catch (SocketException ex) {
            logger.error("LogSender::sendLog - SocketException: " + ex.getMessage());
            throw new IOException("Could not send log: Socket exception");
        } finally {
            stopUpload();
        }
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

    private void stopUpload() {
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

    private String getBaseURL() {
        String s = this.log_URL.startsWith("http://") ? this.log_URL.substring(7) : this.log_URL;
        return s;
    }

    private int getPort() {
        int p = 8088;
        try {
            p = Integer.parseInt(log_PORT);
        } catch (NumberFormatException ex) {
            logger.error("Updater::getPort - NumberFormatException, returning default Port 8088");
        }
        return p;
    }

    public void reload() {
        readProperties();
    }
}
