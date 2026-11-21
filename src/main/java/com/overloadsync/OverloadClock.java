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
 * Sipping 1 tick after a decay tick puts every re-boost 1 tick after a decay, so stats spend 1 tick a level
 * down per decay instead of up to 24. Chain-potting on the expiry tick keeps that alignment, since 500 is a
 * multiple of 25.
 */
class OverloadClock
{
	static final int REBOOST_TICKS = 25;

	/** A click made during a tick is carried out on the next tick. */
	static final int CLICK_LEAD = 1;

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
		return Math.max(0, expiryTick() - tick);
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
	 * Ticks until the tick to click an overload so it is drunk 1 tick after a decay (0 = click now), or -1 if the
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
		int sipTick = decayTick + 1;
		int clickTick = sipTick - CLICK_LEAD + adjust;
		return Math.floorMod(clickTick - tick, REBOOST_TICKS);
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
		return expiryTick() - CLICK_LEAD + adjust - tick;
	}

	/**
	 * How many ticks stats spend a level down after each decay with the current overload, 1 being the best.
	 * 25 means the re-boost lands on the decay tick itself, where it depends on the order the game handles them.
	 * -1 if either clock is unknown.
	 */
	int ticksDownPerDecay()
	{
		if (!isDecayKnown() || !isOverloadTimingKnown())
		{
			return -1;
		}
		int gap = Math.floorMod(nextReboostTick - decayTick, REBOOST_TICKS);
		return gap == 0 ? REBOOST_TICKS : gap;
	}

	private int expiryTick()
	{
		return nextReboostTick + (reboostsLeft - 1) * REBOOST_TICKS;
	}
}
