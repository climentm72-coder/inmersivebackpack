package net.climent.inmersivebackpack.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Guia de controles del mod: que tecla o clic hace cada cosa. */
public class GuideScreen extends Screen {

    private record Entry(Text keys, Text desc) {
    }

    private final Screen parent;
    private final List<Entry> entries = new ArrayList<>();

    public GuideScreen(Screen parent) {
        super(Text.translatable("guide.inmersivebackpack.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        entries.clear();
        Text openKeys = Text.empty()
                .append(InmersiveBackpackClient.openKey.getBoundKeyLocalizedText())
                .append(Text.literal("  /  "))
                .append(Text.translatable("guide.inmersivebackpack.right_click"));
        entries.add(new Entry(openKeys, Text.translatable("guide.inmersivebackpack.open")));
        entries.add(new Entry(Text.translatable("guide.inmersivebackpack.keys.quickmove"),
                Text.translatable("guide.inmersivebackpack.quickmove")));
        entries.add(new Entry(Text.translatable("guide.inmersivebackpack.keys.lantern"),
                Text.translatable("guide.inmersivebackpack.lantern")));
        entries.add(new Entry(Text.translatable("guide.inmersivebackpack.keys.bedroll"),
                Text.translatable("guide.inmersivebackpack.bedroll")));
        entries.add(new Entry(Text.translatable("guide.inmersivebackpack.keys.sleep"),
                Text.translatable("guide.inmersivebackpack.sleep")));
        entries.add(new Entry(Text.translatable("guide.inmersivebackpack.keys.wake"),
                Text.translatable("guide.inmersivebackpack.wake")));
        entries.add(new Entry(Text.translatable("guide.inmersivebackpack.keys.craft"),
                Text.translatable("guide.inmersivebackpack.craft")));
        entries.add(new Entry(InmersiveBackpackClient.guideKey.getBoundKeyLocalizedText(),
                Text.translatable("guide.inmersivebackpack.guide")));

        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> this.close())
                .dimensions(this.width / 2 - 50, this.height - 26, 100, 20).build());
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);

        int keyWidth = 120;
        int descWidth = 230;
        int left = this.width / 2 - (keyWidth + descWidth + 10) / 2;
        int y = 28;
        for (Entry entry : entries) {
            List<OrderedText> keyLines = this.textRenderer.wrapLines(entry.keys(), keyWidth);
            List<OrderedText> descLines = this.textRenderer.wrapLines(entry.desc(), descWidth);
            int rows = Math.max(keyLines.size(), descLines.size());
            for (int i = 0; i < keyLines.size(); i++) {
                context.drawTextWithShadow(this.textRenderer, keyLines.get(i), left, y + i * 10, 0xFFD27F);
            }
            for (int i = 0; i < descLines.size(); i++) {
                context.drawTextWithShadow(this.textRenderer, descLines.get(i), left + keyWidth + 10, y + i * 10, 0xFFFFFF);
            }
            y += rows * 10 + 5;
        }
        super.render(context, mouseX, mouseY, delta);
    }
}
