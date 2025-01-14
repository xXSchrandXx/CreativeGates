package com.massivecraft.creativegates;

import com.massivecraft.massivecore.collections.MassiveSet;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.MUtil;
import org.bukkit.block.BlockFace;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public enum GateOrientation
{
	// -------------------------------------------- //
	// ENUM
	// -------------------------------------------- //
	
	NS(MUtil.set(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.UP, BlockFace.DOWN)),
	WE(MUtil.set(BlockFace.WEST, BlockFace.EAST, BlockFace.UP, BlockFace.DOWN)),
	
	// END OF LIST
	;
	
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	public final Set<BlockFace> expandFaces;
	
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	GateOrientation(Collection<BlockFace> expandFaces)
	{
		Set<BlockFace> expandFacesTemp = new MassiveSet<>(expandFaces);
		this.expandFaces = Collections.unmodifiableSet(expandFacesTemp);
	}
	
	// -------------------------------------------- //
	// UTIL
	// -------------------------------------------- //
	
	public BlockFace getExitFace(PS exit, PS gate)
	{
		exit = exit.getBlockCoords(true);
		gate = gate.getBlockCoords(true);
		
		int mod;
		if (this == NS)
		{
			mod = exit.getBlockX() - gate.getBlockX();
			if (mod > 0)
			{
				return BlockFace.WEST;
			}
			else
			{
				return BlockFace.EAST;
			}
		}
		else
		{
			mod = exit.getBlockZ() - gate.getBlockZ();
			if (mod > 0)
			{
				return BlockFace.NORTH;
			}
			else
			{
				return BlockFace.SOUTH;
			}
		}
	}
	
	public float getExitYaw(PS exit, PS gate)
	{
		return MUtil.getYaw(this.getExitFace(exit, gate));
	}
	
}
