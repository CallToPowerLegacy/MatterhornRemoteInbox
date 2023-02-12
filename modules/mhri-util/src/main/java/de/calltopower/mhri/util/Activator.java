/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import de.calltopower.mhri.util.conf.Configuration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Activator
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class Activator implements BundleActivator {

    private static final Logger logger = Logger.getLogger(Activator.class);
    private Configuration applicationConfig;

    @Override
    public void start(BundleContext bc) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info(Constants.MHRI + " version " + Constants.MHRI_VERSION + " build " + Constants.MHRI_BUILD);
            logger.info("Java information:");
            logger.info("\t" + "Version " + System.getProperty("java.version"));
            logger.info("\t" + "Vendor: " + System.getProperty("java.vendor"));
            logger.info("\t" + "Vendor URL: " + System.getProperty("java.vendor.url"));
            logger.info("\t" + "Class path: " + System.getProperty("java.class.path"));
            logger.info("\t" + "Home: " + System.getProperty("java.home"));
            logger.info("Operating system information:");
            logger.info("\t" + "Name: " + System.getProperty("os.name"));
            logger.info("\t" + "Arch: " + System.getProperty("os.arch"));
            logger.info("\t" + "Version: " + System.getProperty("os.version"));
            // logger.info("\t" + "File separator: " + System.getProperty("file.separator"));
            // logger.info("\t" + "Line separator: " + System.getProperty("line.separator"));
            // logger.info("\t" + "Path separator: " + System.getProperty("path.separator"));
            logger.info("User information:");
            logger.info("\t" + "Name: " + System.getProperty("user.name"));
            logger.info("\t" + "Language: " + System.getProperty("user.language") + " (" + Locale.getDefault() + ")");
            logger.info("\t" + "Directory: " + System.getProperty("user.dir"));
            logger.info("\t" + "Home: " + System.getProperty("user.home"));
        }

        Lock lock = new Lock(Constants.MHRI_LOCK_FILE_NAME);

        if (lock.otherInstanceIsRunning()) {
            logger.error("Activator::start - Another instance is running. Quitting...\n####################");
            Object[] options = {"Quit"};
            int n = JOptionPane.showOptionDialog(null,
                    "Another instance of this application is already running.\nPlease quit the other instance or end it via the task manager.",
                    "Another instance already running",
                    JOptionPane.YES_OPTION,
                    JOptionPane.ERROR_MESSAGE,
                    null,
                    options,
                    options[0]);
            if (n == JOptionPane.YES_OPTION) {
                // final BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
                if (bc != null) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Activator::start - Getting system bundle...");
                    }
                    Bundle b = bc.getBundle(0);
                    if (b != null) {
                        if (logger.isInfoEnabled()) {
                            logger.info("Activator::start - Stopping felix...");
                        }
                        b.stop();
                    } else {
                        logger.error("Activator::start - Could not stop felix...");
                    }
                } else {
                    logger.error("Activator::start - Could not get system bundle...");
                }
                // System.exit(0);
                Runtime.getRuntime().halt(0);
            }
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("Activator::start - Loading configuration");
            }
            // load configuration and register as service
            applicationConfig = new Configuration(loadConfiguration());
            Hashtable props = new Hashtable();
            props.put("description", "full application configuration");
            if (logger.isInfoEnabled()) {
                logger.info("Activator::start - Registering as a service");
            }
            bc.registerService(applicationConfig.getClass().getName(), applicationConfig, props);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }

    private Properties loadConfiguration() {
        InputStream is = null;
        Properties defaults = new Properties();

        // load defaults from bundle
        try {
            is = getClass().getResourceAsStream(Constants.DEFAULT_CONFIG_PATH);
            defaults.load(is);
            // printProperties(defaults);
        } catch (Exception e) {
            logger.error("Activator::loadConfiguration - Failed to load default configuration: " + e.getMessage());
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ex) {
                logger.error("Activator::loadConfiguration - Failed to close: " + ex.getMessage());
            }
        }

        boolean configUserLoaded = false;
        // load user config
        Properties configUser = new Properties(defaults);
        try {
            is = new FileInputStream(new File(Constants.CONFIG_PATH));
            configUser.load(is);
            configUserLoaded = true;
        } catch (IOException e) {
            logger.warn("Activator::loadConfiguration - user config file not found at '" + Constants.CONFIG_PATH + "', application defaults will be used!");
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ex) {
                logger.error("Activator::loadConfiguration - Failed to close: " + ex.getMessage());
            }
        }

        // print and return config
        if (configUserLoaded) {
            printProperties(configUser, "Changed config property");
            return configUser;
        }
        printProperties(defaults, "Default config property");
        return defaults;
    }

    private void printProperties(Properties props, String prefix) {
        for (Entry<Object, Object> it : props.entrySet()) {
            Entry entry = it;
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            String pref = ((prefix == null) || prefix.isEmpty()) ? "" : prefix + " - ";
            if (logger.isInfoEnabled()) {
                logger.info("Activator::printProperties - " + pref + key + " = " + value);
            }
        }
    }
}
