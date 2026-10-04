package dev.jemyz.onekey.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** "Enter the server key" window shown when joining a OneKey server. */
public final class KeyScreen extends Screen {
	private EditBox box;
	private Button activate;
	private Component status = Component.empty();
	private int statusColor = 0xFFA0A0A0;
	private String typed = "";

	public KeyScreen() {
		super(Component.translatable("onekey.screen.title"));
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int cy = this.height / 2;

		this.box = new EditBox(this.font, cx - 90, cy - 10, 180, 20, Component.translatable("onekey.screen.field"));
		this.box.setMaxLength(16);
		this.box.setValue(this.typed);
		this.box.setResponder(value -> this.typed = value);
		this.addRenderableWidget(this.box);
		this.setInitialFocus(this.box);

		this.activate = this.addRenderableWidget(Button.builder(Component.translatable("onekey.screen.activate"), b -> submit())
				.bounds(cx - 90, cy + 18, 88, 20)
				.build());
		this.addRenderableWidget(Button.builder(Component.translatable("onekey.screen.later"), b -> this.onClose())
				.bounds(cx + 2, cy + 18, 88, 20)
				.build());
	}

	private void submit() {
		String key = this.box.getValue().trim();
		if (key.isEmpty()) return;
		OneKeyClient.submit(key, false);
		setStatus(Component.translatable("onekey.screen.checking"), 0xFFA0A0A0);
	}

	void setStatus(Component text, int color) {
		this.status = text;
		this.statusColor = color;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		// Screen draws the (blurred) background itself; drawing it twice crashes ("Can only blur once per frame")
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		int cx = this.width / 2;
		int cy = this.height / 2;
		graphics.centeredText(this.font, this.title.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA), cx, cy - 48, 0xFFFFFFFF);
		graphics.centeredText(this.font, Component.translatable("onekey.screen.hint"), cx, cy - 32, 0xFFC0C0C0);
		graphics.centeredText(this.font, this.status, cx, cy + 46, this.statusColor);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
