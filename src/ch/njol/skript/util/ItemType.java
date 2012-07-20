/*
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 * Copyright 2011, 2012 Peter Güttinger
 * 
 */

package ch.njol.skript.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import ch.njol.skript.Skript;
import ch.njol.skript.util.Container.ContainerType;
import ch.njol.util.iterator.SingleItemIterator;

@ContainerType(ItemStack.class)
public class ItemType implements Cloneable, Iterable<ItemData>, Container<ItemStack> {
	
	/**
	 * Note to self: use {@link #add(ItemData)} to add item datas, don't add them directly to this list.
	 */
	private final ArrayList<ItemData> types = new ArrayList<ItemData>();
	
	private boolean all = false;
	
	private int amount = -1;
	
	private boolean hasPreferred = false;
	
	/**
	 * list of pairs {item/block, block... to replace if possible}
	 */
	private static final int[][] preferredMaterials = {
			{Material.STATIONARY_WATER.getId(), Material.WATER.getId()},
			{Material.STATIONARY_LAVA.getId(), Material.LAVA.getId()},
			{Material.BED.getId(), Material.BED_BLOCK.getId()},
			{Material.BREWING_STAND_ITEM.getId(), Material.BREWING_STAND.getId()},
			{Material.REDSTONE_LAMP_OFF.getId(), Material.REDSTONE_LAMP_ON.getId()},
			{Material.REDSTONE_TORCH_ON.getId(), Material.REDSTONE_TORCH_OFF.getId()},
			{Material.REDSTONE_ORE.getId(), Material.GLOWING_REDSTONE_ORE.getId()},
			{Material.FURNACE.getId(), Material.BURNING_FURNACE.getId()},
			{Material.CAULDRON_ITEM.getId(), Material.CAULDRON.getId()},
			{Material.SEEDS.getId(), Material.CROPS.getId()},
			{Material.DIODE.getId(), Material.DIODE_BLOCK_OFF.getId(), Material.DIODE_BLOCK_ON.getId()},
			{Material.DIODE_BLOCK_OFF.getId(), Material.DIODE_BLOCK_ON.getId()},
			{Material.SUGAR_CANE.getId(), Material.SUGAR_CANE_BLOCK.getId()},
			{Material.SIGN.getId(), Material.SIGN_POST.getId(), Material.WALL_SIGN.getId()},
			{Material.CAKE.getId(), Material.CAKE_BLOCK.getId()}
	};
	
	public ItemType() {}
	
	public ItemType(final ItemStack i) {
		amount = i.getAmount();
		add(new ItemData(i));
	}
	
	public ItemType(final Block block) {
		add(new ItemData(block.getTypeId(), block.getData()));
	}
	
	public ItemType(final ItemType i) {
		all = i.all;
		amount = i.amount;
		hasPreferred = i.hasPreferred;
		for (final ItemData d : i)
			add(d.clone());
	}
	
	public void modified() {
		item = block = null;
		checkHasPreferred();
	}
	
	/**
	 * @return amount or 1 if amount == -1
	 */
	public int getAmount() {
		return amount == -1 ? 1 : amount;
	}
	
	/**
	 * Only use this method if you know what you're doing
	 * 
	 * @return
	 */
	public int getInternalAmount() {
		return amount;
	}
	
	public void setAmount(final int amount) {
		this.amount = amount;
	}
	
	public boolean isAll() {
		return all;
	}
	
	public void setAll(final boolean all) {
		this.all = all;
	}
	
	public boolean isOfType(final ItemStack item) {
		if (item == null)
			return isOfType(0, (short) 0);
		return isOfType(item.getTypeId(), item.getDurability());
	}
	
	public boolean isOfType(final Block block) {
		if (block == null)
			return false;
		return isOfType(block.getTypeId(), block.getData());
	}
	
	public boolean isOfType(final int id, final short data) {
		for (final ItemData type : types) {
			if (type.isOfType(id, data))
				return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return toString(false);
	}
	
	private String toString(final boolean debug) {
		final StringBuilder b = new StringBuilder();
		if (types.size() == 1 && !types.get(0).hasDataRange()) {
			if (getAmount() != 1)
				b.append(amount + " ");
			if (isAll())
				b.append(getAmount() == 1 ? "every " : "of every ");
		} else {
			if (getAmount() != 1)
				b.append(amount + " of ");
			b.append(isAll() ? "every " : "any ");
		}
		for (int i = 0; i < types.size(); i++) {
			if (i != 0) {// this belongs here as size-1 can be 0
				if (i == types.size() - 1)
					b.append(isAll() ? " and " : " or ");
				else
					b.append(", ");
			}
			final String p = types.get(i).toString(debug);
			b.append(amount > 1 ? Utils.toPlural(p) : p);
		}
		return b.toString();
	}
	
	public static String toString(final ItemStack i) {
		return new ItemType(i).toString();
	}
	
	public String getDebugMessage() {
		return toString(true);
	}
	
	private ItemType item = null, block = null;
	
	public ItemType getItem() {
		if (item != null)
			return item;
		if (!hasPreferred || all)
			return this;
		item = clone();
		for (int i = 0; i < item.types.size(); i++) {
			final ItemData d = item.types.get(i);
			for (final int[] p : preferredMaterials) {
				if (Utils.indexOf(p, d.typeid, 1) != -1) {
					final ItemData c = d.clone();
					c.typeid = p[0];
					if (item.types.contains(c))
						item.types.remove(i--);
					break;
				}
			}
		}
		item.hasPreferred = false;
		return item;
	}
	
	public ItemType getBlock() {
		if (block != null)
			return block;
		if (!hasPreferred || all)
			return this;
		block = clone();
		for (int i = 0; i < block.types.size(); i++) {
			final ItemData d = block.types.get(i);
			for (final int[] p : preferredMaterials) {
				if (p[0] <= Skript.MAXBLOCKID && Utils.indexOf(p, d.typeid, 1) != -1) {
					final ItemData c = d.clone();
					c.typeid = p[0];
					if (block.types.contains(c))
						block.types.remove(i--);
					break;
				} else if (d.typeid == p[0]) {
					final ItemData c = d.clone();
					c.typeid = p[1];
					if (block.types.contains(c))
						block.types.remove(i--);
					break;
				}
			}
		}
		block.hasPreferred = false;
		return block;
	}
	
	private void checkHasPreferred() {
		int c;
		for (final int[] p : preferredMaterials) {
			c = -1;
			for (final ItemData d : types) {
				if (c != -1 && Utils.indexOf(p, d.typeid) != -1) {
					hasPreferred = true;
					return;
				} else if (c == -1) {
					c = Utils.indexOf(p, d.typeid);
				}
			}
		}
		hasPreferred = false;
		return;
	}
	
	/**
	 * Sets the given block to this itemType
	 * 
	 * @param block The block to set
	 * @param applyPhysics Whether to run a physics check just after setting the block
	 * @return Whether the block was successfully set, i.e. whether this ItemType contains an ItemData which represents a block
	 */
	public boolean setBlock(final Block block, final boolean applyPhysics) {
		for (final ItemStack i : getBlock().getAll()) {
			if (i.getTypeId() > Skript.MAXBLOCKID)
				continue;
			block.setTypeIdAndData(i.getTypeId(), (byte) (i.getData() == null ? 0 : i.getDurability()), applyPhysics);
			return true;
		}
		return false;
	}
	
	/**
	 * Intersects all ItemDatas with all ItemDatas of the given ItemType, returning an ItemType with at most n*m ItemDatas, where n = #ItemDatas of this ItemType, and m =
	 * #ItemDatas of the
	 * argument. <br/>
	 * more info: {@link ItemData#intersection(ItemData)}
	 * 
	 * @param other
	 * @return a new item type which is the intersection of the two item types or null if the intersection is empty.
	 */
	public ItemType intersection(final ItemType other) {
		final ItemType r = new ItemType();
		for (final ItemData d1 : types) {
			for (final ItemData d2 : other.types) {
				r.add(d1.intersection(d2));
			}
		}
		if (r.types.isEmpty())
			return null;
		return r;
	}
	
	public void add(final ItemData type) {
		if (type != null) {
			type.setParent(this);
			types.add(type);
			modified();
		}
	}
	
	public void addAll(final Collection<ItemData> types) {
		for (final ItemData type : types) {
			if (type != null) {
				type.setParent(this);
				this.types.add(type);
			}
		}
		modified();
	}
	
	public void remove(final ItemData d) {
		types.remove(d);
		modified();
	}
	
	@Override
	public Iterator<ItemStack> containerIterator() {
		return getAll().iterator();
	}
	
	/**
	 * Gets all ItemStacks this ItemType represents. Only use this if you know what you're doing, as it returns only one element if this is not an 'every' alias.
	 * 
	 * @return
	 */
	public Iterable<ItemStack> getAll() {
		if (!isAll()) {
			final ItemStack i = getRandom();
			return new Iterable<ItemStack>() {
				@Override
				public Iterator<ItemStack> iterator() {
					return new SingleItemIterator<ItemStack>(i);
				}
			};
		}
		return new Iterable<ItemStack>() {
			@Override
			public Iterator<ItemStack> iterator() {
				return new Iterator<ItemStack>() {
					
					Iterator<ItemData> iter = types.iterator();
					Iterator<ItemStack> currentDataIter;
					
					@Override
					public boolean hasNext() {
						while (iter.hasNext() && (currentDataIter == null || !currentDataIter.hasNext())) {
							currentDataIter = iter.next().getAll();
						}
						return iter.hasNext() || currentDataIter.hasNext();
					}
					
					@Override
					public ItemStack next() {
						if (!hasNext())
							throw new NoSuchElementException();
						return currentDataIter.next();
					}
					
					@Override
					public void remove() {}
					
				};
			}
		};
	}
	
	/**
	 * Removes this type from the item stack if applicable
	 * 
	 * @param item
	 * @return The passed ItemStack
	 */
	public ItemStack removeFrom(final ItemStack item) {
		if (item == null)
			return null;
		for (final ItemData type : types) {
			if (type.isOfType(item)) {
				item.setAmount(Math.max(item.getAmount() - getAmount(), 0));
				return item;
			}
		}
		return item;
	}
	
	/**
	 * Adds this Itemtype to the given item stack
	 * 
	 * @param item
	 * @return The passed ItemStack or a new one if the passed is null or air
	 */
	public ItemStack addTo(final ItemStack item) {
		if (item == null || item.getTypeId() == 0)
			return getRandom();
		for (final ItemData type : types) {
			if (type.isOfType(item)) {
				item.setAmount(Math.max(item.getAmount() + getAmount(), 0));
				return item;
			}
		}
		return item;
	}
	
	@Override
	public ItemType clone() {
		return new ItemType(this);
	}
	
	/**
	 * @return One random ItemStack. If you have a List or an Inventory, use {@link #addTo(Inventory)} or {@link #addTo(List)} respectively.
	 * @see #addTo(Inventory)
	 * @see #addTo(ItemStack)
	 * @see #addTo(ItemStack[])
	 * @see #addTo(List)
	 * @see #removeFrom(Inventory)
	 * @see #removeFrom(ItemStack)
	 * @see #removeFrom(List...)
	 */
	public ItemStack getRandom() {
		return Utils.getRandom(types).getRandom();
	}
	
	/**
	 * Test whether this ItemType has space on the given inventory.<br>
	 * TODO If this ItemType represents multiple items with OR, this function will immediately return false.<br/>
	 * CondCanHold currently blocks aliases without 'every'/'all' as temporary solution
	 * 
	 * @param invi
	 * @return
	 */
	public boolean hasSpace(final Inventory invi) {
		if (!isAll()) {
			if (getItem().types.size() != 1 || getItem().types.get(0).hasDataRange() || getItem().types.get(0).typeid == -1)
				return false;
		}
		return addTo(getCopiedContents(invi));
	}
	
	public final static ItemStack[] getCopiedContents(final Inventory invi) {
		final ItemStack[] buf = invi.getContents();
		for (int i = 0; i < buf.length; i++)
			if (buf[i] != null)
				buf[i] = buf[i].clone();
		return buf;
	}
	
	private final List<ItemData> unmodable = Collections.unmodifiableList(types);
	
	/**
	 * @return List of ItemDatas. The returned list is not modifyable, use {@link #add(ItemData)} and {@link #remove(ItemData)} if you need to change the list.
	 */
	public List<ItemData> getTypes() {
		return unmodable;
	}
	
	public int numTypes() {
		return types.size();
	}
	
	@Override
	public Iterator<ItemData> iterator() {
		return unmodable.iterator();
	}
	
	public boolean isContainedIn(final Inventory invi) {
		return isContainedIn(invi.getContents());
	}
	
	public boolean isContainedIn(final ItemStack[] list) {
		for (final ItemData d : types) {
			int found = 0;
			for (final ItemStack i : list) {
				if (d.isOfType(i)) {
					found += i == null ? 1 : i.getAmount();
					if (found >= getAmount()) {
						if (!all)
							return true;
						break;
					}
				}
			}
			if (all && found < getAmount())
				return false;
		}
		return all;
	}
	
	/**
	 * Removes this type from the given inventory. Does not call updateInventory for players.
	 * 
	 * @param invi
	 * @return Whether everything could be removed from the inventory
	 */
	public boolean removeFrom(final Inventory invi) {
		final ItemStack[] buf = invi.getContents();
		final boolean isPlayerInvi = invi instanceof PlayerInventory;
		final ItemStack[] armour = isPlayerInvi ? ((PlayerInventory) invi).getArmorContents() : null;
		
		@SuppressWarnings("unchecked")
		final boolean ok = removeFrom(Arrays.asList(buf), armour == null ? null : Arrays.asList(armour));
		
		invi.setContents(buf);
		if (isPlayerInvi)
			((PlayerInventory) invi).setArmorContents(armour);
		return ok;
	}
	
	/**
	 * 
	 * @param lists The lists to remove this type from. Each list should implement {@link RandomAccess} or this method will be slow.
	 * @return 
	 */
	public boolean removeFrom(final List<ItemStack>... lists) {
		int removed = 0;
		boolean ok = true;
		
		for (final ItemData d : types) {
			if (all)
				removed = 0;
			for (final List<ItemStack> list : lists) {
				if (list == null)
					continue;
				for (int i = 0; i < list.size(); i++) {
					final ItemStack is = list.get(i);
					if (is != null && d.isOfType(is)) {
						if (all && amount == -1) {
							list.set(i, null);
							removed = 1;
							continue;
						}
						final int toRemove = Math.min(is.getAmount(), getAmount() - removed);
						removed += toRemove;
						if (toRemove == is.getAmount()) {
							list.set(i, null);
						} else {
							is.setAmount(is.getAmount() - toRemove);
						}
						if (removed == getAmount()) {
							if (!all)
								return true;
							break;
						}
					}
				}
			}
			if (all)
				ok &= removed == getAmount();
		}
		
		if (!all)
			return false;
		return ok;
	}
	
	/**
	 * Adds this ItemType to the given list, without filling existing stacks.
	 * 
	 * @param list
	 */
	public void addTo(final List<ItemStack> list) {
		if (!isAll()) {
			list.add(getItem().getRandom());
			return;
		}
		for (final ItemStack is : getItem().getAll())
			list.add(is);
	}
	
	/**
	 * Tries to add this ItemType to the given inventory. Does not call updateInventory for players.
	 * 
	 * @param invi
	 * @return Whether everything could be added to the inventory
	 */
	public boolean addTo(final Inventory invi) {
		// important: don't use inventory.add() - it ignores max stack sizes
		final ItemStack[] buf = invi.getContents();
		final boolean b = addTo(buf);
		invi.setContents(buf);
		return b;
	}
	
	private static boolean addTo(final ItemStack is, final ItemStack[] buf) {
		if (is == null || is.getTypeId() == 0)
			return true;
		int added = 0;
		for (int i = 0; i < buf.length; i++) {
			if (Utils.itemStacksEqual(is, buf[i])) {
				final int toAdd = Math.min(buf[i].getMaxStackSize() - buf[i].getAmount(), is.getAmount() - added);
				added += toAdd;
				buf[i].setAmount(buf[i].getAmount() + toAdd);
				if (added == is.getAmount())
					return true;
			}
		}
		for (int i = 0; i < buf.length; i++) {
			if (buf[i] == null) {
				final int toAdd = Math.min(is.getMaxStackSize(), is.getAmount() - added);
				added += toAdd;
				buf[i] = is.clone();
				buf[i].setAmount(toAdd);
				if (added == is.getAmount())
					return true;
			}
		}
		return false;
	}
	
	public boolean addTo(final ItemStack[] buf) {
		if (!isAll()) {
			return addTo(getItem().getRandom(), buf);
		}
		boolean ok = true;
		for (final ItemStack is : getItem().getAll()) {
			ok &= addTo(is, buf);
		}
		return ok;
	}
	
}
