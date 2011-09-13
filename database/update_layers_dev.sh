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

check_errs()
{
  # Function. Parameter 1 is the return code
  # Para. 2 is text to display on failure.
  if [ "${1}" -ne "0" ]; then
    echo "ERROR # ${1} : ${2}"
    # as a bonus, make our script exit with the right error code.
    exit ${1}
  fi
}

ls jdkbcjkbc
check_errs $? "echo ERROR running $!!;break"

# run layers create/update script
$PSQL_CMD -U postgres spatialdb -f ./layers_dev.sql
check_errs $? "echo ERROR running $!!"

# add updates here ...
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/001_ger_updates_12052011.sql
check_errs $? "echo ERROR running $!!"
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/002_ger_updates_19052011.sql
check_errs $? "echo ERROR running $!!"
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/003_ne_world_25052011.sql
check_errs $? "echo ERROR running $!!"
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/004_gn_links_06062011.sql
check_errs $? "echo ERROR running $!!"
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/005_env_scale_update_09062011.sql
check_errs $? "echo ERROR running $!!"
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/006_layer_metadata_update_29042011.sql
check_errs $? "echo ERROR running $!!"
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/007_dld_metadata_update_29062011.sql
check_errs $? "echo ERROR running $!!"
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/008_ger_display_name_update_30062011.sql
check_errs $? "echo ERROR running $!!"
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/009_classification_updates_010711.sql
check_errs $? "echo ERROR running $!!"
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/010_lithology_12072011.sql
check_errs $? "echo ERROR running $!!"
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/011_gbr_200711.sql
check_errs $? "echo ERROR running $!!"
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/012_gbrdem_coral_geo_brs_dld_newld_20110801.sql
check_errs $? "echo ERROR running $!!"
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/013_geohab_02082011.sql
check_errs $? "echo ERROR running $!!"
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/014_amb_02082011.sql
check_errs $? "echo ERROR running $!!"
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/015_eez_ozcoasts_20110805.sql
check_errs $? "echo ERROR running $!!"
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/016_beards_080811.sql
check_errs $? "echo ERROR running $!!"
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/017_bath_top_gbr_cat_100811.sql
check_errs $? "echo ERROR running $!!"
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/018_world_150811.sql
check_errs $? "echo ERROR running $!!"
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/019_update_keywords_190811.sql
check_errs $? "echo ERROR running $!!"
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/020_gbr_keywords_190811.sql
check_errs $? "echo ERROR running $!!"
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/021_aspa_asma_aat_290811.sql
check_errs $? "echo ERROR running $!!"
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/022_fire_enable_010911.sql
check_errs $? "echo ERROR running $!!"
$PSQL_CMD -U postgres spatialdb -f ./layers_dev_updates/023_enable_layers_010911.sql 
check_errs $? "echo ERROR running $!!"
