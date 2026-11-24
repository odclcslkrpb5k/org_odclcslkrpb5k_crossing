# Crossing (working name)

An original lane-crossing game for a child's tablet: hop a unicorn foal over roads of hay carts and
streams of logs to the rainbow. No game over, no timer, no ads, no purchases, no network. A bonk
sends her back to the last grass row; stars stay collected.

Play: tap anywhere to hop forward; swipe left, right or down to hop that way.

## Build and check

Needs Docker only.

    docker compose run --build --rm check

runs every check offline: format, lint (detekt and Android Lint), unit tests, licences, a release
build and its merged manifest. Other services: `plants` (each check's planted bug), `vulns`
(known vulnerabilities; needs a network), `ship-tests`, and `tools/emulator <scenario>` (a
headless Android 16 emulator; needs /dev/kvm).

## Play it on this computer

    tools/play            # the tablet as it was left: stars, characters and both apps kept
    tools/play --clean    # a tablet fresh from the box

An emulated tablet shaped like hers (1920x1200 landscape, Android 16) opens in a window on this
X desktop, scaled to fit the screen, with the game built and installed: the mouse is her finger
(click to hop, drag to swipe). It needs Docker, /dev/kvm and an X display; it changes nothing on
this machine. Its state lives in ~/.local/share/odclcslkrpb5k-android/tablet, shared with the
drawing app's tools/play.

## Configure

Nothing reads the environment. Device: Samsung Galaxy Tab A11+ (Android 16, 1920×1200, landscape).

## Install and operate

The release key is made once by a person with `tools/release/make-key`, kept outside every
repository and backed up offline in two places: a lost key means uninstalling the game, and her
progress, to update it. The same key signs the drawing app; `tools/release/sign` and `install`
build, sign and install with it. Installation steps follow the install rehearsal on the tablet.

Parent setup on the tablet: the first time the app opens full screen, Android shows a one-time
"Viewing full screen" notice; dismiss it with "Got it".
