#!/bin/bash
#
# This script restores the layers table and associated table sequence
# Gavin Jackson 11/2/2011

PSQL_CMD=/usr/local/pgsql/bin/psql

#allows script to work on local (OSX) postgres environment
if [ -e /Library/PostgreSQL/9.0/bin/psql ]
then
  PSQL_CMD=/Library/PostgreSQL/9.0/bin/psql
fi

# run layers create/update script
$PSQL_CMD -U postgres spatialdb -f ./layers_dev.sql

# add updates here ...
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/001_ger_updates_12052011.sql
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/002_ger_updates_19052011.sql
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/003_ne_world_25052011.sql
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/004_gn_links_06062011.sql
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/005_env_scale_update_09062011.sql
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/006_layer_metadata_update_29042011.sql
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/007_dld_metadata_update_29062011.sql
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/008_ger_display_name_update_30062011.sql
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/009_classification_updates_010711.sql
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/010_lithology_12072011.sql
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/011_gbr_200711.sql
