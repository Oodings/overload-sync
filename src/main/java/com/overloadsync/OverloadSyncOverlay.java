package com.overloadsync;

import com.google.common.collect.ImmutableSet;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/** Draws the tick countdowns and highlights on overloads and brews in the inventory. */
class OverloadSyncOverlay extends WidgetItemOverlay
{
	static final Set<Integer> OVERLOADS = ImmutableSet.of(
		ItemID.RAIDS_VIAL_OVERLOAD_WEAK_1, ItemID.RAIDS_VIAL_OVERLOAD_WEAK_2,
		ItemID.RAIDS_VIAL_OVERLOAD_WEAK_3, ItemID.RAIDS_VIAL_OVERLOAD_WEAK_4,
		ItemID.RAIDS_VIAL_OVERLOAD_1, ItemID.RAIDS_VIAL_OVERLOAD_2,
		ItemID.RAIDS_VIAL_OVERLOAD_3, ItemID.RAIDS_VIAL_OVERLOAD_4,
		ItemID.RAIDS_VIAL_OVERLOAD_STRONG_1, ItemID.RAIDS_VIAL_OVERLOAD_STRONG_2,
		ItemID.RAIDS_VIAL_OVERLOAD_STRONG_3, ItemID.RAIDS_VIAL_OVERLOAD_STRONG_4);

	/** Xeric's aids (all strengths) and Saradomin brews. */
	static final Set<Integer> BREWS = ImmutableSet.of(
		ItemID.RAIDS_VIAL_XERICAID_WEAK_1, ItemID.RAIDS_VIAL_XERICAID_WEAK_2,
		ItemID.RAIDS_VIAL_XERICAID_WEAK_3, ItemID.RAIDS_VIAL_XERICAID_WEAK_4,
		ItemID.RAIDS_VIAL_XERICAID_1, ItemID.RAIDS_VIAL_XERICAID_2,
		ItemID.RAIDS_VIAL_XERICAID_3, ItemID.RAIDS_VIAL_XERICAID_4,
		ItemID.RAIDS_VIAL_XERICAID_STRONG_1, ItemID.RAIDS_VIAL_XERICAID_STRONG_2,
		ItemID.RAIDS_VIAL_XERICAID_STRONG_3, ItemID.RAIDS_VIAL_XERICAID_STRONG_4,
		ItemID._1DOSEPOTIONOFSARADOMIN, ItemID._2DOSEPOTIONOFSARADOMIN,
		ItemID._3DOSEPOTIONOFSARADOMIN, ItemID._4DOSEPOTIONOFSARADOMIN);

	static final Set<Integer> PRAYER_ENHANCES = ImmutableSet.of(
		ItemID.RAIDS_VIAL_PRAYER_WEAK_1, ItemID.RAIDS_VIAL_PRAYER_WEAK_2,
		ItemID.RAIDS_VIAL_PRAYER_WEAK_3, ItemID.RAIDS_VIAL_PRAYER_WEAK_4,
		ItemID.RAIDS_VIAL_PRAYER_1, ItemID.RAIDS_VIAL_PRAYER_2,
		ItemID.RAIDS_VIAL_PRAYER_3, ItemID.RAIDS_VIAL_PRAYER_4,
		ItemID.RAIDS_VIAL_PRAYER_STRONG_1, ItemID.RAIDS_VIAL_PRAYER_STRONG_2,
		ItemID.RAIDS_VIAL_PRAYER_STRONG_3, ItemID.RAIDS_VIAL_PRAYER_STRONG_4);

	/** One full pulse, in milliseconds. */
	private static final double PULSE_PERIOD_MS = 1200;

	private final Client client;
	private final OverloadSyncPlugin plugin;
	private final OverloadSyncConfig config;
	private final OverloadClock clock;
	private final ItemManager itemManager;

	@Inject
	OverloadSyncOverlay(Client client, OverloadSyncConfig config, OverloadSyncPlugin plugin, ItemManager itemManager)
	{
		this.client = client;
		this.config = config;
		this.clock = plugin.getClock();
		this.plugin = plugin;
		this.itemManager = itemManager;
		showOnInventory();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		Rectangle slot = widgetItem.getCanvasBounds();
		if (config.greyOutBlocked() && plugin.isBlocked(itemId))
		{
			graphics.setColor(config.blockedColor());
			graphics.fill(slot);
		}
		if (PRAYER_ENHANCES.contains(itemId))
		{
			if (config.pulseInactive() && !plugin.isPrayerEnhanceActive())
			{
				drawPulse(graphics, slot);
			}
			return;
		}

		Cue cue;
		TextPosition position;
		if (OVERLOADS.contains(itemId))
		{
			cue = overloadCue(client.getTickCount());
			position = config.overloadTextPosition();
		}
		else if (BREWS.contains(itemId))
		{
			cue = brewCue(client.getTickCount());
			position = config.brewTextPosition();
		}
		else
		{
			return;
		}

		boolean pulse = OVERLOADS.contains(itemId) && config.pulseInactive() && !clock.isOverloadActive();
		if (cue == null)
		{
			if (pulse)
			{
				drawPulse(graphics, slot);
			}
			return;
		}

		Rectangle bounds = slot;
		ColourTarget target = config.colourTarget();
		if (cue.color != null && target.highlight())
		{
			drawHighlight(graphics, itemId, widgetItem.getQuantity(), bounds, cue.color);
		}
		if (cue.text != null)
		{
			Color textColor = cue.color != null && target.number() ? solid(cue.color) : config.textColor();
			drawText(graphics, cue.text, bounds, position, textColor);
		}
		if (pulse)
		{
			drawPulse(graphics, bounds);
		}
	}

	/** A box around the slot that fades in and out. */
	void drawPulse(Graphics2D graphics, Rectangle bounds)
	{
		double phase = (System.currentTimeMillis() % (long) PULSE_PERIOD_MS) / PULSE_PERIOD_MS;
		int alpha = (int) Math.round(60 + 195 * (0.5 + 0.5 * Math.sin(phase * 2 * Math.PI)));
		Color c = config.pulseColor();
		graphics.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
		graphics.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
		graphics.drawRect(bounds.x + 1, bounds.y + 1, bounds.width - 3, bounds.height - 3);
	}

	/** Draws exactly one highlight in one colour, replacing nothing else on the item. */
	private void drawHighlight(Graphics2D graphics, int itemId, int quantity, Rectangle bounds, Color color)
	{
		HighlightStyle style = config.highlightStyle();
		Color solid = solid(color);
		if (style == HighlightStyle.BOX)
		{
			graphics.setColor(color);
			graphics.fill(bounds);
			graphics.setColor(solid);
			graphics.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
			return;
		}

		if (style == HighlightStyle.FILL || style == HighlightStyle.OUTLINE_AND_FILL)
		{
			Image fill = fillImage(itemId, quantity, color);
			if (fill != null)
			{
				graphics.drawImage(fill, bounds.x, bounds.y, null);
			}
		}
		if (style == HighlightStyle.OUTLINE || style == HighlightStyle.OUTLINE_AND_FILL)
		{
			BufferedImage outline = itemManager.getItemOutline(itemId, quantity, solid);
			graphics.drawImage(outline, bounds.x, bounds.y, null);
		}
	}

	/** The item tinted in one colour. Small images, so made fresh each frame (no stale half-loaded copies). */
	private Image fillImage(int itemId, int quantity, Color color)
	{
		BufferedImage item = itemManager.getImage(itemId, quantity, false);
		return item == null ? null : ImageUtil.fillImage(item, color);
	}

	private static Color solid(Color c)
	{
		return new Color(c.getRed(), c.getGreen(), c.getBlue());
	}

	private void drawText(Graphics2D graphics, String text, Rectangle bounds, TextPosition position, Color color)
	{
		graphics.setFont(config.textFont().font());
		FontMetrics metrics = graphics.getFontMetrics();
		int width = metrics.stringWidth(text);
		int ascent = metrics.getAscent();
		int x;
		int y;
		switch (position)
		{
			case TOP_LEFT:
				x = bounds.x + 1;
				y = bounds.y + ascent - 1;
				break;
			case TOP_RIGHT:
				x = bounds.x + bounds.width - width - 1;
				y = bounds.y + ascent - 1;
				break;
			case CENTRE:
				x = bounds.x + (bounds.width - width) / 2;
				y = bounds.y + (bounds.height + ascent) / 2 - 1;
				break;
			case BOTTOM_LEFT:
				x = bounds.x + 1;
				y = bounds.y + bounds.height - 1;
				break;
			case BOTTOM_RIGHT:
			default:
				x = bounds.x + bounds.width - width - 1;
				y = bounds.y + bounds.height - 1;
				break;
		}
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, x + 1, y + 1);
		graphics.setColor(color);
		graphics.drawString(text, x, y);
	}

	Cue overloadCue(int tick)
	{
		int adjust = config.clickAdjust();
		if (clock.isOverloadActive())
		{
			if (!clock.isOverloadTimingKnown())
			{
				return null; // logged in mid-overload: known after its next re-boost
			}

			String text = config.showTimeLeft() ? String.valueOf(clock.overloadTicksLeft(tick)) : null;
			Color color = null;
			if (config.showChainCue())
			{
				int toClick = clock.ticksToChainClick(tick, adjust);
				color = cueColor(toClick, config.overloadWindow());
				if (text == null && color != null)
				{
					// Ticks left is hidden: count down to the click during the window instead.
					text = String.valueOf(toClick);
				}
			}
			return new Cue(text, color);
		}

		if (!config.showSipCountdown())
		{
			return null;
		}
		if (!clock.isDecayKnown())
		{
			return new Cue("?", null);
		}
		int toClick = clock.ticksToAlignedClick(tick, adjust);
		return new Cue(String.valueOf(toClick), cueColor(toClick, config.overloadWindow()));
	}

	Cue brewCue(int tick)
	{
		if (!config.showBrewCountdown() || !clock.isOverloadTimingKnown())
		{
			return null;
		}
		if (config.brewOnlyBelowHp() && client.getBoostedSkillLevel(Skill.HITPOINTS) > config.brewHpThreshold())
		{
			return null;
		}
		// 0 on the re-boost tick itself, then 24 down to 1.
		int toReboost = clock.ticksToReboost(tick) % OverloadClock.REBOOST_TICKS;
		return new Cue(String.valueOf(toReboost), cueColor(toReboost, config.brewWindow()));
	}

	/** Click-now colour at 0, get-ready colour for the last few ticks, window colour before that. */
	private Color cueColor(int ticks, int window)
	{
		if (ticks == 0)
		{
			return config.readyColor();
		}
		if (ticks < 0 || ticks > window)
		{
			return null;
		}
		return ticks <= config.getReadyTicks() ? config.soonColor() : config.windowColor();
	}

	/** What to draw on one item. */
	static final class Cue
	{
		final String text;
		final Color color;

		Cue(String text, Color color)
		{
			this.text = text;
			this.color = color;
		}
	}
}
