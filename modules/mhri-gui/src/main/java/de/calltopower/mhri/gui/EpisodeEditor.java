/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.gui;

import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.TimeZone;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import de.calltopower.mhri.application.api.Recording;
import de.calltopower.mhri.application.impl.SpecificFileUtils;
import de.calltopower.mhri.util.Constants;
import de.calltopower.mhri.util.ParseUtils;

/**
 * SeriesEditor
 *
 * @date 11.11.2013
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class EpisodeEditor extends javax.swing.JFrame {

    private static final Logger logger = Logger.getLogger(EpisodeEditor.class);
    private final MHRIFocusTraversalPolicy traversalPolicy;
    private boolean somethingChanged = false;
    private boolean episodeInformationAvailable = false;
    private final Recording r;
    private final InboxOverview io;
    private String str_title_load = "";
    private String str_subject_load = "";
    private String str_created_load = "";
    private String str_language_load = "";
    private String str_creator_load = "";
    private String str_contributor_load = "";
    private String str_description_load = "";
    private String str_identifier_load = "";
    private String str_title = "";
    private String str_subject = "";
    private String str_created = "";
    private String str_language = "";
    private String str_creator = "";
    private String str_contributor = "";
    private String str_description = "";
    private String str_identifier = "";
    private final String str_titleStart = "<dcterms:title>";
    private final String str_titleEnd = "</dcterms:title>";
    private final String str_subjectStart = "<dcterms:subject>";
    private final String str_subjectEnd = "</dcterms:subject>";
    private final String str_createdStart = "<dcterms:created";
    private final String str_createdEnd = "</dcterms:created>";
    private final String str_languageStart = "<dcterms:language>";
    private final String str_languageEnd = "</dcterms:language>";
    private final String str_creatorStart = "<dcterms:creator>";
    private final String str_creatorEnd = "</dcterms:creator>";
    private final String str_contributorStart = "<dcterms:contributor>";
    private final String str_contributorEnd = "</dcterms:contributor>";
    private final String str_descriptionStart = "<dcterms:description>";
    private final String str_descriptionEnd = "</dcterms:description>";
    private final String str_identifierStart = "<dcterms:identifier>";
    private final String str_identifierEnd = "</dcterms:identifier>";
    private String episode = "";
    private final String format_MH = "yyyy-MM-dd, HH:mm:ss";
    private final String format = "dd.MM.yyyy, HH:mm:ss";
    private final SimpleDateFormat dateFormat_MH = new SimpleDateFormat(format_MH);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(format);

    public EpisodeEditor(InboxOverview io, Recording r, String episodeString) {
        this.io = io;
        this.r = r;

        dateFormat_MH.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormat.setTimeZone(TimeZone.getDefault());

        initComponents();

        this.setTitle(Constants.getInstance().getLocalizedString("EpisodeEditor"));
        this.jLabel1.setText(Constants.getInstance().getLocalizedString("Title") + ":");
        this.jLabel6.setText(Constants.getInstance().getLocalizedString("Subject") + ":");
        this.jLabel2.setText(Constants.getInstance().getLocalizedString("Created") + ":");
        this.jLabel8.setText(Constants.getInstance().getLocalizedString("Language") + ":");
        this.jLabel4.setText(Constants.getInstance().getLocalizedString("Creator") + ":");
        this.jLabel5.setText(Constants.getInstance().getLocalizedString("Contributor") + ":");
        this.jLabel3.setText(Constants.getInstance().getLocalizedString("Description") + ":");
        this.jLabel7.setText(Constants.getInstance().getLocalizedString("Identifier") + ":");
        this.button_ok.setText(Constants.getInstance().getLocalizedString("OK"));
        this.button_cancel.setText(Constants.getInstance().getLocalizedString("Cancel"));
        this.button_setOriginalTitle.setText(Constants.getInstance().getLocalizedString("SetOriginalTitle"));
        this.button_unassign.setText(Constants.getInstance().getLocalizedString("Unassign"));
        this.button_reset.setText(Constants.getInstance().getLocalizedString("Reset"));
        this.button_assign.setText(Constants.getInstance().getLocalizedString("Assign"));
        this.label_timezone.setText(Constants.getInstance().getLocalizedString("Timezone") + ": " + TimeZone.getDefault().getID() + ",");
        this.label_dateFormat.setText(Constants.getInstance().getLocalizedString("Format") + ": " + format);

        this.button_unassign.setEnabled(false);
        this.button_reset.setEnabled(false);
        this.button_assign.setEnabled(false);
        this.setLocationRelativeTo(null);

        episode = episodeString;
        parseEpisodeString();

        LinkedList<Component> componentList = new LinkedList<>();
        componentList.add(textfield_title);
        componentList.add(button_setOriginalTitle);
        componentList.add(textfield_subject);
        componentList.add(textfield_creator);
        componentList.add(textfield_contributor);
        componentList.add(textfield_created);
        componentList.add(textfield_language);
        componentList.add(textfield_description);
        componentList.add(textfield_identifier);
        componentList.add(button_unassign);
        componentList.add(button_reset);
        componentList.add(button_assign);
        componentList.add(button_ok);
        componentList.add(button_cancel);
        traversalPolicy = new MHRIFocusTraversalPolicy(componentList);
        this.setFocusTraversalPolicy(traversalPolicy);

        this.button_unassign.setEnabled(episodeInformationAvailable);
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

    private String fromReadableDate(String s) {
        if (!s.trim().isEmpty() && (s.lastIndexOf(",") != -1) && (s.lastIndexOf(" ") != -1)) {
            try {
                Date time = dateFormat.parse(s);
                String mhTime = dateFormat_MH.format(time);

                return mhTime.replaceAll(", ", "T") + "Z";
            } catch (ParseException e) {
                logger.error("ParseException: " + e.getMessage());
            } catch (Exception e) {
                logger.error("Exception: " + e.getMessage());
            }
        }
        return s;
    }

    private String getReadableDate(String s) {
        if (!s.trim().isEmpty() && (s.lastIndexOf("T") != -1) && (s.lastIndexOf("Z") != -1)) {
            try {
                String d = s.replaceAll("T", ", ").replaceAll("Z", "");

                Date dateCreated = dateFormat_MH.parse(d);
                d = dateFormat.format(dateCreated);

                return d;
            } catch (ParseException e) {
                logger.error("ParseException: " + e.getMessage());
            } catch (Exception e) {
                logger.error("Exception: " + e.getMessage());
            }
        }
        return s;
    }

    private String validateTime(String s) {
        if (logger.isDebugEnabled()) {
            logger.debug("Validating time " + s);
        }
        if (!s.trim().isEmpty() || !s.trim().equals("")) {
            String frd_sc = fromReadableDate(s);
            if ((frd_sc.lastIndexOf("T") != -1) && (frd_sc.lastIndexOf("Z") != -1)) {
                return frd_sc;
            }
        }
        return null;
    }

    private void parseEpisodeString() {
        // filter episode -- get first appearance of <dcterms:IDENTIFIER><VALUE></dcterms:IDENTIFIER>

        str_title = ParseUtils.getInstance().getFirstAppearanceOf(episode, str_titleStart, str_titleEnd);
        str_subject = ParseUtils.getInstance().getFirstAppearanceOf(episode, str_subjectStart, str_subjectEnd);
        str_created = ParseUtils.getInstance().getFirstAppearanceOf(episode, str_createdStart, str_createdEnd);
        str_language = ParseUtils.getInstance().getFirstAppearanceOf(episode, str_languageStart, str_languageEnd);
        str_creator = ParseUtils.getInstance().getFirstAppearanceOf(episode, str_creatorStart, str_creatorEnd);
        str_contributor = ParseUtils.getInstance().getFirstAppearanceOf(episode, str_contributorStart, str_contributorEnd);
        str_description = ParseUtils.getInstance().getFirstAppearanceOf(episode, str_descriptionStart, str_descriptionEnd);
        str_identifier = ParseUtils.getInstance().getFirstAppearanceOf(episode, str_identifierStart, str_identifierEnd);
        if (str_created.contains(">")) {
            str_created = str_created.substring(str_created.lastIndexOf(">") + 1).trim();
        }

        str_title_load = str_title;
        str_subject_load = str_subject;
        str_created_load = str_created;
        str_language_load = str_language;
        str_creator_load = str_creator;
        str_contributor_load = str_contributor;
        str_description_load = str_description;
        str_identifier_load = str_identifier;

        episodeInformationAvailable = !str_title.isEmpty()
                || !str_subject.isEmpty()
                || !str_created.isEmpty()
                || !str_language.isEmpty()
                || !str_creator.isEmpty()
                || !str_contributor.isEmpty()
                || !str_description.isEmpty()
                || !str_identifier.isEmpty();

        resetUI();
    }

    private void resetUI() {
        this.textfield_title.setText(str_title_load);
        this.textfield_subject.setText(str_subject);
        this.textfield_created.setText(getReadableDate(str_created_load));
        this.textfield_language.setText(str_language_load);
        this.textfield_creator.setText(str_creator_load);
        this.textfield_contributor.setText(str_contributor_load);
        this.textfield_description.setText(str_description_load);
        this.textfield_identifier.setText(str_identifier_load);

        str_title = str_title_load;
        str_subject = str_subject_load;
        str_created = str_created_load;
        str_language = str_language_load;
        str_creator = str_creator_load;
        str_contributor = str_contributor_load;
        str_description = str_description_load;
        str_identifier = str_identifier_load;

        somethingChanged = false;
    }

    private void getEmptySeriesString() {
        episode = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
                + "<dublincore xmlns=\"http://www.opencastproject.org/xsd/1.0/dublincore/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + str_titleStart + "" + str_titleEnd
                + str_subjectStart + "" + str_subjectEnd
                + str_createdStart + ">" + "" + str_createdEnd
                + str_languageStart + "" + str_languageEnd
                + str_creatorStart + "" + str_creatorEnd
                + str_contributorStart + "" + str_contributorEnd
                + str_descriptionStart + "" + str_descriptionEnd
                + str_identifierStart + "" + str_identifierEnd
                + "</dublincore>";
    }

    private void getNewEpisodeString(String str_created_inUTC) {
        episode = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<dublincore xmlns=\"http://www.opencastproject.org/xsd/1.0/dublincore/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + str_titleStart + str_title + str_titleEnd
                + str_subjectStart + str_subject + str_subjectEnd
                + str_createdStart + ">" + ((str_created_inUTC == null) ? "" : str_created_inUTC) + str_createdEnd
                + str_languageStart + str_language + str_languageEnd
                + str_creatorStart + str_creator + str_creatorEnd
                + str_contributorStart + str_contributor + str_contributorEnd
                + str_descriptionStart + str_description + str_descriptionEnd
                + str_identifierStart + str_identifier + str_identifierEnd
                + "</dublincore>";
    }

    private void unassignSeries() {
        if ((r != null) && episodeInformationAvailable && (JOptionPane.showConfirmDialog(
                this,
                Constants.getInstance().getLocalizedString("UnassignAssignedEpisodeInfo_msg"),
                Constants.getInstance().getLocalizedString("UnassignAssignedEpisodeInfo"),
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)) {
            if (SpecificFileUtils.removeEpisode(r)) {
                r.resetTitle();
                getEmptySeriesString();
                parseEpisodeString();
                this.button_unassign.setEnabled(false);
                if (io != null) {
                    io.selectLastSelectedRecording();
                }
                JOptionPane.showMessageDialog(
                        this,
                        Constants.getInstance().getLocalizedString("SuccessUnassignedAssignedEpisodeInfo_msg"),
                        Constants.getInstance().getLocalizedString("SuccessUnassignedAssignedEpisodeInfo"),
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        Constants.getInstance().getLocalizedString("FailedUnassignedAssignedEpisodeInfo_msg"),
                        Constants.getInstance().getLocalizedString("FailedUnassignedAssignedEpisodeInfo"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void assignEpisode(final boolean dispose) {
        try {
            boolean unassign = this.button_unassign.isEnabled();
            boolean reset = this.button_reset.isEnabled();
            boolean assign = this.button_assign.isEnabled();
            this.button_unassign.setEnabled(false);
            this.button_reset.setEnabled(false);
            this.button_assign.setEnabled(false);
            String str_created_inUTC = validateTime(str_created);
            if (!str_created.isEmpty() && !str_created.trim().equals("")) {
                if (str_created_inUTC == null) {
                    JOptionPane.showMessageDialog(
                            this,
                            Constants.getInstance().getLocalizedString("FailedParsingTimeInfo_msg"),
                            Constants.getInstance().getLocalizedString("FailedParsingTimeInfo"),
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            if (somethingChanged
                    && (!episodeInformationAvailable || (episodeInformationAvailable && (JOptionPane.showConfirmDialog(
                            this,
                            Constants.getInstance().getLocalizedString("CreateEpisodeInfo_msg"),
                            Constants.getInstance().getLocalizedString("CreateEpisodeInfo"),
                            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)))) {
                boolean success = false;
                try {
                    getNewEpisodeString(str_created_inUTC);
                    parseEpisodeString();
                    if (SpecificFileUtils.saveEpisode(r, episode)) {
                        r.setTitle(str_title);
                        JOptionPane.showMessageDialog(
                                this,
                                Constants.getInstance().getLocalizedString("SuccessCreateEpisodeInfo_msg"),
                                Constants.getInstance().getLocalizedString("SuccessCreateEpisodeInfo"),
                                JOptionPane.INFORMATION_MESSAGE);
                        success = true;
                    } else {
                        JOptionPane.showMessageDialog(
                                this,
                                Constants.getInstance().getLocalizedString("FailedAssignEpisodeInfo_msg"),
                                Constants.getInstance().getLocalizedString("FailedAssignEpisodeInfo"),
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            this,
                            Constants.getInstance().getLocalizedString("FailedAssignEpisodeInfo_msg"),
                            Constants.getInstance().getLocalizedString("FailedAssignEpisodeInfo"),
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    if (dispose && success) {
                        this.dispose();
                        if (io != null) {
                            io.selectLastSelectedRecording();
                        }
                        return;
                    } else if (success) {
                        this.button_unassign.setEnabled(true);
                        if (io != null) {
                            io.selectLastSelectedRecording();
                        }
                    }
                }
            } else if (!somethingChanged) {
                if (dispose) {
                    this.dispose();
                    return;
                }
            }

            this.button_unassign.setEnabled(unassign);
            this.button_reset.setEnabled(reset);
            this.button_assign.setEnabled(assign);
        } catch (Exception e) {
            logger.error("InboxInformation::assignEpisode: " + e.getMessage());
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
        button_setOriginalTitle = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        textfield_subject = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        textfield_created = new javax.swing.JTextField();
        label_timezone = new javax.swing.JLabel();
        label_dateFormat = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        textfield_language = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        textfield_creator = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        textfield_contributor = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        textfield_description = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        textfield_identifier = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JSeparator();
        jPanel9 = new javax.swing.JPanel();
        button_unassign = new javax.swing.JButton();
        button_reset = new javax.swing.JButton();
        button_assign = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        button_ok = new javax.swing.JButton();
        button_cancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Episode editor");
        setMinimumSize(new java.awt.Dimension(644, 302));
        setResizable(false);
        getContentPane().setLayout(new java.awt.GridBagLayout());

        jPanel6.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));
        jPanel6.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("Title:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
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
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel6.add(textfield_title, gridBagConstraints);

        button_setOriginalTitle.setText("Set original title");
        button_setOriginalTitle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_setOriginalTitleActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        jPanel6.add(button_setOriginalTitle, gridBagConstraints);

        jLabel6.setText("Subject:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel6.add(jLabel6, gridBagConstraints);

        textfield_subject.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textfield_subjectKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        jPanel6.add(textfield_subject, gridBagConstraints);

        jLabel2.setText("Created:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel6.add(jLabel2, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        textfield_created.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textfield_createdKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel1.add(textfield_created, gridBagConstraints);

        label_timezone.setForeground(new java.awt.Color(153, 153, 153));
        label_timezone.setText("Timezone: Timezone,");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel1.add(label_timezone, gridBagConstraints);

        label_dateFormat.setForeground(new java.awt.Color(153, 153, 153));
        label_dateFormat.setText("Format: dd.MM.yyyy, HH:mm:ss");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        jPanel1.add(label_dateFormat, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel6.add(jPanel1, gridBagConstraints);

        jLabel8.setText("Language:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel6.add(jLabel8, gridBagConstraints);

        textfield_language.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textfield_languageKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        jPanel6.add(textfield_language, gridBagConstraints);

        jLabel4.setText("Creator:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel6.add(jLabel4, gridBagConstraints);

        textfield_creator.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textfield_creatorKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        jPanel6.add(textfield_creator, gridBagConstraints);

        jLabel5.setText("Contributor:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel6.add(jLabel5, gridBagConstraints);

        textfield_contributor.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textfield_contributorKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        jPanel6.add(textfield_contributor, gridBagConstraints);

        jLabel3.setText("Description:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        jPanel6.add(jLabel3, gridBagConstraints);

        textfield_description.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textfield_descriptionKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
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
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        jPanel6.add(textfield_identifier, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        jPanel6.add(jSeparator1, gridBagConstraints);

        jPanel9.setLayout(new java.awt.GridBagLayout());

        button_unassign.setText("Unassign");
        button_unassign.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_unassignActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel9.add(button_unassign, gridBagConstraints);

        button_reset.setText("Reset");
        button_reset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_resetActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel9.add(button_reset, gridBagConstraints);

        button_assign.setText("Assign");
        button_assign.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_assignActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel9.add(button_assign, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel6.add(jPanel9, gridBagConstraints);

        jPanel5.setLayout(new java.awt.GridBagLayout());

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
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel5.add(button_ok, gridBagConstraints);

        button_cancel.setText("Cancel");
        button_cancel.setOpaque(false);
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
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel5.add(button_cancel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel6.add(jPanel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(jPanel6, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void button_okActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_okActionPerformed
        assignEpisode(true);
    }//GEN-LAST:event_button_okActionPerformed

    private void textfield_titleKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_titleKeyReleased
        if (!textfield_title.getText().trim().isEmpty()) {
            somethingChanged = true;
            str_title = textfield_title.getText();
            this.button_reset.setEnabled(true);
            this.button_assign.setEnabled(true);
        }
    }//GEN-LAST:event_textfield_titleKeyReleased

    private void textfield_createdKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_createdKeyReleased
        somethingChanged = true;
        str_created = textfield_created.getText();
        this.button_reset.setEnabled(true);
        this.button_assign.setEnabled(true);
    }//GEN-LAST:event_textfield_createdKeyReleased

    private void button_cancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_cancelActionPerformed
        cancelPressed();
    }//GEN-LAST:event_button_cancelActionPerformed

    private void button_resetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_resetActionPerformed
        this.button_reset.setEnabled(false);
        this.button_assign.setEnabled(false);
        resetUI();
        this.button_ok.requestFocus();
    }//GEN-LAST:event_button_resetActionPerformed

    private void button_assignActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_assignActionPerformed
        assignEpisode(false);
        this.button_ok.requestFocus();
    }//GEN-LAST:event_button_assignActionPerformed

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

    private void textfield_descriptionKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_descriptionKeyReleased
        somethingChanged = true;
        str_description = textfield_description.getText();
        this.button_reset.setEnabled(true);
        this.button_assign.setEnabled(true);
    }//GEN-LAST:event_textfield_descriptionKeyReleased

    private void textfield_creatorKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_creatorKeyReleased
        somethingChanged = true;
        str_creator = textfield_creator.getText();
        this.button_reset.setEnabled(true);
        this.button_assign.setEnabled(true);
    }//GEN-LAST:event_textfield_creatorKeyReleased

    private void textfield_contributorKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_contributorKeyReleased
        somethingChanged = true;
        str_contributor = textfield_contributor.getText();
        this.button_reset.setEnabled(true);
        this.button_assign.setEnabled(true);
    }//GEN-LAST:event_textfield_contributorKeyReleased

    private void textfield_subjectKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_subjectKeyReleased
        somethingChanged = true;
        str_subject = textfield_subject.getText();
        this.button_reset.setEnabled(true);
        this.button_assign.setEnabled(true);
    }//GEN-LAST:event_textfield_subjectKeyReleased

    private void textfield_languageKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_languageKeyReleased
        somethingChanged = true;
        str_language = textfield_language.getText();
        this.button_reset.setEnabled(true);
        this.button_assign.setEnabled(true);
    }//GEN-LAST:event_textfield_languageKeyReleased

    private void button_setOriginalTitleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_setOriginalTitleActionPerformed
        somethingChanged = true;
        str_title = (r != null) ? r.getOriginalTitle() : "";
        this.textfield_title.setText(str_title);
        this.button_reset.setEnabled(true);
        this.button_assign.setEnabled(true);
    }//GEN-LAST:event_button_setOriginalTitleActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton button_assign;
    private javax.swing.JButton button_cancel;
    private javax.swing.JButton button_ok;
    private javax.swing.JButton button_reset;
    private javax.swing.JButton button_setOriginalTitle;
    private javax.swing.JButton button_unassign;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel label_dateFormat;
    private javax.swing.JLabel label_timezone;
    private javax.swing.JTextField textfield_contributor;
    private javax.swing.JTextField textfield_created;
    private javax.swing.JTextField textfield_creator;
    private javax.swing.JTextField textfield_description;
    private javax.swing.JTextField textfield_identifier;
    private javax.swing.JTextField textfield_language;
    private javax.swing.JTextField textfield_subject;
    private javax.swing.JTextField textfield_title;
    // End of variables declaration//GEN-END:variables
}
