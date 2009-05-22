/*
 * TwitVizView.java
 */

package twitviz;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Label;
import java.awt.PopupMenu;
import java.awt.Toolkit;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.DocumentEvent;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import javax.swing.DefaultListModel;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.event.DocumentListener;
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
import prefuse.util.ColorLib;
import prefuse.visual.VisualItem;
import twitter4j.*;
//import winterwell.jtwitter.Twitter;//JavaDoc for Twitter Java API: http://www.winterwell.com/software/jtwitter/javadoc/
//import winterwell.jtwitter.Twitter.User;

/**
 * The application's main frame.
 */
public class TwitVizView extends FrameView {

    public TwitVizView(SingleFrameApplication app) {
        super(app);


        initComponents();

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        this.getFrame().setBounds((screen.width-400)/2, (screen.height-200)/2, 400, 200);
        this.getFrame().setResizable(true);
        this.getFrame().setVisible(true);

        twittsList.setLayoutOrientation(JList.HORIZONTAL_WRAP);


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

    //TODO: Visualization of keywords and strangers!
    private void displayKeyviz() {
        //make tab visible, if it already isn't
        tabs_control.setSelectedIndex(1);

        //Read the database
        try {
            kwgraph = new GraphMLReader().readGraph("kwviz.xml");
        } catch (DataIOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        if(kwvis!=null) {
            kwvis.removeGroup("graph");
            kwvis.add("graph",kwgraph);
            kwvis.repaint();
            kwvis.run("colour");
            kwvis.run("layout");
            kwvis.run("size");
        }else{
            kwvis = new Visualization();
            kwvis.add("graph", kwgraph);
        }

        // draw the "keyword" label for NodeItems
        LabelRenderer r = new LabelRenderer("keyword");
        r.setRoundedCorner(8, 8); // round the corners

        LabelRenderer names = new LabelRenderer("screenName");
        names.setRoundedCorner(8,8);

        DefaultRendererFactory drf = new DefaultRendererFactory(r,names);

        kwvis.setRendererFactory(drf);

        int[] edgesColor = new int[] {
            ColorLib.rgb(255,255,153),
            ColorLib.rgb(153,255,153)
        };

        //Lets colorize! :D
        DataColorAction nodes = new DataColorAction("graph.nodes", "friend",
            Constants.NOMINAL, VisualItem.FILLCOLOR, edgesColor);

        ColorAction text = new ColorAction("graph.nodes",VisualItem.TEXTCOLOR, ColorLib.gray(0));

        ColorAction edges = new ColorAction("graph.edges", VisualItem.STROKECOLOR, ColorLib.rgb(0,0,0));


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

        kwvis.putAction("color", color);
        kwvis.putAction("size", size);
        kwvis.putAction("layout", layout);

        Display display = new Display(kwvis);
        display.setSize(1024, 683); //this is the size of the background image
        display.pan(400, 300);	// pan to the middle
        display.addControlListener(new DragControl());
        display.addControlListener(new PanControl());
        display.addControlListener(new ZoomControl());
        display.addControlListener(new WheelZoomControl());
        display.addControlListener(new ZoomToFitControl());
        display.addControlListener(new NeighborHighlightControl());

        // add the display (which holds the visualization) to the window
        keyword_viz.add(display);
        keyword_viz.validate();
        keyword_viz.setVisible(true);

        kwvis.run("color");  // assign the colors
        kwvis.run("size"); //assign the sizes
        kwvis.run("layout"); // start up the animated layout
    }

    //Function that gets a specific node by keyword.
    //--If it doesn't exist, creates one automatically and returns it
    private Node getKeywordFromGraph(String name) {
        
        try {
            kwgraph = new GraphMLReader().readGraph("kwviz.xml");
        } catch (DataIOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        Node key = null;
        for(int i=0;i<kwgraph.getNodeCount();i++) {
            key = kwgraph.getNode(i);
            //screenName == null when its a keyword node
            if(key.getString("keyword").compareToIgnoreCase(name)==0 && key.getString("screenName").compareTo("null")==0) {
                return key;
            }
        }

        key = kwgraph.addNode();
        key.setString("keyword", name);
        
        //Save to graph file
        try{
            new GraphMLWriter().writeGraph(kwgraph, new File("kwviz.xml"));
        }catch(DataIOException e){
            e.printStackTrace();
        }
        //--end save graph file
        return key;
    }

    //Function that checks if someone is following you back
    private boolean isFollowing(List<User> list, User who) {
        for(int i=0;i<list.size();i++) {
            User tmp = list.get(i);

            if(!tmp.isProtected()) {
                if(tmp.getId()==who.getId()) {
                    return true;
                }
            }
        }
        return false;
    }

    //Function that builds the logged in user's network
    private void buildSocialNetwork(User user) {

        //restore saved database
        try {
            graph = new GraphMLReader().readGraph("twitviz.xml");
        } catch (DataIOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        
        Node source = null;
        if(graph.getNodeCount()>0) {
            source = graph.getNode(0); //get the first node = user
            if(user.getId()!=source.getLong("id")) {
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

        for(int i=0;i<friends.size();i++) {
            User friend = friends.get(i);

            //There are users with protected profiles and data
            if(!friend.isProtected()) {

                Node tmp = null;
                int friendPos = -1;
                for(int a=0;a<graph.getNodeCount();a++) {
                    tmp = graph.getNode(a);
                    if(tmp.getLong("id")==friend.getId()) {
                       friendPos = a;
                       break;
                    }
                }

                //New friend
                if(friendPos==-1) {
                    tmp = graph.addNode();
                //restore saved
                }else{
                    tmp = graph.getNode(friendPos);
                }

                //set node data
                tmp.setLong("id", friend.getId());
                tmp.setString("screenName", friend.getScreenName());
                tmp.setBoolean("protectedUser", friend.isProtected());

                //The relevance might already be a different number!
                if(friendPos==-1) {
                    tmp.setInt("relevance", 1);
                }else{
                    tmp.setInt("relevance", tmp.getInt("relevance"));
                }
                //--end node data

                //Link to source
                Edge relation = graph.getEdge(source,tmp);
                if(relation==null) {
                    graph.addEdge(source, tmp);
                    relation = graph.getEdge(source,tmp);
                }
                
                if(isFollowing(followers, friend)) {
                    tmp.setBoolean("follows", true);
                    relation.setInt("relationship", 2);
                }else{
                    tmp.setBoolean("follows", false);
                    relation.setInt("relationship", 1);
                }

                if(depth>0) {
                    try{
                        getSubfriends(link.getFriends(Integer.toString(friend.getId())),tmp);
                    }catch(Exception e) {}
                }
            }
        }
         
        //Save to graph file
        try{
            new GraphMLWriter().writeGraph(graph, new File("twitviz.xml"));
        }catch(DataIOException e){
            e.printStackTrace();
        }
        //--end save graph file
    }

    //Recursive function that will get friends of a friend
    private void getSubfriends(List<User> friends, Node source) {
        depth--;
        if(depth>0) {
            
            for(User friend: friends) {
                //There are users with protected profiles and data
                List<User> followers = null;
                try {
                    followers = link.getFollowers(Integer.toString(friend.getId()));
                } catch (TwitterException ex) {
                    Logger.getLogger(TwitVizView.class.getName()).log(Level.SEVERE, null, ex);
                }

                if(!friend.isProtected()) {
                    Node tmp = null;
                    int friendPos = -1;
                    for(int a=0;a<graph.getNodeCount();a++) {
                        tmp = graph.getNode(a);
                        if(tmp.getLong("id")==friend.getId()) {
                           friendPos = a;
                           break;
                        }
                    }

                    //New friend
                    if(friendPos==-1) {
                        tmp = graph.addNode();
                    //restore saved
                    }else{
                        tmp = graph.getNode(friendPos);
                    }

                    //set node data
                    tmp.setLong("id", friend.getId());
                    tmp.setString("screenName", friend.getScreenName());
                    tmp.setBoolean("protectedUser", friend.isProtected());

                    //The relevance might already be a different number!
                    if(friendPos==-1) {
                        tmp.setInt("relevance", 1);
                    }else{
                        tmp.setInt("relevance", tmp.getInt("relevance"));
                    }
                    //--end node data

                    //Link to source
                    Edge relation = graph.getEdge(source,tmp);
                    if(relation==null) {
                        graph.addEdge(source, tmp);
                        relation = graph.getEdge(source,tmp);
                    }

                    if(isFollowing(followers, friend)) {
                        tmp.setBoolean("follows", true);
                        relation.setInt("relationship", 2);
                    }else{
                        tmp.setBoolean("follows", false);
                        relation.setInt("relationship", 1);
                    }

                    try{
                        getSubfriends(link.getFriends(Integer.toString(friend.getId())),tmp);
                    }catch(Exception e) {}
                }
            }
        }
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
        searchButton = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        countLabel = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        updateTextField = new javax.swing.JTextField();
        updateButton = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        twittsList = new javax.swing.JList();
        jLabel9 = new javax.swing.JLabel();
        lbl_username = new javax.swing.JLabel();
        username = new javax.swing.JTextField();
        lbl_password = new javax.swing.JLabel();
        password = new javax.swing.JPasswordField();
        btn_login = new javax.swing.JButton();
        feedback_label = new javax.swing.JLabel();
        tabs_control = new javax.swing.JTabbedPane();
        panel_viz = new javax.swing.JPanel();
        keyword_viz = new javax.swing.JPanel();
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

        searchButton.setText(resourceMap.getString("searchButton.text")); // NOI18N
        searchButton.setName("searchButton"); // NOI18N
        searchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel4)
                    .add(keywordsTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 166, Short.MAX_VALUE)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 166, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2Layout.createSequentialGroup()
                        .add(searchButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(addButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 95, Short.MAX_VALUE)))
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
                    .add(searchButton)
                    .add(addButton))
                .add(5, 5, 5)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 400, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(187, Short.MAX_VALUE))
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

        updateTextField.setName("updateTextField"); // NOI18N
        updateTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                updateTextFieldKeyReleased(evt);
            }
        });

        updateButton.setText(resourceMap.getString("updateButton.text")); // NOI18N
        updateButton.setEnabled(false);
        updateButton.setName("updateButton"); // NOI18N
        updateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateButtonActionPerformed(evt);
            }
        });

        jLabel7.setFont(resourceMap.getFont("jLabel7.font")); // NOI18N
        jLabel7.setForeground(resourceMap.getColor("jLabel7.foreground")); // NOI18N
        jLabel7.setText(resourceMap.getString("jLabel7.text")); // NOI18N
        jLabel7.setName("jLabel7"); // NOI18N

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        twittsList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        twittsList.setName("twittsList"); // NOI18N
        jScrollPane2.setViewportView(twittsList);

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .add(9, 9, 9)
                .add(jLabel6)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 202, Short.MAX_VALUE)
                .add(countLabel)
                .add(8, 8, 8))
            .add(updateTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 379, Short.MAX_VALUE)
            .add(jPanel3Layout.createSequentialGroup()
                .add(12, 12, 12)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel3Layout.createSequentialGroup()
                        .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE)
                        .add(20, 20, 20))
                    .add(jPanel3Layout.createSequentialGroup()
                        .add(jLabel7)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 255, Short.MAX_VALUE)
                        .add(updateButton))))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(countLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(updateTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(updateButton)
                    .add(jLabel7))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 509, Short.MAX_VALUE)
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

        tabs_control.setName("tabs_control"); // NOI18N
        tabs_control.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabs_controlStateChanged(evt);
            }
        });

        panel_viz.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));
        panel_viz.setName("panel_viz"); // NOI18N

        org.jdesktop.layout.GroupLayout panel_vizLayout = new org.jdesktop.layout.GroupLayout(panel_viz);
        panel_viz.setLayout(panel_vizLayout);
        panel_vizLayout.setHorizontalGroup(
            panel_vizLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 578, Short.MAX_VALUE)
        );
        panel_vizLayout.setVerticalGroup(
            panel_vizLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 645, Short.MAX_VALUE)
        );

        tabs_control.addTab(resourceMap.getString("panel_viz.TabConstraints.tabTitle"), null, panel_viz, resourceMap.getString("panel_viz.TabConstraints.tabToolTip")); // NOI18N

        keyword_viz.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        keyword_viz.setName("keyword_viz"); // NOI18N

        org.jdesktop.layout.GroupLayout keyword_vizLayout = new org.jdesktop.layout.GroupLayout(keyword_viz);
        keyword_viz.setLayout(keyword_vizLayout);
        keyword_vizLayout.setHorizontalGroup(
            keyword_vizLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 578, Short.MAX_VALUE)
        );
        keyword_vizLayout.setVerticalGroup(
            keyword_vizLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 645, Short.MAX_VALUE)
        );

        tabs_control.addTab(resourceMap.getString("keyword_viz.TabConstraints.tabTitle"), null, keyword_viz, resourceMap.getString("keyword_viz.TabConstraints.tabToolTip")); // NOI18N

        org.jdesktop.layout.GroupLayout twitvizPanelLayout = new org.jdesktop.layout.GroupLayout(twitvizPanel);
        twitvizPanel.setLayout(twitvizPanelLayout);
        twitvizPanelLayout.setHorizontalGroup(
            twitvizPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(twitvizPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(twitvizPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel9, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 121, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(14, 14, 14)
                .add(twitvizPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(twitvizPanelLayout.createSequentialGroup()
                        .add(tabs_control, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 585, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(18, 18, 18)
                        .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(4, 4, 4))
                    .add(twitvizPanelLayout.createSequentialGroup()
                        .add(feedback_label, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 452, Short.MAX_VALUE)
                        .add(189, 189, 189)
                        .add(lbl_username)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(username, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 90, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(lbl_password)
                        .add(2, 2, 2)
                        .add(password, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 94, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(9, 9, 9)
                        .add(btn_login)))
                .addContainerGap(411, Short.MAX_VALUE))
        );
        twitvizPanelLayout.setVerticalGroup(
            twitvizPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(twitvizPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(twitvizPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel9, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 38, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(password, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(username, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(lbl_password)
                    .add(lbl_username)
                    .add(btn_login)
                    .add(feedback_label, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 37, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(twitvizPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(twitvizPanelLayout.createSequentialGroup()
                        .add(9, 9, 9)
                        .add(twitvizPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(tabs_control, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 675, Short.MAX_VALUE)
                            .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .add(twitvizPanelLayout.createSequentialGroup()
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
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

        statusPanel.setBackground(resourceMap.getColor("statusPanel.background")); // NOI18N
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
            .add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1622, Short.MAX_VALUE)
            .add(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(statusMessageLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 1452, Short.MAX_VALUE)
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
                .add(16, 16, 16))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    private Node getUserFromKeyGraph(User tweeterer) {
        int nodePosition = -1;
        for(int j=0;j<kwgraph.getNodeCount();j++) {
            Node tmp = kwgraph.getNode(j);

            if(tmp.getInt("id")==tweeterer.getId()) {
                nodePosition = j;
                break;
            }
        }

        Node user = null;

        if(nodePosition>=0) {
            user = kwgraph.getNode(nodePosition);
        }else {
            user = kwgraph.addNode();
        }

        return user;
    }

    private void btn_loginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_loginActionPerformed
        //trying to logout
        if(link!=null) {
            //restore GUI
            lbl_username.setVisible(true);
            username.setText("");
            username.setVisible(true);

            lbl_password.setVisible(true);
            password.setText("");
            password.setVisible(true);

            btn_login.setText("Login");

            link = null;
            user = null;
            panel_viz.removeAll();
            panel_viz.validate();
            panel_viz.repaint();
            updateButton.setEnabled(false);
            
        //user logging in for the first time
        }else{
            if(username.getText().length()>0 && password.getPassword().length>0) {
                //establish connection to twitter servers using given credentials
                link = new Twitter(username.getText(),String.valueOf(password.getPassword()));

                //Lets make publicity :D
                link.setUserAgent("TwitViz");
                link.setClientVersion("TwitViz");
                link.setSource("TwitViz");

                if(link!=null) {
                    try {
                        user = link.getUserDetail(username.getText());

                        //Organize GUI
                        lbl_username.setVisible(false);
                        username.setVisible(false);
                        lbl_password.setVisible(false);
                        password.setVisible(false);
                        btn_login.setText("Logout");

                        updateButton.setEnabled(true);

                        //buildSocialNetwork(user);

                        displayTwitviz();

                        //TODO: load previous stored keywords
                        loadKeywords();

                        //Start public line monitor, updates every 20 seconds
                        java.util.Timer timer = new java.util.Timer();
                        timer.scheduleAtFixedRate(new java.util.TimerTask() {
                            public void run() {
                                get_PublicLine();
                            }
                        }, 5000, 20000); //Get public line every 20 

                    } catch (TwitterException ex) {
                        setFeedback("Error getting user information, please try again...", Color.RED);
                    }

                }
                try {

                    List<Status> statusList = link.getFriendsTimeline();
                    twittsList.setVisibleRowCount(statusList.size());
                    Vector faces = new Vector();
                    Vector twitts = new Vector();

                    for(int i=0;i<statusList.size();i++){
                        Status status =statusList.get(i);

                        String screenName = status.getUser().getScreenName();
                        String text = status.getText();
                        String dt = status.getCreatedAt().toString();
                        String twitt = screenName + " " + text + dt;
                        //System.out.println(twitt);

                        ImageIcon icon = new ImageIcon(status.getUser().getProfileImageURL());
                        JLabel label = new JLabel(icon);

                        faces.addElement(icon);
                        twitts.addElement(twitt);
                    }
                    faces.add(twitts);
                    twittsList.setListData(faces);
                    
                } catch (TwitterException ex) {
                    //setFeedback("Error loading friends timeline", Color.RED);
                }

            }

        }
}//GEN-LAST:event_btn_loginActionPerformed

    private boolean isStrangerAFriend(User stranger) {
        boolean friend = false;

        //restore saved database
        try {
            graph = new GraphMLReader().readGraph("twitviz.xml");
        } catch (DataIOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        for(int i=0; i<graph.getNodeCount(); i++) {
            Node tmp = graph.getNode(i);
            if(tmp.getInt("id")==stranger.getId()) {
                friend = true;
                break;
            }
        }

        return friend;
    }

    private void get_PublicLine() {
        //we only start processing the public line if we have keywords on the list!
        if(keywordsmap.getSize()>0) {

            try {
                List<Status> publicStatus = link.getPublicTimeline();

                for(int i=0; i<publicStatus.size();i++) {

                    Status stat = publicStatus.get(i);

                    //If the user has something on the text, etc that we are interested in...
                    Vector interests = getInterests(stat);
                    
                    if(interests.size()>0) {

                        User tweeterer = stat.getUser();
                        if(!tweeterer.isProtected()) {

                            //check if we already have the user or not    
                            Node familiar_stranger = getUserFromKeyGraph(tweeterer);

                            if(familiar_stranger.getString("screenName").compareTo("null")==0) {
                                familiar_stranger.setLong("id", tweeterer.getId());
                                familiar_stranger.setString("screenName", tweeterer.getScreenName());
                                familiar_stranger.setBoolean("protectedUser", tweeterer.isProtected());
                                familiar_stranger.setInt("relevance", 1);
                            }else{
                                //Relevance will change according to how important the user is
                                familiar_stranger.setInt("relevance", familiar_stranger.getInt("relevance")+interests.size());
                            }

                            //Check if the familiar stranger is in reality my friend!
                            familiar_stranger.setBoolean("friend", isStrangerAFriend(tweeterer));

                            //lets check if the stranger is somehow related to someone already on our list
                            List<Node> related = getRelatedToSomeone(familiar_stranger);
                            if(related!=null) {
                                for(int k=0;k<related.size();k++) {
                                    kwgraph.addEdge(related.get(i), familiar_stranger);
                                }
                            }

                            //Associate the person to the keywords we are following
                            for(int k=0;k<keywordsmap.getSize();k++) {
                                Node keyword = getKeywordFromGraph((String)keywordsmap.get(k));
                                kwgraph.addEdge(keyword, familiar_stranger);
                            }
                        }
                    }
                }

                //Save to graph file
                try{
                    new GraphMLWriter().writeGraph(kwgraph, new File("kwviz.xml"));
                }catch(DataIOException e){
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

    //Get list of related nodes
    private List<Node> getRelatedToSomeone(Node who) {
        List<Node> tmp = null;
        
        for(int i=0;i<kwgraph.getEdgeCount();i++) {
            if(kwgraph.getEdge(i).getTargetNode()==who) {
                tmp.add(kwgraph.getEdge(i).getSourceNode());
            }
        }
        return tmp;
    }

    private Vector getInterests(Status stat) {
        Vector keywords = new Vector();

        for(int i=0;i<keywordsmap.getSize();i++) {
            if(stat.getText().matches(".*"+keywordsmap.get(i).toString()+".*")) {
                keywords.addElement(keywordsmap.get(i).toString());
            }
        }
        return keywords;
    }

    //Load previous selected keywords
    private void loadKeywords() {
        try {
            kwgraph = new GraphMLReader().readGraph("kwviz.xml");
        } catch (DataIOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        Node key = null;
        for(int i=0;i<kwgraph.getNodeCount();i++) {
            key = kwgraph.getNode(i);
            //screenName == null when its a keyword node
            if(key.getString("keyword").compareTo("null")!=0 && key.getString("screenName").compareTo("null")==0) {
                keywordsmap.addElement(key.getString("keyword"));
            }
        }

        keyword_list.setModel(keywordsmap);
    }

    //Update the feedback_label to specific message + color
    private void setFeedback(String message, Color color) {
        feedback_label.setText(message);
        feedback_label.setForeground(color);
        //force update on GUI
        feedback_label.validate(); 
        feedback_label.repaint();
    }

    private void updateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateButtonActionPerformed
        try {
            link.updateStatus(updateTextField.getText());
            updateTextField.setText("");
            setFeedback("Updated status successful", Color.WHITE);
        } catch (TwitterException ex) {
            setFeedback("Error updating status", Color.RED);
        }
}//GEN-LAST:event_updateButtonActionPerformed

    private void keywordsTextFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_keywordsTextFieldFocusGained
        // TODO add your handling code here:
        keywordsTextField.setText("");
        keywordsTextField.setForeground(Color.BLACK);
    }//GEN-LAST:event_keywordsTextFieldFocusGained

    private void keywordsTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_keywordsTextFieldFocusLost
        // TODO add your handling code here:
        if(keywordsTextField.getText().length()==0){
            keywordsTextField.setText("Keywords");
            keywordsTextField.setForeground(Color.GRAY);
        }
        
    }//GEN-LAST:event_keywordsTextFieldFocusLost

    private void updateTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_updateTextFieldKeyReleased
        // TODO add your handling code here:
        int cl = 140 -(updateTextField.getText().length());
        if(cl<0){
            countLabel.setForeground(Color.RED);
        }else{
            countLabel.setForeground(Color.GRAY);
        }
        countLabel.setText(Integer.toString(cl));
    }//GEN-LAST:event_updateTextFieldKeyReleased

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        // TODO add your handling code here:
        if(keywordsTextField.getText().length()>0) {
            
            boolean alreadyDefined = false;
            for(int i=0;i<keywordsmap.size();i++) {
                if(((String)keywordsmap.get(i)).compareTo(keywordsTextField.getText())==0) {
                    alreadyDefined=true;
                    break;
                }
            }
            if(! alreadyDefined) {
                keywordsmap.addElement(keywordsTextField.getText());
                keyword_list.setModel(keywordsmap);
            }

            Node key = getKeywordFromGraph(keywordsTextField.getText());
            if(! alreadyDefined) {
                setFeedback("Keyword added successfully", Color.WHITE);
            }

            //Change to the keyword visualization
            tabs_control.setSelectedIndex(1);
            displayKeyviz();
        }
    }//GEN-LAST:event_addButtonActionPerformed

    //Function that refreshes the view according to the tab clicked
    private void tabs_controlStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabs_controlStateChanged
        

    }//GEN-LAST:event_tabs_controlStateChanged

    //TODO: Refine displaying of search results (twitts) by including more parameters like user, time, source
    private void searchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchButtonActionPerformed

        if(keywordsTextField.getText().length() > 0){
            QueryResult results = null;
            List<Tweet> twitts = null;
            Query queryString = new Query(keywordsTextField.getText());
            try {
                results = link.search(queryString);
                twitts = results.getTweets();
            } catch (TwitterException ex) {
                setFeedback("Error while searching:" + ex.getMessage(), Color.RED);
            }
            for(Tweet twitt : twitts)
            {
                keywordsmap.addElement(twitt.getText());
                keyword_list.setModel(keywordsmap);
            }
        }
    }//GEN-LAST:event_searchButtonActionPerformed

    public void displayTwitviz() {
        //make tab visible, if it already isn't
        tabs_control.setSelectedIndex(0);

        //Read the database
        try {
            graph = new GraphMLReader().readGraph("twitviz.xml");
        } catch (DataIOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        if(vis!=null) {
            vis.removeGroup("graph");
            vis.add("graph",graph);
            vis.repaint();
            vis.run("colour");
            vis.run("layout");
            vis.run("size");
        }else{
            vis = new Visualization();
            vis.add("graph", graph);
        }

        // draw the "name" label for NodeItems
        LabelRenderer r = new LabelRenderer("screenName");
        r.setRoundedCorner(8, 8); // round the corners

        vis.setRendererFactory(new DefaultRendererFactory(r));

        int[] edgesColor = new int[] {
            ColorLib.rgb(255,255,153),
            ColorLib.rgb(153,255,153)
        };

        //Lets colorize! :D
        DataColorAction nodes = new DataColorAction("graph.nodes", "follows",
            Constants.NOMINAL, VisualItem.FILLCOLOR, edgesColor);

        ColorAction text = new ColorAction("graph.nodes",VisualItem.TEXTCOLOR, ColorLib.gray(0));
        
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
        display.setSize(1024, 683); //this is the size of the background image
        display.pan(400, 300);	// pan to the middle
        display.addControlListener(new DragControl());
        display.addControlListener(new PanControl());
        display.addControlListener(new ZoomControl());
        display.addControlListener(new WheelZoomControl());
        display.addControlListener(new ZoomToFitControl());
        display.addControlListener(new NeighborHighlightControl());

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
    private javax.swing.JButton btn_login;
    private javax.swing.JLabel countLabel;
    private javax.swing.JLabel feedback_label;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JList keyword_list;
    private javax.swing.JPanel keyword_viz;
    private javax.swing.JTextField keywordsTextField;
    private javax.swing.JLabel lbl_password;
    private javax.swing.JLabel lbl_username;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JPanel panel_viz;
    private javax.swing.JPasswordField password;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JButton searchButton;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JTabbedPane tabs_control;
    private javax.swing.JList twittsList;
    private javax.swing.JPanel twitvizPanel;
    private javax.swing.JButton updateButton;
    private javax.swing.JTextField updateTextField;
    private javax.swing.JTextField username;
    // End of variables declaration//GEN-END:variables

    //Twitter API vars
    private Twitter link = null;
    private User user = null;

    //Prefuse vars
    private Graph graph;
    private Graph kwgraph;
    private Visualization vis = null;
    private Visualization kwvis = null;
    private GraphMLWriter graphWriter;
    private GraphMLReader graphReader;

    //Keywords var
    private DefaultListModel keywordsmap = new DefaultListModel();

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    //Twitviz parameters
    private int depth = 1;

    private JDialog aboutBox;
}
