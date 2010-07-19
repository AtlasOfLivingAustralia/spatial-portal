#!/usr/bin/python26
import os
import shutil
import edlconfig

#for root, dirs, files in os.walk("/mnt/transfer/MCAS_1k_Datapack/Climate/erosivity.tif"):
for root, dirs, files in os.walk(edlconfig.dataset):
	for name in files:
		if ".tif" in name:
			layername = name.replace(".tif","")
			os.system("gdal_translate -of EHdr " + os.path.join(root,name) + " " + layername + ".bil")
			shutil.move(layername+".bil",os.path.join(edlconfig.dataset,"Diva",layername+".gri"))
			os.system("gdalinfo -mm " + os.path.join(root,name))
			p = os.popen("gdalinfo -mm " + os.path.join(root,name),"r")
                      	fout = open(os.path.join(edlconfig.dataset,"Diva",layername + ".grd"),'w')
			text = open("GRDTEMPLATE",'r').read() 
			while 1:
                                line = p.readline()
                                if not line: break
				if "Size is" in line:
					columns,rows = line.split('is ')[1].split(", ")
					text = text.replace("COLUMNS",columns)
					text = text.replace("ROWS",rows)
				if "Computed" in line:
                               		min,max = line.strip().split('=')[1].split(',')
                                	text = text.replace("MINVALUE",min)
                                	text = text.replace("MAXVALUE",max)
                		if "NoData" in line:
					nodata = line.strip().split('=')[1]
					text = text.replace("NODATAVALUE",nodata)
				if "Band" in line:
					datatype = line.split('Type=')[1].split(',')[0]
					text = text.replace("DATATYPE",datatype)
				if "Lower Left" in line:
					minx,miny = line.replace(')','').split('(')[1].replace(' ','').split(',')
					text = text.replace("MINX",minx)
					text = text.replace("MINY",miny)
				if "Upper Right" in line:
                                        maxx,maxy = line.replace(')','').split('(')[1].replace(' ','').split(',')
                                        text = text.replace("MAXX",maxx)
                                        text = text.replace("MAXY",maxy)

				text = text.replace("RESOLUTIONX","1")
				text = text.replace("RESOLUTIONY","1")
				text = text.replace("TITLE",layername)
                        fout.write(text)


