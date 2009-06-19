/*
 * TwitVizView.java
 */
package twitviz;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.DataColorAction;
import prefuse.action.assignment.DataSizeAction;
import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.activity.Activity;
import prefuse.controls.Control;
import prefuse.controls.DragControl;
import prefuse.controls.NeighborHighlightControl;
import prefuse.controls.PanControl;
import prefuse.controls.WheelZoomControl;
import prefuse.controls.ZoomControl;
import prefuse.controls.ZoomToFitControl;
import prefuse.data.Edge;
import prefuse.data.Graph;//Prefuse Visualization toolkit
import prefuse.data.Node;
import prefuse.data.io.DataIOException;
import prefuse.data.io.GraphMLReader;
import prefuse.data.io.GraphMLWriter;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.util.*;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;
import twitter4j.*;

/**
 * The application's main frame.
 */
public class TwitVizView extends FrameView {

    public TwitVizView(SingleFrameApplication app) {
        super(app);

        initComponents();

        label_explain.setText("");

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        this.getFrame().setBounds((screen.width - 400) / 2, (screen.height - 200) / 2, 400, 200);
        this.getFrame().setResizable(true);
        this.getFrame().setVisible(true);

    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = TwitVizApp.getApplication().getMainFrame();
            aboutBox = new TwitVizAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        TwitVizApp.getApplication().show(aboutBox);
    }

    //Visualization of keywords and strangers!
    private void displayKeyviz() {

        if (kwvis != null) {
            kwvis.removeGroup("graph");
            kwvis.addGraph("graph", kwgraph);
            kwvis.repaint();
            kwvis.run("color");  // assign the colors
            kwvis.run("size"); //assign the sizes
            kwvis.run("layout"); // start up the animated layout*/
            return; //this will avoid the threads from overlapping visualizations over and over making it slow!
        } else {
            kwvis = new Visualization();
            kwvis.addGraph("graph", kwgraph);
        }

        LabelRenderer rend = new CustomLabelRenderer();
        rend.setRoundedCorner(8, 8);

        kwvis.setRendererFactory(new DefaultRendererFactory(rend));

        int[] nodesColor = new int[]{
            ColorLib.rgb(153, 255, 153),
            ColorLib.rgb(255, 255, 153)
        };

        //Lets colorize! :D
        DataColorAction nodes = new DataColorAction("graph.nodes", "friend",
                Constants.NOMINAL, VisualItem.FILLCOLOR, nodesColor);

        ColorAction text = new ColorAction("graph.nodes", VisualItem.TEXTCOLOR, ColorLib.gray(0));

        ColorAction edges = new ColorAction("graph.edges", VisualItem.STROKECOLOR, ColorLib.rgb(0, 0, 0));

        ActionList color = new ActionList();
        color.add(nodes);
        color.add(text);
        color.add(edges);

        DataSizeAction sizes = new DataSizeAction("graph.nodes", "relevance");

        ActionList size = new ActionList();
        size.add(sizes);

        ActionList layout = new ActionList(Activity.INFINITY);
        layout.add(new ForceDirectedLayout("graph", true));
        layout.add(new RepaintAction());

        kwvis.putAction("color", color);
        kwvis.putAction("size", size);
        kwvis.putAction("layout", layout);

        Display display = new Display(kwvis);
        display.setSize(580, 430); //this is the size of the background image
        display.pan(580, 430);	// pan to the middle
        display.addControlListener(new DragControl());
        display.addControlListener(new PanControl());
        display.addControlListener(new ZoomControl());
        display.addControlListener(new WheelZoomControl());
        display.addControlListener(new ZoomToFitControl());
        display.addControlListener(new NeighborHighlightControl());

        display.addControlListener(new Control() {
            //When we click a user, not a keyword

            public void itemClicked(VisualItem item, MouseEvent e) {
                if (item instanceof NodeItem) {
                    try {
                        if (item.canGetLong("id") && item.getString("keyword").compareTo("null") == 0) {
                            getUserInfo(new Long(item.getLong("id")).intValue());
                        }
                    } catch (Exception xpto) {
                    }
                }
            }

            public void itemPressed(VisualItem item, MouseEvent e) {
            }

            public void itemReleased(VisualItem item, MouseEvent e) {
            }

            public void itemEntered(VisualItem item, MouseEvent e) {
                Cursor clickMe = panel_viz.getCursor();
                clickMe = new Cursor(Cursor.HAND_CURSOR);
            }

            public void itemExited(VisualItem item, MouseEvent e) {
            }

            public void itemKeyPressed(VisualItem item, KeyEvent e) {
            }

            public void itemKeyReleased(VisualItem item, KeyEvent e) {
            }

            public void itemKeyTyped(VisualItem item, KeyEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }

            public void mouseClicked(MouseEvent e) {
            }

            public void mouseDragged(MouseEvent e) {
            }

            public void mouseMoved(MouseEvent e) {
            }

            public void mouseWheelMoved(MouseWheelEvent e) {
            }

            public void keyPressed(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
            }

            public void keyTyped(KeyEvent e) {
            }

            public boolean isEnabled() {
                return true;
            }

            public void setEnabled(boolean enabled) {
            }

            public void itemDragged(VisualItem item, MouseEvent e) {
            }

            public void itemMoved(VisualItem item, MouseEvent e) {
            }

            public void itemWheelMoved(VisualItem item, MouseWheelEvent e) {
            }
        });

        // add the display (which holds the visualization) to the window
        keyword_viz.add(display);
        keyword_viz.validate();
        keyword_viz.setVisible(true);

        kwvis.repaint();

        kwvis.run("color");  // assign the colors
        kwvis.run("size"); //assign the sizes
        kwvis.run("layout"); // start up the animated layout

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        twitvizPanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        keywordsTextField = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        keyword_list = new javax.swing.JList();
        jLabel4 = new javax.swing.JLabel();
        addButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        countLabel = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        updateButton = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        updateTextArea = new javax.swing.JTextArea();
        section_tabs = new javax.swing.JTabbedPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        twittsList = new javax.swing.JList();
        jPanel1 = new javax.swing.JPanel();
        info_screenname = new javax.swing.JLabel();
        info_picture = new javax.swing.JLabel();
        info_name = new javax.swing.JLabel();
        btn_follow = new javax.swing.JButton();
        info_location = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        info_followerCount = new javax.swing.JLabel();
        asdasd = new javax.swing.JScrollPane();
        info_last_status = new javax.swing.JTextArea();
        jScrollPane5 = new javax.swing.JScrollPane();
        info_description = new javax.swing.JTextArea();
        jLabel9 = new javax.swing.JLabel();
        lbl_username = new javax.swing.JLabel();
        username = new javax.swing.JTextField();
        lbl_password = new javax.swing.JLabel();
        password = new javax.swing.JPasswordField();
        btn_login = new javax.swing.JButton();
        feedback_label = new javax.swing.JLabel();
        tabs_control = new javax.swing.JTabbedPane();
        keyword_viz = new javax.swing.JPanel();
        panel_viz = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        label_explain = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(twitviz.TwitVizApp.class).getContext().getResourceMap(TwitVizView.class);
        mainPanel.setBackground(resourceMap.getColor("mainPanel.background")); // NOI18N
        mainPanel.setName("mainPanel"); // NOI18N

        twitvizPanel.setBackground(resourceMap.getColor("twitvizPanel.background")); // NOI18N
        twitvizPanel.setName("twitvizPanel"); // NOI18N

        jPanel2.setBackground(resourceMap.getColor("jPanel2.background")); // NOI18N
        jPanel2.setName("jPanel2"); // NOI18N

        keywordsTextField.setForeground(resourceMap.getColor("keywordsTextField.foreground")); // NOI18N
        keywordsTextField.setText(resourceMap.getString("keywordsTextField.text")); // NOI18N
        keywordsTextField.setName("keywordsTextField"); // NOI18N
        keywordsTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                keywordsTextFieldFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                keywordsTextFieldFocusLost(evt);
            }
        });

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        keyword_list.setBackground(resourceMap.getColor("keyword_list.background")); // NOI18N
        keyword_list.setForeground(resourceMap.getColor("keyword_list.foreground")); // NOI18N
        keyword_list.setName("keyword_list"); // NOI18N
        keyword_list.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                keyword_listKeyPressed(evt);
            }
        });
        jScrollPane1.setViewportView(keyword_list);

        jLabel4.setFont(resourceMap.getFont("jLabel4.font")); // NOI18N
        jLabel4.setForeground(resourceMap.getColor("jLabel4.foreground")); // NOI18N
        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N

        addButton.setText(resourceMap.getString("addButton.text")); // NOI18N
        addButton.setName("addButton"); // NOI18N
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        removeButton.setText(resourceMap.getString("removeButton.text")); // NOI18N
        removeButton.setName("removeButton"); // NOI18N
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 214, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jLabel4)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, keywordsTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 214, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel2Layout.createSequentialGroup()
                        .add(addButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 83, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(removeButton)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(jLabel4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 28, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(keywordsTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(addButton)
                    .add(removeButton))
                .add(5, 5, 5)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 407, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel3.setBackground(resourceMap.getColor("jPanel3.background")); // NOI18N
        jPanel3.setName("jPanel3"); // NOI18N

        countLabel.setFont(resourceMap.getFont("countLabel.font")); // NOI18N
        countLabel.setForeground(resourceMap.getColor("countLabel.foreground")); // NOI18N
        countLabel.setText(resourceMap.getString("countLabel.text")); // NOI18N
        countLabel.setName("countLabel"); // NOI18N

        jLabel6.setFont(resourceMap.getFont("jLabel6.font")); // NOI18N
        jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
        jLabel6.setName("jLabel6"); // NOI18N

        updateButton.setText(resourceMap.getString("updateButton.text")); // NOI18N
        updateButton.setEnabled(false);
        updateButton.setName("updateButton"); // NOI18N
        updateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateButtonActionPerformed(evt);
            }
        });
        updateButton.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                updateButtonKeyReleased(evt);
            }
        });

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        updateTextArea.setColumns(20);
        updateTextArea.setLineWrap(true);
        updateTextArea.setRows(3);
        updateTextArea.setName("updateTextArea"); // NOI18N
        updateTextArea.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                updateTextAreaKeyReleased(evt);
            }
        });
        jScrollPane3.setViewportView(updateTextArea);

        section_tabs.setName("section_tabs"); // NOI18N

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        twittsList.setAutoscrolls(false);
        twittsList.setName("twittsList"); // NOI18N
        jScrollPane2.setViewportView(twittsList);

        section_tabs.addTab(resourceMap.getString("jScrollPane2.TabConstraints.tabTitle"), jScrollPane2); // NOI18N

        jPanel1.setBackground(resourceMap.getColor("jPanel1.background")); // NOI18N
        jPanel1.setName("jPanel1"); // NOI18N

        info_screenname.setText(resourceMap.getString("info_screenname.text")); // NOI18N
        info_screenname.setName("info_screenname"); // NOI18N

        info_picture.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        info_picture.setText(resourceMap.getString("info_picture.text")); // NOI18N
        info_picture.setName("info_picture"); // NOI18N

        info_name.setText(resourceMap.getString("info_name.text")); // NOI18N
        info_name.setName("info_name"); // NOI18N

        btn_follow.setText(resourceMap.getString("btn_follow.text")); // NOI18N
        btn_follow.setName("btn_follow"); // NOI18N

        info_location.setText(resourceMap.getString("info_location.text")); // NOI18N
        info_location.setName("info_location"); // NOI18N

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        info_followerCount.setFont(resourceMap.getFont("info_followerCount.font")); // NOI18N
        info_followerCount.setText(resourceMap.getString("info_followerCount.text")); // NOI18N
        info_followerCount.setName("info_followerCount"); // NOI18N

        asdasd.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        asdasd.setName("asdasd"); // NOI18N

        info_last_status.setColumns(20);
        info_last_status.setEditable(false);
        info_last_status.setLineWrap(true);
        info_last_status.setRows(5);
        info_last_status.setText(resourceMap.getString("info_last_status.text")); // NOI18N
        info_last_status.setName("info_last_status"); // NOI18N
        asdasd.setViewportView(info_last_status);

        jScrollPane5.setName("jScrollPane5"); // NOI18N

        info_description.setColumns(20);
        info_description.setEditable(false);
        info_description.setLineWrap(true);
        info_description.setRows(5);
        info_description.setName("info_description"); // NOI18N
        jScrollPane5.setViewportView(info_description);

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(asdasd, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 296, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel1Layout.createSequentialGroup()
                                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jLabel1)
                                    .add(info_location, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 64, Short.MAX_VALUE))
                                .add(18, 18, 18)
                                .add(info_followerCount, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(222, 222, 222))
                            .add(jPanel1Layout.createSequentialGroup()
                                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                                        .add(org.jdesktop.layout.GroupLayout.LEADING, info_screenname, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .add(org.jdesktop.layout.GroupLayout.LEADING, info_name, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .add(jScrollPane5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 207, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(btn_follow)
                                    .add(info_picture, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 85, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                        .add(49, 49, 49))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(info_screenname)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(info_name)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jScrollPane5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(info_picture, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 84, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(btn_follow)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 9, Short.MAX_VALUE)
                .add(info_location)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(info_followerCount, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 45, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(asdasd, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(61, 61, 61))
        );

        section_tabs.addTab(resourceMap.getString("jPanel1.TabConstraints.tabTitle"), jPanel1); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(section_tabs, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 360, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jPanel3Layout.createSequentialGroup()
                        .add(272, 272, 272)
                        .add(updateButton))
                    .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel3Layout.createSequentialGroup()
                            .add(jLabel6)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(countLabel))
                        .add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 352, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(countLabel))
                .add(6, 6, 6)
                .add(jScrollPane3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(updateButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(section_tabs, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 402, Short.MAX_VALUE)
                .addContainerGap())
        );

        jLabel9.setIcon(resourceMap.getIcon("jLabel9.icon")); // NOI18N
        jLabel9.setText(resourceMap.getString("jLabel9.text")); // NOI18N
        jLabel9.setName("jLabel9"); // NOI18N

        lbl_username.setText(resourceMap.getString("lbl_username.text")); // NOI18N
        lbl_username.setName("lbl_username"); // NOI18N

        username.setText(resourceMap.getString("username.text")); // NOI18N
        username.setToolTipText(resourceMap.getString("username.toolTipText")); // NOI18N
        username.setName("username"); // NOI18N

        lbl_password.setText(resourceMap.getString("lbl_password.text")); // NOI18N
        lbl_password.setName("lbl_password"); // NOI18N

        password.setText(resourceMap.getString("password.text")); // NOI18N
        password.setName("password"); // NOI18N
        password.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                passwordKeyPressed(evt);
            }
        });

        btn_login.setText(resourceMap.getString("btn_login.text")); // NOI18N
        btn_login.setName("btn_login"); // NOI18N
        btn_login.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_loginActionPerformed(evt);
            }
        });

        feedback_label.setFont(resourceMap.getFont("feedback_label.font")); // NOI18N
        feedback_label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        feedback_label.setText(resourceMap.getString("feedback_label.text")); // NOI18N
        feedback_label.setName("feedback_label"); // NOI18N

        tabs_control.setBackground(resourceMap.getColor("tabs_control.background")); // NOI18N
        tabs_control.setBorder(new javax.swing.border.LineBorder(resourceMap.getColor("tabs_control.border.lineColor"), 1, true)); // NOI18N
        tabs_control.setName("tabs_control"); // NOI18N
        tabs_control.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tabs_controlMouseClicked(evt);
            }
        });

        keyword_viz.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        keyword_viz.setName("keyword_viz"); // NOI18N

        org.jdesktop.layout.GroupLayout keyword_vizLayout = new org.jdesktop.layout.GroupLayout(keyword_viz);
        keyword_viz.setLayout(keyword_vizLayout);
        keyword_vizLayout.setHorizontalGroup(
            keyword_vizLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 582, Short.MAX_VALUE)
        );
        keyword_vizLayout.setVerticalGroup(
            keyword_vizLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 482, Short.MAX_VALUE)
        );

        tabs_control.addTab(resourceMap.getString("keyword_viz.TabConstraints.tabTitle"), null, keyword_viz, resourceMap.getString("keyword_viz.TabConstraints.tabToolTip")); // NOI18N

        panel_viz.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));
        panel_viz.setName("panel_viz"); // NOI18N

        org.jdesktop.layout.GroupLayout panel_vizLayout = new org.jdesktop.layout.GroupLayout(panel_viz);
        panel_viz.setLayout(panel_vizLayout);
        panel_vizLayout.setHorizontalGroup(
            panel_vizLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 582, Short.MAX_VALUE)
        );
        panel_vizLayout.setVerticalGroup(
            panel_vizLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 482, Short.MAX_VALUE)
        );

        tabs_control.addTab(resourceMap.getString("panel_viz.TabConstraints.tabTitle"), null, panel_viz, resourceMap.getString("panel_viz.TabConstraints.tabToolTip")); // NOI18N

        jPanel4.setBackground(resourceMap.getColor("jPanel4.background")); // NOI18N
        jPanel4.setName("jPanel4"); // NOI18N

        label_explain.setText(resourceMap.getString("label_explain.text")); // NOI18N
        label_explain.setName("label_explain"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(label_explain, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 574, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .add(label_explain, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 31, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout twitvizPanelLayout = new org.jdesktop.layout.GroupLayout(twitvizPanel);
        twitvizPanel.setLayout(twitvizPanelLayout);
        twitvizPanelLayout.setHorizontalGroup(
            twitvizPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(twitvizPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(twitvizPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel9, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 121, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(twitvizPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(twitvizPanelLayout.createSequentialGroup()
                        .add(twitvizPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(jPanel4, 0, 607, Short.MAX_VALUE)
                            .add(tabs_control))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .add(twitvizPanelLayout.createSequentialGroup()
                        .add(18, 18, 18)
                        .add(feedback_label, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 520, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 93, Short.MAX_VALUE)
                        .add(lbl_username)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(username, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 90, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(lbl_password)
                        .add(2, 2, 2)
                        .add(password, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 94, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(btn_login)
                        .add(69, 69, 69))))
        );
        twitvizPanelLayout.setVerticalGroup(
            twitvizPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(twitvizPanelLayout.createSequentialGroup()
                .add(twitvizPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(twitvizPanelLayout.createSequentialGroup()
                        .add(21, 21, 21)
                        .add(twitvizPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(jLabel9, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 38, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(feedback_label, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 37, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(twitvizPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .add(twitvizPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(password, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(username, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(lbl_password)
                            .add(lbl_username)
                            .add(btn_login))))
                .add(8, 8, 8)
                .add(twitvizPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jPanel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(tabs_control, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 515, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(46, 46, 46))
        );

        org.jdesktop.layout.GroupLayout mainPanelLayout = new org.jdesktop.layout.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(twitvizPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(twitvizPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(twitviz.TwitVizApp.class).getContext().getActionMap(TwitVizView.class, this);
        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setComponent(mainPanel);
        setMenuBar(menuBar);
    }// </editor-fold>//GEN-END:initComponents

    private void getUserInfo(int id) {
        try {
            final User node = link.getUserDetail(String.valueOf(id));

            if (node != null && !node.isProtected()) {
                section_tabs.setSelectedIndex(1);
                info_screenname.setText("Screenname: " + node.getScreenName());
                info_name.setText("Name: " + node.getName());
                info_description.setText(node.getDescription());
                info_location.setText("Location: " + node.getLocation());
                info_last_status.setText(node.getStatusText());
                info_picture.setIcon(new ImageIcon(node.getProfileImageURL()));
                info_followerCount.setText(String.valueOf(node.getFollowersCount()));

                if (node.getId() == user.getId()) {
                    btn_follow.setText("You!!!");
                    btn_follow.setEnabled(false);
                } else {
                    //Check if we follow already
                    if (!doWeFollow(node.getId())) { //If not following already...
                        btn_follow.setEnabled(true);
                        btn_follow.setText("Follow");
                        btn_follow.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent arg0) {
                                try {
                                    link.enableNotification(String.valueOf(node.getId()));
                                    setFeedback(node.getScreenName() + " followed successfully", Color.WHITE);
                                    //update the social network
                                    buildSocialNetwork();
                                    displayTwitviz();
                                } catch (TwitterException ex) {
                                    setFeedback("Unable to follow " + node.getScreenName() + "!", Color.RED);
                                }
                            }
                        });
                        btn_follow.repaint();
                        btn_follow.setVisible(true);
                    } else {
                        btn_follow.setEnabled(true);
                        btn_follow.setText("Unfollow");
                        btn_follow.addActionListener(new ActionListener() {

                            public void actionPerformed(ActionEvent arg0) {
                                try {
                                    link.destroyFriendship(String.valueOf(node.getId()));
                                    setFeedback(node.getScreenName() + " unfollowed successfully", Color.WHITE);
                                    //update the social network
                                    buildSocialNetwork();
                                    displayTwitviz();
                                } catch (TwitterException ex) {
                                    setFeedback("Unable to unfollow " + node.getScreenName() + "!", Color.RED);
                                }
                            }
                        });
                        btn_follow.repaint();
                        btn_follow.setVisible(true);
                    }
                }
            } else {
                setFeedback("Couldn't load user information!", Color.RED);
            }
        } catch (TwitterException ex) {
            setFeedback("Couldn't load user information!", Color.RED);
        }
    }

    //Function that gets a specific node by keyword, from the keyword graph.
    //--If it doesn't exist, creates one automatically and returns it
    private Node getKeywordFromGraph(String name) {

        Node key = null;
        for (int i = 0; i < kwgraph.getNodeCount(); i++) {
            key = kwgraph.getNode(i);
            //screenName == null when its a keyword node
            if(key.getString("keyword") != null)
                if (key.getString("keyword").compareToIgnoreCase(name) == 0) {
                    return key;
            }
        }

        key = kwgraph.addNode();
        key.setString("keyword", name);

        return key;
    }

    //Function that gets a specific node by given user node fro the keyword graph.
    //--If it doesn't exist, creates one automatically and returns it
    private Node getUserFromKeyGraph(User tweeterer) {
        int nodePosition = -1;
        Node tmp = null;
        Node user_stranger = null;

        for (int j = 0; j < kwgraph.getNodeCount(); j++) {
            tmp = kwgraph.getNode(j);

            if (tmp.getInt("id") == tweeterer.getId()) {
                nodePosition = j;
                break;
            }
        }

        if (nodePosition >= 0) {
            user_stranger = tmp;
            tmp = null;
        } else {
            user_stranger = kwgraph.addNode();
            user_stranger.setLong("id", tweeterer.getId());
            user_stranger.setString("screenName", tweeterer.getScreenName());
            user_stranger.setInt("relevance", 1);
            user_stranger.setBoolean("friend", isStrangerAFriend(tweeterer));
        }

        return user_stranger;
    }

    private boolean doWeFollow(int id) {
        IDs following;
        try {
            following = link.getFriendsIDs();

            for (int followed : following.getIDs()) {
                if (followed == id) {
                    return true;
                }
            }
            return false;
        } catch (TwitterException ex) {
            setFeedback("Unable to get who you follow list...", Color.RED);
        }
        return false;
    }

    //Function that checks if someone is following you back
    private boolean isFollowing(List<User> list, User who) {
        //list is your followers list...
        if (list != null)
            return list.contains(who);
        else
            return false;
    }

    private boolean isStrangerAFriend(User stranger) {
        boolean isFriend = false;

        for (int i = 0; i < graph.getNodeCount(); i++) {
            Node tmp = graph.getNode(i);
            if (tmp.getInt("id") == stranger.getId()) {
                isFriend = true;
                break;
            }
        }

        return isFriend;
    }

    //Function that builds the logged in user's network
    private void buildSocialNetwork() {

        Node source = null;
            if (graph.getNodeCount() > 0) {
                source = graph.getNode(0); //get the first node = logged on user
                if (user.getId() != source.getLong("id")) {
                    graph.clear();
                    source = graph.addNode();
                }
            }

        source.setLong("id", user.getId());
        source.setString("screenName", user.getScreenName());
        source.setBoolean("protectedUser", user.isProtected());
        source.setInt("relevance", 2);

        List<User> friends = null;
        try {
            friends = link.getFriends(Integer.toString(user.getId()));
        } catch (TwitterException ex) {
            setFeedback("Error loading friends", Color.RED);
        }

        List<User> followers = null;
        try {
            followers = link.getFollowers();
        } catch (TwitterException ex) {
            setFeedback("Error loading followers", Color.RED);
        }

        for (int i = 0; i < friends.size(); i++) {
            User friend = friends.get(i);

            //There are users with protected profiles and data
            if (!friend.isProtected()) {

                Node tmp = null;
                int friendPos = -1;
                for (int a = 0; a < graph.getNodeCount(); a++) {
                    tmp = graph.getNode(a);
                    if (tmp.getLong("id") == friend.getId()) {
                        friendPos = a;
                        break;
                    }
                }

                //New friend
                if (friendPos == -1) {
                    tmp = graph.addNode();
                //restore saved
                } else {
                    tmp = graph.getNode(friendPos);
                }

                //set node data
                tmp.setLong("id", friend.getId());
                tmp.setString("screenName", friend.getScreenName());
                tmp.setBoolean("protectedUser", friend.isProtected());

                //The relevance might already be a different number!
                if (friendPos == -1) {
                    tmp.setInt("relevance", 1);
                } else {
                    tmp.setInt("relevance", tmp.getInt("relevance"));
                }
                //--end node data

                //Link to source
                Edge relation = graph.getEdge(source, tmp);
                if (relation == null) {
                    graph.addEdge(source, tmp);
                    relation = graph.getEdge(source, tmp);
                }

                if (isFollowing(followers, friend)) {
                    tmp.setBoolean("follows", true);
                    relation.setInt("relationship", 2);
                } else {
                    tmp.setBoolean("follows", false);
                    relation.setInt("relationship", 1);
                }

                if (depth > 0) {
                    try {
                        getSubfriends(link.getFriends(Integer.toString(friend.getId())), tmp);
                    } catch (Exception e) {
                    }
                }
            }
        }

        //Save to graph file
        try {
            graphWriter.writeGraph(graph, networkFile);
        } catch (DataIOException e) {
            e.printStackTrace();
        }
    //--end save graph file
    }

    //Recursive function that will get friends of a friend
    private void getSubfriends(List<User> friends, Node source) {
        depth--;
        if (depth > 0) {

            for (User friend : friends) {
                //There are users with protected profiles and data
                List<User> followers = null;
                try {
                    followers = link.getFollowers(Integer.toString(friend.getId()));
                } catch (TwitterException ex) {
                    Logger.getLogger(TwitVizView.class.getName()).log(Level.SEVERE, null, ex);
                }

                if (!friend.isProtected()) {
                    Node tmp = null;
                    int friendPos = -1;
                    for (int a = 0; a < graph.getNodeCount(); a++) {
                        tmp = graph.getNode(a);
                        if (tmp.getLong("id") == friend.getId()) {
                            friendPos = a;
                            break;
                        }
                    }

                    //New friend
                    if (friendPos == -1) {
                        tmp = graph.addNode();
                    //restore saved
                    } else {
                        tmp = graph.getNode(friendPos);
                    }

                    //set node data
                    tmp.setLong("id", friend.getId());
                    tmp.setString("screenName", friend.getScreenName());
                    tmp.setBoolean("protectedUser", friend.isProtected());

                    //The relevance might already be a different number!
                    if (friendPos == -1) {
                        tmp.setInt("relevance", 1);
                    } else {
                        tmp.setInt("relevance", tmp.getInt("relevance"));
                    }
                    //--end node data

                    //Link to source
                    Edge relation = graph.getEdge(source, tmp);
                    if (relation == null) {
                        graph.addEdge(source, tmp);
                        relation = graph.getEdge(source, tmp);
                    }

                    if (isFollowing(followers, friend)) {
                        tmp.setBoolean("follows", true);
                        relation.setInt("relationship", 2);
                    } else {
                        tmp.setBoolean("follows", false);
                        relation.setInt("relationship", 1);
                    }

                    try {
                        getSubfriends(link.getFriends(Integer.toString(friend.getId())), tmp);
                    } catch (Exception e) {
                    }
                }
            }
        }
    }
    
    private void cleanUpGUI() {

        // reset social network tab
        panel_viz.removeAll();
        panel_viz.validate();
        panel_viz.repaint();

        // reset keywords tab
        keyword_viz.removeAll();
        keyword_viz.validate();
        keyword_viz.repaint();
        updateButton.setEnabled(false);
        addButton.setEnabled(false);
        removeButton.setEnabled(false);

        // reset keywords textbox
        keywordsTextField.setText("Keywords");
        keywordsTextField.setForeground(Color.GRAY);

        // reset keywords list
        keywordsmap.clear();
        //keyword_list.setModel(keywordsmap);

        // reset update text area
        updateTextArea.setText("");

        // reset home screen
        twittsList.setModel(new DefaultListModel());
        twittsList.validate();
        twittsList.repaint();

        // reset information screen
        info_screenname.setText("Screenname: ");
        info_name.setText("Name: ");
        info_description.setText("");
        info_location.setText("Location: ");
        info_last_status.setText("");
        info_picture.setIcon(null);
        info_followerCount.setText("");
        btn_follow.setEnabled(false);
        btn_follow.setText("Follow");
    }

    private void btn_loginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_loginActionPerformed

        //trying to logout
        if (btn_login.getText().equals("Logout")) {
            //restore GUI to initial state (pre-login)
            lbl_username.setVisible(true);
            username.setText("");
            username.setVisible(true);
            lbl_password.setVisible(true);
            password.setText("");
            password.setVisible(true);

            btn_login.setText("Login");

            // clean-up session information
            link = null;
            user = null;
            graph = null;
            kwgraph = null;

            cleanUpGUI();
        } else {
            if (btn_login.getText().equals("Login")) {

                try {
                    //user logging in for the first time
                    if (username.getText().length() > 0 && password.getPassword().length > 0) {

                        //establish connection to twitter servers using given credentials
                        link = new Twitter(username.getText(), String.valueOf(password.getPassword()));

                        // first verify given credentials to prevent bad logins
                        User valid_cred = link.verifyCredentials();
                        if (valid_cred != null) {

                            //Lets make publicity :D
                            link.setUserAgent("TwitViz");
                            link.setClientVersion("TwitViz 1.0");
                            link.setSource("TwitViz");

                            if (link != null) {
                                user = link.getUserDetail(username.getText());

                                //Organize GUI
                                lbl_username.setVisible(false);
                                username.setVisible(false);
                                lbl_password.setVisible(false);
                                password.setVisible(false);
                                btn_login.setText("Logout");

                                updateButton.setEnabled(true);
                                addButton.setEnabled(true);
                                removeButton.setEnabled(true);

                                //restore saved databases
                                try {

                                    if (networkFile.exists()) {
                                        graph = graphReader.readGraph("twitviz.xml");
                                    }

                                    if (keywordsFile.exists()) {
                                        kwgraph = graphReader.readGraph("kwviz.xml");
                                    } /*else {
                                        kwgraph = new Graph(true);
                                    }*/

                                } catch (DataIOException e) {
                                    e.printStackTrace();
                                    setFeedback(e.getMessage() + "Please restart the application", Color.RED);
                                }

                                buildSocialNetwork();

                                displayTwitviz();

                                //load previous stored keywords
                                loadKeywords();
                                
                                //Start public line monitor, updates every 20 seconds, in order to get twitters related to the specified keywords
                                java.util.Timer timer = new java.util.Timer();
                                timer.scheduleAtFixedRate(new java.util.TimerTask() {

                                    public void run() {
                                        get_PublicLine();
                                    }
                                }, 5000, refreshInterval);

                                getUserInfo(user.getId());

                                //By default we want to display our network...
                                tabs_control.setSelectedIndex(1);

                                setTwittsList();
                            }
                    }
                    else{
                        setFeedback("Bad login! Please try again.", Color.RED);
                    }
                } else{
                    setFeedback("Username and password are needed!", Color.RED);
                }

                } catch (TwitterException ex) {
                    if(ex.getStatusCode() == 401)
                        setFeedback("Bad login!! Please try again.", Color.RED);
                    else
                        setFeedback("Error getting user information, please try again...", Color.RED);
                }

            }

        }
}//GEN-LAST:event_btn_loginActionPerformed

    private void get_PublicLine() {
        //we only start processing the public line if we have keywords on the list!
        if (keywordsmap.getSize() > 0) {

            try {
                List<Status> publicStatus = link.getPublicTimeline();

                for(Status stat : publicStatus){

                    //If the user has something on the text, etc that we are interested in...
                    Vector<String> interests = getInterests(stat);

                    if (interests.size() > 0) {

                        User tweeterer = stat.getUser();
                        if (!tweeterer.isProtected()) {

                            //check if we already have the user or not    
                            Node familiar_stranger = getUserFromKeyGraph(tweeterer);

                            if (familiar_stranger != null) {

                                //Relevance will change according to how important the user is: number of keywords in common
                                familiar_stranger.setInt("relevance", familiar_stranger.getInt("relevance") + interests.size());

                                //Associate the person to the keywords we are following and he said!
                                //for (int k = 0; k < keywordsmap.getSize(); k++) {
                                    //Node keyword = getKeywordFromGraph((String) keywordsmap.get(k));

                                    for (String s : interests) {
                                        Node keyword = getKeywordFromGraph(s);
                                        if(keyword.getString("keyword") != null)
                                        {
                                            //if (keyword.getString("keyword").compareToIgnoreCase(s) == 0){
                                                kwgraph.addEdge(keyword, familiar_stranger);
                                            //}
                                        }
                                    }
                                //}
                            }
                        }
                    }
                }

                //Save to graph file
                try {
                    graphWriter.writeGraph(kwgraph, keywordsFile);
                } catch (DataIOException e) {
                    e.printStackTrace();
                }
                //--end save graph file

                //Reload visualization
                displayKeyviz();
            } catch (TwitterException ex) {
                setFeedback("Error loading public line :(", Color.RED);
            }
        }
    }


    //Returns the keywords present on a tweet
    private Vector<String> getInterests(Status stat) {
        Vector keywords = new Vector();
        for (int i = 0; i < keywordsmap.getSize(); i++) {
            if (stat.getText().matches(".*" + (String) keywordsmap.get(i) + ".*")) {
                keywords.addElement((String) keywordsmap.get(i));
            }
        }
        return keywords;
    }

    //Returns the keywords present on a tweet
    private Vector<String> getInterests(Tweet stat) {
        Vector keywords = new Vector();
        for (int i = 0; i < keywordsmap.getSize(); i++) {
            if (stat.getText().matches(".*" + (String) keywordsmap.get(i) + ".*")) {
                keywords.addElement((String) keywordsmap.get(i));
            }
        }
        return keywords;
    }

    //Load previous selected keywords
    private void loadKeywords() {

        Node key = null;
        //try {
            for (int i = 0; i < kwgraph.getNodeCount(); i++) {
                key = kwgraph.getNode(i);
                if (key.getString("keyword") != null) {
                    //screenName == null when it's a keyword node
                    if (key.getString("keyword").compareTo("null") != 0) {
                        keywordsmap.addElement(key.getString("keyword"));
                        //searchForKeyword(key.getString("keyword"));
                    }
                }
            }
            keyword_list.setModel(keywordsmap);
        /*} catch(TwitterException e) {
            setFeedback("Couldn't connect to Twitter! Please try again later", Color.RED);
        }*/
    }

    //Update the feedback_label to specific message + color
    private void setFeedback(String message, Color color) {
        feedback_label.setText(message);
        feedback_label.setForeground(color);
        //force update on GUI
        feedback_label.validate();
        feedback_label.repaint();

        //Start alternative thread to go away with the message on the top label
        java.util.Timer timer = new java.util.Timer();
        timer.schedule(new java.util.TimerTask() {

            public void run() {
                feedback_label.setText("");
                //force update on GUI
                feedback_label.validate();
                feedback_label.repaint();
            }
        }, feedbackTimeInterval);
    }

    private void updateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateButtonActionPerformed
        try {
            link.updateStatus(updateTextArea.getText());
            updateTextArea.setText("");
            setTwittsList();
            setFeedback("Updated status successful", Color.WHITE);
        } catch (TwitterException ex) {
            setFeedback("Error updating status", Color.RED);
        }

}//GEN-LAST:event_updateButtonActionPerformed

    private void keywordsTextFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_keywordsTextFieldFocusGained
        if (keywordsTextField.getText().compareTo("Keywords") == 0) {
            keywordsTextField.setText("");
            keywordsTextField.setForeground(Color.BLACK);
        }
    }//GEN-LAST:event_keywordsTextFieldFocusGained

    private void keywordsTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_keywordsTextFieldFocusLost
        if (keywordsTextField.getText().length() == 0) {
            keywordsTextField.setText("Keywords");
            keywordsTextField.setForeground(Color.GRAY);
        }
    }//GEN-LAST:event_keywordsTextFieldFocusLost

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed

        if (keywordsTextField.getText().length() > 0) {

            // add keyword to keywords map and find related twitters
            boolean alreadyDefined = false;
            for (int i = 0; i < keywordsmap.size(); i++) {
                if (((String) keywordsmap.get(i)).compareToIgnoreCase(keywordsTextField.getText()) == 0) {
                    alreadyDefined = true;
                    break;
                }
            }
            if (!alreadyDefined) {
                keywordsmap.addElement(keywordsTextField.getText());
                keyword_list.setModel(keywordsmap);

                setFeedback("Keyword added successfully", Color.WHITE);

                try {
                    searchForKeyword(keywordsTextField.getText());
                } catch (TwitterException ex) {
                    setFeedback("Cannot connect to Twitter. Try again later. Error: " + String.valueOf(ex.getStatusCode()), Color.RED);
                }

                // empty keyword text field
                keywordsTextField.setText("");
                keywordsTextField.setForeground(Color.BLACK);

                //Save to graph file
                try {
                    graphWriter.writeGraph(kwgraph, keywordsFile);
                } catch (DataIOException e) {
                    e.printStackTrace();
                }
                //--end save graph file

                //refresh the keyword visualization
                tabs_control.setSelectedIndex(0);
                displayKeyviz();
            } else {
                setFeedback("Keyword already exists!", Color.WHITE);
                // empty keyword text field
                keywordsTextField.setText("Keywords");
                keywordsTextField.setForeground(Color.GRAY);

            }
        } else {
            setFeedback("You need to type a keyword...", Color.RED);
            // empty keyword text field
            keywordsTextField.setText("Keywords");
            keywordsTextField.setForeground(Color.GRAY);
        }
    }//GEN-LAST:event_addButtonActionPerformed

    /* searches for users related to a given keyword, by their respective twitts (which contain that keyword),
     * constructs their respective nodes and adds them to the keyword graph (through the method processTwitts())
    */
    private void searchForKeyword(String keyword) throws TwitterException {

        QueryResult results = null;
        List<Tweet> twitts = null;
        Query queryString = new Query(keyword);
        results = link.search(queryString);
        twitts = results.getTweets();

        processTwitts(twitts, keyword);

        //Save to graph file
        try {
            graphWriter.writeGraph(kwgraph, keywordsFile);
        } catch (DataIOException e) {
            e.printStackTrace();
        }
        //--end save graph file
    }

    private void passwordKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_passwordKeyPressed
        if (evt.getKeyCode() == 10) {
            btn_login.doClick();
        }
    }//GEN-LAST:event_passwordKeyPressed

    // Last 20 friends twitts list Renderer
    public class IconListRenderer extends DefaultListCellRenderer {

        private Map<Object, Icon> icons = null;

        public IconListRenderer(Map<Object, Icon> icons) {
            this.icons = icons;
        }

        @Override
        public Component getListCellRendererComponent(
                JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {

            // Get the renderer component from parent class

            JLabel label =
                    (JLabel) super.getListCellRendererComponent(list,
                    value, index, isSelected, cellHasFocus);

            // Get icon to use for the list item value

            Icon icon = icons.get(value);

            // Set icon to display for value

            label.setSize(12, 12);

            //label.setLayout(new GridLayout());

            label.setIcon(icon);
            label.setVisible(true);
            return label;
        }
    }

    private void setTwittsList() {
        //Last 20 friends twitts list content
        try {

            List<Status> statusList = link.getFriendsTimeline();

            twittsList.setVisibleRowCount(statusList.size());
            twittsList.setLayoutOrientation(JList.HORIZONTAL_WRAP);

            String twitt = null;
            ImageIcon icon = null;
            Map<Object, Icon> icons = new HashMap<Object, Icon>();
            Object[] obj = new Object[statusList.size()];
            Image img;
            for (int i = 0; i < statusList.size(); i++) {

                Status status = statusList.get(i);
                twitt = status.getUser().getScreenName() + " " + status.getText() + "\n" + status.getCreatedAt().toString();

                icon = new ImageIcon(status.getUser().getProfileImageURL());
                img = icon.getImage();
                icons.put(twitt, icon);
                BufferedImage bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                Graphics g = bi.createGraphics();
                g.drawImage(img, 0, 0, 24, 24, null);
                icon = new ImageIcon(bi);

                obj[i] = twitt;
            }

            twittsList.setListData(obj);
            twittsList.setCellRenderer(new IconListRenderer(icons));
            twittsList.setVisible(true);
            twittsList.repaint();

        } catch (TwitterException ex) {
            setFeedback("Error loading friends timeline", Color.RED);
        }
    }

    private void updateButtonKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_updateButtonKeyReleased
        setTwittsList();
    }//GEN-LAST:event_updateButtonKeyReleased

    private void updateTextAreaKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_updateTextAreaKeyReleased
        int cl = 140 - (updateTextArea.getText().length());
        if (cl < 0) {
            countLabel.setForeground(Color.RED);
        } else {
            countLabel.setForeground(Color.GRAY);
        }
        countLabel.setText(Integer.toString(cl));

    }//GEN-LAST:event_updateTextAreaKeyReleased

    //Delete the selected keyword
    private void keyword_listKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_keyword_listKeyPressed

        //if we hit the backspace key
        if (evt.getKeyCode() == 8 && keywordsmap.size() > 1) {

            //delete edges and nodes related to the keyword
            for (int i = 0; i < kwgraph.getNodeCount(); i++) {

                Node tmp = kwgraph.getNode(i);

                if (tmp.isValid()) {
                    //If it is a keyword
                    if (tmp.getString("keyword") != null) {
                        if (tmp.getString("keyword").compareTo("null") != 0 && tmp.getString("keyword").compareToIgnoreCase((String) keywordsmap.elementAt(keyword_list.getSelectedIndex())) == 0) {

                            //getChild to remove dependencies
                            for (int j = 0; j < tmp.getChildCount(); j++) {
                                Node child = tmp.getChild(j);
                                if (child.isValid()) {
                                    kwgraph.removeNode(child);
                                }
                            }
                            kwgraph.removeNode(tmp);
                            break;
                        }
                    }
                }
            }

            //Save to graph file
            try {
                graphWriter.writeGraph(kwgraph, keywordsFile);
            } catch (DataIOException e) {
                e.printStackTrace();
            }
            //--end save graph file

            keywordsmap.remove(keyword_list.getSelectedIndex());

            setFeedback("Keyword removed successfully", Color.WHITE);
            displayKeyviz();

        } else {
            setFeedback("There must be at least one keyword...", Color.WHITE);
        }
    }//GEN-LAST:event_keyword_listKeyPressed

    private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed
        if (keywordsmap.size() > 1) {
            //delete edges and nodes related to the keyword
            for (Iterator<Node> keywordNodes = kwgraph.nodes(); keywordNodes.hasNext(); ) {
            //for (int i = 0; i < kwgraph.getNodeCount(); i++) {

                //Node tmp = kwgraph.getNode(i);
                Node tmp = keywordNodes.next();

                if (tmp.isValid()) {
                    //If it is a keyword
                    if(tmp.getString("keyword") != null) {
                        if (tmp.getString("keyword").compareTo("null") != 0 && tmp.getString("keyword").compareToIgnoreCase((String) keywordsmap.elementAt(keyword_list.getSelectedIndex())) == 0) {

                            int nTweeters = tmp.getOutDegree();
                            if (nTweeters > 0) {
                                for (Iterator<Node> tweeters = tmp.outNeighbors(); tweeters.hasNext(); ) {
                                    Node tweeter = tweeters.next();
                                    if(tweeter.getInDegree() == 1) {
                                        //tweeters.remove();
                                        kwgraph.removeNode(tweeter);
                                    }
                                    tweeters = tmp.outNeighbors();
                                }
                                kwgraph.removeNode(tmp);
                                //getChild to remove dependencies
                                /*for (int j = 0; j < nKeywords; j++) {
                                    Node child = tmp.;
                                    if (child.isValid()) {
                                        // if user node is connected to another keyword node, then remove only the edge between the current keyword node and the user node
                                        /*if(child. == 0)
                                        {*/
                                        //kwgraph.removeNode(child);
                                    /*}
                                    else
                                    {
                                    kwgraph.removeEdge(child.getParentEdge());
                                    }*/
                                    /*}
                                }
                                kwgraph.removeNode(tmp);
                                break;*/
                            }

                        }
                    }
                }
                keywordNodes = kwgraph.nodes();
            }

            //Save to graph file
            try {
                graphWriter.writeGraph(kwgraph, keywordsFile);
            } catch (DataIOException e) {
                e.printStackTrace();
            }
            //--end save graph file

            keywordsmap.remove(keyword_list.getSelectedIndex());

            setFeedback("Keyword removed successfully", Color.WHITE);
            displayKeyviz();

        } else {
            setFeedback("There must be at least one keyword...", Color.WHITE);
        }
}//GEN-LAST:event_removeButtonActionPerformed

    private void tabs_controlMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tabs_controlMouseClicked
        // TODO add your handling code here:
        switch(tabs_control.getSelectedIndex()) {
            case 0:
                label_explain.setText("Here you can see who is twitting about your keywords...");
                break;
            case 1:
                label_explain.setText("Here you can see people you are following. Green are those who follow you back!");
                break;
            default:
                label_explain.setText("");
                break;
        }
    }//GEN-LAST:event_tabs_controlMouseClicked

    private void processTwitts(List<Tweet> twitts, String keywordString) {

        for (Tweet twitt : twitts) {
            User twitterer;
            try {
                twitterer = link.getUserDetail(twitt.getFromUser());

                if (twitterer != null && !twitterer.isProtected()) {

                    Node familiar_stranger = getUserFromKeyGraph(twitterer); // this already creates the user node
                    Node keyword = getKeywordFromGraph(keywordString); // this already creates the keyword node
                    Vector<String> interests = getInterests(twitt);

                    // Complement tweeter base relevance with number of twitts in common
                    familiar_stranger.setInt("relevance", familiar_stranger.getInt("relevance") + interests.size());
                    // Register status text responsible for the established connection between keyword(s) and tweeter
                    familiar_stranger.setString("status_text", twitt.getText());

                    // Create connection between keyword and tweeter
                    kwgraph.addEdge(keyword, familiar_stranger);

                    // Check if tweeter is associated with the other keywords
                    for (String s : interests) {
                        if (s.compareToIgnoreCase(keywordString) != 0) { // The keyword introduced by the user has already been considered
                            Node k = getKeywordFromGraph(s);
                            if(k.getString("keyword") != null) {
                                kwgraph.addEdge(k, familiar_stranger);

                            }
                        }
                    }

                }
            } catch (TwitterException ex) {
                setFeedback("Error getting a result user's info...", Color.RED);
            }
        }
    }

    private void displayTwitviz() {

        if (vis != null) {
            vis.removeGroup("graph");
            vis.add("graph", graph);
            vis.repaint();
            vis.run("colour");
            vis.run("layout");
            vis.run("size");
        } else {
            vis = new Visualization();
            vis.add("graph", graph);
        }

        // draw the "name" label for NodeItems
        LabelRenderer r = new LabelRenderer("screenName");
        r.setRoundedCorner(8, 8); // round the corners

        vis.setRendererFactory(new DefaultRendererFactory(r));

        int[] edgesColor = new int[]{
            ColorLib.rgb(255, 255, 153),
            ColorLib.rgb(153, 255, 153)
        };

        //Lets colorize! :D
        DataColorAction nodes = new DataColorAction("graph.nodes", "follows",
                Constants.NOMINAL, VisualItem.FILLCOLOR, edgesColor);

        ColorAction text = new ColorAction("graph.nodes", VisualItem.TEXTCOLOR, ColorLib.gray(0));

        DataColorAction edges = new DataColorAction("graph.edges", "relationship",
                Constants.NOMINAL, VisualItem.STROKECOLOR, edgesColor);

        ActionList color = new ActionList();
        color.add(nodes);
        color.add(text);
        color.add(edges);

        DataSizeAction sizes = new DataSizeAction("graph.nodes", "relevance");

        ActionList size = new ActionList();
        size.add(sizes);

        ActionList layout = new ActionList(Activity.INFINITY);
        layout.add(new ForceDirectedLayout("graph"));
        layout.add(new RepaintAction());

        vis.putAction("color", color);
        vis.putAction("size", size);
        vis.putAction("layout", layout);

        Display display = new Display(vis);
        display.setSize(580, 430); //this is the size of the background image
        display.pan(300, 230);	// pan to the middle
        display.addControlListener(new DragControl());
        display.addControlListener(new PanControl());
        display.addControlListener(new ZoomControl());
        display.addControlListener(new WheelZoomControl());
        display.addControlListener(new ZoomToFitControl());
        display.addControlListener(new NeighborHighlightControl());

        display.addControlListener(new Control() {

            public void itemClicked(VisualItem item, MouseEvent e) {
                if (item instanceof NodeItem) {
                    try {
                        if (item.canGetLong("id")) {
                            getUserInfo(new Long(item.getLong("id")).intValue()); //dirty hack to get int from long...
                        }
                    } catch (Exception xpto) {
                    }
                }
            }

            public void itemPressed(VisualItem item, MouseEvent e) {
            }

            public void itemReleased(VisualItem item, MouseEvent e) {
            }

            public void itemEntered(VisualItem item, MouseEvent e) {
                Cursor clickMe = panel_viz.getCursor();
                clickMe = new Cursor(Cursor.HAND_CURSOR);
            }

            public void itemExited(VisualItem item, MouseEvent e) {
            }

            public void itemKeyPressed(VisualItem item, KeyEvent e) {
            }

            public void itemKeyReleased(VisualItem item, KeyEvent e) {
            }

            public void itemKeyTyped(VisualItem item, KeyEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }

            public void mouseClicked(MouseEvent e) {
            }

            public void mouseDragged(MouseEvent e) {
            }

            public void mouseMoved(MouseEvent e) {
            }

            public void mouseWheelMoved(MouseWheelEvent e) {
            }

            public void keyPressed(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
            }

            public void keyTyped(KeyEvent e) {
            }

            public boolean isEnabled() {
                return true;
            }

            public void setEnabled(boolean enabled) {
            }

            public void itemDragged(VisualItem item, MouseEvent e) {
            }

            public void itemMoved(VisualItem item, MouseEvent e) {
            }

            public void itemWheelMoved(VisualItem item, MouseWheelEvent e) {
            }
        });

        // add the display (which holds the visualization) to the window
        panel_viz.add(display);
        panel_viz.validate();
        panel_viz.setVisible(true);

        vis.run("color");  // assign the colors
        vis.run("size"); //assign the sizes
        vis.run("layout"); // start up the animated layout
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JScrollPane asdasd;
    private javax.swing.JButton btn_follow;
    private javax.swing.JButton btn_login;
    private javax.swing.JLabel countLabel;
    private javax.swing.JLabel feedback_label;
    private javax.swing.JTextArea info_description;
    private javax.swing.JLabel info_followerCount;
    private javax.swing.JTextArea info_last_status;
    private javax.swing.JLabel info_location;
    private javax.swing.JLabel info_name;
    private javax.swing.JLabel info_picture;
    private javax.swing.JLabel info_screenname;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JList keyword_list;
    private javax.swing.JPanel keyword_viz;
    private javax.swing.JTextField keywordsTextField;
    private javax.swing.JLabel label_explain;
    private javax.swing.JLabel lbl_password;
    private javax.swing.JLabel lbl_username;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JPanel panel_viz;
    private javax.swing.JPasswordField password;
    private javax.swing.JButton removeButton;
    private javax.swing.JTabbedPane section_tabs;
    private javax.swing.JTabbedPane tabs_control;
    private javax.swing.JList twittsList;
    private javax.swing.JPanel twitvizPanel;
    private javax.swing.JButton updateButton;
    private javax.swing.JTextArea updateTextArea;
    private javax.swing.JTextField username;
    // End of variables declaration//GEN-END:variables


    // Constants
    private static final int refreshInterval = 55000;
    private static final int feedbackTimeInterval = 10000;

    //Twitter API vars
    private Twitter link = null;
    private User user = null;

    // File variables
    File networkFile = new File("twitviz.xml");
    File keywordsFile = new File("kwviz.xml");

    //Prefuse vars
    private Graph graph;
    private Graph kwgraph;
    private Visualization vis = null;
    private Visualization kwvis = null;
    private GraphMLWriter graphWriter = new GraphMLWriter();
    private GraphMLReader graphReader = new GraphMLReader();

    //Keywords var
    private DefaultListModel keywordsmap = new DefaultListModel();
    //private final Timer messageTimer;
    //private final Timer busyIconTimer;
    //private final Icon idleIcon;
    //private final Icon[] busyIcons = new Icon[15];
    //private int busyIconIndex = 0;

    //Twitviz parameters
    private int depth = 1;
    private JDialog aboutBox;
}
