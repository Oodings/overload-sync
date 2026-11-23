package com.overloadsync;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(OverloadSyncConfig.GROUP)
public interface OverloadSyncConfig extends Config
{
	String GROUP = "overloadsync";

	@ConfigSection(
		name = "Overload",
		description = "Countdowns on your overloads",
		position = 0
	)
	String overloadSection = "overload";

	@ConfigSection(
		name = "Brews",
		description = "Countdown to the next re-boost on your Xeric's aids and Saradomin brews",
		position = 1
	)
	String brewSection = "brew";

	@ConfigSection(
		name = "Timing",
		description = "Fine-tune when the overload cues show",
		position = 4,
		closedByDefault = true
	)
	String timingSection = "timing";

	@ConfigItem(
		keyName = "showSipCountdown",
		name = "Countdown to lined-up sip",
		description = "With no overload running, count down to the tick to click an overload so it re-boosts<br>"
			+ "on the same tick your stats decay, so they never dip. Comes round every 25 ticks.<br>"
			+ "Needs a boosted combat stat to have dropped by 1 since you logged in (shows ? until then).",
		section = overloadSection,
		position = 0
	)
	default boolean showSipCountdown()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showTimeLeft",
		name = "Ticks left while active",
		description = "While an overload is running, show the ticks until it wears off",
		section = overloadSection,
		position = 1
	)
	default boolean showTimeLeft()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showChainCue",
		name = "Chain-pot cue",
		description = "Highlight your overloads on the tick to click so the next dose is drunk as the current one<br>"
			+ "wears off, keeping the 50 hitpoint heal and the lined-up timing",
		section = overloadSection,
		position = 2
	)
	default boolean showChainCue()
	{
		return true;
	}

	@Range(min = 1, max = 24)
	@Units(Units.TICKS)
	@ConfigItem(
		keyName = "overloadWindow",
		name = "Countdown window",
		description = "Colour your overloads this many ticks before the tick to click:<br>"
			+ "window colour, then get-ready colour, then click-now colour on the tick itself",
		section = overloadSection,
		position = 3
	)
	default int overloadWindow()
	{
		return 6;
	}

	@ConfigItem(
		keyName = "sipFeedback",
		name = "Sip feedback in chat",
		description = "After each overload sip, say in chat how well it lined up with the decay clock",
		section = overloadSection,
		position = 4
	)
	default boolean sipFeedback()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showBrewCountdown",
		name = "Countdown to re-boost",
		description = "While an overload is running, show the ticks until its next re-boost on your Xeric's aids<br>"
			+ "and Saradomin brews",
		section = brewSection,
		position = 0
	)
	default boolean showBrewCountdown()
	{
		return true;
	}

	@Range(min = 1, max = 24)
	@Units(Units.TICKS)
	@ConfigItem(
		keyName = "brewWindowTicks",
		name = "Brew window",
		description = "Colour your brews this many ticks before the re-boost (shown as 0):<br>"
			+ "window colour, then get-ready colour, then click-now colour on the re-boost tick.<br>"
			+ "Potions are 3 ticks apart: clicking on 6 and 3 fits two sips before the re-boost.<br>"
			+ "At 24 the countdown is coloured the whole time.",
		section = brewSection,
		position = 1
	)
	default int brewWindow()
	{
		return 24;
	}

	@ConfigItem(
		keyName = "brewOnlyBelowHp",
		name = "Only when HP is low",
		description = "Only show the brew countdown while your hitpoints are at or below the level set below.<br>"
			+ "An overload heals 50 when it wears off, so brewing while high just wastes the heal.",
		section = brewSection,
		position = 2
	)
	default boolean brewOnlyBelowHp()
	{
		return false;
	}

	@Range(min = 1, max = 99)
	@ConfigItem(
		keyName = "brewHpThreshold",
		name = "At or below HP",
		description = "Show the brew countdown while your hitpoints are at or below this",
		section = brewSection,
		position = 3
	)
	default int brewHpThreshold()
	{
		return 49;
	}

	@ConfigSection(
		name = "Perfect timing",
		description = "Chat message and a subtle ding when you land a perfectly timed overload or brew",
		position = 5
	)
	String perfectSection = "perfect";

	@ConfigItem(
		keyName = "brewFeedback",
		name = "Perfect brew message",
		description = "Say in chat when a brew or Xeric's aid is drunk on the re-boost (clicked on the 0).<br>"
			+ "The ding has its own setting below.",
		section = perfectSection,
		position = 0
	)
	default boolean brewFeedback()
	{
		return true;
	}

	@ConfigItem(
		keyName = "perfectSound",
		name = "Ding on perfect timing",
		description = "A short, quiet ding for a perfectly timed overload or brew",
		section = perfectSection,
		position = 1
	)
	default boolean perfectSound()
	{
		return true;
	}

	@Range(min = 0, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "soundVolume",
		name = "Ding volume",
		description = "Volume of the ding, 0 to 100%. 0 is silent.",
		section = perfectSection,
		position = 2
	)
	default int soundVolume()
	{
		return 25;
	}

	@ConfigSection(
		name = "Click blocking",
		description = "Hold back left-clicks on potions until the tick they should be drunk. Right-click still works.",
		position = 6
	)
	String blockSection = "block";

	@ConfigItem(
		keyName = "blockOverload",
		name = "Block early overload clicks",
		description = "While an overload is running, a left-click on an overload does nothing until the tick to<br>"
			+ "click the next one (the green tick). Right-click, then Drink, always works.",
		section = blockSection,
		position = 0
	)
	default boolean blockOverload()
	{
		return false;
	}

	@ConfigItem(
		keyName = "blockPrayerEnhance",
		name = "Block early prayer enhance clicks",
		description = "While a prayer enhance is running, a left-click on a prayer enhance does nothing until<br>"
			+ "its effect has worn off. Right-click, then Drink, always works.",
		section = blockSection,
		position = 1
	)
	default boolean blockPrayerEnhance()
	{
		return false;
	}

	@ConfigItem(
		keyName = "greyOutBlocked",
		name = "Grey out blocked potions",
		description = "Shade a potion while its left-click is blocked",
		section = blockSection,
		position = 2
	)
	default boolean greyOutBlocked()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "blockedColour",
		name = "Blocked colour",
		description = "Shade drawn over a blocked potion",
		section = blockSection,
		position = 3
	)
	default Color blockedColor()
	{
		return new Color(70, 70, 70, 190);
	}

	@ConfigSection(
		name = "Reminders",
		description = "Pulse overloads and prayer enhances when their effect isn't running",
		position = 2
	)
	String reminderSection = "reminders";

	@ConfigItem(
		keyName = "pulseInactive",
		name = "Pulse when not active",
		description = "A pulsing box around your overloads and prayer enhances while their effect isn't running,<br>"
			+ "so you don't forget to drink them",
		section = reminderSection,
		position = 0
	)
	default boolean pulseInactive()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pulseColour",
		name = "Pulse colour",
		description = "Colour of the pulsing box",
		section = reminderSection,
		position = 1
	)
	default Color pulseColor()
	{
		return new Color(0, 255, 0);
	}

	@ConfigSection(
		name = "Appearance",
		description = "Highlight style, colours, font and number position",
		position = 3
	)
	String styleSection = "style";

	@ConfigItem(
		keyName = "highlightStyle",
		name = "Highlight style",
		description = "Box: shades the inventory slot. Outline: a line around the potion.<br>"
			+ "Fill: tints the potion itself.",
		section = styleSection,
		position = 0
	)
	default HighlightStyle highlightStyle()
	{
		return HighlightStyle.BOX;
	}

	@ConfigItem(
		keyName = "colourTarget",
		name = "Colour the",
		description = "Apply the countdown colours to the highlight, the number, or both",
		section = styleSection,
		position = 1
	)
	default ColourTarget colourTarget()
	{
		return ColourTarget.NUMBER;
	}

	@Alpha
	@ConfigItem(
		keyName = "windowColour",
		name = "Window colour",
		description = "Colour while inside the countdown window",
		section = styleSection,
		position = 2
	)
	default Color windowColor()
	{
		return new Color(255, 0, 0, 64);
	}

	@Range(min = 0, max = 10)
	@Units(Units.TICKS)
	@ConfigItem(
		keyName = "getReadyTicks",
		name = "Get ready ticks",
		description = "The last ticks of the window shown in the get-ready colour",
		section = styleSection,
		position = 3
	)
	default int getReadyTicks()
	{
		return 3;
	}

	@Alpha
	@ConfigItem(
		keyName = "getReadyColour",
		name = "Get ready colour",
		description = "Colour in the last ticks before the click",
		section = styleSection,
		position = 4
	)
	default Color soonColor()
	{
		return new Color(255, 170, 0, 64);
	}

	@Alpha
	@ConfigItem(
		keyName = "clickNowColour",
		name = "Click now colour",
		description = "Colour on the tick itself (0)",
		section = styleSection,
		position = 5
	)
	default Color readyColor()
	{
		return new Color(0, 255, 0, 128);
	}

	@ConfigItem(
		keyName = "textFont",
		name = "Number font",
		description = "Font of the countdown numbers",
		section = styleSection,
		position = 6
	)
	default TextFont textFont()
	{
		return TextFont.SMALL;
	}

	@ConfigItem(
		keyName = "textColor",
		name = "Number colour",
		description = "Colour of the countdown numbers when they are not coloured by the countdown",
		section = styleSection,
		position = 7
	)
	default Color textColor()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "overloadTextPosition",
		name = "Overload number position",
		description = "Where the countdown sits on overloads",
		section = styleSection,
		position = 8
	)
	default TextPosition overloadTextPosition()
	{
		return TextPosition.BOTTOM_RIGHT;
	}

	@ConfigItem(
		keyName = "brewTextPosition",
		name = "Brew number position",
		description = "Where the countdown sits on brews",
		section = styleSection,
		position = 9
	)
	default TextPosition brewTextPosition()
	{
		return TextPosition.BOTTOM_RIGHT;
	}

	@Range(min = -2, max = 2)
	@Units(Units.TICKS)
	@ConfigItem(
		keyName = "clickAdjust",
		name = "Click timing adjust",
		description = "Moves the overload cues later (+) or earlier (-). Leave at 0 unless the chat feedback after<br>"
			+ "a sip on the cue keeps saying it was early or late.",
		section = timingSection,
		position = 0
	)
	default int clickAdjust()
	{
		return 0;
	}
}
