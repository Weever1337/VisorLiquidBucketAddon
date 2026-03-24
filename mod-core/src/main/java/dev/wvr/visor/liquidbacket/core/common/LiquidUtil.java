package dev.wvr.visor.liquidbacket.core.common;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseClient;
import org.vmstudio.visor.api.common.HandType;
import org.vmstudio.visor.api.common.player.VRPose;
import org.vmstudio.visor.api.server.player.VRServerPlayer;

public class LiquidUtil {
    public static final double MAX_HAND_TO_ENTITY_DISTANCE_SQR = 0.04D;
    public static final double MAX_CLIENT_TIP_TO_SERVER_HAND_DISTANCE_SQR = 1.44D;
    public static final double FLUID_PICKUP_DEPTH = 0.04D;
    public static final double BOTTLE_FILL_DEPTH = 0.08D;
    public static final double LAVA_BURN_DEPTH = 0.23D;
    
    public static final Vector3f BOTTLE_TIP_OFFSET = new Vector3f(0.0F, -0.10F, -0.25F);
    public static final Vector3f BUCKET_TIP_OFFSET = new Vector3f(0.0F, -0.12F, -0.25F);
    
    public static final int SCOOP_COOLDOWN_TICKS = 12;
    public static final int LAVA_COOLDOWN_TICKS = 20;
    public static final double FISH_CAPTURE_RADIUS = 0.4D;
    
    public static Vec3 getInteractionTip(PlayerPoseClient pose, HandType handType, ItemStack heldStack) {
        return getInteractionTip(pose.getGripHand(handType), heldStack);
    }

    public static Vec3 getInteractionTip(VRPose handPose, ItemStack heldStack) {
        Vector3f offset = heldStack.is(Items.GLASS_BOTTLE) ? BOTTLE_TIP_OFFSET : BUCKET_TIP_OFFSET;
        Vector3f tip = handPose.getCustomVector(new Vector3f(offset)).add(handPose.getPosition());
        return new Vec3(tip.x(), tip.y(), tip.z());
    }

    public static boolean isHandDeepEnough(ServerPlayer player, BlockPos blockPos, Vec3 handTipPos, FluidState fluidState, double minDepth) {
        if (!BlockPos.containing(handTipPos).equals(blockPos)) {
            return false;
        }

        double fluidSurface = blockPos.getY() + fluidState.getHeight(player.level(), blockPos);
        return handTipPos.y <= fluidSurface - minDepth;
    }

    public static @Nullable VRServerPlayer getVrPlayer(ServerPlayer player) {
        if (VisorAPI.server() == null) {
            return null;
        }

        VRServerPlayer vrPlayer = VisorAPI.server().getVrPlayer(player);
        if (vrPlayer == null || !vrPlayer.isVRActive()) {
            return null;
        }

        return vrPlayer;
    }

    public static boolean isClientTipNearServerHand(VRServerPlayer vrPlayer, HandType handType, Vec3 handTipPos) {
        Vec3 serverHandPos = vrPlayer.getPoseData().getHand(handType).getPositionVec3();
        return serverHandPos.distanceToSqr(handTipPos) <= MAX_CLIENT_TIP_TO_SERVER_HAND_DISTANCE_SQR;
    }

    public static void replaceHeldContainer(ServerPlayer player,
                                             InteractionHand interactionHand,
                                             ItemStack originalStack,
                                             ItemStack resultStack) {
        if (originalStack.getCount() > 1) {
            if (!player.getAbilities().instabuild) {
                originalStack.shrink(1);
            }

            giveResultToInventory(player, resultStack.copy());
            player.containerMenu.broadcastChanges();
            return;
        }

        player.setItemInHand(interactionHand, resultStack.copy());
        player.containerMenu.broadcastChanges();
    }

    public static void giveResultToInventory(ServerPlayer player, ItemStack resultStack) {
        if (!player.getInventory().add(resultStack)) {
            player.drop(resultStack, false);
        }
    }
}
