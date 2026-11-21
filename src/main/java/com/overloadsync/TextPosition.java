package com.overloadsync;

/** Where the countdown number sits on the item. */
public enum TextPosition
{
	TOP_LEFT("Top left"),
	TOP_RIGHT("Top right"),
	CENTRE("Centre"),
	BOTTOM_LEFT("Bottom left"),
	BOTTOM_RIGHT("Bottom right");

	private final String label;

	TextPosition(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
