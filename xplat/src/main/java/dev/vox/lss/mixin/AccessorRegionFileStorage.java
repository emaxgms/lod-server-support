package dev.vox.lss.mixin;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.io.IOException;

/**
 * Exposes {@code RegionFileStorage}'s create-on-demand region-file lookup for the Phase 3
 * background-read split (perf-round plan R1): the raw-fetch executor task needs a region
 * file that is never null — {@code RegionFileRawRead} dereferences it immediately
 * ({@code synchronized (regionFile)}, offset read) and signals chunk absence downstream
 * through the offset-0 / missing-stream shapes it mirrors. That is exactly the pre-26.3
 * contract of the single-arg lookup; 26.3 split the method and made the non-creating
 * variant ({@code getRegionFile(pos, false)}, used by vanilla's own read path) nullable,
 * so this line deliberately keeps the creating one.
 *
 * <p>26.3: {@code getRegionFile(ChunkPos)} was split into {@code getRegionFile(ChunkPos,
 * boolean create)} and {@code getOrCreateRegionFile(ChunkPos)}. We invoke the latter —
 * bytecode-verified to be {@code getRegionFile(pos, true)} plus a null check, i.e. the
 * same never-null create-on-demand behaviour the single-arg method had. NOTE: the
 * NeoForge module is NOT bumped to 26.3 in this port, so this shared (xplat) accessor only
 * holds for the Fabric line; a NeoForge 26.2 build would need a per-line seam here.
 *
 * <p>Pinned by the mixin-config listing leg in {@code ChannelAccessorContractTest} —
 * an unregistered accessor resolves but is never applied, and every raw read would
 * ClassCastException into the per-chunk triage.
 */
@Mixin(RegionFileStorage.class)
public interface AccessorRegionFileStorage {

    @Invoker("getOrCreateRegionFile")
    RegionFile lss$getOrCreateRegionFile(ChunkPos pos) throws IOException;
}
