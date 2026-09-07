<h2 align="center">
  <img height="120" src="logo.svg" />
  <br>
    blck — turns your TV screen off without stopping whatever is playing
</h2>

Listening to something on a TV usually means looking at it too. Screensavers and sleep timers kill playback along with the picture.
`blck` just draws a black overlay.
The app underneath keeps running, the sound keeps playing, any key brings the picture back.
The room goes dark and the sound plays on without you watching it.

<img src="screenshot.png" />


## How it works

There are two ways to black out the screen, meant to be used together.

Hold Back and press OK. The screen goes black immediately, in any app, with nothing to set up first.

Idle timeout. Leave the remote alone for a set time and the screen blacks out on its own. The timeout only runs in the apps you pick, and while that list is empty it never fires — so it can't cut in while you're actually watching something.

Any key brings the picture back except volume keys.

## Limitations

Whether the screen actually stops emitting light is up to your TV, not the app — so OLED and Mini-LED sets work well, while an ordinary LED panel
will still glow.

## Permissions the app needs

- **Display over other apps** — to draw the overlay.
- **Accessibility service** — to catch the Back + OK chord while another app is in the
  foreground. It never reads screen content, and it intercepts nothing but the chord itself.

## Requirements

Google TV or Android TV, version 11 or newer.
