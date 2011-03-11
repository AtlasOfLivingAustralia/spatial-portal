#!/bin/bash
#Small perl script to replace references to spatial.ala.org.au to spatial-dev.ala.org.au 
perl -pi -e 's/spatial\.ala\.org\.au/spatial-dev\.ala\.org\.au/g' ./layers.sql

