/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.directorywatch.impl;

import de.calltopower.mhri.directorywatch.api.events.FileModifiedEventListener;
import de.calltopower.mhri.directorywatch.api.events.FileDeletedEventListener;
import de.calltopower.mhri.directorywatch.api.events.FileCreatedEventListener;
import de.calltopower.mhri.directorywatch.api.events.FileCreatedEvent;
import de.calltopower.mhri.directorywatch.api.events.FileDeletedEvent;
import de.calltopower.mhri.directorywatch.api.events.FileModifiedEvent;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.log4j.Logger;
import de.calltopower.mhri.directorywatch.api.DirectoryWatch;
import de.calltopower.mhri.util.MHRIFileUtils;
import org.osgi.service.component.ComponentContext;

/**
 * DirectoryWatchImpl - Implements DirectoryWatch
 *
 * @date 12.03.2012
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
@Component(name = "de.calltopower.mhri.directorywatch.impl", immediate = false)
@Service(serviceFactory = true)
public class DirectoryWatchImpl implements DirectoryWatch {

    private static final Logger logger = Logger.getLogger(DirectoryWatchImpl.class);
    private WatchService g_watcher;
    private final Map<WatchKey, Path> g_keys;
    private boolean g_mainDirSet = false;
    private final ArrayList g_fileCreatedListeners;
    private final ArrayList g_fileModifiedListeners;
    private final ArrayList g_fileDeletedListeners;
    private Thread eventHandler = null;

    /**
     * Constructor
     *
     * @throws IOException
     */
    public DirectoryWatchImpl() throws IOException {
        g_watcher = FileSystems.getDefault().newWatchService();
        g_keys = new HashMap<>();
        g_fileCreatedListeners = new ArrayList();
        g_fileModifiedListeners = new ArrayList();
        g_fileDeletedListeners = new ArrayList();
    }

    protected void activate(ComponentContext cc) throws Exception {
    }

    protected void deactivate(ComponentContext cc) throws Exception {
        stopEventHandler();
    }

    private void stopEventHandler() {
        if (eventHandler != null) {
            eventHandler.interrupt();
        }
    }

    @Override
    public void reset() {
        if (logger.isInfoEnabled()) {
            logger.info("DirectoryWatchImpl::reset - Reset");
        }
        stopEventHandler();
        g_fileCreatedListeners.clear();
        g_fileModifiedListeners.clear();
        g_fileDeletedListeners.clear();
        g_keys.clear();
        g_mainDirSet = false;
        try {
            g_watcher.close();
            g_watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException ex) {
            logger.error("DirectoryWatchImpl::reset: " + ex.getMessage());
        }
    }

    /**
     * Registers a directory at the watch service
     *
     * @param path directory to register
     * @throws IOException
     */
    private void registerWithWatchService(Path path) throws IOException {
        if (logger.isInfoEnabled()) {
            logger.info("DirectoryWatchImpl::registerWithWatchService");
        }
        WatchKey key = path.register(g_watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        Path p = g_keys.get(key);
        if (p == null) {
            if (logger.isInfoEnabled()) {
                logger.info("DirectoryWatchImpl::registerWithWatchService - Registered '" + path + "'");
            }
        } else {
            if (!path.equals(p)) {
                if (logger.isInfoEnabled()) {
                    logger.info("DirectoryWatchImpl::registerWithWatchService - Updated '" + p + "' to '" + path + "'\n");
                }
            }
        }
        g_keys.put(key, path);
    }

    /**
     * Event 'create' -- thrown when a directory or file has been created
     * (added)
     *
     * @param child Path
     */
    private void eventCreateCaught(Path child) {
        try {
            addDirectory(child.toString());
        } catch (IOException e) {
            logger.error("DirectoryWatchImpl::eventCreateCaught - Failed to add new file or directory to watch list." + e.getMessage());
        }
        fireFileCreatedEvent(child.toString());
    }

    /**
     * Event 'create' -- thrown when a directory or file has been modified
     * ATTENTION: Thrown when a file or directory has been modified _and_ thrown
     * when the super-directory (and therefore all its containing files) has
     * been deleted, means when the directory or file inside a deleted
     * super-directory has been deleted as well
     *
     * @param child Path
     */
    private void eventModifyCaught(Path child) {
        fireFileModifiedEvent(child.toString());
    }

    /**
     * Event 'create' -- thrown when a directory or file has been deleted
     * ATTENTION: Only thrown when it is the folder or file that has actually
     * been deleted, _not_ thrown for files or directories inside of the
     * actually deleted directory
     *
     * @param child Path
     */
    private void eventDeleteCaught(Path child) {
        fireFileDeletedEvent(child.toString());
    }

    /**
     * Called whenever created event listener should be notified
     */
    private synchronized void fireFileCreatedEvent(String file) {
        FileCreatedEvent event = new FileCreatedEvent(this);
        for (Object o : g_fileCreatedListeners) {
            if (o instanceof FileCreatedEventListener) {
                ((FileCreatedEventListener) o).fileCreatedEventOccurred(event, file);
            }
        }
    }

    /**
     * Called whenever modified event listener should be notified
     */
    private synchronized void fireFileModifiedEvent(String file) {
        FileModifiedEvent event = new FileModifiedEvent(this);
        for (Object o : g_fileModifiedListeners) {
            if (o instanceof FileModifiedEventListener) {
                ((FileModifiedEventListener) o).fileModifiedEventOccurred(event, file);
            }
        }
    }

    /**
     * Called whenever deleted event listener should be notified
     */
    private synchronized void fireFileDeletedEvent(String file) {
        FileDeletedEvent event = new FileDeletedEvent(this);
        for (Object o : g_fileDeletedListeners) {
            if (o instanceof FileDeletedEventListener) {
                ((FileDeletedEventListener) o).fileDeletedEventOccurred(event, file);
            }
        }
    }

    private class eventDispatcher implements Runnable {

        @Override
        public void run() {
            try {
                for (;;) {
                    WatchKey key;
                    key = g_watcher.take();

                    Path dir = g_keys.get(key);
                    // WatchKey not available
                    if (dir == null) {
                        continue;
                    }

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind kind = event.kind();
                        if (kind == OVERFLOW) {
                            continue;
                        }
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path name = ev.context();
                        Path child = dir.resolve(name);

                        if (event.kind().equals(ENTRY_CREATE)) {
                            eventCreateCaught(child);
                        } else if (event.kind().equals(ENTRY_MODIFY)) {
                            eventModifyCaught(child);
                        } else if (event.kind().equals(ENTRY_DELETE)) {
                            eventDeleteCaught(child);
                        }
                    }

                    // reset the key, resume waiting for events
                    boolean valid = key.reset();
                    if (!valid) {
                        g_keys.remove(key);
                        if (g_keys.isEmpty()) {
                            // main directory has been deleted
                            g_mainDirSet = false;
                            logger.error("DirectoryWatchImpl::eventDispatcher - Main directory was deleted!");
                            // throw new MainDirectoryDeletedException();
                        }
                    }
                }
            } catch (InterruptedException e) {
                // logger.error("DirectoryWatchImpl::eventDispatcher - Event dispatcher was interrupted");
            }
        }
    }

    // getter
    @Override
    public LinkedList<String> getWatchedDirectories() {
        LinkedList<String> wd = new LinkedList<>();
        for (Map.Entry pairs : g_keys.entrySet()) {
            wd.add(pairs.getValue().toString());
        }
        return wd;
    }

    // setter
    @Override
    public boolean setMainDirectory(String mainDir) throws IOException {
        if (!g_mainDirSet && addDirectory(mainDir)) {
            stopEventHandler();
            eventHandler = new Thread(new eventDispatcher());
            eventHandler.start();
            g_mainDirSet = true;
        }
        return g_mainDirSet;
    }

    // misc
    /**
     * Adds a directory to watch
     *
     * @param path adds a directory to watch
     * @return true if directory has been added to watch list, false else
     * @throws IOException
     */
    @Override
    public boolean addDirectory(String path) throws IOException {
        if (MHRIFileUtils.getInstance().isDirectory(path)) {
            Path p = Paths.get(path);
            registerWithWatchService(p);
            return true;
        }
        return false;
    }

    @Override
    public synchronized void addFileCreatedEventListener(FileCreatedEventListener listener) {
        g_fileCreatedListeners.add(listener);
    }

    @Override
    public void addFileModifiedEventListener(FileModifiedEventListener listener) {
        g_fileModifiedListeners.add(listener);
    }

    @Override
    public void addFileDeletedEventListener(FileDeletedEventListener listener) {
        g_fileDeletedListeners.add(listener);
    }

    @Override
    public void removeFileCreatedEventListener(FileCreatedEventListener listener) {
        g_fileCreatedListeners.remove(listener);
    }

    @Override
    public void removeFileModifiedEventListener(FileModifiedEventListener listener) {
        g_fileModifiedListeners.remove(listener);
    }

    @Override
    public void removeFileDeletedEventListener(FileDeletedEventListener listener) {
        g_fileDeletedListeners.remove(listener);
    }
}
