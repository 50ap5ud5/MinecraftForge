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

package net.minecraftforge.debug;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Dimension;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.gen.NoiseChunkGenerator;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.dimension.BiomeProviders;
import net.minecraftforge.dimension.DynamicDimensionManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.dimension.DimensionRegisterEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@SuppressWarnings("unused")
@Mod(DimensionTest.MODID)
@Mod.EventBusSubscriber(bus = Bus.MOD)
public class DimensionTest {
    static final String MODID = "dimension_test";
    private static final Logger LOGGER = LogManager.getLogger();
    
    private boolean REGISTER_ON_EVENT = false;
    
    
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final RegistryObject<Block> TEST_DIM_CREATOR = BLOCKS.register("test_dim_creator", () -> new TestBlock(AbstractBlock.Properties.create(Material.IRON)));
    
    public DimensionTest() {
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onNewRegistries(RegistryEvent.NewRegistry e) {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(eventBus);
    }
    
    public static class TestBlock extends Block{

        public TestBlock(Properties properties) {
            super(properties);
        }

        @Override
        public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player,
                Hand handIn, BlockRayTraceResult hit) {
            if (!worldIn.isRemote) {
                long seed = 0;
                ResourceLocation test_dim_loc = new ResourceLocation(MODID,"test_dim");
                NoiseChunkGenerator chunk_gen = new NoiseChunkGenerator(BiomeProviders.createOverworldBiomeProvider(seed, false, false), seed, () -> {
                    return DynamicDimensionManager.getDimensionManager().getDimensionSettings(test_dim_loc);
                 });
                DimensionType test_dim_type = DynamicDimensionManager.getDimensionManager().getDimensionType(new ResourceLocation("minecraft", "overworld")); //This must be a local variable as dim types will only be created after player logs in
                Dimension test_dim = new Dimension(() -> test_dim_type, chunk_gen);
                RegistryKey<World> TEST_DIM_WORLD = RegistryKey.func_240903_a_(Registry.field_239699_ae_, test_dim_loc);
                RegistryKey<Dimension> TEST_DIM_DIMENSION = RegistryKey.func_240903_a_(Registry.field_239700_af_, test_dim_loc);
                DynamicDimensionManager.getDimensionManager().registerDimension(test_dim_loc, test_dim);
                DynamicDimensionManager.getDimensionManager().loadOrCreateDimension(worldIn.getServer(), test_dim_loc);
                LOGGER.info("Registered new Dimension of {}", test_dim_loc);
            }
            return ActionResultType.PASS;
        }
        
        
        
    }
    
    @SubscribeEvent
    public void onDimensionRegister(DimensionRegisterEvent e) {
        if(REGISTER_ON_EVENT) {
            System.out.println("Test");
            
            long seed = 0;
            
            //Registers the dimension if it not exists already in the registry.
            e.getDimensionManager().registerDimension(new ResourceLocation("dimension_test", "testworld"), 
                    new Dimension(() -> e.getDimensionManager().getDimensionType(new ResourceLocation("minecraft", "overworld")), 
                            new NoiseChunkGenerator(BiomeProviders.createOverworldBiomeProvider(seed, false, false), seed, () -> {
                                 return e.getDimensionManager().getDimensionSettings(new ResourceLocation("dimension_test", "testworld"));
                              })));
        }
    }
    
    @SubscribeEvent
    public void onServerStart(FMLServerStartingEvent e) {
        if(!REGISTER_ON_EVENT) {
            long seed = 0;
            
            DynamicDimensionManager.getDimensionManager().registerDimension(new ResourceLocation("dimension_test", "testworld"), 
                    new Dimension(() -> DynamicDimensionManager.getDimensionManager().getDimensionType(new ResourceLocation("minecraft", "overworld")), 
                            new NoiseChunkGenerator(BiomeProviders.createOverworldBiomeProvider(seed, false, false), seed, () -> {
                                 return DynamicDimensionManager.getDimensionManager().getDimensionSettings(new ResourceLocation("dimension_test", "testworld"));
                              })));
            
            DynamicDimensionManager.getDimensionManager().loadOrCreateDimension(e.getServer(), new ResourceLocation("dimension_test", "testworld"));
        }
        
        LOGGER.info("Dimension: " + DynamicDimensionManager.getDimensionManager().getServerWorld(e.getServer(), new ResourceLocation("dimension_test", "testworld")));
    }
    
}
