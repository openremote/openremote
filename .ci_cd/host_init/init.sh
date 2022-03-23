#!/bin/bash

if [[ $BASH_SOURCE = */* ]]; then
 DIR=${BASH_SOURCE%/*}/
else
  DIR=./
fi

# Remove existing init files
rm /etc/cron.d/or-manager-restart &>/dev/null

# Remove any existing map data EFS mount
if [ -d "/deployment.local/map" ]; then
  echo "Removing any existing EFS map mount"
  umount /deployment.local/map
  sed -i.bak '\@.amazonaws.com:/ /deployment.local/map@d' /etc/fstab
fi


# Clean deployment persistent files if requested
if [ "$CLEAN_INSTALL" == 'true' ]; then
  source "${DIR}clean.sh"
fi

# Optionally create CRON JOB to restart manager daily at 05:00
if [ "$DAILY_RESTART" == 'true' ]; then
  echo "Adding daily manager restart cron job"

  username=$(id -u -n)
  if [ -n "$SUDO_USER" ]; then
    username=$SUDO_USER
  fi
  echo '#!/bin/bash' >> /etc/cron.d/or-manager-restart
  echo "0 5 * * * $username docker restart or_manager_1" >> /etc/cron.d/or-manager-restart
fi


# Optionally mount EFS map data
if [ -n "$MAP_MOUNT" ]; then

fi
