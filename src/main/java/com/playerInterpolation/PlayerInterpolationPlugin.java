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
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.Player;
import net.runelite.api.*;
import net.runelite.api.Renderable;

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

	private LocalPoint previousTrueTile = null;
	private LocalPoint currentTrueTile = null;

	@Override
	protected void startUp() throws Exception
	{
		renderCallbackManager.register(this);
	}

	@Override
	protected void shutDown() throws Exception
	{
		renderCallbackManager.unregister(this);
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		previousTrueTile = currentTrueTile;
		currentTrueTile = getTrueTile();
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

	private boolean isMoving()
	{
		if (previousTrueTile == null && currentTrueTile == null)
			return false;

		if (previousTrueTile == null || currentTrueTile == null)
			return true;

		return previousTrueTile.getX() != currentTrueTile.getX() || previousTrueTile.getY() != currentTrueTile.getY();
	}

	@Provides
	PlayerInterpolationConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PlayerInterpolationConfig.class);
	}
}
