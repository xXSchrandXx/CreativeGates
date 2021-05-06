package com.massivecraft.creativegates;

import com.massivecraft.creativegates.entity.MConf;
import com.massivecraft.creativegates.entity.UGate;
import com.massivecraft.creativegates.entity.UGateColl;
import com.massivecraft.massivecore.Engine;
import com.massivecraft.massivecore.mixin.MixinMessage;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.InventoryUtil;
import com.massivecraft.massivecore.util.MUtil;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class EngineMain extends Engine
{
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //
	
	private static EngineMain i = new EngineMain();
	public static EngineMain get() { return i; }
	
	// -------------------------------------------- //
	// IS X NEARBY (UTIL)
	// -------------------------------------------- //
	
	public static boolean isGateNearby(Block block)
	{
		if ( ! MConf.get().isEnabled()) return false;
		
		final int radius = 3;
		for (int dx = -radius; dx <= radius; dx++)
		{
			for (int dy = -radius; dy <= radius; dy++)
			{
				for (int dz = -radius; dz <= radius; dz++)
				{
					if (CreativeGates.get().getIndex().get(PS.valueOf(block.getRelative(dx, dy, dz))) != null) return true;
				}
			}
		}
		return false;
	}
	
	// -------------------------------------------- //
	// STABILIZE PORTAL CONENT
	// -------------------------------------------- //
	
	// PORTAL
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void stabilizePortalContent(BlockPhysicsEvent event)
	{
		// If a portal block is running physics ...
		Block block = event.getBlock();
		if (block.getType() != Material.NETHER_PORTAL) return;
		
		// ... and we are filling or that block is stable according to our algorithm ...
		if ( ! (CreativeGates.get().isFilling() || isPortalBlockStable(block))) return;
		
		// ... then block the physics to stop the portal from disappearing.
		event.setCancelled(true);
	}
	
	public static boolean isPortalBlockStable(Block block)
	{
		if (CreativeGates.isVoid(block.getRelative(+0, +1, +0)) == true) return false;
		if (CreativeGates.isVoid(block.getRelative(+0, -1, +0)) == true) return false;
		
		if (CreativeGates.isVoid(block.getRelative(+1, +0, +0)) == false && CreativeGates.isVoid(block.getRelative(-1, +0, +0)) == false) return true;
		if (CreativeGates.isVoid(block.getRelative(+0, +0, +1)) == false && CreativeGates.isVoid(block.getRelative(+0, +0, -1)) == false) return true;
		
		return false;
	}
	
	// WATER
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void stabilizePortalContent(BlockFromToEvent event)
	{
		if ( ! MConf.get().isUsingWater()) return;
		if (UGate.get(event.getBlock()) == null && UGate.get(event.getToBlock()) == null) return;
		event.setCancelled(true);
	}
	
	public static void stabilizePortalContentBlock(Block block, Cancellable cancellable)
	{
		if (UGate.get(block) == null) return;
		cancellable.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void stabilizePortalContent(BlockPlaceEvent event)
	{
		stabilizePortalContentBlock(event.getBlock(), event);
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void stabilizePortalContent(PlayerBucketFillEvent event)
	{
		stabilizePortalContentBlock(event.getBlockClicked(), event);
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void stabilizePortalContent(PlayerBucketEmptyEvent event)
	{
		stabilizePortalContentBlock(event.getBlockClicked(), event);
	}
	
	// -------------------------------------------- //
	// DISABLE VANILLA PORTAL BEHAVIOR
	// -------------------------------------------- //
	
	public static void disableVanillaGates(Location location, Cancellable cancellable)
	{
		if (isGateNearby(location.getBlock()))
		{
			cancellable.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void disableVanillaGates(PlayerPortalEvent event)
	{
		disableVanillaGates(event.getFrom(), event);
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void disableVanillaGates(EntityPortalEvent event)
	{
		disableVanillaGates(event.getFrom(), event);
	}
	
	// -------------------------------------------- //
	// NO ZOMBIE PIGMAN PORTAL SPAWN
	// -------------------------------------------- //
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void noZombiePigmanPortalSpawn(CreatureSpawnEvent event)
	{
		// If a zombie pigman is spawning ...
		if (event.getEntityType() != EntityType.ZOMBIFIED_PIGLIN) return;
		
		// ... because of a nether portal ...
		if (event.getSpawnReason() != SpawnReason.NETHER_PORTAL) return;
		
		// ... near a gate ...
		Location location = event.getLocation();
		if ( ! isGateNearby(location.getBlock())) return;
		
		// ... and we are blocking zombie pigman portal spawn ...
		if (MConf.get().isPigmanPortalSpawnAllowed()) return;
		
		// ... then block the spawn event.
		event.setCancelled(true);
	}
	
	// -------------------------------------------- //
	// USE GATE
	// -------------------------------------------- //
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void useGate(PlayerMoveEvent event)
	{
		// If a player ...
		Player player = event.getPlayer();
		if (MUtil.isntPlayer(player)) return;
		
		// ... is moving from one block to another ...
		if (MUtil.isSameBlock(event)) return;
		
		// ... and there is a gate in the new block ...
		UGate ugate = UGate.get(event.getTo());
		if (ugate == null) return;
		
		// ... and if the gate is intact ...
		if ( ! ugate.isIntact())
		{
			// We try to detect that a gate was destroyed once it happens by listening to a few events.
			// However there will always be cases we miss and by checking at use we catch those as well.
			// Examples could be map corruption or use of WorldEdit.
			ugate.destroy();
			return;
		}
		
		// ... and gates are enabled here ...
		if ( ! MConf.get().isEnabled()) return;
		
		// ... and we have permission to use gates ...
		if ( ! Perm.USE.has(player, MConf.get().verboseUsePermission)) return;
		
		// ... and the gate has enter enabled ...
		if ( ! ugate.isEnterEnabled())
		{
			String message = Txt.parse("<i>This gate has enter disabled.");
			MixinMessage.get().messageOne(player, message);
			return;
		}
		
		// ... and the player is alive ...
		if (player.isDead()) return;
		
		// ... then transport the player.
		ugate.transport(player);
	}
	
	// -------------------------------------------- //
	// DESTROY GATE
	// -------------------------------------------- //
	
	public static void destroyGate(Block block)
	{
		UGate ugate = UGate.get(block);
		if (ugate == null) return;
		ugate.destroy();
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void destroyGate(BlockBreakEvent event)
	{
		destroyGate(event.getBlock());
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void destroyGate(EntityChangeBlockEvent event)
	{
		destroyGate(event.getBlock());
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void destroyGate(EntityExplodeEvent event)
	{
		for (Block block : event.blockList())
		{
			destroyGate(block);
		}
	}
	
	// This one looks weird since it needs to handle beds as well
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void destroyGate(BlockPistonExtendEvent event)
	{
		Set<Block> blocks = new HashSet<>();
		
		Block piston = event.getBlock();
		Block extension = piston.getRelative(event.getDirection());
		blocks.add(extension);
		
		for (Block block : event.getBlocks())
		{
			blocks.add(block);
			blocks.add(block.getRelative(event.getDirection()));
		}
		
		for (Block block : blocks)
		{
			destroyGate(block);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void destroyGate(BlockPistonRetractEvent event)
	{
		destroyGate(event.getBlock().getRelative(event.getDirection(), 1));
		if (event.isSticky())
		{
			destroyGate(event.getBlock().getRelative(event.getDirection(), 2));
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void destroyGate(BlockFadeEvent event)
	{
		destroyGate(event.getBlock());
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void destroyGate(BlockBurnEvent event)
	{
		destroyGate(event.getBlock());
	}
	
	// -------------------------------------------- //
	// TOOLS
	// -------------------------------------------- //
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void tools(PlayerInteractEvent event)
	{
		// If a player ...
		final Player player = event.getPlayer();
		if (MUtil.isntPlayer(player)) return;
		
		// ... is clicking a block ...
		final Block clickedBlock = event.getClickedBlock();
		if (clickedBlock == null) return;
		
		// ... and gates are enabled here ...
		if ( ! MConf.get().isEnabled()) return;
		
		// ... and the item in hand ...
		final ItemStack currentItem = event.getItem();
		if (currentItem == null) return;
		final Material material = currentItem.getType();
		
		// ... is in any way an interesting material ...
		if
		(
			material != MConf.get().getMaterialInspect()
			&&
			material != MConf.get().getMaterialMode()
			&&
			material != MConf.get().getMaterialSecret()
			&&
			material != MConf.get().getMaterialCreate()
		)
		{
			return;
		}
		
		// ... then find the current gate ...
		final UGate currentGate = UGate.get(clickedBlock);
		
		String message = null;
		
		// ... and if ...
		if (material == MConf.get().getMaterialCreate())
		{
			// ... we are trying to create ...
			
			// ... check permission node ...
			if ( ! Perm.CREATE.has(player, MConf.get().verboseCreatePermission)) return;
			
			// ... check if the place is occupied ...
			if (currentGate != null)
			{
				message = Txt.parse("<b>There is no room for a new gate since there already is one here.");
				MixinMessage.get().messageOne(player, message);
				return;
			}
			
			// ... check if the item is named ...
			ItemMeta currentItemMeta = currentItem.getItemMeta();
			if ( ! currentItemMeta.hasDisplayName())
			{
				message = Txt.parse("<b>You must name the %s before creating a gate with it.", Txt.getMaterialName(material));
				MixinMessage.get().messageOne(player, message);
				return;
			}
			String newNetworkId = ChatColor.stripColor(currentItemMeta.getDisplayName());
			
			// ... perform the flood fill ...
			Block startBlock = clickedBlock.getRelative(event.getBlockFace());
			Entry<GateOrientation, Set<Block>> gateFloodInfo = FloodUtil.getGateFloodInfo(startBlock);
			if (gateFloodInfo == null)
			{
				message = Txt.parse("<b>There is no frame for the gate, or it's to big.", Txt.getMaterialName(material));
				MixinMessage.get().messageOne(player, message);
				return;
			}
			
			GateOrientation gateOrientation = gateFloodInfo.getKey();
			Set<Block> blocks = gateFloodInfo.getValue();
			
			// ... ensure the required blocks are present ...
			Map<Material, Integer> materialCounts = MaterialCountUtil.count(blocks);
			if ( ! MaterialCountUtil.has(materialCounts, MConf.get().getBlocksrequired()))
			{
				message = Txt.parse("<b>The frame must contain %s<b>.", MaterialCountUtil.desc(MConf.get().getBlocksrequired()));
				MixinMessage.get().messageOne(player, message);
				return;
			}
			
			// ... calculate the exit location ...
			PS exit = PS.valueOf(player.getLocation());
			exit = exit.withPitch(0F);
			exit = exit.withYaw(gateOrientation.getExitYaw(exit, PS.valueOf(blocks.iterator().next())));
			
			// ... calculate the coords ...
			Set<PS> coords = new HashSet<>();
			for (Block block : blocks)
			{
				coords.add(PS.valueOf(block).withWorld(null));
			}
			
			// ... create the gate ...
			UGate newGate = UGateColl.get().create();
			newGate.setCreatorId(IdUtil.getId(player));
			newGate.setNetworkId(newNetworkId);
			newGate.setExit(exit);
			newGate.setCoords(coords);
			
			// ... set the air blocks to portal material ...
			newGate.fill();
			
			// ... run fx ...
			newGate.fxKitCreate(player);
			
			// ... fx-inform the player ...
			message = Txt.parse("<g>A \"<h>%s<g>\" gate takes form in front of you.", newNetworkId);
			MixinMessage.get().messageOne(player, message);
			
			// ... item cost ...
			if (MConf.get().isRemovingCreateToolItem())
			{
				// ... remove one item amount...
				
				// (decrease count in hand)
				decreaseOne(event);
				
				// (message)
				message = Txt.parse("<i>The %s disappears.", Txt.getMaterialName(material));
				MixinMessage.get().messageOne(player, message);
			}
			else if (MConf.get().isRemovingCreateToolName())
			{
				// ... just remove the item name ...
				
				// (decrease count in hand)
				decreaseOne(event);
				
				// (add one unnamed)
				ItemStack newItemUnnamed = new ItemStack(currentItem);
				ItemMeta newItemUnnamedMeta = newItemUnnamed.getItemMeta();
				newItemUnnamedMeta.setDisplayName(null);
				newItemUnnamed.setItemMeta(newItemUnnamedMeta);
				newItemUnnamed.setAmount(1);
				player.getInventory().addItem(newItemUnnamed);
				
				// Update soon
				InventoryUtil.updateSoon(player);
				
				// (message)
				message = Txt.parse("<i>The %s seems to have lost it's power.", Txt.getMaterialName(material));
				MixinMessage.get().messageOne(player, message);
			}
		}
		else
		{
			// ... we are trying to do something else that create ...
			
			// ... and there is a gate ...
			if (currentGate == null)
			{
				// ... and there is no gate ...
				if (isGateNearby(clickedBlock))
				{
					// ... but there is portal nearby.
					
					// ... exit with a message.
					message = Txt.parse("<i>You use the %s on the %s but there seem to be no gate.", Txt.getMaterialName(material), Txt.getMaterialName(clickedBlock.getType()));
					MixinMessage.get().messageOne(player, message);
					return;
				}
				else
				{
					// ... and there is no portal nearby ...
					
					// ... exit quietly.
					return;
				}
			}
			
			// ... and we are not using water ...
			if ( ! MConf.get().isUsingWater())
			{
				// ... update the portal orientation
				currentGate.fill();
			}
			
			// ... send use action description ...
			message = Txt.parse("<i>You use the %s on the %s...", Txt.getMaterialName(material), Txt.getMaterialName(clickedBlock.getType()));
			MixinMessage.get().messageOne(player, message);
			
			// ... check restriction ...
			if (currentGate.isRestricted())
			{
				if (currentGate.isCreator(player))
				{
					message = Txt.parse("<i>... the gate is restricted but you are the creator ...");
					MixinMessage.get().messageOne(player, message);
				}
				else
				{
					message = Txt.parse("<b>... the gate is restricted and you are not the creator.");
					MixinMessage.get().messageOne(player, message);
					return;
				}
			}
			
			if (material == MConf.get().getMaterialInspect())
			{
				// ... we are trying to inspect ...
				message = Txt.parse("<i>Some gate inscriptions are revealed:");
				MixinMessage.get().messageOne(player, message);
				
				message = Txt.parse("<k>network: <v>%s", currentGate.getNetworkId());
				MixinMessage.get().messageOne(player, message);
				
				message = Txt.parse("<k>gates: <v>%d", currentGate.getGateChain().size());
				MixinMessage.get().messageOne(player, message);
			}
			else if (material == MConf.get().getMaterialSecret())
			{
				// ... we are trying to change secret state ...
				
				boolean creator = currentGate.isCreator(player);
				if (creator)
				{
					boolean secret = !currentGate.isRestricted();
					currentGate.setRestricted(secret);
					
					message = (secret ? Txt.parse("<h>Only you <i>can read the gate inscriptions now.") : Txt.parse("<h>Anyone <i>can read the gate inscriptions now."));
					MixinMessage.get().messageOne(player, message);
				}
				else
				{
					message = Txt.parse("<i>It seems <h>only the gate creator <i>can change inscription readability.", Txt.getMaterialName(material), Txt.getMaterialName(clickedBlock.getType()));
					MixinMessage.get().messageOne(player, message);
				}
			}
			else if (material == MConf.get().getMaterialMode())
			{
				// ... we are trying to change mode ...
				
				currentGate.toggleMode();
				
				String enter = currentGate.isEnterEnabled() ? Txt.parse("<g>enter enabled") : Txt.parse("<b>enter disabled");
				String exit = currentGate.isExitEnabled() ? Txt.parse("<g>exit enabled") : Txt.parse("<b>exit disabled");
				
				message = Txt.parse("<i>The gate now has %s <i>and %s<i>.", enter, exit);
				MixinMessage.get().messageOne(player, message);
			}
			
		}
		
	}
	
	// Note we cannot use event.getHand() because it is too new
	private static void decreaseOne(PlayerInteractEvent event)
	{
		ItemStack currentItem = event.getItem();
		Player player = event.getPlayer();
		
		ItemStack newItem = new ItemStack(currentItem);
		newItem.setAmount(newItem.getAmount() - 1);
		
		boolean weapon = InventoryUtil.equals(currentItem, InventoryUtil.getWeapon(player));
		boolean shield = InventoryUtil.equals(currentItem, InventoryUtil.getShield(player));
		
		if (weapon) InventoryUtil.setWeapon(player, newItem);
		else if (shield) InventoryUtil.setShield(player, newItem);
		else
		{
			// This is not to be epected, but it is an attempt future-proofing, in case the API changes.
			boolean succes = false;
			for (ListIterator<ItemStack> it = player.getInventory().iterator(); it.hasNext();)
			{
				ItemStack itemStack = it.next();
				
				// If the item stack is equal to the old item stack, then they are probably the same.
				if (!InventoryUtil.equals(currentItem, itemStack)) continue;
				
				// So just set
				it.set(newItem);
				succes = true;
				break;
			}
			if (!succes) throw new RuntimeException();
		}
	}
	
}
