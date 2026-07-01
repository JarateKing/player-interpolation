package com.playerInterpolation;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PlayerInterpolationPluginTest
{
	public static void main(String[] args) throws Exception
	{
		try {
			ExternalPluginManager.loadBuiltin(PlayerInterpolationPlugin.class);
			RuneLite.main(args);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}