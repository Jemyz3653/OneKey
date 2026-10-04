package dev.jemyz.onekey.client;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** World -> GUI coordinates using the camera of the frame being drawn. */
final class Projection {
	private final Matrix4f viewProj;
	private final Vec3 camPos;
	private final int guiW;
	private final int guiH;

	private Projection(Matrix4f viewProj, Vec3 camPos, int guiW, int guiH) {
		this.viewProj = viewProj;
		this.camPos = camPos;
		this.guiW = guiW;
		this.guiH = guiH;
	}

	static Projection current(Minecraft mc, float partialTick, int guiW, int guiH) {
		Camera camera = mc.gameRenderer.getMainCamera();
		if (camera == null) return null;

		Matrix4f proj = CameraCapture.projection();
		if (proj == null) return null;

		Matrix4f view = new Matrix4f().rotation(new Quaternionf(camera.rotation()).conjugate());
		Matrix4f viewProj = new Matrix4f(proj).mul(view);
		return new Projection(viewProj, camera.position(), guiW, guiH);
	}

	/** Bounding rectangle of the box on screen, or null if (partly) behind the camera. */
	float[] screenRect(AABB box) {
		float minX = Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE;
		float maxY = -Float.MAX_VALUE;
		Vector4f v = new Vector4f();

		for (int i = 0; i < 8; i++) {
			double x = (i & 1) == 0 ? box.minX : box.maxX;
			double y = (i & 2) == 0 ? box.minY : box.maxY;
			double z = (i & 4) == 0 ? box.minZ : box.maxZ;
			v.set((float) (x - camPos.x), (float) (y - camPos.y), (float) (z - camPos.z), 1f);
			viewProj.transform(v);

			if (v.w <= 0.05f) return null;

			float sx = (v.x / v.w * 0.5f + 0.5f) * guiW;
			float sy = (1f - (v.y / v.w * 0.5f + 0.5f)) * guiH;
			minX = Math.min(minX, sx);
			minY = Math.min(minY, sy);
			maxX = Math.max(maxX, sx);
			maxY = Math.max(maxY, sy);
		}

		if (maxX < 0 || maxY < 0 || minX > guiW || minY > guiH) return null;
		return new float[] {minX, minY, maxX, maxY};
	}
}
