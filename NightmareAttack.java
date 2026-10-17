package net.runelite.client.plugins.nightmare;

import java.awt.Color;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.Prayer;
import net.runelite.api.gameval.AnimationID;

@AllArgsConstructor
@Getter
enum NightmareAttack
{
	MELEE(AnimationID.NIGHTMARE_ATTACK_MELEE, Prayer.PROTECT_FROM_MELEE, Color.RED),
	MAGIC(AnimationID.NIGHTMARE_ATTACK_MAGIC, Prayer.PROTECT_FROM_MAGIC, Color.CYAN),
	RANGE(AnimationID.NIGHTMARE_ATTACK_RANGED, Prayer.PROTECT_FROM_MISSILES, Color.GREEN),
	// While cursed, the prayer buttons are shuffled, so each attack needs a different button.
	CURSE_MELEE(AnimationID.NIGHTMARE_ATTACK_MELEE, Prayer.PROTECT_FROM_MISSILES, Color.GREEN),
	CURSE_MAGIC(AnimationID.NIGHTMARE_ATTACK_MAGIC, Prayer.PROTECT_FROM_MELEE, Color.RED),
	CURSE_RANGE(AnimationID.NIGHTMARE_ATTACK_RANGED, Prayer.PROTECT_FROM_MAGIC, Color.CYAN);

	private final int animation;
	private final Prayer prayer;
	private final Color tickColor;
}
