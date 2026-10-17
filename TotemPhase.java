package net.runelite.client.plugins.nightmare;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.gameval.NpcID;

@AllArgsConstructor
@Getter
enum TotemPhase
{
	DORMANT(false, Color.RED),
	READY(true, Color.ORANGE),
	CHARGED(true, Color.GREEN);

	private final boolean active;
	private final Color color;

	private static final Map<Integer, TotemPhase> BY_NPC_ID = new HashMap<>();

	static
	{
		for (int id : new int[]{NpcID.NIGHTMARE_TOTEM_1_DORMANT, NpcID.NIGHTMARE_TOTEM_2_DORMANT, NpcID.NIGHTMARE_TOTEM_3_DORMANT, NpcID.NIGHTMARE_TOTEM_4_DORMANT})
		{
			BY_NPC_ID.put(id, DORMANT);
		}
		for (int id : new int[]{NpcID.NIGHTMARE_TOTEM_1_READY, NpcID.NIGHTMARE_TOTEM_2_READY, NpcID.NIGHTMARE_TOTEM_3_READY, NpcID.NIGHTMARE_TOTEM_4_READY})
		{
			BY_NPC_ID.put(id, READY);
		}
		for (int id : new int[]{NpcID.NIGHTMARE_TOTEM_1_CHARGED, NpcID.NIGHTMARE_TOTEM_2_CHARGED, NpcID.NIGHTMARE_TOTEM_3_CHARGED, NpcID.NIGHTMARE_TOTEM_4_CHARGED})
		{
			BY_NPC_ID.put(id, CHARGED);
		}
	}

	/** The phase for a totem NPC ID, or null if the ID isn't a totem. */
	static TotemPhase of(int npcId)
	{
		return BY_NPC_ID.get(npcId);
	}
}
