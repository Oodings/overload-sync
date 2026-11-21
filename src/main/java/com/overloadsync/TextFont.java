package com.overloadsync;

import java.awt.Font;
import java.util.function.Supplier;
import net.runelite.client.ui.FontManager;

/** Font for the countdown numbers. */
public enum TextFont
{
	SMALL("Small", FontManager::getRunescapeSmallFont),
	REGULAR("Regular", FontManager::getRunescapeFont),
	BOLD("Bold", FontManager::getRunescapeBoldFont);

	private final String label;
	private final Supplier<Font> font;

	TextFont(String label, Supplier<Font> font)
	{
		this.label = label;
		this.font = font;
	}

	Font font()
	{
		return font.get();
	}

	@Override
	public String toString()
	{
		return label;
	}
}
