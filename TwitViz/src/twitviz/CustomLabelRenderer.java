/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package twitviz;

import prefuse.render.LabelRenderer;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;

/**
 *
 * @author denzilferreira
 */
public class CustomLabelRenderer extends LabelRenderer {

    @Override
    protected String getText(VisualItem item) {
        String safety="";

        if (item instanceof NodeItem && item!=null) {
            if(item.canGetString("screenName") && item.getString("screenName").compareTo("null")!=0) {
                safety = item.getString("screenName");
            }else if(item.canGetString("keyword") && item.getString("keyword").compareTo("null")!=0) {
                safety = item.getString("keyword");
            }
        }
        return safety;
    }

}
