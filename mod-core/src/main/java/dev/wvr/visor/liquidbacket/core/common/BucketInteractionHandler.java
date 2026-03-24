package dev.wvr.visor.liquidbacket.core.common;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.vmstudio.visor.api.common.HandType;
import org.vmstudio.visor.api.server.player.VRServerPlayer;

public final class BucketInteractionHandler {
    public static void scoopFluid(ServerPlayer player, int handId, BlockPos blockPos, Vec3 handTipPos) {
        HandType handType = HandType.fromInt(handId);
        VRServerPlayer vrPlayer = LiquidUtil.getVrPlayer(player);
        if (vrPlayer == null) {
            return;
        }

        InteractionHand interactionHand = handType.asInteractionHand();
        ItemStack heldStack = player.getItemInHand(interactionHand);
        if (!heldStack.is(Items.BUCKET)) {
            return;
        }

        if (!LiquidUtil.isClientTipNearServerHand(vrPlayer, handType, handTipPos)) {
            return;
        }

        FluidState fluidState = player.level().getFluidState(blockPos);
        if (!fluidState.isSource()) {
            return;
        }

        if (!fluidState.is(FluidTags.WATER) && !fluidState.is(FluidTags.LAVA)) {
            return;
        }

        if (!LiquidUtil.isHandDeepEnough(player, blockPos, handTipPos, fluidState, LiquidUtil.FLUID_PICKUP_DEPTH)) {
            return;
        }

        BlockState blockState = player.level().getBlockState(blockPos);
        if (!(blockState.getBlock() instanceof BucketPickup bucketPickup)) {
            return;
        }

        ItemStack filledResult = bucketPickup.pickupBlock(player.level(), blockPos, blockState);
        if (filledResult.isEmpty()) {
            return;
        }

        fluidState.getType().getPickupSound().ifPresent(sound ->
                player.level().playSound(null, blockPos, sound, SoundSource.BLOCKS, 1.0F, 1.0F)
        );
        LiquidUtil.replaceHeldContainer(player, interactionHand, heldStack, filledResult);
        player.awardStat(Stats.ITEM_USED.get(Items.BUCKET));
        player.level().gameEvent(player, GameEvent.FLUID_PICKUP, blockPos);
        player.swing(interactionHand, true);
    }

    public static void fillBottle(ServerPlayer player, int handId, BlockPos blockPos, Vec3 handTipPos) {
        HandType handType = HandType.fromInt(handId);
        VRServerPlayer vrPlayer = LiquidUtil.getVrPlayer(player);
        if (vrPlayer == null) {
            return;
        }

        InteractionHand interactionHand = handType.asInteractionHand();
        ItemStack heldStack = player.getItemInHand(interactionHand);
        if (!heldStack.is(Items.GLASS_BOTTLE)) {
            return;
        }

        if (!LiquidUtil.isClientTipNearServerHand(vrPlayer, handType, handTipPos)) {
            return;
        }

        FluidState fluidState = player.level().getFluidState(blockPos);
        if (!fluidState.is(FluidTags.WATER) || fluidState.isEmpty()) {
            return;
        }

        if (!LiquidUtil.isHandDeepEnough(player, blockPos, handTipPos, fluidState, LiquidUtil.BOTTLE_FILL_DEPTH)) {
            return;
        }

        ItemStack waterBottle = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
        player.level().playSound(null, blockPos, net.minecraft.sounds.SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
        LiquidUtil.replaceHeldContainer(player, interactionHand, heldStack, waterBottle);
        player.awardStat(Stats.ITEM_USED.get(Items.GLASS_BOTTLE));
        player.level().gameEvent(player, GameEvent.FLUID_PICKUP, blockPos);
        player.swing(interactionHand, true);
    }

    public static void catchFish(ServerPlayer player, int handId, int entityId, Vec3 handTipPos) {
        HandType handType = HandType.fromInt(handId);
        VRServerPlayer vrPlayer = LiquidUtil.getVrPlayer(player);
        if (vrPlayer == null) {
            return;
        }

        InteractionHand interactionHand = handType.asInteractionHand();
        ItemStack heldStack = player.getItemInHand(interactionHand);
        if (!heldStack.is(Items.WATER_BUCKET)) {
            return;
        }

        Entity entity = player.level().getEntity(entityId);
        if (!(entity instanceof LivingEntity livingEntity) || !(entity instanceof Bucketable bucketable) || !entity.isAlive()) {
            return;
        }

        if (!LiquidUtil.isClientTipNearServerHand(vrPlayer, handType, handTipPos)) {
            return;
        }

        if (!LiquidUtil.isFishCatchTarget(entity, handTipPos)) {
            return;
        }

        if (!heldStack.is(Items.WATER_BUCKET) || !livingEntity.isAlive()) {
            return;
        }

        SoundEvent pickupSound = bucketable.getPickupSound();
        livingEntity.playSound(pickupSound, 1.0F, 1.0F);

        ItemStack bucketedEntity = bucketable.getBucketItemStack();
        bucketable.saveToBucketTag(bucketedEntity);
        LiquidUtil.replaceHeldContainer(player, interactionHand, heldStack, bucketedEntity);

        if (!player.level().isClientSide()) {
            player.awardStat(Stats.ITEM_USED.get(Items.WATER_BUCKET));
        }

        if (entity instanceof Mob mob) {
            Bucketable.saveDefaultDataToBucketTag(mob, bucketedEntity);
        }

        entity.discard();
        player.level().gameEvent(player, GameEvent.ENTITY_INTERACT, entity.position());
        player.swing(interactionHand, true);
    }

    public static void fluidBucket(ServerPlayer player, int handId, Vec3 handTipPos) {
        HandType handType = HandType.fromInt(handId);
        VRServerPlayer vrPlayer = LiquidUtil.getVrPlayer(player);
        if (vrPlayer == null) {
            return;
        }

        InteractionHand interactionHand = handType.asInteractionHand();
        ItemStack heldStack = player.getItemInHand(interactionHand);
        if (!(heldStack.getItem() instanceof BucketItem bucketItem)) {
            return;
        }

        FlowingFluid fluid = LiquidUtil.getBucketFluid(heldStack);
        if (fluid == null) {
            return;
        }

        if (!LiquidUtil.isClientTipNearServerHand(vrPlayer, handType, handTipPos)) {
            return;
        }

        BlockPos placementPos = LiquidUtil.findFluidStartPos(player.level(), handTipPos, fluid);
        if (placementPos == null) {
            return;
        }

        if (!player.level().mayInteract(player, placementPos) || !player.mayUseItemAt(placementPos, net.minecraft.core.Direction.UP, heldStack)) {
            return;
        }

        if (!bucketItem.emptyContents(player, player.level(), placementPos, null)) {
            return;
        }

        bucketItem.checkExtraContent(player, player.level(), heldStack, placementPos);
        CriteriaTriggers.PLACED_BLOCK.trigger(player, placementPos, heldStack);
        player.awardStat(Stats.ITEM_USED.get(heldStack.getItem()));
        if (!player.getAbilities().instabuild) {
            LiquidUtil.replaceHeldContainer(player, interactionHand, heldStack, BucketItem.getEmptySuccessItem(heldStack, player));
        }
        FallingFluidManager.trackFallingSource(player.serverLevel(), placementPos, fluid);
        player.swing(interactionHand, true);
    }

    public static void igniteFromLava(ServerPlayer player, int handId, Vec3 handTipPos) {
        if (player.fireImmune()) {
            return;
        }

        HandType handType = HandType.fromInt(handId);
        VRServerPlayer vrPlayer = LiquidUtil.getVrPlayer(player);
        if (vrPlayer == null) {
            return;
        }

        if (!LiquidUtil.isClientTipNearServerHand(vrPlayer, handType, handTipPos)) {
            return;
        }

        BlockPos blockPos = BlockPos.containing(handTipPos);
        FluidState fluidState = player.level().getFluidState(blockPos);
        if (!fluidState.is(FluidTags.LAVA)) {
            return;
        }

        if (!LiquidUtil.isHandDeepEnough(player, blockPos, handTipPos, fluidState, LiquidUtil.LAVA_BURN_DEPTH)) {
            return;
        }

        player.setSecondsOnFire(4);
    }
}