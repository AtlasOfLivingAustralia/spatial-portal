#!/usr/bin/python
import os
import shutil
import edlconfig

for root, dirs, files in os.walk(edlconfig.dataset):
	for name in files:
		if ".shp" in name:
			layername = name.replace(".shp","")
			print(layername)		
			command = "sudo " + edlconfig.gdalapps + "/gdal_rasterize -of GTiff -burn 1 -tr 0.01 0.01 -l " + layername + " " + os.path.join(root,name) + " " + os.path.join(root,layername) + ".tif"
			print(command)
			os.system(command)
			
	
