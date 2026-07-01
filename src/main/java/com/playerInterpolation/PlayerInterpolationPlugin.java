package com.playerInterpolation;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
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
	private PlayerInterpolationConfig config;

	@Inject
	private RenderCallbackManager renderCallbackManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject private ModelOutlineRenderer modelOutlineRenderer;

	private PlayerInterpolationOverlay playerInterpolationOverlay;

	private LocalPoint previousTrueTile = null;
	private LocalPoint currentTrueTile = null;

	@Override
	protected void startUp() throws Exception
	{
		playerInterpolationOverlay = new PlayerInterpolationOverlay(client, this, config, modelOutlineRenderer);
		overlayManager.add(playerInterpolationOverlay);

		renderCallbackManager.register(this);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(playerInterpolationOverlay);
		renderCallbackManager.unregister(this);
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		previousTrueTile = currentTrueTile;
		currentTrueTile = getTrueTile();

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

		if (object.getId() != localPlayer.getId())
			return true;

		return !isMoving();
	}

	private LocalPoint getTrueTile()
	{
		WorldPoint worldPoint = client.getLocalPlayer().getWorldLocation();

		if (worldPoint == null)
		{
			return null;
		}

		return LocalPoint.fromWorld(client.getLocalPlayer().getWorldView(), worldPoint);
	}

	public boolean isMoving()
	{
		if (previousTrueTile == null && currentTrueTile == null)
			return false;

		if (previousTrueTile == null || currentTrueTile == null)
			return true;

		return previousTrueTile.getX() != currentTrueTile.getX() || previousTrueTile.getY() != currentTrueTile.getY();
	}

	public LocalPoint getPosition(float percent)
	{
		if (previousTrueTile == null)
		{
			return currentTrueTile;
		}

		int dx = currentTrueTile.getX() - previousTrueTile.getX();
		int dy = currentTrueTile.getY() - previousTrueTile.getY();

		float x = lerp(0, dx, percent);
		float y = lerp(0, dy, percent);

		return previousTrueTile.plus(Math.round(x), Math.round(y));
	}

	private float lerp(float a, float b, float t)
	{
		return a * (t - 1) + b * t;
	}

	@Provides
	PlayerInterpolationConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PlayerInterpolationConfig.class);
	}
}
