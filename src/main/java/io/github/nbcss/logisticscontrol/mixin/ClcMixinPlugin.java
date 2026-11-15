package io.github.nbcss.logisticscontrol.mixin;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Applies each addon's adapter mixins only when that addon is installed. */
public class ClcMixinPlugin implements IMixinConfigPlugin {

    private static final String PACKAGE = "io.github.nbcss.logisticscontrol.mixin.";

    private static final boolean CFA_PRESENT = isModPresent("create_factory_abstractions");
    private static final boolean CFL_PRESENT = isModPresent("fluidlogistics");
    private static final boolean CMP_PRESENT = isModPresent("create_mobile_packages");

    private static boolean isModPresent(String modId) {
        try {
            return LoadingModList.get().getModFileById(modId) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith(PACKAGE + "cfa.")) return CFA_PRESENT;
        if (mixinClassName.startsWith(PACKAGE + "cfl.")) return CFL_PRESENT;
        if (mixinClassName.startsWith(PACKAGE + "cmp.")) return CMP_PRESENT && CFA_PRESENT;
        return true;
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
