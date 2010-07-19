#!/usr/bin/python26
import os
import shutil
import edlconfig

for root, dirs, files in os.walk(edlconfig.dataset):
	for name in files:
		if ".tif" in name:
			layername = name.replace(".tif","")
			print(layername)	
			p = os.popen(edlconfig.gdalapps+"/gdalinfo -mm " + os.path.join(root,name) + " | grep Computed","r")
			sld = layername+".sld"
			while 1:
  	 	 		line = p.readline()
    				if not line: break
	    			min,max = line.strip().split('=')[1].split(',')
				f = open("template.sld", 'r')
				text = f.read()
				text = text.replace("MIN",min)
				text = text.replace("MAX",max)
#				text = text.replace("NODATA",nodata)
#				fout = open(sld, 'w')
#				fout.write(text)		
				curlstring = "curl -u " + edlconfig.geoserver_userpass + " -XPOST -H 'Content-type: text/xml' -d '<style><name>"+layername+"_style</name><filename>"+layername+".sld</filename></style>' " + edlconfig.geoserver_url + "/geoserver/rest/styles/"
#				print(curlstring)
#				os.system(curlstring)
				curlstring = "curl -u " + edlconfig.geoserver_userpass + " -XPUT -H 'Content-type: application/vnd.ogc.sld+xml' -d @"+sld+" " + edlconfig.geoserver_url + "/geoserver/rest/styles/"+layername+"_style"
#				print(curlstring)
				os.system(curlstring)
	
				curlstring="curl -u " + edlconfig.geoserver_userpass + " -XPUT -H 'Content-type: text/xml' -d '<layer><defaultStyle><name>"+layername+"_style</name></defaultStyle><enabled>true</enabled></layer>' " + edlconfig.geoserver_url + "/geoserver/rest/layers/ALA:"+layername
#				print(curlstring)
				os.system(curlstring)
					
						
