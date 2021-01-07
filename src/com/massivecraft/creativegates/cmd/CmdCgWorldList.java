package com.massivecraft.creativegates.cmd;

import com.massivecraft.creativegates.Perm;
import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.mixin.MixinWorld;
import com.massivecraft.massivecore.util.MUtil;
import com.massivecraft.massivecore.util.Txt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CmdCgWorldList extends MassiveCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdCgWorldList()
	{
		// Parameters
		this.addParameter(Parameter.getPage());
		
		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.CG_WORLD_LIST));
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public List<String> getAliases()
	{
		return MConf.get().aliasesCgWorldList;
	}
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		int page = this.readArg();
		
		// Create Lines
		List<String> lines = new ArrayList<>();
		
		// count the gates
		Map<String, Integer> world2count = new HashMap<>();
		int countAll = 0;
		for (UGate gate : UGateColl.get().getAll())
		{
			String world = gate.getExit().getWorld();
			Integer count = world2count.get(world);
			if (count == null) count = 0;
			count++;
			countAll++;
			world2count.put(world, count);
		}

		
		// convert to lines
		for (Entry<String, Integer> entry : MUtil.entriesSortedByValues(world2count, false))
		{
			String world = entry.getKey();
			int count = entry.getValue();
			
			if (MixinWorld.get().getWorldIds().contains(world))
			{
				lines.add(Txt.parse("<v>%d <g>%s", count, world));
			}
			else
			{
				lines.add(Txt.parse("<v>%d <b>%s", count, world));
			}
		}
		lines.add(Txt.parse("<v>%d <k>%s", countAll, "SUM"));
		
		// Send Lines
		this.message(Txt.getPage(lines, page, "Gates per World", this));	
	}
	
}
