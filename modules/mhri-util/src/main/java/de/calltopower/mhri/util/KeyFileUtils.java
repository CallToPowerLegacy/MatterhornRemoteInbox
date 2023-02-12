/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * KeyFileUtils
 *
 * @date 11.07.2013
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class KeyFileUtils {

    private String generateNewKey() {
        return "MHRI_" + UUID.randomUUID().toString();
    }

    public void writeToFile(String pathToFile, String text) throws IOException {
        OutputStreamWriter out = null;
        File f = new File(pathToFile).getAbsoluteFile();
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
                try {
                    out.close();
                } catch (IOException ex) {
                    throw ex;
                }
            }
        }
    }

    private String createNewKeyFile(String keyFilePath) throws IOException {
        String newKey = generateNewKey();
        writeToFile(keyFilePath, newKey);
        return newKey;
    }

    public String getKey(String keyFilePath) throws IOException {
        InputStream is = null;
        try {
            File f = new File(keyFilePath).getAbsoluteFile();
            if (!f.exists()) {
                if (!f.createNewFile()) {
                    throw new IOException("KeyFileTools::getKey - Cannot create key file: " + keyFilePath);
                } else {
                    createNewKeyFile(keyFilePath);
                }
            }
            if (f.isFile() && f.canRead()) {
                BufferedReader in = null;
                try {
                    in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
                    String line;
                    while ((line = in.readLine().trim()) != null) {
                        if (!line.isEmpty()) {
                            return line;
                        }
                    }
                } catch (IOException ex) {
                    throw new IOException("KeyFileTools::getKey - Cannot read file or is not a file: " + keyFilePath);
                } finally {
                    if (in != null) {
                        in.close();
                    }
                }
                return createNewKeyFile(keyFilePath);
            } else {
                throw new IOException("KeyFileTools::getKey - Cannot read file or is not a file: " + keyFilePath);
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }
}
