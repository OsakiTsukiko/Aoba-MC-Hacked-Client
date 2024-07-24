package net.aoba.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.aoba.Aoba;
import net.aoba.misc.Render2D;
import net.aoba.module.modules.render.Tooltips;
import com.mojang.blaze3d.systems.RenderSystem;
import net.aoba.utils.render.TextureBank;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.item.map.MapState;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.tools.Tool;
import java.awt.*;
import java.util.List;
import java.util.Objects;

import static net.aoba.AobaClient.MC;

@Mixin(value = {HandledScreen.class})
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen implements ScreenHandlerProvider<T> {
    @Unique
    private Runnable postRender;

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    @Shadow
    @Nullable
    protected Slot focusedSlot;
    @Shadow
    protected int x;
    @Shadow
    protected int y;

    private boolean hasItems(ItemStack itemStack) {
        ContainerComponent compoundTag = itemStack.get(DataComponentTypes.CONTAINER);
        return compoundTag != null;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Tooltips tooltips = (Tooltips) Aoba.getInstance().moduleManager.tooltips;

        if (tooltips.getState() && focusedSlot != null && !focusedSlot.getStack().isEmpty() && client.player.playerScreenHandler.getCursorStack().isEmpty()) {
            if (hasItems(focusedSlot.getStack()) && tooltips.getStorage()) {
                renderShulkerToolTip(context, mouseX, mouseY, 0, 0, focusedSlot.getStack());
            } else if (focusedSlot.getStack().getItem() == Items.FILLED_MAP && tooltips.getMap()) {
                drawMapPreview(context, focusedSlot.getStack(), mouseX, mouseY);
            }
        }
    }

    public boolean renderShulkerToolTip(DrawContext context, int offsetX, int offsetY, int mouseX, int mouseY, ItemStack stack) {
        try {
            ContainerComponent compoundTag = stack.get(DataComponentTypes.CONTAINER);
            if (compoundTag == null)
                return false;

            float[] colors = new float[]{1F, 1F, 1F};
            Item focusedItem = stack.getItem();
            if (focusedItem instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock) {
                try {
                    Color c = new Color(Objects.requireNonNull(ShulkerBoxBlock.getColor(stack.getItem())).getEntityColor());
                    colors = new float[]{c.getRed() / 255f, c.getGreen() / 255f, c.getRed() / 255f, c.getAlpha() / 255f};
                } catch (NullPointerException npe) {
                    colors = new float[]{1F, 1F, 1F};
                }
            }

            int tooltipWidth = 150;
            int nameHeight = 12;

            RenderSystem.disableDepthTest();
            Render2D.drawStringWithScale(context, stack.getName().getString(), offsetX + 10, offsetY - 10, new Color(255, 255, 255).getRGB(), 1.0f);
            RenderSystem.enableDepthTest();

            draw(context, compoundTag.stream().toList(), offsetX, offsetY + nameHeight, mouseX, mouseY, colors);

            RenderSystem.disableDepthTest();
            context.fill(offsetX, offsetY - nameHeight, offsetX + tooltipWidth, offsetY, new Color(0, 0, 0, 128).getRGB());
            Render2D.drawStringWithScale(context, stack.getName().getString(), offsetX + 5, offsetY - 10, new Color(255, 255, 255).getRGB(), 1.0f);
            RenderSystem.enableDepthTest();

        } catch (Exception ignore) {
            return false;
        }
        return true;
    }


    @Unique
    private void draw(DrawContext context, List<ItemStack> itemStacks, int offsetX, int offsetY, int mouseX, int mouseY, float[] colors) {
        RenderSystem.disableDepthTest();
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        offsetX += 8;
        offsetY -= 82;

        drawBackground(context, offsetX, offsetY, colors);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        DiffuseLighting.enableGuiDepthLighting();
        int row = 0;
        int i = 0;
        for (ItemStack itemStack : itemStacks) {
            context.drawItem(itemStack, offsetX + 8 + i * 18, offsetY + 7 + row * 18);
            context.drawItemInSlot(MC.textRenderer, itemStack, offsetX + 8 + i * 18, offsetY + 7 + row * 18);

            if (mouseX > offsetX + 8 + i * 18 && mouseX < offsetX + 28 + i * 18 && mouseY > offsetY + 7 + row * 18 && mouseY < offsetY + 27 + row * 18)
                postRender = () -> context.drawTooltip(textRenderer, getTooltipFromItem(MC, itemStack), itemStack.getTooltipData(), mouseX, mouseY);

            i++;
            if (i >= 9) {
                i = 0;
                row++;
            }
        }
        DiffuseLighting.disableGuiDepthLighting();
        RenderSystem.enableDepthTest();
    }

    private void drawBackground(DrawContext context, int x, int y, float[] colors) {
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(colors[0], colors[1], colors[2], 1F);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        context.drawTexture(TextureBank.container, x, y, 0, 0, 176, 67, 176, 67);
        RenderSystem.enableBlend();
    }

    private void drawMapPreview(DrawContext context, ItemStack stack, int x, int y) {
        RenderSystem.enableBlend();
        context.getMatrices().push();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int y1 = y - 12;
        int x1 = x + 8;
        int z;

        MapState mapState = FilledMapItem.getMapState(stack, client.world);

        if (mapState != null) {
            mapState.getPlayerSyncData(client.player);

            x1 += 8;
            y1 += 8;
            z = 310;
            double scale = (double) (100 - 16) / 128.0D;
            context.getMatrices().translate(x1, y1, z);
            context.getMatrices().scale((float) scale, (float) scale, 0);
            VertexConsumerProvider.Immediate consumer = client.getBufferBuilders().getEntityVertexConsumers();
            client.gameRenderer.getMapRenderer().draw(context.getMatrices(), consumer, (MapIdComponent) stack.get(DataComponentTypes.MAP_ID), mapState, false, 0xF000F0);
        }
        context.getMatrices().pop();
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void onDrawMouseoverTooltip(DrawContext context, int x, int y, CallbackInfo ci) {
        Tooltips tooltips = (Tooltips) Aoba.getInstance().moduleManager.tooltips;

        if (focusedSlot != null && !focusedSlot.getStack().isEmpty() && client.player.playerScreenHandler.getCursorStack().isEmpty()) {
            if (focusedSlot.getStack().getItem() == Items.FILLED_MAP && tooltips.getState() && tooltips.getMap()) ci.cancel();
            else if (focusedSlot.getStack().getItem() instanceof BlockItem && ((BlockItem) focusedSlot.getStack().getItem()).getBlock() instanceof ShulkerBoxBlock && tooltips.getState() && tooltips.getStorage()) ci.cancel();
        }
    }
}