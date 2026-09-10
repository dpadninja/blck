<h2 align="center">
  <img height="120" src="logo.svg" />
  <br>
    turns off the screen on the TV and keeps the sound playing
</h2>

<img src="screenshot.png" />

## Use cases
- Listen to music, podcasts or radio with the screen dark
- Keep YouTube playing when you only need the sound
- Avoid OLED burn-in on static images

## Features
- Press `Back` + `OK` to black out the screen
- Black out the screen automatically after a set idle time in selected apps
- Press any button (except volume) to turn the picture back on

## Limitations
On regular LED TVs, the backlight stays on, so the screen may glow slightly.

## Requirements
Google TV or Android TV, version 11 or newer.

## Installation
- Sideload the APK for your architecture
- Open the app and grant the requested permissions

### Additional steps for TCL TVs
Open `Safety Guard` → `Permission Shield`, find `blck` and set it to `Opened` in both:
- Auto Launch Permission
- Relevance Start Permission

## Privacy

No internet permission. No data collection. No tracking.

### Why these permissions?
- Display over other apps: to show the black overlay
- Accessibility service: to detect the `Back` + `OK` shortcut and remote activity for the idle timer (key presses are never stored)
- Query all packages: to list installed apps so you can choose where auto blackout works
- Foreground service: to keep the app running in the background
