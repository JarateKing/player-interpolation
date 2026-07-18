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

import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

import static net.runelite.api.HitsplatID.*;
import net.runelite.api.gameval.SpriteID;

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

        setPosition(OverlayPosition.DYNAMIC);
        setPriority(PRIORITY_HIGH);
        setLayer(OverlayLayer.ABOVE_SCENE);
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

            if (playerModel.isActive() && !config.useOutline())
            {
                int zoffset = Perspective.getFootprintTileHeight(client, pos, actor.getWorldView().getPlane(), actor.getFootprintSize()) - actor.getLogicalHeight();
                Point canvasPoint = Perspective.localToCanvas(client, pos.getX(), pos.getY(), zoffset);

                canvasPoint = drawText(actor, playerModel, graphics, canvasPoint);
                canvasPoint = drawHealthbar(actor, playerModel, graphics, canvasPoint);
                canvasPoint = drawSkull(actor, playerModel, graphics, canvasPoint);
                canvasPoint = drawOverheadPrayer(actor, playerModel, graphics, canvasPoint);

                drawHitsplats(actor, playerModel, graphics);
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

    private Point drawOverheadPrayer(Player actor, RuneLiteObject playerModel, Graphics2D graphics, Point canvasPoint)
    {
        HeadIcon icon = actor.getOverheadIcon();

        if (icon == null)
            return canvasPoint;

        int id = getOverheadId(icon);

        if (id == -1)
            return canvasPoint;

        BufferedImage image = spriteManager.getSprite(440, id);
        if (image == null)
            return canvasPoint;

        Point pos = new Point(canvasPoint.getX() - image.getWidth() / 2, canvasPoint.getY() - image.getHeight());
        canvasPoint = new Point(canvasPoint.getX(), canvasPoint.getY() - image.getHeight());

        graphics.drawImage(image, pos.getX(), pos.getY(), null);

        return canvasPoint;
    }

    private Point drawHealthbar(Player actor, RuneLiteObject playerModel, Graphics2D graphics, Point canvasPoint)
    {
        int ratio = actor.getHealthRatio();
        int scale = actor.getHealthScale();

        if (ratio <= -1 || scale <= 0)
            return canvasPoint;

        int width = 30;
        int height = 5;
        int fill = (int) (Math.min((float) ratio / scale, 1.0f) * width);

        Point pos = new Point(canvasPoint.getX() - width / 2, canvasPoint.getY() - height / 2);
        canvasPoint = new Point(canvasPoint.getX(), canvasPoint.getY() - 4);

        graphics.setColor(new Color(255, 0, 0, 255));
        graphics.fillRect(pos.getX(), pos.getY(), width, height);

        graphics.setColor(new Color(0, 255, 0, 255));
        graphics.fillRect(pos.getX(), pos.getY(), fill, height);

        return canvasPoint;
    }

    private Point drawSkull(Player actor, RuneLiteObject playerModel, Graphics2D graphics, Point canvasPoint)
    {
        int id = actor.getSkullIcon();

        if (id == -1)
            return canvasPoint;

        BufferedImage image = spriteManager.getSprite(439, id);
        if (image == null)
            return canvasPoint;

        Point pos = new Point(canvasPoint.getX() - image.getWidth() / 2, canvasPoint.getY() - image.getHeight());
        canvasPoint = new Point(canvasPoint.getX(), canvasPoint.getY() - image.getHeight());

        graphics.drawImage(image, pos.getX(), pos.getY(), null);

        return canvasPoint;
    }

    private Point drawText(Player actor, RuneLiteObject playerModel, Graphics2D graphics, Point canvasPoint)
    {
        String text = actor.getOverheadText();

        if (text == null || text.isEmpty())
            return canvasPoint;

        graphics.setFont(FontManager.getRunescapeFont().deriveFont(Font.BOLD));
        FontMetrics fontMetrics = graphics.getFontMetrics();

        int width = fontMetrics.stringWidth(text);
        int height = fontMetrics.getHeight();

        Point pos = new Point(canvasPoint.getX() - width / 2 - 1, canvasPoint.getY() - height / 2 + 6);
        canvasPoint = new Point(canvasPoint.getX(), canvasPoint.getY() - 15);

        OverlayUtil.renderTextLocation(graphics, pos, text, Color.YELLOW);

        return canvasPoint;
    }

    private void drawHitsplats(Player actor, RuneLiteObject playerModel, Graphics2D graphics)
    {
        var hitsplats = plugin.getHitsplats();
        hitsplats.sort((a, b) -> {
            int ap = 1000 * (a.despawn - client.getGameCycle()) + a.amount;
            int bp = 1000 * (b.despawn - client.getGameCycle()) + b.amount;

            return ap - bp;
        });

        graphics.setFont(FontManager.getRunescapeSmallFont());

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

                // https://github.com/Thource/rl-nameplates-and-hitsplats/blob/main/src/main/java/dev/thource/runelite/nameplates/themes/hitsplats/OSRSDisplayType.java#L30-L35
                int xd = image.getWidth() / 2 + 4;
                int yd = image.getHeight() / 2 - 2;
                int xm = i == 2 ? -1 : i == 3 ? 1 : 0;
                int ym = i == 0 ? 1 : i == 1 ? -1 : 0;

                int x = pos.getX() + xm * xd;
                int y = pos.getY() + (int)((ym - 0.6f) * yd);
                int ix = x + textWidth / 2 - image.getWidth() / 2;
                int iy = y - textHeight + (textHeight - image.getHeight()) / 2;

                graphics.drawImage(image, ix, iy, null);

                graphics.setColor(Color.black);
                graphics.drawString(damage, x + 1, y + 1);
                graphics.setColor(Color.white);
                graphics.drawString(damage, x, y);
            }
        }
    }

    private int getOverheadId(HeadIcon icon)
    {
        return icon.ordinal();
    }

    private BufferedImage getHitsplatImage(int id)
    {
        // reference: https://github.com/Thource/rl-nameplates-and-hitsplats/blob/871ecc5e97a2c8261dd8c5432226514c41661da9/src/main/java/dev/thource/runelite/nameplates/themes/hitsplats/HitsplatDefaultSprite.java
        switch (id)
        {
            case BLOCK_ME:
                return spriteManager.getSprite(SpriteID.Hitmark._0, 0);
            case BLOCK_OTHER:
                return spriteManager.getSprite(SpriteID.Hitmark._9, 0);
            case DAMAGE_ME:
                return spriteManager.getSprite(SpriteID.Hitmark._1, 0);
            case DAMAGE_OTHER:
                return spriteManager.getSprite(SpriteID.Hitmark._10, 0);
            case POISON:
                return spriteManager.getSprite(SpriteID.Hitmark._2, 0);
            case DISEASE:
                return spriteManager.getSprite(SpriteID.Hitmark._3, 0);
            case DISEASE_BLOCKED:
                return spriteManager.getSprite(SpriteID.Hitmark._12, 0);
            case VENOM:
                return spriteManager.getSprite(SpriteID.Hitmark._11, 0);
            case HEAL:
                return spriteManager.getSprite(SpriteID.Hitmark._8, 0);
            case CYAN_UP:
                return spriteManager.getSprite(SpriteID.Hitmark._20, 0);
            case CYAN_DOWN:
                return spriteManager.getSprite(SpriteID.Hitmark._21, 0);
            case DAMAGE_ME_CYAN:
                return spriteManager.getSprite(SpriteID.Hitmark._6, 0);
            case DAMAGE_OTHER_CYAN:
                return spriteManager.getSprite(SpriteID.Hitmark._15, 0);
            case DAMAGE_ME_ORANGE:
                return spriteManager.getSprite(SpriteID.Hitmark._7, 0);
            case DAMAGE_OTHER_ORANGE:
                return spriteManager.getSprite(SpriteID.Hitmark._16, 0);
            case DAMAGE_ME_YELLOW:
                return spriteManager.getSprite(SpriteID.Hitmark._4, 0);
            case DAMAGE_OTHER_YELLOW:
                return spriteManager.getSprite(SpriteID.Hitmark._13, 0);
            case DAMAGE_ME_WHITE:
                return spriteManager.getSprite(SpriteID.Hitmark._5, 0);
            case DAMAGE_OTHER_WHITE:
                return spriteManager.getSprite(SpriteID.Hitmark._14, 0);
            case DAMAGE_MAX_ME:
                return spriteManager.getSprite(SpriteID.Hitmark._24, 0);
            case DAMAGE_MAX_ME_CYAN:
                return spriteManager.getSprite(SpriteID.Hitmark._27, 0);
            case DAMAGE_MAX_ME_ORANGE:
                return spriteManager.getSprite(SpriteID.Hitmark._28, 0);
            case DAMAGE_MAX_ME_YELLOW:
                return spriteManager.getSprite(SpriteID.Hitmark._25, 0);
            case DAMAGE_MAX_ME_WHITE:
                return spriteManager.getSprite(SpriteID.Hitmark._26, 0);
            case DAMAGE_ME_POISE:
                return spriteManager.getSprite(SpriteID.Hitmark._29, 0);
            case DAMAGE_OTHER_POISE:
                return spriteManager.getSprite(SpriteID.Hitmark._30, 0);
            case DAMAGE_MAX_ME_POISE:
                return spriteManager.getSprite(SpriteID.Hitmark._31, 0);
            case CORRUPTION:
                return spriteManager.getSprite(SpriteID.Hitmark._17, 0);
            case PRAYER_DRAIN:
                return spriteManager.getSprite(SpriteID.Hitmark._32, 0);
            case BLEED:
                return spriteManager.getSprite(SpriteID.Hitmark._35, 0);
            case SANITY_DRAIN:
                return spriteManager.getSprite(SpriteID.Hitmark._38, 0);
            case SANITY_RESTORE:
                return spriteManager.getSprite(SpriteID.Hitmark._39, 0);
            case DOOM:
                return spriteManager.getSprite(SpriteID.Hitmark._40, 0);
            case BURN:
                return spriteManager.getSprite(SpriteID.Hitmark._41, 0);
        }

        // default to damage
        return spriteManager.getSprite(SpriteID.Hitmark._1, 0);
    }
}
