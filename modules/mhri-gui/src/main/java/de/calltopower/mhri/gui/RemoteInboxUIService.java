/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.gui;

import java.awt.AWTException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.log4j.Logger;
import de.calltopower.mhri.application.api.RemoteInboxApplication;
import de.calltopower.mhri.application.impl.RemoteInboxApplicationImpl;
import de.calltopower.mhri.util.Constants;
import de.calltopower.mhri.util.conf.Configuration;
import org.osgi.service.component.ComponentContext;

/**
 * RemoteInboxUIService
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
@Component(name = "de.calltopower.mhri.ui", immediate = true)
@Service()
public class RemoteInboxUIService implements RemoteInboxUI {

    private static final Logger logger = Logger.getLogger(RemoteInboxUIService.class);
    @Reference
    RemoteInboxApplication application;
    @Reference
    Configuration config;
    private DesktopUI userInterface;

    protected void activate(ComponentContext cc) throws Exception {
        boolean startAsService = Boolean.parseBoolean(config.get(Constants.PROPKEY_START_AS_SERVICE));
        try {
            if (logger.isInfoEnabled()) {
                logger.info("RemoteInboxUIService::activate - Activating");
                logger.info("RemoteInboxUIService::activate - Starting " + (startAsService ? "" : "not") + " as a service");
            }
            userInterface = new DesktopUI(application, startAsService);
            userInterface.init();
            if (((RemoteInboxApplicationImpl) application).databaseError) {
                logger.error("RemoteInboxUIService::activate - Database error, cannot initialize GUI");
                userInterface.showErrorMessage(
                        null,
                        Constants.getInstance().getLocalizedString("DatabaseError"),
                        Constants.getInstance().getLocalizedString("DatabaseError_msg"));
                ((RemoteInboxApplicationImpl) application).databaseError = false;
            }
        } catch (AWTException ex) {
            logger.error("RemoteInboxUIService::activate - AWTException: " + ex.getMessage());
        }
    }

    protected void deactivate(ComponentContext cc) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("RemoteInboxUIService::activate - Deactivating the GUI");
        }
        userInterface.deinit();
    }
}
