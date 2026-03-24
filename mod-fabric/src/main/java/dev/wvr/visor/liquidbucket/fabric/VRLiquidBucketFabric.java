package dev.wvr.visor.liquidbucket.fabric;

import dev.wvr.visor.liquidbucket.core.common.FallingFluidManager;
import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;
import dev.wvr.visor.liquidbucket.core.client.AddonClient;
import dev.wvr.visor.liquidbucket.core.common.network.NetworkHelper;
import dev.wvr.visor.liquidbucket.core.server.VRLiquidBucketServer;
import dev.wvr.visor.liquidbucket.fabric.network.FabricNetworkChannel;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class VRLiquidBucketFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        NetworkHelper.setChannel(new FabricNetworkChannel());
        ServerTickEvents.END_SERVER_TICK.register(FallingFluidManager::tickServer);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> FallingFluidManager.clear());
        
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
