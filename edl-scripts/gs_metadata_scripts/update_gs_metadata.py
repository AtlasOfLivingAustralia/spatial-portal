#!/Library/Frameworks/Python.framework/Versions/2.7/bin/python
# -*- coding: utf-8 -*-

# Filename: update_gs_metadata.py
# Author: Gavin Jackson gavin.jackson@gmail.com
# Purpose: This script synchronises the Geoserver layer metadata with fields from the layers table
# Configuration: You will need to change the database connection params and the geoserver server, username and password
# Dependencies: This application requires Python 2.7 and the following python modules
#   psycopg2 - python postgres module
#   gsconfig - geoserver REST client

import psycopg2
import logging
from geoserver.catalog import Catalog

#Set up the logger (change logging.INFO to logging.DEBUG to view extended debug messages)
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger();
logger.debug("started update_gs_metadata.py");

#Get reference to geoserver layers on dev

### CHANGE ME ###
gs_server = "http://spatial-dev.ala.org.au/geoserver/rest"
gs_username = "admin"
gs_password = "geoserver"

cat = Catalog(gs_server, gs_username, gs_password)
gs_resources = cat.get_resources()

### CHANGE ME ###
db_server = "localhost"
db_name = "spatialdb"
db_port = "2344"
db_user = "postgres"

conn = psycopg2.connect("dbname=" + db_name + " host=" + db_server + " port=" + db_port + " user=" + db_user)
cur = conn.cursor()

#lets take a look at each layer in geoserver
for rs in gs_resources:
	cur.execute("SELECT * FROM layers where name = '" + rs.name + "'");
	if (cur.rowcount == 0):
		logger.debug("No layer table entry for %s", rs.name);
	elif (cur.rowcount == 1):

		# Field key:
		#  1 - name
		#  2 - description
		#  11 - notes
		#  32 - metadata link
		#  33 - keywords 

		row = cur.fetchone()

		name = row[1]
		description = row[2]
		notes = ''
		if row[11] != None:
			notes = row[11]
		md_link = ''
		if row[32] != None:
			md_link = row[32]
		keywords = ''
		if row[33] != None:
			keywords = row[33]
		
		logger.debug("About to change layer " + name);

		rs_title = ''
		if rs.title != None:
			rs_title = rs.title
		rs_abstract = ''
		if rs.abstract != None:
			rs_abstract = rs.abstract
		rs_keywords = []
		if rs.keywords != None:
			rs_keywords = rs.keywords
		rs_metadata_links = []
		if rs.metadata_links != None:
			rs_metadata_links = rs.metadata_links
		
		logger.debug("\tAbout to change title from " + rs_title + " to " + description)
		my_title = description.replace("�C", "degrees celcius").replace("—","-").replace("\xB0","degrees celcius").replace("\xE2","-").replace("\x80","").replace("\x93","").replace("\xEF","").replace("\xBF","").replace("\xBD","").replace("\xC3","").replace("\xA2","").replace("\x82","").replace("\xAC","").replace("\x84","").replace("\x98","\"").replace("\x99","\"")

		rs.title = my_title

		logger.debug("\tAbout to change abstract from " + rs_abstract + " to " + notes)
		my_abstract = notes.replace("�C", "degrees celcius").replace("—","-").replace("\xB0","degrees celcius").replace("\xE2","-").replace("\x80","").replace("\x93","").replace("\xEF","").replace("\xBF","").replace("\xBD","").replace("\xC3","").replace("\xA2","").replace("\x82","").replace("\xAC","").replace("\x84","").replace("\x98","").replace("\x99","\"")
		rs.abstract = my_abstract
		
		logger.debug("\tAbout to change keywords from " + str(rs_keywords) + " to " + str(keywords.split(", ")));
		rs.keywords = keywords.split(", ")
		logger.debug("\tAbout to change metadata links from " + str(rs_metadata_links) + " to " + str([('text/plain','other',md_link)]))
		rs.metadata_links = [('text/plain','other',md_link)]
		try:
			cat.save(rs)
		except:
			logger.error("An unexpected error occurred updating layer " + name)
			logger.error("\tDescription is " + my_title)
			logger.error("\tNotes is " + my_abstract)
			logger.error("Keywords are " + keywords)
			logger.error("Metadata link is " + md_link)
			raise
		else:
			logger.info("Updated " + name)

logger.debug("finished: update_gs_metadata.py");
