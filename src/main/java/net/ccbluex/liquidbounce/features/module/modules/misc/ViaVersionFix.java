package net.ccbluex.liquidbounce.features.module.modules.misc;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Type;
import de.gerrygames.viarewind.protocol.protocol1_8to1_9.Protocol1_8TO1_9;
import de.gerrygames.viarewind.utils.PacketUtil;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;

@ModuleInfo(name = "ViaVersionFix", category = ModuleCategory.MISC)
public class ViaVersionFix extends Module {

    public BoolValue blocking = new BoolValue("Blocking", false);
    public BoolValue place = new BoolValue("Placement", false);

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (blocking.get() && mc.thePlayer.isBlocking() && mc.thePlayer.getHeldItem() != null
                && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {

            if (!Via.getManager().getConnectionManager().getConnections().isEmpty()) {
                PacketWrapper useItem = PacketWrapper.create(
                        29, null, Via.getManager().getConnectionManager().getConnections().iterator().next()
                );
                useItem.write(Type.VAR_INT, 1);
                PacketUtil.sendToServer(useItem, Protocol1_8TO1_9.class, true, true);
            }

            mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()));
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (place.get()) {
            Packet<?> packet = event.getPacket();
            if (packet instanceof C08PacketPlayerBlockPlacement) {
                C08PacketPlayerBlockPlacement placement = (C08PacketPlayerBlockPlacement) packet;
                placement.facingX = 0.5F;
                placement.facingY = 0.5F;
                placement.facingZ = 0.5F;
            }
        }
    }
}
