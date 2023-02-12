/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ParseUtils (singleton) -- Parse Helper functions
 *
 * @date 16.07.2013
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class ParseUtils {

    private static ParseUtils h = null;

    /**
     * Constructor
     */
    private ParseUtils() {
    }

    /**
     * Returns the Helper instance
     *
     * @return the Helper instance
     */
    public static ParseUtils getInstance() {
        if (h == null) {
            h = new ParseUtils();
        }
        return h;
    }

    public HashMap<String, String> parseSeriesList(String seriesListString) {
        HashMap<String, String> seriesList = new HashMap<>();
        // filter series ID -- get first appearance of prefixIDSOMETHINGpostfixID
        String prefixID = "<dcterms:identifier>";
        String postfixID = "</dcterms:identifier>";
        Pattern pattern = Pattern.compile(prefixID + "(.+?)" + postfixID);
        Matcher matcher = pattern.matcher(seriesListString);
        ArrayList<String> seriesIDs = new ArrayList<>();
        while (matcher.find()) {
            seriesIDs.add(seriesListString.substring(matcher.start() + prefixID.length(), matcher.end() - postfixID.length()));
        }
        // filter series title -- get first appearance of prefixTitleSOMETHINGpostfixTitle
        String prefixTitle = "<dcterms:title>";
        String postfixTitle = "</dcterms:title>";
        pattern = Pattern.compile(prefixTitle + "(.+?)" + postfixTitle);
        matcher = pattern.matcher(seriesListString);
        int i = 0;
        while (matcher.find()) {
            if (i < seriesIDs.size()) {
                seriesList.put(seriesIDs.get(i), seriesListString.substring(matcher.start() + prefixTitle.length(), matcher.end() - postfixTitle.length()));
                ++i;
            }
        }

        return seriesList;
    }

    public List<String> parseWorkflowList(String workflowListString) {
        List<String> workflowList = new LinkedList<>();
        // filter workflow ID -- get first appearance of prefixIDSOMETHINGpostfixID
        String prefixID = "<id>";
        String postfixID = "</id>";
        Pattern pattern = Pattern.compile(prefixID + "(.+?)" + postfixID);
        Matcher matcher = pattern.matcher(workflowListString);
        while (matcher.find()) {
            workflowList.add(workflowListString.substring(matcher.start() + prefixID.length(), matcher.end() - postfixID.length()));
        }

        return workflowList;
    }

    public String parseState(String instanceXML) {
        String state = "";
        String prefix = "<workflow state=\"";
        String postfix = "\"";
        Pattern pattern = Pattern.compile(prefix + "(.+?)" + postfix);
        Matcher matcher = pattern.matcher(instanceXML);
        boolean found = false;
        while (matcher.find() && !found) {
            state = instanceXML.substring(matcher.start() + prefix.length(), matcher.end() - postfix.length());
            found = true;
        }

        return state;
    }

    public String parseEngageURL(String instanceXML) {
        String state = "";
        String prefix = "<ns3:url>";
        String postfix = "</ns3:url>";
        Pattern pattern = Pattern.compile(prefix + "(.+?)" + postfix);
        Matcher matcher = pattern.matcher(instanceXML);
        boolean found = false;
        while (matcher.find() && !found) {
            state = instanceXML.substring(matcher.start() + prefix.length(), matcher.end() - postfix.length());
            found = true;
        }

        return state;
    }

    public String getMediaPackageID(String mediaPackageXML) {
        String id = "";
        // filter id
        Pattern pattern = Pattern.compile("id=\"(.+?)\"");
        Matcher matcher = pattern.matcher(mediaPackageXML);
        boolean found = false;
        // get first id="<id>"
        while (matcher.find() && !found) {
            id = mediaPackageXML.substring(matcher.start() + 4, matcher.end() - 1);
            found = true;
        }
        if (id.length() < 8) {
            id = "";
        }

        return id;
    }

    public String parseTrackURL(String doc) {
        String trackUrl = "";
        // filter id -- get first appearance of <url><trackURL></url>
        String prefix = "<url>";
        String postfix = "</url>";
        Pattern pattern = Pattern.compile(prefix + "(.+?)" + postfix);
        Matcher matcher = pattern.matcher(doc);
        boolean found = false;
        while (matcher.find() && !found) {
            trackUrl = doc.substring(matcher.start() + prefix.length(), matcher.end() - postfix.length());
            found = true;
        }

        if (trackUrl.length() < 8) {
            trackUrl = "";
        }

        return trackUrl;
    }

    // format: xml: 1 (default), json: 2
    public String parseState(int format, String doc) {
        String ret;
        format = ((format == 1) || (format == 2)) ? format : 1;
        if (format == 1) { // format: XML
            if (doc.contains("state=\"" + UploadableFileState.State.READY.toString() + "\"")) {
                return UploadableFileState.State.READY.toString();
            } else if (doc.contains("state=\"" + UploadableFileState.State.INPROGRESS.toString() + "\"")) {
                return UploadableFileState.State.INPROGRESS.toString();
            } else if (doc.contains("state=\"" + UploadableFileState.State.FINALIZING.toString() + "\"")) {
                return UploadableFileState.State.FINALIZING.toString();
            } else if (doc.contains("state=\"" + UploadableFileState.State.COMPLETE.toString() + "\"")) {
                return UploadableFileState.State.COMPLETE.toString();
            } else {
                ret = ""; // if an error occurred: Reset return value
            }
        } else { // format: JSON
            if (doc.contains("\"state\":\"" + UploadableFileState.State.READY.toString() + "\"")) {
                return UploadableFileState.State.READY.toString();
            } else if (doc.contains("\"state\":\"" + UploadableFileState.State.INPROGRESS.toString() + "\"")) {
                return UploadableFileState.State.INPROGRESS.toString();
            } else if (doc.contains("\"state\":\"" + UploadableFileState.State.FINALIZING.toString() + "\"")) {
                return UploadableFileState.State.FINALIZING.toString();
            } else if (doc.contains("\"state\":\"" + UploadableFileState.State.COMPLETE.toString() + "\"")) {
                return UploadableFileState.State.COMPLETE.toString();
            } else {
                ret = ""; // if an error occurred: Reset return value
            }
        }

        return ret;
    }

    public String getFirstAppearanceOf(String s, String prefix, String postfix) {
        if (!s.isEmpty()) {
            Pattern pattern = Pattern.compile(prefix + "(.+?)" + postfix);
            Matcher matcher = pattern.matcher(s);
            if (matcher.find()) {
                return s.substring(matcher.start() + prefix.length(), matcher.end() - postfix.length());
            }
        }
        return "";
    }
}
