#!/bin/sh
exec java ${OR_JAVA_OPTS} -cp "/opt/app/lib/*:/deployment/manager/extensions/*" org.openremote.manager.Main
