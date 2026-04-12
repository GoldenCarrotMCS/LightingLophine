# Folia 绝对安全补丁移植计划 (Folia Safe Optimizations Plan)

## 核心理念
本计划的精髓在于**“避开游戏特性，专攻底层短板”**。通过对内存、数学运算和网络协议的底层重构，Folia 分支可以在保留 100% 生电兼容性、不损失任何红石机器和刷怪塔功能的前提下，获得极高的底层性能增益。

---

## 阶段规划

### 第一阶段：内存与集合基建重构 (Memory & Collections)
**思考逻辑**：Minecraft 服务端在运行过程中（尤其是生电服的密集场景下），会产生极其庞大的对象分配开销。Folia 的多线程架构虽然缓解了主线程压力，但如果不同 Region 的线程都在疯狂触发 GC，依然会导致全局卡顿。将低效的包装类（Wrapper）替换为基本数据类型（Primitive），将频繁 new 的小对象池化，是性价比最高且绝对安全的优化。

1. **基于 ThreadLocal 的基本数据结构池化 (Object Pooling)**
   - **目标对象**：`BlockPos.MutableBlockPos`、`BitSet` 等在寻路、流体计算中被海量临时创建的对象。
   - **实现思路**：利用 `ThreadLocal` 为每个 Region 线程维护一个固定大小的对象池。需要时 `acquire`，用完 `release`。
   - **预期收益**：极大降低新生代 GC（Young GC）的频率，减少多线程下的内存分配竞争。

2. **高性能类型过滤集合 (Type-Filterable Collections)**
   - **目标问题**：原版在执行 `getEntitiesOfClass` 时，通常需要遍历整个区块内的所有实体并使用 `instanceof` 判断。
   - **实现思路**：引入 `fastutil` 的基本类型集合，或者维护一个按 `EntityType` 映射的二维数组/链表。
   - **预期收益**：在漏斗检索矿车、刷怪塔检测上限等场景，将 O(N) 的遍历时间复杂度降至 O(1) 或极小的常数级。

3. **消除 Integer 装箱/拆箱的自定义映射表 (Primitive BiMap)**
   - **目标问题**：协议层和实体 ID 映射大量使用 `HashMap<Integer, Object>`，产生大量不必要的 Integer 对象和哈希碰撞。
   - **实现思路**：编写一个 `Int2ObjectBiMap`，底层使用平行的 `int[]` 和 `Object[]` 数组进行索引和存储。
   - **预期收益**：彻底消除自动装箱开销，内存占用极小化，且数组遍历时的 CPU L1 缓存命中率极高。

### 第二阶段：纯数学运算极速化 (Math & Algorithms)
**思考逻辑**：数学计算的结果是绝对确定的。只要我们能用更少的 CPU 时钟周期算出相同的结果，这就是一次完美的无痛优化。

1. **紧凑型三角函数查找表 (Compact Sine LUT)**
   - **目标问题**：原版的 `Mth.sin()` 背后是一个巨大的浮点数组缓存，在多线程高频调用时会导致严重的 CPU 缓存未命中（Cache Miss）。
   - **实现思路**：参考 Aki-Async 的 CompactSineLUT，缩小数组尺寸，或者在精度允许的范围内使用泰勒展开的近似算法。
   - **预期收益**：减少 L2/L3 缓存的换页开销，让 CPU 核心能更专注于其他逻辑运算。

2. **位运算替代浮点数学函数 (Bitwise Math Replacements)**
   - **目标问题**：像 `Math.log`、`Math.pow` 或求对数、开方等操作在 CPU 指令中耗时较长。
   - **实现思路**：对于求 2 的幂、对数（如 `ceilLog2`）等特定场景，使用德布鲁因序列（De Bruijn sequence）和位移操作（`>>`、`<<`）直接处理。
   - **预期收益**：指令周期从几十个缩短到几个，纯整数运算速度极快。

3. **SIMD 向量化 AABB 包围盒检测 (Vectorized AABB)**
   - **目标问题**：当发生爆炸或多实体挤压时，判断两个长方体（AABB）是否相交需要进行大量的 min/max 比较。
   - **实现思路**：利用 Java 的 `jdk.incubator.vector` API，将实体的 X/Y/Z 坐标打包成 Vector 寄存器，一条 CPU 指令（如 AVX2/AVX-512）同时对比多个实体的坐标边界。
   - **预期收益**：密集碰撞检测的性能可提升 2~4 倍，且完全不改变任何判定结果。

### 第三阶段：网络层降本增效 (Network & IO)
**思考逻辑**：网络发包完全是服务端的单向输出，只要符合 Minecraft 的网络协议规范，怎么发、发什么压缩包都不会影响服务端的内部逻辑状态。

1. **原生压缩与加密引擎 (Native Crypto & Deflate)**
   - **目标问题**：Java 原生的 Zlib 压缩和加解密引擎效率较低，占用大量 CPU。
   - **实现思路**：移植 Velocity 代理端使用的 `libdeflate` (C/C++ 编写的高效压缩库) 和 OpenSSL 加密模块，通过 JNI/JNA 调用。
   - **预期收益**：网络压缩与解压速度提升 200%~300%，极大释放 Netty IO 线程的 CPU 占用。

2. **MTU 感知的数据包批处理 (MTU-Aware Flush Consolidation)**
   - **目标问题**：在红石机器运行或大量玩家移动时，服务端会频繁调用 Netty 的 `flush()`，导致大量的内核态系统调用（Syscalls）。
   - **实现思路**：在 Netty 的 Pipeline 中拦截发包请求，将多个小包（如声音、粒子、单个方块更新）积累在一个 ByteBuf 中，直到达到网络的最大传输单元（MTU，约 1400 字节）时再一次性 Flush。
   - **预期收益**：大幅减少网卡中断频率和 Linux 内核上下文切换开销，降低网络延迟。

3. **快速 VarInt 序列化 (Fast VarInt Encoding)**
   - **目标问题**：Minecraft 协议大量使用可变长度整数（VarInt）。原版的循环位移计算不够高效。
   - **实现思路**：利用循环展开（Loop Unrolling）和硬编码的分支预测（Fast Paths），针对最常见的 1 字节和 2 字节 VarInt 提供极速写入路径。
   - **预期收益**：每个数据包的构建速度都能得到微小提升，积少成多，收益可观。

---

## 配置文件设计 (folia-optimizations.yml)

为了防止环境不兼容或插件冲突，以上优化项均支持通过配置开启/关闭，以下为 YAML 配置设计草案：

```yaml
# ==========================================
# Folia 底层安全优化补丁配置 / Folia Safe Optimizations Configuration
# ==========================================
# 这些优化专注于数学运算、内存管理、集合结构和网络协议层。
# 它们被设计为绝对安全，完全不修改游戏核心机制（如实体 Tick、方块更新、红石时序）。
# 推荐在生电服 (TMC) 或高并发生存服中全部开启。

optimizations:
  
  # ------------------------------------------
  # 1. 内存与集合基建重构 (Memory & Collections)
  # ------------------------------------------
  memory-and-collections:
    object-pooling:
      enabled: true
      max-blockpos-pool-size: 512
    type-filterable-collections:
      enabled: true
    primitive-bimap:
      enabled: true

  # ------------------------------------------
  # 2. 纯数学运算极速化 (Math & Algorithms)
  # ------------------------------------------
  math-and-algorithms:
    compact-sine-lut:
      enabled: true
    fast-bitwise-math:
      enabled: true
    vectorized-aabb:
      enabled: true

  # ------------------------------------------
  # 3. 网络层降本增效 (Network & IO)
  # ------------------------------------------
  network-and-io:
    native-compression:
      enabled: true
      compression-level: 6
    fast-varint:
      enabled: true
    mtu-flush-consolidation:
      enabled: true
      explicit-flush-after: 256
```

---

## 补丁集成指南 (Patch Implementation)

1. **配置类引入**：
   在 `lophine-server` 源码中新增 `FoliaOptimizationConfig.java` 类，使用 `@ConfigClassInfo` 自动生成和绑定上述配置。
2. **源码修改注入**：
   执行 `./gradlew applyPatches` 生成 `work/` 目录，进入对应原版源码（如 `net.minecraft.util.Mth`）修改底层实现，并插入相应的配置开关。
3. **Patch 生成与提交**：
   使用 Git 在工作区 `commit` 修改，返回根目录执行 `./gradlew rebuildPatches` 重新生成 `.patch` 补丁，最终推送到 GitHub 维护分支。
