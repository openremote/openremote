let
  pkgs = import <nixpkgs> { };
in
# unstable = import (fetchTarball "https://nixos.org/channels/nixos-unstable/nixexprs.tar.xz") { };
pkgs.mkShell {
  nativeBuildInputs = with pkgs; [
    playwright-driver
  ];

  shellHook = ''
    export PLAYWRIGHT_BROWSERS_PATH=${pkgs.playwright-driver.browsers}
    export PLAYWRIGHT_SKIP_VALIDATE_HOST_REQUIREMENTS=true
  '';
}
