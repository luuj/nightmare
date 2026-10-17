package net.runelite.client.plugins.nightmare;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

/** Red sanfew icon shown while you're carrying a parasite, as a reminder to drink one. */
@Singleton
class SanfewInfoBox extends Overlay
{
	private static final Color BACKGROUND = new Color(150, 0, 0, 150);

	private final NightmarePlugin plugin;
	private final NightmareConfig config;
	private final ItemManager itemManager;
	private final PanelComponent panel = new PanelComponent();

	@Inject
	private SanfewInfoBox(NightmarePlugin plugin, ItemManager itemManager, NightmareConfig config)
	{
		this.plugin = plugin;
		this.itemManager = itemManager;
		this.config = config;
		panel.setBackgroundColor(BACKGROUND);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGH);
		setPosition(OverlayPosition.BOTTOM_RIGHT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.sanfewReminder() || !plugin.isInFight() || !plugin.isParasite())
		{
			return null;
		}

		panel.getChildren().clear();
		panel.getChildren().add(new ImageComponent(itemManager.getImage(ItemID.SANFEW_SALVE_4_DOSE)));
		return panel.render(graphics);
	}
}
