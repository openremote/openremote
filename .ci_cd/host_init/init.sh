#!/bin/bash
#
# Create CRON JOB to restart manager daily at 05:00
#
rm /etc/cron.d/openremote
echo "0 5 * * * docker restart or_manager_1" >> /etc/cron.d/openremote
