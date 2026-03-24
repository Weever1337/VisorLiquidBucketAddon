package dev.wvr.visor.liquidbacket.fabric;

import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;
import dev.wvr.visor.liquidbacket.core.client.AddonClient;
import dev.wvr.visor.liquidbacket.core.network.NetworkHelper;
import dev.wvr.visor.liquidbacket.core.server.VRLiquidBucketServer;
import dev.wvr.visor.liquidbacket.fabric.network.FabricNetworkChannel;
import net.fabricmc.api.ModInitializer;

public class VRLiquidBucketFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        NetworkHelper.setChannel(new FabricNetworkChannel());
        
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