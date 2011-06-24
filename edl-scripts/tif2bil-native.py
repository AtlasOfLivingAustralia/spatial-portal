#!/usr/bin/python
import os
import shutil
import edlconfig

for root, dirs, files in os.walk(edlconfig.dataset):
	for name in files:
		if name.endswith(".geotiff"):
			layername = name.replace(".geotiff","")
			print(layername)	
	
			p = os.popen(edlconfig.gdalapps+"/gdalinfo -mm " + os.path.join(root,name),"r")
			while 1:
                        	line = p.readline()
                       		if not line: break
				if "Type" in line:
					dataType = line.split("Type=")[1].split(",")[0]
					if dataType == "Int32": #Diva can't handle Int32
						dataType = "Float32"
					command = edlconfig.gdalapps+"/gdalwarp -ot " + dataType + " -s_srs '" + edlconfig.s_srs + "' -t_srs '" + edlconfig.t_srs + "' -of EHdr -srcnodata -9999 -dstnodata -9999 " +  os.path.join(root,layername) + ".geotiff " + os.path.join("/data/ala/data/source/native-bil",layername) + ".bil"

					print(command)
					os.system(command)
			
	
