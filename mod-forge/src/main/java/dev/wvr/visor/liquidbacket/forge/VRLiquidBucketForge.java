package dev.wvr.visor.liquidbacket.forge;

import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;
import dev.wvr.visor.liquidbacket.core.client.AddonClient;
import dev.wvr.visor.liquidbacket.core.common.VRLiquidBucket;
import dev.wvr.visor.liquidbacket.core.network.NetworkHelper;
import dev.wvr.visor.liquidbacket.core.server.VRLiquidBucketServer;
import dev.wvr.visor.liquidbacket.forge.network.ForgeNetworkChannel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;

@Mod(VRLiquidBucket.MOD_ID)
public class VRLiquidBucketForge {
    public VRLiquidBucketForge(){
        NetworkHelper.setChannel(new ForgeNetworkChannel(ResourceLocation.parse(VRLiquidBucket.MOD_ID + ":network")));
        
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
}
