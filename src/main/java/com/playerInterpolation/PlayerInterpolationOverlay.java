package com.playerInterpolation;

import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import java.awt.*;
import java.awt.image.BufferedImage;

import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

import static net.runelite.api.HitsplatID.*;

class PlayerInterpolationOverlay extends Overlay
{
    private Client client;
    private ClientThread clientThread;
    private PlayerInterpolationPlugin plugin;
    private PlayerInterpolationConfig config;
    private ModelOutlineRenderer outlineRenderer;
    private SpriteManager spriteManager;

    RuneLiteObject playerModel;

    private long lastnano;
    private float delta;
    private float posProgress;
    private float rotProgress;
    private float rawTime;
    private boolean wasMoving;

    PlayerInterpolationOverlay(Client client, ClientThread clientThread, PlayerInterpolationPlugin plugin, PlayerInterpolationConfig config, ModelOutlineRenderer outlineRenderer, SpriteManager spriteManager)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.plugin = plugin;
        this.config = config;
        this.outlineRenderer = outlineRenderer;
        this.spriteManager = spriteManager;

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

            int z = Perspective.getFootprintTileHeight(client, pos, actor.getWorldView().getPlane(), actor.getFootprintSize()) - actor.getAnimationHeightOffset();
            playerModel.setZ(z);

            if (!playerModel.isActive() && !config.useOutline())
            {
                playerModel.setActive(true);
                plugin.setPlayerVisibility(false);

                drawOverheadPrayer(actor, playerModel, graphics);
                drawHealthbar(actor, playerModel, graphics);
                drawSkull(actor, playerModel, graphics);
                drawText(actor, playerModel, graphics);
                drawHitsplats(actor, playerModel, graphics);
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

    private void drawOverheadPrayer(Player actor, RuneLiteObject playerModel, Graphics2D graphics)
    {
        HeadIcon icon = actor.getOverheadIcon();

        if (icon == null)
            return;

        int id = getOverheadId(icon);
        if (id == -1)
            return;

        BufferedImage image = spriteManager.getSprite(440, id);
        if (image == null)
            return;

        Point pos = Perspective.localToCanvas(client, playerModel.getLocation(), actor.getWorldView().getPlane(), actor.getLogicalHeight() + 20);
        pos = new Point(pos.getX() - image.getWidth() / 2 - 5, pos.getY() - image.getHeight() / 2 - 64);

        graphics.drawImage(image, pos.getX(), pos.getY(), null);
    }

    private void drawHealthbar(Player actor, RuneLiteObject playerModel, Graphics2D graphics)
    {
        int ratio = actor.getHealthRatio();
        int scale = actor.getHealthScale();

        if (scale <= -1 || scale <= 0)
            return;

        int width = 30;
        int height = 5;
        int fill = (int) (((float) ratio / scale) * width);

        Point pos = Perspective.localToCanvas(client, playerModel.getLocation(), actor.getWorldView().getPlane(), actor.getLogicalHeight() + 20);
        pos = new Point(pos.getX() - width / 2, pos.getY() - height / 2);

        graphics.setColor(new Color(255, 0, 0, 255));
        graphics.fillRect(pos.getX(), pos.getY(), width, height);

        graphics.setColor(new Color(0, 255, 0, 255));
        graphics.fillRect(pos.getX(), pos.getY(), fill, height);
    }

    private void drawSkull(Player actor, RuneLiteObject playerModel, Graphics2D graphics)
    {
        int id = actor.getSkullIcon();

        if (id == -1)
            return;

        BufferedImage image = spriteManager.getSprite(439, id);
        if (image == null)
            return;

        Point pos = Perspective.localToCanvas(client, playerModel.getLocation(), actor.getWorldView().getPlane(), actor.getLogicalHeight() + 20);
        pos = new Point(pos.getX() - image.getWidth() / 2 - 5, pos.getY() - image.getHeight() / 2 - 64);

        graphics.drawImage(image, pos.getX(), pos.getY(), null);
    }

    private void drawText(Player actor, RuneLiteObject playerModel, Graphics2D graphics)
    {
        String text = actor.getOverheadText();
        if (text == null || text.isEmpty())
            return;

        graphics.setFont(FontManager.getRunescapeFont().deriveFont(Font.BOLD));
        FontMetrics fontMetrics = graphics.getFontMetrics();

        int width = fontMetrics.stringWidth(text);
        int height = fontMetrics.getHeight();

        Point pos = Perspective.localToCanvas(client, playerModel.getLocation(), actor.getWorldView().getPlane(), actor.getLogicalHeight() + 20);
        pos = new Point(pos.getX() - width / 2 - 1, pos.getY() - height / 2 - 6);

        OverlayUtil.renderTextLocation(graphics, pos, text, Color.YELLOW);
    }

    private void drawHitsplats(Player actor, RuneLiteObject playerModel, Graphics2D graphics)
    {
        var hitsplats = plugin.getHitsplats();

        // draw most important last
        for (int i = Math.min(3, hitsplats.size() - 1); i >= 0; i--)
        {
            var current = hitsplats.get(i);

            if (current == null)
                continue;

            String damage = Integer.toString(current.amount);
            var image = getHitsplatImage(current.type);

            Point pos = Perspective.localToCanvas(client, playerModel.getLocation(), actor.getWorldView().getPlane(), actor.getLogicalHeight() / 2);

            if (pos != null && image != null)
            {
                FontMetrics metrics = graphics.getFontMetrics();
                int textWidth = metrics.stringWidth(damage);
                int textHeight = metrics.getHeight();

                // todo: handle offsets with multiple hitsplats

                graphics.drawImage(image, pos.getX(), pos.getY(), null);

                graphics.setColor(Color.black);
                graphics.drawString(damage, pos.getX() + 1, pos.getY() + 1);

                graphics.setColor(Color.white);
                graphics.drawString(damage, pos.getX(), pos.getY());
            }
        }
    }

    private int getOverheadId(HeadIcon icon)
    {
        return icon.ordinal();
    }

    private BufferedImage getHitsplatImage(int id)
    {
        // todo: 1628, 1362, 1363, 1634, other types

        switch (id)
        {
            case DAMAGE_ME:
                return spriteManager.getSprite(1359, 0);
            case BLOCK_ME:
                return spriteManager.getSprite(1358, 0);
            case DAMAGE_OTHER:
                return spriteManager.getSprite(1631, 0);
            case BLOCK_OTHER:
                return spriteManager.getSprite(1630, 0);
            case DAMAGE_ME_POISE:
            case POISON:
                return spriteManager.getSprite(1360, 0);
            case DISEASE:
                return spriteManager.getSprite(1633, 0);
            case VENOM:
                return spriteManager.getSprite(1632, 0);
            case DAMAGE_OTHER_POISE:
                return spriteManager.getSprite(2245, 0);
            case HEAL:
                return spriteManager.getSprite(1629, 0);

        }

        // default to damage
        return spriteManager.getSprite(1359, 0);
    }
}
