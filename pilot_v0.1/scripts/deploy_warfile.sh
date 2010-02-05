#!/bin/bash -x

# SET BEFORE CALLING:
#  BACKUP_DIR - directory to store backups
#  WARFILE_BASENAME - name of tomcat context
#  TOMCAT_PATH - full path to tomcat instance
#  SERVER - server to ssh into 
#
# Script expects:
#  JDK installed under /usr/local/java
#  SSH installed and in path (best if ssh agent and public/private keys used)
#  tar command available on target system

echo -e "********* WARFILE DEPLOYMENT FEATURING TOMCAT BOUNCE *********"
echo -e "\tbackups - $BACKUP_DIR"
echo -e "\tcontext - $WARFILE_BASENAME"
echo -e "\tpath to tomcat install -  $TOMCAT_PATH"
echo -e "\tserver - $SERVER"
echo -e "\n"
echo -e "last chance to quit!"
read CONT

ISODATE=`date +"%Y-%m-%d"`
SSH_COMMAND="ssh -t"
JAR_COMMAND="/usr/local/java/bin/jar"

BACKUP_FILE=$BACKUP_DIR"/"$WARFILE_BASENAME"_"$ISODATE".tar.gz"
DEPLOYMENT_DIRECTORY=$TOMCAT_PATH"/webapps/"
TARGET_DIRECTORY=$DEPLOYMENT_DIRECTORY/$WARFILE_BASENAME
NEW_WARFILE="$WARFILE_BASENAME.war"
NEW_WARFILE_DIR="target/"

if [ -e $NEW_WARFILE_DIR/$NEW_WARFILE ] ; then
	# copy to server
	scp $NEW_WARFILE_DIR/$NEW_WARFILE $SERVER:

	# take backup
	$SSH_COMMAND $SERVER "cd $DEPLOYMENT_DIRECTORY && if [ -d $WARFILE_BASENAME ] ; then tar zcvf ~/$BACKUP_FILE $WARFILE_BASENAME ; else echo context not found - no backup will be created ; fi"

	echo "$NEW_WARFILE copied to server, if context existed, backup was saved to $BACKUP_FILE"

	echo -e "\n\n"
	echo "about to shutdown tomcat and redeploy - ctrl+c now to abort!"
	read CONT

	# tomcat down (all of em!)
	$SSH_COMMAND  $SERVER "sudo /sbin/service tomcat stop"
	
	# wait for tomcat to be brought down
	sleep 60

	# delete old deployment dir
	$SSH_COMMAND $SERVER "sudo rm -rf $TARGET_DIRECTORY"

	# extract WAR file
	$SSH_COMMAND $SERVER "cd $DEPLOYMENT_DIRECTORY && mkdir $WARFILE_BASENAME &&  cd $WARFILE_BASENAME && $JAR_COMMAND xvf ~/$NEW_WARFILE"

	# All done, startup tomcat...
	$SSH_COMMAND $SERVER "sudo /sbin/service tomcat start"
	
else
	echo "file $NEW_WARFILE not found in current directory - aborting"
fi
