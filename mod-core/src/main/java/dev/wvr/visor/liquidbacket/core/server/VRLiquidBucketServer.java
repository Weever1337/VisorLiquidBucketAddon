package dev.wvr.visor.liquidbacket.core.server;

import dev.wvr.visor.liquidbacket.core.common.AddonNetworking;
import dev.wvr.visor.liquidbacket.core.common.VRLiquidBucket;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vmstudio.visor.api.common.addon.VisorAddon;

public class VRLiquidBucketServer implements VisorAddon {
    @Override
    public void onAddonLoad() {
        AddonNetworking.initCommon();
    }

    @Override
    public @Nullable String getAddonPackagePath() {
        return "dev.wvr.visor.liquidbacket.core.server";
    }

    @Override
    public @NotNull String getAddonId() {
        return VRLiquidBucket.MOD_ID;
    }

    @Override
    public @NotNull Component getAddonName() {
        return Component.literal(VRLiquidBucket.MOD_NAME);
    }

    @Override
    public String getModId() {
        return VRLiquidBucket.MOD_ID;
    }
}
