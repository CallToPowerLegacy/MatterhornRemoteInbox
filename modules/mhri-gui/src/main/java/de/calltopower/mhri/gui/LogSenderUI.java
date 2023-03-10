/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.gui;

import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedList;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import org.apache.log4j.Logger;
import de.calltopower.mhri.util.Constants;

/**
 * LogSenderUI
 *
 * @date 11.07.2013
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class LogSenderUI extends javax.swing.JFrame {

    private static final Logger logger = Logger.getLogger(LogSenderUI.class);
    private final MHRIFocusTraversalPolicy traversalPolicy;
    private final DesktopUI ui;

    public LogSenderUI(DesktopUI ui) {
        this.ui = ui;

        initComponents();

        this.setTitle(Constants.getInstance().getLocalizedString("SendLogFile"));
        this.label_text.setText(Constants.getInstance().getLocalizedString("ManualDescription") + ":");
        this.button_send_log.setText(Constants.getInstance().getLocalizedString("SendLogFile"));
        this.button_send_log.setToolTipText(Constants.getInstance().getLocalizedString("InfoSendLogFile"));

        LinkedList<Component> componentList = new LinkedList<>();
        componentList.add(textarea_manual_description);
        componentList.add(button_send_log);
        traversalPolicy = new MHRIFocusTraversalPolicy(componentList);
        this.setFocusTraversalPolicy(traversalPolicy);

        this.progressbar.setVisible(false);
        this.setLocationRelativeTo(null);
        loadValues();
        this.jPanel4.requestFocus();

        final JFrame win = this;
        this.jPanel4.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    win.dispose();
                }
            }
        });
    }

    public final void loadValues() {
    }

    public void setUploading(boolean uploading) {
        if (uploading) {
            this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        } else {
            this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        }

        this.button_send_log.setVisible(!uploading);
        this.progressbar.setVisible(uploading);
        this.button_send_log.setEnabled(!uploading);
        this.textarea_manual_description.setEnabled(!uploading);

        if (uploading) {
            this.progressbar.setIndeterminate(true);
        } else {
            this.textarea_manual_description.setText("");
            this.jPanel4.requestFocus();
        }
    }

    private String getManualDescription() {
        String ret = this.textarea_manual_description.getText().trim();
        ret = ((ret == null) || ret.isEmpty()) ? "" : ret;
        return ret;
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

        jPanel4 = new javax.swing.JPanel();
        label_text = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        textarea_manual_description = new javax.swing.JTextArea();
        ImageIcon ii = null;
        try {
            BufferedImage bi = ImageIO.read(getClass().getResourceAsStream("/ui/send_log.png"));
            ii = new ImageIcon(bi);
        } catch (IOException ex) {
            logger.error("LogSenderUI::Constructor - IOException: " + ex.getMessage());
        }
        if(ii != null) {
            button_send_log = new javax.swing.JButton(ii);
            progressbar = new javax.swing.JProgressBar();

            setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
            setTitle("Send log file");
            setAlwaysOnTop(true);
            setMinimumSize(new java.awt.Dimension(488, 304));
            getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));

            jPanel4.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));
            jPanel4.setVerifyInputWhenFocusTarget(false);
            jPanel4.setLayout(new java.awt.GridBagLayout());

            label_text.setText("Manual description [optional]:");
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
            gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
            jPanel4.add(label_text, gridBagConstraints);

            textarea_manual_description.setColumns(20);
            textarea_manual_description.setRows(5);
            jScrollPane1.setViewportView(textarea_manual_description);

            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
            jPanel4.add(jScrollPane1, gridBagConstraints);

        } else {
            button_send_log = new javax.swing.JButton();
        }
        button_send_log.setText("Send log file");
        button_send_log.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_send_logActionPerformed(evt);
            }
        });
        button_send_log.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                button_send_logKeyPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel4.add(button_send_log, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        jPanel4.add(progressbar, gridBagConstraints);

        getContentPane().add(jPanel4);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void button_send_logActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_send_logActionPerformed
        if (ui != null) {
            ui.sendLogFile(false, getManualDescription());
        }
    }//GEN-LAST:event_button_send_logActionPerformed

    private void button_send_logKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_button_send_logKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            button_send_logActionPerformed(null);
        }
    }//GEN-LAST:event_button_send_logKeyPressed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton button_send_log;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel label_text;
    private javax.swing.JProgressBar progressbar;
    private javax.swing.JTextArea textarea_manual_description;
    // End of variables declaration//GEN-END:variables
}
