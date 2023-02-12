/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.gui;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import de.calltopower.mhri.application.api.Recording;
import de.calltopower.mhri.application.api.RecordingFile;
import de.calltopower.mhri.util.Constants;

/**
 * FlavorEditor
 *
 * @date 22.10.2013
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class FlavorEditor extends javax.swing.JFrame {

    private boolean constructedSuccessfully = false;
    private JPopupMenu recItemMenu;
    private JCheckBoxMenuItem recItemMenuItemFlavorUnknown;
    private JCheckBoxMenuItem recItemMenuItemFlavorPresenter;
    private JCheckBoxMenuItem recItemMenuItemFlavorPresentation;
    private RecordingFile selectedRecordingFile = null;
    private final Recording rec;

    public FlavorEditor(Recording rec) {
        this.rec = rec;

        int nrOfAllowedFlavors = 0;
        if (this.rec != null) {
            for (RecordingFile rf : this.rec.getFiles()) {
                switch (rf.getFlavor()) {
                    case Constants.FLAVOR_ATTACHMENT_UNKNOWN:
                    case Constants.FLAVOR_PRESENTER:
                    case Constants.FLAVOR_PRESENTATION:
                        ++nrOfAllowedFlavors;
                        break;
                }
            }
        }
        if (nrOfAllowedFlavors <= 0) {
            JOptionPane.showMessageDialog(
                    this,
                    Constants.getInstance().getLocalizedString("NoRecordingsWithKnownFlavor_msg"),
                    Constants.getInstance().getLocalizedString("NoRecordingsWithKnownFlavor"),
                    JOptionPane.ERROR_MESSAGE);
            this.dispose();
        } else {
            constructedSuccessfully = true;

            initComponents();

            this.setTitle(Constants.getInstance().getLocalizedString("FlavorEditor"));

            this.setLocationRelativeTo(null);


            recItemMenu = new JPopupMenu();
            recItemMenuItemFlavorUnknown = new JCheckBoxMenuItem(Constants.FLAVOR_ATTACHMENT_UNKNOWN, false);
            recItemMenuItemFlavorPresenter = new JCheckBoxMenuItem(Constants.FLAVOR_PRESENTER, false);
            recItemMenuItemFlavorPresentation = new JCheckBoxMenuItem(Constants.FLAVOR_PRESENTATION, false);
            recItemMenuItemFlavorUnknown.addMouseListener(
                    new MouseAdapter() {
                @Override
                public void mouseReleased(java.awt.event.MouseEvent evt) {
                    if (selectedRecordingFile != null) {
                        selectedRecordingFile.setFlavor(Constants.FLAVOR_ATTACHMENT_UNKNOWN);
                        update(Constants.FLAVOR_ATTACHMENT_UNKNOWN);
                    }
                }
            });
            recItemMenuItemFlavorPresenter.addMouseListener(
                    new MouseAdapter() {
                @Override
                public void mouseReleased(java.awt.event.MouseEvent evt) {
                    if (selectedRecordingFile != null) {
                        selectedRecordingFile.setFlavor(Constants.FLAVOR_PRESENTER);
                        update(Constants.FLAVOR_PRESENTER);
                    }
                }
            });
            recItemMenuItemFlavorPresentation.addMouseListener(
                    new MouseAdapter() {
                @Override
                public void mouseReleased(java.awt.event.MouseEvent evt) {
                    if (selectedRecordingFile != null) {
                        selectedRecordingFile.setFlavor(Constants.FLAVOR_PRESENTATION);
                        update(Constants.FLAVOR_PRESENTATION);
                    }
                }
            });
            recItemMenu.add(recItemMenuItemFlavorUnknown);
            recItemMenu.add(recItemMenuItemFlavorPresenter);
            recItemMenu.add(recItemMenuItemFlavorPresentation);

            init();

            this.jPanel6.requestFocus();

            final JFrame win = this;
            this.jPanel6.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(java.awt.event.KeyEvent evt) {
                    if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        win.dispose();
                    }
                }
            });
        }
    }

    public boolean constructedSuccessfully() {
        return constructedSuccessfully;
    }

    private void checkRecordingContextMenu() {
        switch (selectedRecordingFile.getFlavor()) {
            case Constants.FLAVOR_ATTACHMENT_UNKNOWN:
                recItemMenuItemFlavorUnknown.setSelected(true);
                recItemMenuItemFlavorPresenter.setSelected(false);
                recItemMenuItemFlavorPresentation.setSelected(false);
                break;
            case Constants.FLAVOR_PRESENTER:
                recItemMenuItemFlavorUnknown.setSelected(false);
                recItemMenuItemFlavorPresenter.setSelected(true);
                recItemMenuItemFlavorPresentation.setSelected(false);
                break;
            case Constants.FLAVOR_PRESENTATION:
                recItemMenuItemFlavorUnknown.setSelected(false);
                recItemMenuItemFlavorPresenter.setSelected(false);
                recItemMenuItemFlavorPresentation.setSelected(true);
                break;
        }

        int nrOfPresentations = 0;
        if (this.rec != null) {
            for (RecordingFile rf : rec.getFiles()) {
                if (rf.getFlavor().equals(Constants.FLAVOR_PRESENTATION)) {
                    ++nrOfPresentations;
                }
            }
        }
        if (nrOfPresentations > 0) {
            recItemMenuItemFlavorPresentation.setEnabled(false);
        } else {
            recItemMenuItemFlavorPresentation.setEnabled(true);
        }
    }

    private void update(String flavor) {
        for (Object o : this.panel_insert.getComponents()) {
            if (o instanceof RecordingItem) {
                RecordingItem ri = (RecordingItem) o;
                if (selectedRecordingFile.getPath().equals(ri.getPath())) {
                    ri.setFlavor(flavor);
                    break;
                }
            }
        }
    }

    private void checkSelected() {
        for (Object o : this.panel_insert.getComponents()) {
            if (o instanceof RecordingItem) {
                RecordingItem ri = (RecordingItem) o;
                if (selectedRecordingFile.getPath().equals(ri.getPath())) {
                    ri.setSelected(true);
                } else {
                    ri.setSelected(false);
                }
            }
        }
    }

    private void init() {
        if (this.rec != null) {
            for (final RecordingFile rf : rec.getFiles()) {
                if (rf.getFlavor().equals(Constants.FLAVOR_ATTACHMENT_UNKNOWN)
                        || rf.getFlavor().equals(Constants.FLAVOR_PRESENTER)
                        || rf.getFlavor().equals(Constants.FLAVOR_PRESENTATION)) {
                    final RecordingItem ri = new RecordingItem(rf.getPath(), rf.getFlavor());
                    ri.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseReleased(java.awt.event.MouseEvent evt) {
                            selectedRecordingFile = rf;
                            checkSelected();
                            if (SwingUtilities.isRightMouseButton(evt)) {
                                checkRecordingContextMenu();
                                recItemMenu.show(ri, evt.getX(), evt.getY());
                            }
                        }
                    });
                    this.panel_insert.add(ri);
                }
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

        jPanel6 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        panel_insert = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Flavor editor");
        setMinimumSize(new java.awt.Dimension(300, 300));
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));

        jPanel6.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));
        jPanel6.setLayout(new javax.swing.BoxLayout(jPanel6, javax.swing.BoxLayout.Y_AXIS));

        jScrollPane1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        panel_insert.setLayout(new java.awt.GridLayout(3, 0));
        jScrollPane1.setViewportView(panel_insert);

        jPanel6.add(jScrollPane1);

        getContentPane().add(jPanel6);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel panel_insert;
    // End of variables declaration//GEN-END:variables
}
