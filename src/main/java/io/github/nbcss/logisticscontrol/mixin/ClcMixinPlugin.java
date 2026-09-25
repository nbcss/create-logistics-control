package io.github.nbcss.logisticscontrol.mixin;

import net.neoforged.fml.loading.LoadingModList;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class ClcMixinPlugin implements IMixinConfigPlugin {

    private static final String DEPLOYER_ORDER_PACKET_MIXIN =
        "io.github.nbcss.logisticscontrol.mixin.GenericOrderRequestPacketMixin";
    private static final String FLUID_REPACKAGER_MIXIN =
        "io.github.nbcss.logisticscontrol.mixin.FluidRepackagerFilterMixin";
    private static final String FLUID_PACKAGE_STAMP_MIXIN =
        "io.github.nbcss.logisticscontrol.mixin.FluidPackageStampMixin";
    private static final String PHANTOM_ORDER_MIXIN =
        "io.github.nbcss.logisticscontrol.mixin.PhantomTickerOrderMixin";
    private static final String PHANTOM_FILTER_MIXIN =
        "io.github.nbcss.logisticscontrol.mixin.PhantomTickerFilterMixin";
    private static final String CMP_ORDER_MIXIN =
        "io.github.nbcss.logisticscontrol.mixin.CmpTickerOrderMixin";
    private static final String CMP_FILTER_MIXIN =
        "io.github.nbcss.logisticscontrol.mixin.CmpTickerFilterMixin";

    private static final boolean DEPLOYER_PRESENT = isModPresent("deployer");
    private static final boolean FLUIDLOGISTICS_PRESENT = isModPresent("fluidlogistics");
    private static final boolean CREATEPHANTOM_PRESENT = isModPresent("createphantom");
    // Phantom < 1.0.2 builds its order inline in sendIt (no `order` local for PhantomTickerFilterMixin to modify).
    private static final boolean CREATEPHANTOM_1_0_2 = isModAtLeast("createphantom", "1.0.2");
    private static final boolean CMP_PRESENT = isModPresent("create_mobile_packages");

    private static boolean isModPresent(String modId) {
        try {
            return LoadingModList.get().getModFileById(modId) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isModAtLeast(String modId, String minVersion) {
        try {
            var file = LoadingModList.get().getModFileById(modId);
            if (file == null) return false;
            for (var mod : file.getMods())
                if (modId.equals(mod.getModId()))
                    return mod.getVersion().compareTo(new DefaultArtifactVersion(minVersion)) >= 0;
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (DEPLOYER_ORDER_PACKET_MIXIN.equals(mixinClassName)) return DEPLOYER_PRESENT;
        if (FLUID_REPACKAGER_MIXIN.equals(mixinClassName)) return FLUIDLOGISTICS_PRESENT;
        if (FLUID_PACKAGE_STAMP_MIXIN.equals(mixinClassName)) return FLUIDLOGISTICS_PRESENT;
        if (PHANTOM_ORDER_MIXIN.equals(mixinClassName)) return CREATEPHANTOM_PRESENT;
        if (PHANTOM_FILTER_MIXIN.equals(mixinClassName)) return CREATEPHANTOM_1_0_2;
        if (CMP_ORDER_MIXIN.equals(mixinClassName)) return CMP_PRESENT;
        if (CMP_FILTER_MIXIN.equals(mixinClassName)) return CMP_PRESENT;
        return true;
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
