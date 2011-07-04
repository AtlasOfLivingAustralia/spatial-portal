#!/usr/bin/python

#This script removes tiles from the GWC cache
#Ensure that seed_config.py has been configured before use

import os
import seed_config

for layer in seed_config.layers:
        curlstring="curl -u " + seed_config.geoserver_userpass + " -XPOST -H 'Content-type: text/xml' -d '<?xml version=\"1.0\" encoding=\"UTF-8\"?><seedRequest><name>" + layer + "</name><srs><number>900913</number></srs><zoomStart>" + seed_config.zoom_start + "</zoomStart><zoomStop>" + seed_config.zoom_stop + "</zoomStop><format>image/png</format><type>truncate</type><threadCount>2</threadCount></seedRequest>' " + seed_config.geoserver_url + "/geoserver/gwc/rest/seed/" + layer + ".xml"
        os.system(curlstring)
        print "truncated " + layer
