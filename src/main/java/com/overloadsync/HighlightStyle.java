package com.overloadsync;

/** How a cue colour is drawn on the item. */
public enum HighlightStyle
{
	BOX("Box"),
	OUTLINE("Outline"),
	FILL("Fill"),
	OUTLINE_AND_FILL("Outline and fill");

	private final String label;

	HighlightStyle(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
