package com.playerInterpolation;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.Hooks;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.Player;
import net.runelite.api.*;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

import java.awt.*;
import java.util.ArrayList;

@Slf4j
@PluginDescriptor(
	name = "Player Interpolation"
)
public class PlayerInterpolationPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private PlayerInterpolationConfig config;

	@Inject
	private Hooks hooks;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ModelOutlineRenderer outlineRenderer;

	@Inject
	private SpriteManager spriteManager;

	private PlayerInterpolationOverlay playerInterpolationOverlay;

	private LocalPoint previousTrueTile;
	private LocalPoint currentTrueTile;
	private boolean isDefaultVisible;
	private int previousRotation;
	private ArrayList<HitsplatObj> hitsplats;

	private final Hooks.RenderableDrawListener drawListener = this::drawObject;

	@Getter
	public static class HitsplatObj
	{
		public int amount;
		public int type;
		public int despawn;

		HitsplatObj(int amount, int type, int despawn)
		{
			this.amount = amount;
			this.type = type;
			this.despawn = despawn;
		}
	}

	@Override
	protected void startUp() throws Exception
	{
		playerInterpolationOverlay = new PlayerInterpolationOverlay(client, clientThread, this, config, outlineRenderer, spriteManager);
		overlayManager.add(playerInterpolationOverlay);

		hooks.registerRenderableDrawListener(drawListener);

		previousTrueTile = null;
		currentTrueTile = null;
		isDefaultVisible = true;
		previousRotation = 0;
		hitsplats = new ArrayList<>();
	}

	@Override
	protected void shutDown() throws Exception
	{
		playerInterpolationOverlay.shutdown();
		overlayManager.remove(playerInterpolationOverlay);
		hooks.unregisterRenderableDrawListener(drawListener);
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		previousTrueTile = currentTrueTile;
		currentTrueTile = getTrueTile();
		previousRotation = playerInterpolationOverlay.getRotation();

		playerInterpolationOverlay.onTick();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			previousTrueTile = getTrueTile();
			currentTrueTile = getTrueTile();
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (event.getActor() != client.getLocalPlayer())
			return;

		int dmg = event.getHitsplat().getAmount();
		int type = event.getHitsplat().getHitsplatType();
		int delay = event.getHitsplat().getDisappearsOnGameCycle();

		pruneOldHitsplats();
		hitsplats.add(new HitsplatObj(dmg, type, delay));
	}

	public ArrayList<HitsplatObj> getHitsplats()
	{
		pruneOldHitsplats();
		return hitsplats;
	}

	public boolean drawObject(Renderable renderable, boolean drawingUi)
	{
		// hide the local player, if the player is moving
		Player localPlayer = client.getLocalPlayer();

		if (localPlayer == null)
			return true;

		if (renderable != localPlayer)
			return true;

		return isDefaultVisible;
	}

	public void setPlayerVisibility(boolean isVisible)
	{
		isDefaultVisible = isVisible;
	}

	private LocalPoint getTrueTile()
	{
		if (client.getLocalPlayer() == null)
			return null;

		WorldPoint worldPoint = client.getLocalPlayer().getWorldLocation();

		if (worldPoint == null)
			return null;

		return LocalPoint.fromWorld(client.getLocalPlayer().getWorldView(), worldPoint);
	}

	public boolean isTileChanged()
	{
		if (previousTrueTile == null && currentTrueTile == null)
			return false;

		if (previousTrueTile == null || currentTrueTile == null)
			return true;

		return previousTrueTile.getX() != currentTrueTile.getX() || previousTrueTile.getY() != currentTrueTile.getY();
	}

	public boolean isMoving()
	{
		if (currentTrueTile == null)
			return false;

		LocalPoint visualTile = client.getLocalPlayer().getLocalLocation();

		return visualTile.getX() != currentTrueTile.getX() || visualTile.getY() != currentTrueTile.getY();
	}

	public LocalPoint getPosition(float percent)
	{
		if (previousTrueTile == null)
		{
			return currentTrueTile;
		}

		float t = clamp(percent, 0, 1);

		int dx = currentTrueTile.getX() - previousTrueTile.getX();
		int dy = currentTrueTile.getY() - previousTrueTile.getY();

		float x = lerp(0, dx, t);
		float y = lerp(0, dy, t);

		return previousTrueTile.plus(Math.round(x), Math.round(y));
	}

	public int getRotation(float percent, float rawTime)
	{
		if (previousTrueTile == null)
		{
			return 0;
		}

		float t = clamp(percent, 0, 1);

		int dx = currentTrueTile.getX() - previousTrueTile.getX();
		int dy = currentTrueTile.getY() - previousTrueTile.getY();

		if (dx == 0 && dy == 0)
			return previousRotation;

		int targetRotation = 0;

		if (dx == 0 && dy < 0)
			targetRotation = 0;
		if (dx < 0 && dy < 0)
			targetRotation = 256;
		if (dx < 0 && dy == 0)
			targetRotation = 512;
		if (dx < 0 && dy > 0)
			targetRotation = 768;
		if (dx == 0 && dy > 0)
			targetRotation = 1024;
		if (dx > 0 && dy > 0)
			targetRotation = 1280;
		if (dx > 0 && dy == 0)
			targetRotation = 1536;
		if (dx > 0 && dy < 0)
			targetRotation = 1792;

		int start = previousRotation;
		int end = targetRotation;
		int diff = Math.abs(end - start);
		if (diff > 1024)
		{
			if (end > start)
			{
				start += 2048;
			}
			else
			{
				end += 2048;
			}
		}

		if (config.useSteering())
		{
			float ans = start;
			if (start > end)
			{
				ans -= config.steeringRate() * (2048f / 360f) * rawTime;
				if (ans <= end)
					ans = end;
			}
			else
			{
				ans += config.steeringRate() * (2048f / 360f) * rawTime;
				if (ans >= end)
					ans = end;
			}

			return Math.round(ans) % 2048;
		}

		float ans = lerp(start, end, t);
		return Math.round(ans) % 2048;
	}

	private float lerp(float a, float b, float t)
	{
		return a * (1 - t) + b * t;
	}

	private float clamp(float v, float l, float h)
	{
		if (v > h)
			return h;
		if (v < l)
			return l;
		return v;
	}

	private void pruneOldHitsplats()
	{
		hitsplats.removeIf(x -> x.despawn <= client.getGameCycle());
	}

	@Provides
	PlayerInterpolationConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PlayerInterpolationConfig.class);
	}
}
