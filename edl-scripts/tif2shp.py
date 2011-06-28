#!/usr/bin/python
import os
import shutil
import edlconfig

for root, dirs, files in os.walk(edlconfig.dataset):
	for name in files:
		if ".tif" in name:
			layername = name.replace(".tif","")
			print(layername)		
			command = "gdal_polygonize.py -f \"ESRI shapefile\" " + os.path.join(root,name) + " " + os.path.join(root,layername)
			print(command)
			os.system(command)
			
	
