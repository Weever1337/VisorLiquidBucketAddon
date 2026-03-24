package dev.wvr.visor.liquidbacket.core.common;

import dev.wvr.visor.liquidbacket.core.network.NetworkHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class AddonNetworking {
    public static final ResourceLocation SCOOP_FLUID_C2S = new ResourceLocation(VRLiquidBucket.MOD_ID, "scoop_fluid_c2s");
    public static final ResourceLocation FILL_BOTTLE_C2S = new ResourceLocation(VRLiquidBucket.MOD_ID, "fill_bottle_c2s");
    public static final ResourceLocation CATCH_FISH_C2S = new ResourceLocation(VRLiquidBucket.MOD_ID, "catch_fish_c2s");
    public static final ResourceLocation LAVA_BURN_C2S = new ResourceLocation(VRLiquidBucket.MOD_ID, "lava_burn_c2s");

    private static boolean initialized;

    public static void initCommon() {
        if (initialized) return;
        initialized = true;

        NetworkHelper.registerServerReceiver(SCOOP_FLUID_C2S, (buf, player) -> {
            BucketInteractionHandler.scoopFluid(
                    player,
                    buf.readInt(),
                    buf.readBlockPos(),
                    readVec3(buf)
            );
        });

        NetworkHelper.registerServerReceiver(FILL_BOTTLE_C2S, (buf, player) -> {
            BucketInteractionHandler.fillBottle(
                    player,
                    buf.readInt(),
                    buf.readBlockPos(),
                    readVec3(buf)
            );
        });

        NetworkHelper.registerServerReceiver(CATCH_FISH_C2S, (buf, player) -> {
            BucketInteractionHandler.catchFish(
                    player,
                    buf.readInt(),
                    buf.readInt(),
                    readVec3(buf)
            );
        });

        NetworkHelper.registerServerReceiver(LAVA_BURN_C2S, (buf, player) -> {
            BucketInteractionHandler.igniteFromLava(
                    player,
                    buf.readInt(),
                    readVec3(buf)
            );
        });
    }

    private static Vec3 readVec3(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}
