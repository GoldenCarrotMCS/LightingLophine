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

    @ConfigInfo(name = "memory.primitive-bimap", comments = "Use Int2ObjectBiMap for registries and network ID maps")
    public static boolean primitiveBimap = true;

    @ConfigInfo(name = "math.compact-sine-lut", comments = "Use compact sine lookup table")
    public static boolean compactSineLut = true;

    @ConfigInfo(name = "math.fast-bitwise-math", comments = "Use fast bitwise operations for math functions")
    public static boolean fastBitwiseMath = true;

    @ConfigInfo(name = "math.vectorized-aabb", comments = "Use Vector API for AABB intersection checks")
    public static boolean vectorizedAabb = true;

    @ConfigInfo(name = "network.native-compression", comments = "Use libdeflate for network compression")
    public static boolean nativeCompression = true;

    @ConfigInfo(name = "network.compression-level", comments = "Compression level for native deflate")
    public static int compressionLevel = 6;

    @ConfigInfo(name = "network.fast-varint", comments = "Use unrolled loop for VarInt encoding/decoding")
    public static boolean fastVarint = true;

    @ConfigInfo(name = "network.mtu-flush-consolidation", comments = "Consolidate Netty flushes up to MTU size")
    public static boolean mtuFlushConsolidation = true;

    @ConfigInfo(name = "network.explicit-flush-after", comments = "Force flush after this many packets")
    public static int explicitFlushAfter = 256;
}
