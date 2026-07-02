package com.playerInterpolation;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.Player;
import net.runelite.api.*;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

import java.awt.*;

@Slf4j
@PluginDescriptor(
	name = "Player Interpolation"
)
public class PlayerInterpolationPlugin extends Plugin implements RenderCallback
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private PlayerInterpolationConfig config;

	@Inject
	private RenderCallbackManager renderCallbackManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ModelOutlineRenderer outlineRenderer;

	private PlayerInterpolationOverlay playerInterpolationOverlay;

	private LocalPoint previousTrueTile;
	private LocalPoint currentTrueTile;
	private boolean isDefaultVisible;
	private int previousRotation;

	@Override
	protected void startUp() throws Exception
	{
		playerInterpolationOverlay = new PlayerInterpolationOverlay(client, clientThread, this, config, outlineRenderer);
		overlayManager.add(playerInterpolationOverlay);

		renderCallbackManager.register(this);

		previousTrueTile = null;
		currentTrueTile = null;
		isDefaultVisible = true;
		previousRotation = 0;
	}

	@Override
	protected void shutDown() throws Exception
	{
		playerInterpolationOverlay.shutdown();
		overlayManager.remove(playerInterpolationOverlay);
		renderCallbackManager.unregister(this);
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

	@Override
	public boolean drawObject(Scene scene, TileObject object)
	{
		// hide the local player, if the player is moving
		Player localPlayer = client.getLocalPlayer();

		if (localPlayer == null)
			return true;

		if (object.getId() != localPlayer.getId())
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

	public int getRotation(float percent)
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

	@Provides
	PlayerInterpolationConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PlayerInterpolationConfig.class);
	}
}
