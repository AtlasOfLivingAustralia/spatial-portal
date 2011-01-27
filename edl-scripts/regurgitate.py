#!/usr/bin/python

#from geoserver.catalog import Catalog


#tiffdata = {'tiff':'/mnt/transfer/MCAS_1k_Datapack/Climate/erosivity.tif'}


#tiffdata = {
#      'tiff': '/home/angus/dwins-gsconfig.py-7000eaa/test/data/Pk50095.tif',
#      'tfw':  '/home/angus/dwins-gsconfig.py-7000eaa/test/data/Pk50095.tfw',
#      'prj':  '/home/angus/dwins-gsconfig.py-7000eaa/test/data/Pk50095.prj'
#    }

#ala = cat.get_workspace("ALA")
#cat.create_coveragestore("test", tiffdata, ala)
#if(cat.get_resource("test", workspace=ala) is not None):
#	print("woot")

import os
import edlconfig

for root, dirs, files in os.walk(edlconfig.dataset):
	for name in files:
		if ".tif" in name:
			layername = name.replace(".tif","")
			os.system("curl -u " + edlconfig.geoserver_userpass + " -XDELETE " + edlconfig.geoserver_url + "/geoserver/rest/layers/" + layername)
			os.system("curl -u " + edlconfig.geoserver_userpass + " -XDELETE " + edlconfig.geoserver_url + "/geoserver/rest/workspaces/ALA/coveragestores/" + layername + "/coverages/" + layername)
			os.system("curl -u " + edlconfig.geoserver_userpass + " -XDELETE " + edlconfig.geoserver_url + "/geoserver/rest/workspaces/ALA/coveragestores/" + layername)
	
