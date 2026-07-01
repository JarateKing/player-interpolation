package com.playerInterpolation;

import java.awt.*;

import net.runelite.client.config.*;

@ConfigGroup("playerInterpolation")
public interface PlayerInterpolationConfig extends Config
{
	@ConfigSection(
			name = "Interpolation",
			description = "Interpolation Settings",
			position = 0,
			closedByDefault = false
	)
	String panelSection = "Interpolation";

	@ConfigItem(
			keyName = "duration",
			name = "Duration",
			description = "How long (in milliseconds) it should take to finish the interpolation",
			section = panelSection
	)
	default int durationMS()
	{
		return 600;
	}
}
