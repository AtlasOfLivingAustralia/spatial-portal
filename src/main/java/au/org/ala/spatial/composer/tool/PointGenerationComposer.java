/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.org.ala.spatial.composer.tool;

import au.org.ala.spatial.composer.input.UploadSpeciesController;
import au.org.ala.spatial.dto.UserDataDTO;
import au.org.ala.spatial.util.CommonData;
import au.org.emii.portal.menu.SelectedArea;
import org.ala.layers.intersect.SimpleRegion;
import org.ala.layers.intersect.SimpleShapeFile;
import org.apache.log4j.Logger;
import org.zkoss.zul.Doublebox;

import java.io.StringReader;
import java.util.List;

/**
 */
public class PointGenerationComposer extends ToolComposer {

    private static final Logger LOGGER = Logger.getLogger(PointGenerationComposer.class);

    private Doublebox resolution;

    public void afterCompose() {
        super.afterCompose();

        this.selectedMethod = "Point Generation";
        this.totalSteps = 2;

        this.loadAreaLayers();
        this.updateWindowTitle();

        this.updateName(getMapComposer().getNextAreaLayerName("Points on a grid"));
    }

    @Override
    public boolean onFinish() {

        SelectedArea sa = getSelectedArea();
        double[][] bbox = null;
        if (sa.getMapLayer() != null && sa.getMapLayer().getMapLayerMetadata() != null) {
            List<Double> bb = sa.getMapLayer().getMapLayerMetadata().getBbox();
            bbox[0][0] = bb.get(0);
            bbox[0][1] = bb.get(1);
            bbox[1][0] = bb.get(2);
            bbox[1][1] = bb.get(3);
        } else {
            bbox = SimpleShapeFile.parseWKT(sa.getWkt()).getBoundingBox();
        }

        //with bounding box, cut points
        SimpleRegion sr = SimpleShapeFile.parseWKT((sa.getMapLayer() != null ? sa.getMapLayer().getWKT() : sa.getWkt()));
        int width = (int) Math.ceil((bbox[1][0] - bbox[0][0]) / resolution.getValue());
        int height = (int) Math.ceil((bbox[1][1] - bbox[0][1]) / resolution.getValue());
        double startx = Math.floor(bbox[0][0] / resolution.getValue()) * resolution.getValue();
        double starty = Math.floor(bbox[0][1] / resolution.getValue()) * resolution.getValue();
        StringBuilder sb = new StringBuilder();
        sb.append("longitude,latitude");
        int count = 0;
        for (int i = 0; i <= width; i++) {
            for (int j = 0; j <= height; j++) {
                double lng = startx + i * resolution.getValue();
                double lat = starty + j * resolution.getValue();
                if (sr.isWithin(lng, lat)) {
                    sb.append("\n");
                    sb.append(lng).append(",").append(lat);
                    count++;
                }
            }
        }

        if (count <= 0) {
            getMapComposer().showMessage("No points generated. Try again with a smaller resolution or larger area.");
        } else if (count > Integer.parseInt(CommonData.getSettings().getProperty("max_record_count_upload"))) {
            getMapComposer().showMessage(count + " points generated. Maximum is " + CommonData.getSettings().getProperty("generated_points_max") + ".\n"
                    + "Try again with a larger resolution or smaller area.");
        } else {
            String name = tToolName.getValue();
            try {
                UploadSpeciesController.loadUserPoints(new UserDataDTO(name), new StringReader(sb.toString()), true, name
                        , "points in " + getSelectedAreaDisplayName() + " on " + resolution.getValue() + " degree resolution grid."
                        , getMapComposer(), null);

                detach();

                return true;
            } catch (Exception e) {
                LOGGER.error("failed to upload points");
            }
        }

        return false;
    }
}
