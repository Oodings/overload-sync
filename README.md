# Overload Sync

A RuneLite plugin for the Chambers of Xeric that puts tick countdowns on your overloads, Xeric's
aids and Saradomin brews, so you can line your overload up with the stat decay clock and brew just
before each re-boost. It can also remind you to drink your overloads and prayer enhances.

## Why it helps

- Boosted stats drop by 1 level on a background clock every 100 ticks (60 seconds), whenever you
  drank. An overload tops your stats back up every 25 ticks.
- If the overload's re-boost lands on the same game tick as each decay, your stats never dip a
  level. Drunk at a random moment, they can sit a level down for up to 24 ticks per decay.
- 500 ticks (one overload) is a multiple of 25, so drinking the next dose as the last one wears off
  keeps the same alignment, and still gets the 50 hitpoint heal.
- Xeric's aids and Saradomin brews drain your offensive stats, but the next re-boost puts them
  back. Brewing just before a re-boost means they are drained for only a few ticks.

## Getting started in a raid

The overloads show **?** until the plugin has seen your stat decay clock, and that only shows when a
boosted combat stat drops by 1 on the normal timer. It restarts every time you log in or hop.

If you run challenge mode raids, drink your usual pre-potions for Tekton as normal (divine super
combat, stamina, antidote and so on). Divine potions drain on their own faster timer, so they
don't reveal the decay clock. Also drink one potion with the conventional boost and drain, such as
a ranging potion. When that stat drops its first level, the plugin has the clock and the **?**
turns into a countdown, so you can sip the overload you take into Tekton on the right tick. Any
boosted combat stat that drains normally does the job, including a Xeric's aid's Defence boost.

## What it shows

**On overloads**
- With no overload running: the ticks until the tick to click so the dose lines up. This comes
  round every 25 ticks.
- While an overload is running: a countdown in the last 6 ticks before the tick to click the next
  dose, so it is drunk as the current one wears off. Optionally, the ticks left (500 to 0) the
  whole time.
- Colours by default: red, then amber for the last 3 ticks, then green on the tick to click.
- After each sip, a chat message says how well it lined up. A perfectly timed sip (stats never
  dip) gets a "Perfect overload timing" message and an optional quiet ding.

**On Xeric's aids and Saradomin brews** (while an overload is running)
- The ticks until the next re-boost, 0 being the re-boost itself: red, then amber on 3, 2 and 1,
  then green on 0. Potions are 3 ticks apart, so brews clicked on 6 and 3 are both wiped by the
  re-boost. The **Brew window** setting limits how early the colours start.
- Optionally only while your hitpoints are at or below a level you choose, since an overload heals
  50 when it wears off and brewing while high wastes that.
- A brew clicked on the 0 gets a "Perfect brew timing" message and the same optional ding.

**Reminders** (optional, on by default)
- A pulsing green box around your overloads while no overload is running, and around your prayer
  enhances while no prayer enhance is running, so you don't forget the once-every-5-minutes potions.

**Click blocking** (optional, off by default)
- Stops you re-drinking too early by holding back the left-click on a potion until the right moment.
  Overloads are held back while one is running, until the green tick to click the next. Prayer
  enhances are held back until the effect has worn off.
- A held-back potion is shaded grey, and right-click, then Drink, always works.

## Good to know

- The decay clock can only be seen when a boosted combat stat drops by 1, and it restarts when you
  log in or hop. Until then the overloads show **?** (see Getting started in a raid).
- If you log in with an overload already running, its countdowns appear after its next re-boost.
- **Appearance** settings choose the highlight style (a shaded box, an outline around the potion or
  a tint), whether the colours go on the highlight, the number or both, the colours and their
  opacity, the number font, and where the number sits on overloads and on brews.
- **Click timing adjust** moves the overload cues a tick earlier or later. If the chat message after
  sipping on the green cue keeps suggesting a change, follow it.
- The plugin only draws on your inventory and posts chat messages. It never clicks or changes
  menus, and makes no network requests.

## Development note

This plugin was written with AI assistance (Claude) and tested in-game by the author.
