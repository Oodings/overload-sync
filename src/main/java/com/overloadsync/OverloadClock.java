package com.overloadsync;

/**
 * The two clocks that decide when to drink, in game ticks. Holds no game objects so it can be tested on its own.
 *
 * <ul>
 * <li>Stat decay: boosted combat stats drop by 1 on a background timer every 100 ticks (125 or 150 with
 * Preserve). All of these are multiples of 25, so only the decay tick modulo 25 matters here.</li>
 * <li>Overload: re-boosts every 25 ticks from the sip, 20 times (500 ticks), counted by a varbit that drops by 1
 * at each re-boost and reaches 0 when the overload wears off.</li>
 * </ul>
 *
 * Sipping so the first re-boost lands on a decay tick means no re-boost is ever late, so stats never dip a level.
 * Chain-potting on the expiry tick keeps that alignment, since 500 is a multiple of 25.
 */
class OverloadClock
{
	static final int REBOOST_TICKS = 25;

	/** Tick of the last observed decay, or -1 if not seen since logging in. */
	private int decayTick = -1;

	/** Re-boosts left (the varbit value). */
	private int reboostsLeft;

	/** Tick of the next re-boost, or -1 if not known yet (e.g. logged in mid-overload). */
	private int nextReboostTick = -1;

	void reset()
	{
		decayTick = -1;
		reboostsLeft = 0;
		nextReboostTick = -1;
	}

	/** A boosted combat stat dropped by exactly 1 on this tick. */
	void onDecay(int tick)
	{
		decayTick = tick;
	}

	/**
	 * The overload varbit changed.
	 *
	 * @return true if this was a fresh sip (the count went up)
	 */
	boolean onOverloadVarbit(int value, int tick)
	{
		boolean sip = value > reboostsLeft;
		if (value <= 0)
		{
			reboostsLeft = 0;
			nextReboostTick = -1;
			return false;
		}

		// A sip or a re-boost happens on this tick; the next one is 25 ticks later.
		nextReboostTick = tick + REBOOST_TICKS;
		reboostsLeft = value;
		return sip;
	}

	/** Called when the varbit is first read with an overload already running: the timing is unknown. */
	void onOverloadAlreadyActive(int value)
	{
		reboostsLeft = Math.max(0, value);
		nextReboostTick = -1;
	}

	boolean isDecayKnown()
	{
		return decayTick != -1;
	}

	boolean isOverloadActive()
	{
		return reboostsLeft > 0;
	}

	boolean isOverloadTimingKnown()
	{
		return reboostsLeft > 0 && nextReboostTick != -1;
	}

	/** Ticks until the overload wears off, or -1 if unknown. */
	int overloadTicksLeft(int tick)
	{
		if (!isOverloadTimingKnown())
		{
			return -1;
		}
		return Math.max(0, expiryTick() + 1 - tick); // counts to 0 on the tick the overload actually wears off
	}

	/** Ticks until the next re-boost (1 to 25), or -1 if unknown. */
	int ticksToReboost(int tick)
	{
		if (!isOverloadTimingKnown())
		{
			return -1;
		}
		return Math.max(0, nextReboostTick - tick);
	}

	/**
	 * Ticks until the tick to click an overload so it is drunk on a decay tick (0 = click now), or -1 if the
	 * decay clock has not been seen. Repeats every 25 ticks.
	 *
	 * @param adjust moves the cue later (+) or earlier (-) to match how clicks land in practice
	 */
	int ticksToAlignedClick(int tick, int adjust)
	{
		if (!isDecayKnown())
		{
			return -1;
		}
		// A click is carried out on the next game tick, and the game tick count only goes up after that tick's
		// events, so clicking when the count equals the decay tick lands the sip on the decay itself.
		return Math.floorMod(decayTick + adjust - tick, REBOOST_TICKS);
	}

	/**
	 * Ticks until the tick to click the next overload so it is drunk as the current one wears off (0 = click now),
	 * or -1 if unknown. Negative once that moment has passed.
	 */
	int ticksToChainClick(int tick, int adjust)
	{
		if (!isOverloadTimingKnown())
		{
			return -1;
		}
		return expiryTick() + adjust - tick; // same one-tick click delay as above
	}

	/**
	 * How many ticks after each decay the overload re-boosts, i.e. how long stats sit a level down. 0 is perfect: the
	 * re-boost lands on the decay tick itself and stats never dip. -1 if either clock is unknown.
	 */
	int ticksDownPerDecay()
	{
		if (!isDecayKnown() || !isOverloadTimingKnown())
		{
			return -1;
		}
		return Math.floorMod(nextReboostTick - decayTick, REBOOST_TICKS);
	}

	private int expiryTick()
	{
		return nextReboostTick + (reboostsLeft - 1) * REBOOST_TICKS;
	}
}
