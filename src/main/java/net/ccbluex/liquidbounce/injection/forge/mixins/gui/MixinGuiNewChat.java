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
    
    @Shadow private int f; 

    @Shadow private abstracta void setChatLine(IChatComponent p_146237_1_, int p_146237_2_, int p_146237_3_, boolean p_146237_4_);

    @Redirect(method = {"getChatComponent", "drawChat"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/FontRenderer;FONT_HEIGHT:I"))
    private int iFC(FontRenderer i) {
        return HUD.INSTANCE.shouldModifyChatFont() ? Fonts.fontSemibold40.getHeight() : i.FONT_HEIGHT;
    }

    @Redirect(method = "drawChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I"))
    private int iFCB(FontRenderer i, String t, float x, float y, int c) {
        return HUD.INSTANCE.shouldModifyChatFont() ? Fonts.fontSemibold40.drawStringWithShadow(t, x, y, c) : i.drawStringWithShadow(t, x, y, c);
    }

    @Redirect(method = "getChatComponent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I"))
    private int iFCC(FontRenderer i, String t) {
        return HUD.INSTANCE.shouldModifyChatFont() ? Fonts.fontSemibold40.getStringWidth(t) : i.getStringWidth(t);
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

            int nci = ++this.f; 
            
            this.setChatLine(cpt, nci, mc.ingameGUI.getUpdateCounter(), false); 
            
            m.put(mk, new int[]{c, nci});
        }
    }

    @Redirect(method = "setChatLine", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 0))
    private int hNLL(List<ChatLine> l) {
        final ChatControl cc = ChatControl.INSTANCE;

        if (cc.handleEvents() && cc.getNoLengthLimit()) {
            return -1; 
        } 
        return l.size(); 
    } 
    
    @Inject(method = "clearChatMessages", at = @At("HEAD"), cancellable = true) 
    private void hCC(CallbackInfo ci) { 
        final ChatControl cc = ChatControl.INSTANCE; 
        if (cc.handleEvents() && cc.getNoChatClear()) { 
            ci.cancel(); 
        } 
    }
}