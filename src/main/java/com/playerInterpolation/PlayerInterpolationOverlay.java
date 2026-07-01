package com.playerInterpolation;

import net.runelite.api.Client;
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
    private ModelOutlineRenderer renderer;


    private long lastnano;
    private float delta;
    private float progress;

    PlayerInterpolationOverlay(Client client, PlayerInterpolationPlugin plugin, PlayerInterpolationConfig config, ModelOutlineRenderer renderer)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.renderer = renderer;

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
            Player actor = client.getLocalPlayer();

            RuneLiteObject outlineObject = client.createRuneLiteObject();
            outlineObject.setModel(actor.getModel());

            LocalPoint pos = plugin.getPosition(progress);
            outlineObject.setLocation(pos, actor.getWorldView().getPlane());

            outlineObject.setOrientation(actor.getCurrentOrientation());
            renderer.drawOutline(outlineObject, 1, new Color(0, 0, 0, 255), 1);
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
