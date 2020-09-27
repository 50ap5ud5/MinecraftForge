package net.minecraftforge.dimension;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.Validate;

import com.mojang.serialization.Lifecycle;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Dimension;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.DimensionSettings;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IServerConfiguration;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.dimension.DimensionRegisterEvent;

public class DynamicDimensionManager {
    
    private static DynamicDimensionManager DIM_MANAGER = new DynamicDimensionManager();
    
    /*
     * MinecraftServer not loaded at time of dimension creation, so we need to hold a copy of the dynamic registries
     * Not really pretty but we need it. 
     */
    private DynamicRegistries.Impl dynamicRegistries;
    private IServerConfiguration serverWorldInfo;
    
    public static DynamicDimensionManager getDimensionManager() {
        return DIM_MANAGER;
    }
    
    public void initDimensionManager(DynamicRegistries.Impl dynamicregistries, IServerConfiguration serverWorldInfo) {
        this.dynamicRegistries = dynamicregistries;
        this.serverWorldInfo = serverWorldInfo;
    }
    
    public void onIntegratedServerStop() {
        this.dynamicRegistries = null;
        this.serverWorldInfo = null;
    }
    
    /**
     * Checks if the server is ready to accept registrations of dimension types and dimensions
     * @return true if ready, false if not
     */
    public boolean isReady() {
        return this.dynamicRegistries != null && this.serverWorldInfo != null;
    }
    
    /**
     * INTERNAL METHOD, DO NOT CALL
     */
    public static void onDimensionRegister() {
        MinecraftForge.EVENT_BUS.post(new DimensionRegisterEvent(DIM_MANAGER));
    }
    
    /**
     * Registers the dimension, this needs to be used one-time at world creation. 
     * The dimension will be saved in the level file and loaded when used in the DimensionRegisterEvent.
     * This doesn't create the dimension, please refer to {@link loadOrCreateDimension()}
     * @param dimension
     */
    public void registerDimension(ResourceLocation loc, Dimension dimension) {
        Validate.notNull(loc, "Must provide valid ResourceLocation");
        Validate.notNull(dimension, "Must provide dimension definition");
        
        RegistryKey<Dimension> registryKey = RegistryKey.getOrCreateKey(Registry.DIMENSION_KEY, loc);
        
        if(this.serverWorldInfo.getDimensionGeneratorSettings().func_236224_e_().getValueForKey(registryKey) == null) {
            this.serverWorldInfo.getDimensionGeneratorSettings().func_236224_e_().register(registryKey, dimension, Lifecycle.stable());
        }
    }
    
    /**
     * Loads or creates the specified dimension.
     * @param dimension
     */
    public void loadOrCreateDimension(MinecraftServer server, ResourceLocation loc) {
        Validate.notNull(server, "Must provide server when creating world");
        Validate.notNull(loc, "Must provide valid ResourceLocation");
        
        RegistryKey<Dimension> registryKey = RegistryKey.getOrCreateKey(Registry.DIMENSION_KEY, loc);
        RegistryKey<World> registryKey1 = RegistryKey.getOrCreateKey(Registry.WORLD_KEY, registryKey.getLocation());
        
        Dimension dimension = this.serverWorldInfo.getDimensionGeneratorSettings().func_236224_e_().getValueForKey(registryKey);
        
        DimensionType type = dimension.getDimensionType();
        ChunkGenerator gen = dimension.getChunkGenerator();
        
        server.loadDimension(registryKey1, type, gen);
    }
    
    /**
     * Retrieves the dimension type under the specified {@link ResourceLocation}
     * @param loc the location of the dimension
     * @return the dimension type or null
     */
    @Nullable
    public DimensionType getDimensionType(ResourceLocation loc) {
        Validate.notNull(loc, "Must provide valid ResourceLocation");
        
        RegistryKey<DimensionType> key = RegistryKey.getOrCreateKey(Registry.DIMENSION_TYPE_KEY, loc);
        
        Registry<DimensionType> reg = this.dynamicRegistries.func_230520_a_();
        DimensionType type = reg.getValueForKey(key);
        
        return type;
    }
    
    /**
     * Retrieves the dimension under the specified {@link ResourceLocation}
     * @param loc the location of the dimension
     * @return the dimension
     */
    @Nullable
    public Dimension getDimension(ResourceLocation loc) {
        Validate.notNull(loc, "Must provide valid ResourceLocation");
        
        RegistryKey<Dimension> registryKey = RegistryKey.getOrCreateKey(Registry.DIMENSION_KEY, loc);
        
        Dimension dimension = this.serverWorldInfo.getDimensionGeneratorSettings().func_236224_e_().getValueForKey(registryKey);
        
        return dimension;
    }
    
    /**
     * Retrieves worlds that are registered under the specified dimension type
     * @param server the minecraft server
     * @param loc the location of the world
     * @return the world
     */
    @Nullable
    public List<ServerWorld> getServerWorlds(MinecraftServer server, DimensionType type) {
        Validate.notNull(server, "Must provide server when retrieving world");
        Validate.notNull(type, "Must provide DimensionType");
        
        Registry<DimensionType> reg = this.dynamicRegistries.getRegistry(Registry.DIMENSION_TYPE_KEY);
        
        List<ServerWorld> worlds = new ArrayList<ServerWorld>();
        
        for(ServerWorld world : server.getWorlds()) {
            if(reg.getKey(world.getDimensionType()).equals(reg.getKey(type))) {
                worlds.add(world);
            }
        }
        
        return worlds;
    }
    
    /**
     * Retrieves the world instance of the dimension under the specified {@link ResourceLocation}
     * @param server the minecraft server
     * @param loc the location of the world
     * @return the world
     */
    @Nullable
    public World getServerWorld(MinecraftServer server, ResourceLocation loc) {
        Validate.notNull(server, "Must provide server when creating world");
        Validate.notNull(loc, "Must provide valid ResourceLocation");
        
        RegistryKey<World> registryKey = RegistryKey.getOrCreateKey(Registry.WORLD_KEY, loc);
        
        return server.getWorld(registryKey);
    }
    
    /**
     * Retrieves registered json noise settings from the registry
     * @param loc the resource lcoation of the dimension setting
     * @return
     */
    @Nullable
    public DimensionSettings getDimensionSettings(ResourceLocation loc) {
        Validate.notNull(loc, "Must provide valid ResourceLocation");
        
        RegistryKey<DimensionSettings> registryKey = RegistryKey.getOrCreateKey(Registry.NOISE_SETTINGS_KEY, loc);
        
        DimensionSettings settings = this.dynamicRegistries.getRegistry(Registry.NOISE_SETTINGS_KEY).getValueForKey(registryKey);
        
        return settings;
    }
    
    /**
     * Retrieves the Biome registry for internal use.
     * Modders should use {@link BiomeProviders}
     * @return the biome registry
     */
    @Deprecated
    protected Registry<Biome> getBiomeRegistry() {
        return this.dynamicRegistries.getRegistry(Registry.BIOME_KEY);
    }
}
