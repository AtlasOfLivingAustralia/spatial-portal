#!/bin/bash 

SCRIPT_DIR=`dirname $0`
BACKUP_DIR="backup"
WARFILE_BASENAME="webportal"
TOMCAT_PATH="/usr/local/tomcat/instance_03_webportal/"
SERVER="obsidian"

. $SCRIPT_DIR/deploy_warfile.sh
