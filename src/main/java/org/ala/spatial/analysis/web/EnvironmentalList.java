package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import java.util.Map.Entry;
import java.util.Vector;
import org.ala.spatial.util.LayersUtil;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.event.ListDataEvent;

/**
 *
 * @author ajay
 */
public class EnvironmentalList extends Listbox {
    ListEntry[] listEntries;
    float[][] distances;
    String[] layerNames;
    float [] threasholds = {0.2f,0.4f,0.6f};
    SimpleListModel listModel;

    public EnvironmentalList() {
        renderAll();
    }

    public void init(MapComposer mc, String sat_url){
        try{
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(sat_url + "/alaspatial/layers/analysis/inter_layer_association.csv");

            System.out.println(sbProcessUrl.toString());
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());

            get.addRequestHeader("Accept", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            String [] rows = slist.split("\n");

            //got a csv, put into column names, etc
            layerNames = new String[rows.length]; //last row is empty
            distances = new float[rows.length-1][rows.length-1];

            String [] line = rows[0].split(",");
            layerNames[0] = line[1];
            for(int i=1;i<rows.length;i++){   //last row is empty
                line = rows[i].split(",");
                layerNames[i] = line[0];
                for(int j=1;j<line.length;j++){
                    try{
                        distances[i-1][j-1] = Float.parseFloat(line[j]);
                    }catch(Exception e){};                    
                }
            }

            setupEnvironmentalLayers(layerNames,distances);

        }catch(Exception e){
            e.printStackTrace();
        }
    }

     public void setupEnvironmentalLayers(String[] aslist, float[][] assocMx) {
        try {
            listEntries = new ListEntry[aslist.length];
            for(int i=0;i<aslist.length;i++){
                listEntries[i] = new ListEntry(aslist[i],1,i);
            }

            if (aslist.length > 0) {

                setItemRenderer(new ListitemRenderer() {
                    @Override
                    public void render(Listitem li, Object data) {
                        li.setWidth(null);                        
                        new Listcell(((ListEntry) data).name).setParent(li);
                        float value = ((ListEntry) data).value;
                        Listcell lc = new Listcell(" ");
                        if(threasholds[0] > value){
                            lc.setStyle("background: #bb2222;");
                        }else if(threasholds[1] > value){
                            lc.setStyle("background: #ffff22;");
                        }else{
                            lc.setStyle("background: #22aa22;");
                        }
                        lc.setParent(li);
                    }
                });

                listModel = new SimpleListModel(listEntries);
                setModel(listModel);
                
                renderAll();
            }
        } catch (Exception e) {
            System.out.println("error setting up env list");
            e.printStackTrace(System.out);
        }
    }
     

    @Override
    public void toggleItemSelection(Listitem item){
        super.toggleItemSelection(item);
        //update minimum distances here
    }

    public void updateDistances(){
        for(ListEntry le : listEntries){
            le.value = 1;
        }

        for(Object o : getSelectedItems()){
            //int row = ((ListEntry)((Listitem) o).getValue()).row;
            int row = ((Listitem)o).getIndex();
            
            for(ListEntry le : listEntries){
                float d = getDistance(le.row, row);
                le.value = Math.min(le.value,d);
            }
        }

        for(int i=0;i<listEntries.length;i++){
            float value = listEntries[i].value;
            Listcell lc = (Listcell)(getItemAtIndex(i).getLastChild());
            if(threasholds[0] > value){
                lc.setStyle("background: #bb2222;");
            }else if(threasholds[1] > value){
                lc.setStyle("background: #ffff22;");
            }else{
                lc.setStyle("background: #22aa22;");
            }
        }
    }

    public void onClick(Event event){
        int i = 4;
    }

    public void onSelect(Event event){
        int i = 4;
        updateDistances();
    }
    
    @Override
    public void selectItem(Listitem item) {
        super.selectItem(item);
    }


    private float getDistance(int row, int row0) {
        //diagonal
        if(row == row0) return 0;

        //lower right matrix only
        int minrow, maxrow;
        if(row < row0){
            minrow = row;
            maxrow = row0;
        }else{
            minrow = row0;
            maxrow = row;
        }

        //rows are 1-n, columns are 0-(n-1)
        if(maxrow-1 < distances.length && minrow < distances[maxrow-1].length){
            return distances[maxrow-1][minrow];
        }
        return 1;
    }
}

class ListEntry{
    public String name;
    public float value;
    int row;
    public ListEntry(String name_, float value_, int row_){
        name = name_;
        value = value_;
        row = row_;
    }
}