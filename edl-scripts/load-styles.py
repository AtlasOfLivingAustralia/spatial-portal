#!/usr/bin/python
import os
import shutil
import edlconfig

for root, dirs, files in os.walk(edlconfig.dataset):
	for name in files:
		if ((".geotiff" in name) or (".tif" in name)):
#			layername = edlconfig.source + "_" + name.replace(".tif","").replace(".geotiff","")
			layername = name.replace(".tif","").replace(".geotiff","")
			print(layername)	
			sld = layername+".sld"

			curlstring = "curl -u " + edlconfig.geoserver_userpass + " -XPOST -H 'Content-type: text/xml' -d '<style><name>"+layername+"_style</name><filename>"+layername+".sld</filename></style>' " + edlconfig.geoserver_url + "/geoserver/rest/styles/"
			print(curlstring)
			os.system(curlstring)
			curlstring = "curl -u " + edlconfig.geoserver_userpass + " -XPUT -H 'Content-type: application/vnd.ogc.sld+xml' -d @"+sld+" " + edlconfig.geoserver_url + "/geoserver/rest/styles/"+layername+"_style"
			print(curlstring)
			os.system(curlstring)
		
			curlstring="curl -u " + edlconfig.geoserver_userpass + " -XPUT -H 'Content-type: text/xml' -d '<layer><defaultStyle><name>"+layername+"_style</name></defaultStyle><enabled>true</enabled></layer>' " + edlconfig.geoserver_url + "/geoserver/rest/layers/ALA:"+layername
			print(curlstring)
			os.system(curlstring)
					
						
