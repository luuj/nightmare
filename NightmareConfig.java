package net.runelite.client.plugins.nightmare;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.FontType;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("nightmareOfAshihama")
public interface NightmareConfig extends Config
{
	@ConfigSection(name = "General", description = "Attack timer and menu options", position = 0)
	String generalSection = "General";

	@ConfigSection(name = "Totems", description = "Totem highlights", position = 1)
	String totemsSection = "Totems";

	@ConfigSection(name = "Shadows", description = "Shadow (rift) attack highlights", position = 2)
	String shadowsSection = "Shadows";

	@ConfigSection(name = "Spores", description = "Spore highlights and yawning", position = 3)
	String sporesSection = "Spores";

	@ConfigSection(name = "Parasites", description = "Parasite timers and reminders", position = 4)
	String parasitesSection = "Parasites";

	@ConfigSection(name = "Husks", description = "Husk highlights", position = 5)
	String huskSection = "Husk";

	@ConfigSection(name = "Charge", description = "Charge attack highlights", position = 6)
	String chargeSection = "Charge";

	@ConfigSection(name = "Curse", description = "Prayer curse handling", position = 7)
	String curseSection = "curseSection";

	@ConfigSection(name = "Appearance", description = "How the overlay is drawn", position = 8)
	String appearanceSection = "appearanceSection";

	// General

	@ConfigItem(
		keyName = "tickCounter",
		name = "Show ticks",
		description = "Show the ticks until the Nightmare's next attack, colored by attack style",
		position = 0,
		section = generalSection
	)
	default boolean ticksCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideAttackNightmareTotems",
		name = "Hide attack during totems",
		description = "Remove the Attack option on the Nightmare while the totems are up",
		position = 1,
		section = generalSection
	)
	default boolean hideAttackNightmareTotems()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideAttackNightmareParasites",
		name = "Hide attack during parasites",
		description = "Remove the Attack option on the Nightmare while parasites are alive",
		position = 2,
		section = generalSection
	)
	default boolean hideAttackNightmareParasites()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideAttackNightmareHusk",
		name = "Hide attack during husks",
		description = "Remove the Attack option on the Nightmare while husks are alive",
		position = 3,
		section = generalSection
	)
	default boolean hideAttackNightmareHusk()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideAttackNightmareSleepwalkers",
		name = "Hide attack during sleepwalkers",
		description = "Remove the Attack option on the Nightmare while sleepwalkers are alive (not in Phosani's last phase)",
		position = 4,
		section = generalSection
	)
	default boolean hideAttackNightmareSleepwalkers()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideAttackSleepwalkers",
		name = "Hide attack on sleepwalkers",
		description = "Remove the Attack option on sleepwalkers during Phosani's last phase",
		position = 5,
		section = generalSection
	)
	default boolean hideAttackSleepwalkers()
	{
		return false;
	}

	// Totems

	@ConfigItem(
		keyName = "highlightTotems",
		name = "Highlight totems",
		description = "Outline totems by state: orange when ready, green when charged",
		position = 0,
		section = totemsSection
	)
	default boolean highlightTotems()
	{
		return true;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = "totemOutlineSize",
		name = "Outline width",
		description = "Thickness of the totem outline",
		position = 1,
		section = totemsSection
	)
	default int totemOutlineSize()
	{
		return 3;
	}

	// Shadows

	@ConfigItem(
		keyName = "highlightShadows",
		name = "Highlight shadows",
		description = "Mark the tiles hit by the shadow attack",
		position = 0,
		section = shadowsSection
	)
	default boolean highlightShadows()
	{
		return true;
	}

	@ConfigItem(
		keyName = "shadowsTickCounter",
		name = "Show ticks remaining",
		description = "Show how many ticks until the shadows hit",
		position = 1,
		section = shadowsSection
	)
	default boolean shadowsTickCounter()
	{
		return true;
	}

	@Range(min = 1, max = 20)
	@Units(" tiles")
	@ConfigItem(
		keyName = "shadowsRenderDistance",
		name = "Render distance",
		description = "Only draw shadows within this many tiles of your player",
		position = 2,
		section = shadowsSection
	)
	default int shadowsRenderDistance()
	{
		return 5;
	}

	@Alpha
	@ConfigItem(
		keyName = "shadowsBorderColour",
		name = "Border color",
		description = "Outline color of the shadow tiles",
		position = 3,
		section = shadowsSection
	)
	default Color shadowsBorderColour()
	{
		return new Color(0, 255, 255, 100);
	}

	@Alpha
	@ConfigItem(
		keyName = "shadowsColour",
		name = "Fill color",
		description = "Fill color of the shadow tiles",
		position = 4,
		section = shadowsSection
	)
	default Color shadowsColour()
	{
		return new Color(0, 255, 255, 50);
	}

	// Spores

	@ConfigItem(
		keyName = "highlightSpores",
		name = "Highlight spores",
		description = "Mark the area around spores that will make you yawn",
		position = 0,
		section = sporesSection
	)
	default boolean highlightSpores()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "poisonBorderCol",
		name = "Border color",
		description = "Outline color of the spore area",
		position = 1,
		section = sporesSection
	)
	default Color poisonBorderCol()
	{
		return new Color(255, 0, 0, 100);
	}

	@Alpha
	@ConfigItem(
		keyName = "poisonCol",
		name = "Fill color",
		description = "Fill color of the spore area",
		position = 2,
		section = sporesSection
	)
	default Color poisonCol()
	{
		return new Color(255, 0, 0, 50);
	}

	@ConfigItem(
		keyName = "yawnInfoBox",
		name = "Yawn infobox",
		description = "Show an infobox counting down until you stop yawning",
		position = 3,
		section = sporesSection
	)
	default boolean yawnInfoBox()
	{
		return true;
	}

	// Parasites

	@ConfigItem(
		keyName = "showTicksUntilParasite",
		name = "Show parasite ticks",
		description = "Show a tick timer on every player carrying a parasite (green on you once it's weakened)",
		position = 0,
		section = parasitesSection
	)
	default boolean showTicksUntilParasite()
	{
		return true;
	}

	@ConfigItem(
		keyName = "parasitesInfoBox",
		name = "Parasite infobox",
		description = "Show an infobox counting down until the parasites burst out",
		position = 1,
		section = parasitesSection
	)
	default boolean parasitesInfoBox()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sanfewReminder",
		name = "Sanfew reminder",
		description = "Show a sanfew icon while you're carrying a parasite",
		position = 2,
		section = parasitesSection
	)
	default boolean sanfewReminder()
	{
		return true;
	}

	@ConfigItem(
		keyName = "flash",
		name = "Flash when impregnated",
		description = "Flash the screen red when the Nightmare gives you a parasite",
		position = 3,
		section = parasitesSection
	)
	default boolean flash()
	{
		return false;
	}

	// Husks

	@ConfigItem(
		keyName = "highlightHusk",
		name = "Highlight husks",
		description = "Outline the magic (cyan) and ranged (green) husks",
		position = 0,
		section = huskSection
	)
	default boolean huskHighlight()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightHuskTarget",
		name = "Highlight husk targets",
		description = "Mark the players the husks will spawn on",
		position = 1,
		section = huskSection
	)
	default boolean highlightHuskTarget()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "huskBorderCol",
		name = "Border color",
		description = "Outline color of the husk target tiles",
		position = 2,
		section = huskSection
	)
	default Color huskBorderCol()
	{
		return new Color(255, 0, 0, 100);
	}

	// Charge

	@ConfigItem(
		keyName = "highlightNightmareHitboxOnCharge",
		name = "Highlight hitbox on charge",
		description = "Outline the Nightmare's tiles when she charges",
		position = 0,
		section = chargeSection
	)
	default boolean highlightNightmareHitboxOnCharge()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightNightmareChargeRange",
		name = "Highlight charge range",
		description = "Mark the tiles her charge will hit",
		position = 1,
		section = chargeSection
	)
	default boolean highlightNightmareChargeRange()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "nightmareChargeBorderCol",
		name = "Border color",
		description = "Outline color of the charge area",
		position = 2,
		section = chargeSection
	)
	default Color nightmareChargeBorderCol()
	{
		return new Color(255, 0, 0, 100);
	}

	@Alpha
	@ConfigItem(
		keyName = "nightmareChargeCol",
		name = "Fill color",
		description = "Fill color of the charge area",
		position = 3,
		section = chargeSection
	)
	default Color nightmareChargeCol()
	{
		return new Color(255, 0, 0, 50);
	}

	// Curse

	@ConfigItem(
		keyName = "fixCursedPrayers",
		name = "Unshuffle cursed prayers",
		description = "While cursed, rearrange the protection prayers so each button is back where you expect. "
			+ "Needs RuneLite's Prayer plugin on. Your own prayer order is restored when the curse ends",
		position = 0,
		section = curseSection
	)
	default boolean fixCursedPrayers()
	{
		return true;
	}

	// Appearance

	@Range(min = 1, max = 5)
	@ConfigItem(
		keyName = "borderWidth",
		name = "Border width",
		description = "Thickness of tile outlines",
		position = 0,
		section = appearanceSection
	)
	default int borderWidth()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "font",
		name = "Font",
		description = "Font used for the overlay text",
		position = 1,
		section = appearanceSection
	)
	default FontType font()
	{
		return FontType.BOLD.withSize(16);
	}
}
