package com.playerInterpolation;

import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.overlay.Overlay;
import java.awt.*;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

class PlayerInterpolationOverlay extends Overlay
{
    private Client client;
    private ClientThread clientThread;
    private PlayerInterpolationPlugin plugin;
    private PlayerInterpolationConfig config;
    private ModelOutlineRenderer outlineRenderer;

    RuneLiteObject playerModel;

    private long lastnano;
    private float delta;
    private float posProgress;
    private float rotProgress;
    private float rawTime;
    private boolean wasMoving;

    PlayerInterpolationOverlay(Client client, ClientThread clientThread, PlayerInterpolationPlugin plugin, PlayerInterpolationConfig config, ModelOutlineRenderer outlineRenderer)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.plugin = plugin;
        this.config = config;
        this.outlineRenderer = outlineRenderer;

        lastnano = System.nanoTime();
        delta = 0;
        posProgress = 0;
        rotProgress = 0;
        rawTime = 0;
        wasMoving = false;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        updateDelta();
        posProgress += delta * (1000f / config.durationMS());
        rotProgress += delta * (1000f / config.rotationMS());
        rawTime += delta;

        if (plugin.isMoving() || wasMoving || (config.useOutline() && config.alwaysShowOutline()))
        {
            wasMoving = plugin.isMoving();

            if (playerModel != null)
            {
                playerModel.setActive(false);
            }
            playerModel = client.createRuneLiteObject();

            Player actor = client.getLocalPlayer();
            LocalPoint pos = plugin.getPosition(posProgress);

            int rot = plugin.getRotation(rotProgress, rawTime);
            if (config.rawRotation())
            {
                rot = actor.getCurrentOrientation();
            }

            Model model = client.mergeModels(actor.getModel());
            playerModel.setModel(model);

            playerModel.setLocation(pos, actor.getWorldView().getPlane());
            playerModel.setOrientation(rot);

            if (!playerModel.isActive() && !config.useOutline())
            {
                playerModel.setActive(true);
                plugin.setPlayerVisibility(false);
            }

            if (config.useOutline())
            {
                if (playerModel.isActive())
                {
                    playerModel.setActive(false);
                }
                plugin.setPlayerVisibility(true);
                outlineRenderer.drawOutline(playerModel, config.outlineWidth(), config.outlineColour(), config.outlineFeather());
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
        rawTime = 0;
    }

    public void shutdown()
    {
        clientThread.invoke(() ->
        {
            playerModel.setActive(false);
            plugin.setPlayerVisibility(true);
        });
    }

    public int getRotation()
    {
        if (playerModel == null) {
            return 0;
        }

        return playerModel.getOrientation();
    }

    private void updateDelta()
    {
        long nano = System.nanoTime();
        delta = (nano - lastnano) / 1000000000f;
        lastnano = nano;
    }
}
