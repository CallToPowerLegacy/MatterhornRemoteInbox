/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.gui;

import java.awt.Color;
import java.io.File;
import de.calltopower.mhri.application.api.Recording;
import de.calltopower.mhri.application.api.Recording.State;
import de.calltopower.mhri.application.api.RecordingFile;
import de.calltopower.mhri.application.impl.RecordingImpl;
import de.calltopower.mhri.util.Constants;
import de.calltopower.mhri.util.MHRIFileUtils;

/**
 * RecordingListItemPanel
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class RecordingListItemPanel extends javax.swing.JPanel {

    private Recording recording;
    private String title;
    private Color color_idle = Color.decode("#eeeeee");
    private Color color_idle_selected = Color.decode("#e0e0e0");
    private Color color_scheduled = Color.decode("#97bfad");
    private Color color_scheduled_selected = Color.decode("#8fb7a6");
    private Color color_recieving = Color.decode("#9fb9ce");
    private Color color_recieving_selected = Color.decode("#8da9bf");
    private Color color_uploading = Color.decode("#f4df76");
    private Color color_uploading_selected = Color.decode("#ead470");
    private Color color_pause = Color.decode("#efcdb1");
    private Color color_pause_selected = Color.decode("#e0bda1");
    private Color color_complete = Color.decode("#9fe855");
    private Color color_complete_selected = Color.decode("#91d14d");
    private Color color_error = Color.decode("#ff5e4d");
    private Color color_error_selected = Color.decode("#f75a4c");

    private void setBackgroundColor(Color c) {
        this.setBackground(c);
        panel_main.setBackground(c);
    }

    public RecordingListItemPanel(Recording _recording, boolean isSelected) {
        this.recording = _recording;
        this.title = (_recording != null) ? _recording.getTitle() : "";

        initComponents();

        this.noOfFilesText.setText(Constants.getInstance().getLocalizedString("NumberOfFiles") + ":");
        this.workflowText.setText(Constants.getInstance().getLocalizedString("Workflow") + ":");
        this.seriesText.setText(Constants.getInstance().getLocalizedString("Series") + ":");
        this.statusText.setText(Constants.getInstance().getLocalizedString("Status") + ":");
        this.detailsText.setText(Constants.getInstance().getLocalizedString("Details") + ":");

        if (recording != null) {
            double fileSize = 0;
            for (RecordingFile rf : recording.getFiles()) {
                File pfile = new File(rf.getPath()).getAbsoluteFile();
                fileSize += pfile.length();
            }
            int nrOfFiles = recording.getFiles().length;
            String klString = (nrOfFiles > 0) ? (" (" + MHRIFileUtils.getInstance().getFormattedSize(fileSize) + ")") : "";
            String workflow = recording.getWorkflowId();
            workflow = (!workflow.isEmpty() && (!workflow.equals(""))) ? workflow : recording.getInbox().getWorkflowId();
            workflow = (!workflow.isEmpty() && (!workflow.equals(""))) ? workflow : Constants.getInstance().getLocalizedString("PreferencesDefault");
            recording.getInbox().updateSeriesTitle();
            String series = recording.getInbox().getSeriesTitle();
            series = (!series.isEmpty() && (!series.equals(""))) ? series : Constants.getInstance().getLocalizedString("NoInfoAvailable");
            this.label_noOfFiles.setText(nrOfFiles + klString);
            this.label_workflow.setText(workflow);
            this.label_series.setText(series);

            if (!isSelected) {
                // setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
                setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
            } else {
                // setBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.shadow")));
                setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
            }
            // set state icon
            switch (recording.getState()) {
                case IDLE:
                    stateIcon.setIcon(DesktopUI.icons.get(DesktopUI.UIIcon.IDLE));
                    setBackgroundColor(isSelected ? color_idle_selected : color_idle);
                    setProgress(-1);
                    this.progressBar.setVisible(false);
                    setStatus(Constants.getInstance().getLocalizedString("Idle"));
                    break;
                case RECIEVING:
                    stateIcon.setIcon(DesktopUI.icons.get(DesktopUI.UIIcon.RECIEVING));
                    setBackgroundColor(isSelected ? color_recieving_selected : color_recieving);
                    setProgress(-1);
                    this.progressBar.setVisible(false);
                    setStatus(Constants.getInstance().getLocalizedString("DataBeingWritten"));
                    break;
                case SCHEDULED:
                    if (recording.getTrim()) {
                        stateIcon.setIcon(DesktopUI.icons.get(DesktopUI.UIIcon.SCHEDULED_TRIM));
                    } else {
                        stateIcon.setIcon(DesktopUI.icons.get(DesktopUI.UIIcon.SCHEDULED));
                    }
                    setBackgroundColor(isSelected ? color_scheduled_selected : color_scheduled);
                    setProgress(-1);
                    this.progressBar.setVisible(true);
                    setStatus(Constants.getInstance().getLocalizedString("ScheduledForUpload"));
                    break;
                case INPROGRESS:
                    if (recording.getTrim()) {
                        stateIcon.setIcon(DesktopUI.icons.get(DesktopUI.UIIcon.UPLOADING_TRIM));
                    } else {
                        stateIcon.setIcon(DesktopUI.icons.get(DesktopUI.UIIcon.UPLOADING));
                    }
                    setBackgroundColor(isSelected ? color_uploading_selected : color_uploading);
                    setStatus(recording.getIngestStatus());
                    setProgress(recording.getUploadProgress());
                    break;
                case PAUSED:
                    stateIcon.setIcon(DesktopUI.icons.get(DesktopUI.UIIcon.PAUSE));
                    setBackgroundColor(isSelected ? color_pause_selected : color_pause);
                    setStatus(Constants.getInstance().getLocalizedString("Paused"));
                    break;
                case COMPLETE:
                    stateIcon.setIcon(DesktopUI.icons.get(DesktopUI.UIIcon.COMPLETE));
                    setBackgroundColor(isSelected ? color_complete_selected : color_complete);
                    this.progressBar.setValue(100);
                    this.progressBar.setVisible(false);
                    setStatus(Constants.getInstance().getLocalizedString("SuccessfullyUploaded"));
                    break;
                case FAILED:
                    stateIcon.setIcon(DesktopUI.icons.get(DesktopUI.UIIcon.ERROR));
                    setBackgroundColor(isSelected ? color_error_selected : color_error);
                    setProgress(-1);
                    this.progressBar.setVisible(false);
                    String errMsg = recording.getErrorMessage();
                    if ((errMsg != null) && !errMsg.isEmpty()) {
                        setStatus(errMsg);
                    } else {
                        setStatus(Constants.getInstance().getLocalizedString("UploadFailed"));
                    }
                    break;
            }
        }
    }

    private void setStatus(String status) {
        if (recording != null) {
            label_status.setText((!status.isEmpty() && !status.equals("")) ? status : "-");
            if (recording.getState().equals(State.INPROGRESS)) {
                String ingestDetails = recording.getIngestDetails();
                ingestDetails = (!ingestDetails.isEmpty() && !ingestDetails.equals("")) ? (", " + recording.getIngestDetails()) : "";
                label_details.setText(
                        Constants.getInstance().getLocalizedString("File") + ": "
                        + (recording.getFiles().length - ((RecordingImpl) recording).getUnfinishedFiles().size())
                        + "/"
                        + recording.getFiles().length
                        + ingestDetails);
            } else if (recording.getState().equals(State.COMPLETE)) {
                String ingestDetails = recording.getIngestDetails();
                if (!ingestDetails.isEmpty() && !ingestDetails.equals("")) {
                    label_details.setText(ingestDetails);
                } else {
                    label_details.setText("-");
                }
            } else {
                label_details.setText("-");
            }
        } else {
            label_details.setText("-");
        }
    }

    private void setProgress(int percentage) {
        progressBar.setEnabled(true);
        if (percentage == -1) {
            progressBar.setIndeterminate(true);
            progressBar.setString("");
            progressBar.setStringPainted(false);
        } else {
            progressBar.setIndeterminate(false);
            progressBar.setValue(percentage);
            progressBar.setString(String.valueOf(percentage)
                    + " %");
            progressBar.setStringPainted(true);
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

        panel_main = new javax.swing.JPanel();
        titleText = new javax.swing.JLabel();
        noOfFilesText = new javax.swing.JLabel();
        label_noOfFiles = new javax.swing.JLabel();
        seriesText = new javax.swing.JLabel();
        label_series = new javax.swing.JLabel();
        workflowText = new javax.swing.JLabel();
        label_workflow = new javax.swing.JLabel();
        statusText = new javax.swing.JLabel();
        label_status = new javax.swing.JLabel();
        detailsText = new javax.swing.JLabel();
        label_details = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        stateIcon = new javax.swing.JLabel();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setMinimumSize(new java.awt.Dimension(498, 165));
        setPreferredSize(new java.awt.Dimension(498, 165));
        setLayout(new java.awt.GridBagLayout());

        panel_main.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel_main.setLayout(new java.awt.GridBagLayout());

        titleText.setFont(new java.awt.Font("Times New Roman", 1, 13)); // NOI18N
        titleText.setText(this.title);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        panel_main.add(titleText, gridBagConstraints);

        noOfFilesText.setText("Number of files:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        panel_main.add(noOfFilesText, gridBagConstraints);

        label_noOfFiles.setText("-");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        panel_main.add(label_noOfFiles, gridBagConstraints);

        seriesText.setText("Series:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        panel_main.add(seriesText, gridBagConstraints);

        label_series.setText("-");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        panel_main.add(label_series, gridBagConstraints);

        workflowText.setText("Workflow:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        panel_main.add(workflowText, gridBagConstraints);

        label_workflow.setText("-");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        panel_main.add(label_workflow, gridBagConstraints);

        statusText.setFont(new java.awt.Font("Times New Roman", 0, 12)); // NOI18N
        statusText.setText("Status:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        panel_main.add(statusText, gridBagConstraints);

        label_status.setText("-");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        panel_main.add(label_status, gridBagConstraints);

        detailsText.setFont(new java.awt.Font("Times New Roman", 0, 12)); // NOI18N
        detailsText.setText("Details:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 5);
        panel_main.add(detailsText, gridBagConstraints);

        label_details.setText("-");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        panel_main.add(label_details, gridBagConstraints);

        progressBar.setFont(new java.awt.Font("Times New Roman", 0, 12)); // NOI18N
        progressBar.setEnabled(false);
        progressBar.setString("");
        progressBar.setStringPainted(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 5);
        panel_main.add(progressBar, gridBagConstraints);

        stateIcon.setIcon(DesktopUI.icons.get(DesktopUI.UIIcon.IDLE));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 5);
        panel_main.add(stateIcon, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(panel_main, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel detailsText;
    private javax.swing.JLabel label_details;
    private javax.swing.JLabel label_noOfFiles;
    private javax.swing.JLabel label_series;
    private javax.swing.JLabel label_status;
    private javax.swing.JLabel label_workflow;
    private javax.swing.JLabel noOfFilesText;
    private javax.swing.JPanel panel_main;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel seriesText;
    private javax.swing.JLabel stateIcon;
    private javax.swing.JLabel statusText;
    private javax.swing.JLabel titleText;
    private javax.swing.JLabel workflowText;
    // End of variables declaration//GEN-END:variables
}
