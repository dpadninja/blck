<h2 align="center">
  <img height="150" src="logo.svg" />
  <br>
    blck: turns your TV screen off without stopping whatever is playing
</h2>

You put music on in the evening and the TV keeps demanding attention — album art,
scrolling lists, a bright panel lighting up the whole room. Google TV gives you no
way out: every screensaver and sleep timer kills playback along with the picture.

## Limitations

`blck` only draws a black overlay. Whether the screen actually stops emitting light is up
to your TV, not the app — so OLED and Mini-LED sets work well, while an ordinary LED panel
will still glow.


## Features

There are two ways to black out the screen, and they complement each other.

**Back + OK.** Hold Back and press OK. The screen goes black immediately, in any app,
with nothing to set up first. Press any key except volume up/down to bring it back.

**Idle timeout.** Leave the remote alone for a set time and the screen blacks out
on its own. It only runs in the apps you choose, and while that list is empty the
timeout never fires — so it can't cut in while you are actually watching
something.

Any key except volume brings the picture back. Volume is the one exception:
people change it without looking, and there is no reason to light up the screen for
that. The overlay intercepts nothing — the key still reaches the app underneath as usual.


## Permissions the app needs

- **Display over other apps** — to draw the overlay.
- **Accessibility service** — to catch the Back + OK chord while another app is in the
  foreground. It never reads screen content, and it intercepts nothing but the chord itself.


## Requirements

Google TV or Android TV, version 11 or newer.
