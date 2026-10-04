package dev.jemyz.onekey.client;

import org.joml.Matrix4f;

import net.minecraft.client.Minecraft;

/** The projection matrix of the last rendered frame. */
final class CameraCapture {
	private static volatile Matrix4f captured;

	private CameraCapture() {
	}

	static void capture(Matrix4f projection) {
		captured = new Matrix4f(projection);
	}

	static Matrix4f projection() {
		Matrix4f m = captured;
		if (m != null) return m;

		// fallback: plain perspective from the FOV setting
		Minecraft mc = Minecraft.getInstance();
		float fov = mc.options.fov().get();
		float aspect = (float) mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
		return new Matrix4f().perspective((float) Math.toRadians(fov), aspect, 0.05f, 1024f);
	}
}
