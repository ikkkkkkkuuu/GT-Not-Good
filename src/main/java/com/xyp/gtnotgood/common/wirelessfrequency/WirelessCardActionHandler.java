// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure PR #627 at 651396c75d108e6ff5399a44c56c12214ad27d3d.

package com.xyp.gtnotgood.common.wirelessfrequency;

import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.CLOSE_DETAILS;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.CYCLE_VISUALISATION;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.DELETE_SOURCE;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.HIGHLIGHT;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.OPEN_DETAILS;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.REFRESH;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.RENAME;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.RENAME_SOURCE;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.SELECT_SOURCE;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.TELEPORT;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.TOGGLE_AUTO;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.TOGGLE_GROUPING;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.TOGGLE_PAUSE;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.isRowAction;

import java.util.Comparator;
import java.util.stream.Collectors;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentTranslation;

public class WirelessCardActionHandler {

    private final WirelessCardContainer session;
    private long nextRequestTick = Long.MIN_VALUE;

    WirelessCardActionHandler(WirelessCardContainer session) {
        this.session = session;
    }

    void execute(long revision, int action, long targetId, String text, long nameRevision) {
        EntityPlayer player = session.player;
        ItemStack card = session.card;
        WirelessCardBinding binding = session.binding;
        if (action == CLOSE_DETAILS) {
            if (session.view.detailNode != null) {
                session.view.detailNode = null;
                session.update(true);
            }
            return;
        }
        if (
            isRowAction(action) && action != OPEN_DETAILS
                && action != TOGGLE_PAUSE
                && action != TELEPORT
                && action != HIGHLIGHT
                && action != RENAME_SOURCE
                && action != DELETE_SOURCE
        ) return;
        if (!acceptRequest(player.worldObj.getTotalWorldTime())) return;
        if (action == OPEN_DETAILS) session.update(false);
        if (session.revision != revision) {
            session.update(true);
            return;
        }
        var row = session.snapshot == null ? null : session.snapshot.target(targetId);
        if (isRowAction(action) && action != RENAME_SOURCE && action != DELETE_SOURCE || action == RENAME) {
            if (binding == null || row == null) {
                message("stale");
                session.update(true);
                return;
            }
        }
        if (action == SELECT_SOURCE || action == RENAME_SOURCE || action == DELETE_SOURCE) {
            var sources = WirelessCardBinding.sources(card.getTagCompound());
            if (targetId < 1 || targetId > sources.size()) return;
            var selected = sources.get((int) targetId - 1);
            if (!selected.belongsTo(player.getUniqueID())) return;
            if (action == DELETE_SOURCE) {
                session.deleteSource((int) targetId - 1);
            } else if (action == RENAME_SOURCE) {
                if (!WirelessCardBinding.renameSource(card.getTagCompound(), (int) targetId - 1, text)) return;
                WirelessCardInventory.sync(player, card);
            } else {
                var node = selected.source().node();
                if (node != null && !WirelessChannelManager.canBuild(node, player)) {
                    message("denied");
                    return;
                }
                session.selectSource((int) targetId - 1);
            }
        } else if (action == CYCLE_VISUALISATION) {
            WirelessCardVisualisation.cycle(card, player);
        } else if (action == RENAME) {
            if (session.view.detailNode == null || !row.members().contains(session.view.detailNode)) return;
            if (session.view.nameRevision() != nameRevision) {
                message("stale");
            } else {
                try {
                    if (!WirelessOperations.rename(player, binding.source(), row, text)) message("denied");
                } catch (WirelessOperations.Denied denied) {
                    message("denied");
                } catch (IllegalStateException stale) {
                    message("stale");
                }
            }
        } else if (action == TELEPORT && player instanceof EntityPlayerMP serverPlayer) {
            try {
                switch (WirelessOperations.teleport(serverPlayer, binding.source(), row)) {
                    case STALE -> message("stale");
                    case UNAVAILABLE -> message("teleport_unavailable");
                    case DENIED -> message("denied");
                    case APPLIED -> {
                        return;
                    }
                    default -> {}
                }
            } catch (PhysicalClusterTracker.ScanLimitException limit) {
                message("stale");
            }
        } else if (action == HIGHLIGHT) {
            try {
                if (!WirelessClusterManager.isCurrentLink(binding.source(), row)) {
                    message("stale");
                    session.update(true);
                    return;
                }
            } catch (PhysicalClusterTracker.ScanLimitException limit) {
                message("stale");
                return;
            }
            if (row.target().dimension() != player.dimension) {
                message("highlight_dimension", row.target().dimension());
            } else {
                session.pendingHighlight = new NBTTagList();
                var targets = WirelessClusterManager.highlight(row).stream()
                    .filter(target -> target.dimension() == player.dimension)
                    .sorted(
                        Comparator.comparingDouble(target -> player.getDistanceSq(target.x(), target.y(), target.z())))
                    .limit(512).collect(Collectors.toList());
                for (var target : targets) {
                    NBTTagCompound pos = new NBTTagCompound();
                    pos.setInteger("dimension", target.dimension());
                    pos.setInteger("x", target.x());
                    pos.setInteger("y", target.y());
                    pos.setInteger("z", target.z());
                    session.pendingHighlight.appendTag(pos);
                }
                message("highlight_started", targets.size());
            }
        } else if (action == TOGGLE_AUTO) {
            WirelessAutoConnect.toggleAutomatic(player, card);
        } else if (action == TOGGLE_GROUPING && session.view.detailNode == null) {
            WirelessCardBinding.toggleGrouping(card);
            WirelessCardInventory.sync(player, card);
        } else if (action == OPEN_DETAILS) {
            if (session.view.detailNode == null && row instanceof IWirelessLinkView.ClusterView)
                session.view.detailNode = row.node();
        } else if (action == TOGGLE_PAUSE) {
            try {
                WirelessOperations.setPaused(player, binding.source(), row);
            } catch (WirelessOperations.Denied denied) {
                message("denied");
            } catch (IllegalStateException stale) {
                message("stale");
            }
        } else if (action != REFRESH) return;
        session.update(true);
    }

    private boolean acceptRequest(long tick) {
        if (tick < nextRequestTick) return false;
        nextRequestTick = tick + 4;
        return true;
    }

    private void message(String key, Object... args) {
        session.player.addChatMessage(new ChatComponentTranslation("gtnotgood.wireless.gui." + key, args));
    }
}
