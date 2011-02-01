#!/usr/bin/python
import os
import shutil
import edlconfig
import math

unitFile  = open("units.csv")
#unitLines = unitFile.read().split("\n")
unitDict = {}
for line in unitFile.readlines():
	print(line)
	name,unit =  line.split(';')
	unitDict[name.replace('"','')] = unit.replace('"','').replace('\n','')

for root, dirs, files in os.walk(edlconfig.dataset):
	for name in files:
		if (".tif" in name) and (name not in ['landuse.tif','landcover.tif','vast.tif','tenure08.tif','present_veg.tif','aria.tif']):
#			layername = edlconfig.source + "_" + name.replace(".tif","")
			layername = name.replace(".tif","")
			print(layername)	
			p = os.popen(edlconfig.gdalapps+"/gdalinfo -mm " + os.path.join(root,name),"r")
			sld = layername+".sld"

					
			f = open("template.sld", 'r')
                        text = f.read()
			nodata = None
	
			unit = ""
			if (unitDict.has_key(layername)):	
				unit = unitDict[layername]	
			
			while 1:
  	 	 		line = p.readline()
    				if not line: break
				if "Computed" in line:
		    			print line
					min,max = line.strip().split('Min/Max=')[1].split(',')
					if ((float(max) - float(min)) > 10):
						min = math.floor(float(min)/10)*10
						max = math.ceil(float(max)/10)*10
					else:
						min = float(min)
						max = float(max)

					text = text.replace("MIN_QUANTITY",str(min))
					text = text.replace("MIN_LABEL",str(min) + " " + unit)
					text = text.replace("MAX_QUANTITY",str(max))
					text = text.replace("MAX_LABEL",str(max) + " " + unit)

					d = (float(max) - float(min))/10
					print(d)
					for i in range(1,10):
						text = text.replace(str(i*10) + "_QUANTITY",str(min+i*d))
						text = text.replace(str(i*10) + "_LABEL",str(min+i*d) + " " + unit)
				if "NoData" in line:
					print line
					nodata = line.strip().split('Value=')[1]
			if (nodata is None):
				nodata=min
			#Getting round an sld parsing bug in geoserver
			if float(nodata) >= float(max):
				text = text.replace("<!--Higher-->",'<ColorMapEntry color="#ffffff" quantity="NODATA" opacity="0"/>')
			else:	
				text = text.replace("<!--Lower-->",'<ColorMapEntry color="#ffffff" quantity="NODATA" opacity="0"/>')
			text = text.replace("NODATA",str(nodata))
			fout = open(sld, 'w')
                        fout.write(text)
			fout.close()
				

			curlstring = "curl -u " + edlconfig.geoserver_userpass + " -XPOST -H 'Content-type: text/xml' -d '<style><name>"+layername+"_style</name><filename>"+layername+".sld</filename></style>' " + edlconfig.geoserver_url + "/geoserver/rest/styles/"
			print(curlstring)
#			os.system(curlstring)
			curlstring = "curl -u " + edlconfig.geoserver_userpass + " -XPUT -H 'Content-type: application/vnd.ogc.sld+xml' -d @"+sld+" " + edlconfig.geoserver_url + "/geoserver/rest/styles/"+layername+"_style"
			print(curlstring)
#			os.system(curlstring)
	
			curlstring="curl -u " + edlconfig.geoserver_userpass + " -XPUT -H 'Content-type: text/xml' -d '<layer><defaultStyle><name>"+layername+"_style</name></defaultStyle><enabled>true</enabled></layer>' " + edlconfig.geoserver_url + "/geoserver/rest/layers/ALA:"+layername
			print(curlstring)
#			os.system(curlstring)
					
						
