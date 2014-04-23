package org.ala.layers.util;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URL;

public class MapCache {

    private static MapCache singleton;

    private MapCache() {
    }

    public static MapCache getMapCache() {
        if (singleton == null) {
            singleton = new MapCache();
        }
        return singleton;
    }

    String mapCachePath = "/data/layers-service/mapCache/";

    String baseUrl = "http://spatial.ala.org.au/geoserver/ALA/wms?service=WMS" +
            "&version=1.1.0&request=GetMap" +
            "&sld=http://fish.ala.org.au/data/dist.sld" +
            "&layers=ALA:aus1,ALA:Distributions&styles=" +
            "&bbox=109,-47,157,-7&srs=EPSG:4326" +
            "&format=image/png&width=400&height=400&viewparams=s:";

    public InputStream getCachedMap(String geomIdx) throws Exception {
        File map = new File(mapCachePath + geomIdx);
        if (!map.exists()) {
            cacheMap(geomIdx);
        }
        return new FileInputStream(map);
    }

    public void cacheMap(String geomIdx) throws IOException {
        File map = new File(mapCachePath + geomIdx);
        File directory = new File(mapCachePath);
        if (!directory.exists()) {
            FileUtils.forceMkdir(directory);
        }
        if (map.exists())
            map.delete(); //remove original

        map.createNewFile();
        //download map and write to file
        InputStream mapInput = (new URL(baseUrl + geomIdx)).openStream();
        FileOutputStream out = new FileOutputStream(map);
        int read = 0;
        byte[] buff = new byte[1024];
        while ((read = mapInput.read(buff)) > 0) {
            out.write(buff, 0, read);
        }
        out.flush();
        mapInput.close();
    }
}
