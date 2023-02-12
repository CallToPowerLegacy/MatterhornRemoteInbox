/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.application.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import de.calltopower.mhri.application.api.Inbox;
import de.calltopower.mhri.application.api.Recording;
import de.calltopower.mhri.application.api.RemoteInboxApplication;
import de.calltopower.mhri.util.Constants;

/**
 * SpecificFileUtils
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class SpecificFileUtils {

    private static final Logger logger = Logger.getLogger(SpecificFileUtils.class);
    private static boolean isDeletingFiles = false;

    public static String getEpisodeXmlTemplate(RemoteInboxApplication ria) {
        if (logger.isInfoEnabled()) {
            logger.info("SpecificFileUtils::getEpisodeXmlTemplate");
        }
        String tmp = "";
        BufferedReader ir = null;
        try {
            ir = new BufferedReader(new InputStreamReader(ria.getClass().getResourceAsStream("/xml/" + Constants.FILENAME_DC_EPISODE), "UTF-8"));
            String s = ir.readLine();
            while (s != null) {
                tmp += s;
                s = ir.readLine();
            }
        } catch (IOException e) {
            logger.error("SpecificFileUtils::getEpisodeXmlTemplate - IOException (Could not read " + Constants.FILENAME_DC_EPISODE + " template): " + e.getMessage());
            throw new RuntimeException("Could not read " + Constants.FILENAME_DC_EPISODE + ".", e);
        } finally {
            if (ir != null) {
                try {
                    ir.close();
                } catch (IOException ex) {
                }
            }
        }

        byte[] utf8 = tmp.getBytes(Charset.forName("UTF-8"));
        tmp = new String(utf8, Charset.forName("UTF-8"));

        return tmp;
    }

    public static void checkForEpisodeXml(RemoteInboxApplication ria, Recording r) {
        if (logger.isInfoEnabled()) {
            logger.info("SpecificFileUtils::checkForEpisodeXml - for recording " + r.getTitle());
        }
        String fPath = r.getPath();
        fPath = fPath.endsWith(File.separator) ? fPath : (fPath + File.separator);
        File f = new File(fPath + Constants.FILENAME_DC_EPISODE);
        String episodeContent;
        if (!f.exists()) {
            if (logger.isInfoEnabled()) {
                logger.info("SpecificFileUtils::checkForEpisodeXml - file exists");
            }
            File parent = new File(fPath);

            // format date like 2012-07-24T08:40:00Z
            Date d = new Date(parent.lastModified());
            SimpleDateFormat sdf1 = new SimpleDateFormat();
            SimpleDateFormat sdf2 = new SimpleDateFormat();
            String dateFormat1 = new String("yyyy-MM-dd".getBytes(Charset.forName("UTF-8")), Charset.forName("UTF-8"));
            String dateFormat2 = new String("hh:mm:ss".getBytes(Charset.forName("UTF-8")), Charset.forName("UTF-8"));
            sdf1.applyPattern(dateFormat1);
            sdf2.applyPattern(dateFormat2);
            String theDate = new String((sdf1.format(d) + "T" + sdf2.format(d) + "Z").getBytes(Charset.forName("UTF-8")), Charset.forName("UTF-8"));

            String randomUUID = UUID.randomUUID().toString();
            String created = theDate;
            String title = parent.getName();

            byte[] utf8_created = created.getBytes(Charset.forName("UTF-8"));
            created = new String(utf8_created, Charset.forName("UTF-8"));

            byte[] utf8_title = title.getBytes(Charset.forName("UTF-8"));
            title = new String(utf8_title, Charset.forName("UTF-8"));

            episodeContent = getEpisodeXmlTemplate(ria).replaceAll("TMPL_ID", randomUUID);
            episodeContent = episodeContent.replaceAll("TMPL_CREATED", created);
            episodeContent = episodeContent.replaceAll("TMPL_TITLE", title);

            OutputStreamWriter out = null;
            try {
                out = new OutputStreamWriter(new FileOutputStream(fPath + Constants.FILENAME_DC_EPISODE), Charset.forName("UTF-8"));
                out.write(episodeContent);
            } catch (IOException e) {
                logger.error("SpecificFileUtils::checkForEpisodeXml - could not create file '" + fPath + Constants.FILENAME_DC_EPISODE + "'");
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    public static void checkForSeriesXml(Recording r) {
        if (logger.isInfoEnabled()) {
            logger.info("SpecificFileUtils::checkForSeriesXml - for recording " + r.getTitle());
        }
        File parent = new File(r.getPath()).getParentFile();
        if (parent != null) {
            String fPath = parent.getPath();
            fPath = fPath.endsWith(File.separator) ? fPath : (fPath + File.separator);
            File inF = new File(fPath + Constants.FILENAME_DC_SERIES);
            if (inF.exists() && inF.canRead()) {
                if (logger.isInfoEnabled()) {
                    logger.info("SpecificFileUtils::checkForSeriesXml - Found " + Constants.FILENAME_DC_SERIES + " -- copying into folder");
                }
                String fPath2 = r.getPath();
                fPath2 = fPath2.endsWith(File.separator) ? fPath2 : (fPath2 + File.separator);
                File outF = new File(fPath2 + Constants.FILENAME_DC_SERIES);

                InputStreamReader in = null;
                OutputStreamWriter out = null;
                try {
                    in = new InputStreamReader(new FileInputStream(inF), Charset.forName("UTF-8"));
                    out = new OutputStreamWriter(new FileOutputStream(outF), Charset.forName("UTF-8"));
                    int c;
                    while ((c = in.read()) != -1) {
                        out.write(c);
                    }
                } catch (IOException e) {
                    logger.error("SpecificFileUtils::checkForSeriesXml - IOException #1: " + e.getMessage());
                } finally {
                    try {
                        if (in != null) {
                            in.close();
                        }
                    } catch (IOException e) {
                        logger.error("SpecificFileUtils::checkForSeriesXml - IOException #2: " + e.getMessage());
                    }
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                        logger.error("SpecificFileUtils::checkForSeriesXml - IOException #2: " + e.getMessage());
                    }
                }
            }
        }
    }

    public static String getSeries(Inbox inbox) {
        String fPath = inbox.getPath();
        fPath = fPath.endsWith(File.separator) ? fPath : (fPath + File.separator);
        File inF = new File(fPath + Constants.FILENAME_DC_SERIES);
        if (inF.exists() && inF.canRead()) {
            if (logger.isInfoEnabled()) {
                logger.info("SpecificFileUtils::getSeries - Found " + Constants.FILENAME_DC_SERIES + " -- extracting name");
            }

            String seriesString = "";
            FileInputStream fis = null;
            BufferedReader ir = null;
            try {
                fis = new FileInputStream(inF);
                ir = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
                String s = ir.readLine();
                while (s != null) {
                    seriesString += s;
                    s = ir.readLine();
                }
                byte[] utf8 = seriesString.getBytes(Charset.forName("UTF-8"));
                seriesString = new String(utf8, Charset.forName("UTF-8"));

                return seriesString;
            } catch (IOException e) {
                logger.error("SpecificFileUtils::getSeries - IOException (Could not read " + fPath + Constants.FILENAME_DC_SERIES + "): " + e.getMessage());
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ex) {
                    }
                }
                if (ir != null) {
                    try {
                        ir.close();
                    } catch (IOException ex) {
                    }
                }
            }
        }
        return "";
    }

    public static String getEpisode(Recording r) {
        String fPath = r.getPath();
        fPath = fPath.endsWith(File.separator) ? fPath : (fPath + File.separator);
        File inF = new File(fPath + Constants.FILENAME_DC_EPISODE);
        if (inF.exists() && inF.canRead()) {
            if (logger.isInfoEnabled()) {
                logger.info("SpecificFileUtils::getSeries - Found " + Constants.FILENAME_DC_EPISODE + " -- extracting name");
            }

            String episodeString = "";
            FileInputStream fis = null;
            BufferedReader ir = null;
            try {
                fis = new FileInputStream(inF);
                ir = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
                String s = ir.readLine();
                while (s != null) {
                    episodeString += s;
                    s = ir.readLine();
                }
                byte[] utf8 = episodeString.getBytes(Charset.forName("UTF-8"));
                episodeString = new String(utf8, Charset.forName("UTF-8"));

                return episodeString;
            } catch (IOException e) {
                logger.error("SpecificFileUtils::getEpisode - IOException (Could not read " + fPath + Constants.FILENAME_DC_EPISODE + "): " + e.getMessage());
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ex) {
                    }
                }
                if (ir != null) {
                    try {
                        ir.close();
                    } catch (IOException ex) {
                    }
                }
            }
        }
        return "";
    }

    public static boolean saveSeries(Inbox inbox, String seriesString) {
        String fPath = inbox.getPath();
        fPath = fPath.endsWith(File.separator) ? fPath : (fPath + File.separator);
        File inF = new File(fPath + Constants.FILENAME_DC_SERIES);
        try {
            if (logger.isInfoEnabled()) {
                logger.info("SpecificFileUtils::saveSeries - Trying to write to " + Constants.FILENAME_DC_SERIES);
            }
            FileUtils.writeStringToFile(inF, seriesString);
            return true;
        } catch (IOException ex) {
            logger.error("SpecificFileUtils::saveSeries - Could not write to " + Constants.FILENAME_DC_SERIES + ": " + ex.getMessage());
        }
        return false;
    }

    public static boolean saveEpisode(Recording r, String episodeString) {
        if (r == null) {
            return false;
        }
        String fPath = r.getPath();
        fPath = fPath.endsWith(File.separator) ? fPath : (fPath + File.separator);
        File inF = new File(fPath + Constants.FILENAME_DC_EPISODE);
        try {
            if (logger.isInfoEnabled()) {
                logger.info("SpecificFileUtils::saveEpisode - Trying to write to " + Constants.FILENAME_DC_EPISODE);
            }
            FileUtils.writeStringToFile(inF, episodeString);
            return true;
        } catch (IOException ex) {
            logger.error("SpecificFileUtils::saveSeries - Could not write to " + Constants.FILENAME_DC_EPISODE + ": " + ex.getMessage());
        }
        return false;
    }

    public static boolean removeSeries(Inbox inbox) {
        String fPath = inbox.getPath();
        fPath = fPath.endsWith(File.separator) ? fPath : (fPath + File.separator);
        File inF = new File(fPath + Constants.FILENAME_DC_SERIES);
        try {
            if (logger.isInfoEnabled()) {
                logger.info("SpecificFileUtils::removeSeries - Trying to delete " + Constants.FILENAME_DC_SERIES);
            }
            return inF.delete();
        } catch (Exception ex) {
            logger.error("SpecificFileUtils::removeSeries - Could not delete " + Constants.FILENAME_DC_SERIES + ": " + ex.getMessage());
        }
        return false;
    }

    public static boolean removeEpisode(Recording r) {
        String fPath = r.getPath();
        fPath = fPath.endsWith(File.separator) ? fPath : (fPath + File.separator);
        File inF = new File(fPath + Constants.FILENAME_DC_EPISODE);
        try {
            if (logger.isInfoEnabled()) {
                logger.info("SpecificFileUtils::removeEpisode - Trying to delete " + Constants.FILENAME_DC_EPISODE);
            }
            return inF.delete();
        } catch (Exception ex) {
            logger.error("SpecificFileUtils::removeEpisode - Could not delete " + Constants.FILENAME_DC_EPISODE + ": " + ex.getMessage());
        }
        return false;
    }

    public static void deleteTmpFiles() {
        if (logger.isInfoEnabled()) {
            logger.info("SpecificFileUtils::deleteTmpFiles - Deleting temporary files");
        }
        if (!isDeletingFiles) {
            isDeletingFiles = true;
            try {
                String tmpDir = System.getProperty(Constants.MHRI_SPLIT_FILE_LOCATION);
                String str = tmpDir + Constants.MHRI_TMP_FILE_DIR_NAME_WO_PATH;
                File tmpFolder = new File(str);
                if (tmpFolder.exists() && tmpFolder.isDirectory()) {
                    for (File f : tmpFolder.listFiles()) {
                        if (f.isFile() && f.getName().endsWith(Constants.MHRI_SPLIT_FILE_SUFFIX)) {
                            try {
                                f.delete();
                            } catch (Exception ex) {
                            }
                        }
                    }
                    isDeletingFiles = false;
                }
            } catch (Exception ex) {
            } finally {
                isDeletingFiles = false;
            }
        }
    }
}
