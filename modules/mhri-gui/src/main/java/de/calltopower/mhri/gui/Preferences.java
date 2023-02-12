/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.gui;

import java.awt.Component;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.Properties;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.NumberFormatter;
import org.apache.log4j.Logger;
import de.calltopower.mhri.application.api.Recording;
import de.calltopower.mhri.application.api.Recording.State;
import de.calltopower.mhri.application.api.RemoteInboxApplication;
import de.calltopower.mhri.application.impl.RemoteInboxApplicationImpl;
import de.calltopower.mhri.util.Constants;
import de.calltopower.mhri.util.conf.Configuration;

/**
 * Preferences
 *
 * @date 09.08.2012
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class Preferences extends javax.swing.JFrame {

    private static final Logger logger = Logger.getLogger(Preferences.class);
    private final MHRIFocusTraversalPolicy traversalPolicy;
    private final DesktopUI desktopUI;
    private final RemoteInboxApplication application;
    private final Configuration config;
    private final Properties initial_props;
    private String selectedDirectory = "PathToInbox";
    private boolean somethingChanged;
    private boolean checkBoxStartAsServiceChecked;
    private boolean restartRequired;
    private boolean warnOnlyForNewRecordings;
    private boolean inboxChanged;

    public Preferences(DesktopUI ui, Configuration config) {
        this.desktopUI = ui;
        this.application = (ui != null) ? ui.application : null;
        this.config = config;

        this.initial_props = this.config.getProperties();

        initComponents();

        this.setTitle(Constants.getInstance().getLocalizedString("Preferences"));
        this.button_ok.setText(Constants.getInstance().getLocalizedString("OK"));
        this.button_cancel.setText(Constants.getInstance().getLocalizedString("Cancel"));
        this.jTabbedPane1.setTitleAt(0, Constants.getInstance().getLocalizedString("General"));
        this.jTabbedPane1.setTitleAt(1, Constants.getInstance().getLocalizedString("Server"));
        this.jTabbedPane1.setTitleAt(2, Constants.getInstance().getLocalizedString("Upload"));
        this.jTabbedPane1.setTitleAt(3, Constants.getInstance().getLocalizedString("Inbox"));
        this.jTabbedPane1.setTitleAt(4, Constants.getInstance().getLocalizedString("Media"));
        this.checkbox_startAsService.setText(Constants.getInstance().getLocalizedString("StartAsService"));
        this.checkbox_checkForUpdates.setText(Constants.getInstance().getLocalizedString("CheckForUpdates"));
        this.checkbox_tooltips.setText(Constants.getInstance().getLocalizedString("ShowTooltips"));
        this.label_host.setText(Constants.getInstance().getLocalizedString("Host") + ":");
        this.label_port.setText(Constants.getInstance().getLocalizedString("Port") + ":");
        this.label_username.setText(Constants.getInstance().getLocalizedString("Username") + ":");
        this.label_password.setText(Constants.getInstance().getLocalizedString("Password") + ":");
        this.checkbox_chunkedUpload.setText(Constants.getInstance().getLocalizedString("ChunkedUpload"));
        this.checkbox_chunkedUploadFallback.setText(Constants.getInstance().getLocalizedString("ChunkedUploadFallback"));
        this.label_chunkSize.setText(Constants.getInstance().getLocalizedString("ChunkSize") + ":");
        this.label_maxConcurrentUploads.setText(Constants.getInstance().getLocalizedString("MaxConcurrentUploads") + ":");
        this.checkbox_retryIngestingFailedRecordings.setText(Constants.getInstance().getLocalizedString("RetryIngesting"));
        this.label_triesFailedRecordings.setText(Constants.getInstance().getLocalizedString("NrOfRetries") + ":");
        this.label_mainDir.setText(Constants.getInstance().getLocalizedString("MainDir") + ":");
        this.label_defInboxName.setText(Constants.getInstance().getLocalizedString("DefInboxName") + ":");
        this.button_select.setText(Constants.getInstance().getLocalizedString("Select"));
        this.label_defaultWorkflow.setText(Constants.getInstance().getLocalizedString("DefaultWorkflow") + ":");
        this.label_presentationSuffix.setText(Constants.getInstance().getLocalizedString("PresentationSuffix") + ":");
        this.label_presenterSuffix.setText(Constants.getInstance().getLocalizedString("PresenterSuffix") + ":");
        this.label_mediafileSuffixes.setText(Constants.getInstance().getLocalizedString("MediafileSuffixes") + ":");
        this.button_reset_textfield_defaultWorkflow.setText(Constants.getInstance().getLocalizedString("Reset"));
        this.button_reset_textfield_presentationSuffix.setText(Constants.getInstance().getLocalizedString("Reset"));
        this.button_reset_textfield_presenterSuffix.setText(Constants.getInstance().getLocalizedString("Reset"));
        this.button_reset_textfield_mediafileSuffixes.setText(Constants.getInstance().getLocalizedString("Reset"));
        this.button_import.setText(Constants.getInstance().getLocalizedString("ImportProperties"));
        this.button_export.setText(Constants.getInstance().getLocalizedString("SaveProperties"));

        LinkedList<Component> componentList = new LinkedList<>();
        componentList.add(checkbox_startAsService);
        componentList.add(checkbox_checkForUpdates);
        componentList.add(checkbox_tooltips);
        componentList.add(button_import);
        componentList.add(button_export);
        componentList.add(button_ok);
        componentList.add(button_cancel);
        componentList.add(textfield_host);
        componentList.add(spinner_port);
        componentList.add(textfield_username);
        componentList.add(textfield_password);
        componentList.add(button_ok);
        componentList.add(button_cancel);
        componentList.add(checkbox_chunkedUpload);
        componentList.add(checkbox_chunkedUploadFallback);
        componentList.add(spinner_chunkSize);
        componentList.add(spinner_maxConcurrentUploads);
        componentList.add(checkbox_retryIngestingFailedRecordings);
        componentList.add(spinner_triesFailedRecordings);
        componentList.add(button_ok);
        componentList.add(button_cancel);
        componentList.add(textfield_mainDir);
        componentList.add(button_select);
        componentList.add(textfield_defInboxName);
        componentList.add(button_ok);
        componentList.add(button_cancel);
        componentList.add(textfield_defaultWorkflow);
        componentList.add(button_reset_textfield_defaultWorkflow);
        componentList.add(textfield_presentationSuffix);
        componentList.add(button_reset_textfield_presentationSuffix);
        componentList.add(textfield_presenterSuffix);
        componentList.add(button_reset_textfield_presenterSuffix);
        componentList.add(textfield_mediafileSuffixes);
        componentList.add(button_reset_textfield_mediafileSuffixes);
        componentList.add(button_ok);
        componentList.add(button_cancel);
        traversalPolicy = new MHRIFocusTraversalPolicy(componentList);
        this.setFocusTraversalPolicy(traversalPolicy);

        this.setLocationRelativeTo(null);
        loadValues();
        this.button_ok.requestFocus();

        this.jPanel27.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancelPressed();
                }
            }
        });
        this.jTabbedPane1.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancelPressed();
                }
            }
        });
    }

    private void cancelPressed() {
        if (!somethingChanged || (somethingChanged && JOptionPane.showConfirmDialog(
                this,
                Constants.getInstance().getLocalizedString("UnsavedChangesDiscard_msg"),
                Constants.getInstance().getLocalizedString("UnsavedChangesDiscard"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)) {
            if (config != null) {
                config.setProperties(initial_props);
            }
            this.dispose();
        }
    }

    public final void loadValues() {
        restartRequired = false;
        inboxChanged = false;
        warnOnlyForNewRecordings = false;
        if (config != null) {
            // general settings
            checkbox_startAsService.setSelected(Boolean.parseBoolean(config.get(Constants.PROPKEY_START_AS_SERVICE)));
            checkbox_checkForUpdates.setSelected(Boolean.parseBoolean(config.get(Constants.PROPKEY_CHECK_FOR_UPDATES)));
            checkbox_tooltips.setSelected(Boolean.parseBoolean(config.get(Constants.PROPKEY_SHOW_TOOLTIPS)));
            checkbox_chunkedUpload.setSelected(Boolean.parseBoolean(config.get(Constants.PROPKEY_CHUNKED_UPLOAD)));
            checkbox_chunkedUploadFallback.setSelected(Boolean.parseBoolean(config.get(Constants.PROPKEY_CHUNKED_UPLOAD_FALLBACK)));
            checkbox_retryIngestingFailedRecordings.setSelected(Boolean.parseBoolean(config.get(Constants.PROPKEY_RETRY_INGESTING_FAILED_RECORDINGS)));
            spinner_chunkSize.setValue(Integer.valueOf(config.get(Constants.PROPKEY_CHUNKSIZE)));
            spinner_triesFailedRecordings.setValue(Integer.valueOf(config.get(Constants.PROPKEY_INGESTFAILEDRECORDINGSTRIES)));
            spinner_maxConcurrentUploads.setValue(Integer.valueOf(config.get(Constants.PROPKEY_MAXCONCURRENTUPLOADS)));
            // server settings
            textfield_host.setText(config.get(Constants.PROPKEY_HOST));
            spinner_port.setValue(Integer.valueOf(config.get(Constants.PROPKEY_PORT)));
            textfield_username.setText(config.get(Constants.PROPKEY_USERNAME));
            textfield_password.setText(config.get(Constants.PROPKEY_PASSWORD));
            // inbox settings
            textfield_mainDir.setText(config.get(Constants.PROPKEY_MAINDIR));
            textfield_defInboxName.setText(config.get(Constants.PROPKEY_DEFAULTINBOX));
            // media settings
            textfield_presentationSuffix.setText(config.get(Constants.PROPKEY_PRESENTATION_SUFFIX));
            textfield_presenterSuffix.setText(config.get(Constants.PROPKEY_PRESENTER_SUFFIX));
            textfield_mediafileSuffixes.setText(config.get(Constants.PROPKEY_MEDIAFILE_SUFFIXES));
            textfield_defaultWorkflow.setText(config.get(Constants.PROPKEY_DEFAULT_WORKFLOW));

            // misc variables
            somethingChanged = false;
            checkBoxStartAsServiceChecked = checkbox_startAsService.isSelected();
            if (!checkbox_chunkedUpload.isSelected()) {
                checkbox_chunkedUploadFallback.setEnabled(false);
                spinner_chunkSize.setEnabled(false);
            }
            if (!checkbox_retryIngestingFailedRecordings.isSelected()) {
                spinner_triesFailedRecordings.setEnabled(false);
            }
        } else {
            checkbox_startAsService.setEnabled(false);
            checkbox_checkForUpdates.setEnabled(false);
            checkbox_tooltips.setEnabled(false);
            checkbox_chunkedUpload.setEnabled(false);
            checkbox_chunkedUploadFallback.setEnabled(false);
            checkbox_retryIngestingFailedRecordings.setEnabled(false);
            spinner_chunkSize.setEnabled(false);
            spinner_triesFailedRecordings.setEnabled(false);
            spinner_maxConcurrentUploads.setEnabled(false);
            // server settings
            textfield_host.setEnabled(false);
            spinner_port.setEnabled(false);
            textfield_username.setEnabled(false);
            textfield_password.setEnabled(false);
            // inbox settings
            textfield_mainDir.setEnabled(false);
            textfield_defInboxName.setEnabled(false);
            // media settings
            textfield_presentationSuffix.setEnabled(false);
            textfield_presenterSuffix.setEnabled(false);
            textfield_mediafileSuffixes.setEnabled(false);
            textfield_defaultWorkflow.setEnabled(false);

            jPanel28.setEnabled(false);
            jPanel29.setEnabled(false);
            jPanel30.setEnabled(false);
            jPanel31.setEnabled(false);
            jPanel32.setEnabled(false);
            jTabbedPane1.setEnabled(false);
            button_ok.setEnabled(false);
        }
    }

    private boolean loadPropertyFile(String file) {
        InputStream is = null;
        if (config != null) {
            Properties props = new Properties(config.getProperties());

            try {
                is = new FileInputStream(new File(file));
                props.load(is);
                config.setProperties(props);
                return true;
            } catch (IOException e) {
                logger.error("IOException: loadPropertyFile(" + file + "): " + e.getMessage());
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    logger.error("IOException: loadPropertyFile(" + file + "): " + e.getMessage());
                }
            }
        }

        return false;
    }

    private void importPreferences() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(false);
        fc.setCurrentDirectory(new File("."));
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("MHRI property file", "properties");
        fc.setFileFilter(filter);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String fileStr = "";
            try {
                fileStr = fc.getSelectedFile().getAbsolutePath();

                if (loadPropertyFile(fileStr)) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Preferences::importPreferences - Successfully imported the property file from: " + fileStr);
                    }
                    loadValues();
                    somethingChanged = true;
                    restartRequired = true;
                    inboxChanged = true;
                    JOptionPane.showMessageDialog(
                            this,
                            Constants.getInstance().getLocalizedString("PropertiesImported_msg"),
                            Constants.getInstance().getLocalizedString("PropertiesImported"),
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(
                            this,
                            Constants.getInstance().getLocalizedString("PropertiesImportFailed_msg"),
                            Constants.getInstance().getLocalizedString("PropertiesImportFailed"),
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                logger.error("Preferences::importPreferences - Exception: " + ex.getMessage());
                JOptionPane.showMessageDialog(
                        this,
                        Constants.getInstance().getLocalizedString("PropertiesImportFailed_msg"),
                        Constants.getInstance().getLocalizedString("PropertiesImportFailed"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportPreferences() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(false);
        fc.setCurrentDirectory(new File("."));
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = "";
            try {
                path = fc.getSelectedFile().getAbsolutePath();
                path = path.endsWith(File.separator) ? path : (path + File.separator);
                path += "mhri.properties";
                File to = new File(path).getAbsoluteFile();

                boolean write = false;
                if (!to.exists() && to.createNewFile() && to.canWrite()) {
                    write = true;
                } else if (to.exists() && to.canWrite()) {
                    int result = JOptionPane.showConfirmDialog(
                            this,
                            Constants.getInstance().getLocalizedString("PropertiesFileFound_msg"),
                            Constants.getInstance().getLocalizedString("PropertiesFileFound"),
                            JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.YES_OPTION) {
                        write = true;
                    }
                }
                if (write) {
                    if (config.store(path)) {
                        if (logger.isInfoEnabled()) {
                            logger.info("Preferences::export - Successfully saved properties file to" + path);
                        }
                        JOptionPane.showMessageDialog(
                                this,
                                Constants.getInstance().getLocalizedString("PropertiesSave_msg"),
                                Constants.getInstance().getLocalizedString("PropertiesSave"),
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(
                                this,
                                Constants.getInstance().getLocalizedString("PropertiesSaveFailed_msg"),
                                Constants.getInstance().getLocalizedString("PropertiesSaveFailed"),
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (IOException ex) {
                logger.error("Preferences::export - IOException: " + ex.getMessage());
                JOptionPane.showMessageDialog(
                        this,
                        Constants.getInstance().getLocalizedString("PropertiesSaveFailed_msg"),
                        Constants.getInstance().getLocalizedString("PropertiesSaveFailed"),
                        JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                logger.error("Preferences::export - Exception: " + ex.getMessage());
                JOptionPane.showMessageDialog(
                        this,
                        Constants.getInstance().getLocalizedString("PropertiesSaveFailed_msg"),
                        Constants.getInstance().getLocalizedString("PropertiesSaveFailed"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel27 = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel28 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        checkbox_startAsService = new javax.swing.JCheckBox();
        checkbox_checkForUpdates = new javax.swing.JCheckBox();
        checkbox_tooltips = new javax.swing.JCheckBox();
        jSeparator3 = new javax.swing.JSeparator();
        panel_importExport = new javax.swing.JPanel();
        button_import = new javax.swing.JButton();
        button_export = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JSeparator();
        jPanel30 = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        label_host = new javax.swing.JLabel();
        textfield_host = new javax.swing.JTextField();
        label_port = new javax.swing.JLabel();
        spinner_port = new javax.swing.JSpinner();
        label_username = new javax.swing.JLabel();
        textfield_username = new javax.swing.JTextField();
        label_password = new javax.swing.JLabel();
        textfield_password = new javax.swing.JPasswordField();
        jPanel29 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        checkbox_chunkedUpload = new javax.swing.JCheckBox();
        checkbox_chunkedUploadFallback = new javax.swing.JCheckBox();
        jSeparator2 = new javax.swing.JSeparator();
        label_chunkSize = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        spinner_chunkSize = new javax.swing.JSpinner();
        label_mb = new javax.swing.JLabel();
        label_maxConcurrentUploads = new javax.swing.JLabel();
        spinner_maxConcurrentUploads = new javax.swing.JSpinner();
        jSeparator1 = new javax.swing.JSeparator();
        checkbox_retryIngestingFailedRecordings = new javax.swing.JCheckBox();
        label_triesFailedRecordings = new javax.swing.JLabel();
        spinner_triesFailedRecordings = new javax.swing.JSpinner();
        jPanel31 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        label_mainDir = new javax.swing.JLabel();
        textfield_mainDir = new javax.swing.JTextField();
        button_select = new javax.swing.JButton();
        label_defInboxName = new javax.swing.JLabel();
        textfield_defInboxName = new javax.swing.JTextField();
        jPanel32 = new javax.swing.JPanel();
        jPanel16 = new javax.swing.JPanel();
        label_defaultWorkflow = new javax.swing.JLabel();
        textfield_defaultWorkflow = new javax.swing.JTextField();
        button_reset_textfield_defaultWorkflow = new javax.swing.JButton();
        label_presentationSuffix = new javax.swing.JLabel();
        textfield_presentationSuffix = new javax.swing.JTextField();
        button_reset_textfield_presentationSuffix = new javax.swing.JButton();
        label_presenterSuffix = new javax.swing.JLabel();
        textfield_presenterSuffix = new javax.swing.JTextField();
        button_reset_textfield_presenterSuffix = new javax.swing.JButton();
        label_mediafileSuffixes = new javax.swing.JLabel();
        textfield_mediafileSuffixes = new javax.swing.JTextField();
        button_reset_textfield_mediafileSuffixes = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        button_ok = new javax.swing.JButton();
        button_cancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Preferences");
        setAlwaysOnTop(true);
        setMinimumSize(new java.awt.Dimension(599, 299));
        setResizable(false);
        getContentPane().setLayout(new java.awt.GridBagLayout());

        jPanel27.setLayout(new java.awt.GridBagLayout());

        jTabbedPane1.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));

        jPanel28.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel28.setLayout(new java.awt.GridBagLayout());

        jPanel3.setLayout(new java.awt.GridBagLayout());

        checkbox_startAsService.setSelected(true);
        checkbox_startAsService.setText("Start as a service");
        checkbox_startAsService.setName(""); // NOI18N
        checkbox_startAsService.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkbox_startAsServiceActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel3.add(checkbox_startAsService, gridBagConstraints);

        checkbox_checkForUpdates.setSelected(true);
        checkbox_checkForUpdates.setText("Check for updates");
        checkbox_checkForUpdates.setName(""); // NOI18N
        checkbox_checkForUpdates.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkbox_checkForUpdatesActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel3.add(checkbox_checkForUpdates, gridBagConstraints);

        checkbox_tooltips.setSelected(true);
        checkbox_tooltips.setText("Show tooltips");
        checkbox_tooltips.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkbox_tooltipsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel3.add(checkbox_tooltips, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        jPanel3.add(jSeparator3, gridBagConstraints);

        panel_importExport.setLayout(new java.awt.GridBagLayout());

        button_import.setText("Import preferences");
        button_import.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                button_importMouseClicked(evt);
            }
        });
        button_import.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                button_importKeyReleased(evt);
            }
        });
        panel_importExport.add(button_import, new java.awt.GridBagConstraints());

        button_export.setText("Export preferences");
        button_export.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                button_exportMouseClicked(evt);
            }
        });
        button_export.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                button_exportKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        panel_importExport.add(button_export, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel3.add(panel_importExport, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        jPanel3.add(jSeparator4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel28.add(jPanel3, gridBagConstraints);

        jTabbedPane1.addTab("General", jPanel28);

        jPanel30.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel30.setLayout(new java.awt.GridBagLayout());

        jPanel11.setLayout(new java.awt.GridBagLayout());

        label_host.setText("Host:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel11.add(label_host, gridBagConstraints);

        textfield_host.setText("YOUR.SERVER.TLD");
        textfield_host.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                textfield_hostKeyPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel11.add(textfield_host, gridBagConstraints);

        label_port.setText("Port:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel11.add(label_port, gridBagConstraints);

        spinner_port.setModel(new javax.swing.SpinnerNumberModel(0, 0, 65535, 1));
        JFormattedTextField txt = ((JSpinner.NumberEditor) spinner_port.getEditor()).getTextField();
        NumberFormatter f = (NumberFormatter) txt.getFormatter();
        DecimalFormat d = new DecimalFormat("###");
        d.setDecimalSeparatorAlwaysShown(false);
        f.setFormat(d);
        f.setAllowsInvalid(false);
        spinner_port.setValue(8080);
        spinner_port.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinner_portStateChanged(evt);
            }
        });
        spinner_port.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                spinner_portKeyPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel11.add(spinner_port, gridBagConstraints);

        label_username.setText("Username:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel11.add(label_username, gridBagConstraints);

        textfield_username.setText("admin");
        textfield_username.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                textfield_usernameKeyPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel11.add(textfield_username, gridBagConstraints);

        label_password.setText("Password:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel11.add(label_password, gridBagConstraints);

        textfield_password.setText("opencast");
        textfield_password.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                textfield_passwordKeyPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel11.add(textfield_password, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel30.add(jPanel11, gridBagConstraints);

        jTabbedPane1.addTab("Server", jPanel30);

        jPanel29.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel29.setLayout(new java.awt.GridBagLayout());

        jPanel2.setLayout(new java.awt.GridBagLayout());

        checkbox_chunkedUpload.setText("Use chunked upload if available");
        checkbox_chunkedUpload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkbox_chunkedUploadActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel2.add(checkbox_chunkedUpload, gridBagConstraints);

        checkbox_chunkedUploadFallback.setText("Fall back to non-chunked upload");
        checkbox_chunkedUploadFallback.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkbox_chunkedUploadFallbackActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel2.add(checkbox_chunkedUploadFallback, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        jPanel2.add(jSeparator2, gridBagConstraints);

        label_chunkSize.setText("Chunk size:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 5);
        jPanel2.add(label_chunkSize, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        spinner_chunkSize.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(1), Integer.valueOf(1), null, Integer.valueOf(1)));
        spinner_chunkSize.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinner_chunkSizeStateChanged(evt);
            }
        });
        spinner_chunkSize.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                spinner_chunkSizeKeyPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        jPanel1.add(spinner_chunkSize, gridBagConstraints);

        label_mb.setText("MB");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        jPanel1.add(label_mb, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel2.add(jPanel1, gridBagConstraints);

        label_maxConcurrentUploads.setText("Max. concurrent uploads:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 5);
        jPanel2.add(label_maxConcurrentUploads, gridBagConstraints);

        spinner_maxConcurrentUploads.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(1), Integer.valueOf(1), null, Integer.valueOf(1)));
        spinner_maxConcurrentUploads.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinner_maxConcurrentUploadsStateChanged(evt);
            }
        });
        spinner_maxConcurrentUploads.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                spinner_maxConcurrentUploadsKeyPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel2.add(spinner_maxConcurrentUploads, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        jPanel2.add(jSeparator1, gridBagConstraints);

        checkbox_retryIngestingFailedRecordings.setSelected(true);
        checkbox_retryIngestingFailedRecordings.setText("Retry ingesting failed recordings");
        checkbox_retryIngestingFailedRecordings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkbox_retryIngestingFailedRecordingsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel2.add(checkbox_retryIngestingFailedRecordings, gridBagConstraints);

        label_triesFailedRecordings.setText("Number of retries:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 5);
        jPanel2.add(label_triesFailedRecordings, gridBagConstraints);

        spinner_triesFailedRecordings.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(1), Integer.valueOf(1), null, Integer.valueOf(1)));
        spinner_triesFailedRecordings.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinner_triesFailedRecordingsStateChanged(evt);
            }
        });
        spinner_triesFailedRecordings.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                spinner_triesFailedRecordingsKeyPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel2.add(spinner_triesFailedRecordings, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel29.add(jPanel2, gridBagConstraints);

        jTabbedPane1.addTab("Upload", jPanel29);

        jPanel31.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel31.setLayout(new java.awt.GridBagLayout());

        jPanel9.setLayout(new java.awt.GridBagLayout());

        label_mainDir.setText("Main directory:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        jPanel9.add(label_mainDir, gridBagConstraints);

        textfield_mainDir.setEditable(false);
        textfield_mainDir.setText("PathToInbox");
        textfield_mainDir.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                textfield_mainDirKeyPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel9.add(textfield_mainDir, gridBagConstraints);

        button_select.setText("Select");
        button_select.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_selectActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel9.add(button_select, gridBagConstraints);

        label_defInboxName.setText("Default Inbox Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        jPanel9.add(label_defInboxName, gridBagConstraints);

        textfield_defInboxName.setText("Default");
        textfield_defInboxName.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                textfield_defInboxNameKeyPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel9.add(textfield_defInboxName, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel31.add(jPanel9, gridBagConstraints);

        jTabbedPane1.addTab("Inbox", jPanel31);

        jPanel32.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel32.setLayout(new java.awt.GridBagLayout());

        jPanel16.setLayout(new java.awt.GridBagLayout());

        label_defaultWorkflow.setText("Default workflow:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        jPanel16.add(label_defaultWorkflow, gridBagConstraints);

        textfield_defaultWorkflow.setText("full");
        textfield_defaultWorkflow.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                textfield_defaultWorkflowKeyPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel16.add(textfield_defaultWorkflow, gridBagConstraints);

        button_reset_textfield_defaultWorkflow.setText("Reset");
        button_reset_textfield_defaultWorkflow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_reset_textfield_defaultWorkflowActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel16.add(button_reset_textfield_defaultWorkflow, gridBagConstraints);

        label_presentationSuffix.setText("Presentation suffix:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        jPanel16.add(label_presentationSuffix, gridBagConstraints);

        textfield_presentationSuffix.setText("_VGA");
        textfield_presentationSuffix.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                textfield_presentationSuffixKeyPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel16.add(textfield_presentationSuffix, gridBagConstraints);

        button_reset_textfield_presentationSuffix.setText("Reset");
        button_reset_textfield_presentationSuffix.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_reset_textfield_presentationSuffixActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel16.add(button_reset_textfield_presentationSuffix, gridBagConstraints);

        label_presenterSuffix.setText("Presenter suffix:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        jPanel16.add(label_presenterSuffix, gridBagConstraints);

        textfield_presenterSuffix.setText("_VIDEO");
        textfield_presenterSuffix.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                textfield_presenterSuffixKeyPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel16.add(textfield_presenterSuffix, gridBagConstraints);

        button_reset_textfield_presenterSuffix.setText("Reset");
        button_reset_textfield_presenterSuffix.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_reset_textfield_presenterSuffixActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel16.add(button_reset_textfield_presenterSuffix, gridBagConstraints);

        label_mediafileSuffixes.setText("Mediafile suffixes:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        jPanel16.add(label_mediafileSuffixes, gridBagConstraints);

        textfield_mediafileSuffixes.setText("avi,mpg,mpeg,mp2,mp3,mp4,m4v,mov");
        textfield_mediafileSuffixes.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                textfield_mediafileSuffixesKeyPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel16.add(textfield_mediafileSuffixes, gridBagConstraints);

        button_reset_textfield_mediafileSuffixes.setText("Reset");
        button_reset_textfield_mediafileSuffixes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_reset_textfield_mediafileSuffixesActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel16.add(button_reset_textfield_mediafileSuffixes, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel32.add(jPanel16, gridBagConstraints);

        jTabbedPane1.addTab("Media", jPanel32);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel27.add(jTabbedPane1, gridBagConstraints);

        jPanel6.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel6.setAlignmentX(0.0F);
        jPanel6.setAlignmentY(0.0F);
        jPanel6.setLayout(new java.awt.GridBagLayout());

        button_ok.setText("OK");
        button_ok.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_okActionPerformed(evt);
            }
        });
        button_ok.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                button_okKeyPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel6.add(button_ok, gridBagConstraints);

        button_cancel.setText("Cancel");
        button_cancel.setName(""); // NOI18N
        button_cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_cancelActionPerformed(evt);
            }
        });
        button_cancel.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                button_cancelKeyPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel6.add(button_cancel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_END;
        jPanel27.add(jPanel6, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(jPanel27, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void button_cancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_cancelActionPerformed
        cancelPressed();
    }//GEN-LAST:event_button_cancelActionPerformed

    private void button_okActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_okActionPerformed
        boolean displayMessage = false;
        String messageDialog = Constants.getInstance().getLocalizedString("SavePrefsStopActions1");
        messageDialog = inboxChanged ? (messageDialog + " " + Constants.getInstance().getLocalizedString("SavePrefsStopActions2")) : messageDialog;
        if ((application != null) && (restartRequired || inboxChanged)) {
            for (int it : ((RemoteInboxApplicationImpl) application).getDB().getRecordings().keySet()) {
                Recording recording = ((RemoteInboxApplicationImpl) application).getDB().getRecordings().get(it);
                if (!(recording.getState() == State.IDLE)) {
                    displayMessage = true;
                    break;
                }
            }
        }
        if (somethingChanged && (!displayMessage || (displayMessage && JOptionPane.showConfirmDialog(
                this,
                (messageDialog + ".\n" + Constants.getInstance().getLocalizedString("Continue")),
                Constants.getInstance().getLocalizedString("StopAllCurrentActions"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION))) {

            final SavePreferencesUI spui = new SavePreferencesUI();
            try {
                Image img = ImageIO.read(getClass().getResourceAsStream("/ui/matterhorn-icon.png"));
                spui.setIconImage(img);
            } catch (IOException ex) {
            }
            spui.setVisible(true);
            this.setVisible(false);

            final JFrame jf = this;
            Thread t = new Thread() {
                @Override
                public void run() {
                    // general settings
                    boolean startAsService = checkbox_startAsService.isSelected();
                    boolean checkForUpdates = checkbox_checkForUpdates.isSelected();
                    boolean showTooltips = checkbox_tooltips.isSelected();
                    boolean chunkedUpload = checkbox_chunkedUpload.isSelected();
                    boolean chunkedUploadFallback = checkbox_chunkedUploadFallback.isSelected();
                    boolean retryIngestingFailedRecordings = checkbox_retryIngestingFailedRecordings.isSelected();
                    int chunkSize = (int) spinner_chunkSize.getValue();
                    int triesFailedIngests = (int) spinner_triesFailedRecordings.getValue();
                    int maxConcurrentUploads = (int) spinner_maxConcurrentUploads.getValue();
                    // server settings
                    String host = textfield_host.getText();
                    int port = (int) spinner_port.getValue();
                    String username = textfield_username.getText();
                    String password = new String(textfield_password.getPassword());
                    // inbox settings
                    String mainDir = textfield_mainDir.getText();
                    String defInboxName = textfield_defInboxName.getText();
                    // media settings
                    String presentationSuffix = textfield_presentationSuffix.getText();
                    String presenterSuffix = textfield_presenterSuffix.getText();
                    String mediaFileSuffixes = textfield_mediafileSuffixes.getText();
                    String defaultWorkflow = textfield_defaultWorkflow.getText();

                    // general settings
                    if (config != null) {
                        config.set(Constants.PROPKEY_START_AS_SERVICE, String.valueOf(startAsService));
                        config.set(Constants.PROPKEY_CHECK_FOR_UPDATES, String.valueOf(checkForUpdates));
                        config.set(Constants.PROPKEY_SHOW_TOOLTIPS, String.valueOf(showTooltips));
                        config.set(Constants.PROPKEY_CHUNKED_UPLOAD, String.valueOf(chunkedUpload));
                        config.set(Constants.PROPKEY_CHUNKED_UPLOAD_FALLBACK, String.valueOf(chunkedUploadFallback));
                        config.set(Constants.PROPKEY_RETRY_INGESTING_FAILED_RECORDINGS, String.valueOf(retryIngestingFailedRecordings));
                        config.set(Constants.PROPKEY_CHUNKSIZE, String.valueOf(chunkSize));
                        config.set(Constants.PROPKEY_INGESTFAILEDRECORDINGSTRIES, String.valueOf(triesFailedIngests));
                        config.set(Constants.PROPKEY_MAXCONCURRENTUPLOADS, String.valueOf(maxConcurrentUploads));
                        // server settings
                        config.set(Constants.PROPKEY_HOST, host);
                        config.set(Constants.PROPKEY_PORT, String.valueOf(port));
                        config.set(Constants.PROPKEY_USERNAME, username);
                        config.set(Constants.PROPKEY_PASSWORD, password);
                        // inbox settings
                        config.set(Constants.PROPKEY_MAINDIR, mainDir);
                        config.set(Constants.PROPKEY_DEFAULTINBOX, defInboxName);
                        // media settings
                        config.set(Constants.PROPKEY_PRESENTATION_SUFFIX, presentationSuffix);
                        config.set(Constants.PROPKEY_PRESENTER_SUFFIX, presenterSuffix);
                        config.set(Constants.PROPKEY_MEDIAFILE_SUFFIXES, mediaFileSuffixes);
                        config.set(Constants.PROPKEY_DEFAULT_WORKFLOW, defaultWorkflow);

                        config.store(de.calltopower.mhri.util.Constants.CONFIG_PATH);
                    }

                    String text = Constants.getInstance().getLocalizedString("SuccessSavePreferences_msg_1");
                    String text2 = "\n" + Constants.getInstance().getLocalizedString("SuccessSavePreferences_msg_2");
                    String text3 = "\n" + Constants.getInstance().getLocalizedString("SuccessSavePreferences_msg_3");

                    String textToDisplay = text;
                    textToDisplay = warnOnlyForNewRecordings ? textToDisplay + text2 : textToDisplay;
                    textToDisplay = (checkBoxStartAsServiceChecked == checkbox_startAsService.isSelected()) ? textToDisplay : textToDisplay + text3;
                    boolean saved = false;
                    try {
                        if (application != null) {
                            ((RemoteInboxApplicationImpl) application).restart(restartRequired, inboxChanged);
                            if (desktopUI != null) {
                                desktopUI.checkNetworkStatus();
                            }
                        }
                        saved = true;
                    } catch (Exception ex) {
                    } finally {
                        if (spui != null) {
                            spui.dispose();
                        }
                        if (saved) {
                            JOptionPane.showMessageDialog(
                                    jf,
                                    textToDisplay,
                                    Constants.getInstance().getLocalizedString("SuccessSavePreferences"),
                                    JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(
                                    jf,
                                    text + (warnOnlyForNewRecordings ? text2 : "") + text3,
                                    Constants.getInstance().getLocalizedString("SuccessSavePreferences"),
                                    JOptionPane.INFORMATION_MESSAGE);
                        }
                        if (jf != null) {
                            jf.dispose();
                        }
                    }
                }
            };
            t.start();
        } else {
            if (config != null) {
                config.setProperties(initial_props);
            }
            // config.store(de.calltopower.mhri.util.Constants.CONFIG_PATH);
            if (!somethingChanged) {
                if (this != null) {
                    this.dispose();
                }
            }
        }
    }//GEN-LAST:event_button_okActionPerformed

    private void button_selectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_selectActionPerformed
        if (JOptionPane.showConfirmDialog(
                this,
                Constants.getInstance().getLocalizedString("WarningChangeMainDir_msg"),
                Constants.getInstance().getLocalizedString("WarningChangeMainDir"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
            JFileChooser chooser = new JFileChooser();
            File f = new File(selectedDirectory);
            if (f.exists() && f.isDirectory()) {
                chooser.setCurrentDirectory(new File(selectedDirectory));
            }
            chooser.setDialogTitle(Constants.getInstance().getLocalizedString("SelectNewMainDir"));
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                selectedDirectory = chooser.getSelectedFile().getAbsolutePath();
                if (!textfield_mainDir.getText().equals(selectedDirectory)) {
                    textfield_mainDir.setText(selectedDirectory);
                    somethingChanged = true;
                    restartRequired = true;
                    inboxChanged = true;
                }
            }
        }
    }//GEN-LAST:event_button_selectActionPerformed

    private void checkbox_startAsServiceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkbox_startAsServiceActionPerformed
        somethingChanged = true;
    }//GEN-LAST:event_checkbox_startAsServiceActionPerformed

    private void checkbox_chunkedUploadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkbox_chunkedUploadActionPerformed
        somethingChanged = true;
        restartRequired = true;
        if (!checkbox_chunkedUpload.isSelected()) {
            checkbox_chunkedUploadFallback.setEnabled(false);
            spinner_chunkSize.setEnabled(false);
        } else {
            checkbox_chunkedUploadFallback.setEnabled(true);
            spinner_chunkSize.setEnabled(true);
        }
    }//GEN-LAST:event_checkbox_chunkedUploadActionPerformed

    private void checkbox_chunkedUploadFallbackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkbox_chunkedUploadFallbackActionPerformed
        somethingChanged = true;
        restartRequired = true;
    }//GEN-LAST:event_checkbox_chunkedUploadFallbackActionPerformed

    private void textfield_mainDirKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_mainDirKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            button_okActionPerformed(null);
        } else {
            somethingChanged = true;
            restartRequired = true;
        }
    }//GEN-LAST:event_textfield_mainDirKeyPressed

    private void textfield_defInboxNameKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_defInboxNameKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            button_okActionPerformed(null);
        } else {
            somethingChanged = true;
            restartRequired = true;
        }
    }//GEN-LAST:event_textfield_defInboxNameKeyPressed

    private void textfield_presentationSuffixKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_presentationSuffixKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            button_okActionPerformed(null);
        } else {
            somethingChanged = true;
            restartRequired = true;
            warnOnlyForNewRecordings = true;
        }
    }//GEN-LAST:event_textfield_presentationSuffixKeyPressed

    private void textfield_presenterSuffixKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_presenterSuffixKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            button_okActionPerformed(null);
        } else {
            somethingChanged = true;
            restartRequired = true;
            warnOnlyForNewRecordings = true;
        }
    }//GEN-LAST:event_textfield_presenterSuffixKeyPressed

    private void textfield_mediafileSuffixesKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_mediafileSuffixesKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            button_okActionPerformed(null);
        } else {
            somethingChanged = true;
            restartRequired = true;
        }
    }//GEN-LAST:event_textfield_mediafileSuffixesKeyPressed

    private void textfield_defaultWorkflowKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_defaultWorkflowKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            button_okActionPerformed(null);
        } else {
            somethingChanged = true;
            restartRequired = true;
        }
    }//GEN-LAST:event_textfield_defaultWorkflowKeyPressed

    private void spinner_maxConcurrentUploadsKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_spinner_maxConcurrentUploadsKeyPressed
        somethingChanged = true;
    }//GEN-LAST:event_spinner_maxConcurrentUploadsKeyPressed

    private void spinner_maxConcurrentUploadsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinner_maxConcurrentUploadsStateChanged
        somethingChanged = true;
    }//GEN-LAST:event_spinner_maxConcurrentUploadsStateChanged

    private void spinner_chunkSizeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinner_chunkSizeStateChanged
        somethingChanged = true;
        restartRequired = true;
    }//GEN-LAST:event_spinner_chunkSizeStateChanged

    private void spinner_chunkSizeKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_spinner_chunkSizeKeyPressed
        somethingChanged = true;
        restartRequired = true;
    }//GEN-LAST:event_spinner_chunkSizeKeyPressed

    private void button_okKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_button_okKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            button_okActionPerformed(null);
        } else if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
            button_cancelActionPerformed(null);
        }
    }//GEN-LAST:event_button_okKeyPressed

    private void button_cancelKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_button_cancelKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            button_cancelActionPerformed(null);
        } else if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
            button_cancelActionPerformed(null);
        }
    }//GEN-LAST:event_button_cancelKeyPressed

    private void checkbox_retryIngestingFailedRecordingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkbox_retryIngestingFailedRecordingsActionPerformed
        somethingChanged = true;
        if (!checkbox_retryIngestingFailedRecordings.isSelected()) {
            spinner_triesFailedRecordings.setEnabled(false);
        } else {
            spinner_triesFailedRecordings.setEnabled(true);
        }
    }//GEN-LAST:event_checkbox_retryIngestingFailedRecordingsActionPerformed

    private void checkbox_checkForUpdatesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkbox_checkForUpdatesActionPerformed
        somethingChanged = true;
    }//GEN-LAST:event_checkbox_checkForUpdatesActionPerformed

    private void spinner_triesFailedRecordingsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinner_triesFailedRecordingsStateChanged
        somethingChanged = true;
    }//GEN-LAST:event_spinner_triesFailedRecordingsStateChanged

    private void spinner_triesFailedRecordingsKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_spinner_triesFailedRecordingsKeyPressed
        somethingChanged = true;
    }//GEN-LAST:event_spinner_triesFailedRecordingsKeyPressed

    private void button_reset_textfield_presentationSuffixActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_reset_textfield_presentationSuffixActionPerformed
        String text = textfield_presentationSuffix.getText();
        if (config != null) {
            String newText = config.get(Constants.PROPKEY_PRESENTATION_SUFFIX);
            if (!text.equals(newText)) {
                textfield_presentationSuffix.setText(newText);
                somethingChanged = true;
                restartRequired = true;
            }
        }
    }//GEN-LAST:event_button_reset_textfield_presentationSuffixActionPerformed

    private void button_reset_textfield_presenterSuffixActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_reset_textfield_presenterSuffixActionPerformed
        String text = textfield_presenterSuffix.getText();
        if (config != null) {
            String newText = config.get(Constants.PROPKEY_PRESENTER_SUFFIX);
            if (!text.equals(newText)) {
                textfield_presenterSuffix.setText(newText);
                somethingChanged = true;
                restartRequired = true;
            }
        }
    }//GEN-LAST:event_button_reset_textfield_presenterSuffixActionPerformed

    private void button_reset_textfield_mediafileSuffixesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_reset_textfield_mediafileSuffixesActionPerformed
        String text = textfield_mediafileSuffixes.getText();
        String newText = config.get(Constants.PROPKEY_MEDIAFILE_SUFFIXES);
        if (!text.equals(newText)) {
            textfield_mediafileSuffixes.setText(newText);
            somethingChanged = true;
            restartRequired = true;
        }
    }//GEN-LAST:event_button_reset_textfield_mediafileSuffixesActionPerformed

    private void button_reset_textfield_defaultWorkflowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_reset_textfield_defaultWorkflowActionPerformed
        String text = textfield_defaultWorkflow.getText();
        if (config != null) {
            String newText = config.get(Constants.PROPKEY_DEFAULT_WORKFLOW);
            if (!text.equals(newText)) {
                textfield_defaultWorkflow.setText(newText);
                somethingChanged = true;
                restartRequired = true;
            }
        }
    }//GEN-LAST:event_button_reset_textfield_defaultWorkflowActionPerformed

    private void textfield_passwordKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_passwordKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            button_okActionPerformed(null);
        } else {
            somethingChanged = true;
            restartRequired = true;
        }
    }//GEN-LAST:event_textfield_passwordKeyPressed

    private void textfield_usernameKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_usernameKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            button_okActionPerformed(null);
        } else {
            somethingChanged = true;
            restartRequired = true;
        }
    }//GEN-LAST:event_textfield_usernameKeyPressed

    private void textfield_hostKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_hostKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            button_okActionPerformed(null);
        } else {
            somethingChanged = true;
            restartRequired = true;
        }
    }//GEN-LAST:event_textfield_hostKeyPressed

    private void checkbox_tooltipsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkbox_tooltipsActionPerformed
        somethingChanged = true;
    }//GEN-LAST:event_checkbox_tooltipsActionPerformed

    private void spinner_portStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinner_portStateChanged
        somethingChanged = true;
        restartRequired = true;
    }//GEN-LAST:event_spinner_portStateChanged

    private void spinner_portKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_spinner_portKeyPressed
        somethingChanged = true;
        restartRequired = true;
    }//GEN-LAST:event_spinner_portKeyPressed

    private void button_importKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_button_importKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            importPreferences();
        }
    }//GEN-LAST:event_button_importKeyReleased

    private void button_importMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_button_importMouseClicked
        importPreferences();
    }//GEN-LAST:event_button_importMouseClicked

    private void button_exportKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_button_exportKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            exportPreferences();
        }
    }//GEN-LAST:event_button_exportKeyReleased

    private void button_exportMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_button_exportMouseClicked
        exportPreferences();
    }//GEN-LAST:event_button_exportMouseClicked
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton button_cancel;
    private javax.swing.JButton button_export;
    private javax.swing.JButton button_import;
    private javax.swing.JButton button_ok;
    private javax.swing.JButton button_reset_textfield_defaultWorkflow;
    private javax.swing.JButton button_reset_textfield_mediafileSuffixes;
    private javax.swing.JButton button_reset_textfield_presentationSuffix;
    private javax.swing.JButton button_reset_textfield_presenterSuffix;
    private javax.swing.JButton button_select;
    private javax.swing.JCheckBox checkbox_checkForUpdates;
    private javax.swing.JCheckBox checkbox_chunkedUpload;
    private javax.swing.JCheckBox checkbox_chunkedUploadFallback;
    private javax.swing.JCheckBox checkbox_retryIngestingFailedRecordings;
    private javax.swing.JCheckBox checkbox_startAsService;
    private javax.swing.JCheckBox checkbox_tooltips;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel27;
    private javax.swing.JPanel jPanel28;
    private javax.swing.JPanel jPanel29;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel30;
    private javax.swing.JPanel jPanel31;
    private javax.swing.JPanel jPanel32;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel label_chunkSize;
    private javax.swing.JLabel label_defInboxName;
    private javax.swing.JLabel label_defaultWorkflow;
    private javax.swing.JLabel label_host;
    private javax.swing.JLabel label_mainDir;
    private javax.swing.JLabel label_maxConcurrentUploads;
    private javax.swing.JLabel label_mb;
    private javax.swing.JLabel label_mediafileSuffixes;
    private javax.swing.JLabel label_password;
    private javax.swing.JLabel label_port;
    private javax.swing.JLabel label_presentationSuffix;
    private javax.swing.JLabel label_presenterSuffix;
    private javax.swing.JLabel label_triesFailedRecordings;
    private javax.swing.JLabel label_username;
    private javax.swing.JPanel panel_importExport;
    private javax.swing.JSpinner spinner_chunkSize;
    private javax.swing.JSpinner spinner_maxConcurrentUploads;
    private javax.swing.JSpinner spinner_port;
    private javax.swing.JSpinner spinner_triesFailedRecordings;
    private javax.swing.JTextField textfield_defInboxName;
    private javax.swing.JTextField textfield_defaultWorkflow;
    private javax.swing.JTextField textfield_host;
    private javax.swing.JTextField textfield_mainDir;
    private javax.swing.JTextField textfield_mediafileSuffixes;
    private javax.swing.JPasswordField textfield_password;
    private javax.swing.JTextField textfield_presentationSuffix;
    private javax.swing.JTextField textfield_presenterSuffix;
    private javax.swing.JTextField textfield_username;
    // End of variables declaration//GEN-END:variables
}
