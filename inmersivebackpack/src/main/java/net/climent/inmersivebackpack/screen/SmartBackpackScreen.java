package net.climent.inmersivebackpack.screen;

import net.climent.inmersivebackpack.client.GuideScreen;
import net.climent.inmersivebackpack.inventory.BackpackInventory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

/** Pantalla de la mochila (solo cliente). Se dibuja por codigo, sin textura de GUI. */
public class SmartBackpackScreen extends HandledScreen<BackpackScreenHandler> {

    private static final int OUTLINE = 0xFF000000;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_SHADOW = 0xFF555555;
    private static final int PANEL_FACE = 0xFFC6C6C6;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_LIGHT = 0xFFFFFFFF;
    private static final int SLOT_FACE = 0xFF8B8B8B;
    private static final int SLOT_FACE_LANTERN = 0xFF7F93AA; // azul: farol
    private static final int SLOT_FACE_BEDROLL = 0xFFAA8F7F; // rojizo: manta
    private static final int LABEL_COLOR = 0x404040;

    public SmartBackpackScreen(BackpackScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = BackpackScreenHandler.BACKGROUND_WIDTH;
        this.backgroundHeight = BackpackScreenHandler.BACKGROUND_HEIGHT;
        this.playerInventoryTitleY = BackpackScreenHandler.PLAYER_INV_Y - 10;
    }

    @Override
    protected void init() {
        super.init();
        // Boton "?" que abre la guia de controles
        this.addDrawableChild(ButtonWidget.builder(Text.literal("?"),
                        button -> this.client.setScreen(new GuideScreen(this)))
                .dimensions(this.x + this.backgroundWidth - 20, this.y + 4, 14, 12)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x0 = this.x;
        int y0 = this.y;
        int w = this.backgroundWidth;
        int h = this.backgroundHeight;

        context.fill(x0, y0, x0 + w, y0 + h, OUTLINE);
        context.fill(x0 + 1, y0 + 1, x0 + w - 1, y0 + h - 1, PANEL_LIGHT);
        context.fill(x0 + 3, y0 + 3, x0 + w - 1, y0 + h - 1, PANEL_SHADOW);
        context.fill(x0 + 3, y0 + 3, x0 + w - 3, y0 + h - 3, PANEL_FACE);

        for (Slot slot : this.handler.slots) {
            int face = SLOT_FACE;
            if (slot.inventory instanceof BackpackInventory) {
                if (BackpackInventory.isLanternSlot(slot.getIndex())) {
                    face = SLOT_FACE_LANTERN;
                } else if (BackpackInventory.isBedrollSlot(slot.getIndex())) {
                    face = SLOT_FACE_BEDROLL;
                }
            }
            drawSlotBackground(context, x0 + slot.x - 1, y0 + slot.y - 1, face);
        }
    }

    private void drawSlotBackground(DrawContext context, int x, int y, int face) {
        context.fill(x, y, x + 18, y + 18, SLOT_LIGHT);
        context.fill(x, y, x + 17, y + 17, SLOT_DARK);
        context.fill(x + 1, y + 1, x + 17, y + 17, face);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);

        int labelY = BackpackScreenHandler.EXTRAS_Y + 5;
        Text lantern = Text.translatable("container.inmersivebackpack.lantern");
        Text bedroll = Text.translatable("container.inmersivebackpack.bedroll");

        context.drawText(this.textRenderer, lantern,
                BackpackScreenHandler.GRID_X + BackpackScreenHandler.SLOT_SIZE + 6, labelY, LABEL_COLOR, false);

        int bedrollSlotX = BackpackScreenHandler.GRID_X + 8 * BackpackScreenHandler.SLOT_SIZE;
        context.drawText(this.textRenderer, bedroll,
                bedrollSlotX - 4 - this.textRenderer.getWidth(bedroll), labelY, LABEL_COLOR, false);
    }
}
