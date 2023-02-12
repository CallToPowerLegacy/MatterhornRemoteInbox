/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.gui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import org.apache.log4j.Logger;
import de.calltopower.mhri.util.Constants;

/**
 * CreateSeries
 *
 * @date 11.10.2013
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class CreateSeries extends javax.swing.JFrame {

    private static final Logger logger = Logger.getLogger(CreateSeries.class);

    public CreateSeries() {
        initComponents();

        this.label_imgInfo.setText("");
        
        this.setTitle(Constants.getInstance().getLocalizedString("CreateSeries"));
        this.jLabel2.setText(Constants.getInstance().getLocalizedString("UpdatingSeriesInformation"));
        
        this.setLocationRelativeTo(null);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        ImageIcon ii = null;
        try {
            BufferedImage bi = ImageIO.read(getClass().getResourceAsStream("/ui/matterhorn-icon-sending.png"));
            ii = new ImageIcon(bi);
        } catch (IOException ex) {
            logger.error("CreateSeries::Constructor - IOException: " + ex.getMessage());
            label_imgInfo.setVisible(false);
        }
        if(ii != null) {
            label_imgInfo = new javax.swing.JLabel();
            jLabel2 = new javax.swing.JLabel();

            setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
            setTitle("Create series");
            setAlwaysOnTop(true);
            setMinimumSize(new java.awt.Dimension(361, 119));
            setModalExclusionType(java.awt.Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
            setUndecorated(true);
            setResizable(false);
            getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.LINE_AXIS));

            jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));

            label_imgInfo.setIcon(ii);
            label_imgInfo.setText("LOGO");
            jPanel1.add(label_imgInfo);
        } else {
            label_imgInfo = new javax.swing.JLabel();
            label_imgInfo.setVisible(false);
        }

        jLabel2.setText("Updating series information...");
        jPanel1.add(jLabel2);

        getContentPane().add(jPanel1);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel label_imgInfo;
    // End of variables declaration//GEN-END:variables
}