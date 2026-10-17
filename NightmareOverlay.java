package net.runelite.client.plugins.nightmare;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.geometry.Geometry;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

@Singleton
class NightmareOverlay extends Overlay
{
	private static final long FLASH_MILLIS = 1000;
	private static final Color FLASH_COLOR = new Color(255, 0, 0, 70);
	private static final int NIGHTMARE_SIZE = 5;
	private static final int SPORE_AREA = 3;

	private final Client client;
	private final NightmarePlugin plugin;
	private final NightmareConfig config;
	private final ModelOutlineRenderer outliner;

	private Stroke stroke;
	private int strokeWidth = -1;

	@Inject
	private NightmareOverlay(Client client, NightmarePlugin plugin, NightmareConfig config, ModelOutlineRenderer outliner)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.outliner = outliner;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_LOW);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		WorldView wv = client.getTopLevelWorldView();
		if (wv == null || !wv.isInstance() || !plugin.isInFight())
		{
			return null;
		}

		graphics.setFont(config.font().getFont());
		graphics.setStroke(getStroke(config.borderWidth()));

		NPC nm = plugin.getNm();

		if (config.highlightShadows())
		{
			renderShadows(graphics);
			if (plugin.isShadowsSpawning() && nm != null)
			{
				renderPoly(graphics, Perspective.getCanvasTileAreaPoly(client, nm.getLocalLocation(), NIGHTMARE_SIZE), config.shadowsBorderColour(), null);
			}
		}

		if (nm != null && plugin.isNightmareCharging())
		{
			if (config.highlightNightmareHitboxOnCharge())
			{
				renderPoly(graphics, Perspective.getCanvasTileAreaPoly(client, nm.getLocalLocation(), NIGHTMARE_SIZE), config.nightmareChargeBorderCol(), null);
			}

			if (config.highlightNightmareChargeRange())
			{
				renderChargeRange(graphics, nm, wv);
			}
		}

		int ticksUntilNext = plugin.getTicksUntilNextAttack();
		if (config.ticksCounter() && ticksUntilNext > 0 && nm != null)
		{
			NightmareAttack nextAttack = plugin.getPendingNightmareAttack();
			Color color = ticksUntilNext >= 4 && nextAttack != null ? nextAttack.getTickColor() : Color.WHITE;
			renderText(graphics, nm.getLocalLocation(), Integer.toString(ticksUntilNext), color);
		}

		int ticksUntilParasite = plugin.getTicksUntilParasite();
		if (config.showTicksUntilParasite() && ticksUntilParasite > 0)
		{
			String text = Integer.toString(ticksUntilParasite);
			for (Player player : plugin.getParasiteTargets().values())
			{
				Color color = !plugin.isParasite() && player == client.getLocalPlayer() ? Color.GREEN : Color.RED;
				renderText(graphics, player.getLocalLocation(), text, color);
			}
		}

		if (config.highlightTotems())
		{
			for (MemorizedTotem totem : plugin.getTotems().values())
			{
				TotemPhase phase = totem.getCurrentPhase();
				if (phase != null && phase.isActive())
				{
					outliner.drawOutline(totem.getNpc(), config.totemOutlineSize(), phase.getColor(), 0);
				}
			}
		}

		if (config.highlightSpores())
		{
			renderSpores(graphics);
		}

		if (config.highlightHuskTarget())
		{
			for (Player target : plugin.getHuskTargets())
			{
				renderPoly(graphics, target.getCanvasTilePoly(), config.huskBorderCol(), null);
			}
		}

		if (config.huskHighlight())
		{
			renderHusks(graphics, wv);
		}

		if (config.flash() && plugin.getFlashStartMillis() > 0)
		{
			// Time-based so the flash lasts the same at any frame rate
			if (System.currentTimeMillis() - plugin.getFlashStartMillis() < FLASH_MILLIS)
			{
				graphics.setColor(FLASH_COLOR);
				graphics.fill(new Rectangle(client.getCanvas().getSize()));
			}
			else
			{
				plugin.clearFlash();
			}
		}

		return null;
	}

	private void renderShadows(Graphics2D graphics)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}

		WorldPoint playerLocation = player.getWorldLocation();
		String ticks = Integer.toString(plugin.getShadowsTicks());

		for (GraphicsObject shadow : plugin.getShadows())
		{
			LocalPoint lp = shadow.getLocation();
			if (WorldPoint.fromLocal(client, lp).distanceTo(playerLocation) > config.shadowsRenderDistance())
			{
				continue;
			}

			renderPoly(graphics, Perspective.getCanvasTilePoly(client, lp), config.shadowsBorderColour(), config.shadowsColour());

			if (config.shadowsTickCounter())
			{
				renderText(graphics, lp, ticks, Color.WHITE);
			}
		}
	}

	private void renderSpores(Graphics2D graphics)
	{
		if (plugin.getSpores().isEmpty())
		{
			return;
		}

		Area area = new Area();
		for (LocalPoint point : plugin.getSpores().keySet())
		{
			Polygon poly = Perspective.getCanvasTileAreaPoly(client, point, SPORE_AREA);
			if (poly != null)
			{
				area.add(new Area(poly));
			}
		}

		renderPoly(graphics, area, config.poisonBorderCol(), config.poisonCol());
	}

	private void renderHusks(Graphics2D graphics, WorldView wv)
	{
		for (NPC npc : wv.npcs())
		{
			Color color;
			switch (npc.getId())
			{
				case NpcID.NIGHTMARE_HUSK_MAGIC:
				case NpcID.NIGHTMARE_CHALLENGE_HUSK_MAGIC:
					color = Color.CYAN;
					break;
				case NpcID.NIGHTMARE_HUSK_RANGED:
				case NpcID.NIGHTMARE_CHALLENGE_HUSK_RANGED:
					color = Color.GREEN;
					break;
				default:
					continue;
			}

			if (!npc.isDead())
			{
				renderPoly(graphics, npc.getConvexHull(), color, null);
			}
		}
	}

	/** The lane her charge will hit, worked out from which side of the arena she charges from. */
	private void renderChargeRange(Graphics2D graphics, NPC nm, WorldView wv)
	{
		LocalPoint lp = nm.getLocalLocation();
		int x = lp.getX();
		int y = lp.getY();
		final int half = 256 + 64; // half the Nightmare's width plus a margin, in local units

		// At the gates there's an extra row of dangerous tiles
		int offset = (x == 6208 || x == 7232) ? 2048 : 1792;

		Polygon lane = new Polygon();
		if (x == 5312 || x == 6336) // facing west
		{
			lane.addPoint(x + offset + half, y + half);
			lane.addPoint(x - half, y + half);
			lane.addPoint(x - half, y - half);
			lane.addPoint(x + offset + half, y - half);
		}
		else if (x == 7104 || x == 8128) // facing east
		{
			lane.addPoint(x + half, y + half);
			lane.addPoint(x - offset - half, y + half);
			lane.addPoint(x - offset - half, y - half);
			lane.addPoint(x + half, y - half);
		}
		else if (y == 8000 || y == 8128 || y == 9024 || y == 9152) // facing north
		{
			lane.addPoint(x + half, y + half);
			lane.addPoint(x - half, y + half);
			lane.addPoint(x - half, y - offset - half);
			lane.addPoint(x + half, y - offset - half);
		}
		else if (y == 6080 || y == 6208 || y == 7104 || y == 7232) // facing south
		{
			lane.addPoint(x + half, y + offset + half);
			lane.addPoint(x - half, y + offset + half);
			lane.addPoint(x - half, y - half);
			lane.addPoint(x + half, y - half);
		}
		else
		{
			return;
		}

		int plane = wv.getPlane();
		GeneralPath path = new GeneralPath(new Area(lane));
		path = Geometry.filterPath(path, (p1, p2) ->
			toCanvas(wv, p1[0], p1[1], plane) != null && toCanvas(wv, p2[0], p2[1], plane) != null);
		path = Geometry.transformPath(path, coords ->
		{
			Point point = toCanvas(wv, coords[0], coords[1], plane);
			if (point != null)
			{
				coords[0] = point.getX();
				coords[1] = point.getY();
			}
		});

		renderPoly(graphics, path, config.nightmareChargeBorderCol(), config.nightmareChargeCol());
	}

	private Point toCanvas(WorldView wv, double x, double y, int plane)
	{
		return Perspective.localToCanvas(client, new LocalPoint((int) x, (int) y, wv), plane);
	}

	/** Fill first, then the border, like the other [J] plugins. Either color can be null. */
	private void renderPoly(Graphics2D graphics, Shape shape, Color border, Color fill)
	{
		if (shape == null)
		{
			return;
		}

		if (fill != null)
		{
			graphics.setColor(fill);
			graphics.fill(shape);
		}

		if (border != null)
		{
			graphics.setColor(border);
			graphics.draw(shape);
		}
	}

	private void renderText(Graphics2D graphics, LocalPoint lp, String text, Color color)
	{
		if (lp == null)
		{
			return;
		}

		Point point = Perspective.getCanvasTextLocation(client, graphics, lp, text, 0);
		if (point != null)
		{
			// OverlayUtil already draws a black 1px shadow under the text.
			OverlayUtil.renderTextLocation(graphics, point, text, color);
		}
	}

	private Stroke getStroke(int width)
	{
		if (stroke == null || strokeWidth != width)
		{
			stroke = new BasicStroke(width);
			strokeWidth = width;
		}
		return stroke;
	}
}
