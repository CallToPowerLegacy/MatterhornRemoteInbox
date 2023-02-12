/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.ingestclient.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
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
import de.calltopower.mhri.ingestclient.api.IngestClient;
import de.calltopower.mhri.ingestclient.api.IngestClientController;
import de.calltopower.mhri.ingestclient.api.IngestClientException;
import de.calltopower.mhri.util.Constants;
import de.calltopower.mhri.util.ParseUtils;
import de.calltopower.mhri.util.conf.Configuration;
import org.osgi.service.component.ComponentContext;

/**
 * IngestClientControllerImpl - Implements IngestClientController
 *
 * @date 19.03.2012
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
@Component(name = "de.calltopower.mhri.ingestclient.impl", immediate = true)
@Service
public class IngestClientControllerImpl implements IngestClientController {

    private static final Logger logger = Logger.getLogger(IngestClientControllerImpl.class);
    // authentication
    private String username = "";
    private String password = "";
    // REST endpoints URLs
    private static String series_list_URL = "/series/series";
    private static String series_URL = "/series";
    private static String series_create_URL = "/";
    private static String workflow_PATH = "/workflow";
    private static String workflow_definitions_PATH = "/definitions.xml";
    // GET parameters for series
    private static String getparam_series_startpage = "startPage";
    private static String series_startpage = "0";
    private static String getparam_series_count = "count";
    private static String series_count = "100";
    // POST parameters for series
    private static String postparam_createSeries = "series";
    private static String postparam_createAcl = "acl";
    // misc
    @Reference
    private Configuration config;
    private String matterhornHost = "localhost";
    private int serverPort = 8080;
    private final ExecutorService threadPool = Executors.newCachedThreadPool(); // create new threads as needed, reuse previously constructed threads
    // Apache Commons
    private CookieStore cookieStore = null;
    private DefaultHttpClient httpClient = null;
    private HttpContext client_context = null;
    private HttpPost httppost = null;
    private HttpGet httpget = null;
    // controller
    private List<IngestClient> list;

    /**
     * Set up
     *
     * @param cc ComponetnContext
     * @throws Exception
     */
    protected void activate(ComponentContext cc) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("IngestClientControllerImpl::activate - Activating");
        }
        readProperties();

        String uName = config.get(Constants.PROPKEY_USERNAME);
        String pWord = config.get(Constants.PROPKEY_PASSWORD);
        setCredentials(uName, pWord);

        list = new LinkedList<>();
    }

    /**
     * Shut down http client and task executor when service is deactivated
     *
     * @param cc ComponentContext
     * @throws Exception
     */
    protected void deactivate(ComponentContext cc) throws Exception {
        releaseConnections();
        threadPool.shutdownNow();
    }

    private IngestClient getClient_internal(String pathToRecording) {
        for (IngestClient ic : list) {
            if (ic.getID().equals(pathToRecording)) {
                return ic;
            }
        }
        return null;
    }

    @Override
    public IngestClient getClient(String pathToRecording) {
        IngestClient ic = getClient_internal(pathToRecording);
        if (ic == null) {
            ic = new IngestClientImpl(config, pathToRecording);
            list.add(ic);
        }
        return ic;
    }

    @Override
    public boolean removeClient(String pathToRecording) {
        for (IngestClient ic : list) {
            if (ic.getID().equals(pathToRecording)) {
                ic.stopProcessing();
                list.remove(ic);
                return true;
            }
        }
        return false;
    }

    private void readProperties() {
        // read
        matterhornHost = config.get(Constants.PROPKEY_HOST).toLowerCase();
        matterhornHost = getBaseURL();
        serverPort = Integer.parseInt(config.get(Constants.PROPKEY_PORT));

        series_list_URL = config.get(Constants.PROPKEY_SERIES_LIST_URL);
        series_URL = config.get(Constants.PROPKEY_SERIES);
        series_create_URL = config.get(Constants.PROPKEY_SERIES_CREATE);

        workflow_PATH = config.get(Constants.PROPKEY_GETPARAM_WORKFLOW);
        workflow_definitions_PATH = config.get(Constants.PROPKEY_GETPARAM_WORKFLOW_DEFINITIONS);

        getparam_series_startpage = config.get(Constants.PROPKEY_GETPARAM_SERIES_STARTPAGE);
        series_startpage = config.get(Constants.PROPKEY_SERIES_STARTPAGE);
        getparam_series_count = config.get(Constants.PROPKEY_GETPARAM_SERIES_COUNT);
        series_count = config.get(Constants.PROPKEY_SERIES_COUNT);
        postparam_createSeries = config.get(Constants.PROPKEY_POSTPARAM_SERIES_CREATE_SERIES);
        postparam_createAcl = config.get(Constants.PROPKEY_POSTPARAM_SERIES_CREATE_ACL);
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
                logger.info("IngestClientControllerImpl::initHttpClient - About to send POST request: " + uri.toURL().toString());
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
                logger.info("IngestClientControllerImpl::initHttpClient - Status code: " + response.getStatusLine().getStatusCode() + ", Location: " + location);
            }
            int statCode = response.getStatusLine().getStatusCode();
            if (((statCode != 200) && (statCode != 302)) || location.endsWith("error")) {
                logger.error("IngestClientControllerImpl::initHttpClient -  - Could not authenticate: IngestClientException(CLIENT_ERROR), Status Code: " + response.getStatusLine().getStatusCode());
                throw new IngestClientException("Could not authenticate", IngestClientException.Type.CLIENT_ERROR);
            }
            return client_context;
        } catch (ConnectionClosedException ex) {
            logger.error("IngestClientControllerImpl::initHttpClient - IngestClientException(NETWORK_ERROR)"
                    + "-- connection closed");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
        } catch (NoHttpResponseException ex) {
            logger.error("IngestClientControllerImpl::initHttpClient - IngestClientException(SERVER_ERROR)"
                    + "-- dropped connection without any response. Maybe the server is under heavy load.");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
        } catch (SocketException ex) {
            logger.error("IngestClientControllerImpl::initHttpClient - IngestClientException(SERVER_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
        } catch (IOException ex) {
            logger.error("IngestClientControllerImpl::initHttpClient - IngestClientException(NETWORK_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
        } catch (URISyntaxException ex) {
            logger.error("IngestClientControllerImpl::initHttpClient - URISyntaxException");
            throw ex;
        } finally {
            httppost.releaseConnection();
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
        if (httpget != null) {
            httpget.releaseConnection();
        }
        if (httppost != null) {
            httppost.releaseConnection();
        }
        list.clear();
    }

    private void setCredentials(String _username, String _password) {
        this.username = _username;
        this.password = _password;
    }

    private String getBaseURL() {
        String s = this.matterhornHost.startsWith("http://") ? this.matterhornHost.substring(7) : this.matterhornHost;
        return s;
    }

    private int getPort() {
        return this.serverPort;
    }

    public String getACL(String role) {
        String acl = "";
        acl += "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
        acl += "<acl xmlns=\"http://org.opencastproject.security\">";
        acl += "<ace>";
        acl += "<role>" + role + "</role>";
        acl += "<action>read</action>";
        acl += "<allow>true</allow>";
        acl += "</ace>";
        acl += "<ace>";
        acl += "<role>" + role + "</role>";
        acl += "<action>contribute</action>";
        acl += "<allow>true</allow>";
        acl += "</ace>";
        acl += "<ace>";
        acl += "<role>" + role + "</role>";
        acl += "<action>write</action>";
        acl += "<allow>true</allow>";
        acl += "</ace>";
        acl += "</acl>";
        return acl;
    }

    @Override
    public String createNewSeries(String document) throws IngestClientException, URISyntaxException {
        httpClient = new DefaultHttpClient();
        client_context = initHttpClient(httpClient);
        try {
            // POST /series/
            // parameter:
            // series: The series document
            // acl: The access control list for the series

            URI uri = new URI("http", null, getBaseURL(), getPort(), series_URL + series_create_URL, null, null);
            httpget = new HttpGet(uri.toURL().toString());
            if (logger.isInfoEnabled()) {
                logger.info("IngestClientControllerImpl::createNewSeries - About to send POST request: " + uri.toURL().toString());
            }

            String acl_str = getACL("ROLE_ADMIN"); // TODO: Get role from info/me.json

            httppost = new HttpPost(uri.toURL().toString());
            StringBody series = new StringBody(document, Charset.forName("UTF-8"));
            StringBody acl = new StringBody(acl_str, Charset.forName("UTF-8"));
            MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            reqEntity.addPart(postparam_createSeries, series);
            reqEntity.addPart(postparam_createAcl, acl);
            httppost.setEntity(reqEntity);
            HttpResponse response = httpClient.execute(httppost, client_context);

            String ret = "";
            HttpEntity resEntity = response.getEntity();
            int statCode = response.getStatusLine().getStatusCode();
            if (logger.isInfoEnabled()) {
                logger.info("IngestClientControllerImpl::createNewSeries - Status code: " + statCode);
            }
            // created
            if ((resEntity != null) && (statCode == 201)) {
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
                } catch (IOException ex) {
                    logger.error("IngestClientControllerImpl::createNewSeries - Caught IOException: " + ex.getMessage());
                    throw ex;
                } catch (RuntimeException ex) {
                    httppost.abort();
                    logger.error("IngestClientControllerImpl::createNewSeries - Caught RuntimeException: " + ex.getMessage());
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
            } // updated
            else if (statCode == 204) {
                return "";
            } else {
                logger.error("IngestClientControllerImpl::createNewSeries - IngestClientException(NETWORK_ERROR) 1");
                throw new IngestClientException(
                        "Got "
                        + response.getStatusLine().getStatusCode()
                        + " when attempting to start processing (GET).", IngestClientException.Type.NETWORK_ERROR);
            }

            return ret;
        } catch (URISyntaxException ex) {
            logger.error("IngestClientControllerImpl::createNewSeries - IngestClientException(CLIENT_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.CLIENT_ERROR);
        } catch (ConnectionClosedException ex) {
            logger.error("IngestClientControllerImpl::createNewSeries - IngestClientException(NETWORK_ERROR)"
                    + "-- connection closed");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
        } catch (NoHttpResponseException ex) {
            logger.error("IngestClientControllerImpl::createNewSeries - IngestClientException(SERVER_ERROR)"
                    + "-- dropped connection without any response. Maybe the server is under heavy load.");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
        } catch (SocketException ex) {
            logger.error("IngestClientControllerImpl::createNewSeries - IngestClientException(SERVER_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
        } catch (IOException ex) {
            logger.error("IngestClientControllerImpl::createNewSeries - IngestClientException(NETWORK_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
        } finally {
            releaseConnections();
        }
    }

    @Override
    public NetworkConnectionState getNetworkConnectionState() {
        if (logger.isInfoEnabled()) {
            logger.info("IngestClientControllerImpl::getNetworkConnectionState - checking network connection");
        }
        DefaultHttpClient _httpClient = new DefaultHttpClient();
        HttpPost _httppost = null;
        try {
            _httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
            CookieStore _cookieStore = new BasicCookieStore();
            HttpContext _client_context = new BasicHttpContext();
            _client_context.setAttribute(ClientContext.COOKIE_STORE, _cookieStore);

            // POST /j_spring_security_check
            // j_username: Username
            // j_password: Password
            // Return value description: Cookie
            URI uri = new URI("http", null, getBaseURL(), getPort(), "/j_spring_security_check", null, null);
            if (logger.isInfoEnabled()) {
                logger.info("IngestClientControllerImpl::getNetworkConnectionState - About to send POST request: " + uri.toURL().toString());
            }
            _httppost = new HttpPost(uri.toURL().toString());

            List<NameValuePair> nameValuePairs = new ArrayList<>(3);
            nameValuePairs.add(new BasicNameValuePair("j_username", this.username));
            nameValuePairs.add(new BasicNameValuePair("j_password", this.password));
            nameValuePairs.add(new BasicNameValuePair("submit", "Login"));
            _httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));

            HttpResponse response = _httpClient.execute(_httppost, _client_context);

            String location = response.getHeaders("Location")[0].getValue();

            if (logger.isInfoEnabled()) {
                logger.info("IngestClientControllerImpl::getNetworkConnectionState - Status code: " + response.getStatusLine().getStatusCode() + ", Location: " + location);
            }
            int statCode = response.getStatusLine().getStatusCode();
            if (((statCode != 200) && (statCode != 302)) || location.endsWith("error")) {
                logger.error("IngestClientControllerImpl::getNetworkConnectionState - Status Code: " + response.getStatusLine().getStatusCode());
                return NetworkConnectionState.WRONG_CREDENTIALS;
            }
            return NetworkConnectionState.ONLINE;
        } catch (ConnectionClosedException ex) {
            // logger.error("IngestClientControllerImpl::getNetworkConnectionState - IngestClientException(NETWORK_ERROR)" + "-- connection closed");
            return NetworkConnectionState.SERVER_OFFLINE;
        } catch (NoHttpResponseException ex) {
            // logger.error("IngestClientControllerImpl::getNetworkConnectionState - IngestClientException(SERVER_ERROR)"  "-- dropped connection without any response. Maybe the server is under heavy load.");
            return NetworkConnectionState.SERVER_OFFLINE;
        } catch (SocketException ex) {
            // logger.error("IngestClientControllerImpl::getNetworkConnectionState - IngestClientException(SERVER_ERROR)");
            return NetworkConnectionState.SERVER_OFFLINE;
        } catch (IOException ex) {
            // logger.info("IngestClientControllerImpl::getNetworkConnectionState - IngestClientException(NETWORK_ERROR)");
            return NetworkConnectionState.CLIENT_OFFLINE;
        } catch (URISyntaxException ex) {
            // logger.error("IngestClientControllerImpl::getNetworkConnectionState - URISyntaxException");
            return NetworkConnectionState.ERROR;
        } finally {
            if (_httppost != null) {
                _httppost.releaseConnection();
            }
            _httpClient.getConnectionManager().shutdown();
        }
    }

    // getter
    @Override
    public String getSeriesCatalog(String seriesID) throws IngestClientException, URISyntaxException {
        httpClient = new DefaultHttpClient();
        client_context = initHttpClient(httpClient);
        try {
            // GET /seriesID.format, format = {xml, json}

            URI uri = new URI("http", null, getBaseURL(), getPort(), series_URL + "/" + seriesID + Constants.SERIES_CATALOG_FORMAT, null, null);
            httpget = new HttpGet(uri.toURL().toString());
            if (logger.isInfoEnabled()) {
                logger.info("IngestClientControllerImpl::getSeriesCatalog - About to send GET request: " + uri.toURL().toString());
            }
            HttpResponse response_get = httpClient.execute(httpget, client_context);

            if (logger.isInfoEnabled()) {
                logger.info("IngestClientControllerImpl::getSeriesCatalog - Status code: " + response_get.getStatusLine().getStatusCode());
            }
            String series = "";
            if (response_get.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity_get = response_get.getEntity();
                if (entity_get != null) {

                    BufferedReader reader = null;
                    InputStream instream = null;
                    try {
                        instream = entity_get.getContent();
                        reader = new BufferedReader(new InputStreamReader(instream, "UTF-8"));
                        String line = "";
                        while (line != null) {
                            series += line;
                            line = reader.readLine();
                        }
                    } catch (IOException ex) {
                        logger.error("IngestClientimpl::getSeriesCatalog - Caught IOException 1: " + ex.getMessage());
                    } catch (RuntimeException ex) {
                        httpget.abort();
                        logger.error("IngestClientimpl::getSeriesCatalog - Caught RuntimeException 1: " + ex.getMessage());
                    } finally {
                        EntityUtils.consume(entity_get);
                        if (instream != null) {
                            instream.close();
                        }
                        if (reader != null) {
                            reader.close();
                        }
                    }
                } else {
                    logger.error("IngestClientControllerImpl::getSeriesCatalog - IngestClientException(CLIENT_ERROR)");
                    throw new IngestClientException("Entity is null", IngestClientException.Type.CLIENT_ERROR);
                }
            } else {
                logger.error("IngestClientControllerImpl::getSeriesCatalog - IngestClientException(NETWORK_ERROR) 1, Status Code: " + response_get.getStatusLine().getStatusCode());
                throw new IngestClientException(
                        "Got "
                        + response_get.getStatusLine().getStatusCode()
                        + " when attempting to start processing (GET).", IngestClientException.Type.NETWORK_ERROR);
            }

            return series;
        } catch (URISyntaxException ex) {
            logger.error("IngestClientControllerImpl::getSeriesCatalog - IngestClientException(CLIENT_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.CLIENT_ERROR);
        } catch (ConnectionClosedException ex) {
            logger.error("IngestClientControllerImpl::getSeriesCatalog - IngestClientException(NETWORK_ERROR)"
                    + "-- connection closed");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
        } catch (NoHttpResponseException ex) {
            logger.error("IngestClientControllerImpl::getSeriesCatalog - IngestClientException(SERVER_ERROR)"
                    + "-- dropped connection without any response. Maybe the server is under heavy load.");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
        } catch (SocketException ex) {
            logger.error("IngestClientControllerImpl::getSeriesCatalog - IngestClientException(SERVER_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
        } catch (IOException ex) {
            logger.error("IngestClientControllerImpl::getSeriesCatalog - IngestClientException(NETWORK_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
        } finally {
            releaseConnections();
        }
    }

    @Override
    public HashMap<String, String> getSeriesList() throws IOException, IngestClientException, URISyntaxException {
        httpClient = new DefaultHttpClient();
        client_context = initHttpClient(httpClient);
        try {
            // GET /series.format, format = {xml, json}
            String seriesCnt = "";
            try {
                seriesCnt = IngestClientControllerImpl.getparam_series_startpage + "=" + IngestClientControllerImpl.series_startpage
                        + "&" + IngestClientControllerImpl.getparam_series_count + "=" + IngestClientControllerImpl.series_count;
            } catch (NumberFormatException nfe) {
            }
            URI uri = new URI("http", null, getBaseURL(), getPort(), IngestClientControllerImpl.series_list_URL + Constants.SERIES_LIST_FORMAT, seriesCnt, null);
            httpget = new HttpGet(uri.toURL().toString());
            if (logger.isInfoEnabled()) {
                logger.info("IngestClientControllerImpl::getSeriesList - About to send GET request: " + uri.toURL().toString());
            }
            HttpResponse response_get = httpClient.execute(httpget, client_context);

            if (logger.isInfoEnabled()) {
                logger.info("IngestClientControllerImpl::getSeriesList - Status code: " + response_get.getStatusLine().getStatusCode());
            }
            String seriesList = "";
            if (response_get.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity_get = response_get.getEntity();
                if (entity_get != null) {

                    InputStream instream = null;
                    BufferedReader reader = null;
                    try {
                        instream = entity_get.getContent();
                        reader = new BufferedReader(new InputStreamReader(instream, "UTF-8"));
                        String line = "";
                        while (line != null) {
                            seriesList += line;
                            line = reader.readLine();
                        }
                    } catch (IOException ex) {
                        logger.error("IngestClientimpl::getSeriesList - Caught IOException 1: " + ex.getMessage());
                    } catch (RuntimeException ex) {
                        httpget.abort();
                        logger.error("IngestClientimpl::getSeriesList - Caught RuntimeException 1: " + ex.getMessage());
                    } finally {
                        EntityUtils.consume(entity_get);
                        if (instream != null) {
                            instream.close();
                        }
                        if (reader != null) {
                            reader.close();
                        }
                    }
                } else {
                    logger.error("IngestClientControllerImpl::getSeriesList - IngestClientException(CLIENT_ERROR)");
                    throw new IngestClientException("Entity is null", IngestClientException.Type.CLIENT_ERROR);
                }
            } else {
                logger.error("IngestClientControllerImpl::getSeriesList - IngestClientException(NETWORK_ERROR) 1, Status Code: " + response_get.getStatusLine().getStatusCode());
                throw new IngestClientException(
                        "Got "
                        + response_get.getStatusLine().getStatusCode()
                        + " when attempting to start processing (GET).", IngestClientException.Type.NETWORK_ERROR);
            }
            if (seriesList.isEmpty()) {
                logger.error("IngestClientControllerImpl::getSeriesList - IngestClientException(NETWORK_ERROR) 1");
                throw new IngestClientException("No Series List could be retrieved.", IngestClientException.Type.NETWORK_ERROR);
            }

            return ParseUtils.getInstance().parseSeriesList(seriesList);
        } catch (URISyntaxException ex) {
            logger.error("IngestClientControllerImpl::getSeriesList - IngestClientException(CLIENT_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.CLIENT_ERROR);
        } catch (ConnectionClosedException ex) {
            logger.error("IngestClientControllerImpl::getSeriesList - IngestClientException(NETWORK_ERROR)"
                    + "-- connection closed");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
        } catch (NoHttpResponseException ex) {
            logger.error("IngestClientControllerImpl::getSeriesList - IngestClientException(SERVER_ERROR)"
                    + "-- dropped connection without any response. Maybe the server is under heavy load.");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
        } catch (SocketException ex) {
            logger.error("IngestClientControllerImpl::getSeriesList - IngestClientException(SERVER_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
        } catch (IOException ex) {
            logger.error("IngestClientControllerImpl::getSeriesList - IngestClientException(NETWORK_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
        } finally {
            releaseConnections();
        }
    }

    @Override
    public List<String> getWorkflowList() throws IOException, IngestClientException, URISyntaxException {
        httpClient = new DefaultHttpClient();
        client_context = initHttpClient(httpClient);
        try {
            // GET /definitions.format, format = {xml, json}
            URI uri = new URI("http", null, getBaseURL(), getPort(), IngestClientControllerImpl.workflow_PATH + IngestClientControllerImpl.workflow_definitions_PATH, null, null);
            httpget = new HttpGet(uri.toURL().toString());
            if (logger.isInfoEnabled()) {
                logger.info("IngestClientControllerImpl::getWorkflowList - About to send GET request: " + uri.toURL().toString());
            }
            HttpResponse response_get = httpClient.execute(httpget, client_context);

            if (logger.isInfoEnabled()) {
                logger.info("IngestClientControllerImpl::getWorkflowList - Status code: " + response_get.getStatusLine().getStatusCode());
            }
            String workflowList = "";
            if (response_get.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity_get = response_get.getEntity();
                if (entity_get != null) {

                    InputStream instream = null;
                    BufferedReader reader = null;
                    try {
                        instream = entity_get.getContent();
                        reader = new BufferedReader(new InputStreamReader(instream, "UTF-8"));
                        String line = "";
                        while (line != null) {
                            workflowList += line;
                            line = reader.readLine();
                        }
                    } catch (IOException ex) {
                        logger.error("IngestClientimpl::getWorkflowList - Caught IOException 1: " + ex.getMessage());
                    } catch (RuntimeException ex) {
                        httpget.abort();
                        logger.error("IngestClientimpl::getWorkflowList - Caught RuntimeException 1: " + ex.getMessage());
                    } finally {
                        EntityUtils.consume(entity_get);
                        if (instream != null) {
                            instream.close();
                        }
                        if (reader != null) {
                            reader.close();
                        }
                    }
                } else {
                    logger.error("IngestClientControllerImpl::getWorkflowList - IngestClientException(CLIENT_ERROR)");
                    throw new IngestClientException("Entity is null", IngestClientException.Type.CLIENT_ERROR);
                }
            } else {
                logger.error("IngestClientControllerImpl::getWorkflowList - IngestClientException(NETWORK_ERROR) 1, Status Code: " + response_get.getStatusLine().getStatusCode());
                throw new IngestClientException(
                        "Got "
                        + response_get.getStatusLine().getStatusCode()
                        + " when attempting to start processing (GET).", IngestClientException.Type.NETWORK_ERROR);
            }
            if (workflowList.isEmpty()) {
                logger.error("IngestClientControllerImpl::getWorkflowList - IngestClientException(NETWORK_ERROR) 1");
                throw new IngestClientException("No Series List could be retrieved.", IngestClientException.Type.NETWORK_ERROR);
            }

            return ParseUtils.getInstance().parseWorkflowList(workflowList);
        } catch (URISyntaxException ex) {
            logger.error("IngestClientControllerImpl::getWorkflowList - IngestClientException(CLIENT_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.CLIENT_ERROR);
        } catch (ConnectionClosedException ex) {
            logger.error("IngestClientControllerImpl::getWorkflowList - IngestClientException(NETWORK_ERROR)"
                    + "-- connection closed");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
        } catch (NoHttpResponseException ex) {
            logger.error("IngestClientControllerImpl::getWorkflowList - IngestClientException(SERVER_ERROR)"
                    + "-- dropped connection without any response. Maybe the server is under heavy load.");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
        } catch (SocketException ex) {
            logger.error("IngestClientControllerImpl::getWorkflowList - IngestClientException(SERVER_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.SERVER_ERROR);
        } catch (IOException ex) {
            logger.error("IngestClientControllerImpl::getWorkflowList - IngestClientException(NETWORK_ERROR)");
            throw new IngestClientException(ex.getMessage(), IngestClientException.Type.NETWORK_ERROR);
        } finally {
            releaseConnections();
        }
    }

    @Override
    public void reload() {
        releaseConnections();
        readProperties();

        String uName = config.get(Constants.PROPKEY_USERNAME);
        String pWord = config.get(Constants.PROPKEY_PASSWORD);
        setCredentials(uName, pWord);
    }
}
