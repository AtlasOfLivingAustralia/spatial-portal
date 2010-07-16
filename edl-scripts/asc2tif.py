#!/usr/bin/python26
import os
import shutil
import edlconfig
#dataset = "/mnt/transfer/media/LaCie/datasets/DEM/"
#gdalapps = "/home/angus/gdal-1.7.2/apps"

for root, dirs, files in os.walk(edlconfig.dataset):
	for name in files:
		if ".asc" in name:
			layername = name.replace(".asc","")
			print(layername)	
			os.system(edlconfig.gdalapps+"/gdal_translate -of GTiff " + os.path.join(root,name) + " "  + os.path.join(root,layername) + "GDA.tif")
			os.system(edlconfig.gdalapps+"/gdalwarp -t_srs '" + edlconfig.t_srs + "' " +  os.path.join(root,layername) + "GDA.tif " + os.path.join(root,layername) + ".tif")
			os.system("rm " + os.path.join(root,layername) + "GDA.tif") 
