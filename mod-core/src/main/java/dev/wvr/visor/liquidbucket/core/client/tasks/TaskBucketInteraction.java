package dev.wvr.visor.liquidbucket.core.client.tasks;

import dev.wvr.visor.liquidbucket.core.common.AddonNetworking;
import dev.wvr.visor.liquidbucket.core.common.LiquidUtil;
import dev.wvr.visor.liquidbucket.core.common.network.NetworkHelper;
import io.netty.buffer.Unpooled;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseClient;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseType;
import org.vmstudio.visor.api.client.tasks.RegisterVisorTask;
import org.vmstudio.visor.api.client.tasks.TaskType;
import org.vmstudio.visor.api.client.tasks.VisorTask;
import org.vmstudio.visor.api.common.HandType;
import org.vmstudio.visor.api.common.addon.VisorAddon;

import java.util.Comparator;
import java.util.EnumMap;

@RegisterVisorTask
public class TaskBucketInteraction extends VisorTask {
    public static final String ID = "bucket_interaction";

    private final EnumMap<HandType, Integer> scoopCooldowns = new EnumMap<>(HandType.class);
    private final EnumMap<HandType, Integer> lavaCooldowns = new EnumMap<>(HandType.class);
    private final EnumMap<HandType, Vec3> debugTipPositions = new EnumMap<>(HandType.class);

    public TaskBucketInteraction(@NotNull VisorAddon owner) {
        super(owner);
        for (HandType handType : HandType.values()) {
            scoopCooldowns.put(handType, 0);
            lavaCooldowns.put(handType, 0);
            debugTipPositions.put(handType, null);
        }
    }

    @Override
    protected void onRun(@Nullable LocalPlayer player) {
        if (player == null) {
            return;
        }

        Level level = player.level();
        PlayerPoseClient pose = VisorAPI.client().getVRLocalPlayer().getPoseData(PlayerPoseType.TICK);
        for (HandType handType : HandType.values()) {
            tickCooldown(scoopCooldowns, handType);
            tickCooldown(lavaCooldowns, handType);

            ItemStack heldStack = player.getItemInHand(handType.asInteractionHand());
            Vec3 handTipPos = LiquidUtil.getInteractionTip(pose, handType, heldStack);
            debugTipPositions.put(handType, shouldRenderDebugTip(heldStack) ? handTipPos : null);
            tryIgniteFromLava(level, handType, handTipPos);
            tryUseBucket(player, heldStack, level, pose, handType, handTipPos);
        }
    }

    private void tryIgniteFromLava(Level level, HandType handType, Vec3 handTipPos) {
        if (lavaCooldowns.get(handType) > 0) {
            return;
        }

        BlockPos blockPos = BlockPos.containing(handTipPos);
        FluidState fluidState = level.getFluidState(blockPos);
        if (!fluidState.is(FluidTags.LAVA) || !isHandDeepEnough(level, blockPos, handTipPos, fluidState, LiquidUtil.LAVA_BURN_DEPTH)) {
            return;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(handType.ordinal());
        writeVec3(buf, handTipPos);
        NetworkHelper.sendToServer(AddonNetworking.LAVA_BURN_C2S, buf);
        lavaCooldowns.put(handType, LiquidUtil.LAVA_COOLDOWN_TICKS);
    }

    private void tryUseBucket(LocalPlayer player, ItemStack heldStack, Level level, PlayerPoseClient pose, HandType handType, Vec3 handTipPos) {
        if (scoopCooldowns.get(handType) > 0) {
            return;
        }

        if (heldStack.is(Items.BUCKET)) {
            tryScoopFluid(level, handType, handTipPos);
            return;
        }

        if (heldStack.is(Items.GLASS_BOTTLE)) {
            tryFillBottle(level, handType, handTipPos);
            return;
        }

        if (tryFluidBucket(pose, heldStack, handType, handTipPos)) {
            return;
        }

        if (heldStack.is(Items.WATER_BUCKET)) {
            tryCatchEntity(player, level, handType, handTipPos);
        }
    }

    private boolean tryFluidBucket(PlayerPoseClient pose, ItemStack heldStack, HandType handType, Vec3 handTipPos) {
        if (!LiquidUtil.isFluidableBucket(heldStack) || !LiquidUtil.isBucketUpsideDown(pose.getHand(handType))) {
            return false;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(handType.ordinal());
        writeVec3(buf, handTipPos);
        NetworkHelper.sendToServer(AddonNetworking.FLUID_BUCKET_C2S, buf);
        scoopCooldowns.put(handType, LiquidUtil.SCOOP_COOLDOWN_TICKS);
        VisorAPI.client().getInputManager().triggerHapticPulse(handType, 0.08F);
        return true;
    }

    private void tryScoopFluid(Level level, HandType handType, Vec3 handTipPos) {
        BlockPos blockPos = BlockPos.containing(handTipPos);
        FluidState fluidState = level.getFluidState(blockPos);
        if (!fluidState.isSource()) {
            return;
        }

        if (!fluidState.is(FluidTags.WATER) && !fluidState.is(FluidTags.LAVA)) {
            return;
        }

        if (!isHandDeepEnough(level, blockPos, handTipPos, fluidState, LiquidUtil.FLUID_PICKUP_DEPTH)) {
            return;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(handType.ordinal());
        buf.writeBlockPos(blockPos);
        writeVec3(buf, handTipPos);
        NetworkHelper.sendToServer(AddonNetworking.SCOOP_FLUID_C2S, buf);
        scoopCooldowns.put(handType, LiquidUtil.SCOOP_COOLDOWN_TICKS);
        VisorAPI.client().getInputManager().triggerHapticPulse(handType, 0.08F);
    }

    private void tryFillBottle(Level level, HandType handType, Vec3 handTipPos) {
        BlockPos blockPos = BlockPos.containing(handTipPos);
        FluidState fluidState = level.getFluidState(blockPos);
        if (!fluidState.is(FluidTags.WATER) || fluidState.isEmpty()) {
            return;
        }

        if (!isHandDeepEnough(level, blockPos, handTipPos, fluidState, 0.08D)) {
            return;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(handType.ordinal());
        buf.writeBlockPos(blockPos);
        writeVec3(buf, handTipPos);
        NetworkHelper.sendToServer(AddonNetworking.FILL_BOTTLE_C2S, buf);
        scoopCooldowns.put(handType, LiquidUtil.SCOOP_COOLDOWN_TICKS);
        VisorAPI.client().getInputManager().triggerHapticPulse(handType, 0.06F);
    }

    private void tryCatchEntity(LocalPlayer player, Level level, HandType handType, Vec3 handTipPos) {
        Entity nearestEntity = level.getEntities(player, new AABB(handTipPos, handTipPos).inflate(LiquidUtil.FISH_CAPTURE_RADIUS), entity -> entity.isAlive() && entity instanceof LivingEntity && entity instanceof Bucketable).stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(handTipPos)))
                .orElse(null);

        if (nearestEntity == null) {
            return;
        }

        if (!LiquidUtil.isFishCatchTarget(nearestEntity, handTipPos)) {
            return;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(handType.ordinal());
        buf.writeInt(nearestEntity.getId());
        writeVec3(buf, handTipPos);
        NetworkHelper.sendToServer(AddonNetworking.CATCH_FISH_C2S, buf);
        scoopCooldowns.put(handType, LiquidUtil.FISH_CAPTURE_COOLDOWN_TICKS);
        VisorAPI.client().getInputManager().triggerHapticPulse(handType, 0.08F);
    }

    private boolean isHandDeepEnough(Level level, BlockPos blockPos, Vec3 handTipPos, FluidState fluidState, double minDepth) {
        if (!BlockPos.containing(handTipPos).equals(blockPos)) {
            return false;
        }

        double fluidSurface = blockPos.getY() + fluidState.getHeight(level, blockPos);
        return handTipPos.y <= fluidSurface - minDepth;
    }

    private void tickCooldown(EnumMap<HandType, Integer> cooldowns, HandType handType) {
        int cooldown = cooldowns.get(handType);
        if (cooldown > 0) {
            cooldowns.put(handType, cooldown - 1);
        }
    }

    @Override
    protected void onClear(@Nullable LocalPlayer player) {
        for (HandType handType : HandType.values()) {
            debugTipPositions.put(handType, null);
        }
    }

    @Override
    public boolean isActive(@Nullable LocalPlayer player) {
        return true;
    }

    @Override
    public @NotNull TaskType getType() {
        return TaskType.VR_PLAYER_TICK;
    }

    @Override
    public @NotNull String getId() {
        return ID;
    }

    public @Nullable Vec3 getDebugTipPosition(HandType handType) {
        return debugTipPositions.get(handType);
    }

    private boolean shouldRenderDebugTip(ItemStack heldStack) {
        return heldStack.is(Items.BUCKET) || heldStack.getItem() instanceof BucketItem || heldStack.is(Items.GLASS_BOTTLE);
    }

    private void writeVec3(FriendlyByteBuf buf, Vec3 vec) {
        buf.writeDouble(vec.x);
        buf.writeDouble(vec.y);
        buf.writeDouble(vec.z);
    }
}
