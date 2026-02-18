#!/bin/bash
CONTAINER="or-manager-1"
TC_IMAGE="ghcr.io/alexei-led/pumba-alpine-nettools:latest"
TARGET_IP="" # Set the public IP of the central instance here, e.g., demo.openremote.app . I use dig for this.
DELAY="75ms"
JITTER="25ms"
LOSS="1%"
RATE="10mbit"

run_tc() {
  docker run --rm --net=container:$CONTAINER --cap-add NET_ADMIN --entrypoint /bin/sh $TC_IMAGE -c "$1"
}

is_active() {
  run_tc "tc qdisc show dev eth0" 2>/dev/null | grep -q "prio"
}

is_dropped() {
  run_tc "iptables -L INPUT -n" 2>/dev/null | grep -q "$TARGET_IP.*DROP"
}

start_throttle() {
  run_tc "
    # Egress throttling (delay + loss + rate)
    tc qdisc add dev eth0 root handle 1: prio
    tc qdisc add dev eth0 parent 1:3 handle 30: netem delay $DELAY $JITTER loss $LOSS rate $RATE
    tc filter add dev eth0 protocol ip parent 1:0 prio 3 u32 match ip dst $TARGET_IP/32 flowid 1:3
    # Ingress packet loss (iptables)
    iptables -A INPUT -s $TARGET_IP -m statistic --mode random --probability 0.01 -j DROP
  "
}

stop_throttle() {
  run_tc "
    tc qdisc del dev eth0 root
    while iptables -D INPUT -s $TARGET_IP -m statistic --mode random --probability 0.01 -j DROP 2>/dev/null; do :; done
  " 2>/dev/null
}

start_drop() {
  run_tc "
    iptables -A INPUT -s $TARGET_IP -j DROP
    iptables -A OUTPUT -d $TARGET_IP -j DROP
  "
}

stop_drop() {
  run_tc "
    while iptables -D INPUT -s $TARGET_IP -j DROP 2>/dev/null; do :; done
    while iptables -D OUTPUT -d $TARGET_IP -j DROP 2>/dev/null; do :; done
  "
}

case "${1:-toggle}" in
  on)
    stop_throttle
    stop_drop
    start_throttle
    echo "Throttling ON for $TARGET_IP"
    ;;
  off)
    stop_throttle
    stop_drop
    echo "Throttling OFF"
    ;;
  toggle)
    if is_active; then
      stop_throttle
      echo "Throttling OFF"
    else
      start_throttle
      echo "Throttling ON for $TARGET_IP"
    fi
    ;;
  drop)
    DURATION="${2:-5}"
    start_drop
    echo "Connection DROPPED for ${DURATION}s..."
    sleep "$DURATION"
    stop_drop
    echo "Connection RESTORED"
    ;;
  status)
    echo "=== Status for $TARGET_IP ==="
    if is_dropped; then
      echo "Connection: DROPPED (blackhole)"
    elif is_active; then
      echo "Connection: THROTTLED"
      run_tc "tc qdisc show dev eth0"
    else
      echo "Connection: NORMAL (no throttling)"
    fi
    ;;
  *)
    echo "Usage: $0 [on|off|toggle|drop [seconds]|status]"
    exit 1
    ;;
esac
