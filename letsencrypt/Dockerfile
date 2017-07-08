# Adapted from https://github.com/bringnow/docker-letsencrypt-manager
FROM centos:7

RUN yum update -y && yum -y install epel-release && yum -y install certbot cronie && yum clean all

ENV TERM xterm
ENV WEBROOT "/webroot"

VOLUME ["/etc/letsencrypt", "/webroot", "/var/acme-webroot"]

COPY cli.ini /root/.config/letsencrypt/

COPY entrypoint.sh /
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
CMD ["help"]
