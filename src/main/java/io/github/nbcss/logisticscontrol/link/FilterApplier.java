package io.github.nbcss.logisticscontrol.link;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.SidedFilteringBehaviour;
import io.github.nbcss.logisticscontrol.CreateLogisticsControl;
import io.github.nbcss.logisticscontrol.ServerConfig;
import io.github.nbcss.logisticscontrol.filter.Labels;
import io.github.nbcss.logisticscontrol.filter.PackageFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Applies a package's filter to its Filter Link's targets when the package is unwrapped: a filter sets them, no filter
 * clears them, {@link PackageFilter#KEEP} leaves them alone.
 */
public final class FilterApplier {
    private FilterApplier() {}

    public static int range() {
        return ServerConfig.filterLinkRange();
    }

    public static boolean hasFilter(BlockGetter level, BlockPos pos) {
        return BlockEntityBehaviour.get(level, pos, FilteringBehaviour.TYPE) != null;
    }

    public static void applyFromBox(PackagerBlockEntity packager, ItemStack box) {
        if (!ServerConfig.filterLinkEnabled()) return;
        Level level = packager.getLevel();
        if (level == null || level.isClientSide) return;
        // apply links
        BlockPos pos = packager.getBlockPos();
        ItemStack filter = null;
        for (Direction d : Direction.values()) {
            BlockPos adj = pos.relative(d);
            BlockState adjState = level.getBlockState(adj);
            if (adjState.getBlock() == CreateLogisticsControl.FILTER_LINK.get()
                    && FilterLinkBlock.getConnectedDirection(adjState) == d
                    && level.getBlockEntity(adj) instanceof FilterLinkBlockEntity link) {
                if (filter == null) {
                    PackageFilter pf = box.get(CreateLogisticsControl.PACKAGE_FILTER.get());
                    if (pf != null && pf.isKeep()) return;   // a split order's leftovers: leave the filters alone
                    filter = pf == null || !Labels.canLabel(pf.stack()) ? ItemStack.EMPTY : pf.stack();
                }
                apply(level, link, filter);
            }
        }
    }

    public static void apply(Level level, FilterLinkBlockEntity link, ItemStack filter) {
        ItemStack toApply = filter.isEmpty() ? ItemStack.EMPTY : filter.copyWithCount(1);
        boolean applied = false;
        for (BlockPos target : link.getTargets()) {
            FilteringBehaviour beh = BlockEntityBehaviour.get(level, target, FilteringBehaviour.TYPE);
            if (beh == null) continue;
            if (!(beh instanceof SidedFilteringBehaviour sided)) {
                applied |= beh.setFilter(toApply);
                continue;
            }
            boolean faceApplied = false;
            for (Direction direction : Direction.values()) {
                FilteringBehaviour side = sided.get(direction);
                if (side != null) faceApplied |= side.setFilter(toApply);
            }
            applied |= faceApplied;
        }
        if (applied) link.flash();
    }
}
