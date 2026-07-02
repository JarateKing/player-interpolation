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
    private float posProgress;
    private float rotProgress;

    PlayerInterpolationOverlay(Client client, PlayerInterpolationPlugin plugin, PlayerInterpolationConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        lastnano = System.nanoTime();
        delta = 0;
        posProgress = 0;
        rotProgress = 0;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        updateDelta();
        posProgress += delta * (1000f / config.durationMS());
        rotProgress += delta * (1000f / config.rotationMS());

        if (plugin.isMoving())
        {
            if (playerModel == null)
            {
                playerModel = client.createRuneLiteObject();
            }

            Player actor = client.getLocalPlayer();
            LocalPoint pos = plugin.getPosition(posProgress);
            int rot = plugin.getRotation(rotProgress);

            Model model = client.mergeModels(client.getLocalPlayer().getModel());
            playerModel.setModel(model);

            playerModel.setLocation(pos, actor.getWorldView().getPlane());
            playerModel.setOrientation(rot);

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
        posProgress = 0;
        rotProgress = 0;
    }

    public int getRotation()
    {
        System.out.println("Getting: " + playerModel.getOrientation());
        return playerModel.getOrientation();
    }

    private void updateDelta()
    {
        long nano = System.nanoTime();
        delta = (nano - lastnano) / 1000000000f;
        lastnano = nano;
    }
}
