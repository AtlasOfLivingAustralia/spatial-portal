#!/usr/bin/python
import os
import shutil
import edlconfig

for root, dirs, files in os.walk(edlconfig.dataset):
	for name in files:
		if (".tif" in name) and (name not in ['landuse.tif','landcover.tif','vast.tif','tenure08.tif','present_veg.tif','aria.tif']):
			layername = edlconfig.source + "_" + name.replace(".tif","")
			print(layername)	
			p = os.popen(edlconfig.gdalapps+"/gdalinfo -mm " + os.path.join(root,name),"r")
			sld = layername+".sld"
			f = open("template.sld", 'r')
                        text = f.read()
			nodata = None
			while 1:
  	 	 		line = p.readline()
    				if not line: break
				if "Computed" in line:
		    			print line
					min,max = line.strip().split('Min/Max=')[1].split(',')
					text = text.replace("MIN",min)
					text = text.replace("MAX",max)
					min = float(min)
					max = float(max)
				
					d = (float(max) - float(min))/10
					print(d)
					text = text.replace("TEN",str(min+d))
					text = text.replace("TWENTY",str(min+2*d))
					text = text.replace("THIRTY",str(min+3*d))
					text = text.replace("FOURTY",str(min+4*d))
					text = text.replace("FIFTY",str(min+5*d))
				 	text = text.replace("SIXTY",str(min+6*d))
                                        text = text.replace("SEVENTY",str(min+7*d))
                                        text = text.replace("EIGHTY",str(min+8*d))
                                        text = text.replace("NINETY",str(min+9*d))
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
			os.system(curlstring)
			curlstring = "curl -u " + edlconfig.geoserver_userpass + " -XPUT -H 'Content-type: application/vnd.ogc.sld+xml' -d @"+sld+" " + edlconfig.geoserver_url + "/geoserver/rest/styles/"+layername+"_style"
			print(curlstring)
			os.system(curlstring)
	
			curlstring="curl -u " + edlconfig.geoserver_userpass + " -XPUT -H 'Content-type: text/xml' -d '<layer><defaultStyle><name>"+layername+"_style</name></defaultStyle><enabled>true</enabled></layer>' " + edlconfig.geoserver_url + "/geoserver/rest/layers/ALA:"+layername
			print(curlstring)
			os.system(curlstring)
					
						
