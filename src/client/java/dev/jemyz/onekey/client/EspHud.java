package dev.jemyz.onekey.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** CS-style 2D boxes around other players, drawn on the HUD (so they show through walls). */
public final class EspHud {
	private static final int COLOR_VISIBLE = 0xFFFFFFFF;
	private static final int COLOR_INVISIBLE = 0xFFC77DFF;
	private static final int OUTLINE = 0xC0000000;

	private EspHud() {
	}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
		if (!OneKeyClient.boxesOn()) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;

		float pt = deltaTracker.getGameTimeDeltaPartialTick(false);
		Projection projection = Projection.current(mc, pt, g.guiWidth(), g.guiHeight());
		if (projection == null) return;

		Font font = mc.font;
		Vec3 eye = mc.player.getEyePosition(pt);

		for (Player other : mc.level.players()) {
			if (other == mc.player || other.isSpectator() || !other.isAlive()) continue;

			double dx = Mth.lerp(pt, other.xo, other.getX()) - other.getX();
			double dy = Mth.lerp(pt, other.yo, other.getY()) - other.getY();
			double dz = Mth.lerp(pt, other.zo, other.getZ()) - other.getZ();
			AABB box = other.getBoundingBox().move(dx, dy, dz).inflate(0.08);

			float[] rect = projection.screenRect(box);
			if (rect == null) continue;

			int x0 = Math.round(rect[0]);
			int y0 = Math.round(rect[1]);
			int x1 = Math.round(rect[2]);
			int y1 = Math.round(rect[3]);
			if (x1 - x0 < 2 || y1 - y0 < 3) continue;

			boolean invisible = other.isInvisible();
			int color = invisible ? COLOR_INVISIBLE : COLOR_VISIBLE;

			frame(g, x0, y0, x1, y1, color);
			healthBar(g, other, x0 - 4, y0, y1);

			int cx = (x0 + x1) / 2;
			String name = other.getName().getString();
			int dist = (int) Math.round(eye.distanceTo(other.position()));
			text(g, font, Component.literal(name), cx, y0 - 10, color);
			text(g, font, Component.literal(dist + "m" + (invisible ? "  [INV]" : "")), cx, y1 + 3, 0xFFD0D0D0);
		}
	}

	/** 1 px coloured rectangle with a dark 1 px outline on both sides, like CS. */
	private static void frame(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
		rect(g, x0 - 1, y0 - 1, x1 + 1, y1 + 1, OUTLINE);
		rect(g, x0 + 1, y0 + 1, x1 - 1, y1 - 1, OUTLINE);
		rect(g, x0, y0, x1, y1, color);
	}

	private static void rect(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
		g.fill(x0, y0, x1, y0 + 1, color);
		g.fill(x0, y1 - 1, x1, y1, color);
		g.fill(x0, y0 + 1, x0 + 1, y1 - 1, color);
		g.fill(x1 - 1, y0 + 1, x1, y1 - 1, color);
	}

	private static void healthBar(GuiGraphicsExtractor g, Player p, int x, int y0, int y1) {
		float frac = Mth.clamp(p.getHealth() / Math.max(1f, p.getMaxHealth()), 0f, 1f);
		int h = y1 - y0;
		int filled = Math.round(h * frac);
		int r = (int) (255 * Math.min(1f, 2f * (1f - frac)));
		int gr = (int) (255 * Math.min(1f, 2f * frac));
		g.fill(x - 1, y0 - 1, x + 2, y1 + 1, OUTLINE);
		g.fill(x, y1 - filled, x + 1, y1, 0xFF000000 | (r << 16) | (gr << 8) | 0x20);
	}

	private static void text(GuiGraphicsExtractor g, Font font, Component text, int cx, int y, int color) {
		g.pose().pushMatrix();
		g.pose().translate(cx, y);
		g.pose().scale(0.75f, 0.75f);
		g.centeredText(font, text, 0, 0, color);
		g.pose().popMatrix();
	}
}
