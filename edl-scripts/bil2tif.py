#!/usr/bin/python
import os
import shutil
import edlconfig

for root, dirs, files in os.walk(edlconfig.dataset):
	for name in files:
		if ".BIL" in name:
			layername = name.replace(".BIL","")
			print(layername)		
			os.system(edlconfig.gdalapps+"/gdalwarp -s_srs '" + edlconfig.s_srs + "' -t_srs '" + edlconfig.t_srs + "' -srcnodata \"-9999\" -dstnodata \"-9999\" -of GTiff " + os.path.join(root,name) + " "  + os.path.join(root,layername) + ".tif")
#			os.system(edlconfig.gdalapps+"/gdal_translate -of GTiff " + os.path.join(root,name) + " "  + os.path.join(root,layername) + ".tif")

	#		os.system(edlconfig.gdalapps+"/gdalwarp -t_srs '" + edlconfig.t_srs + "' " +  os.path.join(root,layername) + "GDA.tif " + os.path.join(root,layername) + ".tif")

	#		os.system("rm " + os.path.join(root,layername) + "GDA.tif") 
