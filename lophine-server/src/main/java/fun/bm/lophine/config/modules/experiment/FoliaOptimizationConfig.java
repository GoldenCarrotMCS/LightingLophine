package fun.bm.lophine.config.modules.experiment;

import me.earthme.luminol.config.IConfigModule;
import me.earthme.luminol.config.flags.ConfigClassInfo;
import me.earthme.luminol.config.flags.ConfigInfo;
import me.earthme.luminol.enums.EnumConfigCategory;

@ConfigClassInfo(category = EnumConfigCategory.EXPERIMENT, name = "folia_optimization")
public class FoliaOptimizationConfig implements IConfigModule {
    
    @ConfigInfo(name = "memory.blockpos-pooling", comments = "Enable BlockPos pooling for Folia region threads")
    public static boolean blockposPooling = true;

    @ConfigInfo(name = "memory.max-blockpos-pool-size", comments = "Maximum number of cached MutableBlockPos per thread")
    public static int maxBlockposPoolSize = 512;

    @ConfigInfo(name = "memory.type-filterable-collections", comments = "Enable fastutil type-filterable collections for O(1) entity lookups")
    public static boolean typeFilterableCollections = true;
}
