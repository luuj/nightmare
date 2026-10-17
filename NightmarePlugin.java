package net.runelite.client.plugins.nightmare;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GraphicsObject;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.infobox.Timer;

@PluginDescriptor(
	name = "<html><font color=#b82584>[J] Nightmare",
	description = "Attack timers and attack highlights for the Nightmare and Phosani's Nightmare",
	tags = {"bosses", "combat", "nm", "overlay", "nightmare", "phosani", "pve", "pvm", "ashihama"},
	enabledByDefault = false
)
@Slf4j
@Singleton
public class NightmarePlugin extends Plugin
{
	private static final String CONFIG_GROUP = "nightmareOfAshihama";

	// Nightmare's animations. The charge animation is named NIGHTMARE_RESPAWN in the game data.
	private static final int NIGHTMARE_HUSK_SPAWN = AnimationID.HUSK_MAGIC_ATTACK;
	private static final int NIGHTMARE_PARASITE_TOSS = AnimationID.NIGHTMARE_ATTACK_PARASITE;
	private static final int NIGHTMARE_CHARGE = AnimationID.NIGHTMARE_RESPAWN;

	private static final int SHADOW_TICKS = 5;
	private static final int ATTACK_TICKS = 7;
	private static final int CHARGE_TICKS = 5;
	private static final int PARASITE_TOSS_TICKS = 27;
	private static final int PARASITE_IMPREGNATED_TICKS = 22;
	private static final long PARASITE_INFOBOX_MILLIS = 16_200;
	private static final long YAWN_INFOBOX_MILLIS = 15_600;

	// Instance-local tiles where her charge animation is a reposition, not an attack
	private static final int MIDDLE_X = 6208;
	private static final int MIDDLE_Y = 8128;
	private static final Set<Long> PHOSANIS_MIDDLE = ImmutableSet.of(pack(6208, 7104), pack(7232, 7104));

	private static final Set<Integer> HUSK_IDS = ImmutableSet.of(
		NpcID.NIGHTMARE_HUSK_MAGIC, NpcID.NIGHTMARE_HUSK_RANGED,
		NpcID.NIGHTMARE_CHALLENGE_HUSK_MAGIC, NpcID.NIGHTMARE_CHALLENGE_HUSK_RANGED);
	private static final Set<Integer> DEAD_IDS = ImmutableSet.of(NpcID.NIGHTMARE_DEAD, NpcID.NIGHTMARE_CHALLENGE_DEAD);

	@Inject
	private Client client;

	@Inject
	private NightmareConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private SpriteManager spriteManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private NightmareOverlay overlay;

	@Inject
	private SanfewInfoBox sanfewInfoBox;

	@Inject
	private PrayerCurse prayerCurse;

	@Getter(AccessLevel.PACKAGE)
	private final Map<Integer, MemorizedTotem> totems = new HashMap<>();

	@Getter(AccessLevel.PACKAGE)
	private final Map<LocalPoint, GameObject> spores = new HashMap<>();

	/** Players the husks will spawn on. Their tiles are worked out each frame, so the marker follows the camera. */
	@Getter(AccessLevel.PACKAGE)
	private final Set<Player> huskTargets = new HashSet<>();

	@Getter(AccessLevel.PACKAGE)
	private final Map<Integer, Player> parasiteTargets = new HashMap<>();

	@Getter(AccessLevel.PACKAGE)
	private final Set<GraphicsObject> shadows = new HashSet<>();

	private final Set<NPC> husks = new HashSet<>();
	private final Set<NPC> parasites = new HashSet<>();
	private final Set<NPC> sleepwalkers = new HashSet<>();
	private final Set<Timer> timers = new HashSet<>();

	@Nullable
	@Getter(AccessLevel.PACKAGE)
	private NightmareAttack pendingNightmareAttack;

	@Nullable
	@Getter(AccessLevel.PACKAGE)
	private NPC nm;

	@Getter(AccessLevel.PACKAGE)
	private boolean inFight;

	private boolean cursed;

	@Getter(AccessLevel.PACKAGE)
	private int ticksUntilNextAttack;

	@Getter(AccessLevel.PACKAGE)
	private boolean parasite;

	@Getter(AccessLevel.PACKAGE)
	private int ticksUntilParasite;

	@Getter(AccessLevel.PACKAGE)
	private boolean nightmareCharging;

	@Getter(AccessLevel.PACKAGE)
	private boolean shadowsSpawning;

	@Getter(AccessLevel.PACKAGE)
	private int shadowsTicks;

	/** Totems that are ready but not yet charged; the Nightmare is shielded while this is above 0. */
	private int totemsAlive;

	/** When the parasite flash started, or 0 if it isn't showing. */
	@Getter(AccessLevel.PACKAGE)
	private long flashStartMillis;

	@Provides
	NightmareConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(NightmareConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		overlayManager.add(sanfewInfoBox);
		reset();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(sanfewInfoBox);
		timers.forEach(infoBoxManager::removeInfoBox);
		timers.clear();
		reset();
	}

	private void reset()
	{
		inFight = false;
		nm = null;
		pendingNightmareAttack = null;
		nightmareCharging = false;
		shadowsSpawning = false;
		setCursed(false);
		flashStartMillis = 0;
		parasite = false;
		ticksUntilNextAttack = 0;
		ticksUntilParasite = 0;
		shadowsTicks = 0;
		totemsAlive = 0;
		totems.clear();
		spores.clear();
		shadows.clear();
		husks.clear();
		huskTargets.clear();
		parasites.clear();
		parasiteTargets.clear();
		sleepwalkers.clear();
	}

	void clearFlash()
	{
		flashStartMillis = 0;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (CONFIG_GROUP.equals(event.getGroup()) && "fixCursedPrayers".equals(event.getKey()))
		{
			// Apply or undo the unshuffle straight away if you're cursed when you change it
			if (config.fixCursedPrayers() && cursed)
			{
				prayerCurse.apply();
			}
			else
			{
				prayerCurse.restore();
			}
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject obj = event.getGameObject();
		if (inFight && isSpore(obj.getId()))
		{
			spores.put(obj.getLocalLocation(), obj);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		GameObject obj = event.getGameObject();
		if (inFight && isSpore(obj.getId()))
		{
			spores.remove(obj.getLocalLocation());
		}
	}

	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		if (inFight && event.getGraphicsObject().getId() == SpotanimID.NIGHTMARE_RIFT)
		{
			shadows.add(event.getGraphicsObject());
			shadowsSpawning = true;
			shadowsTicks = SHADOW_TICKS;
			ticksUntilNextAttack = SHADOW_TICKS;
		}
	}

	// Previously missing @Subscribe, so other players' parasites and the husk targets were never tracked.
	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		if (!inFight)
		{
			return;
		}

		Projectile proj = event.getProjectile();
		if (!(proj.getTargetActor() instanceof Player))
		{
			return;
		}

		Player target = (Player) proj.getTargetActor();
		if (proj.getId() == SpotanimID.NIGHTMARE_PARASITE_TRAVEL)
		{
			parasiteTargets.putIfAbsent(target.getId(), target);
		}
		else if (proj.getId() == SpotanimID.NIGHTMARE_TRANCE_TRAVEL)
		{
			huskTargets.add(target);
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		Actor actor = event.getActor();
		if (!(actor instanceof NPC))
		{
			return;
		}

		NPC npc = (NPC) actor;

		// Triggers once when the fight begins
		if (nm == null && isNightmare(npc))
		{
			reset();
			nm = npc;
			inFight = true;
		}

		if (!inFight || npc != nm)
		{
			return;
		}

		int animationId = npc.getAnimation();

		for (NightmareAttack attack : new NightmareAttack[]{NightmareAttack.MAGIC, NightmareAttack.MELEE, NightmareAttack.RANGE})
		{
			if (animationId == attack.getAnimation())
			{
				ticksUntilNextAttack = ATTACK_TICKS;
				pendingNightmareAttack = cursed ? cursedVersion(attack) : attack;
			}
		}

		// The charge animation is also used when she moves to the middle, which isn't an attack
		if (animationId == NIGHTMARE_CHARGE && !isMiddleTile(npc))
		{
			nightmareCharging = true;
			ticksUntilNextAttack = CHARGE_TICKS;
		}

		if (nightmareCharging && animationId != -1 && animationId != NIGHTMARE_CHARGE)
		{
			nightmareCharging = false;
		}

		if (animationId != NIGHTMARE_HUSK_SPAWN && !huskTargets.isEmpty())
		{
			huskTargets.clear();
		}

		if (animationId == NIGHTMARE_PARASITE_TOSS)
		{
			ticksUntilParasite = PARASITE_TOSS_TICKS;

			if (config.parasitesInfoBox())
			{
				Timer timer = new Timer(PARASITE_INFOBOX_MILLIS, ChronoUnit.MILLIS, itemManager.getImage(ItemID.NIGHTMARE_CHALLENGE_MORPH) /* parasitic egg icon */, this);
				timer.setTooltip("Parasites");
				addTimer(timer);
			}
		}
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		NPC npc = event.getNpc();
		TotemPhase phase = TotemPhase.of(npc.getId());
		if (phase == null)
		{
			return;
		}

		MemorizedTotem totem = totems.get(npc.getIndex());
		if (totem != null)
		{
			totem.updateCurrentPhase(npc.getId());
		}

		if (phase == TotemPhase.READY)
		{
			totems.putIfAbsent(npc.getIndex(), new MemorizedTotem(npc));
			totemsAlive++;
		}
		else if (phase == TotemPhase.CHARGED)
		{
			totemsAlive = Math.max(0, totemsAlive - 1);
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		if (nameIs(npc, "parasite"))
		{
			parasites.add(npc);
		}
		else if (HUSK_IDS.contains(npc.getId()) || nameIs(npc, "husk"))
		{
			husks.add(npc);
		}
		else if (nameIs(npc, "sleepwalker"))
		{
			sleepwalkers.add(npc);
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		parasites.remove(npc);
		husks.remove(npc);
		sleepwalkers.remove(npc);
		totems.remove(npc.getIndex());
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (event.getActor() instanceof NPC)
		{
			NPC npc = (NPC) event.getActor();
			parasites.remove(npc);
			husks.remove(npc);
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		// These options were in the settings before but never did anything; they're wired up now.
		if (!inFight || !"Attack".equalsIgnoreCase(event.getOption()))
		{
			return;
		}

		MenuEntry entry = event.getMenuEntry();
		NPC target = entry.getNpc();
		if (target == null)
		{
			return;
		}

		boolean hide = false;
		if (target == nm)
		{
			hide = config.hideAttackNightmareTotems() && totemsAlive > 0
				|| config.hideAttackNightmareParasites() && !parasites.isEmpty()
				|| config.hideAttackNightmareHusk() && !husks.isEmpty()
				|| config.hideAttackNightmareSleepwalkers() && !sleepwalkers.isEmpty() && !isPhosanisLastPhase();
		}
		else if (sleepwalkers.contains(target))
		{
			hide = config.hideAttackSleepwalkers() && isPhosanisLastPhase();
		}

		if (hide)
		{
			client.getMenu().removeMenuEntry(entry);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!inFight || nm == null || event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = event.getMessage().toLowerCase();

		if (message.contains("the nightmare has impregnated you with a deadly parasite!"))
		{
			Player localPlayer = client.getLocalPlayer();
			if (localPlayer != null)
			{
				parasiteTargets.putIfAbsent(localPlayer.getId(), localPlayer);
			}

			flashStartMillis = System.currentTimeMillis();
			parasite = true;
			ticksUntilParasite = PARASITE_IMPREGNATED_TICKS;
		}

		if (message.contains("the parasite within you has been weakened") || message.contains("the parasite bursts out of you, fully grown"))
		{
			parasite = false;
		}

		if (message.contains("the nightmare has cursed you, shuffling your prayers!"))
		{
			setCursed(true);
		}

		if (message.contains("you feel the effects of the nightmare's curse wear off."))
		{
			setCursed(false);
		}

		if (config.yawnInfoBox() && message.contains("the nightmare's spores have infected you, making you feel drowsy!"))
		{
			Timer timer = new Timer(YAWN_INFOBOX_MILLIS, ChronoUnit.MILLIS, spriteManager.getSprite(SpriteID.LunarMagicOn.DREAM, 0), this);
			timer.setTooltip("Yawning");
			addTimer(timer);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		// Loading during the fight means you've left (death or teleport)
		if (event.getGameState() == GameState.LOADING && inFight)
		{
			reset();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!inFight || nm == null)
		{
			return;
		}

		if (DEAD_IDS.contains(nm.getId()))
		{
			reset();
			return;
		}

		ticksUntilNextAttack--;

		if (ticksUntilParasite > 0)
		{
			ticksUntilParasite--;
			if (ticksUntilParasite == 0)
			{
				parasiteTargets.clear();
			}
		}

		if (pendingNightmareAttack != null && ticksUntilNextAttack <= 3)
		{
			pendingNightmareAttack = null;
		}

		if (shadowsTicks > 0)
		{
			shadowsTicks--;
			if (shadowsTicks == 0)
			{
				shadowsSpawning = false;
				shadows.clear();
			}
		}
	}

	private void addTimer(Timer timer)
	{
		// Forget timers that have already run out, so the set doesn't grow over a long trip
		timers.removeIf(Timer::cull);
		timers.add(timer);
		infoBoxManager.addInfoBox(timer);
	}

	private void setCursed(boolean cursed)
	{
		this.cursed = cursed;
		if (cursed && config.fixCursedPrayers())
		{
			prayerCurse.apply();
		}
		else
		{
			prayerCurse.restore();
		}
	}

	private boolean isMiddleTile(NPC npc)
	{
		LocalPoint lp = npc.getLocalLocation();
		if (isPhosanis(npc.getId()))
		{
			return PHOSANIS_MIDDLE.contains(pack(lp.getX(), lp.getY()));
		}
		return lp.getX() == MIDDLE_X && lp.getY() == MIDDLE_Y;
	}

	private boolean isPhosanisLastPhase()
	{
		return nm != null && nm.getId() >= NpcID.NIGHTMARE_CHALLENGE_PHASE_04 && nm.getId() <= NpcID.NIGHTMARE_CHALLENGE_WEAK_PHASE_04;
	}

	private static boolean isPhosanis(int id)
	{
		return (id >= NpcID.NIGHTMARE_CHALLENGE_PHASE_01 && id <= NpcID.NIGHTMARE_CHALLENGE_DYING)
			|| (id >= NpcID.NIGHTMARE_CHALLENGE_PHASE_04 && id <= NpcID.NIGHTMARE_CHALLENGE_WEAK_PHASE_04);
	}

	private static boolean isNightmare(NPC npc)
	{
		return nameIs(npc, "The Nightmare") || nameIs(npc, "Phosani's Nightmare");
	}

	private static boolean isSpore(int id)
	{
		return id == ObjectID.NIGHTMARE_SPORES || id == ObjectID.NIGHTMARE_SPORES_INITIAL;
	}

	private static NightmareAttack cursedVersion(NightmareAttack attack)
	{
		switch (attack)
		{
			case MAGIC:
				return NightmareAttack.CURSE_MAGIC;
			case MELEE:
				return NightmareAttack.CURSE_MELEE;
			default:
				return NightmareAttack.CURSE_RANGE;
		}
	}

	private static boolean nameIs(NPC npc, String name)
	{
		return npc.getName() != null && npc.getName().equalsIgnoreCase(name);
	}

	private static long pack(int x, int y)
	{
		return ((long) x << 32) | (y & 0xffffffffL);
	}
}
