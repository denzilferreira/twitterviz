/*
 * TwitVizView.java
 */

package twitviz;

import java.awt.Color;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.ItemAction;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.filter.GraphDistanceFilter;
import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.activity.Activity;
import prefuse.controls.Control;
import prefuse.controls.ControlAdapter;
import prefuse.controls.DragControl;
import prefuse.controls.NeighborHighlightControl;
import prefuse.controls.PanControl;
import prefuse.controls.ToolTipControl;
import prefuse.controls.WheelZoomControl;
import prefuse.controls.ZoomControl;
import prefuse.controls.ZoomToFitControl;
import prefuse.data.Graph;//Prefuse Visualization toolkit
import prefuse.data.Node;
import prefuse.data.io.DataIOException;
import prefuse.data.io.GraphMLReader;
import prefuse.data.io.GraphMLWriter;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.EdgeRenderer;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.InGroupPredicate;
import winterwell.jtwitter.Twitter;//JavaDoc for Twitter Java API: http://www.winterwell.com/software/jtwitter/javadoc/

/**
 * The application's main frame.
 */
public class TwitVizView extends FrameView {

    private Graph graph;
    private Visualization vis;
    private GraphMLWriter graphWriter;
    private GraphMLReader graphReader;

    public TwitVizView(SingleFrameApplication app) {
        super(app);

        initComponents();

        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String)(evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer)(evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });
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

    //recursive function that will fill out the rest of the nodes relationships
    public void getFriendsOfFriends(Twitter link, int user_id, Node origin) {

        List<Twitter.User> following = link.getFriends(Integer.toString(user_id));

        int max = (following.size()>5)?5:following.size();

        for(int j=0; j<max; j++) {
            Twitter.User tmp = following.get(j);

            //there are users that like privacy, so we bypass them...
            if(!tmp.isProtectedUser()) {

                Node follower = null;
                for(int k=0;k<graph.getNodeCount();k++) {
                    if(graph.getNode(k).getLong("id")==tmp.getId()) {
                        follower = graph.getNode(k);
                        break;
                    }
                }

                if(follower == null) {
                    follower = graph.addNode();
                    //connect friend with user
                    graph.addEdge(origin, follower);
                }

                follower.setString("description",tmp.getDescription());
                follower.setLong("id", tmp.getId());
                follower.setString("location", tmp.getLocation());
                follower.setString("name", tmp.getName());
                follower.setString("profileImageUrl", tmp.getProfileImageUrl().toString());
                follower.setBoolean("protectedUser", tmp.isProtectedUser());
                follower.setString("screenName", tmp.getScreenName());

                if(tmp.getStatus()!=null) {
                    follower.setString("status", tmp.getStatus().getText());
                }else follower.setString("status", "");

                if(tmp.getWebsite()!=null) {
                    follower.setString("website", tmp.getWebsite().toString());
                }else follower.setString("website", "");
                
                //getFriendsOfFriends(link, tmp.getId(), follower);
            }
        }
    }

    //Function used to display visualization
    public void displayTwitviz(Twitter link, String filter) {
        Twitter.User user = link.show(username.getText());
        
        //Load previous recorded data
        try {
            graph = new Graph();
            graph = new GraphMLReader().readGraph("twitviz.xml");
            
        } catch (DataIOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        int i=0;
        int nodePosition = -1;

        //if we have already this id on the database

        if(graph.getNodeCount()>0) {
            while(i<graph.getNodeCount()) {
                if(graph.getNode(i).getLong("id")==user.getId()) {
                    nodePosition = i;
                    break;
                }
                i++;
            }
        }

        Node viz_node;

        if(nodePosition>=0) {//update previous recorded node
            viz_node = graph.getNode(nodePosition);
        }else{//create new node
            //this is a new user logged in, so we need to clear previous graph and rebuild
            graph.clear();
            viz_node = graph.addNode();
        }

        viz_node.setString("description",user.getDescription());
        viz_node.setLong("id", user.getId());
        viz_node.setString("location", user.getLocation());
        viz_node.setString("name", user.getName());
        viz_node.setString("profileImageUrl", user.getProfileImageUrl().toString());
        viz_node.setBoolean("protectedUser", user.isProtectedUser());
        viz_node.setString("screenName", user.getScreenName());
        
        if(user.getStatus()!=null) viz_node.setString("status", user.getStatus().getText());
        else viz_node.setString("status", "");

        if(user.getWebsite()!=null) viz_node.setString("website", user.getWebsite().toString());
        else viz_node.setString("website", "");

        //we need to build the graph from scratch
        if(nodePosition==-1) {
            List<Twitter.User> following = link.getFriends();
            for(int j=0; j<following.size(); j++) {
                Twitter.User tmp = following.get(j);

                //there are users that like privacy, so we bypass them...
                if(!tmp.isProtectedUser()) {

                    Node follower = null;
                    for(int k=0;k<graph.getNodeCount();k++) {
                        if(graph.getNode(k).getLong("id")==tmp.getId()) {
                            follower = graph.getNode(k);
                            break;
                        }
                    }

                    if(follower == null) {
                        follower = graph.addNode();
                        //connect friend with user
                        graph.addEdge(viz_node, follower);
                    }

                    follower.setString("description",tmp.getDescription());
                    follower.setLong("id", tmp.getId());
                    follower.setString("location", tmp.getLocation());
                    follower.setString("name", tmp.getName());
                    follower.setString("profileImageUrl", tmp.getProfileImageUrl().toString());
                    follower.setBoolean("protectedUser", tmp.isProtectedUser());
                    follower.setString("screenName", tmp.getScreenName());

                    if(tmp.getStatus()!=null) {
                        follower.setString("status", tmp.getStatus().getText());
                    }else follower.setString("status", "");

                    if(tmp.getWebsite()!=null) {
                        follower.setString("website", tmp.getWebsite().toString());
                    }else follower.setString("website", "");

                    getFriendsOfFriends(link, tmp.getId(), follower);
                }
            }
        }

        try{
            new GraphMLWriter().writeGraph(graph, new File("twitviz.xml"));
        }catch(DataIOException e){
            e.printStackTrace();
        }

        /* load the data from an XML file */
        vis = new Visualization();
        /* vis is the main object that will run the visualization */
        vis.add("social_network", graph);

        //Profile pictures
        LabelRenderer imgLabel = new LabelRenderer("name","profileImageUrl");
        imgLabel.setHorizontalAlignment(Constants.BOTTOM);
        imgLabel.setVerticalAlignment(Constants.BOTTOM);
        imgLabel.setMaxImageDimensions(48, 48);
        
        DefaultRendererFactory drf = new DefaultRendererFactory(imgLabel);

        //Relationships
        EdgeRenderer relationship = new EdgeRenderer();
        drf.add(new InGroupPredicate("social_network.edges"),relationship);
        
        vis.setRendererFactory(drf);

        ActionList layout = new ActionList(Activity.INFINITY);
        layout.add(new ForceDirectedLayout("social_network"));
        layout.add(new RepaintAction());

        vis.putAction("layout", layout);

        ItemAction edgeColor = new ColorAction("social_network.edges",
                VisualItem.STROKECOLOR, ColorLib.rgb(200,200,200));

        ActionList color = new ActionList();
        color.add(edgeColor);
        vis.putAction("color", color);

        Display display = new Display(vis);
        display.setSize(1024, 683); //this is the size of the background image
        display.pan(400, 300);	// pan to the middle
        display.addControlListener(new DragControl());
        display.addControlListener(new PanControl());
        display.addControlListener(new ZoomControl());
        display.addControlListener(new WheelZoomControl());
        display.addControlListener(new ZoomToFitControl());
        display.addControlListener(new NeighborHighlightControl());
        
        ToolTipControl labels = new ToolTipControl("name");

        Control hover = new ControlAdapter(){
            public void itemEntered(VisualItem item, MouseEvent evt) {
                //item.setFillColor(ColorLib.color(Color.BLACK));

                item.getVisualization().repaint();
            }

            public void itemExited(VisualItem item, MouseEvent evt) {
                item.setFillColor(Color.TRANSLUCENT);
                item.getVisualization().repaint();
            }
        };

        display.addControlListener(labels);
        display.addControlListener(hover);

        panel_viz.add(display);
        /* add the display (which holds the visualization) to the window */

        panel_viz.validate();
        panel_viz.setVisible(true);

        vis.run("color");
        vis.run("layout");
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
        jLabel1 = new javax.swing.JLabel();
        username = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        btn_login = new javax.swing.JButton();
        password = new javax.swing.JPasswordField();
        panel_viz = new javax.swing.JPanel();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();

        mainPanel.setName("mainPanel"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(twitviz.TwitVizApp.class).getContext().getResourceMap(TwitVizView.class);
        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        username.setText(resourceMap.getString("username.text")); // NOI18N
        username.setToolTipText(resourceMap.getString("username.toolTipText")); // NOI18N
        username.setName("username"); // NOI18N

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        btn_login.setText(resourceMap.getString("btn_login.text")); // NOI18N
        btn_login.setName("btn_login"); // NOI18N
        btn_login.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_loginActionPerformed(evt);
            }
        });

        password.setText(resourceMap.getString("password.text")); // NOI18N
        password.setName("password"); // NOI18N

        panel_viz.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));
        panel_viz.setName("panel_viz"); // NOI18N

        org.jdesktop.layout.GroupLayout panel_vizLayout = new org.jdesktop.layout.GroupLayout(panel_viz);
        panel_viz.setLayout(panel_vizLayout);
        panel_vizLayout.setHorizontalGroup(
            panel_vizLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 601, Short.MAX_VALUE)
        );
        panel_vizLayout.setVerticalGroup(
            panel_vizLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 362, Short.MAX_VALUE)
        );

        org.jdesktop.layout.GroupLayout mainPanelLayout = new org.jdesktop.layout.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(mainPanelLayout.createSequentialGroup()
                .add(9, 9, 9)
                .add(mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(panel_viz, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(mainPanelLayout.createSequentialGroup()
                        .add(jLabel1)
                        .add(1, 1, 1)
                        .add(username, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 90, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel2)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(password, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 94, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(btn_login)))
                .addContainerGap(94, Short.MAX_VALUE))
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(mainPanelLayout.createSequentialGroup()
                .add(mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(username, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel1)
                    .add(btn_login)
                    .add(jLabel2)
                    .add(password, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(panel_viz, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
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

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        org.jdesktop.layout.GroupLayout statusPanelLayout = new org.jdesktop.layout.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 706, Short.MAX_VALUE)
            .add(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(statusMessageLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 510, Short.MAX_VALUE)
                .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusPanelLayout.createSequentialGroup()
                .add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(statusMessageLabel)
                    .add(statusAnimationLabel)
                    .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(3, 3, 3))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    private void btn_loginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_loginActionPerformed
        // TODO add your handling code here:
        if(username.getText().length()>0 && password.getPassword().length>0) {

            //establish connection to twitter servers using given credentials
            Twitter link = new Twitter(username.getText(),String.valueOf(password.getPassword()));
            
            displayTwitviz(link,""); //load user social network

            //Get current logged user
            //Twitter.User user = link.show(username.getText());
            

            //System.out.println(user.getProfileImageUrl());

            //Get status from current user
            //Twitter.Status stats = link.getStatus();
            //System.out.println(stats.getText());
        }
}//GEN-LAST:event_btn_loginActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_login;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JPanel panel_viz;
    private javax.swing.JPasswordField password;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JTextField username;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;
}
