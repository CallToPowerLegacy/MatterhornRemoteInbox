/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.gui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import de.calltopower.mhri.util.Constants;

/**
 * Update
 *
 * @date 05.07.2013
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class Update extends javax.swing.JFrame {

    private static final Logger logger = Logger.getLogger(Update.class);
    private final String currVersion = "v" + Constants.MHRI_VERSION + " build " + Constants.MHRI_BUILD;
    private String newVersion = currVersion;
    private final Color bgColor = new Color(200, 200, 200);

    public Update() {
        initComponents();

        this.setTitle(Constants.getInstance().getLocalizedString("UpdateAvailableShort"));
        this.label_mhri.setText(" " + Constants.getInstance().getLocalizedString("UpdateAvailable"));
        this.label_version.setText(Constants.getInstance().getLocalizedString("CurrentVersion"));
        this.label_avail_version.setText(Constants.getInstance().getLocalizedString("AvailableVersion"));
        this.label_icons_link.setText(Constants.getInstance().getLocalizedString("ClickToDownload"));

        this.label_version_no.setText(currVersion);
        this.label_mhriLogo.setText("");
        this.label_avail_version_no.setText(currVersion);

        this.setLocationRelativeTo(null);

        panelMain.requestFocus();

        ArrayList<Container> containerList = new ArrayList<>();
        containerList.add(panelMain);
        containerList.add(panel0);
        containerList.add(panel1);
        containerList.add(panel2);
        containerList.add(panel3);
        containerList.add(panel4);
        for (Container c : containerList) {
            c.setBackground(bgColor);
            final JFrame win = this;
            c.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent evt) {
                    if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        win.dispose();
                    }
                }
            });
        }
    }

    public void setNewVersion(String _newVersion) {
        newVersion = _newVersion;
        this.label_avail_version_no.setText(newVersion);
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

        panelMain = new javax.swing.JPanel();
        panel0 = new javax.swing.JPanel();
        panel1 = new javax.swing.JPanel();
        ImageIcon ii = null;
        try {
            BufferedImage bi = ImageIO.read(getClass().getResourceAsStream("/ui/matterhorn-icon.png"));
            ii = new ImageIcon(bi);
        } catch (IOException ex) {
            logger.error("About::Constructor - IOException: " + ex.getMessage());
        }
        if(ii != null) {
            label_mhriLogo = new javax.swing.JLabel(ii);
            label_mhri = new javax.swing.JLabel();
            panel2 = new javax.swing.JPanel();
            label_version = new javax.swing.JLabel();
            label_version_no = new javax.swing.JLabel();
            panel3 = new javax.swing.JPanel();
            label_avail_version = new javax.swing.JLabel();
            label_avail_version_no = new javax.swing.JLabel();
            panel4 = new javax.swing.JPanel();
            label_icons_link = new javax.swing.JLabel();

            setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
            setTitle("Update available");
            setAlwaysOnTop(true);
            setMinimumSize(new java.awt.Dimension(455, 237));
            setName("about"); // NOI18N
            setUndecorated(true);
            setResizable(false);
            getContentPane().setLayout(new java.awt.GridBagLayout());

            panelMain.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
            panelMain.setLayout(new java.awt.GridBagLayout());

            panel0.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));
            panel0.setAlignmentX(0.0F);
            panel0.setAlignmentY(0.0F);
            panel0.setLayout(new java.awt.GridBagLayout());

            panel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));

        } else {
            label_mhriLogo = new javax.swing.JLabel();
        }
        label_mhriLogo.setText("MHRI");
        panel1.add(label_mhriLogo);

        label_mhri.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        label_mhri.setText(" An update is available");
        panel1.add(label_mhri);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 0);
        panel0.add(panel1, gridBagConstraints);

        panel2.setLayout(new java.awt.GridBagLayout());

        label_version.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        label_version.setText("Current version ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        panel2.add(label_version, gridBagConstraints);

        label_version_no.setText("VERSION");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        panel2.add(label_version_no, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 0);
        panel0.add(panel2, gridBagConstraints);

        panel3.setLayout(new java.awt.GridBagLayout());

        label_avail_version.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        label_avail_version.setText("Available version");
        label_avail_version.setAlignmentX(0.5F);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        panel3.add(label_avail_version, gridBagConstraints);

        label_avail_version_no.setText("AVAIL_VERSION");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        panel3.add(label_avail_version_no, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 0);
        panel0.add(panel3, gridBagConstraints);

        panel4.setLayout(new java.awt.GridBagLayout());

        label_icons_link.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        label_icons_link.setForeground(new java.awt.Color(0, 51, 255));
        label_icons_link.setText("Click here to download the latest version");
        label_icons_link.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                label_icons_linkMouseClicked(evt);
            }
        });
        panel4.add(label_icons_link, new java.awt.GridBagConstraints());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        panel0.add(panel4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panelMain.add(panel0, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(panelMain, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void label_icons_linkMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_label_icons_linkMouseClicked
        if (Desktop.isDesktopSupported()) {
            try {
                URI uri = new URI("http://zentrum.virtuos.uni-osnabrueck.de/mhri/#download");
                Desktop.getDesktop().browse(uri);
            } catch (URISyntaxException ex) {
                logger.error("About::label_icons_linkMouseClicked - URISyntaxException" + ex.getMessage());
            } catch (IOException ex) {
                logger.error("About::label_icons_linkMouseClicked - IOException" + ex.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "http://zentrum.virtuos.uni-osnabrueck.de/mhri/#download",
                    "http://zentrum.virtuos.uni-osnabrueck.de/mhri/#download",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        this.dispose();
    }//GEN-LAST:event_label_icons_linkMouseClicked
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel label_avail_version;
    private javax.swing.JLabel label_avail_version_no;
    private javax.swing.JLabel label_icons_link;
    private javax.swing.JLabel label_mhri;
    private javax.swing.JLabel label_mhriLogo;
    private javax.swing.JLabel label_version;
    private javax.swing.JLabel label_version_no;
    private javax.swing.JPanel panel0;
    private javax.swing.JPanel panel1;
    private javax.swing.JPanel panel2;
    private javax.swing.JPanel panel3;
    private javax.swing.JPanel panel4;
    private javax.swing.JPanel panelMain;
    // End of variables declaration//GEN-END:variables
}
