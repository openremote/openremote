let
  pkgs = import <nixpkgs> { };
  unstable = import (fetchTarball "https://nixos.org/channels/nixos-unstable/nixexprs.tar.xz") { };
in
pkgs.mkShell {
  nativeBuildInputs = [
    # pkgs.playwright-driver
    unstable.playwright-driver
  ];

  shellHook = ''
    export PLAYWRIGHT_BROWSERS_PATH=${unstable.playwright-driver.browsers}
    export PLAYWRIGHT_SKIP_VALIDATE_HOST_REQUIREMENTS=true
    export PLAYWRIGHT_HOST_PLATFORM_OVERRIDE="ubuntu-24.04"
    export WEBSERVER_LISTEN_HOST=0.0.0.0
    export IDENTITY_NETWORK_WEBSERVER_PORT=443
    export IDENTITY_NETWORK_SECURE=true
  '';
}
