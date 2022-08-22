package main;

import java.util.Date;
import net.risingworld.api.Plugin;
import net.risingworld.api.World;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.PlayerChangePositionEvent;
import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerDropItemEvent;
import net.risingworld.api.gui.GuiLabel;
import net.risingworld.api.objects.Inventory;
import net.risingworld.api.objects.Item;
import net.risingworld.api.objects.Player;
import net.risingworld.api.objects.WorldItem;
import net.risingworld.api.utils.SoundInformation;

public class AutoPickup extends Plugin implements Listener {
	private World world;
	private long lastDrop;
	private long dropCooldown;
	public SoundInformation pickupSound;
	
	

	public void onDisable() {
		// TODO Auto-generated method stub
		
	}

	public void onEnable() {
        world = getWorld();
        
        pickupSound = new SoundInformation(getPath() + "/Sounds/pickup.ogg");

        registerEventListener(this);
        
        Date date = new Date();
		long timeMilli = date.getTime();
		
		lastDrop = timeMilli;
       
		dropCooldown = 2000;
		
		System.out.println("Autopickup enabled!");		
	}
	
	@EventMethod
    public void onPlayerConnect(final PlayerConnectEvent event) {

		
	}
	
	@EventMethod
	public void onPlayerDropItem(final PlayerDropItemEvent event) {
		Date date = new Date();
		lastDrop = date.getTime();
	}
	
	@EventMethod
	public void onPlayerChangePosition(final PlayerChangePositionEvent event) {
		Date date = new Date();
		final GuiLabel debugLabel4 = (GuiLabel) event.getPlayer().getAttribute("Debug-Label4");
		final GuiLabel debugLabel5 = (GuiLabel) event.getPlayer().getAttribute("Debug-Label5");
		
		
		if( (date.getTime() - lastDrop) > dropCooldown) {
			WorldItem worldItem = world.findNearestItem(event.getPlayer().getPosition());
			
			String itemType = worldItem.getDefinition().getType();
			
			float distanceToItem = worldItem.getPosition().distance(event.getPlayer().getPosition());	
			
			boolean canAddItem = false;
			
			if(!worldItem.isDummy()) {
				canAddItem = inventoryCheck(event.getPlayer(), worldItem);
			} 
			
			debugLabel4.setText("Can Add Nearest Item: " + canAddItem);
			
			if(worldItem != null) {
				debugLabel5.setText(
					"Nearest Item: " + worldItem.getName() + 
					", Type: " + itemType + 
					", Status: " + worldItem.getStatus() +
					", Value: " + worldItem.getValue() +
					", Attribute: " + worldItem.getAttribute() +
					", IsDummy: " + worldItem.isDummy() +
					", Variation: " + worldItem.getVariation()
				);
			}
			
			if(distanceToItem < 3 && canAddItem && !worldItem.isDummy()) {
				if(itemType == "BLOCK" || itemType == "PLANT" || itemType == "OBJECT" || itemType == "DEFAULT"  || itemType == "ORE") { // temporary if block
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
	
	private boolean inventoryCheck(Player player, WorldItem checkItem) {
		String inventoryList = "-empty-";
		
		int inventorySlotCount = player.getInventory().getSlotCount(Inventory.SlotType.Inventory);
		int quickSlotCount = player.getInventory().getSlotCount(Inventory.SlotType.Quickslot);
		
		boolean canAdd = false;
		
		//final GuiLabel inventoryOutput = (GuiLabel) player.getAttribute("inventoryOutput");
		
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
							if(inventoryList == "-empty-") {
								inventoryList = "Slot " + i + " - Item: " + inventoryItem.getName() + ", Stack Size: " + inventoryItem.getStacksize() +", Max Stack Size: " + inventoryItem.getMaxStacksize() +
										", checkItemId: " + checkItemId + ", checkItemVariation: " + checkItemVariation +
										", inventItemId: " + inventItemId + ", inventItemVariation: " + inventItemVariation;
							} else {
								inventoryList += "\nSlot " + i + " - Item: " + inventoryItem.getName() + ", Stack Size: " + inventoryItem.getStacksize() +", Max Stack Size: " + inventoryItem.getMaxStacksize() +
									", checkItemId: " + checkItemId + ", checkItemVariation: " + checkItemVariation +
									", inventItemId: " + inventItemId + ", inventItemVariation: " + inventItemVariation;
							}					
							canAdd = true;
						}
					} 
				} 
			} else {
				if(inventoryList == "-empty-") {
					inventoryList = "Slot " + i + " - [empty]" +
							", checkItemId: " + checkItemId + ", checkItemVariation: " + checkItemVariation;
				} else {
					inventoryList += "\nSlot " + i + " - [empty]" +
							", checkItemId: " + checkItemId + ", checkItemVariation: " + checkItemVariation;
				}
				canAdd = true;
			}
		}
		
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
							if(inventoryList == "-empty-") {
								inventoryList = "QuickSlot " + i + " - Item: " + quickSlotItem.getName() + ", Stack Size: " + quickSlotItem.getStacksize() +", Max Stack Size: " + quickSlotItem.getMaxStacksize() +
										", checkItemId: " + checkItemId + ", checkItemVariation: " + checkItemVariation +
										", inventItemId: " + inventItemId + ", inventItemVariation: " + inventItemVariation;
							} else {
								inventoryList += "\nQuickSlot " + i + " - Item: " + quickSlotItem.getName() + ", Stack Size: " + quickSlotItem.getStacksize() +", Max Stack Size: " + quickSlotItem.getMaxStacksize() +
									", checkItemId: " + checkItemId + ", checkItemVariation: " + checkItemVariation +
									", inventItemId: " + inventItemId + ", inventItemVariation: " + inventItemVariation;
							}					
							canAdd = true;
						}
					} 
				} 
			} else {
				if(inventoryList == "-empty-") {
					inventoryList = "QuickSlot " + i + " - [empty]" +
							", checkItemId: " + checkItemId + ", checkItemVariation: " + checkItemVariation;
				} else {
					inventoryList += "\nQuickSlot " + i + " - [empty]" +
							", checkItemId: " + checkItemId + ", checkItemVariation: " + checkItemVariation;
				}
				canAdd = true;
			}
		}
		
		// special cases list
		switch(checkItem.getName()) {
			case "m14":
				canAdd = false;
				break;
			case "fireworkrocket":
				canAdd = false;
				break;
			default:
			
		}

		
		//inventoryOutput.setText(inventoryList);
		return canAdd;
	}
	
	

}
