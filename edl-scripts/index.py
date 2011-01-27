#!/usr/bin/python

import os
import edlconfig

for root, dirs, files in os.walk(edlconfig.dataset):
	for name in files:
		if ".tif" in name:
			layername = edlconfig.source + "_" + name.replace(".tif","")
#			print(os.path.join(root,name));
			p = os.popen(edlconfig.gdalapps + "/gdalinfo -mm " + os.path.join(root,name),"r")
			while 1:
                                line = p.readline()
#                                print(line)
				if not line: break
				if "Lower Left" in line:
                                        minx,miny = line.replace(')','').split('(')[1].replace(' ','').split(',')
                                if "Upper Right" in line:
                                        maxx,maxy = line.replace(')','').split('(')[1].replace(' ','').split(',')
			 	if "Computed Min/Max" in line:
                                        min,max = line.strip().split('Min/Max=')[1].split(',')

			sql = "INSERT INTO layers (name,type,source,path,enabled,displayname,displaypath,minlatitude,minlongitude,maxlatitude,maxlongitude,EnvironmentalValueMin,EnvironmentalValueMax) values('" + layername +"','Environmental','"+edlconfig.source+"','"+os.path.join(root,name)+"',true,'"+layername+"','http://spatial.ala.org.au/geoserver/gwc/service/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:"+layername+"&format=image/png&styles=','"+miny+"','"+minx+"','"+maxy+"','"+maxx+"','"+min+"','"+max+"');"
#			sql = " UPDATE layers SET name='"+ layername + "', source='" + edlconfig.source + "', path='" + os.path.join(root,name) + "', displaypath='http://spatial.ala.org.au/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:"+layername+"&format=image/png&styles=',  minlatitude='" + miny + "', minlongitude ='" + minx + "', maxlatitude ='" + maxy + "', maxlongitude='" + maxx + "', EnvironmentalValueMin ='" + min +"', EnvironmentalValueMax ='" + max + "', WHERE name='" + layername + "';"
			print(sql)


