package main;

import java.util.Date;
import java.util.HashMap;

import net.risingworld.api.Plugin;
import net.risingworld.api.World;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.PlayerChangePositionEvent;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerDisconnectEvent;
import net.risingworld.api.events.player.PlayerDropItemEvent;
import net.risingworld.api.objects.Inventory;
import net.risingworld.api.objects.Item;
import net.risingworld.api.objects.Player;
import net.risingworld.api.objects.WorldItem;
import net.risingworld.api.utils.SoundInformation;

public class AutoPickup extends Plugin implements Listener {
	private World world;
	private long dropCooldown;	
	private HashMap<Player, Long> lastDropTimes;	
	public SoundInformation pickupSound;
	
	private HashMap<Player, Boolean> disabled;	
	
	public void onDisable() { }

	public void onEnable() {
        world = getWorld();
        pickupSound = new SoundInformation(getPath() + "/sounds/pickup.ogg");
        
        lastDropTimes = new HashMap<Player, Long>();
        disabled = new HashMap<Player, Boolean>();
        
        registerEventListener(this);
        
		dropCooldown = 2000;
		
		System.out.println("Autopickup enabled!");		
	}
	
	@EventMethod
    public void onPlayerConnect(final PlayerConnectEvent event) {
		Date date = new Date();
		lastDropTimes.put(event.getPlayer(), date.getTime());
		disabled.put(event.getPlayer(), false);
	}
	
	@EventMethod
    public void onPlayerDisconnect(final PlayerDisconnectEvent event) {
		lastDropTimes.remove(event.getPlayer());
		disabled.remove(event.getPlayer());
	}
	
	@EventMethod
	public void onPlayerDropItem(final PlayerDropItemEvent event) {
		Date date = new Date();
		lastDropTimes.put(event.getPlayer(),date.getTime());
	}
	
	@EventMethod
    public void onPlayerCommand(final PlayerCommandEvent event) {
		Player cmdPlayer = event.getPlayer();	
		String[] cmdParams = event.getCommand().split(" ");
		
		switch(cmdParams.length) {
			case 1:
				
				break;
			case 2:
				if(cmdParams[0].equals("/ap") && cmdParams[1].equals("disable")) {
					disabled.put(cmdPlayer, true);
					cmdPlayer.sendTextMessage("AutoPickup Disabled.");
				}
				
				if(cmdParams[0].equals("/ap") && cmdParams[1].equals("enable")) {
					disabled.put(cmdPlayer, false);
					cmdPlayer.sendTextMessage("AutoPickup Enabled.");
				}
				break;
			default:
			
		}
	}
	
	@EventMethod
	public void onPlayerChangePosition(final PlayerChangePositionEvent event) {
		if(!disabled.get(event.getPlayer())) {
			Date date = new Date();
			
			if( (date.getTime() - lastDropTimes.get(event.getPlayer())) > dropCooldown) {
				WorldItem worldItem = world.findNearestItem(event.getPlayer().getPosition());			
				String itemType = worldItem.getDefinition().getType();			
				float distanceToItem = worldItem.getPosition().distance(event.getPlayer().getPosition());				
				boolean canAddItem = false;
				
				if(!worldItem.isDummy()) {
					canAddItem = inventoryCheck(event.getPlayer(), worldItem);
				}			
				
				if(distanceToItem < 3 && canAddItem && !worldItem.isDummy()) {
					if(itemType == "BLOCK" || itemType == "PLANT" || itemType == "OBJECT" || itemType == "DEFAULT"  || itemType == "ORE") { // temporary 
						worldItem.applyPhysicalImpulse(event.getPlayer().getPosition().subtract(worldItem.getPosition()).mult(2f));
						
						if(distanceToItem < 2) {
							Item.Attribute attribute = worldItem.getAttribute();
							Item.ObjectAttribute objAtt = null;
							
							switch(itemType) {						
								case "OBJECT":
									objAtt = (Item.ObjectAttribute) attribute;	
									if(objAtt != null) {
										event.getPlayer().getInventory().insertNewObjectItem((short) objAtt.getObjectID(), worldItem.getVariation(),worldItem.getStacksize());
									} else {
										event.getPlayer().getInventory().insertNewItem(worldItem.getDefinition().getID(), worldItem.getVariation(),worldItem.getStacksize());
									}
	
									break;
								default:
									event.getPlayer().getInventory().insertNewItem(worldItem.getDefinition().getID(), worldItem.getVariation(),worldItem.getStacksize());
							}
							
							worldItem.destroy();
							event.getPlayer().playSound(pickupSound, event.getPlayer().getPosition());
						}
					}
				}
			} 
		}
	}
	
	private boolean inventoryCheck(Player player, WorldItem checkItem) {
		int inventorySlotCount = player.getInventory().getSlotCount(Inventory.SlotType.Inventory);
		int quickSlotCount = player.getInventory().getSlotCount(Inventory.SlotType.Quickslot);
		
		boolean canAdd = false;
		
		int checkItemId;
		int checkItemVariation;
		
		int inventItemId;
		int inventItemVariation;
		
		String checkItemType = checkItem.getDefinition().getType();
		
		Item.Attribute checkItemAttribute = checkItem.getAttribute();
		Item.ObjectAttribute checkItemObjAtt = null;
		
		switch(checkItemType) {
			case "OBJECT":
				checkItemObjAtt = (Item.ObjectAttribute) checkItemAttribute;
				if(checkItemObjAtt == null) {
					checkItemId = checkItem.getDefinition().getID();
				} else {
					checkItemId = checkItemObjAtt.getObjectID();
				}
				checkItemVariation = checkItem.getVariation(); 
				break;
			default:
				checkItemId = checkItem.getDefinition().getID();
				checkItemVariation = checkItem.getVariation();
		}
		
		//special case
		if(checkItemId == 700 && checkItemVariation == 0) {
			checkItemVariation = 21;
		}

		Item.Attribute attribute = checkItem.getAttribute();
		Item.ObjectAttribute objAtt = null;

		// check inventory
		for(int i = 0; i < inventorySlotCount; i++) {
			Item inventoryItem = player.getInventory().getItem(i, Inventory.SlotType.Inventory);
			
			
			if(inventoryItem != null) {
				String inventoryItemType = inventoryItem.getDefinition().getType();
				if(checkItemType == inventoryItemType) {
					switch(checkItemType) {
						case "OBJECT":
							objAtt = (Item.ObjectAttribute) attribute;
							
							if(objAtt == null) {
								inventItemId = inventoryItem.getDefinition().getID();
							} else {
								inventItemId = objAtt.getObjectID();
							}
							inventItemVariation = inventoryItem.getVariation();
							break;
						default:
							inventItemId = inventoryItem.getDefinition().getID();
							inventItemVariation = inventoryItem.getVariation();
					}
					
					if(inventItemId == checkItemId && inventItemVariation == checkItemVariation) {
						int stackSize = inventoryItem.getStacksize();
						int itemMaxStackSize = inventoryItem.getMaxStacksize();
						
						if(stackSize < itemMaxStackSize) {					
							canAdd = true;
						}
					} 
				} 
			} else {
				canAdd = true;
			}
		}
		
		// check quickslots
		for(int i = 0; i < quickSlotCount; i++) {
			Item quickSlotItem = player.getInventory().getItem(i, Inventory.SlotType.Quickslot);
			
			
			if(quickSlotItem != null) {	
				String quickSlotItemType = quickSlotItem.getDefinition().getType();
				if(checkItemType == quickSlotItemType) {		
					switch(checkItemType) {						
						case "OBJECT":
							objAtt = (Item.ObjectAttribute) attribute;	
							
							if(objAtt == null) {
								inventItemId = quickSlotItem.getDefinition().getID();
							} else {
								inventItemId = objAtt.getObjectID();
							}
							
							inventItemVariation = quickSlotItem.getVariation();
							break;
						default:
							inventItemId = quickSlotItem.getDefinition().getID();
							inventItemVariation = quickSlotItem.getVariation();
					}
					
					if(inventItemId == checkItemId && inventItemVariation == checkItemVariation) {
						int stackSize = quickSlotItem.getStacksize();
						int itemMaxStackSize = quickSlotItem.getMaxStacksize();
						
						if(stackSize < itemMaxStackSize) {
							canAdd = true;
						}
					} 
				} 
			} else {
				canAdd = true;
			}
		}
		
		// special cases list - will probably grow
		switch(checkItem.getName()) {
			case "m14":
				canAdd = false;
				break;
			case "fireworkrocket":
				canAdd = false;
				break;
			default:
			
		}
		
		return canAdd;
	}
}
