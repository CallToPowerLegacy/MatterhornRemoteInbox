/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.gui;

import java.awt.AWTException;
import java.awt.Component;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.apache.log4j.Logger;
import de.calltopower.mhri.application.api.RemoteInboxApplication;
import de.calltopower.mhri.application.impl.RemoteInboxApplicationImpl;
import de.calltopower.mhri.ingestclient.api.IngestClientController;
import de.calltopower.mhri.logsender.LogSender;
import de.calltopower.mhri.util.Constants;
import de.calltopower.mhri.util.conf.Configuration;

/**
 * DesktopUI
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
class DesktopUI {

    private static final Logger logger = Logger.getLogger(DesktopUI.class);
    private final ScheduledExecutorService executor;
    private MenuItem About;
    private MenuItem UpdateUIItem;
    private MenuItem showUIItem;
    private MenuItem showPrefsItem;
    private MenuItem showLogSenderItem;
    private MenuItem showQuitItem;
    protected RemoteInboxApplication application;
    private TrayIcon trayIcon;
    private final Image trayIconImage;
    private InboxOverview inboxOverview;
    private Preferences preferences = null;
    private About inboxAbout = null;
    private Update update = null;
    private UpdateCheck updateCheck = null;
    private LogSenderUI logSender = null;
    private boolean checkingForUpdate = false;
    protected static Map<UIIcon, ImageIcon> icons = null;
    private boolean startAsService = true;
    private final int networkStatusCheckInitial = 15; // s
    private final int networkStatusCheck = 30; // s
    private final int versionCheckInitial = 15; // s
    private final int versionCheck = 60 * 60 * 6; // 6 hours in s
    private final int buttonCheckInitial = 1500; // ms
    private final int buttonCheck = 500; // ms

    public static enum UIIcon {

        MATTERHORN("matterhorn-icon.png"),
        OPEN_IN("open_in.png"),
        INBOX_INFO("inbox_info.png"),
        INBOX_CREATE("inbox_add.png"),
        INBOX_DELETE("inbox_delete.png"),
        SERIES_EDITOR("series_editor.png"),
        EPISODE_EDITOR("episode_editor.png"),
        FLAVOR_EDITOR("flavor_editor.png"),
        SERIES("series_list.png"),
        WORKFLOW("workflow_list.png"),
        PAUSE("ingest_pause.png"),
        STOP("ingest_stop.png"),
        DELETE("ingest_delete.png"),
        IDLE("ingest_idle.png"),
        RECIEVING("state_recieving.png"),
        SCHEDULED("ingest_schedule.png"),
        SCHEDULED_TRIM("ingest_schedule_trim.png"),
        SCENE_DETECTION("scene_detection.png"),
        UPLOADING_TRIM("state_upload_trim.png"),
        UPLOADING("state_upload.png"),
        COMPLETE("state_complete.png"),
        ERROR("state_failed.png"),
        NETWORK_WHITE("network_status_white.png"),
        NETWORK_RED("network_status_red.png"),
        NETWORK_YELLOW("network_status_yellow.png"),
        NETWORK_GREEN("network_status_green.png");
        private final String filename;

        UIIcon(String filename) {
            this.filename = filename;
        }

        public String getFilename() {
            return filename;
        }
    };

    public DesktopUI(RemoteInboxApplication application, boolean startAsService) throws Exception {
        if (application == null) {
            logger.error("DesktopUI::DesktopUI - Exception: RemoteInboxApplication is null");
            throw new Exception("DesktopUI::DesktopUI - Exception: RemoteInboxApplication is null");
        }
        this.startAsService = startAsService;
        this.application = application;
        trayIconImage = ImageIO.read(getClass().getResourceAsStream("/ui/matterhorn-icon.png"));
        if (icons == null) {
            loadIcons();
        }

        final Configuration config = this.application.getConfig();

        executor = Executors.newScheduledThreadPool(3);
        Runnable versionWatcher = new Runnable() {
            @Override
            public void run() {
                try {
                    boolean checkForUpdates = Boolean.parseBoolean(config.get(Constants.PROPKEY_CHECK_FOR_UPDATES));
                    if (checkForUpdates) {
                        checkVersion(false);
                    }
                } catch (Exception ex) {
                    // if (logger.isInfoEnabled()) {
                    // logger.info("DesktopUI::versionWatcher: " + ex.getMessage());
                    // }
                }
            }
        };
        Runnable networkStatusWatcher = new Runnable() {
            @Override
            public void run() {
                try {
                    checkNetworkStatus();
                } catch (Exception ex) {
                    // if (logger.isInfoEnabled()) {
                    // logger.info("DesktopUI::networkStatusWatcher: " + ex.getMessage());
                    // }
                }
            }
        };
        Runnable buttonWatcher = new Runnable() {
            @Override
            public void run() {
                try {
                    checkButtons();
                } catch (Exception ex) {
                    // if (logger.isInfoEnabled()) {
                    // logger.info("DesktopUI::buttonWatcher: " + ex.getMessage());
                    // }
                }
            }
        };
        executor.scheduleAtFixedRate(versionWatcher, versionCheckInitial, versionCheck, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(networkStatusWatcher, networkStatusCheckInitial, networkStatusCheck, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(buttonWatcher, buttonCheckInitial, buttonCheck, TimeUnit.MILLISECONDS);
    }

    private int checkVersion(boolean displayUI) {
        if (logger.isInfoEnabled()) {
            logger.info("DesktopUI::DesktopUI - Checking version (displaying GUI: " + displayUI + ")");
        }
        if (!checkingForUpdate) {
            if (displayUI) {
                updateCheck = new UpdateCheck();
                updateCheck.setIconImage(trayIconImage);
                updateCheck.setVisible(true);
            }
            checkingForUpdate = true;
            this.UpdateUIItem.setEnabled(false);
            this.UpdateUIItem.setLabel(Constants.getInstance().getLocalizedString("CheckingForUpdates"));
            int ret = 0;
            try {
                String currVersion = ((RemoteInboxApplicationImpl) application).getCurrentVersion().trim();
                String currAvailVersion = ((RemoteInboxApplicationImpl) application).getCurrentlyAvailableVersion().trim();
                if (logger.isInfoEnabled()) {
                    logger.info("DesktopUI::DesktopUI - Current version: " + currVersion);
                    logger.info("DesktopUI::DesktopUI - Current available version: " + currAvailVersion);
                }
                if (currAvailVersion.equals("unknown")) {
                    logger.error("DesktopUI::DesktopUI - Could not reach the update server");
                    ret = -10;
                } else if (!currVersion.equals(currAvailVersion)) {
                    if(updateCheck != null) {
                        updateCheck.setVisible(false);
                    }
                    if (logger.isInfoEnabled()) {
                        logger.info("DesktopUI::DesktopUI - Newer version is available");
                    }
                    if (update == null) {
                        update = new Update();
                        update.setIconImage(trayIconImage);
                    }
                    update.setNewVersion(currAvailVersion);
                    update.setVisible(true);
                    ret = 1;
                } else {
                    if (logger.isInfoEnabled()) {
                        logger.info("DesktopUI::DesktopUI - No newer version is available");
                    }
                }
            } catch (Exception ex) {
            } finally {
                if (updateCheck != null) {
                    updateCheck.dispose();
                    updateCheck = null;
                }
                this.UpdateUIItem.setLabel(Constants.getInstance().getLocalizedString("CheckForUpdates"));
                UpdateUIItem.setEnabled(true);
                checkingForUpdate = false;
            }
            return ret;
        }
        return -1;
    }

    private void checkButtons() {
        inboxOverview.checkButtons();
    }

    private void loadIcons() {
        if (logger.isInfoEnabled()) {
            logger.info("DesktopUI::loadIcons - Loading icons");
        }
        icons = new HashMap<>();
        for (UIIcon icon : UIIcon.values()) {
            try {
                ImageIcon image = new ImageIcon(ImageIO.read(getClass().getResourceAsStream("/ui/" + icon.getFilename())));
                icons.put(icon, image);
            } catch (IOException e) {
                logger.error("DesktopUI::loadIcons - could not load " + icon.getFilename());
            }
        }
    }

    protected void checkNetworkStatus() {
        if (logger.isInfoEnabled()) {
            logger.info("DesktopUI::checkNetworkStatus - Checking network status");
        }
        IngestClientController.NetworkConnectionState state = ((RemoteInboxApplicationImpl) application).getNetworkConnectionState();
        switch (state) {
            case CLIENT_OFFLINE:
                inboxOverview.setNetworkStatus(2, Constants.getInstance().getLocalizedString("NetworkStatusOffline1"));
                break;
            case SERVER_OFFLINE:
                inboxOverview.setNetworkStatus(2, Constants.getInstance().getLocalizedString("NetworkStatusOffline2"));
                break;
            case WRONG_CREDENTIALS:
                inboxOverview.setNetworkStatus(1, Constants.getInstance().getLocalizedString("NetworkStatusOnline1"));
                break;
            case ONLINE:
                inboxOverview.setNetworkStatus(3, Constants.getInstance().getLocalizedString("NetworkStatusOnline2"));
                break;
            case ERROR:
                inboxOverview.setNetworkStatus(0, Constants.getInstance().getLocalizedString("NetworkStatusError"));
                break;
        }
    }

    protected void sendLogFile(final boolean autogenerated, final String manualDescription) {
        if (logger.isInfoEnabled()) {
            logger.info("DesktopUI::sendLogFile - Sending log file");
        }
        showLogSenderWindow();
        logSender.setUploading(true);
        final Configuration conf = this.application.getConfig();
        About a = new About(application.getConfig().get(Constants.PROPKEY_MHRI_KEY_FILE));
        a.loadValues();
        final String key = a.getKey();
        new Thread(new Runnable() {
            @Override
            public void run() {
                LogSender l = new LogSender(conf);
                if (logger.isInfoEnabled()) {
                    logger.info("DesktopUI::sendLogFile - Starting sending log file");
                }
                String text = "";
                String title = "";
                int type = 0;
                boolean sent = false;
                try {
                    logger.error("DesktopUI::sendLogFile - Sending log file"); // this HAS TO BE error, so leave it alone!
                    boolean ret = l.sendLog(
                            key,
                            Constants.MHRI,
                            Constants.MHRI_VERSION,
                            Constants.MHRI_BUILD,
                            2,
                            autogenerated,
                            manualDescription,
                            Constants.LOG_FILE_PATH);
                    if (ret) {
                        if (logger.isInfoEnabled()) {
                            logger.info("DesktopUI::sendLogFile - Successfully send log file");
                        }
                        sent = true;
                        logSender.setUploading(false);
                        text = Constants.getInstance().getLocalizedString("LogFileSent_msg");
                        title = Constants.getInstance().getLocalizedString("LogFileSent");
                        type = JOptionPane.INFORMATION_MESSAGE;
                    } else {
                        logger.error("DesktopUI::sendLogFile - Could not send log file");
                        logSender.setUploading(false);
                        text = Constants.getInstance().getLocalizedString("LogFileNotSent_msg");
                        title = Constants.getInstance().getLocalizedString("LogFileNotSent");
                        type = JOptionPane.ERROR_MESSAGE;
                    }
                } catch (FileNotFoundException ex) {
                    logger.error("DesktopUI::sendLogFile - FileNotFoundException: " + ex.getMessage());
                    logSender.setUploading(false);
                    text = Constants.getInstance().getLocalizedString("LogFileNotFound_msg");
                    title = Constants.getInstance().getLocalizedString("LogFileNotFound");
                    type = JOptionPane.ERROR_MESSAGE;
                } catch (IOException ex) {
                    logger.error("DesktopUI::sendLogFile - IOException: " + ex.getMessage());
                    logSender.setUploading(false);
                    text = Constants.getInstance().getLocalizedString("LogFileNotUploaded_msg");
                    title = Constants.getInstance().getLocalizedString("LogFileNotUploaded");
                    type = JOptionPane.ERROR_MESSAGE;
                } catch (Exception ex) {
                    logger.error("DesktopUI::sendLogFile - Exception: " + ex.getMessage());
                    logSender.setUploading(false);
                    text = Constants.getInstance().getLocalizedString("LogFileNotUploaded_msg");
                    title = Constants.getInstance().getLocalizedString("LogFileNotUploaded");
                    type = JOptionPane.ERROR_MESSAGE;
                } finally {
                    logSender.setUploading(false);
                    JOptionPane.showMessageDialog(logSender,
                            text,
                            title,
                            type);
                    if (sent && (logSender != null)) {
                        logSender.dispose();
                        logSender = null;
                    }
                }
            }
        }).start();
    }

    protected void showErrorMessage(Component c, String title, String text) {
        JOptionPane.showMessageDialog(c,
                text,
                title,
                JOptionPane.ERROR_MESSAGE);
    }

    protected void init() throws AWTException {
        // set operating system look-and-feel
        try {
            if (logger.isInfoEnabled()) {
                logger.info("DesktopUI::init - Setting system look and feel");
            }
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            logger.error("DesktopUI::init - Could not set operating system look-and-feel: " + ex.getMessage());
        }

        // initialize tray icon
        if (SystemTray.isSupported()) {
            if (logger.isInfoEnabled()) {
                logger.info("DesktopUI::init - System try is supported");
                logger.info("DesktopUI::init - Building the menu");
            }
            // Build menu
            About = new MenuItem(Constants.getInstance().getLocalizedString("About"));
            About.addActionListener(showAboutWindowListener);
            UpdateUIItem = new MenuItem(Constants.getInstance().getLocalizedString("CheckForUpdates"));
            UpdateUIItem.addActionListener(showUpdateWindowListener);
            showUIItem = new MenuItem(Constants.getInstance().getLocalizedString("Inboxes"));
            showUIItem.addActionListener(toggleMainWindowListener);
            showPrefsItem = new MenuItem(Constants.getInstance().getLocalizedString("Preferences"));
            showPrefsItem.addActionListener(prefsApplicationListener);
            showLogSenderItem = new MenuItem(Constants.getInstance().getLocalizedString("SendLogFile"));
            showLogSenderItem.addActionListener(logSenderApplicationListener);
            showQuitItem = new MenuItem(Constants.getInstance().getLocalizedString("Quit"));
            showQuitItem.addActionListener(quitApplicationListener);

            PopupMenu mainMenu = new PopupMenu();
            mainMenu.add(About);
            mainMenu.addSeparator();
            mainMenu.add(showLogSenderItem);
            mainMenu.add(UpdateUIItem);
            mainMenu.addSeparator();
            mainMenu.add(showUIItem);
            mainMenu.add(showPrefsItem);
            mainMenu.addSeparator();
            mainMenu.add(showQuitItem);

            try {
                if (logger.isInfoEnabled()) {
                    logger.info("DesktopUI::init - Adding the tray icon");
                }
                trayIcon = new TrayIcon(trayIconImage, Constants.getInstance().getLocalizedString("MatterhornRemoteInbox"), mainMenu);
                trayIcon.setImageAutoSize(true);
                trayIcon.addMouseListener(toggleMainWindowMouseListener);
                SystemTray tray = SystemTray.getSystemTray();
                tray.add(trayIcon);
            } catch (Exception ex) {
            }

            initInboxOverview();

            if (Boolean.parseBoolean(application.getConfig().get(Constants.PROPKEY_FIRSTSTART))) {
                if (logger.isInfoEnabled()) {
                    logger.info("DesktopUI::init - First start! Showing a welcome message");
                }
                JOptionPane.showMessageDialog(null,
                        Constants.getInstance().getLocalizedString("MessageWelcome_msg"),
                        Constants.getInstance().getLocalizedString("MessageWelcome"),
                        JOptionPane.INFORMATION_MESSAGE);

                if (preferences == null) {
                    showPreferences();
                }

                application.getConfig().set(Constants.PROPKEY_FIRSTSTART, String.valueOf(false));
                try {
                    application.getConfig().store(de.calltopower.mhri.util.Constants.CONFIG_PATH);
                } catch (Exception ex) {
                }
            }
        } else {
            logger.error("DesktopUI::init - Could not initialize tray icon. Not supported for this operating system.");
            JOptionPane.showMessageDialog(null,
                    Constants.getInstance().getLocalizedString("ErrorTrayNotSupported_msg"),
                    Constants.getInstance().getLocalizedString("ErrorTrayNotSupported"),
                    JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    public void deinit() {
        if (logger.isInfoEnabled()) {
            logger.info("DesktopUI::init - Removing the tray icon");
        }
        SystemTray.getSystemTray().remove(trayIcon);
    }

    private void initInboxOverview() {
        inboxOverview = new InboxOverview((RemoteInboxApplicationImpl) application);
        inboxOverview.setIconImage(trayIconImage);
        if (!startAsService) {
            inboxOverview.setVisible(true);
            trayIcon.removeMouseListener(toggleMainWindowMouseListener);
            inboxOverview.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    deinit();
                    System.exit(0);
                }
            });
        }
    }

    private void toggleUpdateWindow() {
        if (!checkingForUpdate) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    int versCheck = checkVersion(true);
                    if (versCheck == 0) {
                        JOptionPane.showMessageDialog(null,
                                Constants.getInstance().getLocalizedString("VersionUpToDate_msg"),
                                Constants.getInstance().getLocalizedString("VersionUpToDate"),
                                JOptionPane.INFORMATION_MESSAGE);
                    } else if (versCheck == -10) {
                        JOptionPane.showMessageDialog(null,
                                Constants.getInstance().getLocalizedString("NoConnectionToUpdateServer_msg"),
                                Constants.getInstance().getLocalizedString("NoConnectionToUpdateServer"),
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
        }
    }

    private void toggleMainWindow() {
        if (inboxOverview == null) {
            initInboxOverview();
        }
        inboxOverview.setVisible(!inboxOverview.isVisible());
    }

    private void showAboutWindow() {
        if ((inboxAbout != null) && (!inboxAbout.isVisible())) {
            inboxAbout.dispose();
            inboxAbout = null;
        }
        if (inboxAbout == null) {
            inboxAbout = new About(application.getConfig().get(Constants.PROPKEY_MHRI_KEY_FILE));
            inboxAbout.setIconImage(trayIconImage);
            inboxAbout.loadValues();
            inboxAbout.setVisible(true);
        }
    }

    private void showPreferences() {
        if ((preferences != null) && (!preferences.isVisible())) {
            preferences.dispose();
            preferences = null;
        }
        if (preferences == null) {
            preferences = new Preferences(this, application.getConfig());
            preferences.setIconImage(trayIconImage);
            preferences.loadValues();
            preferences.setVisible(true);
        }
    }

    private void showLogSenderWindow() {
        if ((logSender != null) && (!logSender.isVisible())) {
            logSender.dispose();
            logSender = null;
        }
        if (logSender == null) {
            logSender = new LogSenderUI(this);
            logSender.setIconImage(trayIconImage);
            logSender.loadValues();
            logSender.setVisible(true);
        }
    }
    private final ActionListener showUpdateWindowListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ae) {
            toggleUpdateWindow();
        }
    };
    private final ActionListener showAboutWindowListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ae) {
            showAboutWindow();
        }
    };
    private final ActionListener toggleMainWindowListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ae) {
            toggleMainWindow();
        }
    };
    private final MouseListener toggleMainWindowMouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                toggleMainWindow();
            }
        }
    };
    private final ActionListener prefsApplicationListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ae) {
            showPreferences();
        }
    };
    private final ActionListener logSenderApplicationListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ae) {
            showLogSenderWindow();
        }
    };
    private final ActionListener quitApplicationListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ae) {
            deinit();
            System.exit(0);
        }
    };
}
