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
package net.minecraftforge.event.dimension;

import net.minecraftforge.dimension.DynamicDimensionManager;
import net.minecraftforge.eventbus.api.Event;

/**
 * Register your custom dimension definitions when you receive this event.
 * <code>event.getDimensionManager().registerDimension(...)</code>
 * <br>Every dimension registered within this event will be loaded automatically on server start,
 * otherwise you need to load it manually using {@link DynamicDimensionManager#loadOrCreateDimension(net.minecraft.server.MinecraftServer, net.minecraft.util.ResourceLocation)}
 */
public class DimensionRegisterEvent extends Event {
    
    private DynamicDimensionManager dimManager;
    
    public DimensionRegisterEvent(DynamicDimensionManager dimManager) {
        this.dimManager = dimManager;
    }
    
    public DynamicDimensionManager getDimensionManager() {
        return dimManager;
    }

}
