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
	String interpolationSection = "Interpolation";

	@ConfigSection(
			name = "Outline",
			description = "Outline Settings",
			position = 1,
			closedByDefault = false
	)
	String outlineSection = "Outline";

	@ConfigItem(
			keyName = "duration",
			name = "Duration",
			description = "How long (in milliseconds) it should take to finish the interpolation",
			section = interpolationSection,
			position = 0
	)
	default int durationMS()
	{
		return 600;
	}

	@ConfigItem(
			keyName = "rawRotation",
			name = "Raw Rotation",
			description = "Instead of overriding with interpolating rotation, use default rotation",
			section = interpolationSection,
			position = 4
	)
	default boolean rawRotation() { return false; }

	@ConfigItem(
			keyName = "rotationTime",
			name = "Rotation Time",
			description = "How long (in milliseconds) it should take to rotate",
			section = interpolationSection,
			position = 1
	)
	default int rotationMS() { return 600; }

	@ConfigItem(
			keyName = "useSteering",
			name = "Use Steering",
			description = "Make rotation faster for short rotations and longer for large rotations, instead of taking the same amount of time regardless. Should feel more consistent.",
			section = interpolationSection,
			position = 2
	)
	default boolean useSteering() { return true; }

	@ConfigItem(
			keyName = "steeringRate",
			name = "Steering Rate",
			description = "How many degrees per second to rotate when using steering",
			section = interpolationSection,
			position = 3
	)
	default int steeringRate() { return 360; }

	@ConfigItem(
			keyName = "useOutline",
			name = "Use Outline",
			description = "Instead of moving the player model, keep the original position and show an interpolated outline",
			section = outlineSection,
			position = 0
	)
	default boolean useOutline() { return false; }

	@ConfigItem(
			keyName = "alwaysShowOutline",
			name = "Always Show Outline",
			description = "Keep showing the outline even when not moving",
			section = outlineSection,
			position = 1
	)
	default boolean alwaysShowOutline() { return false; }

	@Alpha
	@ConfigItem(
			keyName = "outlineColour",
			name = "Outline Color",
			description = "Color of the outline",
			section = outlineSection,
			position = 1
	)
	default Color outlineColour()
	{
		return new Color(0, 0, 0, 175);
	}

	@ConfigItem(
			keyName = "outlineWidth",
			name = "Outline Width",
			description = "How wide (in pixels) should the outline be",
			section = outlineSection,
			position = 2
	)
	default int outlineWidth() { return 2; }

	@ConfigItem(
			keyName = "outlineFeather",
			name = "Outline Feather",
			description = "How much fade should the outline have",
			section = outlineSection,
			position = 3
	)
	default int outlineFeather() { return 0; }
}
