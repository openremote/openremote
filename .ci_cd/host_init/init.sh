#!/bin/bash

# ---------------------------------------------------------------------------------------------------------------------
# Host init script that runs between stack down/up and can be configured with environment variables
#
# CLEAN_INSTALL - Run clean.sh if set to 'true'
# DAILY_RESTART - Creates daily 5AM cronjob to run restart.sh (set to 'true' to enable)
# DAILY_BACKUP - Create daily 4AM cronjob to run backup.sh (set to 'false' to disable)
#
# This file and referenced scripts can be modified here or in a custom project repo or an alternative init script can be
# specified using the HOST_INIT_SCRIPT environment variable.
# ---------------------------------------------------------------------------------------------------------------------

if [[ $BASH_SOURCE = */* ]]; then
 DIR=${BASH_SOURCE%/*}/
else
  DIR=./
fi

if [ -n "$SUDO_USER" ]; then
  HOME_DIR=$(eval echo "~$SUDO_USER")
else
  HOME_DIR=~
fi

# Clean deployment persistent files if requested
if [ "$CLEAN_INSTALL" == 'true' ]; then
  source "${DIR}clean.sh"
fi

# Optionally create CRON JOB to restart manager daily at 05:00
if [ "$DAILY_RESTART" == 'true' ]; then
  echo "Adding daily manager restart cron job"

  echo '#!/bin/bash' > /etc/cron.d/or-restart
  echo "0 5 * * * root $HOME_DIR/temp/host_init/restart.sh" >> /etc/cron.d/or-restart
else
  echo "Removing any existing daily restart cron job"
  rm -f /etc/cron.d/or-restart &>/dev/null
fi

if [ "$DAILY_BACKUP" != 'false' ]; then
  echo "Adding daily backup cron job"

  echo '#!/bin/bash' > /etc/cron.d/or-backup
  echo "0 4 * * * root $HOME_DIR/temp/host_init/backup.sh" >> /etc/cron.d/or-backup
else
  echo "Removing any existing daily backup cron job"
  rm -f /etc/cron.d/or-backup &>/dev/null
fi
