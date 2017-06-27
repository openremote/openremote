#!/bin/bash -e

if ! [ $(getent group openremote) ]; then

    echo "Creating default user 'openremote'"
    groupadd openremote
    usermod --move-home --login openremote --home /home/openremote --gid openremote pirate
    groupdel pirate
    rm /home/openremote/.bash_prompt
    rm /etc/sudoers.d/user-pirate
    echo 'openremote ALL=NOPASSWD: ALL' > /etc/sudoers.d/user-openremote
    
    if [ -f /boot/openremote-ssh.pub ]; then
        echo "Disabling password authentication, using provided SSH public key"
        mkdir /home/openremote/.ssh
        cp /boot/openremote-ssh.pub /home/openremote/.ssh/authorized_keys
        chown openremote:openremote /home/openremote/.ssh/authorized_keys
        sed -i 's/^#PasswordAuthentication.*/PasswordAuthentication no/g' /etc/ssh/sshd_config
        systemctl restart sshd
        rm /boot/openremote-ssh.pub
    fi

    if [ -d /boot/dockerssl ]; then
        echo "Configuring Docker SSL setup and HTTP listening socket"
        sed -i 's#^ExecStart.*#ExecStart=/usr/bin/dockerd#g' /lib/systemd/system/docker.service
        cp /boot/dockerssl/* /etc/docker/
        cat >> /etc/docker/daemon.json << EOF
{
 "hosts": ["fd://", "tcp://0.0.0.0:2376", "unix:///var/run/docker.sock"],
 "max-concurrent-downloads": 10,
 "tls": true,
 "tlscacert": "/etc/docker/ca.pem",
 "tlscert": "/etc/docker/cert.pem",
 "tlskey": "/etc/docker/key.pem",
 "tlsverify": true
}
EOF
        systemctl daemon-reload
        systemctl restart docker
        rm -r /boot/dockerssl
    fi

else
    echo "OpenRemote environment already configured!"
fi
