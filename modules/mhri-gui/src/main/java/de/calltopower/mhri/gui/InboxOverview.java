/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.gui;

import java.awt.Desktop;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.apache.log4j.Logger;
import de.calltopower.mhri.application.api.Inbox;
import de.calltopower.mhri.application.api.Recording;
import de.calltopower.mhri.application.api.Recording.State;
import de.calltopower.mhri.application.api.RecordingFile;
import de.calltopower.mhri.application.impl.RecordingImpl;
import de.calltopower.mhri.application.impl.RecordingListModelImpl;
import de.calltopower.mhri.application.impl.RemoteInboxApplicationImpl;
import de.calltopower.mhri.application.impl.SpecificFileUtils;
import de.calltopower.mhri.gui.DesktopUI.UIIcon;
import de.calltopower.mhri.util.Constants;
import de.calltopower.mhri.util.MHRIFileUtils;

/**
 * InboxOverview
 *
 * @date unknown
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class InboxOverview extends JFrame implements ListSelectionListener {

    private static final Logger logger = Logger.getLogger(InboxOverview.class);
    private final RemoteInboxApplicationImpl application;
    private final ListModel inboxListModel;
    private final RecordingListModelImpl recordingListModel;
    private final String[] mediafileSuffixes;
    private InboxInformation inboxInformation = null;
    private SeriesEditor seriesEditor = null;
    private EpisodeEditor episodeEditor = null;
    private FlavorEditor flavorEditor = null;
    private int selectedRecordingIndex = -1;
    private final JPopupMenu inboxMenu;
    private final JMenuItem inboxMenuItemInformation;
    private final JMenuItem inboxMenuItemSeriesEditor;
    private final JMenuItem inboxMenuItemOpenIn;
    private final JMenuItem inboxMenuItemScheduleIngestForAll;
    private final JMenuItem inboxMenuItemStopIngestForAll;
    private final JMenuItem inboxMenuItemSelectSeries;
    private final JMenuItem inboxMenuItemSelectWorkflow;
    private final JMenuItem inboxMenuItemTrim;
    private final JMenuItem inboxMenuItemSceneDetection;
    private final JMenuItem inboxMenuItemNew;
    private final JMenuItem inboxMenuItemDelete;
    private final JPopupMenu recordingMenu;
    private final JMenuItem recordingMenuItemOpenInBrowser;
    private final JMenuItem recordingMenuItemOpenIn;
    private final JMenuItem recordingMenuItemUpload;
    private JCheckBoxMenuItem recordingMenuItemUploadTrim;
    private final JCheckBoxMenuItem recordingMenuItemUploadSceneDetection;
    private final JMenuItem recordingMenuItemFlavorEditor;
    private final JMenuItem recordingMenuItemEpisodeEditor;
    private final JMenuItem recordingMenuItemSelectWorkflow;
    private final JMenuItem recordingMenuItemStop;
    private final JMenuItem recordingMenuItemMarkIdle;
    private final JMenuItem recordingMenuItemMarkComplete;
    private final JMenuItem recordingMenuItemMarkFailed;
    private final JMenuItem recordingMenuItemDelete;
    private Icon icon;
    private int currSelectedIndexP;

    private void checkTooltips() {
        if (Boolean.parseBoolean(application.getConfig().get(Constants.PROPKEY_SHOW_TOOLTIPS))) {
            inboxList.setToolTipText(Constants.getInstance().getLocalizedString("ToolTipRightClickInbox"));
            recordingList.setToolTipText(Constants.getInstance().getLocalizedString("ToolTipRightClickRecording"));
            buttonInboxInfo.setToolTipText(Constants.getInstance().getLocalizedString("InboxInformation"));
        } else {
            inboxList.setToolTipText(null);
            recordingList.setToolTipText(null);
            buttonInboxInfo.setToolTipText(null);
        }
    }

    public InboxOverview(final RemoteInboxApplicationImpl application) {
        this.application = application;
        inboxListModel = application.getListModelAdapter().getInboxListModel();
        recordingListModel = (RecordingListModelImpl) application.getListModelAdapter().getRecordingListModel();
        mediafileSuffixes = application.getConfig().get(Constants.PROPKEY_MEDIAFILE_SUFFIXES).split(",");

        this.setTitle(Constants.getInstance().getLocalizedString("MatterhornRemoteInbox"));

        icon = null;
        try {
            BufferedImage bi = ImageIO.read(getClass().getResourceAsStream("/ui/ingest_delete.png"));
            icon = new ImageIcon(bi);
        } catch (IOException ex) {
        }

        initComponents();
        this.setLocationRelativeTo(null);

        checkTooltips();

        inboxList.addListSelectionListener(this);
        recordingList.addListSelectionListener(this);

        recordingListModel.filter(null);

        this.buttonInboxInfo.setEnabled(false);

        toolBar.setFloatable(false);

        this.inboxList.requestFocus();

        inboxMenu = new JPopupMenu();
        inboxMenuItemInformation = new JMenuItem(Constants.getInstance().getLocalizedString("InboxInformation"), DesktopUI.icons.get(UIIcon.INBOX_INFO));
        inboxMenuItemSeriesEditor = new JMenuItem(Constants.getInstance().getLocalizedString("SeriesEditor"), DesktopUI.icons.get(UIIcon.SERIES_EDITOR));
        inboxMenuItemOpenIn = new JMenuItem(Constants.getInstance().getLocalizedString("OpenIn"), DesktopUI.icons.get(UIIcon.OPEN_IN));
        inboxMenuItemScheduleIngestForAll = new JMenuItem(Constants.getInstance().getLocalizedString("ScheduleIngestForAll"), DesktopUI.icons.get(UIIcon.SCHEDULED));
        inboxMenuItemStopIngestForAll = new JMenuItem(Constants.getInstance().getLocalizedString("StopIngestForAll"), DesktopUI.icons.get(UIIcon.STOP));
        inboxMenuItemSelectSeries = new JMenuItem(Constants.getInstance().getLocalizedString("AssignSeries"), DesktopUI.icons.get(UIIcon.SERIES));
        inboxMenuItemSelectWorkflow = new JMenuItem(Constants.getInstance().getLocalizedString("SetWorkflow"), DesktopUI.icons.get(UIIcon.WORKFLOW));
        inboxMenuItemTrim = new JMenuItem(Constants.getInstance().getLocalizedString("Trim"), DesktopUI.icons.get(UIIcon.SCHEDULED_TRIM));
        inboxMenuItemSceneDetection = new JMenuItem(Constants.getInstance().getLocalizedString("SceneDetection"), DesktopUI.icons.get(UIIcon.SCENE_DETECTION));
        inboxMenuItemNew = new JMenuItem(Constants.getInstance().getLocalizedString("CreateNewInbox"), DesktopUI.icons.get(UIIcon.INBOX_CREATE));
        inboxMenuItemDelete = new JMenuItem(Constants.getInstance().getLocalizedString("Delete"), DesktopUI.icons.get(UIIcon.INBOX_DELETE));
        inboxMenuItemNew.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                inboxCreate();
            }
        });
        inboxMenuItemInformation.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                inboxInformation();
            }
        });
        inboxMenuItemSeriesEditor.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                seriesEditor(true);
            }
        });
        inboxMenuItemOpenIn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                openIn(false);
            }
        });
        inboxMenuItemScheduleIngestForAll.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                scheduleIngestForAll();
            }
        });
        inboxMenuItemStopIngestForAll.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                stopIngestForAll();
            }
        });
        inboxMenuItemSelectSeries.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                selectSeries();
            }
        });
        inboxMenuItemSelectWorkflow.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                selectWorkflow(true);
            }
        });
        inboxMenuItemTrim.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                trim();
            }
        });
        inboxMenuItemSceneDetection.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                sceneDetectionForAll();
            }
        });
        inboxMenuItemDelete.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                inboxDelete();
            }
        });
        inboxMenu.add(inboxMenuItemInformation);
        inboxMenu.add(inboxMenuItemSeriesEditor);
        inboxMenu.addSeparator();
        if (Desktop.isDesktopSupported()) {
            inboxMenu.add(inboxMenuItemOpenIn);
            inboxMenu.addSeparator();
        }
        inboxMenu.add(inboxMenuItemScheduleIngestForAll);
        inboxMenu.add(inboxMenuItemStopIngestForAll);
        inboxMenu.addSeparator();
        inboxMenu.add(inboxMenuItemSelectSeries);
        inboxMenu.add(inboxMenuItemSelectWorkflow);
        inboxMenu.addSeparator();
        inboxMenu.add(inboxMenuItemTrim);
        inboxMenu.add(inboxMenuItemSceneDetection);
        inboxMenu.addSeparator();
        inboxMenu.add(inboxMenuItemNew);
        inboxMenu.add(inboxMenuItemDelete);

        recordingMenu = new JPopupMenu();
        recordingMenuItemOpenInBrowser = new JMenuItem(Constants.getInstance().getLocalizedString("OpenInBrowser"), DesktopUI.icons.get(UIIcon.MATTERHORN));
        recordingMenuItemOpenIn = new JMenuItem(Constants.getInstance().getLocalizedString("OpenIn"), DesktopUI.icons.get(UIIcon.OPEN_IN));
        recordingMenuItemUpload = new JMenuItem(Constants.getInstance().getLocalizedString("ScheduleIngest"), DesktopUI.icons.get(UIIcon.SCHEDULED));
        recordingMenuItemUploadTrim = new JCheckBoxMenuItem(Constants.getInstance().getLocalizedString("Trim"), false);
        recordingMenuItemUploadTrim.setIcon(DesktopUI.icons.get(UIIcon.SCHEDULED_TRIM));
        recordingMenuItemUploadSceneDetection = new JCheckBoxMenuItem(Constants.getInstance().getLocalizedString("SceneDetection"), false);
        recordingMenuItemUploadSceneDetection.setIcon(DesktopUI.icons.get(UIIcon.SCENE_DETECTION));
        recordingMenuItemFlavorEditor = new JMenuItem(Constants.getInstance().getLocalizedString("FlavorEditor"), DesktopUI.icons.get(UIIcon.FLAVOR_EDITOR));
        recordingMenuItemEpisodeEditor = new JMenuItem(Constants.getInstance().getLocalizedString("EpisodeEditor"), DesktopUI.icons.get(UIIcon.EPISODE_EDITOR));
        recordingMenuItemSelectWorkflow = new JMenuItem(Constants.getInstance().getLocalizedString("SetWorkflow"), DesktopUI.icons.get(UIIcon.WORKFLOW));
        recordingMenuItemStop = new JMenuItem(Constants.getInstance().getLocalizedString("StopIngest"), DesktopUI.icons.get(UIIcon.STOP));
        recordingMenuItemMarkIdle = new JMenuItem(Constants.getInstance().getLocalizedString("MarkAsIdle") + " ('1')", DesktopUI.icons.get(UIIcon.IDLE));
        recordingMenuItemMarkComplete = new JMenuItem(Constants.getInstance().getLocalizedString("MarkAsComplete") + " ('2')", DesktopUI.icons.get(UIIcon.COMPLETE));
        recordingMenuItemMarkFailed = new JMenuItem(Constants.getInstance().getLocalizedString("MarkAsFailed") + " ('3')", DesktopUI.icons.get(UIIcon.ERROR));
        recordingMenuItemDelete = new JMenuItem(Constants.getInstance().getLocalizedString("Delete"), DesktopUI.icons.get(UIIcon.DELETE));

        recordingMenuItemOpenInBrowser.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseReleased(java.awt.event.MouseEvent evt) {
                        openInBrowser();
                    }
                });
        recordingMenuItemOpenIn.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseReleased(java.awt.event.MouseEvent evt) {
                        openIn(true);
                    }
                });
        recordingMenuItemUpload.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseReleased(java.awt.event.MouseEvent evt) {
                        upload(null);
                    }
                });
        recordingMenuItemUploadTrim.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseReleased(java.awt.event.MouseEvent evt) {
                        try {
                            Recording recording = getSelectedRecording();
                            if ((recording != null) && (recording.getFiles().length > 0)) {
                                recording.setTrim(recordingMenuItemUploadTrim.isSelected());
                            }
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(
                                    null,
                                    Constants.getInstance().getLocalizedString("SomethingWentWrong_msg"),
                                    Constants.getInstance().getLocalizedString("SomethingWentWrong"),
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
        recordingMenuItemUploadSceneDetection.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseReleased(java.awt.event.MouseEvent evt) {
                        try {
                            Recording recording = getSelectedRecording();
                            if ((recording != null) && (recording.getFiles().length > 0)) {
                                sceneDetection(recording);
                            }
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(
                                    null,
                                    Constants.getInstance().getLocalizedString("SomethingWentWrong_msg"),
                                    Constants.getInstance().getLocalizedString("SomethingWentWrong"),
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
        recordingMenuItemFlavorEditor.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseReleased(java.awt.event.MouseEvent evt) {
                        flavorEditor();
                    }
                });
        recordingMenuItemEpisodeEditor.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseReleased(java.awt.event.MouseEvent evt) {
                        episodeEditor();
                    }
                });
        recordingMenuItemSelectWorkflow.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseReleased(java.awt.event.MouseEvent evt) {
                        selectWorkflow(false);
                    }
                });
        recordingMenuItemStop.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseReleased(java.awt.event.MouseEvent evt) {
                        stop(null);
                    }
                });
        recordingMenuItemMarkIdle.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseReleased(java.awt.event.MouseEvent evt) {
                        markAs(State.IDLE);
                    }
                });
        recordingMenuItemMarkComplete.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseReleased(java.awt.event.MouseEvent evt) {
                        markAs(State.COMPLETE);
                    }
                });
        recordingMenuItemMarkFailed.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseReleased(java.awt.event.MouseEvent evt) {
                        markAs(State.FAILED);
                    }
                });
        recordingMenuItemDelete.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseReleased(java.awt.event.MouseEvent evt) {
                        delete();
                    }
                });

        if (Desktop.isDesktopSupported()) {
            recordingMenu.add(recordingMenuItemOpenInBrowser);
            recordingMenu.add(recordingMenuItemOpenIn);
            recordingMenu.addSeparator();
        }
        recordingMenu.add(recordingMenuItemUpload);
        recordingMenu.add(recordingMenuItemStop);
        recordingMenu.addSeparator();
        recordingMenu.add(recordingMenuItemUploadTrim);
        recordingMenu.add(recordingMenuItemUploadSceneDetection);
        recordingMenu.addSeparator();
        recordingMenu.add(recordingMenuItemEpisodeEditor);
        recordingMenu.add(recordingMenuItemFlavorEditor);
        recordingMenu.add(recordingMenuItemSelectWorkflow);
        recordingMenu.addSeparator();
        recordingMenu.add(recordingMenuItemMarkIdle);
        recordingMenu.add(recordingMenuItemMarkComplete);
        recordingMenu.add(recordingMenuItemMarkFailed);
        recordingMenu.addSeparator();
        recordingMenu.add(recordingMenuItemDelete);
        recordingMenuItemOpenInBrowser.setEnabled(false);

        final JFrame win = this;
        this.toolBar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    win.setVisible(false);
                }
            }
        });
        this.splitPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    win.setVisible(false);
                }
            }
        });
    }

    private Recording getSelectedRecording() {
        int select;
        if (selectedRecordingIndex != -1) {
            select = selectedRecordingIndex;
        } else {
            select = recordingList.getSelectedIndex();
        }
        if ((select >= 0) && (select < recordingListModel.getSize())) {
            return (Recording) recordingListModel.getElementAt(select);
        } else {
            return null;
        }
    }

    private void trim() {
        try {
            Inbox inbox = (Inbox) inboxList.getSelectedValue();
            if (inbox != null) {
                boolean cont = true;
                boolean allTrimming = true;
                for (Recording r : inbox.getRecordings()) {
                    if ((r.getState().equals(State.INPROGRESS))
                            || (r.getState().equals(State.PAUSED))) {
                        cont = false;
                    }
                    allTrimming = allTrimming && r.getTrim();
                }

                String text = !allTrimming ? Constants.getInstance().getLocalizedString("TrimAllContainingsRecordings_msg") : Constants.getInstance().getLocalizedString("DontTrimAllContainingsRecordings_msg");
                String text2 = !allTrimming ? Constants.getInstance().getLocalizedString("TrimAllContainingsRecordings") : Constants.getInstance().getLocalizedString("DontTrimAllContainingsRecordings");
                if (cont && (JOptionPane.showConfirmDialog(
                        this,
                        text,
                        text2,
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)) {
                    for (Recording r : inbox.getRecordings()) {
                        r.setTrim(!allTrimming);
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    Constants.getInstance().getLocalizedString("SomethingWentWrong_msg"),
                    Constants.getInstance().getLocalizedString("SomethingWentWrong"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sceneDetectionForAll() {
        try {
            Inbox inbox = (Inbox) inboxList.getSelectedValue();
            if (inbox != null) {
                boolean cont = true;
                for (Recording r : inbox.getRecordings()) {
                    if (r.getState().equals(State.INPROGRESS)) {
                        cont = false;
                    }
                }

                if (cont && (JOptionPane.showConfirmDialog(
                        this,
                        Constants.getInstance().getLocalizedString("DetectScenesOnRecordings_msg"),
                        Constants.getInstance().getLocalizedString("DetectScenesOnRecordings"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)) {
                    for (Recording r : inbox.getRecordings()) {
                        sceneDetection(r);
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    Constants.getInstance().getLocalizedString("SomethingWentWrong_msg"),
                    Constants.getInstance().getLocalizedString("SomethingWentWrong"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sceneDetection(Recording recording) {
        try {
            if ((recording != null)
                    && ((recording.getState() == Recording.State.IDLE)
                    || recording.getState().equals(Recording.State.COMPLETE)
                    || (recording.getState() == Recording.State.FAILED)
                    || (recording.getState() == Recording.State.RECIEVING)
                    || (recording.getState() == Recording.State.PAUSED)
                    || (recording.getState() == Recording.State.SCHEDULED))) {
                int nrOfPresenters = 0;
                int nrOfPresentations = 0;
                int nrOfUnknown = 0;
                RecordingFile rf_save = null;
                for (RecordingFile rf : recording.getFiles()) {
                    switch (rf.getFlavor()) {
                        case Constants.FLAVOR_PRESENTER:
                            ++nrOfPresenters;
                            rf_save = rf;
                            break;
                        case Constants.FLAVOR_PRESENTATION:
                            ++nrOfPresentations;
                            rf_save = rf;
                            break;
                        case Constants.FLAVOR_ATTACHMENT_UNKNOWN:
                            ++nrOfUnknown;
                            rf_save = rf;
                            break;
                    }
                }
                if ((nrOfPresentations == 0)
                        && (nrOfPresenters == 1)
                        && (rf_save != null)) {
                    rf_save.setFlavor(Constants.FLAVOR_PRESENTATION);
                } else if ((nrOfPresentations == 1)
                        && (nrOfPresenters == 0)
                        && (rf_save != null)) {
                    rf_save.setFlavor(Constants.FLAVOR_PRESENTER);
                } else if ((nrOfPresentations == 0)
                        && (nrOfPresenters == 0)
                        && (nrOfUnknown == 1)
                        && (rf_save != null)) {
                    rf_save.setFlavor(Constants.FLAVOR_PRESENTATION);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    Constants.getInstance().getLocalizedString("SomethingWentWrong_msg"),
                    Constants.getInstance().getLocalizedString("SomethingWentWrong"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void inboxCreate() {
        try {
            String name = JOptionPane.showInputDialog(
                    this,
                    Constants.getInstance().getLocalizedString("EnterInboxName_msg") + ":",
                    Constants.getInstance().getLocalizedString("EnterInboxName"),
                    JOptionPane.PLAIN_MESSAGE);
            if (name != null) {
                if (application.createInbox(name)) {
                    JOptionPane.showMessageDialog(
                            this,
                            Constants.getInstance().getLocalizedString("CreatedInbox_msg"),
                            Constants.getInstance().getLocalizedString("CreatedInbox"),
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(
                            this,
                            Constants.getInstance().getLocalizedString("FailedCreatedInbox_msg"),
                            Constants.getInstance().getLocalizedString("FailedCreatedInbox"),
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    Constants.getInstance().getLocalizedString("SomethingWentWrong_msg"),
                    Constants.getInstance().getLocalizedString("SomethingWentWrong"),
                    JOptionPane.ERROR_MESSAGE);
            logger.error("InboxOverview::buttonCreateInboxActionPerformed - Exception caught:" + ex.getMessage());
        }
    }

    private void inboxDelete() {
        try {
            Inbox inbox = (Inbox) inboxList.getSelectedValue();
            if (inbox != null) {
                boolean someRecIsRecieving = false;
                for (Recording r : inbox.getRecordings()) {
                    if (r.getState().equals(State.RECIEVING)) {
                        someRecIsRecieving = true;
                        break;
                    }
                }

                if (!someRecIsRecieving) {
                    Object o = JOptionPane.showInputDialog(
                            this,
                            Constants.getInstance().getLocalizedString("WarningDeleteInbox_msg") + ":",
                            Constants.getInstance().getLocalizedString("WarningDeleteInbox"),
                            JOptionPane.WARNING_MESSAGE,
                            icon,
                            null,
                            null);
                    if ((o != null) && (o instanceof String)) {
                        String answer = (String) o;
                        if (answer.equals(inbox.getName())) {
                            application.deleteInbox(inbox);
                            JOptionPane.showMessageDialog(
                                    this,
                                    Constants.getInstance().getLocalizedString("DeletedInbox_msg"),
                                    Constants.getInstance().getLocalizedString("DeletedInbox"),
                                    JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(
                                    this,
                                    Constants.getInstance().getLocalizedString("FailedDeletedInbox_msg"),
                                    Constants.getInstance().getLocalizedString("FailedDeletedInbox"),
                                    JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    Constants.getInstance().getLocalizedString("FailedDeleteInbox_msg"),
                    Constants.getInstance().getLocalizedString("FailedDeleteInbox"),
                    JOptionPane.ERROR_MESSAGE);
            logger.error("InboxOverview::buttonDeleteInboxActionPerformed - Exception caught:" + ex.getMessage());
        }
    }

    private void flavorEditor() {
        try {
            flavorEditor(getSelectedRecording());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    Constants.getInstance().getLocalizedString("SomethingWentWrong_msg"),
                    Constants.getInstance().getLocalizedString("SomethingWentWrong"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void flavorEditor(Recording recording) {
        try {
            if (recording != null) {
                if (recording.getFiles().length > 0) {
                    if (!(recording.getState().equals(State.INPROGRESS))
                            && !(recording.getState().equals(State.SCHEDULED))
                            && !(recording.getState().equals(State.PAUSED))) {
                        if (flavorEditor != null) {
                            flavorEditor.dispose();
                        }
                        flavorEditor = new FlavorEditor(recording);
                        if (flavorEditor.constructedSuccessfully()) {
                            try {
                                Image iconImage = ImageIO.read(getClass().getResourceAsStream("/ui/matterhorn-icon.png"));
                                flavorEditor.setIconImage(iconImage);
                            } catch (IOException ex) {
                                logger.error("InboxOverview::recordingView - IOException caught:" + ex.getMessage());
                            }
                            flavorEditor.setVisible(true);
                        } else {
                            flavorEditor = null;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    Constants.getInstance().getLocalizedString("SomethingWentWrong_msg"),
                    Constants.getInstance().getLocalizedString("SomethingWentWrong"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void inboxInformation() {
        try {
            Inbox inbox = (Inbox) inboxList.getSelectedValue();
            if (inbox != null) {
                if (inboxInformation != null) {
                    inboxInformation.setVisible(false);
                    inboxInformation.dispose();
                }
                inboxInformation = new InboxInformation(application, inbox);
                try {
                    Image iconImage = ImageIO.read(getClass().getResourceAsStream("/ui/matterhorn-icon.png"));
                    inboxInformation.setIconImage(iconImage);
                } catch (IOException ex) {
                    logger.error("InboxOverview::inboxInformation - IOException caught:" + ex.getMessage());
                }
                inboxInformation.setVisible(true);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    Constants.getInstance().getLocalizedString("SomethingWentWrong_msg"),
                    Constants.getInstance().getLocalizedString("SomethingWentWrong"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openIn(boolean recording) {
        try {
            String path = "";
            if (recording) {
                Recording r = getSelectedRecording();
                if (r != null) {
                    path = r.getPath();
                }
            } else {
                Inbox inbox = (Inbox) inboxList.getSelectedValue();
                if (inbox != null) {
                    path = inbox.getPath();
                }
            }
            if ((!path.isEmpty() && (!path.equals(""))) && Desktop.isDesktopSupported()) {
                File f = new File(path);
                if (f.exists()) {
                    Desktop.getDesktop().open(f);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    Constants.getInstance().getLocalizedString("SomethingWentWrong_msg"),
                    Constants.getInstance().getLocalizedString("SomethingWentWrong"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void seriesEditor(boolean show) {
        try {
            Inbox inbox = (Inbox) inboxList.getSelectedValue();
            if (inbox != null) {
                boolean cont = true;
                for (Recording r : inbox.getRecordings()) {
                    if ((r.getState().equals(State.INPROGRESS))
                            || (r.getState().equals(State.SCHEDULED))
                            || (r.getState().equals(State.PAUSED))) {
                        cont = false;
                        break;
                    }
                }

                if (cont) {
                    if (seriesEditor != null) {
                        seriesEditor.setVisible(false);
                        seriesEditor.dispose();
                    }
                    seriesEditor = new SeriesEditor(application, inbox, SpecificFileUtils.getSeries(inbox));
                    try {
                        Image iconImage = ImageIO.read(getClass().getResourceAsStream("/ui/matterhorn-icon.png"));
                        seriesEditor.setIconImage(iconImage);
                    } catch (IOException ex) {
                        logger.error("InboxOverview::seriesEditor - IOException caught:" + ex.getMessage());
                    }
                    if (show) {
                        seriesEditor.setVisible(true);
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    Constants.getInstance().getLocalizedString("SomethingWentWrong_msg"),
                    Constants.getInstance().getLocalizedString("SomethingWentWrong"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void episodeEditor() {
        try {
            Recording r = getSelectedRecording();
            selectedRecordingIndex = (selectedRecordingIndex != -1) ? selectedRecordingIndex : recordingList.getSelectedIndex();
            if (r != null) {
                if (episodeEditor != null) {
                    episodeEditor.setVisible(false);
                    episodeEditor.dispose();
                }
                episodeEditor = new EpisodeEditor(this, r, SpecificFileUtils.getEpisode(r));
                try {
                    Image iconImage = ImageIO.read(getClass().getResourceAsStream("/ui/matterhorn-icon.png"));
                    episodeEditor.setIconImage(iconImage);
                } catch (IOException ex) {
                    logger.error("InboxOverview::episodeEditor - IOException caught:" + ex.getMessage());
                }
                episodeEditor.setVisible(true);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    Constants.getInstance().getLocalizedString("SomethingWentWrong_msg"),
                    Constants.getInstance().getLocalizedString("SomethingWentWrong"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void selectLastSelectedRecording() {
        if (selectedRecordingIndex != -1) {
            recordingList.setSelectedIndex(selectedRecordingIndex);
        }
    }

    private void selectSeries() {
        Thread t = new Thread() {
            @Override
            public void run() {
                checkForSeries();
            }
        };
        t.start();
    }

    private void selectWorkflow(final boolean inbox) {
        Thread t = new Thread() {
            @Override
            public void run() {
                if (inbox) {
                    checkForWorkflowInbox();
                } else {
                    checkForWorkflowRecording();
                }
            }
        };
        t.start();
    }

    private int checkFiles(Recording recording) {
        int nrOfFiles = recording.getFiles().length;
        for (RecordingFile rec : recording.getFiles()) {
            if (!rec.getPath().endsWith(Constants.FILENAME_DC_SERIES)) {
                if (!rec.getPath().endsWith(Constants.FILENAME_DC_EPISODE)) {
                    if (!MHRIFileUtils.getInstance().endsWithOne(rec.getPath(), mediafileSuffixes)) {
                        return -1;
                    }
                } else {
                    --nrOfFiles;
                }
            } else {
                --nrOfFiles;
            }
        }

        return nrOfFiles;
    }

    private void scheduleIngestForAll() {
        try {
            boolean scheduleAll = false;
            boolean cancel = false;
            Object[] options = {
                Constants.getInstance().getLocalizedString("ScheduleIngestOnlyNotUploaded"),
                Constants.getInstance().getLocalizedString("ScheduleIngestForAllIngestAll"),
                Constants.getInstance().getLocalizedString("CancelScheduleAll")
            };
            int answer = JOptionPane.showOptionDialog(
                    this,
                    Constants.getInstance().getLocalizedString("ScheduleIngestForAllIngestAllOrOnlyNotUploaded_msg"),
                    Constants.getInstance().getLocalizedString("ScheduleIngestForAllIngestAllOrOnlyNotUploaded"),
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]);
            if (answer == 0) {
                scheduleAll = false;
            } else if (answer == 1) {
                scheduleAll = true;
            } else {
                cancel = true;
            }

            if (!cancel) {
                Inbox inbox = (Inbox) inboxList.getSelectedValue();
                if (inbox != null) {
                    for (Recording r : inbox.getRecordings()) {
                        if (scheduleAll) {
                            if (!r.getState().equals(Recording.State.INPROGRESS)
                                    && !r.getState().equals(Recording.State.SCHEDULED)
                                    && !r.getState().equals(Recording.State.RECIEVING)) {
                                upload(r);
                            }
                        } else {
                            if (!r.getState().equals(Recording.State.COMPLETE)
                                    && !r.getState().equals(Recording.State.INPROGRESS)
                                    && !r.getState().equals(Recording.State.SCHEDULED)
                                    && !r.getState().equals(Recording.State.RECIEVING)) {
                                upload(r);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    Constants.getInstance().getLocalizedString("SomethingWentWrong_msg"),
                    Constants.getInstance().getLocalizedString("SomethingWentWrong"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopIngestForAll() {
        try {
            Inbox inbox = (Inbox) inboxList.getSelectedValue();
            if (inbox != null) {
                for (Recording r : inbox.getRecordings()) {
                    if (!r.getState().equals(Recording.State.COMPLETE)
                            && !r.getState().equals(Recording.State.FAILED)
                            && !r.getState().equals(Recording.State.IDLE)
                            && !r.getState().equals(Recording.State.RECIEVING)) {
                        stop(r);
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    Constants.getInstance().getLocalizedString("SomethingWentWrong_msg"),
                    Constants.getInstance().getLocalizedString("SomethingWentWrong"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void upload(Recording r) {
        try {
            Recording recording;
            if (r == null) {
                recording = getSelectedRecording();
            } else {
                recording = r;
            }
            int select = recordingList.getSelectedIndex();
            if (recording != null) {
                if (recording.getFiles().length > 0) {
                    if ((recording.getState() == Recording.State.IDLE)
                            || recording.getState().equals(Recording.State.COMPLETE)
                            || (recording.getState() == Recording.State.FAILED)) {
                        if (this.flavorEditor != null) {
                            this.flavorEditor.dispose();
                        }
                        if (this.seriesEditor != null) {
                            this.seriesEditor.dispose();
                        }
                        if (this.episodeEditor != null) {
                            this.episodeEditor.dispose();
                        }

                        checkContextMenuRecordings(recording);

                        boolean seriesOK = true;
                        boolean assignSeriesFromServer = false;
                        String serverSeriesID = "";
                        Map<String, String> seriesList = getSortedSeriesFromServer();
                        seriesEditor(false);
                        Inbox inbox = (Inbox) inboxList.getSelectedValue();
                        if ((seriesEditor != null) && (inbox != null) && (seriesList != null) && !seriesList.isEmpty()) {
                            for (Map.Entry<String, String> entry : seriesList.entrySet()) {
                                if (seriesEditor.getSeriesTitle().equals(entry.getValue()) && !seriesEditor.getSeriesID().equals(entry.getKey())) {
                                    Object[] options = {
                                        Constants.getInstance().getLocalizedString("SameSeriesNameAssignSeriesFromServer"),
                                        Constants.getInstance().getLocalizedString("SameSeriesNameCreateSeries"),
                                        Constants.getInstance().getLocalizedString("CancelUpload")
                                    };
                                    int answer = JOptionPane.showOptionDialog(
                                            this,
                                            Constants.getInstance().getLocalizedString("SameSeriesName_msg"),
                                            Constants.getInstance().getLocalizedString("SameSeriesName"),
                                            JOptionPane.YES_NO_CANCEL_OPTION,
                                            JOptionPane.WARNING_MESSAGE,
                                            null,
                                            options,
                                            options[0]);
                                    if (answer == 0) {
                                        assignSeriesFromServer = true;
                                        serverSeriesID = entry.getKey();
                                    } else if (answer == 1) {
                                    } else {
                                        seriesOK = false;
                                    }

                                    break;
                                }
                            }
                        }

                        if (assignSeriesFromServer) {
                            seriesOK = assignSeries(serverSeriesID, false);
                        }

                        if (seriesOK && !recording.getState().equals(Recording.State.COMPLETE)
                                || (recording.getState().equals(Recording.State.COMPLETE)
                                && JOptionPane.showConfirmDialog(
                                        this,
                                        recording.getTitle() + "\n"
                                        + Constants.getInstance().getLocalizedString("WarningRecordingAlreadyUploaded_msg"),
                                        Constants.getInstance().getLocalizedString("WarningRecordingAlreadyUploaded"),
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)) {
                            int nrOfFiles = checkFiles(recording);
                            if ((nrOfFiles > 0)
                                    || ((nrOfFiles == -1) && (JOptionPane.showConfirmDialog(
                                            this,
                                            recording.getTitle() + "\n"
                                            + Constants.getInstance().getLocalizedString("WarningOneMediaFileTypeUnknown_msg"),
                                            Constants.getInstance().getLocalizedString("WarningOneMediaFileTypeUnknown"),
                                            JOptionPane.YES_NO_OPTION,
                                            JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION))
                                    || ((nrOfFiles == 0) && (JOptionPane.showConfirmDialog(
                                            this,
                                            recording.getTitle() + "\n"
                                            + Constants.getInstance().getLocalizedString("WarningNoMediaFiles_msg"),
                                            Constants.getInstance().getLocalizedString("WarningNoMediaFiles"),
                                            JOptionPane.YES_NO_OPTION,
                                            JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION))) {
                                int nrOfPresentations = 0;
                                List<RecordingFile> unknownRecordingFiles = new LinkedList<>();
                                for (RecordingFile rf : recording.getFiles()) {
                                    switch (rf.getFlavor()) {
                                        case Constants.FLAVOR_PRESENTATION:
                                            ++nrOfPresentations;
                                            break;
                                        case Constants.FLAVOR_ATTACHMENT_UNKNOWN:
                                            unknownRecordingFiles.add(rf);
                                            break;
                                    }
                                }
                                if (nrOfPresentations > 1) {
                                    Object[] options = {
                                        Constants.getInstance().getLocalizedString("OpenInFlavorEditor"),
                                        Constants.getInstance().getLocalizedString("UploadAnyway"),
                                        Constants.getInstance().getLocalizedString("CancelUpload")
                                    };
                                    int answer = JOptionPane.showOptionDialog(
                                            this,
                                            recording.getTitle() + "\n"
                                            + Constants.getInstance().getLocalizedString("WarningSameFlavors_msg"),
                                            Constants.getInstance().getLocalizedString("WarningSameFlavors"),
                                            JOptionPane.YES_NO_CANCEL_OPTION,
                                            JOptionPane.WARNING_MESSAGE,
                                            null,
                                            options,
                                            options[0]);
                                    if (answer == 0) {
                                        flavorEditor(recording);
                                    } else if (answer == 1) {
                                        recording.setIngestStatus("");
                                        recording.setIngestDetails("");
                                        application.retryIngest(recording);
                                    }
                                } else {
                                    int noOfUnknown = unknownRecordingFiles.size();
                                    if (noOfUnknown <= 0) {
                                        recording.setIngestStatus("");
                                        recording.setIngestDetails("");
                                        application.retryIngest(recording);
                                    } else {
                                        Object[] options = {
                                            Constants.getInstance().getLocalizedString("OpenInFlavorEditor"),
                                            Constants.getInstance().getLocalizedString("UploadAnyway"),
                                            Constants.getInstance().getLocalizedString("CancelUpload")
                                        };
                                        int answer = JOptionPane.showOptionDialog(
                                                this,
                                                recording.getTitle() + "\n"
                                                + Constants.getInstance().getLocalizedString("WarningUnknownFlavor_msg"),
                                                Constants.getInstance().getLocalizedString("WarningUnknownFlavor"),
                                                JOptionPane.YES_NO_CANCEL_OPTION,
                                                JOptionPane.WARNING_MESSAGE,
                                                null,
                                                options,
                                                options[0]);
                                        if (answer == 0) {
                                            flavorEditor(recording);
                                        } else if (answer == 1) {
                                            recording.setIngestStatus("");
                                            recording.setIngestDetails("");
                                            application.retryIngest(recording);
                                        }
                                    }
                                }
                            }
                        }
                        if (select != -1) {
                            try {
                                recordingList.setSelectedIndex(select);
                                checkContextMenuRecordings(recording);
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(
                                        this,
                                        recording.getTitle() + "\n"
                                        + Constants.getInstance().getLocalizedString("SomethingWentWrong_msg"),
                                        Constants.getInstance().getLocalizedString("SomethingWentWrong"),
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    Constants.getInstance().getLocalizedString("CouldNotScheduleIngest_msg"),
                    Constants.getInstance().getLocalizedString("CouldNotScheduleIngest"),
                    JOptionPane.ERROR_MESSAGE);
            logger.error("InboxOverview::buttonScheduleActionPerformed - Exception caught:" + ex.getMessage());
        }
    }

    private void stop(Recording r) {
        try {
            Recording recording;
            if (r == null) {
                recording = getSelectedRecording();
            } else {
                recording = r;
            }
            if (recording != null) {
                if (recording.getFiles().length > 0) {
                    checkContextMenuRecordings(recording);
                    if (((recording.getState() == Recording.State.INPROGRESS)
                            || (recording.getState() == Recording.State.SCHEDULED)
                            || (recording.getState() == Recording.State.PAUSED))
                            && (JOptionPane.showConfirmDialog(
                                    this,
                                    recording.getTitle() + "\n"
                                    + Constants.getInstance().getLocalizedString("ReallyAbortIngest_msg"),
                                    Constants.getInstance().getLocalizedString("ReallyAbortIngest"),
                                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)) {
                        application.stopIngest(recording);
                    }
                }
            }
        } catch (Exception ex) {
            // small hack to stop even if an exception occurred
            logger.error("InboxOverview::buttonStopActionPerformed - Exception caught:" + ex.getMessage());
            try {
                Recording recording;
                if (r == null) {
                    recording = getSelectedRecording();
                } else {
                    recording = r;
                }
                if (recording != null) {
                    if (recording.getFiles().length > 0) {
                        checkContextMenuRecordings(recording);
                        if ((recording.getState() == Recording.State.INPROGRESS)
                                || (recording.getState() == Recording.State.SCHEDULED)
                                || (recording.getState() == Recording.State.PAUSED)) {
                            try {
                                application.stopIngest(recording);
                            } catch (Exception ex2) {
                                JOptionPane.showMessageDialog(
                                        this,
                                        recording.getTitle() + "\n"
                                        + Constants.getInstance().getLocalizedString("SomethingWentWrong_msg"),
                                        Constants.getInstance().getLocalizedString("SomethingWentWrong"),
                                        JOptionPane.ERROR_MESSAGE);
                                logger.error("InboxOverview::buttonStopActionPerformed - Exception 2 caught:" + ex2.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        this,
                        Constants.getInstance().getLocalizedString("SomethingWentWrong_msg"),
                        Constants.getInstance().getLocalizedString("SomethingWentWrong"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    void delete() {
        try {
            Recording recording = getSelectedRecording();
            if (recording != null) {
                checkContextMenuRecordings(recording);
                if (!recording.getState().equals(State.RECIEVING)) {
                    String theAnswer = Constants.getInstance().getLocalizedString("Delete");
                    Object o = JOptionPane.showInputDialog(
                            this,
                            Constants.getInstance().getLocalizedString("WarningDeleteRecording_msg") + " \"" + theAnswer + "\":",
                            Constants.getInstance().getLocalizedString("WarningDeleteRecording"),
                            JOptionPane.WARNING_MESSAGE,
                            icon,
                            null,
                            null);
                    if ((o != null) && (o instanceof String)) {
                        String answer = (String) o;
                        if (answer.equals(theAnswer)) {
                            application.deleteRecording(recording);
                            JOptionPane.showMessageDialog(
                                    this,
                                    Constants.getInstance().getLocalizedString("DeletedRecording_msg"),
                                    Constants.getInstance().getLocalizedString("DeletedRecording"),
                                    JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(
                                    this,
                                    Constants.getInstance().getLocalizedString("StringsDontMatch_msg"),
                                    Constants.getInstance().getLocalizedString("StringsDontMatch"),
                                    JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    Constants.getInstance().getLocalizedString("FailedDeletedRecording_msg"),
                    Constants.getInstance().getLocalizedString("FailedDeletedRecording"),
                    JOptionPane.ERROR_MESSAGE);
            logger.error("InboxOverview::buttonDeleteActionPerformed - Exception caught:" + ex.getMessage());
        }
    }

    private void markAs(State state) {
        if ((state == State.IDLE)
                || (state == State.COMPLETE)
                || (state == State.FAILED)) {
            try {
                Recording recording = getSelectedRecording();
                if (recording != null) {
                    if (recording.getFiles().length > 0) {
                        checkContextMenuRecordings(recording);
                        if ((recording.getState() == Recording.State.IDLE)
                                || (recording.getState() == Recording.State.COMPLETE)
                                || (recording.getState() == Recording.State.FAILED)) {
                            boolean cont = true;
                            if (recording.getState().equals(Recording.State.COMPLETE)) {
                                cont = JOptionPane.showConfirmDialog(
                                        this,
                                        Constants.getInstance().getLocalizedString("WarningRecordingAlreadyUploaded_msg"),
                                        Constants.getInstance().getLocalizedString("WarningRecordingAlreadyUploaded"),
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
                            }
                            if (cont) {
                                recording.setIngestDetails("");
                                State s = recording.getState();
                                if (state == State.IDLE) {
                                    s = State.IDLE;
                                } else if (state == State.COMPLETE) {
                                    s = State.COMPLETE;
                                } else if (state == State.FAILED) {
                                    s = State.FAILED;
                                }
                                recording.setState(s);
                                inboxList.setSelectedIndex(currSelectedIndexP);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        this,
                        Constants.getInstance().getLocalizedString("SomethingWentWrong_msg"),
                        Constants.getInstance().getLocalizedString("SomethingWentWrong"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private boolean isDigit(char ch) {
        return ch >= 48 && ch <= 57;
    }

    /**
     * Length of string is passed in for improved efficiency (only need to
     * calculate it once) *
     */
    private String getChunk(String s, int slength, int marker) {
        StringBuilder chunk = new StringBuilder();
        char c = s.charAt(marker);
        chunk.append(c);
        marker++;
        if (isDigit(c)) {
            while (marker < slength) {
                c = s.charAt(marker);
                if (!isDigit(c)) {
                    break;
                }
                chunk.append(c);
                marker++;
            }
        } else {
            while (marker < slength) {
                c = s.charAt(marker);
                if (isDigit(c)) {
                    break;
                }
                chunk.append(c);
                marker++;
            }
        }
        return chunk.toString();
    }

    private Map<String, String> sortByValues(Map<String, String> map) {
        List<Map.Entry<String, String>> entries = new LinkedList<>(map.entrySet());

        Collections.sort(entries, new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Entry<String, String> o1, Entry<String, String> o2) {
                if ((o1 == null) || (o2 == null)) {
                    return 0;
                }
                String s1 = o1.getValue();
                String s2 = o2.getValue();

                int thisMarker = 0;
                int thatMarker = 0;
                int s1Length = s1.length();
                int s2Length = s2.length();

                while (thisMarker < s1Length && thatMarker < s2Length) {
                    String thisChunk = getChunk(s1, s1Length, thisMarker);
                    thisMarker += thisChunk.length();

                    String thatChunk = getChunk(s2, s2Length, thatMarker);
                    thatMarker += thatChunk.length();

                    // If both chunks contain numeric characters, sort them numerically
                    int result;
                    if (isDigit(thisChunk.charAt(0)) && isDigit(thatChunk.charAt(0))) {
                        // Simple chunk comparison by length.
                        int thisChunkLength = thisChunk.length();
                        result = thisChunkLength - thatChunk.length();
                        // If equal, the first different number counts
                        if (result == 0) {
                            for (int i = 0; i < thisChunkLength; i++) {
                                result = thisChunk.charAt(i) - thatChunk.charAt(i);
                                if (result != 0) {
                                    return result;
                                }
                            }
                        }
                    } else {
                        result = thisChunk.compareTo(thatChunk);
                    }

                    if (result != 0) {
                        return result;
                    }
                }

                return s1Length - s2Length;
            }
        });

        Map<String, String> sortedMap = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    private Map<String, String> getSortedSeriesFromServer() {
        HashMap<String, String> seriesList = null;
        try {
            this.setEnabled(false);
            seriesList = application.getSeriesList();
        } catch (IOException ex) {
            this.setEnabled(true);
            JOptionPane.showMessageDialog(
                    this,
                    Constants.getInstance().getLocalizedString("NoSeriesInfoRetrieved_msg"),
                    Constants.getInstance().getLocalizedString("NoSeriesInfoRetrieved"),
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            this.setEnabled(true);
        }
        this.setEnabled(true);
        if (seriesList != null) {
            return sortByValues(seriesList);
        }
        return null;
    }

    private boolean assignSeries(String seriesID, boolean askToReplace) {
        Inbox inbox = (Inbox) inboxList.getSelectedValue();
        if (inbox != null) {
            String path = application.getInboxPath() + inbox.getName();
            application.setCurrentSelectedInbox(inbox.getName());
            if (path != null) {
                File f = new File(path + File.separator + "series.xml");
                if (!f.exists() || ((f.exists() && !askToReplace) || (f.exists()
                        && (JOptionPane.showConfirmDialog(
                                this,
                                Constants.getInstance().getLocalizedString("ReplaceExistingSeries_msg"),
                                Constants.getInstance().getLocalizedString("ReplaceExistingSeries"),
                                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)))) {
                    try {
                        application.downloadSeries(seriesID);
                        JOptionPane.showMessageDialog(
                                this,
                                Constants.getInstance().getLocalizedString("AssignedSeries_msg"),
                                Constants.getInstance().getLocalizedString("AssignedSeries"),
                                JOptionPane.INFORMATION_MESSAGE);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(
                                this,
                                Constants.getInstance().getLocalizedString("NoSeriesInfoRetrieved_msg"),
                                Constants.getInstance().getLocalizedString("NoSeriesInfoRetrieved"),
                                JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                    inbox.setSeriesId(seriesID);
                    return true;
                }
            }
        }
        return false;
    }

    private void checkForSeries() {
        try {
            Inbox inbox = (Inbox) inboxList.getSelectedValue();
            if (inbox != null) {
                boolean inProgressScheduledOrPaused = false;
                for (Recording r : inbox.getRecordings()) {
                    if ((r.getState().equals(State.INPROGRESS))
                            || (r.getState().equals(State.SCHEDULED))
                            || (r.getState().equals(State.PAUSED))) {
                        inProgressScheduledOrPaused = true;
                    }
                }

                if (!inProgressScheduledOrPaused) {
                    HashMap<String, String> seriesList = null;
                    try {
                        this.setEnabled(false);
                        seriesList = application.getSeriesList();
                    } catch (IOException ex) {
                        this.setEnabled(true);
                        JOptionPane.showMessageDialog(
                                this,
                                Constants.getInstance().getLocalizedString("NoSeriesInfoRetrieved_msg"),
                                Constants.getInstance().getLocalizedString("NoSeriesInfoRetrieved"),
                                JOptionPane.ERROR_MESSAGE);
                    } finally {
                        this.setEnabled(true);
                    }
                    this.setEnabled(true);
                    if (seriesList != null) {
                        Map<String, String> sorted_map = sortByValues(seriesList);

                        Object[] a = sorted_map.keySet().toArray();
                        String[] keys = new String[a.length];
                        String[] values = new String[a.length];
                        int i = 0;
                        for (String s : sorted_map.keySet()) {
                            keys[i] = s;
                            values[i] = seriesList.get(keys[i]).toString();
                            ++i;
                        }

                        if (values.length > 0) {
                            String selectedName = ListDialog.showDialog(
                                    this,
                                    this,
                                    Constants.getInstance().getLocalizedString("SelectSeries") + ":",
                                    Constants.getInstance().getLocalizedString("SeriesList"),
                                    values,
                                    values[0],
                                    null);
                            if (!selectedName.equals("") && !selectedName.isEmpty()) {
                                if (logger.isInfoEnabled()) {
                                    logger.info("InboxOverview::checkForSeries - Selected name: " + selectedName);
                                }
                                String path = application.getInboxPath() + inbox.getName();
                                application.setCurrentSelectedInbox(inbox.getName());
                                if (path != null) {
                                    File f = new File(path + File.separator + "series.xml");
                                    if (!f.exists() || (f.exists()
                                            && (JOptionPane.showConfirmDialog(
                                                    this,
                                                    Constants.getInstance().getLocalizedString("ReplaceExistingSeries_msg"),
                                                    Constants.getInstance().getLocalizedString("ReplaceExistingSeries"),
                                                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION))) {
                                        for (int j = 0; j < values.length; ++j) {
                                            if (values[j].equals(selectedName)) {
                                                try {
                                                    application.downloadSeries(keys[j]);
                                                    JOptionPane.showMessageDialog(
                                                            this,
                                                            Constants.getInstance().getLocalizedString("AssignedSeries_msg"),
                                                            Constants.getInstance().getLocalizedString("AssignedSeries"),
                                                            JOptionPane.INFORMATION_MESSAGE);
                                                } catch (IOException ex) {
                                                    JOptionPane.showMessageDialog(
                                                            this,
                                                            Constants.getInstance().getLocalizedString("NoSeriesInfoRetrieved_msg"),
                                                            Constants.getInstance().getLocalizedString("NoSeriesInfoRetrieved"),
                                                            JOptionPane.ERROR_MESSAGE);
                                                }
                                                inbox.setSeriesId(keys[j]);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        Constants.getInstance().getLocalizedString("NoInboxSelected1_msg"),
                        Constants.getInstance().getLocalizedString("NoInboxSelected1"),
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    Constants.getInstance().getLocalizedString("SomethingWentWrong_msg"),
                    Constants.getInstance().getLocalizedString("SomethingWentWrong"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void checkForWorkflowInbox() {
        Inbox inbox = (Inbox) inboxList.getSelectedValue();
        if (inbox != null) {
            boolean cont = true;
            for (Recording r : inbox.getRecordings()) {
                if ((r.getState().equals(State.INPROGRESS))
                        || (r.getState().equals(State.SCHEDULED))
                        || (r.getState().equals(State.PAUSED))) {
                    cont = false;
                    break;
                }
            }

            if (cont) {
                List<String> workflowList = null;
                try {
                    this.setEnabled(false);
                    workflowList = application.getWorkflowList();
                } catch (IOException ex) {
                    this.setEnabled(true);
                    JOptionPane.showMessageDialog(
                            this,
                            Constants.getInstance().getLocalizedString("NoWorkflowInfoRetrieved_msg"),
                            Constants.getInstance().getLocalizedString("NoWorkflowInfoRetrieved"),
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    this.setEnabled(true);
                }
                this.setEnabled(true);
                if (workflowList != null) {
                    Object[] a = workflowList.toArray();
                    String[] keys = new String[a.length];

                    int i = 0;
                    for (String s : workflowList) {
                        keys[i] = s;
                        ++i;
                    }

                    if (keys.length > 0) {
                        Arrays.sort(keys);
                        String selectedName = ListDialog.showDialog(
                                this,
                                this,
                                Constants.getInstance().getLocalizedString("SelectWorkflowForAll_msg") + ":",
                                Constants.getInstance().getLocalizedString("SelectWorkflowForAll"),
                                keys,
                                keys[0],
                                null);
                        if (!selectedName.equals("") && !selectedName.isEmpty()) {
                            if (logger.isInfoEnabled()) {
                                logger.info("InboxOverview::checkForWorkflow - Selected workflow: " + selectedName);
                            }
                            inbox.setWorkflowId(selectedName);
                            for (Recording r : inbox.getRecordings()) {
                                r.setWorkflowId("");
                            }
                            JOptionPane.showMessageDialog(
                                    this,
                                    Constants.getInstance().getLocalizedString("SuccessSelectWorkflowForAll_msg"),
                                    Constants.getInstance().getLocalizedString("SuccessSelectWorkflowForAll"),
                                    JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            }
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    Constants.getInstance().getLocalizedString("NoInboxSelected2_msg"),
                    Constants.getInstance().getLocalizedString("NoInboxSelected2"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void checkForWorkflowRecording() {
        try {
            Recording recording = getSelectedRecording();
            if (recording != null) {
                if (recording.getFiles().length > 0) {
                    checkContextMenuRecordings(recording);
                    if (!((recording.getState().equals(State.INPROGRESS))
                            || (recording.getState().equals(State.SCHEDULED))
                            || (recording.getState().equals(State.PAUSED)))) {
                        List<String> workflowList = null;
                        try {
                            this.setEnabled(false);
                            workflowList = application.getWorkflowList();
                        } catch (IOException ex) {
                            this.setEnabled(true);
                            JOptionPane.showMessageDialog(
                                    this,
                                    Constants.getInstance().getLocalizedString("NoWorkflowInfoRetrieved_msg"),
                                    Constants.getInstance().getLocalizedString("NoWorkflowInfoRetrieved"),
                                    JOptionPane.ERROR_MESSAGE);
                        } finally {
                            this.setEnabled(true);
                        }
                        this.setEnabled(true);
                        if (workflowList != null) {
                            Object[] a = workflowList.toArray();
                            String[] keys = new String[a.length];

                            int i = 0;
                            for (String s : workflowList) {
                                keys[i] = s;
                                ++i;
                            }

                            if (keys.length > 0) {
                                Arrays.sort(keys);
                                String selectedName = ListDialog.showDialog(
                                        this,
                                        this,
                                        Constants.getInstance().getLocalizedString("SelectWorkflow_msg") + ":",
                                        Constants.getInstance().getLocalizedString("SelectWorkflow"),
                                        keys,
                                        keys[0],
                                        null);
                                if (!selectedName.equals("") && !selectedName.isEmpty()) {
                                    if (logger.isInfoEnabled()) {
                                        logger.info("InboxOverview::checkForWorkflow - Selected workflow name: " + selectedName);
                                    }
                                    recording.setWorkflowId(selectedName);
                                    JOptionPane.showMessageDialog(
                                            this,
                                            Constants.getInstance().getLocalizedString("SuccessSelectWorkflow_msg"),
                                            Constants.getInstance().getLocalizedString("SuccessSelectWorkflow"),
                                            JOptionPane.INFORMATION_MESSAGE);
                                }
                            }
                        }
                    }
                }
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        Constants.getInstance().getLocalizedString("NoRecordingSelected_msg"),
                        Constants.getInstance().getLocalizedString("NoRecordingSelected"),
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    Constants.getInstance().getLocalizedString("SomethingWentWrong_msg"),
                    Constants.getInstance().getLocalizedString("SomethingWentWrong"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean sceneDetectionPossible(Recording recording) {
        int nrOfPresenters = 0;
        int nrOfPresentations = 0;
        int nrOfUnknown = 0;
        for (RecordingFile rf : recording.getFiles()) {
            switch (rf.getFlavor()) {
                case Constants.FLAVOR_PRESENTER:
                    ++nrOfPresenters;
                    break;
                case Constants.FLAVOR_PRESENTATION:
                    ++nrOfPresentations;
                    break;
                case Constants.FLAVOR_ATTACHMENT_UNKNOWN:
                    ++nrOfUnknown;
                    break;
            }
        }
        if (nrOfPresentations >= 1) {
            recordingMenuItemUploadSceneDetection.setSelected(true);
        } else {
            recordingMenuItemUploadSceneDetection.setSelected(false);
        }
        if (((nrOfPresentations == 0)
                && (nrOfPresenters == 1))
                || ((nrOfPresentations == 1)
                && (nrOfPresenters == 0))
                || ((nrOfPresentations == 0)
                && (nrOfPresenters == 0)
                && (nrOfUnknown == 1))) {
            return true;
        } else {
            return false;
        }
    }

    private void checkContextMenuRecordings(Recording recording) {
        if (recording != null) {
            if (recording.getState() == Recording.State.RECIEVING) {
                recordingMenuItemOpenInBrowser.setEnabled(false);
                recordingMenuItemUpload.setEnabled(false);
                recordingMenuItemUploadTrim.setEnabled(true);
                recordingMenuItemUploadSceneDetection.setEnabled(sceneDetectionPossible(recording));
                recordingMenuItemEpisodeEditor.setEnabled(true);
                recordingMenuItemFlavorEditor.setEnabled(true);
                recordingMenuItemSelectWorkflow.setEnabled(true);
                recordingMenuItemStop.setEnabled(false);
                recordingMenuItemDelete.setEnabled(false);
                recordingMenuItemMarkIdle.setEnabled(false);
                recordingMenuItemMarkComplete.setEnabled(false);
                recordingMenuItemMarkFailed.setEnabled(false);
            } else if (recording.getState() == Recording.State.INPROGRESS) {
                recordingMenuItemOpenInBrowser.setEnabled(false);
                recordingMenuItemUpload.setEnabled(false);
                recordingMenuItemUploadTrim.setEnabled(false);
                recordingMenuItemUploadSceneDetection.setEnabled(false);
                recordingMenuItemEpisodeEditor.setEnabled(false);
                recordingMenuItemFlavorEditor.setEnabled(false);
                recordingMenuItemSelectWorkflow.setEnabled(false);
                recordingMenuItemStop.setEnabled(true);
                recordingMenuItemDelete.setEnabled(true);
                recordingMenuItemMarkIdle.setEnabled(false);
                recordingMenuItemMarkComplete.setEnabled(false);
                recordingMenuItemMarkFailed.setEnabled(false);
            } else if (recording.getState() == Recording.State.SCHEDULED) {
                recordingMenuItemOpenInBrowser.setEnabled(false);
                recordingMenuItemUpload.setEnabled(false);
                recordingMenuItemUploadTrim.setEnabled(true);
                recordingMenuItemUploadSceneDetection.setEnabled(sceneDetectionPossible(recording));
                recordingMenuItemEpisodeEditor.setEnabled(false);
                recordingMenuItemFlavorEditor.setEnabled(false);
                recordingMenuItemSelectWorkflow.setEnabled(false);
                recordingMenuItemStop.setEnabled(true);
                recordingMenuItemDelete.setEnabled(true);
                recordingMenuItemMarkIdle.setEnabled(false);
                recordingMenuItemMarkComplete.setEnabled(false);
                recordingMenuItemMarkFailed.setEnabled(false);
            } else if (recording.getState() == Recording.State.COMPLETE) {
                recordingMenuItemOpenInBrowser.setEnabled(false);
                recordingMenuItemUpload.setEnabled(true);
                recordingMenuItemUploadTrim.setEnabled(true);
                recordingMenuItemUploadSceneDetection.setEnabled(sceneDetectionPossible(recording));
                recordingMenuItemEpisodeEditor.setEnabled(true);
                recordingMenuItemFlavorEditor.setEnabled(true);
                recordingMenuItemSelectWorkflow.setEnabled(true);
                recordingMenuItemStop.setEnabled(false);
                recordingMenuItemDelete.setEnabled(true);
                recordingMenuItemMarkIdle.setEnabled(true);
                recordingMenuItemMarkComplete.setEnabled(true);
                recordingMenuItemMarkFailed.setEnabled(true);
                if (recording.getState() == Recording.State.COMPLETE) {
                    final String link = ((RecordingImpl) recording).linkToEngageUI;
                    if (!link.isEmpty() && !link.equals("")) {
                        recordingMenuItemOpenInBrowser.setEnabled(true);
                    }
                }
            } else if (recording.getState() == Recording.State.FAILED) {
                recordingMenuItemOpenInBrowser.setEnabled(false);
                recordingMenuItemUpload.setEnabled(true);
                recordingMenuItemUploadTrim.setEnabled(true);
                recordingMenuItemUploadSceneDetection.setEnabled(sceneDetectionPossible(recording));
                recordingMenuItemEpisodeEditor.setEnabled(true);
                recordingMenuItemFlavorEditor.setEnabled(true);
                recordingMenuItemSelectWorkflow.setEnabled(true);
                recordingMenuItemStop.setEnabled(false);
                recordingMenuItemDelete.setEnabled(true);
                recordingMenuItemMarkIdle.setEnabled(true);
                recordingMenuItemMarkComplete.setEnabled(true);
                recordingMenuItemMarkFailed.setEnabled(true);
            } else if ((recording.getState() == Recording.State.IDLE)) {
                recordingMenuItemOpenInBrowser.setEnabled(false);
                recordingMenuItemUpload.setEnabled(true);
                recordingMenuItemUploadTrim.setEnabled(true);
                recordingMenuItemUploadSceneDetection.setEnabled(sceneDetectionPossible(recording));
                recordingMenuItemEpisodeEditor.setEnabled(true);
                recordingMenuItemFlavorEditor.setEnabled(true);
                recordingMenuItemSelectWorkflow.setEnabled(true);
                recordingMenuItemStop.setEnabled(false);
                recordingMenuItemDelete.setEnabled(true);
                recordingMenuItemMarkIdle.setEnabled(true);
                recordingMenuItemMarkComplete.setEnabled(true);
                recordingMenuItemMarkFailed.setEnabled(true);
            } else if (recording.getState() == Recording.State.PAUSED) {
                recordingMenuItemOpenInBrowser.setEnabled(false);
                recordingMenuItemUpload.setEnabled(false);
                recordingMenuItemUploadTrim.setEnabled(false);
                recordingMenuItemUploadSceneDetection.setEnabled(false);
                recordingMenuItemEpisodeEditor.setEnabled(false);
                recordingMenuItemFlavorEditor.setEnabled(false);
                recordingMenuItemSelectWorkflow.setEnabled(false);
                recordingMenuItemStop.setEnabled(true);
                recordingMenuItemDelete.setEnabled(true);
                recordingMenuItemMarkIdle.setEnabled(false);
                recordingMenuItemMarkComplete.setEnabled(false);
                recordingMenuItemMarkFailed.setEnabled(false);
            }
        } else {
            recordingMenuItemOpenInBrowser.setEnabled(false);
            recordingMenuItemUpload.setEnabled(false);
            recordingMenuItemUploadTrim.setEnabled(false);
            recordingMenuItemUploadSceneDetection.setEnabled(false);
            recordingMenuItemEpisodeEditor.setEnabled(false);
            recordingMenuItemFlavorEditor.setEnabled(false);
            recordingMenuItemSelectWorkflow.setEnabled(false);
            recordingMenuItemStop.setEnabled(false);
            recordingMenuItemDelete.setEnabled(false);
            recordingMenuItemMarkIdle.setEnabled(false);
            recordingMenuItemMarkComplete.setEnabled(false);
            recordingMenuItemMarkFailed.setEnabled(false);
        }
    }

    public void checkButtons() {
        try {
            checkTooltips();
            Recording recording = getSelectedRecording();
            if (recording != null) {
                if (recording.getFiles().length > 0) {
                    if (inboxList.getSelectedValue() != null) {
                        Inbox inbox = (Inbox) inboxList.getSelectedValue();
                        if (inbox != null) {
                            this.buttonInboxInfo.setEnabled(true);
                        } else {
                            this.buttonInboxInfo.setEnabled(false);
                        }
                    }
                }
            } else {
                recordingList.clearSelection();
            }
        } catch (Exception ex) {
        }
    }

    private void openInBrowser() {
        try {
            Recording recording = getSelectedRecording();
            if (recording != null) {
                if (recording.getFiles().length > 0) {
                    if (recording.getState() == Recording.State.COMPLETE) {
                        final String link = ((RecordingImpl) recording).linkToEngageUI;
                        if (!link.isEmpty() && !link.equals("")) {
                            if (Desktop.isDesktopSupported()) {
                                try {
                                    URI uri = new URI(link);
                                    Desktop.getDesktop().browse(uri);
                                } catch (URISyntaxException | IOException ex) {
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    Constants.getInstance().getLocalizedString("SomethingWentWrong_msg"),
                    Constants.getInstance().getLocalizedString("SomethingWentWrong"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void recordingFocused() {
        checkButtons();
    }

    private void inboxFocused() {
        try {
            if (!((episodeEditor != null) && (episodeEditor.isVisible()))) {
                selectedRecordingIndex = -1;
            }
            recordingList.clearSelection();
            if (!inboxList.isSelectionEmpty()) {
                this.buttonInboxInfo.setEnabled(true);
            } else {
                this.buttonInboxInfo.setEnabled(false);
            }
        } catch (Exception ex) {
        }
    }

    public void setNetworkStatus(int i, String msg) {
        switch (i) {
            default:
            case 0:
                label_network_status.setIcon(DesktopUI.icons.get(UIIcon.NETWORK_WHITE));
                break;
            case 1:
                label_network_status.setIcon(DesktopUI.icons.get(UIIcon.NETWORK_YELLOW));
                break;
            case 2:
                label_network_status.setIcon(DesktopUI.icons.get(UIIcon.NETWORK_RED));
                break;
            case 3:
                label_network_status.setIcon(DesktopUI.icons.get(UIIcon.NETWORK_GREEN));
                break;
        }
        label_network_status.setToolTipText(msg);
    }

    @Override
    public void valueChanged(ListSelectionEvent lse) {
        try {
            if (lse.getSource() == inboxList) {
                Object value = inboxList.getSelectedValue();
                if (value != null) {
                    Inbox inbox = (Inbox) value;
                    recordingListModel.setInbox(inbox);
                }
            }
        } catch (Exception e1) {
            logger.error("Exception: valueChanged: " + e1.getMessage() + ", retrying action");
            try {
                if (lse.getSource() == inboxList) {
                    Object value = inboxList.getSelectedValue();
                    if (value != null) {
                        Inbox inbox = (Inbox) value;
                        recordingListModel.setInbox(inbox);
                    }
                }
            } catch (Exception e2) {
                logger.error("Exception 2: valueChanged: " + e2.getMessage());
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

        toolBar = new javax.swing.JToolBar();
        buttonInboxInfo = new javax.swing.JButton();
        filler4 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        combobox_filter = new javax.swing.JComboBox();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        label_network_status = new javax.swing.JLabel();
        splitPane = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        inboxList = new javax.swing.JList();
        jScrollPane2 = new javax.swing.JScrollPane();
        recordingList = new javax.swing.JList();

        setTitle("Matterhorn Remote Inbox");
        setMinimumSize(new java.awt.Dimension(750, 530));

        toolBar.setRollover(true);

        buttonInboxInfo.setIcon(DesktopUI.icons.get(UIIcon.INBOX_INFO));
        buttonInboxInfo.setToolTipText("Inbox information");
        buttonInboxInfo.setFocusable(false);
        buttonInboxInfo.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonInboxInfo.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonInboxInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonInboxInfoActionPerformed(evt);
            }
        });
        toolBar.add(buttonInboxInfo);
        toolBar.add(filler4);

        combobox_filter.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Filter recordings", "Reset filter", "Recieving", "Idle", "Scheduled", "Paused", "InProgress", "Complete", "Failed" }));
        combobox_filter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                combobox_filterActionPerformed(evt);
            }
        });
        toolBar.add(combobox_filter);
        toolBar.add(filler3);
        toolBar.add(filler1);

        label_network_status.setText(" ");
        label_network_status.setToolTipText("Network status: Checking status...");
        label_network_status.setIcon(DesktopUI.icons.get(UIIcon.NETWORK_WHITE));
        toolBar.add(label_network_status);

        splitPane.setDividerLocation(180);
        splitPane.setDividerSize(4);

        inboxList.setModel(inboxListModel);
        inboxList.setToolTipText("Right click an inbox to open its context menu");
        inboxList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                inboxListMouseClicked(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                inboxListMouseReleased(evt);
            }
        });
        inboxList.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                inboxListFocusGained(evt);
            }
        });
        inboxList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                inboxListKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(inboxList);

        splitPane.setLeftComponent(jScrollPane1);

        recordingList.setModel(recordingListModel);
        recordingList.setToolTipText("Right click a recording to open its context menu");
        recordingList.setCellRenderer(new RecordingListCellRenderer());
        recordingList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                recordingListMouseReleased(evt);
            }
        });
        recordingList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                recordingListKeyReleased(evt);
            }
            public void keyPressed(java.awt.event.KeyEvent evt) {
                recordingListKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                recordingListKeyTyped(evt);
            }
        });
        jScrollPane2.setViewportView(recordingList);

        splitPane.setRightComponent(jScrollPane2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(splitPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 750, Short.MAX_VALUE)
            .addComponent(toolBar, javax.swing.GroupLayout.DEFAULT_SIZE, 750, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(toolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(1, 1, 1)
                .addComponent(splitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 507, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void inboxListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_inboxListMouseClicked
        inboxFocused();
    }//GEN-LAST:event_inboxListMouseClicked

    private void inboxListFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_inboxListFocusGained
        inboxFocused();
    }//GEN-LAST:event_inboxListFocusGained

    private void inboxListKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_inboxListKeyReleased
        inboxFocused();
    }//GEN-LAST:event_inboxListKeyReleased

    private void inboxListMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_inboxListMouseReleased
        try {
            inboxList.setSelectedIndex(inboxList.locationToIndex(evt.getPoint()));
            currSelectedIndexP = inboxList.locationToIndex(evt.getPoint());
            if (SwingUtilities.isRightMouseButton(evt)) {
                if ((inboxList.getSelectedValue() != null) && (inboxList.getSelectedIndex() == currSelectedIndexP)) {
                    boolean recieving = false;
                    boolean inProgress = false;
                    boolean scheduled = false;
                    boolean paused = false;
                    boolean allTrimming = true;
                    Inbox inbox = (Inbox) inboxList.getSelectedValue();
                    if (inbox != null) {
                        for (Recording r : inbox.getRecordings()) {
                            if (r.getState().equals(State.RECIEVING)) {
                                recieving = true;
                            } else if (r.getState().equals(State.INPROGRESS)) {
                                inProgress = true;
                            } else if (r.getState().equals(State.SCHEDULED)) {
                                scheduled = true;
                            } else if (r.getState().equals(State.PAUSED)) {
                                paused = true;
                            }
                            allTrimming = allTrimming && r.getTrim();
                        }
                    }
                    inboxMenuItemInformation.setEnabled(true);
                    inboxMenuItemSeriesEditor.setEnabled(!(inProgress || scheduled || paused));
                    inboxMenuItemSelectSeries.setEnabled(!(inProgress || scheduled || paused));
                    inboxMenuItemSelectWorkflow.setEnabled(!(inProgress || scheduled || paused));
                    if (!allTrimming) {
                        inboxMenuItemTrim.setText(Constants.getInstance().getLocalizedString("Trim"));
                        inboxMenuItemTrim.setIcon(DesktopUI.icons.get(UIIcon.SCHEDULED_TRIM));
                    } else {
                        inboxMenuItemTrim.setText(Constants.getInstance().getLocalizedString("DontTrim"));
                        inboxMenuItemTrim.setIcon(DesktopUI.icons.get(UIIcon.SCHEDULED));
                    }
                    inboxMenuItemTrim.setEnabled(!(inProgress));
                    inboxMenuItemSceneDetection.setEnabled(!(inProgress));
                    inboxMenuItemDelete.setEnabled(!(recieving));
                } else {
                    inboxMenuItemInformation.setEnabled(false);
                    inboxMenuItemSeriesEditor.setEnabled(false);
                    inboxMenuItemSelectSeries.setEnabled(false);
                    inboxMenuItemSelectWorkflow.setEnabled(false);
                    inboxMenuItemTrim.setEnabled(false);
                    inboxMenuItemDelete.setEnabled(false);
                }
                inboxMenu.show(inboxList, evt.getX(), evt.getY());
            } else {
                inboxFocused();
            }
        } catch (Exception ex) {
        }
    }//GEN-LAST:event_inboxListMouseReleased

    private void recordingListMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_recordingListMouseReleased
        try {
            recordingList.setSelectedIndex(recordingList.locationToIndex(evt.getPoint()));
            selectedRecordingIndex = recordingList.locationToIndex(evt.getPoint());
            Recording recording = (Recording) recordingList.getSelectedValue();
            if ((recording != null) && application.inDatabase(recording)) {
                if (SwingUtilities.isRightMouseButton(evt)) {
                    checkContextMenuRecordings(recording);
                    recordingMenuItemUploadTrim.setSelected(recording.getTrim());
                    recordingMenu.show(recordingList, evt.getX(), evt.getY());
                } else {
                    recordingFocused();
                }
            } else {
                inboxList.setSelectedIndex(0);
            }
        } catch (Exception ex) {
        }
    }//GEN-LAST:event_recordingListMouseReleased

    private void combobox_filterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_combobox_filterActionPerformed
        try {
            JComboBox cb = (JComboBox) evt.getSource();
            String state = (String) cb.getSelectedItem();
            int select = recordingList.getSelectedIndex();
            if (state.toLowerCase().contains("reset")) {
                recordingListModel.filter(null);
                this.combobox_filter.setSelectedIndex(0);
            } else if (state.equalsIgnoreCase(State.RECIEVING.toString())) {
                recordingListModel.filter(State.RECIEVING);
            } else if (state.equalsIgnoreCase(State.IDLE.toString())) {
                recordingListModel.filter(State.IDLE);
            } else if (state.equalsIgnoreCase(State.SCHEDULED.toString())) {
                recordingListModel.filter(State.SCHEDULED);
            } else if (state.equalsIgnoreCase(State.PAUSED.toString())) {
                recordingListModel.filter(State.PAUSED);
            } else if (state.equalsIgnoreCase(State.INPROGRESS.toString())) {
                recordingListModel.filter(State.INPROGRESS);
            } else if (state.equalsIgnoreCase(State.COMPLETE.toString())) {
                recordingListModel.filter(State.COMPLETE);
            } else if (state.equalsIgnoreCase(State.FAILED.toString())) {
                recordingListModel.filter(State.FAILED);
            }
            if (select != -1) {
                recordingList.setSelectedIndex(select);
            }
            checkButtons();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    Constants.getInstance().getLocalizedString("SomethingWentWrong_msg"),
                    Constants.getInstance().getLocalizedString("SomethingWentWrong"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_combobox_filterActionPerformed

    private void recordingListKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_recordingListKeyPressed
        recordingFocused();
    }//GEN-LAST:event_recordingListKeyPressed

    private void recordingListKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_recordingListKeyReleased
        recordingFocused();
    }//GEN-LAST:event_recordingListKeyReleased

    private void recordingListKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_recordingListKeyTyped
        if (evt.getKeyChar() == '1') {
            markAs(State.IDLE);
        } else if (evt.getKeyChar() == '2') {
            markAs(State.COMPLETE);
        } else if (evt.getKeyChar() == '3') {
            markAs(State.FAILED);
        }
    }//GEN-LAST:event_recordingListKeyTyped

    private void buttonInboxInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonInboxInfoActionPerformed
        inboxInformation();
    }//GEN-LAST:event_buttonInboxInfoActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonInboxInfo;
    private javax.swing.JComboBox combobox_filter;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler3;
    private javax.swing.Box.Filler filler4;
    private javax.swing.JList inboxList;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel label_network_status;
    private javax.swing.JList recordingList;
    private javax.swing.JSplitPane splitPane;
    private javax.swing.JToolBar toolBar;
    // End of variables declaration//GEN-END:variables
}
