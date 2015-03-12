package au.org.ala.spatial.composer.tool;

import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.spatial.StringConstants;
import au.org.ala.spatial.composer.add.area.AreaToolComposer;
import au.org.ala.spatial.util.CommonData;
import au.org.ala.spatial.util.LayersUtil;
import au.org.ala.spatial.util.Util;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.log4j.Logger;
import org.apache.log4j.lf5.util.StreamUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;

import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Adam
 */
public class PointComparison extends AreaToolComposer {

    private static final Logger LOGGER = Logger.getLogger(PointComparison.class);
    private Button btnAdd;
    private Button btnCompare;
    private Button btnDownload;
    private Button btnCancel;
    private Listbox lbPoints;
    private Listbox lbResults;
    private List<String[]> points = new ArrayList<String[]>();
    private List<MapLayer> layers = new ArrayList<MapLayer>();
    private List<Listitem> items = new ArrayList<Listitem>();
    private int currentPoint = -1;
    private String comparisonCsv = "";

    @Override
    public void afterCompose() {
        super.afterCompose();

        lbPoints.setModel(new SimpleListModel<String[]>(points));
        lbPoints.setItemRenderer(new ListitemRenderer<String[]>() {
            @Override
            public void render(Listitem item, String[] data, int index) throws Exception {
                items.set(index, item);
                Listcell lc = new Listcell(String.valueOf(index + 1));
                lc.setParent(item);

                lc = new Listcell();
                Textbox txt1 = new Textbox(data[0] != null ? data[0] : "");
                lc.setSclass("txtCoordinate");
                txt1.setParent(lc);
                if (index != currentPoint) txt1.setDisabled(true);
                lc.setParent(item);

                lc = new Listcell();
                Textbox txt2 = new Textbox(data[1] != null ? data[1] : "");
                lc.setSclass("txtCoordinate");
                txt2.setParent(lc);
                if (index != currentPoint) txt2.setDisabled(true);
                lc.setParent(item);

                lc = new Listcell();
                Button b = new Button("edit");
                b.setSclass("btn-mini");
                b.setParent(lc);
                lc.setParent(item);
                b.addEventListener("onClick", new EventListener<Event>() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        enablePointClick();
                        disableEdit();
                        ((Textbox) event.getTarget().getParent().getPreviousSibling().getFirstChild()).setDisabled(false);
                        ((Textbox) event.getTarget().getParent().getPreviousSibling().getPreviousSibling().getFirstChild()).setDisabled(false);
                        ((Button) event.getTarget().getParent().getNextSibling().getFirstChild()).setDisabled(false);
                        currentPoint = Integer.parseInt(((Listcell) event.getTarget().getParent().getPreviousSibling().getPreviousSibling().getPreviousSibling()).getLabel());
                        currentPoint--;
                    }
                });
                lc = new Listcell();
                b = new Button("save");
                b.setParent(lc);
                b.setSclass("btn-mini");
                lc.setParent(item);
                if (index != currentPoint) b.setDisabled(true);
                b.addEventListener("onClick", new EventListener<Event>() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        disableEdit();
                        currentPoint = Integer.parseInt(((Listcell) event.getTarget().getParent().getPreviousSibling().getPreviousSibling().getPreviousSibling().getPreviousSibling()).getLabel());
                        currentPoint--;
                        points.get(currentPoint)[1] = ((Textbox) event.getTarget().getParent().getPreviousSibling().getPreviousSibling().getFirstChild()).getValue();
                        points.get(currentPoint)[0] = ((Textbox) event.getTarget().getParent().getPreviousSibling().getPreviousSibling().getPreviousSibling().getFirstChild()).getValue();

                        MapComposer mc = getMapComposer();
                        layerName = mc.getNextAreaLayerName("Point");
                        mc.deactiveLayer(layers.get(currentPoint), true, false);
                        String pointGeom = "POINT(" + points.get(currentPoint)[0] + " " + points.get(currentPoint)[1] + ")";
                        MapLayer mapLayer = mc.addWKTLayer(pointGeom, layerName, "Point");
                        mapLayer.getMapLayerMetadata().setMoreInfo(LayersUtil.getMetadataForWKT(CommonData.lang(StringConstants.METADATA_USER_BOUNDING_BOX), pointGeom));
                        layers.set(currentPoint, mapLayer);
                    }
                });
                lc = new Listcell();
                b = new Button("remove");
                b.setParent(lc);
                b.setSclass("btn-mini");
                lc.setParent(item);
                b.addEventListener("onClick", new EventListener<Event>() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        int point = Integer.parseInt(((Listcell) event.getTarget().getParent().getPreviousSibling().getPreviousSibling().getPreviousSibling().getPreviousSibling().getPreviousSibling()).getLabel());
                        ;
                        point--;
                        getMapComposer().deactiveLayer(layers.get(point), true, false);
                        if (currentPoint == point) {
                            currentPoint = -1;
                        }
                        points.remove(point);
                        layers.remove(point);
                        items.remove(point);
                        lbPoints.setModel(new SimpleListModel<Object>(points));

                    }
                });
            }
        });

        lbResults.setItemRenderer(new ListitemRenderer<String[]>() {
            @Override
            public void render(Listitem item, String[] data, int index) throws Exception {
                for (int i = 0; i < data.length; i++) {
                    Listcell lc = new Listcell(data[i]);
                    lc.setParent(item);
                }
            }
        });

    }

    public void onClick$btnAdd(Event event) {
        points.add(new String[2]);
        layers.add(null);
        items.add(null);
        currentPoint = points.size() - 1;
        lbPoints.setModel(new SimpleListModel<Object>(points));

        enablePointClick();
    }

    public void onClick$btnCancel(Event event) {
        for (MapLayer ml : layers) {
            getMapComposer().deactiveLayer(ml, true, false);
        }
        this.detach();
    }

    public void onMapClick(Event event) {
        String pointGeom = (String) event.getData();
        try {
            if (pointGeom.contains(StringConstants.NAN_NAN)) {
            } else {
                points.get(currentPoint)[0] = pointGeom.replace("POINT(", "").split(" ")[0];
                points.get(currentPoint)[1] = pointGeom.split(" ")[1].replace(")", "");
                lbPoints.setModel(new SimpleListModel<Object>(points));

                //get the current MapComposer instance
                MapComposer mc = getMapComposer();

                //add feature to the map as a new layer
                layerName = mc.getNextAreaLayerName("Point");
                MapLayer mapLayer = mc.addWKTLayer(pointGeom, layerName, "Point");
                mapLayer.getMapLayerMetadata().setMoreInfo(LayersUtil.getMetadataForWKT(CommonData.lang(StringConstants.METADATA_USER_BOUNDING_BOX), pointGeom));

                layers.set(currentPoint, mapLayer);

                disableEdit();
            }
        } catch (Exception e) {
            LOGGER.error("Error adding user point", e);
        }
    }

    public void onClick$btnDownload(Event event) {
        Filedownload.save(comparisonCsv, "text/plain", "point_comparison.csv");
    }

    public void onClick$btnCompare(Event event) {
        try {
            //sampling
            String url = CommonData.getSettings().getProperty("layers_batch_intersect_url");
            NameValuePair[] params = new NameValuePair[2];
            StringBuilder sb = new StringBuilder();
            JSONArray ja = CommonData.getLayerListJSONArray();
            for (int i = 0; i < ja.size(); i++) {
                JSONObject jo = (JSONObject) ja.get(i);
                if (jo.containsKey("name")) {
                    String name = jo.get("name").toString();
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(name);
                }
            }
            params[0] = new NameValuePair("fids", sb.toString());
            sb = new StringBuilder();
            for (int i = 0; i < points.size(); i++) {
                try {
                    //validate points as doubles
                    String part = "" + Double.parseDouble(points.get(i)[1]) + "," + Double.parseDouble(points.get(i)[0]);
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(part);
                } catch (Exception e) {

                }
            }
            params[1] = new NameValuePair("points", sb.toString());
            String batchId = Util.readUrlPost(url, params);
            JSONParser jp = new JSONParser();
            batchId = ((JSONObject) jp.parse(batchId)).get("batchId").toString();
            LOGGER.debug("batch intersect id: " + batchId);

            Thread.sleep(2000);

            long maxtime = 30000; //30s
            long starttime = System.currentTimeMillis();

            String csv = "";
            while (starttime + maxtime > System.currentTimeMillis()) {
                Thread.sleep(2000);
                String response = Util.readUrl(url + "/" + batchId);
                try {
                    String status = ((JSONObject) jp.parse(response)).get("status").toString();
                    if ("finished".equals(status)) {
                        URL get = new URL(((JSONObject) jp.parse(response)).get("downloadUrl").toString());
                        InputStream is = get.openStream();
                        ZipInputStream zip = new ZipInputStream(is);
                        ZipEntry ze = zip.getNextEntry();
                        if (ze != null) {
                            csv = new String(StreamUtils.getBytes(zip), "UTF-8");
                        }
                        zip.close();
                        is.close();
                        break;
                    }
                } catch (Exception e) {
                }

            }

            //transpose csv and merge longitude & latitude columns
            List<String[]> data = new CSVReader(new StringReader(csv)).readAll();
            List<Integer> rowNumbers = new ArrayList<Integer>();
            for (int j = 2; j < data.get(0).length; j++) {
                for (int i = 1; i < data.size(); i++) {
                    if (data.get(i)[j] != null && !data.get(i)[j].isEmpty() && !"n/a".equals(data.get(i)[j])) {
                        rowNumbers.add(j);
                        break;
                    }
                }
            }
            String[][] tdata = new String[rowNumbers.size() + 1][data.size()];
            StringBuilder tcsv = new StringBuilder();
            int row = 0;
            for (int i = 0; i < data.size(); i++) {
                if (i > 0) {
                    tcsv.append(",");
                }
                tdata[row][i] = data.get(i)[0] + " " + data.get(i)[1];
                tcsv.append(tdata[0][i]);
            }
            row++;
            for (int j = 2; j < data.get(0).length; j++) {
                if (rowNumbers.size() > 0 && rowNumbers.get(0) == j) {
                    rowNumbers.remove(0);
                    tcsv.append("\n");
                    for (int i = 0; i < data.size(); i++) {

                        if (i > 0) {
                            tcsv.append(",");
                        }
                        if (i == 0) {
                            tcsv.append("\"").append(CommonData.getLayerDisplayName(data.get(i)[j])).append("\"");
                            tdata[row][i] = CommonData.getLayerDisplayName(data.get(i)[j]);
                        } else {
                            tcsv.append(data.get(i)[j]);
                            tdata[row][i] = data.get(i)[j];
                        }
                    }
                    row++;
                }
            }

            comparisonCsv = tcsv.toString();
            btnDownload.setDisabled(false);

            lbResults.setModel(new SimpleListModel<String[]>(tdata));

        } catch (Exception e) {
            LOGGER.error("error comparing points", e);
        }
    }

    private void disableEdit() {
        for (Listitem c1 : items) {
            for (Component c3 : c1.getChildren()) {
                if (c3.getChildren() != null) {
                    for (Component c2 : c3.getChildren()) {
                        if (c2 instanceof Button && ((Button) c2).getLabel().equals("save")) {
                            ((Button) c2).setDisabled(true);
                        }
                        if (c2 instanceof Textbox) {
                            ((Textbox) c2).setDisabled(true);
                        }
                    }
                }
            }
        }
    }

    private void enablePointClick() {
        String script = "window.mapFrame.addPointDrawingTool()";
        getMapComposer().getOpenLayersJavascript().execute(getMapComposer().getOpenLayersJavascript().getIFrameReferences() + script);
    }
}
