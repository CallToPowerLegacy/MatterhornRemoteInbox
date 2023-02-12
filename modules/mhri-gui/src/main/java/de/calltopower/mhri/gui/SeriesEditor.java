/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.gui;

import java.awt.Component;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.LinkedList;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import de.calltopower.mhri.application.api.Inbox;
import de.calltopower.mhri.application.api.RemoteInboxApplication;
import de.calltopower.mhri.application.impl.RemoteInboxApplicationImpl;
import de.calltopower.mhri.application.impl.SpecificFileUtils;
import de.calltopower.mhri.ingestclient.api.IngestClientException;
import de.calltopower.mhri.util.Constants;
import de.calltopower.mhri.util.ParseUtils;

/**
 * SeriesEditor
 *
 * @date 09.02.2013
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class SeriesEditor extends javax.swing.JFrame {

    private static final Logger logger = Logger.getLogger(SeriesEditor.class);
    private final MHRIFocusTraversalPolicy traversalPolicy;
    private final RemoteInboxApplicationImpl application;
    private final Inbox inbox;
    private final String str_titleStart = "<dcterms:title>";
    private final String str_titleEnd = "</dcterms:title>";
    private final String str_creatorStart = "<dcterms:creator>";
    private final String str_creatorEnd = "</dcterms:creator>";
    private final String str_contributorStart = "<dcterms:contributor>";
    private final String str_contributorEnd = "</dcterms:contributor>";
    private final String str_licenseStart = "<dcterms:license>";
    private final String str_licenseEnd = "</dcterms:license>";
    private final String str_subjectStart = "<dcterms:subject>";
    private final String str_subjectEnd = "</dcterms:subject>";
    private final String str_languageStart = "<dcterms:language>";
    private final String str_languageEnd = "</dcterms:language>";
    private final String str_identifierStart = "<dcterms:identifier>";
    private final String str_identifierEnd = "</dcterms:identifier>";
    private final String str_descriptionStart = "<dcterms:description>";
    private final String str_descriptionEnd = "</dcterms:description>";
    private CreateSeries createSeries = null;
    private boolean somethingChanged = false;
    private boolean seriesInformationAvailable = false;
    private String series = "";
    private String str_title_load = "";
    private String str_creator_load = "";
    private String str_contributor_load = "";
    private String str_license_load = "";
    private String str_subject_load = "";
    private String str_language_load = "";
    private String str_identifier_load = "";
    private String str_description_load = "";
    private String str_title = "";
    private String str_creator = "";
    private String str_contributor = "";
    private String str_license = "";
    private String str_subject = "";
    private String str_language = "";
    private String str_identifier = "";
    private String str_description = "";

    public SeriesEditor(RemoteInboxApplication application, Inbox inbox, String seriesString) {
        this.application = (RemoteInboxApplicationImpl) application;
        this.inbox = inbox;
        initComponents();

        this.setTitle(Constants.getInstance().getLocalizedString("SeriesEditor"));
        this.jLabel1.setText(Constants.getInstance().getLocalizedString("Title") + ":");
        this.jLabel2.setText(Constants.getInstance().getLocalizedString("Creator") + ":");
        this.jLabel3.setText(Constants.getInstance().getLocalizedString("Contributor") + ":");
        this.jLabel4.setText(Constants.getInstance().getLocalizedString("License") + ":");
        this.jLabel5.setText(Constants.getInstance().getLocalizedString("Subject") + ":");
        this.jLabel6.setText(Constants.getInstance().getLocalizedString("Language") + ":");
        this.jLabel12.setText(Constants.getInstance().getLocalizedString("Description") + ":");
        this.jLabel7.setText(Constants.getInstance().getLocalizedString("Identifier") + ":");
        this.button_ok.setText(Constants.getInstance().getLocalizedString("OK"));
        this.button_cancel.setText(Constants.getInstance().getLocalizedString("Cancel"));

        this.button_unassign.setText(Constants.getInstance().getLocalizedString("Unassign"));
        this.button_reset.setText(Constants.getInstance().getLocalizedString("Reset"));
        this.button_createNew.setText(Constants.getInstance().getLocalizedString("Create"));
        this.button_update.setText(Constants.getInstance().getLocalizedString("Update"));

        this.button_unassign.setEnabled(false);
        this.button_reset.setEnabled(false);
        this.button_createNew.setEnabled(false);
        this.button_update.setEnabled(false);
        this.setLocationRelativeTo(null);

        series = seriesString;
        parseSeriesString();

        LinkedList<Component> componentList = new LinkedList<>();
        componentList.add(textfield_title);
        componentList.add(textfield_subject);
        componentList.add(textfield_creator);
        componentList.add(textfield_contributor);
        componentList.add(textfield_license);
        componentList.add(textfield_language);
        componentList.add(textfield_description);
        componentList.add(textfield_identifier);
        componentList.add(button_unassign);
        componentList.add(button_reset);
        componentList.add(button_createNew);
        componentList.add(button_update);
        componentList.add(button_ok);
        componentList.add(button_cancel);
        traversalPolicy = new MHRIFocusTraversalPolicy(componentList);
        this.setFocusTraversalPolicy(traversalPolicy);

        this.button_unassign.setEnabled(seriesInformationAvailable);
        this.textfield_identifier.setEditable(false);
        this.button_ok.requestFocus();

        this.jPanel6.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancelPressed();
                }
            }
        });
    }

    private void initCreateSeriesUI() {
        if ((createSeries != null) && (!createSeries.isVisible())) {
            createSeries.dispose();
            createSeries = null;
        }
        if (createSeries == null) {
            createSeries = new CreateSeries();
            try {
                Image img = ImageIO.read(getClass().getResourceAsStream("/ui/matterhorn-icon.png"));
                createSeries.setIconImage(img);
            } catch (IOException ex) {
            }
        }
    }

    public String getSeriesTitle() {
        return str_title_load;
    }

    public String getSeriesCreator() {
        return str_creator_load;
    }

    public String getSeriesContributor() {
        return str_contributor_load;
    }

    public String getSeriesLicense() {
        return str_license_load;
    }

    public String getSeriesSubject() {
        return str_subject_load;
    }

    public String getSeriesLanguage() {
        return str_language_load;
    }

    public String getSeriesID() {
        return str_identifier_load;
    }

    public String getSeriesDescription() {
        return str_description_load;
    }

    private void cancelPressed() {
        if (!somethingChanged || (somethingChanged && JOptionPane.showConfirmDialog(
                this,
                Constants.getInstance().getLocalizedString("UnsavedChangesDiscard_msg"),
                Constants.getInstance().getLocalizedString("UnsavedChangesDiscard"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)) {
            this.dispose();
        }
    }

    private void parseSeriesString() {
        // filter series -- get first appearance of <dcterms:IDENTIFIER><VALUE></dcterms:IDENTIFIER>
        str_title = ParseUtils.getInstance().getFirstAppearanceOf(series, str_titleStart, str_titleEnd);
        str_creator = ParseUtils.getInstance().getFirstAppearanceOf(series, str_creatorStart, str_creatorEnd);
        str_contributor = ParseUtils.getInstance().getFirstAppearanceOf(series, str_contributorStart, str_contributorEnd);
        str_license = ParseUtils.getInstance().getFirstAppearanceOf(series, str_licenseStart, str_licenseEnd);
        str_subject = ParseUtils.getInstance().getFirstAppearanceOf(series, str_subjectStart, str_subjectEnd);
        str_language = ParseUtils.getInstance().getFirstAppearanceOf(series, str_languageStart, str_languageEnd);
        str_identifier = ParseUtils.getInstance().getFirstAppearanceOf(series, str_identifierStart, str_identifierEnd);
        str_description = ParseUtils.getInstance().getFirstAppearanceOf(series, str_descriptionStart, str_descriptionEnd);

        str_title_load = str_title;
        str_creator_load = str_creator;
        str_contributor_load = str_contributor;
        str_license_load = str_license;
        str_subject_load = str_subject;
        str_language_load = str_language;
        str_identifier_load = str_identifier;
        str_description_load = str_description;

        seriesInformationAvailable = !str_title.isEmpty()
                || !str_creator.isEmpty()
                || !str_contributor.isEmpty()
                || !str_license.isEmpty()
                || !str_subject.isEmpty()
                || !str_language.isEmpty()
                || !str_identifier.isEmpty()
                || !str_description.isEmpty();

        resetUI();
    }

    private void resetUI() {
        this.textfield_title.setText(str_title_load);
        this.textfield_creator.setText(str_creator_load);
        this.textfield_contributor.setText(str_contributor_load);
        this.textfield_license.setText(str_license_load);
        this.textfield_subject.setText(str_subject_load);
        this.textfield_language.setText(str_language_load);
        this.textfield_identifier.setText(str_identifier_load);
        this.textfield_description.setText(str_description_load);

        str_title = str_title_load;
        str_creator = str_creator_load;
        str_contributor = str_contributor_load;
        str_license = str_license_load;
        str_subject = str_subject_load;
        str_language = str_language_load;
        str_identifier = str_identifier_load;
        str_description = str_description_load;

        somethingChanged = false;
    }

    private void getEmptySeriesString() {
        series = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<dublincore xmlns=\"http://www.opencastproject.org/xsd/1.0/dublincore/\" xmlns:dcterms=\"http://purl.org/dc/terms/\">"
                + str_titleStart + "" + str_titleEnd
                + str_creatorStart + "" + str_creatorEnd
                + str_contributorStart + "" + str_contributorEnd
                + str_licenseStart + "" + str_licenseEnd
                + str_subjectStart + "" + str_subjectEnd
                + str_languageStart + "" + str_languageEnd
                + str_descriptionStart + "" + str_descriptionEnd
                + str_identifierStart + "" + str_identifierEnd
                + "</dublincore>";
    }

    private void getNewSeriesString() throws IngestClientException {
        series = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<dublincore xmlns=\"http://www.opencastproject.org/xsd/1.0/dublincore/\" xmlns:dcterms=\"http://purl.org/dc/terms/\">"
                + str_titleStart + str_title + str_titleEnd
                + str_creatorStart + str_creator + str_creatorEnd
                + str_contributorStart + str_contributor + str_contributorEnd
                + str_licenseStart + str_license + str_licenseEnd
                + str_subjectStart + str_subject + str_subjectEnd
                + str_languageStart + str_language + str_languageEnd
                + str_descriptionStart + str_description + str_descriptionEnd
                + str_identifierStart + str_identifier + str_identifierEnd
                + "</dublincore>";

        String seriesNew = (application != null) ? application.createNewSeries(series) : series;

        if (!seriesNew.isEmpty() && !seriesNew.equals("")) {
            series = seriesNew;
        }
    }

    private void unassignSeries() {
        if (seriesInformationAvailable && (JOptionPane.showConfirmDialog(
                this,
                Constants.getInstance().getLocalizedString("UnassignAssignedSeriesInfo_msg"),
                Constants.getInstance().getLocalizedString("UnassignAssignedSeriesInfo"),
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)) {
            if ((inbox != null) && SpecificFileUtils.removeSeries(inbox)) {
                getEmptySeriesString();
                parseSeriesString();
                inbox.setSeriesId("");
                inbox.setSeriesTitle("");
                this.button_unassign.setEnabled(false);
                JOptionPane.showMessageDialog(
                        this,
                        Constants.getInstance().getLocalizedString("SuccessUnassignedAssignedSeriesInfo_msg"),
                        Constants.getInstance().getLocalizedString("SuccessUnassignedAssignedSeriesInfo"),
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        Constants.getInstance().getLocalizedString("FailedUnassignedAssignedSeriesInfo_msg"),
                        Constants.getInstance().getLocalizedString("FailedUnassignedAssignedSeriesInfo"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void assignSeries(final boolean dispose, final boolean update) {
        try {
            initCreateSeriesUI();
            String text = update ? Constants.getInstance().getLocalizedString("UpdateSeriesInfo_msg") : Constants.getInstance().getLocalizedString("CreateSeriesInfo_msg");
            String title = update ? Constants.getInstance().getLocalizedString("UpdateSeriesInfo") : Constants.getInstance().getLocalizedString("CreateSeriesInfo");
            if (somethingChanged
                    && (!seriesInformationAvailable || (seriesInformationAvailable && (JOptionPane.showConfirmDialog(
                            this,
                            text,
                            title,
                            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)))) {
                this.createSeries.setVisible(true);
                final SeriesEditor jf = this;
                this.button_unassign.setEnabled(false);
                this.button_reset.setEnabled(false);
                this.button_createNew.setEnabled(false);
                this.button_update.setEnabled(false);

                final boolean wasEnabled = this.button_unassign.isEnabled();
                final JButton button_unassign_final = this.button_unassign;
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        boolean success = false;
                        try {
                            getNewSeriesString();
                            parseSeriesString();
                            if ((inbox != null) && SpecificFileUtils.saveSeries(inbox, series)) {
                                inbox.setSeriesId(str_identifier);
                                inbox.setSeriesTitle(str_title);
                                jf.createSeries.dispose();
                                String text = update ? Constants.getInstance().getLocalizedString("SuccessUpdateSeriesInfo_msg") : Constants.getInstance().getLocalizedString("SuccessCreateSeriesInfo_msg");
                                String title = update ? Constants.getInstance().getLocalizedString("SuccessUpdateSeriesInfo") : Constants.getInstance().getLocalizedString("SuccessCreateSeriesInfo");
                                JOptionPane.showMessageDialog(
                                        jf,
                                        text,
                                        title,
                                        JOptionPane.INFORMATION_MESSAGE);
                                success = true;
                            } else {
                                jf.createSeries.dispose();
                                JOptionPane.showMessageDialog(
                                        jf,
                                        Constants.getInstance().getLocalizedString("FailedAssignSeriesInfo_msg"),
                                        Constants.getInstance().getLocalizedString("FailedAssignSeriesInfo"),
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (IngestClientException ex) {
                            String msg1 = Constants.getInstance().getLocalizedString("FailedAssignSeriesInfo2_msg");
                            String msg1_title = Constants.getInstance().getLocalizedString("FailedAssignSeriesInfo2");
                            if ((ex.getType() == IngestClientException.Type.SERVER_ERROR)
                                    || (ex.getType() == IngestClientException.Type.NETWORK_ERROR)) {
                                msg1 = Constants.getInstance().getLocalizedString("FailedAssignSeriesInfo3_msg");
                                msg1_title = Constants.getInstance().getLocalizedString("FailedAssignSeriesInfo3");
                            }
                            jf.createSeries.dispose();
                            JOptionPane.showMessageDialog(
                                    jf,
                                    msg1_title,
                                    msg1,
                                    JOptionPane.ERROR_MESSAGE);
                        } finally {
                            jf.createSeries.dispose();
                            if (dispose && success) {
                                jf.dispose();
                            } else if (success) {
                                button_unassign_final.setEnabled(true);
                            } else {
                                button_unassign_final.setEnabled(wasEnabled);
                            }
                        }
                    }
                };
                t.start();
            } else if (!somethingChanged) {
                if (dispose) {
                    this.dispose();
                }
            }
        } catch (Exception e) {
            logger.error("InboxInformation::assignSeries: " + e.getMessage());
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

        jPanel6 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        textfield_title = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        textfield_creator = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        textfield_contributor = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        textfield_license = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        textfield_subject = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        textfield_language = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        textfield_description = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        textfield_identifier = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JSeparator();
        jPanel1 = new javax.swing.JPanel();
        button_unassign = new javax.swing.JButton();
        button_reset = new javax.swing.JButton();
        button_createNew = new javax.swing.JButton();
        button_update = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        button_ok = new javax.swing.JButton();
        button_cancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Series editor");
        setMinimumSize(new java.awt.Dimension(666, 305));
        setResizable(false);
        getContentPane().setLayout(new java.awt.GridBagLayout());

        jPanel6.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));
        jPanel6.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("Title:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel6.add(jLabel1, gridBagConstraints);

        textfield_title.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textfield_titleKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        jPanel6.add(textfield_title, gridBagConstraints);

        jLabel2.setText("Creator:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel6.add(jLabel2, gridBagConstraints);

        textfield_creator.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textfield_creatorKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        jPanel6.add(textfield_creator, gridBagConstraints);

        jLabel3.setText("Contributor:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel6.add(jLabel3, gridBagConstraints);

        textfield_contributor.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textfield_contributorKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        jPanel6.add(textfield_contributor, gridBagConstraints);

        jLabel4.setText("License:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel6.add(jLabel4, gridBagConstraints);

        textfield_license.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textfield_licenseKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        jPanel6.add(textfield_license, gridBagConstraints);

        jLabel5.setText("Subject:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel6.add(jLabel5, gridBagConstraints);

        textfield_subject.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textfield_subjectKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        jPanel6.add(textfield_subject, gridBagConstraints);

        jLabel6.setText("Language:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel6.add(jLabel6, gridBagConstraints);

        textfield_language.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textfield_languageKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        jPanel6.add(textfield_language, gridBagConstraints);

        jLabel12.setText("Description:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel6.add(jLabel12, gridBagConstraints);

        textfield_description.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textfield_descriptionKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        jPanel6.add(textfield_description, gridBagConstraints);

        jLabel7.setText("Identifier:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel6.add(jLabel7, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        jPanel6.add(textfield_identifier, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        jPanel6.add(jSeparator1, gridBagConstraints);

        button_unassign.setText("Unassign");
        button_unassign.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_unassignActionPerformed(evt);
            }
        });
        jPanel1.add(button_unassign);

        button_reset.setText("Reset");
        button_reset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_resetActionPerformed(evt);
            }
        });
        jPanel1.add(button_reset);

        button_createNew.setText("Create");
        button_createNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_createNewActionPerformed(evt);
            }
        });
        jPanel1.add(button_createNew);

        button_update.setText("Update");
        button_update.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_updateActionPerformed(evt);
            }
        });
        jPanel1.add(button_update);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_END;
        jPanel6.add(jPanel1, gridBagConstraints);

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
        jPanel2.add(button_ok);

        button_cancel.setText("Cancel");
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
        jPanel2.add(button_cancel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_END;
        jPanel6.add(jPanel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(jPanel6, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void button_okActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_okActionPerformed
        assignSeries(true, true);
    }//GEN-LAST:event_button_okActionPerformed

    private void textfield_titleKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_titleKeyReleased
        somethingChanged = true;
        str_title = textfield_title.getText();
        this.button_reset.setEnabled(true);
        this.button_createNew.setEnabled(true);
        this.button_update.setEnabled(!str_identifier.isEmpty() && !str_identifier.equals(""));
    }//GEN-LAST:event_textfield_titleKeyReleased

    private void textfield_creatorKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_creatorKeyReleased
        somethingChanged = true;
        str_creator = textfield_creator.getText();
        this.button_reset.setEnabled(true);
        this.button_createNew.setEnabled(true);
        this.button_update.setEnabled(!str_identifier.isEmpty() && !str_identifier.equals(""));
    }//GEN-LAST:event_textfield_creatorKeyReleased

    private void textfield_contributorKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_contributorKeyReleased
        somethingChanged = true;
        str_contributor = textfield_contributor.getText();
        this.button_reset.setEnabled(true);
        this.button_createNew.setEnabled(true);
        this.button_update.setEnabled(!str_identifier.isEmpty() && !str_identifier.equals(""));
    }//GEN-LAST:event_textfield_contributorKeyReleased

    private void textfield_licenseKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_licenseKeyReleased
        somethingChanged = true;
        str_license = textfield_license.getText();
        this.button_reset.setEnabled(true);
        this.button_createNew.setEnabled(true);
        this.button_update.setEnabled(!str_identifier.isEmpty() && !str_identifier.equals(""));
    }//GEN-LAST:event_textfield_licenseKeyReleased

    private void textfield_subjectKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_subjectKeyReleased
        somethingChanged = true;
        str_subject = textfield_subject.getText();
        this.button_reset.setEnabled(true);
        this.button_createNew.setEnabled(true);
        this.button_update.setEnabled(!str_identifier.isEmpty() && !str_identifier.equals(""));
    }//GEN-LAST:event_textfield_subjectKeyReleased

    private void textfield_languageKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_languageKeyReleased
        somethingChanged = true;
        str_language = textfield_language.getText();
        this.button_reset.setEnabled(true);
        this.button_createNew.setEnabled(true);
        this.button_update.setEnabled(!str_identifier.isEmpty() && !str_identifier.equals(""));
    }//GEN-LAST:event_textfield_languageKeyReleased

    private void button_cancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_cancelActionPerformed
        cancelPressed();
    }//GEN-LAST:event_button_cancelActionPerformed

    private void button_resetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_resetActionPerformed
        this.button_reset.setEnabled(false);
        this.button_createNew.setEnabled(false);
        this.button_update.setEnabled(false);
        resetUI();
        this.button_ok.requestFocus();
    }//GEN-LAST:event_button_resetActionPerformed

    private void textfield_descriptionKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_descriptionKeyReleased
        somethingChanged = true;
        str_description = textfield_description.getText();
        this.button_reset.setEnabled(true);
        this.button_createNew.setEnabled(true);
        this.button_update.setEnabled(!str_identifier.isEmpty() && !str_identifier.equals(""));
    }//GEN-LAST:event_textfield_descriptionKeyReleased

    private void button_createNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_createNewActionPerformed
        str_identifier = "";
        assignSeries(false, false);
        this.button_ok.requestFocus();
    }//GEN-LAST:event_button_createNewActionPerformed

    private void button_updateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_updateActionPerformed
        assignSeries(false, true);
        this.button_ok.requestFocus();
    }//GEN-LAST:event_button_updateActionPerformed

    private void button_unassignActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_unassignActionPerformed
        unassignSeries();
        this.button_ok.requestFocus();
    }//GEN-LAST:event_button_unassignActionPerformed

    private void button_cancelKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_button_cancelKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            button_cancelActionPerformed(null);
        } else if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
            button_cancelActionPerformed(null);
        }
    }//GEN-LAST:event_button_cancelKeyPressed

    private void button_okKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_button_okKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            button_okActionPerformed(null);
        } else if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
            button_cancelActionPerformed(null);
        }
    }//GEN-LAST:event_button_okKeyPressed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton button_cancel;
    private javax.swing.JButton button_createNew;
    private javax.swing.JButton button_ok;
    private javax.swing.JButton button_reset;
    private javax.swing.JButton button_unassign;
    private javax.swing.JButton button_update;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTextField textfield_contributor;
    private javax.swing.JTextField textfield_creator;
    private javax.swing.JTextField textfield_description;
    private javax.swing.JTextField textfield_identifier;
    private javax.swing.JTextField textfield_language;
    private javax.swing.JTextField textfield_license;
    private javax.swing.JTextField textfield_subject;
    private javax.swing.JTextField textfield_title;
    // End of variables declaration//GEN-END:variables
}
