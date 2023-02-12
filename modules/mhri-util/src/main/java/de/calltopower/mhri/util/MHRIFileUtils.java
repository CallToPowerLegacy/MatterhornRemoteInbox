/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * MHRIFileUtils (singleton) -- File Helper functions
 *
 * @date 12.03.2012
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class MHRIFileUtils {

    private static MHRIFileUtils h = null;

    /**
     * Constructor
     */
    private MHRIFileUtils() {
    }

    /**
     * Returns the Helper instance
     *
     * @return the Helper instance
     */
    public static MHRIFileUtils getInstance() {
        if (h == null) {
            h = new MHRIFileUtils();
        }
        return h;
    }

    /**
     * Checks whether file is a file
     *
     * @param file file to check
     * @return true if file is a file, false else
     */
    public boolean isFile(String file) {
        return new File(file).isFile();
    }

    /**
     * Checks whether directory is a directory
     *
     * @param directory directory to check
     * @return true if directory is a directory, false else
     */
    public boolean isDirectory(String directory) {
        return new File(directory).isDirectory();
    }

    public String getNewTmpFileName(String fileDirWOPath, String fileNameWOPath) {
        String tmpDir = System.getProperty(Constants.MHRI_SPLIT_FILE_LOCATION);
        tmpDir = tmpDir.endsWith(File.separator) ? tmpDir : (tmpDir + File.separator);
        File f = new File(tmpDir + fileDirWOPath);
        f.mkdirs();
        return tmpDir + fileDirWOPath + File.separator + fileNameWOPath + "_" + UUID.randomUUID().toString();
    }

    public int calculateNumberOfSplitFiles(long fileLength, int chunkSize) {
        int noOfSplitFiles = (int) Math.floor(fileLength / (long) chunkSize) + 1;
        return noOfSplitFiles;
    }

    /**
     * Recursively scans directory down to the specified depth and returns a
     * List of the children's absolute paths.
     *
     * @param directory
     * @param depth
     * @return
     */
    public List<String> scanDir(File directory, int depth) {
        List<String> out = new LinkedList<>();
        File[] files = directory.listFiles();
        for (File file : files) {
            File f = file.getAbsoluteFile();
            if (depth > 0) {
                if (f.isDirectory()) {
                    out.add(f.getAbsolutePath());
                    out.addAll(scanDir(f, depth - 1));
                }
            } else {
                if (f.isFile()) {
                    out.add(f.getAbsolutePath());
                }
            }
        }
        return out;
    }

    /**
     * Returns true if name ends with one of the suffixes, false otherwise.
     *
     * @param name
     * @param suffixes
     * @return
     */
    public boolean endsWithOne(String name, String[] suffixes) {
        String[] pieces = name.split("\\.");
        if (pieces.length < 2) {
            return false;
        }
        String suffix = pieces[pieces.length - 1];
        for (String suff : suffixes) {
            if (suffix.equalsIgnoreCase(suff)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Recursively deletes a directory and all its children.
     *
     * @param f
     */
    public void delete(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            for (File child : children) {
                if (child.isDirectory()) {
                    delete(child);
                } else if (child.isFile()) {
                    try {
                        child.delete();
                    } catch (Exception e) {
                    }
                }
            }
        }
        try {
            f.delete();
        } catch (Exception e) {
        }
    }

    public void writeToFile(String pathToFile, String text) throws IOException {
        OutputStreamWriter out = null;
        File f = new File(pathToFile);
        try {
            if (!f.exists() && !f.createNewFile()) {
                throw new IOException("Could not create file " + pathToFile);
            }
            if (f.canWrite()) {
                out = new OutputStreamWriter(new FileOutputStream(f), Charset.forName("UTF-8"));
                out.write(text);
                out.flush();
            } else {
                throw new IOException("Can't write to file " + pathToFile);
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    // fileSize in bytes
    public String getFormattedSize(double fileSize) {
        String sizeStr = "";
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
}
