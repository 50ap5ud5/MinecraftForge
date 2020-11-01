/*
 * Minecraft Forge
 * Copyright (c) 2016-2020.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.debug.world;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;

import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.concurrent.TickDelayedTask;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeAmbience;
import net.minecraft.world.biome.BiomeGenerationSettings;
import net.minecraft.world.biome.DefaultBiomeFeatures;
import net.minecraft.world.biome.MobSpawnInfo;
import net.minecraft.world.biome.MoodSoundAmbience;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.FlatChunkGenerator;
import net.minecraft.world.gen.FlatGenerationSettings;
import net.minecraft.world.gen.GenerationStage.Decoration;
import net.minecraft.world.gen.Heightmap.Type;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.jigsaw.JigsawManager;
import net.minecraft.world.gen.feature.structure.AbstractVillagePiece;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.gen.feature.structure.VillageConfig;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.gen.settings.DimensionStructuresSettings;
import net.minecraft.world.gen.settings.StructureSeparationSettings;
import net.minecraft.world.gen.surfacebuilders.ConfiguredSurfaceBuilders;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.common.BiomeManager.BiomeEntry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(ChunkSaveTest.MODID)
public class ChunkSaveTest {
  public static final String MODID = "chunk_save_test";
  public static final Logger LOGGER = LogManager.getLogger(MODID);
  
  public static ResourceLocation DIMENSION_LOC = new ResourceLocation(MODID,"test_dim");
  public static ResourceLocation BIOME_LOC = new ResourceLocation(MODID,"test_biome");
  
  public static RegistryKey<World> TEST_WORLD;
  
  public static RegistryKey<Biome> TEST_BIOME;
 
  public ChunkSaveTest() {
      FMLJavaModLoadingContext.get().getModEventBus().register(this);
      Structures.STRUCTURES.register(FMLJavaModLoadingContext.get().getModEventBus());
      Features.FEATURES.register(FMLJavaModLoadingContext.get().getModEventBus());
      Biomes.BIOMES.register(FMLJavaModLoadingContext.get().getModEventBus());
      FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
      IEventBus forgeBus = MinecraftForge.EVENT_BUS;
      forgeBus.register(this);
      forgeBus.addListener(EventPriority.NORMAL, this::addDimensionalSpacing);
      forgeBus.addListener(EventPriority.HIGH, this::biomeModification);
  }
  
  public void setup(final FMLCommonSetupEvent event){
      event.enqueueWork(() -> {
          Structures.setupStructures();
          ConfiguredStructures.registerConfiguredStructures();
          ConfiguredFeatures.registerConfiguredFeatures();
          TEST_WORLD = RegistryKey.getOrCreateKey(Registry.WORLD_KEY, DIMENSION_LOC);
          TEST_BIOME = RegistryKey.getOrCreateKey(Registry.BIOME_KEY, BIOME_LOC);
          BiomeManager.addBiome(BiomeManager.BiomeType.WARM, new BiomeEntry(TEST_BIOME, 1));
      });
  }

  public void biomeModification(final BiomeLoadingEvent event) {
      event.getGeneration().getStructures().add(() -> ConfiguredStructures.CONFIGURED_LOG_CABIN);
      event.getGeneration().getFeatures(Decoration.SURFACE_STRUCTURES).add(() -> ConfiguredFeatures.CONFIGURED_TEST_HOUSE);
  }
  
  public void addDimensionalSpacing(final WorldEvent.Load event) {
      if(event.getWorld() instanceof ServerWorld){
          ServerWorld serverWorld = (ServerWorld)event.getWorld();

          /* Prevent spawning our structure in Vanilla's superflat world as
           * people seem to want their superflat worlds free of modded structures.
           * Also, vanilla superflat is really tricky and buggy to work with. 
           * BiomeModificationEvent does not seem to fire for superflat biomes...you can't add structures to superflat without mixin it seems. 
           * */
          if(serverWorld.getChunkProvider().getChunkGenerator() instanceof FlatChunkGenerator &&
              serverWorld.getDimensionKey().equals(World.OVERWORLD)){
              return;
          }

          Map<Structure<?>, StructureSeparationSettings> tempMap = new HashMap<>(serverWorld.getChunkProvider().generator.func_235957_b_().func_236195_a_());
          tempMap.put(Structures.LOG_CABIN.get(), DimensionStructuresSettings.field_236191_b_.get(Structures.LOG_CABIN.get()));
          serverWorld.getChunkProvider().generator.func_235957_b_().field_236193_d_ = tempMap;
      }
 }
  
  /* Biomes */
  public static class Biomes{
      public static final DeferredRegister<Biome> BIOMES = DeferredRegister.create(ForgeRegistries.BIOMES, ChunkSaveTest.MODID);
      
      public static final RegistryObject<Biome> TEST = BIOMES.register("test_biome", () -> createTestBiome());
      
      public static Biome createTestBiome(){
          MobSpawnInfo.Builder spawns = new MobSpawnInfo.Builder();

          spawns.withSpawner(EntityClassification.CREATURE, new MobSpawnInfo.Spawners(EntityType.RABBIT, 100, 2, 4));
          spawns.withSpawner(EntityClassification.CREATURE, new MobSpawnInfo.Spawners(EntityType.COW, 50, 1, 3));
          spawns.withSpawner(EntityClassification.CREATURE, new MobSpawnInfo.Spawners(EntityType.HORSE, 50, 1, 6));
          
          BiomeGenerationSettings.Builder builder = (new BiomeGenerationSettings.Builder()).withSurfaceBuilder(ConfiguredSurfaceBuilders.field_244178_j);

          DefaultBiomeFeatures.withMonsterRoom(builder);
          DefaultBiomeFeatures.withOverworldOres(builder);
          
          return (new Biome.Builder()).precipitation(Biome.RainType.NONE).category(Biome.Category.PLAINS).depth(0.1F).scale(0.2F).temperature(0.5F).downfall(0.4F).setEffects((new BiomeAmbience.Builder()).setWaterColor(4159204).setWaterFogColor(329011).setFogColor(0xbec4ee).withSkyColor(0xbec4ee).setMoodSound(MoodSoundAmbience.DEFAULT_CAVE).build()).withMobSpawnSettings(spawns.copy()).withGenerationSettings(builder.build()).build();
      }
  }

  
  /* Features */
  public static class Features{
      public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, ChunkSaveTest.MODID);
      
      public static final RegistryObject<TestHouseFeature> TEST_HOUSE = FEATURES.register("test_house", () -> (new TestHouseFeature(NoFeatureConfig.field_236558_a_)));
  }
  
  public static class ConfiguredFeatures {
      public static ConfiguredFeature<?,?> CONFIGURED_TEST_HOUSE = Features.TEST_HOUSE.get().withConfiguration(IFeatureConfig.NO_FEATURE_CONFIG);
      
      
      public static void registerConfiguredFeatures() {
          Registry<ConfiguredFeature<?, ?>> registry = WorldGenRegistries.CONFIGURED_FEATURE;
          Registry.register(registry, new ResourceLocation(ChunkSaveTest.MODID, "configured_test_house"), CONFIGURED_TEST_HOUSE);
      }
  }
  
  public static class TestHouseFeature extends Feature<NoFeatureConfig>{
    
    public static final ResourceLocation HOUSE = new ResourceLocation(MODID, "worldgen/feature/test_house");  
    
    public TestHouseFeature(Codec<NoFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(ISeedReader reader, ChunkGenerator generator, Random rand, BlockPos pos,
            NoFeatureConfig config) {
        BlockPos blockpos = reader.getHeight(Type.WORLD_SURFACE_WG, pos).down();
        TemplateManager tempM = reader.getWorld().getServer().getTemplateManager();
        Template temp = tempM.getTemplate(Features.TEST_HOUSE.get().getRegistryName());
        if (temp != null) {
            reader.getWorld().getServer().enqueue(new TickDelayedTask(1, () -> {
                if (doesStartInChunk(blockpos)) {
                    PlacementSettings settings = new PlacementSettings().setIgnoreEntities(false);
                    temp.func_237144_a_(reader.getWorld(), blockpos, settings, rand); //addBlocksToWorld
                    ChunkSaveTest.LOGGER.log(Level.INFO, "Test House at: " + blockpos.getX() + " " + blockpos.getY() + " " + blockpos.getZ());
                }
            }));
            return true;
        }
        return false;
    }
    
    public boolean doesStartInChunk(BlockPos pos){
        return pos.getX() >> 4 == 0 >> 4 && pos.getZ() >> 4 == 0 >> 4;
    }

      
  }
  
  /* Structures */
  public static class Structures{
      
      public static final DeferredRegister<Structure<?>> STRUCTURES = DeferredRegister.create(ForgeRegistries.STRUCTURE_FEATURES, ChunkSaveTest.MODID);
      
      /** If the registry name is changed or this registry object is removed from a world,
       * https://github.com/MinecraftForge/MinecraftForge/issues/7363 will occur.
       * */
      public static final RegistryObject<Structure<NoFeatureConfig>> LOG_CABIN = setupStructure("log_cabin", () -> (new LogCabin(NoFeatureConfig.field_236558_a_)));
      

      private static <T extends Structure<?>> RegistryObject<T> setupStructure(String name, Supplier<T> structure) {
          return STRUCTURES.register(name, structure);
      }
      
      /** Setup the structure and add the rarity settings. This is set to very high for dev testing purposes.*/
      public static void setupStructures() { 
          setupStructure(LOG_CABIN.get(), new StructureSeparationSettings(10, 5, 1234567890), true); //10 maximum distance apart, 5 minimum distance apart, chunk seed
      }
      
      /** Add Structure to the structure registry map and setup the seperation settings.*/
      public static <F extends Structure<?>> void setupStructure(F structure, StructureSeparationSettings structureSeparationSettings, boolean transformSurroundingLand){
          /*
          * We need to add our structures into the map in Structure alongside vanilla
          * structures or else it will cause errors. Called by registerStructure.
          *
          * If the registration is setup properly for the structure, getRegistryName() should never return null.
          */
          Structure.NAME_STRUCTURE_BIMAP.put(structure.getRegistryName().toString(), structure);
          /*
           * Will add land at the base of the structure like it does for Villages and Outposts.
           * Doesn't work well on structures that have pieces stacked vertically or change in heights.
           */
          if(transformSurroundingLand){ 
              Structure.field_236384_t_ = ImmutableList.<Structure<?>>builder().addAll(Structure.field_236384_t_).add(structure).build();
          }
          /*
           * Adds the structure's spacing into several places so that the structure's spacing remains
           * correct in any dimension or worldtype instead of not spawning.
           *
           * However, it seems it doesn't always work for code made dimensions as they read from
           * this list beforehand. Use the WorldEvent.Load event to add
           * the structure spacing from this list into that dimension.
           */
          DimensionStructuresSettings.field_236191_b_ =
                  ImmutableMap.<Structure<?>, StructureSeparationSettings>builder()
                          .putAll(DimensionStructuresSettings.field_236191_b_)
                          .put(structure, structureSeparationSettings)
                          .build();
      }
      
      
  }
  
  public static class ConfiguredStructures{
      public static StructureFeature<?, ?> CONFIGURED_LOG_CABIN = Structures.LOG_CABIN.get().withConfiguration(IFeatureConfig.NO_FEATURE_CONFIG);
      
      /** Register Configured Structures in Common Setup. There is currently no Forge Registry for configured structures because configure structures are a dynamic registry and can cause issues if it were a Forge registry.
       * See https://github.com/MinecraftForge/MinecraftForge/pull/7455 for a recent discussion on configured structure registration.
       * */
      public static void registerConfiguredStructures() {
          Registry<StructureFeature<?, ?>> registry = WorldGenRegistries.CONFIGURED_STRUCTURE_FEATURE;
          Registry.register(registry, new ResourceLocation(ChunkSaveTest.MODID, "configured_log_cabin"), CONFIGURED_LOG_CABIN);
          
          FlatGenerationSettings.STRUCTURES.put(Structures.LOG_CABIN.get(), CONFIGURED_LOG_CABIN); //We have to add this to flatGeneratorSettings to account for mods that add custom chunk generators or superflat world type
      }
  }
  
  public static class LogCabin extends Structure<NoFeatureConfig>{

    public LogCabin(Codec<NoFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public IStartFactory<NoFeatureConfig> getStartFactory() {
        return LogCabin.Start::new;
    }
    
    @Override
    public Decoration getDecorationStage() {
        return Decoration.SURFACE_STRUCTURES;
    }

    private static final List<MobSpawnInfo.Spawners> STRUCTURE_MONSTERS = ImmutableList.of(
            new MobSpawnInfo.Spawners(EntityType.ILLUSIONER, 100, 4, 9),
            new MobSpawnInfo.Spawners(EntityType.VINDICATOR, 100, 4, 9)
    );
    
    @Override
    public List<MobSpawnInfo.Spawners> getDefaultSpawnList() {
        return STRUCTURE_MONSTERS;
    }

    private static final List<MobSpawnInfo.Spawners> STRUCTURE_CREATURES = ImmutableList.of(
            new MobSpawnInfo.Spawners(EntityType.SHEEP, 30, 10, 15),
            new MobSpawnInfo.Spawners(EntityType.RABBIT, 100, 1, 2)
    );
    @Override
    public List<MobSpawnInfo.Spawners> getDefaultCreatureSpawnList() {
        return STRUCTURE_CREATURES;
    }
    
    public static class Start extends StructureStart<NoFeatureConfig>  {
        public Start(Structure<NoFeatureConfig> structureIn, int chunkX, int chunkZ, MutableBoundingBox mutableBoundingBox, int referenceIn, long seedIn) {
            super(structureIn, chunkX, chunkZ, mutableBoundingBox, referenceIn, seedIn);
        }

        @Override
        public void func_230364_a_(DynamicRegistries dynamicRegistryManager, ChunkGenerator chunkGenerator, TemplateManager templateManagerIn, int chunkX, int chunkZ, Biome biomeIn, NoFeatureConfig config) {
         // Turns the chunk coordinates into actual coordinates we can use. (Gets center of that chunk)
            int x = (chunkX << 4) + 7;
            int z = (chunkZ << 4) + 7;
            BlockPos blockpos = new BlockPos(x, 64, z); //hardcode the y pos for now
            JigsawManager.func_242837_a(dynamicRegistryManager,
                    new VillageConfig(() -> dynamicRegistryManager.getRegistry(Registry.JIGSAW_POOL_KEY)
                            // The path to the starting Template Pool JSON file to read.
                            .getOrDefault(new ResourceLocation(ChunkSaveTest.MODID, "log_cabin/start_pool")),
                            50),
                            AbstractVillagePiece::new,
                            chunkGenerator,
                            templateManagerIn,
                            blockpos, // Position of the structure. Y value is ignored if last parameter is set to true.
                            this.components, // The list that will be populated with the jigsaw pieces after this method.
                            this.rand,
                            true, // Allow intersecting jigsaw pieces. If false, villages cannot generate houses. I recommend to keep this to true.
                            true);
                    //Offset structure up by 1, and shift bounding box down by 1, so that the structure is start from the terrain. The offset ensures the structure is placed one block above the terrain.
                    this.components.forEach(piece -> piece.offset(0, 1, 0));
                    this.components.forEach(piece -> piece.getBoundingBox().minY -= 1);
                    
                    this.recalculateStructureSize();
            ChunkSaveTest.LOGGER.log(Level.INFO, "Log Cabin at " + (blockpos.getX()) + " " + blockpos.getY() + " " + (blockpos.getZ()));
        }

    }
      
  }
}
