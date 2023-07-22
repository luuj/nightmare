package net.runelite.client.plugins.nightmare;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.awt.Polygon;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.projectilecd.ProjectileInfo;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.infobox.Timer;
import net.runelite.client.plugins.prayer.PrayerReorder;

@PluginDescriptor(
	name = "<html><font color=#b82584>[J] Nightmare",
	enabledByDefault = false,
	description = "Nightmare plugin",
	tags = {"bosses", "combat", "nm", "overlay", "nightmare", "pve", "pvm", "ashihama"}
)

@Slf4j
@Singleton
public class NightmarePlugin extends Plugin
{
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
	private PrayerReorder prayerReorder;
	@Inject
	private EventBus eventBus;


	// Nightmare's attack animations
	private static final int NIGHTMARE_HUSK_SPAWN = 8565;
	private static final int NIGHTMARE_PARASITE_TOSS = 8606;
	private static final int NIGHTMARE_CHARGE = 8609;
	private static final int NIGHTMARE_MELEE_ATTACK = 8594;
	private static final int NIGHTMARE_RANGE_ATTACK = 8596;
	private static final int NIGHTMARE_MAGIC_ATTACK = 8595;
	private static final int NIGHTMARE_PRE_MUSHROOM = 37738;
	private static final int NIGHTMARE_MUSHROOM = 37739;
	private static final int NIGHTMARE_SHADOW = 1767;   // graphics object

	private static final LocalPoint MIDDLE_LOCATION = new LocalPoint(6208, 8128);
	private static final Set<LocalPoint> PHOSANIS_MIDDLE_LOCATIONS = ImmutableSet.of(new LocalPoint(6208, 7104), new LocalPoint(7232, 7104));
	private static final List<Integer> INACTIVE_TOTEMS = Arrays.asList(9435, 9438, 9441, 9444);
	private static final List<Integer> ACTIVE_TOTEMS = Arrays.asList(9436, 9439, 9442, 9445);
	@Getter(AccessLevel.PACKAGE)
	private final Map<Integer, MemorizedTotem> totems = new HashMap<>();
	@Getter(AccessLevel.PACKAGE)
	private final Map<LocalPoint, GameObject> spores = new HashMap<>();
	@Getter(AccessLevel.PACKAGE)
	private final Map<Polygon, Player> huskTarget = new HashMap<>();
	@Getter(AccessLevel.PACKAGE)
	private final Map<Integer, Player> parasiteTargets = new HashMap<>();
	@Getter(AccessLevel.PACKAGE)
	private final Set<GraphicsObject> shadows = new HashSet<>();
	private final Set<NPC> husks = new HashSet<>();
	private final Set<NPC> parasites = new HashSet<>();
	private final Set<NPC> sleepwalkers = new HashSet<>();

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
	private int ticksUntilNextAttack = 0;

	@Getter(AccessLevel.PACKAGE)
	private boolean parasite;
	@Getter(AccessLevel.PACKAGE)
	private int ticksUntilParasite = 0;

	@Getter(AccessLevel.PACKAGE)
	private boolean nightmareCharging = false;

	@Getter(AccessLevel.PACKAGE)
	private boolean shadowsSpawning = false;
	@Getter(AccessLevel.PACKAGE)
	private int shadowsTicks;

	private int totemsAlive = 0;

	public ArrayList<ProjectileInfo> projectileList = new ArrayList();

	@Getter(AccessLevel.PACKAGE)
	@Setter
	private boolean flash = false;

	public NightmarePlugin()
	{
		inFight = false;
	}

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
		prayerReorder.startUp();
		eventBus.register(prayerReorder);
		reset();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(sanfewInfoBox);

		prayerReorder.shutDown();
		eventBus.unregister(prayerReorder);
		reset();
	}

	private void reset()
	{
		inFight = false;
		nm = null;
		pendingNightmareAttack = null;
		nightmareCharging = false;
		shadowsSpawning = false;
		cursed = false;
		flash = false;
		parasite = false;
		ticksUntilNextAttack = 0;
		ticksUntilParasite = 0;
		shadowsTicks = 0;
		totemsAlive = 0;
		totems.clear();
		spores.clear();
		shadows.clear();
		husks.clear();
		huskTarget.clear();
		parasites.clear();
		parasiteTargets.clear();
		sleepwalkers.clear();
	}

	@Subscribe
	private void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (!inFight)
		{
			return;
		}

		GameObject gameObj = event.getGameObject();
		int id = gameObj.getId();
		if (id == NIGHTMARE_MUSHROOM || id == NIGHTMARE_PRE_MUSHROOM)
		{
			spores.put(gameObj.getLocalLocation(), gameObj);
		}
	}

	@Subscribe
	private void onGameObjectDespawned(GameObjectDespawned event)
	{
		if (!inFight)
		{
			return;
		}

		GameObject gameObj = event.getGameObject();
		int id = gameObj.getId();
		if (id == NIGHTMARE_MUSHROOM || id == NIGHTMARE_PRE_MUSHROOM)
		{
			spores.remove(gameObj.getLocalLocation());
		}
	}

	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		if (!inFight)
		{
			return;
		}

		if (event.getGraphicsObject().getId() == NIGHTMARE_SHADOW)
		{
			shadows.add(event.getGraphicsObject());
			shadowsSpawning = true;
			shadowsTicks = 5;
			ticksUntilNextAttack = 5;
		}
	}

	public void onProjectileMoved(final ProjectileMoved event){
		if (!inFight)
		{
			return;
		}

		final Projectile proj = event.getProjectile();
		Player targetPlayer;

		switch (proj.getId())
		{
			case 1770:
				targetPlayer = (Player) proj.getInteracting();
				parasiteTargets.putIfAbsent(targetPlayer.getId(), targetPlayer);
				break;
			case 1781:
				targetPlayer = (Player) proj.getInteracting();
				huskTarget.putIfAbsent(targetPlayer.getCanvasTilePoly(), targetPlayer);
				break;
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

		// this will trigger once when the fight begins
		if (nm == null && npc.getName() != null && (npc.getName().equalsIgnoreCase("The Nightmare") || npc.getName().equalsIgnoreCase("Phosani's Nightmare")))
		{
			//reset everything
			reset();
			nm = npc;
			inFight = true;
		}
		
		if (!inFight || !npc.equals(nm))
		{
			return;
		}
		
		int animationId = npc.getAnimation();
		
		if (animationId == NIGHTMARE_MAGIC_ATTACK)
		{
			ticksUntilNextAttack = 7;
			pendingNightmareAttack = cursed ? NightmareAttack.CURSE_MAGIC : NightmareAttack.MAGIC;
		}
		else if (animationId == NIGHTMARE_MELEE_ATTACK)
		{
			ticksUntilNextAttack = 7;
			pendingNightmareAttack = cursed ? NightmareAttack.CURSE_MELEE : NightmareAttack.MELEE;
		}
		else if (animationId == NIGHTMARE_RANGE_ATTACK)
		{
			ticksUntilNextAttack = 7;
			pendingNightmareAttack = cursed ? NightmareAttack.CURSE_RANGE : NightmareAttack.RANGE;
		}
		// check if phosanis because the middle locations may be used in the others charge locations
		else if (animationId == NIGHTMARE_CHARGE && ((!isPhosanis(npc.getId()) && !MIDDLE_LOCATION.equals(npc.getLocalLocation())) || (isPhosanis(npc.getId()) && !PHOSANIS_MIDDLE_LOCATIONS.contains(npc.getLocalLocation()))))
		{
			nightmareCharging = true;
			ticksUntilNextAttack = 5;
		}

		if (nightmareCharging && animationId != -1 && animationId != NIGHTMARE_CHARGE)
		{
			nightmareCharging = false;
		}

		if (animationId != NIGHTMARE_HUSK_SPAWN && !huskTarget.isEmpty())
		{
			huskTarget.clear();
		}

		if (animationId == NIGHTMARE_PARASITE_TOSS)
		{
			ticksUntilParasite = 27;

			if (config.parasitesInfoBox())
			{
				Timer parasiteInfoBox = new Timer(16200L, ChronoUnit.MILLIS, itemManager.getImage(ItemID.PARASITIC_EGG), this);
				parasiteInfoBox.setTooltip("Parasites");
				infoBoxManager.addInfoBox(parasiteInfoBox);
			}
		}
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		final NPC npc = event.getNpc();

		//if npc is in the totems map, update its phase
		if (totems.containsKey(npc.getIndex()))
		{
			totems.get(npc.getIndex()).updateCurrentPhase(npc.getId());
		}
		if (INACTIVE_TOTEMS.contains(npc.getId()))
		{
			//else if the totem is not in the totem array and it is an inactive totem, add it to the totem map.
			totems.putIfAbsent(npc.getIndex(), new MemorizedTotem(npc));
			totemsAlive++;
		}
		if (ACTIVE_TOTEMS.contains(npc.getId()))
		{
			totemsAlive--;
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		final NPC npc = event.getNpc();

		if (npc.getName() != null && npc.getName().equalsIgnoreCase("parasite"))
		{
			parasites.add(npc);
		}

		if (npc.getName() != null && npc.getName().equalsIgnoreCase("husk"))
		{
			husks.add(npc);
		}

		if (npc.getName() != null && npc.getName().equalsIgnoreCase("sleepwalker"))
		{
			sleepwalkers.add(npc);
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		final NPC npc = event.getNpc();

		if (npc.getName() != null && npc.getName().equalsIgnoreCase("sleepwalker"))
		{
			sleepwalkers.remove(npc);
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (event.getActor() instanceof NPC && event.getActor().getName() != null)
		{
			final NPC npc = (NPC)event.getActor();

			if (npc.getName() != null && npc.getName().equalsIgnoreCase("parasite"))
			{
				parasites.remove(npc);
			}

			if (npc.getName() != null && npc.getName().equalsIgnoreCase("husk"))
			{
				husks.remove(npc);
			}
		}
	}

	@Subscribe
	private void onChatMessage(ChatMessage event)
	{
		if (!inFight || nm == null || event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		if (event.getMessage().contains("The Nightmare has impregnated you with a deadly parasite!"))
		{
			Player localPlayer = client.getLocalPlayer();
			if (localPlayer != null)
			{
				parasiteTargets.putIfAbsent(localPlayer.getId(), localPlayer);
			}

			flash = true;
			parasite = true;
			ticksUntilParasite = 22;
		}

		if (event.getMessage().toLowerCase().contains("the parasite within you has been weakened") || event.getMessage().toLowerCase().contains("the parasite bursts out of you, fully grown"))
		{
			parasite = false;
		}

		// Set PrayerReorder class to public, functions setPrayerOrder and rebuildPrayers to public
		if (event.getMessage().toLowerCase().contains("the nightmare has cursed you, shuffling your prayers!"))
		{
			cursed = true;
			int[] cursedPrayers = {0,1,2,18,19,3,8,17,15,9,20,28,16,7,10,11,4,5,6,22,23,14,12,13,21,25,26,24,27};
			prayerReorder.setPrayerOrder(0, cursedPrayers);
			prayerReorder.rebuildPrayers(false);
		}

		if (event.getMessage().toLowerCase().contains("you feel the effects of the nightmare's curse wear off."))
		{
			cursed = false;
			int[] normalPrayers = {0,1,2,18,19,3,8,17,15,9,20,28,16,7,10,11,4,5,6,22,23,12,13,14,21,25,26,24,27};
			prayerReorder.setPrayerOrder(0, normalPrayers);
			prayerReorder.rebuildPrayers(false);
		}

		if (config.yawnInfoBox() && event.getMessage().toLowerCase().contains("the nightmare's spores have infected you, making you feel drowsy!"))
		{
			Timer yawnInfoBox = new Timer(15600L, ChronoUnit.MILLIS, spriteManager.getSprite(SpriteID.SPELL_DREAM, 0), this);
			yawnInfoBox.setTooltip("Yawning");
			infoBoxManager.addInfoBox(yawnInfoBox);
		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		GameState gamestate = event.getGameState();

		//if loading happens while inFight, the user has left the area (either via death or teleporting).
		if (gamestate == GameState.LOADING && inFight)
		{
			reset();
		}
	}

	@Subscribe
	private void onGameTick(final GameTick event)
	{
		if (!inFight || nm == null)
		{
			return;
		}

		//the fight has ended and everything should be reset
		if (nm.getId() == 378 || nm.getId() == 377)
		{
			reset();
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



	private boolean isPhosanis(int id)
	{
		return (id >= 9416 && id <= 9424) || (id >= 11153 && id <= 11155);
	}
}
