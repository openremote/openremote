# Copyright (c) 2014, Docker PostGIS Authors (See AUTHORS)

# Permission is hereby granted, free of charge, to any person
# obtaining a copy of this software and associated documentation
# files (the "Software"), to deal in the Software without
# restriction, including without limitation the rights to use,
# copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the
# Software is furnished to do so, subject to the following
# conditions:

# The above copyright notice and this permission notice shall be
# included in all copies or substantial portions of the Software.

FROM postgres:9.6.1

ENV POSTGIS_MAJOR 2.3
# TODO This is frequently broken by someone upstream who doesn't know what they are doing
# TODO To get the latest 'correct' version, look here: https://github.com/appropriate/docker-postgis/blob/master/9.6-2.3/Dockerfile
ENV POSTGIS_VERSION 2.3.1+dfsg-1.pgdg80+1

RUN apt-get update \
      && apt-get install -y --no-install-recommends \
           postgresql-$PG_MAJOR-postgis-$POSTGIS_MAJOR=$POSTGIS_VERSION \
           postgresql-$PG_MAJOR-postgis-$POSTGIS_MAJOR-scripts=$POSTGIS_VERSION \
           postgis=$POSTGIS_VERSION \
      && rm -rf /var/lib/apt/lists/*

ENV POSTGRES_USER postgres

ADD scripts /docker-entrypoint-initdb.d/
RUN chmod +x /docker-entrypoint-initdb.d/*.sh