#!/bin/bash 

SCRIPT_DIR=`dirname $0`
BACKUP_DIR="backup"
WARFILE_BASENAME="webportal"
TOMCAT_PATH="/usr/local/tomcat/instance_00_webportal"
SERVER="imos1.ersa.edu.au"

. $SCRIPT_DIR/deploy_warfile.sh
