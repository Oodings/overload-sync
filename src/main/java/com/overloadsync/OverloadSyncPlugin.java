package com.overloadsync;

import com.google.inject.Provides;
import java.util.Arrays;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Overload Sync",
	description = "Tick countdowns on Chambers of Xeric overloads and brews: line your sip up with the stat decay clock and brew just before each re-boost",
	tags = {"cox", "raids", "chambers", "overload", "brew", "xeric", "boost", "tick"}
)
public class OverloadSyncPlugin extends Plugin
{
	/** The combat stats that boosts decay on. */
	private static final Skill[] DECAY_SKILLS = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC};

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private OverloadSyncOverlay overlay;

	@Inject
	private OverloadSyncConfig config;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private PerfectSound sound;

	private final OverloadClock clock = new OverloadClock();

	/** Last boosted level of each decay skill, or -1 if not read yet. */
	private final int[] lastLevels = new int[DECAY_SKILLS.length];

	/** True from logging in until the first game tick: varbits sent then are the current state, not a sip. */
	private boolean freshLogin;

	/** A prayer enhance lasts about 290 seconds; if the worn-off message is missed, assume it ended after this. */
	static final int PRAYER_ENHANCE_MAX_TICKS = 500;

	/** Tick the current prayer enhance is assumed to end by, or -1 if none is running. */
	private int prayerEnhanceUntilTick = -1;

	OverloadClock getClock()
	{
		return clock;
	}

	boolean isPrayerEnhanceActive()
	{
		return prayerEnhanceUntilTick != -1 && client.getTickCount() < prayerEnhanceUntilTick;
	}

	@Provides
	OverloadSyncConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OverloadSyncConfig.class);
	}

	@Override
	protected void startUp()
	{
		clock.reset();
		Arrays.fill(lastLevels, -1);
		freshLogin = false;
		prayerEnhanceUntilTick = -1;
		overlayManager.add(overlay);
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				readLevels();
				clock.onOverloadAlreadyActive(client.getVarbitValue(VarbitID.RAIDS_OVERLOAD_TIMER));
			}
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		clock.reset();
		sound.close();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING || state == GameState.LOGGING_IN)
		{
			// The decay clock restarts when you log in, so anything known before is stale.
			clock.reset();
			Arrays.fill(lastLevels, -1);
			freshLogin = true;
			prayerEnhanceUntilTick = -1;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (freshLogin)
		{
			freshLogin = false;
			readLevels();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
		{
			return;
		}
		String message = Text.removeTags(event.getMessage());
		String lower = message.toLowerCase();
		if (lower.contains("prayer enhance"))
		{
			if (lower.startsWith("you drink"))
			{
				prayerEnhanceUntilTick = client.getTickCount() + PRAYER_ENHANCE_MAX_TICKS;
			}
			else if (lower.contains("worn off"))
			{
				prayerEnhanceUntilTick = -1;
			}
		}
		else if (lower.startsWith("you drink") && (lower.contains("xeric's aid") || lower.contains("saradomin brew")))
		{
			onBrewDrunk();
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		Player local = client.getLocalPlayer();
		if (local != null && event.getActor() == local)
		{
			prayerEnhanceUntilTick = -1; // a prayer enhance ends on death
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() == VarbitID.RAIDS_CLIENT_INDUNGEON)
		{
			if (event.getValue() == 0)
			{
				prayerEnhanceUntilTick = -1; // leaving a raid ends the effect
			}
			return;
		}
		if (event.getVarbitId() != VarbitID.RAIDS_OVERLOAD_TIMER)
		{
			return;
		}

		int value = event.getValue();
		if (freshLogin)
		{
			clock.onOverloadAlreadyActive(value);
			return;
		}

		if (clock.onOverloadVarbit(value, client.getTickCount()))
		{
			reportSip();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		int index = Arrays.asList(DECAY_SKILLS).indexOf(event.getSkill());
		if (index < 0)
		{
			return;
		}

		int last = lastLevels[index];
		int current = event.getBoostedLevel();
		lastLevels[index] = current;

		// A boosted stat dropping by exactly 1, without going below its real level, is the decay clock.
		// Brews and an overload wearing off drop by more than 1.
		if (last != -1 && current == last - 1 && current >= event.getLevel() && !freshLogin)
		{
			boolean first = !clock.isDecayKnown();
			clock.onDecay(client.getTickCount());
			if (first && config.sipFeedback())
			{
				int down = clock.ticksDownPerDecay();
				if (down == -1)
				{
					chat("Decay clock found: your overloads now count down to a perfectly timed sip.");
				}
				else if (down == 0)
				{
					chat("Decay clock found: your current overload is already perfectly timed.");
				}
				else
				{
					chat("Decay clock found: " + describeOffset(down) + " Chain-potting keeps this timing"
						+ " for the rest of the raid.");
				}
			}
		}
	}

	private void readLevels()
	{
		for (int i = 0; i < DECAY_SKILLS.length; i++)
		{
			lastLevels[i] = client.getBoostedSkillLevel(DECAY_SKILLS[i]);
		}
	}

	/** Plain-words version of how far a sip is from perfect (down = ticks after the decay that it re-boosts). */
	private static String describeOffset(int down)
	{
		if (down <= OverloadClock.REBOOST_TICKS / 2)
		{
			String hint = down <= 2 ? " If you clicked on the green cue, set Click timing adjust to -" + down + "." : "";
			return "your overload re-boosts " + down + (down == 1 ? " tick" : " ticks") + " after each decay, so stats"
				+ " dip a level for " + down + (down == 1 ? " tick" : " ticks") + " each time." + hint;
		}
		int early = OverloadClock.REBOOST_TICKS - down;
		String hint = early <= 2 ? " If you clicked on the green cue, set Click timing adjust to +" + early + "." : "";
		return "your overload is " + early + (early == 1 ? " tick" : " ticks") + " early: each re-boost lands just"
			+ " before a decay, so stats dip a level for " + down + " ticks each time." + hint;
	}

	private void reportSip()
	{
		int down = clock.ticksDownPerDecay();
		if (down == -1)
		{
			sipChat("Overload drunk before the decay clock was seen, so it could not be lined up. A boosted combat"
				+ " stat dropping by 1 (e.g. Defence after a Xeric's aid) sets the clock.");
		}
		else if (down == 0)
		{
			sipChat("Perfect overload timing: every re-boost lands on a decay tick, so your stats never dip a level.");
			ding();
		}
		else
		{
			sipChat("Overload not perfect: " + describeOffset(down));
		}
	}

	/** The chat line after a sip, unless sip feedback is off (the ding has its own setting). */
	private void sipChat(String message)
	{
		if (config.sipFeedback())
		{
			chat(message);
		}
	}

	/** A brew or Xeric's aid was drunk. Perfect when it lands on the re-boost game tick (clicked on the 0). */
	private void onBrewDrunk()
	{
		if (!clock.isOverloadTimingKnown())
		{
			return;
		}
		// Clicked on the 0, carried out on the re-boost game tick: the re-boost is either just counted (25 away) or
		// about to be (0 away) when this message arrives.
		if (clock.ticksToReboost(client.getTickCount()) % OverloadClock.REBOOST_TICKS == 0)
		{
			if (config.brewFeedback())
			{
				chat("Perfect brew timing: drunk on the re-boost, so the heal lands with your stats back at full boost.");
			}
			ding();
		}
	}

	private void ding()
	{
		if (config.perfectSound())
		{
			sound.ding(config.soundVolume());
		}
	}

	private void chat(String message)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage("Overload Sync: " + message)
			.build());
	}
}
