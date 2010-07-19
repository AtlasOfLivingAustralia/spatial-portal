#!/usr/bin/python26
import os
import shutil
import edlconfig
#dataset = "/mnt/transfer/media/LaCie/datasets/DEM/"
#gdalapps = "/home/angus/gdal-1.7.2/apps"

for root, dirs, files in os.walk(edlconfig.dataset):
	for dir in dirs:
#		print(dir)
		for name in os.listdir(os.path.join(root,dir)):
			if "hdr.adf" in name:
				layername = dir
				print(layername)		
#				os.system(edlconfig.gdalapps+"/gdal_translate -of GTiff " + os.path.join(root,dir,name) + " "  + os.path.join(root,layername) + ".tif")
				os.system(edlconfig.gdalapps+"/gdalwarp -s_srs '" + edlconfig.s_srs + "' -t_srs '" + edlconfig.t_srs + "' -of GTiff " + os.path.join(root,dir,name) + " "  + os.path.join(root,layername) + ".tif")

