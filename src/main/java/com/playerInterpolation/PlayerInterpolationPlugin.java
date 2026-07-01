package com.playerInterpolation;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Player Interpolation"
)
public class PlayerInterpolationPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private PlayerInterpolationConfig config;

	@Override
	protected void startUp() throws Exception
	{
	}

	@Override
	protected void shutDown() throws Exception
	{
	}

	@Subscribe
	public void onGameTick(GameTick tick) {
	}

	@Provides
	PlayerInterpolationConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PlayerInterpolationConfig.class);
	}
}
