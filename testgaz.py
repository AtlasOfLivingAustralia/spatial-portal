#!/usr/bin/python

import os
import shutil
import time
import threading
	
curlbase = "curl -GET http://localhost:8080/geoserver/rest/gazetteer"
curlbase = "curl -GET http://localhost:3129/geoserver/rest/gazetteer"

n = 64
threads = 64
times = []

class PointSearch (threading.Thread):
	#Test point search 
	def run (self):
		print("blah")
		layer = curlbase + "/aus1/latlon/-36.12345,145.12345"
		for i in range(n/threads):
			print(layer)
			t1 = time.time()
			os.system(layer)
			t2 = time.time()
			t = t2 - t1
			times.append(t)
			print("time:" + str(t))

t1 = time.time()
for t in range(threads):
	PointSearch().start()	

while (len(times) < n):
	pass
t2 = time.time()
t = t2 - t1
print("AVERAGE REQUEST: " + str(t/n) + " seconds")
#	print(len(times))
#Test point search
"""point = curlbase + "/result.xml?point=145,-36\&layer=aus2"
for i in range(n):
	print(point)
	t1 = time.time()
	os.system(point)
	t2 = time.time()
	t = t2 - t1
	times.append(t)
	print("time:" + str(t))

#Test basic search
search = curlbase + "/result.xml?q=tasmania"
for i in range(n):
	print(search)
	t1 = time.time()
	os.system(search)
	t2 = time.time()
	t = t2 - t1
	times.append(t)

	print("time:" + str(t))

#Test feature request
feature = curlbase + "/aus1/Australian_Capital_Territory.json"
for i in range(n):
	print(feature)
	t1 = time.time()
	os.system(feature)
	t2 = time.time()
	t = t2 - t1
	times.append(t)
	print("time:" + str(t))
"""

