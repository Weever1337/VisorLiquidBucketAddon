package dev.wvr.visor.liquidbacket.core.client.render.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wvr.visor.liquidbacket.core.client.tasks.TaskBucketInteraction;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseClient;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseType;
import org.vmstudio.visor.api.client.render.VRCameraType;
import org.vmstudio.visor.api.client.render.decoration.VRDecorator;
import org.vmstudio.visor.api.client.render.decoration.annotations.RegisterVRGameEffect;
import org.vmstudio.visor.api.client.render.decoration.effects.VRGameEffect;
import org.vmstudio.visor.api.common.HandType;
import org.vmstudio.visor.api.common.addon.VisorAddon;

@RegisterVRGameEffect
public class GameEffectBucketDebug extends VRGameEffect {
    public static final String ID = "bucket_debug_renderer";
    private static final double BOX_HALF_SIZE = 0.05D;
    private static final double AXIS_LENGTH = 0.18D;
    private static final double AXIS_HALF_WIDTH = 0.005D;

    public GameEffectBucketDebug(@NotNull VisorAddon owner) {
        super(owner);
    }

    private static void applyCameraOrientation(VRCameraType renderPass, PoseStack poseStack) {
        PlayerPoseClient renderPose = VisorAPI.client().getVRLocalPlayer().getPoseData(PlayerPoseType.RENDER);
        Matrix4f rotationMatrix = renderPose
                .getCameraPose(renderPass)
                .getRotation()
                .transpose(new Matrix4f());

        poseStack.last().pose().mul(rotationMatrix);
        poseStack.last().normal().mul(new Matrix3f(rotationMatrix));
    }

    @Override
    public boolean isVisible(@NotNull VRDecorator currentDecorator) {
        return true; // todo: isDev fml
    }

    @Override
    public boolean isGlobal() {
        return true;
    }

    @Override
    public void render(@NotNull VRCameraType cameraType, @NotNull PoseStack poseStack, float partialTicks) {
        TaskBucketInteraction task = (TaskBucketInteraction) VisorAPI.addonManager()
                .getRegistries()
                .tasks()
                .getComponent(TaskBucketInteraction.ID);
        if (task == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        poseStack.pushPose();
        poseStack.setIdentity();
        applyCameraOrientation(cameraType, poseStack);

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        renderHandDebug(poseStack, bufferSource, task.getDebugTipPosition(HandType.MAIN), 0.20F, 0.80F, 1.00F);
        renderHandDebug(poseStack, bufferSource, task.getDebugTipPosition(HandType.OFFHAND), 1.00F, 0.65F, 0.15F);

        bufferSource.endBatch();
        poseStack.popPose();
    }

    private void renderHandDebug(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Vec3 tipPos, float red, float green, float blue) {
        if (tipPos == null) return;

        AABB hitbox = new AABB(
                tipPos.x - BOX_HALF_SIZE,
                tipPos.y - BOX_HALF_SIZE,
                tipPos.z - BOX_HALF_SIZE,
                tipPos.x + BOX_HALF_SIZE,
                tipPos.y + BOX_HALF_SIZE,
                tipPos.z + BOX_HALF_SIZE
        );

        DebugRenderer.renderFilledBox(poseStack, bufferSource, hitbox, red, green, blue, 0.18F);
        DebugRenderer.renderFilledBox(
                poseStack,
                bufferSource,
                tipPos.x,
                tipPos.y - AXIS_HALF_WIDTH,
                tipPos.z - AXIS_HALF_WIDTH,
                tipPos.x + AXIS_LENGTH,
                tipPos.y + AXIS_HALF_WIDTH,
                tipPos.z + AXIS_HALF_WIDTH,
                1.0F,
                0.15F,
                0.15F,
                0.95F
        );
        DebugRenderer.renderFilledBox(
                poseStack,
                bufferSource,
                tipPos.x - AXIS_HALF_WIDTH,
                tipPos.y,
                tipPos.z - AXIS_HALF_WIDTH,
                tipPos.x + AXIS_HALF_WIDTH,
                tipPos.y + AXIS_LENGTH,
                tipPos.z + AXIS_HALF_WIDTH,
                0.20F,
                1.0F,
                0.20F,
                0.95F
        );
        DebugRenderer.renderFilledBox(
                poseStack,
                bufferSource,
                tipPos.x - AXIS_HALF_WIDTH,
                tipPos.y - AXIS_HALF_WIDTH,
                tipPos.z,
                tipPos.x + AXIS_HALF_WIDTH,
                tipPos.y + AXIS_HALF_WIDTH,
                tipPos.z + AXIS_LENGTH,
                0.25F,
                0.45F,
                1.0F,
                0.95F
        );
    }

    @Override
    public @NotNull String getId() {
        return ID;
    }
}
