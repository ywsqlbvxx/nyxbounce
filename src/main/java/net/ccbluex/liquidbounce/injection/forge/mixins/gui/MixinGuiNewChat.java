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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo; 
  
 import java.util.LinkedHashMap;
 import java.util.Map;
 import java.util.List; 
 import java.util.Map; 
  
 import static net.ccbluex.liquidbounce.utils.client.MinecraftInstance.mc;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat {
    private final Map<String, int[]> m = new LinkedHashMap<String, int[]>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, int[]> e) {
            return size() > 100;
        }
    };
    
    @Redirect(method = {"getChatComponent", "drawChat"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/FontRenderer;FONT_HEIGHT:I"))
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
    public void onP(IChatComponent cc, CallbackInfo ci) {
        String rm = cc.getFormattedText(); 
        String mk = rm;

        if (ChatControl.INSTANCE.handleEvents() && ChatControl.INSTANCE.getStackMessage()) {
            int[] d = m.getOrDefault(mk, new int[]{0, -1});
            int c = d[0] + 1;
            int lpci = d[1];

            IChatComponent cpt;
            
            if (c > 1) {
                if (lpci != -1) {
                    mc.ingameGUI.getChatGUI().deleteChatLine(lpci);
                }
                String ms = rm + " " + EnumChatFormatting.GRAY + "[" + c + "x]";
                cpt = new ChatComponentText(ms);
            } else {
                cpt = cc;
            }

            ci.cancel();

            int nci = mc.ingameGUI.getChatGUI().printChatMessage(cpt);
            
            m.put(mk, new int[]{c, nci});
        }
    }
  
    @Inject(method = "clearChatMessages", at = @At("HEAD"), cancellable = true) 
    private void hookChatClear(CallbackInfo ci) { 
         final ChatControl chatControl = ChatControl.INSTANCE; 
  
         if (chatControl.handleEvents() && chatControl.getNoChatClear()) { 
             ci.cancel(); 
         } 
    }
}
