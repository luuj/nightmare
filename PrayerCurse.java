package net.runelite.client.plugins.nightmare;

import java.util.Arrays;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;

/**
 * The Nightmare's curse shuffles Protect from Magic, Missiles and Melee. This rearranges RuneLite's
 * prayer order (the core Prayer plugin's reordering) so the buttons stay where you expect while
 * cursed, then puts your own order back afterwards.
 *
 * Works with the stock Prayer plugin; it no longer needs PrayerReorder patched to be public.
 */
@Singleton
class PrayerCurse
{
	private static final String PRAYER_GROUP = "prayer";
	private static final String ORDER_KEY = "prayer_order_book_0";

	// Standard prayer book order, used when you haven't reordered your prayers.
	private static final int[] DEFAULT_ORDER = {0, 1, 2, 18, 19, 3, 8, 17, 15, 9, 20, 28, 16, 7, 10, 11, 4, 5, 6, 22, 23, 12, 13, 14, 21, 25, 26, 24, 27};

	// Prayer IDs within the standard book
	private static final int PROTECT_FROM_MAGIC = 12;
	private static final int PROTECT_FROM_MISSILES = 13;
	private static final int PROTECT_FROM_MELEE = 14;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	private boolean applied;
	/** Your order before the curse, or null if you were using the default order. */
	private String savedOrder;

	void apply()
	{
		if (applied)
		{
			return;
		}

		savedOrder = configManager.getConfiguration(PRAYER_GROUP, ORDER_KEY);
		int[] order = savedOrder != null ? parse(savedOrder) : DEFAULT_ORDER.clone();
		if (order == null)
		{
			return;
		}

		for (int i = 0; i < order.length; i++)
		{
			order[i] = cursed(order[i]);
		}

		configManager.setConfiguration(PRAYER_GROUP, ORDER_KEY, Arrays.stream(order).mapToObj(Integer::toString).collect(Collectors.joining(",")));
		applied = true;
		redraw();
	}

	void restore()
	{
		if (!applied)
		{
			return;
		}

		if (savedOrder == null)
		{
			configManager.unsetConfiguration(PRAYER_GROUP, ORDER_KEY);
		}
		else
		{
			configManager.setConfiguration(PRAYER_GROUP, ORDER_KEY, savedOrder);
		}

		applied = false;
		savedOrder = null;
		redraw();
	}

	boolean isApplied()
	{
		return applied;
	}

	private static int cursed(int prayerId)
	{
		switch (prayerId)
		{
			case PROTECT_FROM_MAGIC:
				return PROTECT_FROM_MELEE;
			case PROTECT_FROM_MISSILES:
				return PROTECT_FROM_MAGIC;
			case PROTECT_FROM_MELEE:
				return PROTECT_FROM_MISSILES;
			default:
				return prayerId;
		}
	}

	private static int[] parse(String s)
	{
		try
		{
			return Arrays.stream(s.split(",")).mapToInt(v -> Integer.parseInt(v.trim())).toArray();
		}
		catch (NumberFormatException ex)
		{
			return null;
		}
	}

	/** Makes the prayer tab rebuild, which is when the Prayer plugin applies the order. */
	private void redraw()
	{
		clientThread.invokeLater(() ->
		{
			Widget w = client.getWidget(InterfaceID.Prayerbook.UNIVERSE);
			if (w != null && w.getOnVarTransmitListener() != null)
			{
				client.runScript(w.getOnVarTransmitListener());
			}
		});
	}
}
