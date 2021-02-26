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

package net.minecraftforge.client.event;

import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.World;

/**
 * Allows modders to adjust the lightmap colours for a specific Dimension
 * <br>
 * While DimensionRenderInfo seems to be the closest solution for this task, its entries are stored in private, immutable map that is specific to a DimensionType
 * <br>
 * There can be many Dimension(s) per DimensionType, so this event allows modders to control the light per dimension
 * <br>
 * This event fired in {@link LightMapTexture#updateLightMap}
 */
public class DimensionLightMapModificationEvent extends net.minecraftforge.eventbus.api.Event
{
    private final World world;
    private final float partialTicks;
    private final float sunBrightness;
    private final float skyLight;
    private final float blockLight;
    private final Vector3f colors;
    
    public DimensionLightMapModificationEvent(World world, float partialTicks, float sunBrightness, float skyLight, float blockLight, Vector3f colors)
    {
        this.world = world;
        this.partialTicks = partialTicks;
        this.sunBrightness = sunBrightness;
        this.skyLight = skyLight;
        this.blockLight = blockLight;
        this.colors = colors;
    }
    
    /**
     * The world which we're modifying the lightmap colors for
     */
    public World getWorld()
    {
        return this.world;     
    }
    
    /**
     * Get the progress between ticks.
     */
    public float getPartialTicks()
    {
        return this.partialTicks;
    }
    
    /**
     * Get the current sun brightness.
     */
    public float getSunBrightness()
    {
        return this.sunBrightness;
    }
    
    /**
     * Get the sky light brightness factor.
     */
    public float getSkyLight()
    {
        return this.skyLight;
    }
    
    /**
     * Get block light brightness factor.
     */
    public float getBlockLight()
    {
        return this.blockLight;
    }
    
    /**
     * The color values that will be used - (r, g, b)
     */
    public Vector3f getColors()
    {
        return this.colors;
    }

}
