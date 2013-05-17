#!/bin/bash

# This script periodically cleans up generated data from the spatial portal
export DATE=$(date +%y%m%d)
export OUTPUTFILE="/data/ala/cleanup.log"
export GEOSERVER_USRPWD="user:password"
export GEOSERVER_URL="http://localhost:8082/geoserver"

echo $DATE > $OUTPUTFILE

echo "## Delete subdirectories of /data/ala/data/alaspatial more than 2 days old" >> $OUTPUTFILE
find /data/ala/data/alaspatial -maxdepth 1 -mindepth 1 -regex '.*[0-9]+' -type d -mtime +2 -exec rm -r {} \; 1>> $OUTPUTFILE 2>&1

echo "## Delete files prefixed with 'JOB' from /data/ala/data/alaspatial older than 180 days" >> $OUTPUTFILE
find /data/ala/data/alaspatial/JOB* -mtime +180 -exec rm -r {} \; 1>> $OUTPUTFILE 2>&1

echo "## Delete files and subdirectories of /usr/local/tomcat/instance_03_webportal/webapps/webportal/print more than 2 days old" >> $OUTPUTFILE
find /usr/local/tomcat/instance_03_webportal/webapps/webportal/print -maxdepth 1 -mindepth 1 -regex '.*[0-9]+' -type d  -mtime +2 -exec rm -r {} \; 1>> $OUTPUTFILE 2>&1
find /usr/local/tomcat/instance_03_webportal/webapps/webportal/print -maxdepth 1 -mindepth 1 -regex '.*[0-9]+' -mtime +2 -exec rm {} \; 1>> $OUTPUTFILE 2>&1

echo "## Delete anything older than 90 days from generated density layers folder" >> $OUTPUTFILE 
find /data/ala/data/layers/process/density -maxdepth 1 -mindepth 1 -type d -mtime +90  -exec rm -r {} \; 1>> $OUTPUTFILE 2>&1

echo "## Delete anything older than 90 days from generated endemism layers folder" >> $OUTPUTFILE
find /data/ala/data/layers/process/endemism -maxdepth 1 -mindepth 1 -type d -mtime +90  -exec rm -r {} \; 1>> $OUTPUTFILE 2>&1
  
echo "## Clean out anything older than 180 days from the analysis directories under /data/ala/runtime/output:" >> $OUTPUTFILE

echo "## aloc directory" >> $OUTPUTFILE
find /data/ala/runtime/output/aloc -maxdepth 1 -mindepth 1 -type d -mtime +180  -exec rm -r {} \; 1>> $OUTPUTFILE 2>&1
find /data/ala/runtime/output/aloc -maxdepth 1 -mindepth 1 -mtime +180 -exec rm {} \; 1>> $OUTPUTFILE 2>&1

echo "## export directory" >> $OUTPUTFILE
find /data/ala/runtime/output/export -maxdepth 1 -mindepth 1 -type d -mtime +180  -exec rm -r {} \; 1>> $OUTPUTFILE 2>&1
find /data/ala/runtime/output/export -maxdepth 1 -mindepth 1 -mtime +180 -exec rm {} \; 1>> $OUTPUTFILE 2>&1
  
echo "## filtering directory" >> $OUTPUTFILE
find /data/ala/runtime/output/filtering -maxdepth 1 -mindepth 1 -type d -mtime +180  -exec rm -r {} \; 1>> $OUTPUTFILE 2>&1
find /data/ala/runtime/output/filtering -maxdepth 1 -mindepth 1 -mtime +180 -exec rm {} \; 1>> $OUTPUTFILE 2>&1

echo "## gdm directory" >> $OUTPUTFILE
find /data/ala/runtime/output/gdm -maxdepth 1 -mindepth 1 -type d -mtime +180  -exec rm -r {} \; 1>> $OUTPUTFILE 2>&1
find /data/ala/runtime/output/gdm -maxdepth 1 -mindepth 1 -mtime +180 -exec rm {} \; 1>> $OUTPUTFILE 2>&1

echo "## layers directory" >> $OUTPUTFILE
find /data/ala/runtime/output/layers -maxdepth 1 -mindepth 1 -type d -mtime +180  -exec rm -r {} \; 1>> $OUTPUTFILE 2>&1
find /data/ala/runtime/output/layers -maxdepth 1 -mindepth 1 -mtime +180 -exec rm {} \; 1>> $OUTPUTFILE 2>&1

echo "## maxent directory" >> $OUTPUTFILE
find /data/ala/runtime/output/maxent -maxdepth 1 -mindepth 1 -type d -mtime +180  -exec rm -r {} \; 1>> $OUTPUTFILE 2>&1
find /data/ala/runtime/output/maxent -maxdepth 1 -mindepth 1 -mtime +180 -exec rm {} \; 1>> $OUTPUTFILE 2>&1

echo "## sampling directory" >> $OUTPUTFILE
find /data/ala/runtime/output/sampling -maxdepth 1 -mindepth 1 -type d -mtime +180  -exec rm -r {} \; 1>> $OUTPUTFILE 2>&1
find /data/ala/runtime/output/sampling -maxdepth 1 -mindepth 1 -mtime +180 -exec rm {} \; 1>> $OUTPUTFILE 2>&1
  
echo "## session directory" >> $OUTPUTFILE
find /data/ala/runtime/output/session -maxdepth 1 -mindepth 1 -type d -mtime +180  -exec rm -r {} \; 1>> $OUTPUTFILE 2>&1
find /data/ala/runtime/output/session -maxdepth 1 -mindepth 1 -mtime +180 -exec rm {} \; 1>> $OUTPUTFILE 2>&1
  
echo "## sitesbyspecies directory" >> $OUTPUTFILE
find /data/ala/runtime/output/sitesbyspecies -maxdepth 1 -mindepth 1 -type d -mtime +180  -exec rm -r {} \; 1>> $OUTPUTFILE 2>&1
find /data/ala/runtime/output/sitesbyspecies -maxdepth 1 -mindepth 1 -mtime +180 -exec rm {} \; 1>> $OUTPUTFILE 2>&1   

echo "## Remove generated aloc layers from geoserver" >> $OUTPUTFILE
find /data2/ala/data/geoserver_data_dir/workspaces/ALA/aloc* -maxdepth 0 -mindepth 0 -regex '.*[0-9]+' -type d -mtime +180  -exec rm -r {} \; 1>> $OUTPUTFILE 2>&1
find /data2/ala/data/geoserver_data_dir/styles/aloc* -maxdepth 0 -mindepth 0 -regex '.*[0-9]+' -mtime +180 -exec rm {} \; 1>> $OUTPUTFILE 2>&1

echo "## Remove generated gdm layers from geoserver" >> $OUTPUTFILE
find /data2/ala/data/geoserver_data_dir/workspaces/ALA/gdm* -maxdepth 0 -mindepth 0 -regex '.*[0-9]+' -type d -mtime +180  -exec rm -r {} \; 1>> $OUTPUTFILE 2>&1
find /data2/ala/data/geoserver_data_dir/styles/gdm* -maxdepth 0 -mindepth 0 -regex '.*[0-9]+' -mtime +180 -exec rm {} \; 1>> $OUTPUTFILE 2>&1

echo "## Remove generated odensity layers from geoserver" >> $OUTPUTFILE
find /data2/ala/data/geoserver_data_dir/workspaces/ALA/odensity_* -maxdepth 0 -mindepth 0 -regex '.*[0-9]+' -type d -mtime +180  -exec rm -r {} \; 1>> $OUTPUTFILE 2>&1
find /data2/ala/data/geoserver_data_dir/styles/odensity_* -maxdepth 0 -mindepth 0 -regex '.*[0-9]+' -mtime +180 -exec rm {} \; 1>> $OUTPUTFILE 2>&1

echo "## Remove generated srichness layers from geoserver" >> $OUTPUTFILE
find /data2/ala/data/geoserver_data_dir/workspaces/ALA/srichness_* -maxdepth 0 -mindepth 0 -regex '.*[0-9]+' -type d -mtime +180  -exec rm -r {} \; 1>> $OUTPUTFILE 2>&1
find /data2/ala/data/geoserver_data_dir/styles/srichness_* -maxdepth 0 -mindepth 0 -mtime +180 -regex '.*[0-9]+' -exec rm {} \; 1>> $OUTPUTFILE 2>&1

echo "## Remove generated envelope layers from geoserver" >> $OUTPUTFILE
find /data2/ala/data/geoserver_data_dir/workspaces/ALA/envelope_* -maxdepth 0 -mindepth 0 -regex '.*[0-9]+' -type d -mtime +180  -exec rm -r {} \; 1>> $OUTPUTFILE 2>&1
find /data2/ala/data/geoserver_data_dir/styles/envelope_* -maxdepth 0 -mindepth 0 -mtime +180 -regex '.*[0-9]+' -exec rm {} \; 1>> $OUTPUTFILE 2>&1

echo "## Use geoserver REST API to force it to reload its configuration given the changes that we have made." 1>> $OUTPUTFILE 2>&1
curl -u $GEOSERVER_USRPWD -XPOST -d "" $GEOSERVER_URL/rest/reload 1>> $OUTPUTFILE 2>&1

echo "## done" 1>> $OUTPUTFILE 2>&1