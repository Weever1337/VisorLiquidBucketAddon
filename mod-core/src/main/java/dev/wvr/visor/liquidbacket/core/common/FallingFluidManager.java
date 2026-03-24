package dev.wvr.visor.liquidbacket.core.common;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class FallingFluidManager {
    private static final Map<ResourceKey<Level>, List<FallingFluid>> ACTIVE_FLUIDS = new HashMap<>();

    public static void trackFallingSource(ServerLevel level, BlockPos pos, FlowingFluid fluid) {
        if (!LiquidUtil.isTrackedSource(level, pos, fluid) || !LiquidUtil.shouldSourceKeepFalling(level, pos, fluid)) {
            return;
        }

        List<FallingFluid> fluids = ACTIVE_FLUIDS.computeIfAbsent(level.dimension(), key -> new ArrayList<>());
        for (FallingFluid fallingFluid : fluids) {
            if (fallingFluid.pos.equals(pos)) {
                return;
            }
        }

        fluids.add(new FallingFluid(pos.immutable(), fluid));
    }

    public static void tickServer(MinecraftServer server) {
        Iterator<Map.Entry<ResourceKey<Level>, List<FallingFluid>>> levelIterator = ACTIVE_FLUIDS.entrySet().iterator();
        while (levelIterator.hasNext()) {
            Map.Entry<ResourceKey<Level>, List<FallingFluid>> entry = levelIterator.next();
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) {
                levelIterator.remove();
                continue;
            }

            tickLevel(level, entry.getValue());
            if (entry.getValue().isEmpty()) {
                levelIterator.remove();
            }
        }
    }

    public static void clear() {
        ACTIVE_FLUIDS.clear();
    }

    private static void tickLevel(ServerLevel level, List<FallingFluid> fallingFluids) {
        Iterator<FallingFluid> iterator = fallingFluids.iterator();
        while (iterator.hasNext()) {
            FallingFluid fluid = iterator.next();
            if (!LiquidUtil.isTrackedSource(level, fluid.pos, fluid.fluid)) {
                iterator.remove();
                continue;
            }

            if (!LiquidUtil.shouldSourceKeepFalling(level, fluid.pos, fluid.fluid)) {
                settleSource(level, fluid.pos, fluid.fluid);
                iterator.remove();
                continue;
            }

            BlockPos nextPos = fluid.pos.below();
            if (!level.isLoaded(nextPos)) { // hello to unloaded chunks
                iterator.remove();
                continue;
            }

            if (!LiquidUtil.canFluidFallThrough(nextPos, level, fluid.fluid)) {
                settleSource(level, fluid.pos, fluid.fluid);
                iterator.remove();
                continue;
            }

            moveSourceDown(level, fluid.pos, nextPos, fluid.fluid);
            fluid.pos = nextPos.immutable();

            if (!LiquidUtil.shouldSourceKeepFalling(level, fluid.pos, fluid.fluid)) {
                settleSource(level, fluid.pos, fluid.fluid);
                iterator.remove();
            }
        }
    }

    private static void moveSourceDown(ServerLevel level, BlockPos currentPos, BlockPos nextPos, FlowingFluid fluid) {
        level.setBlock(currentPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(nextPos, fluid.getSource(false).createLegacyBlock(), Block.UPDATE_ALL);
    }

    private static void settleSource(ServerLevel level, BlockPos pos, FlowingFluid fluid) {
        if (LiquidUtil.isTrackedSource(level, pos, fluid)) {
            level.scheduleTick(pos, fluid, fluid.getTickDelay(level));
        }
    }

    private static final class FallingFluid {
        private BlockPos pos;
        private final FlowingFluid fluid;

        private FallingFluid(BlockPos pos, FlowingFluid fluid) {
            this.pos = pos;
            this.fluid = fluid;
        }
    }
}
