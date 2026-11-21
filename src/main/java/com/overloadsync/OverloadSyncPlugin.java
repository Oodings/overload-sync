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
		if (message.startsWith("You drink some of your") && message.toLowerCase().contains("prayer enhance"))
		{
			prayerEnhanceUntilTick = client.getTickCount() + PRAYER_ENHANCE_MAX_TICKS;
		}
		else if (message.equals("Your prayer enhance effect has worn off."))
		{
			prayerEnhanceUntilTick = -1;
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
			prayerEnhanceUntilTick = -1; // entering or leaving a raid
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

		if (clock.onOverloadVarbit(value, client.getTickCount()) && config.sipFeedback())
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
					chat("Decay clock found: your overloads now count down to a lined-up sip.");
				}
				else if (down == 1)
				{
					chat("Decay clock found: your current overload is already lined up.");
				}
				else
				{
					chat("Decay clock found: your current overload is off by " + (down - 1)
						+ (down == 2 ? " tick" : " ticks") + " (" + down + " ticks a level down per decay)."
						+ " Chain-potting keeps this timing for the rest of the raid.");
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

	private void reportSip()
	{
		int down = clock.ticksDownPerDecay();
		if (down == -1)
		{
			chat("Overload drunk before the decay clock was seen, so it could not be lined up. A boosted combat"
				+ " stat dropping by 1 (e.g. Defence after a Xeric's aid) sets the clock.");
		}
		else if (down == 1)
		{
			chat("Overload lined up: stats will spend just 1 tick a level down per decay.");
		}
		else if (down == OverloadClock.REBOOST_TICKS)
		{
			chat("Overload re-boosts land on the decay tick itself. If you clicked on the green cue,"
				+ " set Click timing adjust to +1.");
		}
		else
		{
			String hint = down == 2 ? " If you clicked on the green cue, set Click timing adjust to -1." : "";
			chat("Overload off by " + (down - 1) + (down == 2 ? " tick" : " ticks") + ": stats will spend "
				+ down + " ticks a level down per decay (best is 1)." + hint);
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
