#!/bin/bash
#
# Create CRON JOB to restart manager daily at 05:00
#
rm /etc/cron.d/openremote
username=$(id -u -n)
if [ ! -z "$SUDO_USER" ]; then
  username=$SUDO_USER
fi
echo '#!/bin/bash' >> /etc/cron.d/openremote
echo "0 5 * * * $username docker restart or_manager_1" >> /etc/cron.d/openremote
