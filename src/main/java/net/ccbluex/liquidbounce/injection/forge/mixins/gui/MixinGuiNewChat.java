/*
 * RinBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/rattermc/rinbounce69
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.features.module.modules.misc.ChatControl;
import net.ccbluex.liquidbounce.features.module.modules.render.HUD;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

import static net.ccbluex.liquidbounce.utils.client.MinecraftInstance.mc;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat {

    private final Map<String, int[]> m = new LinkedHashMap<String, int[]>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, int[]> e) {
            return size() > 100;
        }
    };
    
    private int customChatLineIdCounter = 0;

    @Shadow protected abstract void setChatLine(IChatComponent p_146237_1_, int p_146237_2_, int p_146237_3_, boolean p_146237_4_);


    @Redirect(method = "drawChat", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/FontRenderer;FONT_HEIGHT:I"))
    private int injectFontChat(FontRenderer instance) {
        return HUD.INSTANCE.shouldModifyChatFont() ? Fonts.fontSemibold40.getHeight() : instance.FONT_HEIGHT;
    }

    @Redirect(method = "drawChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I"))
    private int injectFontChatB(FontRenderer instance, String text, float x, float y, int color) {
        return HUD.INSTANCE.shouldModifyChatFont() ? Fonts.fontSemibold40.drawStringWithShadow(text, x, y, color) : instance.drawStringWithShadow(text, x, y, color);
    }

    @Redirect(method = "getChatComponent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I"))
    private int injectFontChatC(FontRenderer instance, String text) {
        return HUD.INSTANCE.shouldModifyChatFont() ? Fonts.fontSemibold40.getStringWidth(text) : instance.getStringWidth(text);
    }

    @Inject(method = "printChatMessage", at = @At("HEAD"), cancellable = true)
    public void onPrintChatMessage(IChatComponent chatComponent, CallbackInfo ci) {
        String rawMessage = chatComponent.getFormattedText(); 
        String messageKey = rawMessage;

        if (ChatControl.INSTANCE.handleEvents() && ChatControl.INSTANCE.getStackMessage()) {
            int[] messageData = m.getOrDefault(messageKey, new int[]{0, -1});
            int currentCount = messageData[0] + 1;
            int lastChatLineID = messageData[1];

            IChatComponent componentToPrint;
            
            if (currentCount > 1) {
                if (lastChatLineID != -1) {
                    mc.ingameGUI.getChatGUI().deleteChatLine(lastChatLineID);
                }
                String modifiedMessage = rawMessage + " " + EnumChatFormatting.GRAY + "[" + currentCount + "x]";
                componentToPrint = new ChatComponentText(modifiedMessage);
            } else {
                componentToPrint = chatComponent;
            }

            ci.cancel();

            int newChatLineID = ++this.customChatLineIdCounter;
            
            this.setChatLine(componentToPrint, newChatLineID, mc.ingameGUI.getUpdateCounter(), false); 
            
            m.put(messageKey, new int[]{currentCount, newChatLineID});
        }
    }

    @Redirect(method = "setChatLine", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 0))
    private int hookNoLengthLimit(List<ChatLine> list) {
        final ChatControl chatControl = ChatControl.INSTANCE;

        if (chatControl.handleEvents() && chatControl.getNoLengthLimit()) {
            return -1; 
        } 
        return list.size(); 
    } 
    
    @Inject(method = "clearChatMessages", at = @At("HEAD"), cancellable = true) 
    private void hookChatClear(CallbackInfo ci) { 
        final ChatControl chatControl = ChatControl.INSTANCE; 
        if (chatControl.handleEvents() && chatControl.getNoChatClear()) { 
            ci.cancel(); 
        } 
    }
}