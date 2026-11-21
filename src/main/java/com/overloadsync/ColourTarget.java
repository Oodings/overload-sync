package com.overloadsync;

/** What the cue colours are applied to. */
public enum ColourTarget
{
	HIGHLIGHT("Highlight"),
	NUMBER("Number"),
	BOTH("Both");

	private final String label;

	ColourTarget(String label)
	{
		this.label = label;
	}

	boolean highlight()
	{
		return this != NUMBER;
	}

	boolean number()
	{
		return this != HIGHLIGHT;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
