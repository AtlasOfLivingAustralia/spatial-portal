#!/usr/bin/python
import os
import shutil
import edlconfig

for root, dirs, files in os.walk(edlconfig.dataset):
	for name in files:
		if ".tif" in name:
			layername = name.replace(".tif","")
			print(layername)		
		#	os.system(edlconfig.gdalapps+"/gdal_translate -of EHdr " + os.path.join(root,name) + " "  + os.path.join(root,layername) + ".bil")
			command = edlconfig.gdalapps+"/gdalwarp -ot int16 -te 109.51 -44.37 157.28 -8.19 -tr 0.01 -0.01 -s_srs '" + edlconfig.s_srs + "' -t_srs '" + edlconfig.t_srs + "' -of EHdr -srcnodata -9999 -dstnodata -9999 " +  os.path.join(root,layername) + ".tif " + os.path.join(root,layername) + ".bil"

			print(command)
			os.system(command)
			
	
