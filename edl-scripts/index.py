#!/usr/bin/python26

import os
import edlconfig

for root, dirs, files in os.walk(edlconfig.dataset):
	for name in files:
		if ".tif" in name:
			layername = name.replace(".tif","")
			sql = "INSERT INTO layers (name,type,source,path,enabled,displayname,displaypath,minlatitude,minlongitude,maxlatitude,maxlongitude) values('" + layername +"','Environmental','"+edlconfig.source+"','"+os.path.join(root,name)+"',true,'"+layername+"','http://spatial.ala.org.au/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:"+layername+"&format=image/png&styles=','0','0','0','0');"
			print(sql)

