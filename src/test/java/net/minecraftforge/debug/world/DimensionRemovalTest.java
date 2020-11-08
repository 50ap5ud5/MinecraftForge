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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(DimensionRemovalTest.MODID)
public class DimensionRemovalTest {
  public static final String MODID = "dimension_removal_test";
  public static final Logger LOGGER = LogManager.getLogger(MODID);
  
  public static ResourceLocation DIMENSION_LOC = new ResourceLocation(MODID,"test_dim");
  
  public static RegistryKey<World> TEST_WORLD;
  
  public static RegistryKey<Biome> TEST_BIOME;
 
  public DimensionRemovalTest() {
     FMLJavaModLoadingContext.get().getModEventBus().register(this);
     FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
  }
  
  public void setup(final FMLCommonSetupEvent event){
      event.enqueueWork(() -> {
          TEST_WORLD = RegistryKey.getOrCreateKey(Registry.WORLD_KEY, DIMENSION_LOC);
      });
  }
}
