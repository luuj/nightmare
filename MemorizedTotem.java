package net.runelite.client.plugins.nightmare;

import lombok.Getter;
import net.runelite.api.NPC;

@Getter
class MemorizedTotem
{
	private final NPC npc;
	private TotemPhase currentPhase;

	MemorizedTotem(NPC npc)
	{
		this.npc = npc;
		updateCurrentPhase(npc.getId());
	}

	void updateCurrentPhase(int npcId)
	{
		TotemPhase phase = TotemPhase.of(npcId);
		if (phase != null)
		{
			currentPhase = phase;
		}
	}
}
