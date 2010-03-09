#!/bin/bash 

SCRIPT_DIR=`dirname $0`
BACKUP_DIR="backup"
WARFILE_BASENAME="webportal"
TOMCAT_PATH="/usr/local/tomcat/instance_02_webportal"
SERVER="imos.aodn.org.au"

. $SCRIPT_DIR/deploy_warfile.sh
