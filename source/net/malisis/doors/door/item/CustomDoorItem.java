/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Ordinastie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.malisis.doors.door.item;

import java.util.HashMap;

import net.malisis.doors.MalisisDoors;
import net.malisis.doors.block.MixedBlock;
import net.malisis.doors.door.DoorRegistry;
import net.malisis.doors.door.block.Door;
import net.malisis.doors.door.tileentity.CustomDoorTileEntity;
import net.malisis.doors.door.tileentity.DoorTileEntity;
import net.malisis.doors.entity.DoorFactoryTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * @author Ordinastie
 * 
 */
public class CustomDoorItem extends ItemDoor
{
	private static HashMap<Item, Block> itemsAllowed = new HashMap<>();
	static
	{
		itemsAllowed.put(Items.flint_and_steel, Blocks.fire);
		itemsAllowed.put(Items.ender_pearl, Blocks.portal);
		itemsAllowed.put(Items.water_bucket, Blocks.water);
		itemsAllowed.put(Items.lava_bucket, Blocks.lava);
	}

	public CustomDoorItem()
	{
		super(Material.wood);
		setUnlocalizedName("custom_door");
	}

	/**
	 * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return True if something
	 * happen and false if it don't. This is for ITEMS, not BLOCKS
	 */
	@Override
	public boolean onItemUse(ItemStack itemStack, EntityPlayer player, World world, int x, int y, int z, int side, float par8, float par9, float par10)
	{
		if (side != 1)
			return false;

		++y;
		if (!player.canPlayerEdit(x, y, z, side, itemStack) || !player.canPlayerEdit(x, y + 1, z, side, itemStack))
			return false;

		Block block = MalisisDoors.Blocks.customDoor;
		if (!block.canPlaceBlockAt(world, x, y, z))
			return false;

		int i1 = MathHelper.floor_double((player.rotationYaw + 180.0F) * 4.0F / 360.0F - 0.5D) & 3;
		placeDoorBlock(world, x, y, z, i1, block);

		DoorTileEntity te = Door.getDoor(world, x, y, z);
		if (te == null)
			return false;

		((CustomDoorTileEntity) te).onBlockPlaced(itemStack);
		--itemStack.stackSize;
		return true;
	}

	@Override
	public void registerIcons(IIconRegister par1IconRegister)
	{

	}

	@Override
	public void onCreated(ItemStack itemStack, World world, EntityPlayer player)
	{
		itemStack.stackTagCompound = new NBTTagCompound();
	}

	public static ItemStack fromDoorFactory(DoorFactoryTileEntity te)
	{
		ItemStack frameItemStack = te.frameSlot.getItemStack();
		ItemStack topMaterialItemStack = te.topMaterialSlot.getItemStack();
		ItemStack bottomMaterialItemStack = te.bottomMaterialSlot.getItemStack();
		if (!canBeUsedForDoor(frameItemStack, true) || !canBeUsedForDoor(topMaterialItemStack, false)
				|| !canBeUsedForDoor(bottomMaterialItemStack, false))
			return null;

		//frame
		Block frameBlock = Block.getBlockFromItem(frameItemStack.getItem());
		int frameMetadata = ((ItemBlock) frameItemStack.getItem()).getMetadata(frameItemStack.getItemDamage());

		//top material
		Block topMaterialBlock = itemsAllowed.get(topMaterialItemStack.getItem());
		if (topMaterialBlock == null)
			topMaterialBlock = Block.getBlockFromItem(topMaterialItemStack.getItem());

		int topMaterialMetadata;
		if (topMaterialItemStack.getItem() instanceof ItemBlock)
			topMaterialMetadata = ((ItemBlock) topMaterialItemStack.getItem()).getMetadata(topMaterialItemStack.getItemDamage());
		else
			topMaterialMetadata = topMaterialItemStack.getItemDamage();

		//bottom material
		Block bottomMaterialBlock = itemsAllowed.get(bottomMaterialItemStack.getItem());
		if (bottomMaterialBlock == null)
			bottomMaterialBlock = Block.getBlockFromItem(bottomMaterialItemStack.getItem());

		int bottomMaterialMetadata;
		if (bottomMaterialItemStack.getItem() instanceof ItemBlock)
			bottomMaterialMetadata = ((ItemBlock) bottomMaterialItemStack.getItem()).getMetadata(bottomMaterialItemStack.getItemDamage());
		else
			bottomMaterialMetadata = bottomMaterialItemStack.getItemDamage();

		//NBT
		NBTTagCompound nbt = new NBTTagCompound();

		if (te.getDoorMovement() != null)
			nbt.setString("movement", DoorRegistry.getId(te.getDoorMovement()));
		if (te.getDoorSound() != null)
			nbt.setString("doorSound", DoorRegistry.getId(te.getDoorSound()));
		nbt.setInteger("openingTime", te.getOpeningTime());
		nbt.setBoolean("requireRedstone", te.requireRedstone());
		nbt.setBoolean("doubleDoor", te.isDoubleDoor());

		nbt.setInteger("frame", Block.getIdFromBlock(frameBlock));
		nbt.setInteger("topMaterial", Block.getIdFromBlock(topMaterialBlock));
		nbt.setInteger("bottomMaterial", Block.getIdFromBlock(bottomMaterialBlock));
		nbt.setInteger("frameMetadata", frameMetadata);
		nbt.setInteger("topMaterialMetadata", topMaterialMetadata);
		nbt.setInteger("bottomMaterialMetadata", bottomMaterialMetadata);

		//ItemStack
		ItemStack is = new ItemStack(MalisisDoors.Items.customDoorItem, 1);
		is.stackTagCompound = nbt;
		return is;
	}

	public static ItemStack fromTileEntity(CustomDoorTileEntity te)
	{
		NBTTagCompound nbt = new NBTTagCompound();

		if (te.getMovement() != null)
			nbt.setString("movement", DoorRegistry.getId(te.getMovement()));
		if (te.getDoorSound() != null)
			nbt.setString("doorSound", DoorRegistry.getId(te.getDoorSound()));
		nbt.setInteger("openingTime", te.getOpeningTime());
		nbt.setBoolean("requireRedstone", te.requireRedstone());
		nbt.setBoolean("doubleDoor", te.isDoubleDoor());

		nbt.setInteger("frame", Block.getIdFromBlock(te.getFrame()));
		nbt.setInteger("topMaterial", Block.getIdFromBlock(te.getTopMaterial()));
		nbt.setInteger("bottomMaterial", Block.getIdFromBlock(te.getBottomMaterial()));
		nbt.setInteger("frameMetadata", te.getFrameMetadata());
		nbt.setInteger("topMaterialMetadata", te.getTopMaterialMetadata());
		nbt.setInteger("bottomMaterialMetadata", te.getBottomMaterialMetadata());

		ItemStack is = new ItemStack(MalisisDoors.Items.customDoorItem, 1);
		is.stackTagCompound = nbt;
		return is;
	}

	public static boolean canBeUsedForDoor(ItemStack itemStack, boolean frame)
	{
		if (!frame && itemsAllowed.get(itemStack.getItem()) != null)
			return true;

		Block block = Block.getBlockFromItem(itemStack.getItem());
		return !(block instanceof MixedBlock) && block.getRenderType() != -1;
	}
}