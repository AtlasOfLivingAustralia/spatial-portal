package org.ala.spatial.analysis.web;

import au.com.bytecode.opencsv.CSVReader;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.util.LayerUtilities;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import net.sf.json.JSONObject;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.util.Util;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Button;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;

/**
 * 
 * @author ajay
 */
public class DistributionsWCController extends UtilityComposer {

    final static String EXPERT_DISTRIBUTION_AREA_NAME = "Expert distribution";
    Label distributionLabel;
    Listbox distributionListbox;
    EventListener el;
    String[] text;
    ArrayList<String[]> original_data;
    ArrayList<String[]> current_data;
    String type;
    String original_count;
    Doublebox minDepth;
    Doublebox maxDepth;

    public void onClick$btnApplyDepthFilter() {
        ArrayList<String[]> new_data = new ArrayList<String[]>();
        for (int i = 0; i < original_data.size(); i++) {
            try {
                double min = Double.parseDouble(original_data.get(i)[7]);
                double max = Double.parseDouble(original_data.get(i)[8]);
                if ((minDepth.getValue() == null || minDepth.getValue() <= min) && (maxDepth.getValue() == null || maxDepth.getValue() >= max)) {
                    new_data.add(original_data.get(i));
                }
            } catch (Exception e) {
            }
        }
        update(new_data, String.valueOf(new_data.size()));
    }

    public void onClick$btnClearDepthFilter() {
        minDepth.setValue(null);
        maxDepth.setValue(null);

        update(original_data, original_count);
    }

    public void onClick$btnDownload(Event event) {
        // try {
        // el.onEvent(event);
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
        try {
            String spid = String.valueOf(System.currentTimeMillis());

            SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
            String sdate = date.format(new Date());

            StringBuilder sb = new StringBuilder();
            for (String s : text) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(s);
            }

            // File f = new File(System.getProperty("java.io.tmpdir") +
            // File.separator + type + "_" + sdate + "_" + spid + ".csv");
            // FileOutputStream fos = new FileOutputStream(f);
            // fos.write(sb.toString().getBytes("UTF-8"));
            // Filedownload.save(f, "text/html;charset=UTF-8");

            Filedownload.save(sb.toString(), "text/plain;charset=UTF-8", type + "_" + sdate + "_" + spid + ".csv");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void init(String[] text, String type, String count, EventListener el) {
        this.setTitle(type);

        this.el = el;
        this.original_count = count;
        this.type = type;
        this.text = text;
        if (text != null && text.length > 1) {
            ArrayList<String[]> data = new ArrayList<String[]>();
            for (int i = 1; i < text.length; i++) {
                if (text[i] != null && text[i].length() > 0) {
                    try {
                        StringReader sreader = new StringReader(text[i]);
                        CSVReader reader = new CSVReader(sreader);

                        // last row indicates if it is mapped or not
                        String[] row = reader.readNext();
                        String[] newrow = java.util.Arrays.copyOf(row, 15);
                        try {
                            String niceAreaSqKm = String.format("%.1f", (float) Double.parseDouble(row[12]));
                            newrow[12] = niceAreaSqKm;
                        } catch (Exception e) {
                        }
                        data.add(newrow);

                        reader.close();
                        sreader.close();
                    } catch (Exception e) {
                    }
                }
            }

            if (this.original_data == null) {
                this.original_data = data;
            }

            update(data, original_count);
        }
    }

    void update(ArrayList<String[]> data, String count) {
        current_data = data;

        distributionLabel.setValue("found " + count + " " + type + " in the Area");
        distributionListbox.setItemRenderer(new ListitemRenderer() {

            @Override
            public void render(Listitem li, Object data) {
                try {
                    String[] cells = (String[]) data;
                    li.setValue(cells);
                    if (cells.length > 0) {
                        Listcell lc = new Listcell();
                        if (!cells[0].equals("SPCODE")) {
                            Button b = new Button("map");
                            b.setSclass("goButton");
                            if ((cells[14] != null && cells[14].length() > 0)
                                    || (CommonData.getSpeciesChecklistWMSFromSpcode(cells[0]) != null && getMapComposer().getMapLayerWMS(CommonData.getSpeciesChecklistWMSFromSpcode(cells[0])[1]) != null)
                                    || (CommonData.getSpeciesDistributionWMSFromSpcode(cells[0]) != null && getMapComposer()
                                            .getMapLayerWMS(CommonData.getSpeciesDistributionWMSFromSpcode(cells[0])[1]) != null)) {
                                b.setDisabled(true);
                            } else {
                                b.addEventListener("onClick", new EventListener() {

                                    @Override
                                    public void onEvent(Event event) throws Exception {
                                        // get spcode
                                        Listcell lc = (Listcell) event.getTarget().getParent().getNextSibling();
                                        String spcode = lc.getLabel();

                                        // row as metadata
                                        Listitem li = (Listitem) lc.getParent();
                                        String[] row = (String[]) li.getValue();
                                        String layerName = getMapComposer().getNextAreaLayerName(row[0] + " area");
                                        String html = getMetadataHtmlFor(row[0], row, layerName);

                                        // map it
                                        String[] mapping = CommonData.getSpeciesDistributionWMSFromSpcode(spcode);
                                        if (mapping == null) {
                                            mapping = CommonData.getSpeciesChecklistWMSFromSpcode(spcode);
                                        }
                                        String displayName = mapping[0] + " area";
                                        if (row[11] != null && row[11].length() > 0) {// &&
                                                                                      // !row[11].startsWith(EXPERT_DISTRIBUTION_AREA_NAME))
                                                                                      // {
                                            displayName = row[11];
                                        }
                                        MapLayer ml = getMapComposer().addWMSLayer(layerName, displayName, mapping[1], 0.6f, html, null, LayerUtilities.WKT, null, null);
                                        ml.setData("spcode", row[0]);
                                        MapComposer.setupMapLayerAsDistributionArea(ml);
                                        getMapComposer().updateLayerControls();

                                        // disable this button
                                        ((Button) event.getTarget()).setDisabled(true);

                                        // flag as mapped by area_name or spcode
                                        for (int i = 0; i < original_data.size(); i++) {
                                            if (original_data.get(i)[0].length() > 0
                                                    && (original_data.get(i)[0].equals(row[0]) || (original_data.get(i)[11] != null && original_data.get(i)[11].length() > 0
                                                            && !original_data.get(i)[11].startsWith(EXPERT_DISTRIBUTION_AREA_NAME) && original_data.get(i)[11].equals(row[11])))) {
                                                original_data.get(i)[14] = "1";
                                            }
                                        }
                                        for (int i = 0; i < current_data.size(); i++) {
                                            if (current_data.get(i)[0].length() > 0
                                                    && (current_data.get(i)[0].equals(row[0]) || (current_data.get(i)[11] != null && current_data.get(i)[11].length() > 0
                                                            && !current_data.get(i)[11].startsWith(EXPERT_DISTRIBUTION_AREA_NAME) && current_data.get(i)[11].equals(row[11])))) {
                                                current_data.get(i)[14] = "1";
                                            }
                                        }
                                        for (int i = 0; i < distributionListbox.getItemCount(); i++) {
                                            String[] data = (String[]) distributionListbox.getItemAtIndex(i).getValue();
                                            if (data != null && data[14] != null && data[14].length() > 0) {
                                                ((Button) distributionListbox.getItemAtIndex(i).getFirstChild().getFirstChild()).setDisabled(true);
                                            }
                                        }
                                    }
                                });
                            }
                            b.setParent(lc);
                        }

                        lc.setParent(li);

                        for (int i = 0; i < 14; i++) {
                            if (i == 9) { // metadata url
                                lc = new Listcell();
                                if (cells[i] != null && cells[i].length() > 0) {
                                    A a = new A("link");
                                    a.setHref(cells[i]);
                                    a.setTarget("_blank");
                                    a.setParent(lc);
                                }
                                lc.setParent(li);
                            } else if (i == 10) { // lsid
                                lc = new Listcell();
                                if (cells[i] != null && cells[i].length() > 0) {
                                    A a = new A("more...");
                                    a.setHref(CommonData.bieServer + "/species/" + cells[i]);
                                    a.setTarget("_blank");
                                    a.setParent(lc);
                                }
                                lc.setParent(li);
                            } else {
                                lc = new Listcell(cells[i]);
                                lc.setParent(li);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        ListModelList lme = new ListModelList(data);
        distributionListbox.setModel(lme);

        Listhead head = distributionListbox.getListhead();

        for (int i = 0; i < head.getChildren().size(); i++) {
            Listheader lh = (Listheader) head.getChildren().get(i);

            // -1 for first column containing buttons.
            if (i == 8 || i == 9 || i == 13) { // min depth, max depth, area_km
                lh.setSortAscending(new DListComparator(true, true, i - 1));
                lh.setSortDescending(new DListComparator(false, true, i - 1));
            } else if (i > 0 && i != 10 && i != 11) { // exclude 'map button',
                                                      // 'metadata link', 'BIE
                                                      // link'
                lh.setSortAscending(new DListComparator(true, false, i - 1));
                lh.setSortDescending(new DListComparator(false, false, i - 1));
            }
        }
    }

    public static String getMetadataHtmlFor(String spcode, String[] row, String layerName) {
        if (CommonData.getSpeciesDistributionWMSFromSpcode(spcode) != null) {
            if (row == null) {
                row = FilteringResultsWCController.getDistributionOrChecklist(spcode);
            }
            return getMetadataHtmlForExpertDistribution(row);
        } else {
            return getMetadataHtmlForAreaChecklist(spcode, layerName);
        }
    }

    public static String getMetadataHtmlForExpertDistribution(String[] row) {
        if (row == null) {
            return null;
        }

        String scientificName = row[1];
        String commonName = row[3];
        String familyName = row[4];
        String minDepth = row[7];
        String maxDepth = row[8];
        String metadataLink = row[9];
        String lsid = row[10];
        String area = row[12];
        String dataResourceUid = row[13];

        String[] distributionCollectoryDetails = getDistributionCollectoryDetails(dataResourceUid);
        String websiteUrl = distributionCollectoryDetails[0];
        String citation = distributionCollectoryDetails[1];
        String logoUrl = distributionCollectoryDetails[2];
        
        String speciesPageUrl = CommonData.bieServer + "/species/" + lsid;

        String html = "Expert Distribution\n";
        html += "<table class='md_table'>";
        html += "<tr class='md_grey-bg'><td class='md_th'>Scientific name: </td><td class='md_spacer'/><td class='md_value'><a target='_blank' href='" + speciesPageUrl + "'>" + scientificName
                    + "</a></td></tr>";
        html += "<tr><td class='md_th'>Common name: </td><td class='md_spacer'/><td class='md_value'>" + commonName + "</td></tr>";
        html += "<tr class='md_grey-bg'><td class='md_th'>Family name: </td><td class='md_spacer'/><td class='md_value'>" + familyName + "</td></tr>";
        String lastClass = "";
        if (row[7] != null && row[7].length() > 0) {
            html += "<tr class='" + lastClass + "'><td class='md_th'>Min depth: </td><td class='md_spacer'/><td class='md_value'>" + minDepth + "</td></tr>";
            lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
        }
        if (row[8] != null && row[8].length() > 0) {
            html += "<tr class='" + lastClass + "'><td class='md_th'>Max depth: </td><td class='md_spacer'/><td class='md_value'>" + maxDepth + "</td></tr>";
            lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
        }
        if (row[9] != null && row[9].length() > 0) {
            html += "<tr class='" + lastClass + "'><td class='md_th'>Metadata link: </td><td class='md_spacer'/><td class='md_value'><a target='_blank' href='" + metadataLink + "'>" + metadataLink
                    + "</a></td></tr>";
            lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
        }
        if (row[12] != null && row[12].length() > 0) {
            html += "<tr class='" + lastClass + "'><td class='md_th'>Area sq km: </td><td class='md_spacer'/><td class='md_value'>" + area + "</td></tr>";
            lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
        }
        html += "<tr class='" + lastClass + "'><td class='md_th'>Source website: </td><td class='md_spacer'/><td class='md_value'><a target='_blank' href='" + websiteUrl + "'>" + websiteUrl
                + "</a></td></tr>";
        lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";

        if (citation != null) {
            html += "<tr class='" + lastClass + "'><td class='md_th'>Citation: </td><td class='md_spacer'/><td class='md_value'>" + citation + "</td></tr>";
            lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
        }

        if (logoUrl != null) {
            html += "<tr class='" + lastClass + "'><td class='md_th'></td><td class='md_spacer'/><td class='md_value'><img src=\"" + logoUrl + "\"/></td></tr>";
        }

        html += "</table>";

        return html;
    }

    // Fetches the website url, citation and logo url from the data resource
    // associated with an expert distribution.
    public static String[] getDistributionCollectoryDetails(String dataResourceUid) {
        try {
            String url = CommonData.collectoryServer + "/dataResource/" + dataResourceUid;

            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(url);
            System.out.println(url);
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            int result = client.executeMethod(get);
            if (result == 200) {
                String txt = get.getResponseBodyAsString();
                JSONObject jo = JSONObject.fromObject(txt);
                if (jo == null) {
                    return null;
                } else {
                    String[] output = new String[13];
                    String websiteUrl = jo.containsKey("websiteUrl") ? jo.getString("websiteUrl") : "";
                    String citation = (jo.containsKey("citation") && jo.containsValue("citation")) ? jo.getString("citation") : null;
                    String logoUrl = null;

                    if (jo.containsKey("logoRef")) {
                        JSONObject logoObject = jo.getJSONObject("logoRef");
                        if (logoObject.containsKey("uri")) {
                            logoUrl = logoObject.getString("uri");
                        }
                    }

                    output[0] = websiteUrl;
                    output[1] = citation;
                    output[2] = logoUrl;

                    return output;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getMetadataHtmlForAreaChecklist(String spcode, String layerName) {
        if (spcode == null) {
            return null;
        }

        try {
            int count = CommonData.getSpeciesChecklistCountByWMS(CommonData.getSpeciesChecklistWMSFromSpcode(spcode)[1]);

            String url = CommonData.layersServer + "/checklist/" + spcode;
            String jsontxt = Util.readUrl(url);
            if (jsontxt == null || jsontxt.length() == 0) {
                return null;
            }

            JSONObject jo = JSONObject.fromObject(jsontxt);

            String html = "Checklist area\n";
            html += "<table class='md_table'>";

            String lastClass = "";

            if (layerName != null && jo.containsKey("geom_idx")) {
                html += "<tr class='" + lastClass + "'><td class='md_th'>Number of scientific names: </td><td class='md_spacer'/><td class='md_value'><a href='#' onClick='openAreaChecklist(\""
                        + jo.getString("geom_idx") + "\")'>" + count + "</a></td></tr>";
            } else {
                html += "<tr class='" + lastClass + "'><td class='md_th'>Number of scientific names: </td><td class='md_spacer'/><td class='md_value'>" + count + "</td></tr>";
            }

            lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";

            if (jo != null && jo.containsKey("metadata_u")) {
                html += "<tr class='" + lastClass + "'><td class='md_th'>Metadata link: </td><td class='md_spacer'/><td class='md_value'><a target='_blank' href='" + jo.getString("metadata_u") + "'>"
                        + jo.getString("metadata_u") + "</a></td></tr>";
                lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
            }
            if (jo != null && jo.containsKey("area_name")) {
                html += "<tr class='" + lastClass + "'><td class='md_th'>Area name: </td><td class='md_spacer'/><td class='md_value'>" + jo.getString("area_name") + "</td></tr>";
                lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
            }
            if (jo != null && jo.containsKey("area_km")) {
                html += "<tr class='" + lastClass + "'><td class='md_th'>Area sq km: </td><td class='md_spacer'/><td class='md_value'>" + jo.getString("area_km") + "</td></tr>";
                lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
            }

            try {
                if (jo != null && jo.containsKey("pid") && jo.containsKey("area_name")) {
                    String fid = Util.getStringValue(null, "fid", Util.readUrl(CommonData.layersServer + "/object/" + jo.getString("pid")));
                    String spid = Util.getStringValue("\"id\":\"" + fid + "\"", "spid", Util.readUrl(CommonData.layersServer + "/fields"));
                    if (spid != null) {
                        String layerInfoUrl = CommonData.layersServer + "/layers/view/more/" + spid;
                        html += "<tr class='" + lastClass + "'><td class='md_th'>More about this area: </td><td class='md_spacer'/><td class='md_value'><a target='_blank' href='" + layerInfoUrl
                                + "'>" + layerInfoUrl + "</a></td></tr>";
                        lastClass = lastClass.length() == 0 ? "md_grey-bg" : "";
                    }
                }
            } catch (Exception e) {
            }

            html += "</table>";

            return html;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}

class DListComparator implements Comparator {

    boolean ascending;
    boolean number;
    int index;

    public DListComparator(boolean ascending, boolean number, int index) {
        this.ascending = ascending;
        this.number = number;
        this.index = index;
    }

    public int compare(Object o1, Object o2) {
        String[] s1 = (String[]) o1;
        String[] s2 = (String[]) o2;
        int sort = 0;

        if (number) {
            Double d1 = null, d2 = null;
            try {
                d1 = Double.parseDouble(s1[index]);
            } catch (Exception e) {
            }
            try {
                d2 = Double.parseDouble(s2[index]);
            } catch (Exception e) {
            }
            if (d1 == null || d2 == null) {
                sort = (d1 == null ? 1 : 0) + (d2 == null ? -1 : 0);
            } else {
                sort = d1.compareTo(d2);
            }
        } else {
            String t1 = s1[index];
            String t2 = s2[index];
            if (t1 == null || t2 == null) {
                sort = (t1 == null ? 1 : 0) + (t2 == null ? -1 : 0);
            } else {
                sort = t1.compareTo(t2);
            }
        }

        return ascending ? sort : -sort;
    }
}
