# Gateway Network Throttling Script

A bash utility for simulating degraded network conditions between an OpenRemote gateway and a central instance. Useful 
for testing gateway resilience, reconnection logic, and behavior under poor network conditions.

The reasoning for the creation of this script is to allow developers and testers to simulate real-world network issues 
such as latency, packet loss, and bandwidth limitations in a controlled manner. This applies for gateways that
transfer large amounts of data to and from the central instance, but especially used for testing the gateway tunneling 
functionality, where large amounts of data is transferred (especially the frontend JS bundle), and even minor issues with
connectivity can cause issues with interacting with the remote gateway.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            GATEWAY HOST                                     │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    or-manager-1 container                           │   │
│  │                                                                     │   │
│  │   ┌─────────────┐      ┌──────────────────────────────────────┐    │   │
│  │   │   Gateway   │      │           eth0 interface             │    │   │
│  │   │   Manager   │─────▶│  ┌─────────────────────────────────┐ │    │   │
│  │   │   Process   │      │  │     Traffic Control (tc)        │ │    │   │
│  │   └─────────────┘      │  │  ┌────────────────────────────┐ │ │    │   │
│  │                        │  │  │  netem qdisc (egress)      │ │ │    │   │
│  │                        │  │  │  • delay: 75ms ± 25ms      │ │ │    │   │
│  │                        │  │  │  • loss: 1%                │ │ │    │   │
│  │                        │  │  │  • rate: 10mbit            │ │ │    │   │
│  │                        │  │  └────────────────────────────┘ │ │    │   │
│  │                        │  └─────────────────────────────────┘ │    │   │
│  │                        │  ┌─────────────────────────────────┐ │    │   │
│  │                        │  │  iptables (ingress)             │ │    │   │
│  │                        │  │  • 1% random packet drop        │ │    │   │
│  │                        │  │  • or full DROP (blackhole)     │ │    │   │
│  │                        │  └─────────────────────────────────┘ │    │   │
│  │                        └──────────────────────────────────────┘    │   │
│  │                                         │                          │   │
│  │  (shared network namespace)             │                          │   │
│  │           ▲                             │                          │   │
│  │           │                             │                          │   │
│  │  ┌────────┴────────┐                    │                          │   │
│  │  │ pumba-nettools  │                    │                          │   │
│  │  │  (temporary)    │                    │                          │   │
│  │  │  Applies tc &   │                    │                          │   │
│  │  │  iptables rules │                    │                          │   │
│  │  └─────────────────┘                    │                          │   │
│  └─────────────────────────────────────────┼──────────────────────────┘   │
│                                            │                               │
└────────────────────────────────────────────┼───────────────────────────────┘
                                             │
                              ~~~~~~~~~~~~~~~│~~~~~~~~~~~~~~~
                                   Internet  │  (PUBLIC)
                              ~~~~~~~~~~~~~~~│~~~~~~~~~~~~~~~
                                             │
                                             ▼
                              ┌──────────────────────────────┐
                              │      CENTRAL INSTANCE        │
                              │   (e.g., demo.openremote.app)│
                              │                              │
                              │   ┌──────────────────────┐   │
                              │   │   Central Manager    │   │
                              │   └──────────────────────┘   │
                              └──────────────────────────────┘
```

### Throttle Mode (`on`)

```
Gateway                                                    Central
   │                                                          │
   │ ──────── outbound packet ────────▶                       │
   │          [+75ms delay, ±25ms jitter, 1% loss, 10mbit]    │
   │                                                          │
   │                       ◀──────── inbound packet ───────── │
   │                                 [1% random drop]         │
   │                                                          │
```

### Drop Mode (`drop`)

```
Gateway                                                    Central
   │                                                          │
   │ ────── outbound packet ──────▶ ╳ BLOCKED                 │
   │                                                          │
   │                    ╳ BLOCKED ◀────── inbound packet ──── │
   │                                                          │
   │            (complete blackhole for N seconds)            │
```

## Prerequisites

- Docker installed and running
- The `or-manager-1` container must be running
- The script uses [pumba-alpine-nettools](https://github.com/alexei-led/pumba) for network manipulation

## Configuration

Edit these variables at the top of `throttle.sh`:

| Variable | Default | Description |
|----------|---------|-------------|
| `CONTAINER` | `or-manager-1` | Target Docker container |
| `TARGET_IP` | (empty) | Public IP of the central instance (e.g., resolved IP of `demo.openremote.app`) |
| `DELAY` | `75ms` | Network latency added to packets |
| `JITTER` | `25ms` | Variation in latency |
| `LOSS` | `1%` | Percentage of packets to drop |
| `RATE` | `10mbit` | Bandwidth limit |

**Important:** You must set `TARGET_IP` before using the script. Use `dig` to resolve the hostname:
```bash
TARGET_IP=$(dig +short demo.openremote.app | head -1)
```

## Usage

```bash
./throttle.sh [command]
```

### Commands

| Command | Description |
|---------|-------------|
| `on` | Enable throttling with configured delay, jitter, loss, and rate limit |
| `off` | Disable all throttling and restore normal connectivity |
| `toggle` | Toggle throttling on/off (default if no command given) |
| `drop [seconds]` | Completely block connectivity for specified duration (default: 5s) |
| `status` | Show current throttling status |

### Examples

```bash
# Enable throttling
./throttle.sh on

# Check status
./throttle.sh status

# Simulate connection loss for 10 seconds
./throttle.sh drop 10

# Disable throttling
./throttle.sh off

# Toggle (on if off, off if on)
./throttle.sh
```

## How It Works

The script manipulates network traffic using two mechanisms:

### Egress Throttling (tc/netem)
Uses Linux Traffic Control (`tc`) with the `netem` qdisc to:
- Add configurable latency and jitter
- Simulate packet loss
- Limit bandwidth

Traffic shaping is applied only to packets destined for `TARGET_IP`.

### Connection Dropping (iptables)
Uses `iptables` rules to:
- Drop incoming packets from `TARGET_IP` (for throttle mode: 1% random drop)
- Completely block bidirectional traffic (for drop mode)

### Docker Integration
The script runs a temporary container (`pumba-alpine-nettools`) that shares the network namespace with `or-manager-1` (`--net=container:$CONTAINER`). This allows modifying the network stack of the manager container without installing tools inside it.

## Troubleshooting

**"Permission denied" errors:**
Ensure the script is executable: `chmod +x throttle.sh`

**No effect observed:**
- Verify `TARGET_IP` is set correctly
- Check that `or-manager-1` container is running
- Run `./throttle.sh status` to verify rules are applied

**Cannot pull Docker image:**
The script requires access to `ghcr.io`. Ensure network/firewall allows this.
