#!/bin/bash

# This script generates the occurrence density and species richness layers and then loads them into the spatial portal.

DATE=$(date +built_%y%m%d)
GEOSERVER_USRPWD="user:password"
GEOSERVER_URL="http://localhost:8082/geoserver"
PTH="/data/ala/data/layers"
GDAL_TRANSLATE="/data/ala/utils/gdal-1.9.0/apps/gdal_translate"

echo $DATE

mkdir $PTH/process/density/$DATE/

echo $DATE > $PTH/process/density/$DATE/build.log

java -Xmx20000m -cp /usr/local/tomcat/instance_00_alaspatial/webapps/alaspatial/WEB-INF/classes/.:/usr/local/tomcat/instance_00_alaspatial/webapps/alaspatial/WEB-INF/lib/* org.ala.spatial.analysis.layers.DensityLayers "http://biocache.ala.org.au/ws" "$PTH/process/density/$DATE/" 9 "-180,-90,180,90" 0.01 6 >> $PTH/process/density/$DATE/build.log

if [ -f "$PTH/process/density/$DATE/_species_density_av_9x9_001.grd" ]
then

mv $PTH/process/density/$DATE/_species_density_av_9x9_001.gri $PTH/process/density/$DATE/srichness.gri
mv $PTH/process/density/$DATE/_species_density_av_9x9_001.grd $PTH/process/density/$DATE/srichness.grd
mv $PTH/process/density/$DATE/_species_density_av_9x9_001.asc $PTH/process/density/$DATE/srichness.asc
mv $PTH/process/density/$DATE/_occurrence_density_av_9x9_001.gri $PTH/process/density/$DATE/odensity.gri
mv $PTH/process/density/$DATE/_occurrence_density_av_9x9_001.grd $PTH/process/density/$DATE/odensity.grd
mv $PTH/process/density/$DATE/_occurrence_density_av_9x9_001.asc $PTH/process/density/$DATE/odensity.asc

cp $PTH/process/density/$DATE/*density*.g* $PTH/ready/diva/
cp $PTH/process/density/$DATE/*richness*.g* $PTH/ready/diva/
cp $PTH/process/density/*.prj $PTH/process/density/$DATE/

java -Xmx8000m -cp $PTH/util/cutpoints/. GridLegend $PTH/ready/diva/odensity $PTH/test/odensity 8 1 >> $PTH/process/density/$DATE/build.log

java -Xmx8000m -cp $PTH/util/cutpoints/. GridLegend $PTH/ready/diva/srichness $PTH/test/srichness 8 1 >> $PTH/process/density/$DATE/build.log

$GDAL_TRANSLATE -of GTiff -ot Float32 $PTH/process/density/$DATE/odensity.asc $PTH/process/density/$DATE/odensity.tif >> $PTH/process/density/$DATE/build.log

mv $PTH/process/density/$DATE/odensity.tif $PTH/ready/geotiff/odensity.tif

$GDAL_TRANSLATE -of GTiff -ot Float32 $PTH/process/density/$DATE/srichness.asc $PTH/process/density/$DATE/srichness.tif >> $PTH/process/density/$DATE/build.log

mv $PTH/process/density/$DATE/srichness.tif $PTH/ready/geotiff/srichness.tif

curl -u $GEOSERVER_USRPWD -XPUT -H "Content-type: text/plain"  -d "file://$PTH/ready/geotiff/odensity.tif" $GEOSERVER_URL/rest/workspaces/ALA/coveragestores/odensity/external.geotiff >> $PTH/process/density/$DATE/build.log

curl -u $GEOSERVER_USRPWD -XPUT -H "Content-type: text/plain"  -d "file://$PTH/ready/geotiff/srichness.tif" $GEOSERVER_URL/rest/workspaces/ALA/coveragestores/srichness/external.geotiff >> $PTH/process/density/$DATE/build.log

curl -u $GEOSERVER_USRPWD -XPOST -H "Content-type: text/xml"  -d "<style><name>odensity_style</name><filename>odensity.sld</filename></style>" $GEOSERVER_URL/rest/styles >> $PTH/process/density/$DATE/build.log

curl -u $GEOSERVER_USRPWD -XPOST -H "Content-type: text/xml"  -d "<style><name>srichness_style</name><filename>srichness.sld</filename></style>"  $GEOSERVER_URL/rest/styles >> $PTH/process/density/$DATE/build.log

curl -u $GEOSERVER_USRPWD -XPUT -H "Content-type: application/vnd.ogc.sld+xml"  -d @$PTH/test/odensity.sld $GEOSERVER_URL/rest/styles/odensity_style >> $PTH/process/density/$DATE/build.log

curl -u $GEOSERVER_USRPWD -XPUT -H "Content-type: application/vnd.ogc.sld+xml"  -d @$PTH/test/srichness.sld $GEOSERVER_URL/rest/styles/srichness_style >> $PTH/process/density/$DATE/build.log

curl -u $GEOSERVER_USRPWD -XPUT -H "Content-type: text/xml"   -d "<layer><enabled>true</enabled><defaultStyle><name>odensity_style</name></defaultStyle></layer>" $GEOSERVER_URL/rest/layers/ALA:odensity >> $PTH/process/density/$DATE/build.log

curl -u $GEOSERVER_USRPWD -XPUT -H "Content-type: text/xml"   -d "<layer><enabled>true</enabled><defaultStyle><name>srichness_style</name></defaultStyle></layer>" $GEOSERVER_URL/rest/layers/ALA:srichness >> $PTH/process/density/$DATE/build.log

curl -u $GEOSERVER_USRPWD -XPOST -H 'Content-type: text/xml' -d '<?xml version="1.0" encoding="UTF-8"?><seedRequest><name>ALA:odensity</name><srs><number>900913</number></srs><zoomStart>0</zoomStart><zoomStop>10</zoomStop><format>image/png</format><type>reseed</type><threadCount>1</threadCount></seedRequest>' $GEOSERVER_URL/gwc/rest/seed/ALA:odensity.xml  >> $PTH/process/density/$DATE/build.log

curl -u $GEOSERVER_USRPWD -XPOST -H 'Content-type: text/xml' -d '<?xml version="1.0" encoding="UTF-8"?><seedRequest><name>ALA:srichness</name><srs><number>900913</number></srs><zoomStart>0</zoomStart><zoomStop>10</zoomStop><format>image/png</format><type>reseed</type><threadCount>1</threadCount></seedRequest>' $GEOSERVER_URL/gwc/rest/seed/ALA:srichness.xml  >> $PTH/process/density/$DATE/build.log

#Fix mode and ownership of diva grids
chmod 777 $PTH/ready/diva/* >> $PTH/process/density/$DATE/build.log
chown tomcat:wheel $PTH/ready/diva/* >> $PTH/process/density/$DATE/build.log

#Fix mode and ownership of geotiffs
chmod 777 $PTH/ready/geotiff/* >> $OUTPUTDIR/build.log
chown tomcat:wheel $PTH/ready/geotiff/* >> $PTH/process/density/$DATE/build.log

# Regenerate layer analysis and distances
echo "regenerating layer analysis and distances" >> $PTH/process/density/$DATE/build.log
rm -f /data/ala/data/layers/analysis/0.5/el898.gr* >> $PTH/process/density/$DATE/build.log
rm -f /data/ala/data/layers/analysis/0.5/el899.gr* >> $PTH/process/density/$DATE/build.log
rm -f /data/ala/data/layers/analysis/0.01/el898.gr* >> $PTH/process/density/$DATE/build.log
rm -f /data/ala/data/layers/analysis/0.01/el899.gr* >> $PTH/process/density/$DATE/build.log
rm -f /data/ala/data/layers/analysis/0.0025/el898.gr* >> $PTH/process/density/$DATE/build.log
rm -f /data/ala/data/layers/analysis/0.0025/el899.gr* >> $PTH/process/density/$DATE/build.log
cp /data/ala/data/alaspatial/layerDistances.properties /data/ala/data/alaspatial/layerDistances.properties_old >> $PTH/process/density/$DATE/build.log
sed -i '/el898\|el899/d' /data/ala/data/alaspatial/layerDistances.properties >> $PTH/process/density/$DATE/build.log

sh $PTH/process/layer-ingestion-1.0-SNAPSHOT/environmental_background_processing.sh 1>> $PTH/process/density/$DATE/build.log 2>&1

echo "finished" >> $PTH/process/density/$DATE/build.log

cp $PTH/process/density/$DATE/build.log /data/ala/runtime/output/density_layers.log

else
echo "failed" >> $PTH/process/density/$DATE/build.log
fi
