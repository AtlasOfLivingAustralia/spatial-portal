#!/usr/bin/python

import os
import shutil

f = open("layers.txt", 'r')
while 1:
	line = f.readline()
    	if not line: break
	curlstring = "curl \"" + line.strip() + "&bbox=108.5378844,-51.13711560000001,158.2461156,-1.4288843999999963&width=511&height=512&srs=EPSG:4326&format=image/jpeg\" > " + line[line.find("ALA"):line.find("&format")] + ".jpeg"

	print(curlstring)
	os.system(curlstring)

