package com.playerInterpolation;

import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.Player;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import java.awt.*;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

class PlayerInterpolationOverlay extends Overlay
{
    private Client client;
    private PlayerInterpolationPlugin plugin;
    private PlayerInterpolationConfig config;

    RuneLiteObject playerModel;


    private long lastnano;
    private float delta;
    private float progress;

    PlayerInterpolationOverlay(Client client, PlayerInterpolationPlugin plugin, PlayerInterpolationConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        lastnano = System.nanoTime();
        delta = 0;
        progress = 0;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        updateDelta();
        progress += delta * (1000f / config.durationMS());

        if (plugin.isMoving())
        {
            if (playerModel == null)
            {
                playerModel = client.createRuneLiteObject();
            }

            Player actor = client.getLocalPlayer();
            LocalPoint pos = plugin.getPosition(progress);

            Model model = client.mergeModels(client.getLocalPlayer().getModel());
            playerModel.setModel(model);

            playerModel.setLocation(pos, actor.getWorldView().getPlane());
            playerModel.setOrientation(actor.getCurrentOrientation());

            if (!playerModel.isActive())
            {
                playerModel.setActive(true);
                plugin.setPlayerVisibility(false);
            }
        }
        else
        {
            if (playerModel != null && playerModel.isActive())
            {
                playerModel.setActive(false);
                plugin.setPlayerVisibility(true);
            }
        }

        return null;
    }

    public void onTick()
    {
        progress = 0;
    }

    private void updateDelta()
    {
        long nano = System.nanoTime();
        delta = (nano - lastnano) / 1000000000f;
        lastnano = nano;
    }
}
