#!/bin/bash
# -----------------------------------------------------------------------------------------------------
# This is called daily by a cron job if DAILY_RESTART=true
# By default it just restarts the manager service
# -----------------------------------------------------------------------------------------------------
echo "Restarting the manager service"
docker restart or-manager-1
