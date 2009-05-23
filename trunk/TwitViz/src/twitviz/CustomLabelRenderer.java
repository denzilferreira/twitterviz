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
        String safety = null;

        if (item instanceof NodeItem && item!=null) {
            try{
                if(item.canGetString("screenName") && item.getString("screenName").compareTo("null")!=0) {
                    safety = item.getString("screenName");
                }
            }catch(Exception e) {}

            try{
                if(item.canGetString("keyword") && item.getString("keyword").compareTo("null")!=0) {
                    safety = item.getString("keyword");
                }
            }catch(Exception e) {}
        }
        return safety;
    }

}
