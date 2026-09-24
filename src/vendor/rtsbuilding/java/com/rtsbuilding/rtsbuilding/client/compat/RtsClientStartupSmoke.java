package com.rtsbuilding.rtsbuilding.client.compat;

import com.rtsbuilding.rtsbuilding.RtsbuildingMod;
import com.rtsbuilding.rtsbuilding.client.controller.ClientRtsController;
import com.rtsbuilding.rtsbuilding.client.network.RtsClientPacketGateway;
import com.rtsbuilding.rtsbuilding.platform.math.BlockPos;
import com.rtsbuilding.rtsbuilding.platform.math.EnumFacing;
import com.rtsbuilding.rtsbuilding.platform.math.RayTraceResult;
import com.rtsbuilding.rtsbuilding.platform.math.Vec3d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 真实客户端启动烟测的游戏内驱动器。
 *
 * <p>它只在 Gradle {@code runClientSmoke} 显式设置系统属性时启用。驱动器会从主菜单
 * 创建隔离单人世界，跨过入门提醒的延迟窗口，开关一次 RTS 相机并继续运行若干帧，
 * 最后写出机器可判定的报告并正常关闭客户端。普通发布包虽然包含这个类，但没有属性
 * 时不会创建目录、注册额外命令或改变任何玩家行为。</p>
 */
@SideOnly(Side.CLIENT)
public final class RtsClientStartupSmoke {
    private static final String ENABLE_PROPERTY = "rtsbuilding.clientStartupSmoke";
    private static final String REPORT_PROPERTY = "rtsbuilding.clientStartupSmokeReport";
    private static final String WORLD_DIRECTORY = "GTNGOfficialRTSSmoke-" + System.currentTimeMillis();
    private static final int MAIN_MENU_STABLE_TICKS = 20;
    /** 必须超过入门提醒的 80 tick，才能覆盖最初的客户端崩溃路径。 */
    private static final int WORLD_STABLE_TICKS = 140;
    private static final int RTS_RENDER_TICKS = 60;
    private static final int MINING_PACKET_SETTLE_TICKS = 20;
    private static final int FINAL_STABLE_TICKS = 40;
    private static final int STAGE_TIMEOUT_TICKS = 20 * 30;
    private static final int TOTAL_TIMEOUT_TICKS = 20 * 120;

    private static final boolean ENABLED = Boolean.getBoolean(ENABLE_PROPERTY);
    private static final Path REPORT_PATH = resolveReportPath();

    private static Stage stage = Stage.WAIT_MAIN_MENU;
    private static int stageTicks;
    private static int totalTicks;
    private static int finalStableTicks;
    private static BlockPos emptyToolMinePos;
    private static BlockPos creativePrototypePlacedPos;
    private static BlockPos[] gtProbePositions;
    private static ItemStack[] gtProbeStacks;
    private static net.minecraft.tileentity.TileEntity rotatingTile;
    private static net.minecraftforge.common.util.ForgeDirection expectedFacing;
    private static int rotationCount;
    private static boolean finished;
    private static volatile boolean harvestProbeRequested;
    private static volatile boolean harvestProbeDone;
    private static volatile String harvestProbeFailure;

    /** Runs native empty-hand harvesting checks on the integrated server, only in the disposable QA world. */
    @SubscribeEvent
    public void onHarvestProbeTick(TickEvent.WorldTickEvent event) {
        if (!ENABLED || !harvestProbeRequested || harvestProbeDone || event.phase != TickEvent.Phase.END
                || event.world.isRemote || event.world.playerEntities.isEmpty()) return;
        net.minecraft.entity.player.EntityPlayerMP player =
                (net.minecraft.entity.player.EntityPlayerMP) event.world.playerEntities.get(0);
        boolean originalCreative = player.capabilities.isCreativeMode;
        int slot = player.inventory.currentItem;
        ItemStack held = player.inventory.mainInventory[slot];
        try {
            player.inventory.mainInventory[slot] = null;
            for (boolean creative : new boolean[] { false, true }) {
                player.capabilities.isCreativeMode = creative;
                int x = (int) Math.floor(player.posX) + 4;
                int y = (int) Math.floor(player.posY) + 6;
                int z = (int) Math.floor(player.posZ);
                for (net.minecraft.block.Block block : new net.minecraft.block.Block[] { Blocks.stone, Blocks.diamond_ore }) {
                    event.world.setBlock(x, y, z, block, 0, 3);
                    net.minecraft.item.Item expected = block == Blocks.stone
                            ? net.minecraft.item.Item.getItemFromBlock(Blocks.cobblestone) : net.minecraft.init.Items.diamond;
                    if (!com.rtsbuilding.rtsbuilding.server.service.mining.RtsToollessHarvest.harvest(
                            player, new BlockPos(x, y, z))) throw new IllegalStateException("native harvest refused");
                    int count = 0;
                    for (Object value : event.world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class,
                            net.minecraft.util.AxisAlignedBB.getBoundingBox(x - 1, y - 1, z - 1, x + 2, y + 2, z + 2))) {
                        net.minecraft.entity.item.EntityItem entity = (net.minecraft.entity.item.EntityItem) value;
                        if (entity.isDead) continue;
                        if (entity.getEntityItem().getItem() == expected) count += entity.getEntityItem().stackSize;
                        entity.setDead();
                    }
                    if (count != 1) throw new IllegalStateException("drop count=" + count + " creative=" + creative);
                    if (player.getHeldItem() != null || player.capabilities.isCreativeMode != creative)
                        throw new IllegalStateException("harvest changed player state");
                    z += 4;
                }
                for (ItemStack prototype : gtProbeStacks) {
                    ItemStack placement = prototype.copy();
                    if (!((net.minecraft.item.ItemBlock) placement.getItem()).placeBlockAt(
                            placement, player, event.world, x, y, z, 1, 0.5F, 0.5F, 0.5F, placement.getItemDamage()))
                        throw new IllegalStateException("GT drop probe placement failed");
                    if (!com.rtsbuilding.rtsbuilding.server.service.mining.RtsToollessHarvest.harvest(
                            player, new BlockPos(x, y, z))) throw new IllegalStateException("GT harvest refused");
                    int count = 0;
                    for (Object value : event.world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class,
                            net.minecraft.util.AxisAlignedBB.getBoundingBox(x - 1, y - 1, z - 1, x + 2, y + 2, z + 2))) {
                        net.minecraft.entity.item.EntityItem entity = (net.minecraft.entity.item.EntityItem) value;
                        if (entity.isDead) continue;
                        if (entity.getEntityItem().isItemEqual(prototype)) count += entity.getEntityItem().stackSize;
                        entity.setDead();
                    }
                    if (count != 1) throw new IllegalStateException("GT drop count=" + count
                            + " subtype=" + prototype.getItemDamage() + " creative=" + creative);
                    z += 4;
                }
                event.world.setBlock(x, y, z, Blocks.bedrock, 0, 3);
                if (com.rtsbuilding.rtsbuilding.server.service.mining.RtsToollessHarvest.harvest(
                        player, new BlockPos(x, y, z))) throw new IllegalStateException("bedrock removed");
                event.world.setBlockToAir(x, y, z);
            }
        } catch (Throwable failure) {
            harvestProbeFailure = failure.toString();
        } finally {
            player.inventory.mainInventory[slot] = held;
            player.capabilities.isCreativeMode = originalCreative;
            harvestProbeDone = true;
        }
    }

    private RtsClientStartupSmoke() {
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || finished || event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        try {
            totalTicks++;
            stageTicks++;
            if (totalTicks > TOTAL_TIMEOUT_TICKS) {
                finish(minecraft, false, "total timeout at stage=" + stage);
                return;
            }
            tickStage(minecraft);
        } catch (Throwable failure) {
            finish(minecraft, false,
                    failure.getClass().getName() + ": " + safeMessage(failure));
        }
    }

    private static void tickStage(Minecraft minecraft) throws IOException {
        if (stage == Stage.WAIT_MAIN_MENU) {
            if (minecraft.currentScreen == null
                    || minecraft.theWorld != null || minecraft.thePlayer != null) {
                if (stageTicks > STAGE_TIMEOUT_TICKS) {
                    finish(minecraft, false, "main menu did not become ready");
                }
                return;
            }
            if (stageTicks < MAIN_MENU_STABLE_TICKS) return;
            append("MAIN_MENU_READY");
            verifyTopBarTextures(minecraft);
            if (!com.rtsbuilding.rtsbuilding.compat.ae2.RtsAe2Compat.isAvailable()) {
                throw new IllegalStateException("GTNH AE2 inventory bridge unavailable");
            }
            append("GTNH_AE2_BRIDGE_OK");
            verifyHistoryStateCodec();
            verifyTranslation(minecraft, "screen.rtsbuilding.plugins");
            WorldSettings settings = new WorldSettings(
                    0x525453112L, GameType.CREATIVE, true, false, WorldType.DEFAULT);
            settings.enableCommands();
            minecraft.launchIntegratedServer(
                    WORLD_DIRECTORY, "RTSBuilding Client Smoke", settings);
            moveTo(Stage.WAIT_WORLD);
            return;
        }

        if (stage == Stage.WAIT_WORLD) {
            if (minecraft.theWorld == null || minecraft.thePlayer == null
                    || minecraft.thePlayer.sendQueue == null
                    || minecraft.getIntegratedServer() == null) {
                failStageTimeout(minecraft, "integrated client world did not become ready");
                return;
            }
            if (stageTicks == 1) {
                append("WORLD_READY folder=" + minecraft.getIntegratedServer().getFolderName());
            }
            if (stageTicks < WORLD_STABLE_TICKS) return;
            if (Boolean.getBoolean("rtsbuilding.clientStartupSmokeInventoryOnly")) {
                if (!(minecraft.currentScreen instanceof net.minecraft.client.gui.inventory.GuiInventory)) {
                    minecraft.playerController.setGameType(GameType.SURVIVAL);
                    minecraft.thePlayer.capabilities.isCreativeMode = false;
                    minecraft.displayGuiScreen(new net.minecraft.client.gui.inventory.GuiInventory(minecraft.thePlayer));
                }
                if (stageTicks < WORLD_STABLE_TICKS + 40) return;
                net.minecraft.util.ScreenShotHelper.saveScreenshot(minecraft.mcDataDir,
                        "rts-inventory-no-button.png", minecraft.displayWidth, minecraft.displayHeight,
                        minecraft.getFramebuffer());
                finish(minecraft, true, "inventory-only visual check");
                return;
            }
            RtsClientPacketGateway.sendToggleCamera(
                    ClientRtsController.get().isStartCameraAtPlayerHead());
            append("RTS_ENABLE_SENT");
            moveTo(Stage.WAIT_RTS_ON);
            return;
        }

        if (stage == Stage.WAIT_RTS_ON) {
            if (!ClientRtsController.get().isEnabled()) {
                failStageTimeout(minecraft, "RTS enable acknowledgement timed out");
                return;
            }
            append("RTS_ENABLED");
            RtsClientPacketGateway.sendQuickDrop("minecraft:diamond", 8,
                    new Vec3d(minecraft.thePlayer.posX + 2, minecraft.thePlayer.posY, minecraft.thePlayer.posZ));
            append("RTS_QUICK_DROP_EMPTY_SOURCES_SENT");
            moveTo(Stage.WAIT_QUICK_DROP);
            return;
        }

        if (stage == Stage.WAIT_QUICK_DROP) {
            if (!integratedServerHealthy(minecraft)) {
                finish(minecraft, false, "server stopped after RTS quick drop with empty sources");
                return;
            }
            if (stageTicks < MINING_PACKET_SETTLE_TICKS) return;
            append("RTS_QUICK_DROP_EMPTY_SOURCES_OK");
            BlockPos clicked = com.rtsbuilding.rtsbuilding.platform.player.PlayerCompat
                    .blockPosition(minecraft.thePlayer).down();
            Vec3d hit = new Vec3d(clicked.getX() + 0.5D, clicked.getY() + 1.0D, clicked.getZ() + 0.5D);
            Vec3d origin = new Vec3d(minecraft.thePlayer.posX,
                    minecraft.thePlayer.posY + minecraft.thePlayer.getEyeHeight(), minecraft.thePlayer.posZ);
            RtsClientPacketGateway.sendEmptyHandPlace(new RayTraceResult(hit, EnumFacing.UP, clicked),
                    origin, hit.subtract(origin).normalize());
            append("NULL_PROTOTYPE_DURABLE_PLACE_SENT");
            moveTo(Stage.WAIT_NULL_PROTOTYPE_PLACE);
            return;
        }

        if (stage == Stage.WAIT_NULL_PROTOTYPE_PLACE) {
            if (!integratedServerHealthy(minecraft)) {
                finish(minecraft, false, "server stopped executing null-prototype placement task");
                return;
            }
            if (stageTicks < MINING_PACKET_SETTLE_TICKS) return;
            append("NULL_PROTOTYPE_DURABLE_PLACE_OK");
            emptyToolMinePos = com.rtsbuilding.rtsbuilding.platform.player.PlayerCompat
                    .blockPosition(minecraft.thePlayer).down();
            RtsClientPacketGateway.sendMineStart(
                    emptyToolMinePos, EnumFacing.UP.getIndex(), 0, "", null, false, false);
            append("EMPTY_TOOL_MINE_START_SENT pos=" + emptyToolMinePos);
            moveTo(Stage.WAIT_EMPTY_TOOL_MINE_START);
            return;
        }

        if (stage == Stage.WAIT_EMPTY_TOOL_MINE_START) {
            if (!integratedServerHealthy(minecraft)) {
                finish(minecraft, false, "integrated server stopped after empty-tool mine start");
                return;
            }
            if (stageTicks < MINING_PACKET_SETTLE_TICKS) return;
            RtsClientPacketGateway.sendMineAbort(
                    emptyToolMinePos, EnumFacing.UP.getIndex(), 0);
            append("EMPTY_TOOL_MINE_ABORT_SENT pos=" + emptyToolMinePos);
            moveTo(Stage.WAIT_EMPTY_TOOL_MINE_ABORT);
            return;
        }

        if (stage == Stage.WAIT_EMPTY_TOOL_MINE_ABORT) {
            if (!integratedServerHealthy(minecraft)) {
                finish(minecraft, false, "integrated server stopped after empty-tool mine abort");
                return;
            }
            if (stageTicks < MINING_PACKET_SETTLE_TICKS) return;
            append("EMPTY_TOOL_MINING_ROUND_TRIP_OK");
            Vec3d hit = new Vec3d(
                    emptyToolMinePos.getX() + 0.5D,
                    emptyToolMinePos.getY() + 1.0D,
                    emptyToolMinePos.getZ() + 0.5D);
            Vec3d origin = new Vec3d(
                    minecraft.thePlayer.posX,
                    minecraft.thePlayer.posY + minecraft.thePlayer.getEyeHeight(),
                    minecraft.thePlayer.posZ);
            Vec3d direction = hit.subtract(origin).normalize();
            RtsClientPacketGateway.sendInteractBlockEmptyHand(
                    new RayTraceResult(hit, EnumFacing.UP, emptyToolMinePos),
                    origin,
                    direction);
            append("EMPTY_HAND_INTERACTION_SENT pos=" + emptyToolMinePos);
            moveTo(Stage.WAIT_EMPTY_HAND_INTERACTION);
            return;
        }

        if (stage == Stage.WAIT_EMPTY_HAND_INTERACTION) {
            if (!integratedServerHealthy(minecraft)) {
                finish(minecraft, false, "integrated server stopped after empty-hand interaction");
                return;
            }
            if (stageTicks < MINING_PACKET_SETTLE_TICKS) return;
            append("EMPTY_HAND_INTERACTION_ROUND_TRIP_OK");
            BlockPos anchor = findCreativePlacementAnchor(minecraft);
            if (anchor == null) {
                finish(minecraft, false, "no nearby placement anchor for creative prototype probe");
                return;
            }
            creativePrototypePlacedPos = anchor.up();
            Vec3d hit = new Vec3d(
                    anchor.getX() + 0.5D,
                    anchor.getY() + 1.0D,
                    anchor.getZ() + 0.5D);
            Vec3d origin = new Vec3d(
                    minecraft.thePlayer.posX,
                    minecraft.thePlayer.posY + minecraft.thePlayer.getEyeHeight(),
                    minecraft.thePlayer.posZ);
            RtsClientPacketGateway.sendInteractBlockWithPinnedItem(
                    new RayTraceResult(hit, EnumFacing.UP, anchor),
                    "minecraft:wool",
                    new ItemStack(Blocks.wool, 1, 14),
                    origin,
                    hit.subtract(origin).normalize());
            append("CREATIVE_PINNED_PROTOTYPE_SENT target=" + creativePrototypePlacedPos + " metadata=14");
            moveTo(Stage.WAIT_CREATIVE_PINNED_PROTOTYPE);
            return;
        }

        if (stage == Stage.WAIT_CREATIVE_PINNED_PROTOTYPE) {
            if (!integratedServerHealthy(minecraft)) {
                finish(minecraft, false, "integrated server stopped after creative pinned-item interaction");
                return;
            }
            if (stageTicks < MINING_PACKET_SETTLE_TICKS) return;
            int x = creativePrototypePlacedPos.getX();
            int y = creativePrototypePlacedPos.getY();
            int z = creativePrototypePlacedPos.getZ();
            if (minecraft.theWorld.getBlock(x, y, z) != Blocks.wool
                    || minecraft.theWorld.getBlockMetadata(x, y, z) != 14) {
                finish(minecraft, false,
                        "creative pinned-item prototype was not preserved at " + creativePrototypePlacedPos);
                return;
            }
            append("CREATIVE_PINNED_PROTOTYPE_ROUND_TRIP_OK metadata=14");
            RtsClientPacketGateway.sendAreaDestroy(java.util.Collections.singletonList(creativePrototypePlacedPos),
                    0, "", null, false);
            append("DURABLE_AREA_DESTROY_SENT");
            moveTo(Stage.WAIT_DURABLE_DESTRUCTION);
            return;
        }

        if (stage == Stage.WAIT_DURABLE_DESTRUCTION) {
            if (!integratedServerHealthy(minecraft)) {
                finish(minecraft, false, "server stopped during durable area destruction");
                return;
            }
            if (!minecraft.theWorld.isAirBlock(creativePrototypePlacedPos.getX(),
                    creativePrototypePlacedPos.getY(), creativePrototypePlacedPos.getZ())) {
                failStageTimeout(minecraft, "durable destruction did not remove placed wool");
                return;
            }
            append("DURABLE_AREA_DESTROY_OK");
            BlockPos anchor = creativePrototypePlacedPos.down();
            Vec3d hit = new Vec3d(anchor.getX() + 0.5D, anchor.getY() + 1.0D, anchor.getZ() + 0.5D);
            Vec3d origin = new Vec3d(minecraft.thePlayer.posX,
                    minecraft.thePlayer.posY + minecraft.thePlayer.getEyeHeight(), minecraft.thePlayer.posZ);
            RtsClientPacketGateway.sendPlaceBatch(java.util.Collections.singletonList(
                    new RayTraceResult(hit, EnumFacing.UP, creativePrototypePlacedPos)), false, true,
                    "minecraft:wool", new ItemStack(Blocks.wool, 1, 14), 0, origin, hit.subtract(origin).normalize());
            append("DURABLE_BATCH_PLACE_SENT");
            moveTo(Stage.WAIT_DURABLE_PLACEMENT);
            return;
        }

        if (stage == Stage.WAIT_DURABLE_PLACEMENT) {
            if (!integratedServerHealthy(minecraft)) {
                finish(minecraft, false, "server stopped during durable batch placement");
                return;
            }
            int x = creativePrototypePlacedPos.getX(), y = creativePrototypePlacedPos.getY(), z = creativePrototypePlacedPos.getZ();
            if (minecraft.theWorld.getBlock(x, y, z) != Blocks.wool || minecraft.theWorld.getBlockMetadata(x, y, z) != 14) {
                failStageTimeout(minecraft, "durable batch placement did not rebuild red wool");
                return;
            }
            append("DURABLE_BATCH_PLACE_OK");
            gtProbeStacks = new ItemStack[] {
                    gregtech.api.util.GTOreDictUnificator.get(gregtech.api.enums.OrePrefixes.wireGt01,
                            gregtech.api.enums.Materials.Copper, 1),
                    gregtech.api.util.GTOreDictUnificator.get(gregtech.api.enums.OrePrefixes.cableGt01,
                            gregtech.api.enums.Materials.Copper, 1),
                    gregtech.api.enums.ItemList.Machine_Multi_Assemblyline.get(1),
                    gregtech.api.enums.ItemList.Machine_Multi_Assemblyline.get(1)
            };
            gtProbePositions = new BlockPos[] { creativePrototypePlacedPos.up(), creativePrototypePlacedPos.up().up(),
                    creativePrototypePlacedPos.add(1, 0, 0), creativePrototypePlacedPos.add(-1, 0, 0) };
            // Both controller interactions must exceed the eight-block chunk-lease threshold.
            int remoteX = ((int) minecraft.thePlayer.posX >> 4) * 16 + 24;
            int remoteZ = ((int) minecraft.thePlayer.posZ >> 4) * 16 + 8;
            for (int i = 2; i < 4; i++) {
                int xPos = remoteX + (i - 2) * 2;
                int yPos = minecraft.theWorld.getTopSolidOrLiquidBlock(xPos, remoteZ);
                gtProbePositions[i] = new BlockPos(xPos, yPos, remoteZ);
            }
            sendGtProbe(minecraft, 0);
            moveTo(Stage.WAIT_GT_WIRE);
            return;
        }

        if (stage == Stage.WAIT_GT_WIRE || stage == Stage.WAIT_GT_CABLE
                || stage == Stage.WAIT_GT_CONTROLLER_FIRST || stage == Stage.WAIT_GT_CONTROLLER_SECOND) {
            if (!integratedServerHealthy(minecraft)) {
                finish(minecraft, false, "server stopped during GT placement");
                return;
            }
            int index = stage == Stage.WAIT_GT_WIRE ? 0 : stage == Stage.WAIT_GT_CABLE ? 1
                    : stage == Stage.WAIT_GT_CONTROLLER_FIRST ? 2 : 3;
            BlockPos pos = gtProbePositions[index];
            net.minecraft.tileentity.TileEntity tile = minecraft.theWorld.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
            if (!(tile instanceof gregtech.api.interfaces.tileentity.IGregTechTileEntity)
                    || ((gregtech.api.interfaces.tileentity.IGregTechTileEntity) tile).getMetaTileEntity() == null
                    || ((gregtech.api.interfaces.tileentity.IGregTechTileEntity) tile).getMetaTileID()
                            != gtProbeStacks[index].getItemDamage()) {
                failStageTimeout(minecraft, "GT subtype was not synchronized for probe " + index);
                return;
            }
            append("GT_NATIVE_PLACEMENT_OK index=" + index + " subtype=" + gtProbeStacks[index].getItemDamage());
            if (index < 3) {
                sendGtProbe(minecraft, index + 1);
                moveTo(index == 0 ? Stage.WAIT_GT_CABLE : index == 1
                        ? Stage.WAIT_GT_CONTROLLER_FIRST : Stage.WAIT_GT_CONTROLLER_SECOND);
                return;
            }
            rotatingTile = minecraft.theWorld.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
            RtsClientPacketGateway.sendSetMode(com.rtsbuilding.rtsbuilding.common.build.BuilderMode.ROTATE);
            com.rtsbuilding.rtsbuilding.client.screen.mode.PlacedBlockRotationHandles handles =
                    new com.rtsbuilding.rtsbuilding.client.screen.mode.PlacedBlockRotationHandles();
            if (!handles.select(minecraft.theWorld, pos, EnumFacing.NORTH) || handles.arcs(minecraft.theWorld, EnumFacing.NORTH).isEmpty()) {
                throw new IllegalStateException("GT rotation handles unavailable");
            }
            sendRotationProbe(minecraft);
            moveTo(Stage.WAIT_GT_ROTATION);
            return;
        }

        if (stage == Stage.WAIT_GT_ROTATION) {
            BlockPos pos = gtProbePositions[3];
            net.minecraft.tileentity.TileEntity tile = minecraft.theWorld.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
            if (tile != rotatingTile) throw new IllegalStateException("GT rotation replaced tile identity");
            if (((gregtech.api.interfaces.tileentity.IGregTechTileEntity) tile).getFrontFacing() != expectedFacing) {
                failStageTimeout(minecraft, "GT rotation did not synchronize");
                return;
            }
            rotationCount++;
            if (rotationCount < 4) {
                sendRotationProbe(minecraft);
                moveTo(Stage.WAIT_GT_ROTATION);
            } else {
                append("GT_ROTATION_FOUR_TURNS_OK");
                moveTo(Stage.OBSERVE_RTS_RENDER);
            }
            return;
        }

        if (stage == Stage.OBSERVE_RTS_RENDER) {
            for (int i = 0; i < gtProbePositions.length; i++) {
                BlockPos pos = gtProbePositions[i];
                net.minecraft.tileentity.TileEntity tile = minecraft.theWorld.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
                if (!(tile instanceof gregtech.api.interfaces.tileentity.IGregTechTileEntity)
                        || ((gregtech.api.interfaces.tileentity.IGregTechTileEntity) tile).getMetaTileEntity() == null
                        || ((gregtech.api.interfaces.tileentity.IGregTechTileEntity) tile).getMetaTileID()
                                != gtProbeStacks[i].getItemDamage()) {
                    throw new IllegalStateException("Earlier GT placement lost subtype: " + i);
                }
            }
            if (!ClientRtsController.get().isEnabled()) {
                finish(minecraft, false, "RTS disabled unexpectedly during render observation");
                return;
            }
            if (stageTicks < RTS_RENDER_TICKS) return;
            verifyHandleProjection(minecraft);
            append("GT_CONSECUTIVE_PLACEMENTS_STABLE");
            net.minecraft.util.ScreenShotHelper.saveScreenshot(minecraft.mcDataDir,
                    "rts-official-integrated.png", minecraft.displayWidth, minecraft.displayHeight,
                    minecraft.getFramebuffer());
            RtsClientPacketGateway.sendSetMode(com.rtsbuilding.rtsbuilding.common.build.BuilderMode.INTERACT);
            RtsClientPacketGateway.sendAreaDestroy(java.util.Arrays.asList(gtProbePositions), 0, "", null, false);
            append("GT_AREA_DESTRUCTION_SENT");
            moveTo(Stage.WAIT_GT_AREA_DESTRUCTION);
            return;
        }

        if (stage == Stage.WAIT_GT_AREA_DESTRUCTION) {
            if (!integratedServerHealthy(minecraft)) throw new IllegalStateException("GT area destruction stopped server");
            if (stageTicks < 40) return;
            for (BlockPos pos : gtProbePositions) {
                if (!minecraft.theWorld.isAirBlock(pos.getX(), pos.getY(), pos.getZ())) {
                    failStageTimeout(minecraft, "GT area destruction incomplete");
                    return;
                }
            }
            append("GT_AREA_DESTRUCTION_HISTORY_OK");
            RtsClientPacketGateway.sendToggleCamera(false);
            append("RTS_DISABLE_SENT");
            moveTo(Stage.WAIT_RTS_OFF);
            return;
        }

        if (stage == Stage.WAIT_RTS_OFF) {
            if (ClientRtsController.get().isEnabled()) {
                failStageTimeout(minecraft, "RTS disable acknowledgement timed out");
                return;
            }
            harvestProbeRequested = true;
            if (!harvestProbeDone) {
                failStageTimeout(minecraft, "tool-less harvest probe timed out");
                return;
            }
            if (harvestProbeFailure != null) throw new IllegalStateException(harvestProbeFailure);
            if (finalStableTicks == 0) {
                append("TOOLLESS_STONE_DIAMOND_GT_DROPS_BOTH_MODES_OK");
                minecraft.playerController.setGameType(GameType.SURVIVAL);
                minecraft.thePlayer.capabilities.isCreativeMode = false;
                minecraft.displayGuiScreen(new net.minecraft.client.gui.inventory.GuiInventory(minecraft.thePlayer));
            }
            finalStableTicks++;
            if (finalStableTicks >= FINAL_STABLE_TICKS) {
                net.minecraft.util.ScreenShotHelper.saveScreenshot(minecraft.mcDataDir,
                        "rts-inventory-no-button.png", minecraft.displayWidth, minecraft.displayHeight,
                        minecraft.getFramebuffer());
                finish(minecraft, true,
                        "worldTicks>=" + WORLD_STABLE_TICKS
                                + " rtsRenderTicks=" + RTS_RENDER_TICKS);
            }
        }
    }

    /** Exercises every top-bar state plus bundled fallback under a real GL context. */
    private static void verifyTopBarTextures(Minecraft minecraft) throws IOException {
        int count = 0;
        for (com.rtsbuilding.rtsbuilding.client.screen.topbar.TopBarTypes.TopBarButtonId id
                : com.rtsbuilding.rtsbuilding.client.screen.topbar.TopBarTypes.TopBarButtonId.values()) {
            for (com.rtsbuilding.rtsbuilding.client.screen.topbar.TopBarIconRenderer.VisualState state
                    : com.rtsbuilding.rtsbuilding.client.screen.topbar.TopBarIconRenderer.VisualState.values()) {
                net.minecraft.util.ResourceLocation location =
                        com.rtsbuilding.rtsbuilding.client.screen.topbar.TopBarIconRenderer.texture(id, state);
                if (location == null) continue;
                if (!minecraft.getTextureManager().loadTexture(location,
                        new com.rtsbuilding.rtsbuilding.client.screen.topbar.TopBarTexture(location)))
                    throw new IOException("Top-bar texture failed: " + location);
                count++;
            }
        }
        net.minecraft.client.resources.IResourceManager failing =
                (net.minecraft.client.resources.IResourceManager) java.lang.reflect.Proxy.newProxyInstance(
                        net.minecraft.client.resources.IResourceManager.class.getClassLoader(),
                        new Class<?>[] { net.minecraft.client.resources.IResourceManager.class },
                        (proxy, method, arguments) -> { throw new IOException("Intentional QA resource failure"); });
        com.rtsbuilding.rtsbuilding.client.screen.topbar.TopBarTexture fallback =
                new com.rtsbuilding.rtsbuilding.client.screen.topbar.TopBarTexture(new net.minecraft.util.ResourceLocation(
                        "rtsbuilding", "textures/gui/topbar/mode_rotate_active.png"));
        fallback.loadTexture(failing);
        fallback.deleteGlTexture();
        append("TOPBAR_ALL_STATES_AND_BUNDLED_FALLBACK_OK count=" + count);
    }

    private static void failStageTimeout(Minecraft minecraft, String message) {
        if (stageTicks > STAGE_TIMEOUT_TICKS) finish(minecraft, false, message);
    }

    /** Projects visible handle geometry and checks picking through those exact framebuffer pixels. */
    private static void verifyHandleProjection(Minecraft minecraft) {
        try {
            Class<?> type = com.rtsbuilding.rtsbuilding.client.rendering.util.RtsCursorRay.class;
            java.lang.reflect.Field modelField = type.getDeclaredField("MODEL");
            java.lang.reflect.Field projectionField = type.getDeclaredField("PROJECTION");
            java.lang.reflect.Field viewportField = type.getDeclaredField("VIEWPORT");
            java.lang.reflect.Field originField = type.getDeclaredField("renderOrigin");
            modelField.setAccessible(true); projectionField.setAccessible(true);
            viewportField.setAccessible(true); originField.setAccessible(true);
            Vec3d origin = (Vec3d) originField.get(null);
            com.rtsbuilding.rtsbuilding.client.screen.culling.RtsCullingBox box =
                    new com.rtsbuilding.rtsbuilding.client.screen.culling.RtsCullingBox(1,
                            creativePrototypePlacedPos, creativePrototypePlacedPos.add(3, 2, 3));
            java.nio.FloatBuffer pixel = org.lwjgl.BufferUtils.createFloatBuffer(4);
            for (com.rtsbuilding.rtsbuilding.client.screen.culling.RtsCullingAxisHandle.Handle handle :
                    com.rtsbuilding.rtsbuilding.client.screen.culling.RtsCullingAxisHandle.handles(box)) {
                Vec3d center = handle.head().getCenter().subtract(origin);
                if (!org.lwjgl.util.glu.GLU.gluProject((float) center.x, (float) center.y, (float) center.z,
                        (java.nio.FloatBuffer) modelField.get(null), (java.nio.FloatBuffer) projectionField.get(null),
                        (java.nio.IntBuffer) viewportField.get(null), pixel)) throw new IllegalStateException("Handle projection failed");
                com.rtsbuilding.rtsbuilding.client.rendering.util.RtsCursorRay.Snapshot ray =
                        com.rtsbuilding.rtsbuilding.client.rendering.util.RtsCursorRay.fromRenderedFrame(minecraft, pixel.get(0), pixel.get(1));
                if (ray == null || !com.rtsbuilding.rtsbuilding.client.screen.culling.RtsCullingAxisHandle.nearestHit(
                        box, ray.origin(), ray.direction(), 128, java.util.Collections.singleton(handle.direction())).isPresent()) {
                    throw new IllegalStateException("Visible arrow cannot be picked: " + handle.direction());
                }
            }
            append("SIX_HANDLE_FRAMEBUFFER_PICKING_OK");
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Handle projection probe failed", failure);
        }
    }

    /** Exercises the actual rotation packet and native client-facing synchronization. */
    private static void sendRotationProbe(Minecraft minecraft) {
        expectedFacing = com.rtsbuilding.rtsbuilding.common.placement.GtMachineRotation.target(
                minecraft.theWorld, gtProbePositions[3], EnumFacing.UP, 1);
        if (expectedFacing == net.minecraftforge.common.util.ForgeDirection.UNKNOWN) {
            throw new IllegalStateException("GT controller has no legal horizontal turn");
        }
        RtsClientPacketGateway.sendRotateBlockStep(gtProbePositions[3], EnumFacing.UP, 1);
    }

    /** Validates history serialization against the running pack's case-sensitive registry. */
    private static void verifyHistoryStateCodec() {
        try {
            Class<?> codec = com.rtsbuilding.rtsbuilding.server.service.destruction.RtsDestructionBatch.class;
            java.lang.reflect.Method write = codec.getDeclaredMethod("writeBlockState", com.rtsbuilding.rtsbuilding.platform.block.BlockState.class);
            java.lang.reflect.Method read = codec.getDeclaredMethod("readBlockState", net.minecraft.nbt.NBTTagCompound.class);
            write.setAccessible(true);
            read.setAccessible(true);
            int mixedCase = 0;
            for (Object entry : cpw.mods.fml.common.registry.GameData.getBlockRegistry()) {
                net.minecraft.block.Block block = (net.minecraft.block.Block) entry;
                String name = cpw.mods.fml.common.registry.GameData.getBlockRegistry().getNameForObject(block);
                if (name == null || name.equals(name.toLowerCase(java.util.Locale.ROOT))) continue;
                com.rtsbuilding.rtsbuilding.platform.block.BlockState state = com.rtsbuilding.rtsbuilding.platform.block.BlockState.of(block, 7);
                Object restored = read.invoke(null, write.invoke(null, state));
                if (!state.equals(restored)) throw new IllegalStateException("History lost state: " + name);
                mixedCase++;
            }
            if (mixedCase == 0) throw new IllegalStateException("No mixed-case history fixture");
            append("DESTRUCTION_HISTORY_CASE_AND_METADATA_OK count=" + mixedCase);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("History probe failed", failure);
        }
    }

    /** Sends the same creative batch request used by RTS for real GT wire/cable prototypes. */
    private static void sendGtProbe(Minecraft minecraft, int index) {
        ItemStack stack = gtProbeStacks[index];
        if (stack == null) throw new IllegalStateException("GT probe item unavailable");
        BlockPos pos = gtProbePositions[index];
        Vec3d hit = new Vec3d(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        Vec3d origin = new Vec3d(minecraft.thePlayer.posX,
                minecraft.thePlayer.posY + minecraft.thePlayer.getEyeHeight(), minecraft.thePlayer.posZ);
        if (index >= 2) {
            EnumFacing face = EnumFacing.UP;
            Vec3d controllerHit = new Vec3d(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            RtsClientPacketGateway.sendInteractBlockWithPinnedItem(
                    new RayTraceResult(controllerHit, face, pos.down()),
                    net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem()), stack,
                    origin, controllerHit.subtract(origin).normalize());
            append("GT_SINGLE_CONTROLLER_SENT index=" + index);
            return;
        }
        RtsClientPacketGateway.sendPlaceBatch(java.util.Collections.singletonList(
                new RayTraceResult(hit, EnumFacing.UP, pos)), false, true,
                net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem()), stack, 0,
                origin, hit.subtract(origin).normalize());
        append("GT_NATIVE_PLACEMENT_SENT index=" + index);
    }

    private static boolean integratedServerHealthy(Minecraft minecraft) {
        return minecraft != null && minecraft.getIntegratedServer() != null
                && minecraft.getIntegratedServer().isServerRunning();
    }

    /** 在玩家附近寻找一个上方为空气的实体方块，避免探针依赖固定出生地地形。 */
    private static BlockPos findCreativePlacementAnchor(Minecraft minecraft) {
        BlockPos center = com.rtsbuilding.rtsbuilding.platform.player.PlayerCompat
                .blockPosition(minecraft.thePlayer);
        for (int radius = 3; radius <= 8; radius++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 2; dy >= -6; dy--) {
                    BlockPos anchor = center.add(radius, dy, dz);
                    BlockPos above = anchor.up();
                    if (!minecraft.theWorld.isAirBlock(
                            anchor.getX(), anchor.getY(), anchor.getZ())
                            && minecraft.theWorld.isAirBlock(
                            above.getX(), above.getY(), above.getZ())) {
                        return anchor;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 1.7.10 按 en_US / zh_CN 形式精确寻找 .lang；文件名沿用现代小写格式时，
     * {@link I18n#format(String, Object...)} 会原样返回 key。把这项检查放进真客户端，
     * 可以同时覆盖资源是否进 JAR、语言管理器是否加载以及最终 UI 翻译调用三层边界。
     */
    private static void verifyTranslation(Minecraft minecraft, String key) {
        String translated = I18n.format(key);
        if (translated == null || translated.trim().isEmpty() || key.equals(translated)) {
            finish(minecraft, false, "i18n unresolved key=" + key);
            throw new IllegalStateException("I18n unresolved: " + key);
        }
        append("I18N_OK key=" + key + " value=" + translated);
    }

    private static void moveTo(Stage next) {
        stage = next;
        stageTicks = 0;
    }

    private static void finish(Minecraft minecraft, boolean success, String detail) {
        if (finished) return;
        finished = true;
        String status = success ? "PASS" : "FAIL";
        append(status + " " + detail);
        System.out.println("RTS_112_CLIENT_SMOKE " + status + " " + detail);
        if (minecraft != null) minecraft.shutdown();
    }

    private static Path resolveReportPath() {
        if (!ENABLED) return null;
        String configured = System.getProperty(REPORT_PROPERTY, "").trim();
        if (configured.isEmpty()) {
            throw new IllegalStateException(REPORT_PROPERTY + " must be set for client smoke");
        }
        return Paths.get(configured).toAbsolutePath().normalize();
    }

    private static void append(String line) {
        if (REPORT_PATH == null) return;
        try {
            Path parent = REPORT_PATH.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.write(REPORT_PATH,
                    (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException failure) {
            throw new IllegalStateException("Unable to write client smoke report", failure);
        }
    }

    private static String safeMessage(Throwable failure) {
        return failure.getMessage() == null ? "" : failure.getMessage();
    }

    private enum Stage {
        WAIT_MAIN_MENU,
        WAIT_WORLD,
        WAIT_RTS_ON,
        WAIT_GT_ROTATION,
        WAIT_GT_AREA_DESTRUCTION,
        WAIT_NULL_PROTOTYPE_PLACE,
        WAIT_GT_WIRE,
        WAIT_GT_CABLE,
        WAIT_GT_CONTROLLER_FIRST,
        WAIT_GT_CONTROLLER_SECOND,
        WAIT_QUICK_DROP,
        WAIT_EMPTY_TOOL_MINE_START,
        WAIT_EMPTY_TOOL_MINE_ABORT,
        WAIT_EMPTY_HAND_INTERACTION,
        WAIT_CREATIVE_PINNED_PROTOTYPE,
        WAIT_DURABLE_DESTRUCTION,
        WAIT_DURABLE_PLACEMENT,
        OBSERVE_RTS_RENDER,
        WAIT_RTS_OFF
    }
}
