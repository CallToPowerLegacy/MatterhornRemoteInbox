/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.util;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import org.apache.log4j.Logger;

/**
 * Lock
 *
 * @date 19.05.2013
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class Lock {

    private static final Logger logger = Logger.getLogger(Lock.class);
    private final String appName;
    private File file;
    private FileChannel channel;
    private FileLock lock;

    public Lock(String appName) {
        this.appName = appName;
    }

    public boolean otherInstanceIsRunning() {
        if (logger.isInfoEnabled()) {
            logger.info("LockImpl::otherInstanceIsRunning - Checking for other instances named '" + this.appName + "'");
        }
        try {
            file = new File(System.getProperty(Constants.MHRI_LOCK_FILE_LOCATION), appName + Constants.MHRI_LOCK_FILE_SUFFIX);
            if (logger.isInfoEnabled()) {
                logger.info("LockImpl::otherInstanceIsRunning - Checking for lock file '" + file.getAbsolutePath() + "'");
            }
            channel = new RandomAccessFile(file, "rw").getChannel();

            try {
                lock = channel.tryLock();
            } catch (OverlappingFileLockException e) {
                // already locked
                logger.error("LockImpl::otherInstanceIsRunning - An instance is already running");
                closeLock();
                return true;
            }

            if (lock == null) {
                logger.error("LockImpl::otherInstanceIsRunning - An instance is already running");
                closeLock();
                return true;
            } else {
                if (logger.isInfoEnabled()) {
                    logger.info("LockImpl::otherInstanceIsRunning - No other instance seems to be running");
                }
            }

            Runtime.getRuntime().addShutdownHook(new Thread() {
                // destroy the lock when the JVM is shutting down
                @Override
                public void run() {
                    if (logger.isInfoEnabled()) {
                        logger.info("LockImpl::otherInstanceIsRunning - JVM is shutting down - closing the lock");
                    }
                    closeLock();
                    deleteFile();
                }
            });
            return false;
        } catch (Exception e) {
            logger.error("LockImpl::otherInstanceIsRunning - Can't lock the file '" + this.appName + "'");
            closeLock();
            return true;
        }
    }

    private void closeLock() {
        try {
            lock.release();
        } catch (Exception e) {
        }
        try {
            channel.close();
        } catch (Exception e) {
        }
    }

    private void deleteFile() {
        try {
            file.delete();
        } catch (Exception e) {
        }
    }
}
