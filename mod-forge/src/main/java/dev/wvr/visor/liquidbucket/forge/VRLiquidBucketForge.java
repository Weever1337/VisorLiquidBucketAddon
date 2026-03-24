package dev.wvr.visor.liquidbucket.forge;

import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;
import dev.wvr.visor.liquidbucket.core.client.AddonClient;
import dev.wvr.visor.liquidbucket.core.common.FallingFluidManager;
import dev.wvr.visor.liquidbucket.core.common.VRLiquidBucket;
import dev.wvr.visor.liquidbucket.core.common.network.NetworkHelper;
import dev.wvr.visor.liquidbucket.core.server.VRLiquidBucketServer;
import dev.wvr.visor.liquidbucket.forge.network.ForgeNetworkChannel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(VRLiquidBucket.MOD_ID)
public class VRLiquidBucketForge {
    public VRLiquidBucketForge(){
        NetworkHelper.setChannel(new ForgeNetworkChannel(ResourceLocation.parse(VRLiquidBucket.MOD_ID + ":network")));
        MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);
        
        if(ModLoader.get().isDedicatedServer()){
            VisorAPI.registerAddon(
                    new VRLiquidBucketServer()
            );
        }else{
            VisorAPI.registerAddon(
                    new AddonClient()
            );
        }
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            FallingFluidManager.tickServer(event.getServer());
        }
    }

    private void onServerStopped(ServerStoppedEvent event) {
        FallingFluidManager.clear();
    }
}
