package com.massivecraft.creativegates.cmd;

import com.massivecraft.creativegates.Perm;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.type.primitive.TypeString;

import java.util.List;

public class CmdCgWorldDelete extends MassiveCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdCgWorldDelete()
	{
		// Parameters
		this.addParameter(TypeString.get(), "world", true);
		
		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.CG_WORLD_DELETE));
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<String> getAliases()
	{
		return MConf.get().aliasesCgWorldDelete;
	}
	
	@Override
	public void perform() throws MassiveException
	{
		// Parameters
		String world = this.readArg();
		
		// Apply
		int countDeleted = 0;
		for (UGate gate : UGateColl.get().getAll())
		{
			if (world.equals(gate.getExit().getWorld()))
			{
				gate.destroy();
				countDeleted++;
			}
		}
		
		// Inform
		msg("<i>Deleted all <h>%d <i>gates in world <h>%s<i>.", countDeleted, world);
	}
	
}
