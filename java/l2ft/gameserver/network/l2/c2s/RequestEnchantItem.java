package l2ft.gameserver.network.l2.c2s;


import l2ft.commons.dao.JdbcEntityState;
import l2ft.commons.util.Rnd;
import l2ft.gameserver.Config;
import l2ft.gameserver.data.xml.holder.EnchantItemHolder;
import l2ft.gameserver.model.Player;
import l2ft.gameserver.model.actor.instances.player.Bonus;
import l2ft.gameserver.model.items.ItemInstance;
import l2ft.gameserver.model.items.PcInventory;
import l2ft.gameserver.network.l2.components.SystemMsg;
import l2ft.gameserver.network.l2.s2c.EnchantResult;
import l2ft.gameserver.network.l2.s2c.InventoryUpdate;
import l2ft.gameserver.network.l2.s2c.MagicSkillUse;
import l2ft.gameserver.network.l2.s2c.SystemMessage;
import l2ft.gameserver.templates.item.ItemTemplate;
import l2ft.gameserver.templates.item.support.EnchantScroll;
import l2ft.gameserver.utils.ItemFunctions;
import l2ft.gameserver.utils.Log;

public class RequestEnchantItem extends L2GameClientPacket
{
	private int _objectId, _catalystObjId;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_catalystObjId = readD();
	}

	@Override
	protected void runImpl()
	{
		Player player = getClient().getActiveChar();
		if(player == null)
			return;

		if(player.isActionsDisabled())
		{
			player.setEnchantScroll(null);
			player.sendActionFailed();
			return;
		}

		if(player.isInTrade())
		{
			player.setEnchantScroll(null);
			player.sendActionFailed();
			return;
		}

		if(player.isInStoreMode())
		{
			player.setEnchantScroll(null);
			player.sendPacket(EnchantResult.CANCEL);
			player.sendPacket(SystemMsg.YOU_CANNOT_ENCHANT_WHILE_OPERATING_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
			player.sendActionFailed();
			return;
		}

		PcInventory inventory = player.getInventory();
		inventory.writeLock();
		try
		{
			ItemInstance item = inventory.getItemByObjectId(_objectId);
			ItemInstance catalyst = _catalystObjId > 0 ? inventory.getItemByObjectId(_catalystObjId) : null;
			ItemInstance scroll = player.getEnchantScroll();

			if(item == null || scroll == null)
			{
				player.sendActionFailed();
				return;
			}

			if((item.getItemId() == 21580 || item.getItemId() == 21706) && item.getEnchantLevel() == 9)
			{
				player.sendPacket(EnchantResult.CANCEL);
				player.sendMessage(player.isLangRus() ? "Достигнут максимальный уровень заточки предмета!" : "The maximum level of sharpening the subject!");
				player.sendActionFailed();
				return;
			}
			EnchantScroll enchantScroll = EnchantItemHolder.getInstance().getEnchantScroll(scroll.getItemId());
			if(enchantScroll == null)
			{
				doEnchantOld(player, item, scroll, catalyst);
				return;
			}

			if(enchantScroll.getMaxEnchant() != -1 && item.getEnchantLevel() >= enchantScroll.getMaxEnchant())
			{
				player.sendPacket(EnchantResult.CANCEL);
				player.sendPacket(SystemMsg.INAPPROPRIATE_ENCHANT_CONDITIONS);
				player.sendActionFailed();
				return;
			}

			if(enchantScroll.getItems().size() > 0)
			{
				if(!enchantScroll.getItems().contains(item.getItemId()))
				{
					player.sendPacket(EnchantResult.CANCEL);
					player.sendPacket(SystemMsg.DOES_NOT_FIT_STRENGTHENING_CONDITIONS_OF_THE_SCROLL);
					player.sendActionFailed();
					return;
				}
			}
			else
			{
				if(!enchantScroll.getGrades().contains(item.getCrystalType()))
				{
					player.sendPacket(EnchantResult.CANCEL);
					player.sendPacket(SystemMsg.DOES_NOT_FIT_STRENGTHENING_CONDITIONS_OF_THE_SCROLL);
					player.sendActionFailed();
					return;
				}
			}

			if(!item.canBeEnchanted(false))
			{
				player.sendPacket(EnchantResult.CANCEL);
				player.sendPacket(SystemMsg.INAPPROPRIATE_ENCHANT_CONDITIONS);
				player.sendActionFailed();
				return;
			}

			if(!inventory.destroyItem(scroll, 1L) || catalyst != null && !inventory.destroyItem(catalyst, 1L))
			{
				player.sendPacket(EnchantResult.CANCEL);
				player.sendActionFailed();
				return;
			}

			boolean equipped = false;
			if(equipped = item.isEquipped())
				inventory.unEquipItem(item);

			int safeEnchantLevel = item.getTemplate().getBodyPart() == ItemTemplate.SLOT_FULL_ARMOR ? 4 : 3;

			int chance = 0;
			if(item.getItemId() != 21580 || item.getItemId() != 21706)
				chance = enchantScroll.getChance();
			else if(item.getItemId() == 21580 || item.getItemId() == 21706)
			{
				if(item.getEnchantLevel() == 9)
				{
					player.sendPacket(EnchantResult.CANCEL);
					player.sendActionFailed();
					return;
				}
				if(item.getEnchantLevel() < 3)
					chance = 100;
				else if(item.getEnchantLevel() == 3)
					chance = Config.ENCHANT_CHANCE_CRYSTAL_ARMOR_OLF_4;
				else if(item.getEnchantLevel() == 4)
					chance = Config.ENCHANT_CHANCE_CRYSTAL_ARMOR_OLF_5;
				else if(item.getEnchantLevel() == 5)
					chance = Config.ENCHANT_CHANCE_CRYSTAL_ARMOR_OLF_6;
				else if(item.getEnchantLevel() == 6)
					chance = Config.ENCHANT_CHANCE_CRYSTAL_ARMOR_OLF_7;
				else if(item.getEnchantLevel() == 7)
					chance = Config.ENCHANT_CHANCE_CRYSTAL_ARMOR_OLF_8;
				else if(item.getEnchantLevel() == 8)
					chance = Config.ENCHANT_CHANCE_CRYSTAL_ARMOR_OLF_9;
			}
			if(item.getEnchantLevel() < safeEnchantLevel)
				chance = 100;

			if(Rnd.chance(chance))
			{
				item.setEnchantLevel(item.getEnchantLevel() + 1);
				item.setJdbcState(JdbcEntityState.UPDATED);
				item.update();

				if(equipped)
					inventory.equipItem(item);

				player.sendPacket(new InventoryUpdate().addModifiedItem(item));

				player.sendPacket(EnchantResult.SUCESS);

				if(enchantScroll.isHasVisualEffect() && item.getEnchantLevel() > 3)
					player.broadcastPacket(new MagicSkillUse(player, player, 5965, 1, 500, 1500));
			}
			else
			{
				switch(enchantScroll.getResultType())
				{
					case CRYSTALS:
						if(item.isEquipped())
						player.sendDisarmMessage(item);

						Log.LogItem(player, Log.EnchantFail, item);

						if(!inventory.destroyItem(item, 1L))
						{
							player.sendActionFailed();
							return;
						}

						int crystalId = item.getCrystalType().cry;
						if(crystalId > 0 && item.getTemplate().getCrystalCount() > 0)
						{
							int crystalAmount = (int) (item.getTemplate().getCrystalCount() * 0.87);
							if(item.getEnchantLevel() > 3)
								crystalAmount += item.getTemplate().getCrystalCount() * 0.25 * (item.getEnchantLevel() - 3);
							if(crystalAmount < 1)
								crystalAmount = 1;

							player.sendPacket(new EnchantResult(1, crystalId, crystalAmount));
							ItemFunctions.addItem(player, crystalId, crystalAmount, true);
						}
						else
							player.sendPacket(EnchantResult.FAILED_NO_CRYSTALS);

						if(enchantScroll.isHasVisualEffect())
							player.broadcastPacket(new MagicSkillUse(player, player, 5949, 1, 500, 1500));
						break;
					case DROP_ENCHANT:
						item.setEnchantLevel(0);
						item.setJdbcState(JdbcEntityState.UPDATED);
						item.update();

						if(equipped)
							inventory.equipItem(item);

						player.sendPacket(new InventoryUpdate().addModifiedItem(item));
						player.sendPacket(SystemMsg.THE_BLESSED_ENCHANT_FAILED);
						player.sendPacket(EnchantResult.BLESSED_FAILED);
						break;
					case NOTHING:
						player.sendPacket(EnchantResult.ANCIENT_FAILED);
						break;
				}
			}
		}
		finally
		{
			inventory.writeUnlock();

			player.setEnchantScroll(null);
			player.updateStats();
		}
	}

	//@Deprecated
	private static void doEnchantOld(Player player, ItemInstance item, ItemInstance scroll, ItemInstance catalyst)
	{
		PcInventory inventory = player.getInventory();
		// Затычка, ибо клиент криво обрабатывает RequestExTryToPutEnchantSupportItem
		if(!ItemFunctions.checkCatalyst(item, catalyst))
			catalyst = null;

		if(!item.canBeEnchanted(true))
		{
			player.sendPacket(EnchantResult.CANCEL);
			player.sendPacket(SystemMsg.INAPPROPRIATE_ENCHANT_CONDITIONS);
			player.sendActionFailed();
			return;
		}
		if((item.getItemId() == 21580 || item.getItemId() == 21706) && item.getEnchantLevel() == 9)
		{
			player.sendPacket(EnchantResult.CANCEL);
			player.sendMessage(player.isLangRus() ? "Достигнут максимальный уровень заточки предмета!" : "The maximum level of sharpening the subject!");
			player.sendActionFailed();
			return;
		}
		int crystalId = ItemFunctions.getEnchantCrystalId(item, scroll, catalyst);

		if(crystalId == -1)
		{
			player.sendPacket(EnchantResult.CANCEL);
			player.sendPacket(SystemMsg.DOES_NOT_FIT_STRENGTHENING_CONDITIONS_OF_THE_SCROLL);
			player.sendActionFailed();
			return;
		}

		int scrollId = scroll.getItemId();

		if(scrollId == 13540 && item.getItemId() != 13539)
		{
			player.sendPacket(EnchantResult.CANCEL);
			player.sendPacket(SystemMsg.DOES_NOT_FIT_STRENGTHENING_CONDITIONS_OF_THE_SCROLL);
			player.sendActionFailed();
			return;
		}

		// ольф 21580(21581/21582)
		if((scrollId == 21581 || scrollId == 21582) && item.getItemId() != 21580)
		{
			player.sendPacket(EnchantResult.CANCEL);
			player.sendPacket(SystemMsg.DOES_NOT_FIT_STRENGTHENING_CONDITIONS_OF_THE_SCROLL);
			player.sendActionFailed();
			return;
		}

		// ольф 21706(21707)
		if(scrollId == 21707 && item.getItemId() != 21706)
		{
			player.sendPacket(EnchantResult.CANCEL);
			player.sendPacket(SystemMsg.DOES_NOT_FIT_STRENGTHENING_CONDITIONS_OF_THE_SCROLL);
			player.sendActionFailed();
			return;
		}

		// ольф 21706(21707)
		if(scrollId != 21707 && item.getItemId() == 21706)
		{
			player.sendPacket(EnchantResult.CANCEL);
			player.sendPacket(SystemMsg.DOES_NOT_FIT_STRENGTHENING_CONDITIONS_OF_THE_SCROLL);
			player.sendActionFailed();
			return;
		}

		// ольф 21580(21581/21582)
		if((scrollId != 21581 || scrollId != 21582) && item.getItemId() == 21580)
		{
			player.sendPacket(EnchantResult.CANCEL);
			player.sendPacket(SystemMsg.DOES_NOT_FIT_STRENGTHENING_CONDITIONS_OF_THE_SCROLL);
			player.sendActionFailed();
			return;
		}

		// TODO: [pchayka] временный хардкод до улучения системы описания свитков заточки
		if(ItemFunctions.isDestructionWpnEnchantScroll(scrollId) && item.getEnchantLevel() >= 15 || ItemFunctions.isDestructionArmEnchantScroll(scrollId) && item.getEnchantLevel() >= 6)
		{
			player.sendPacket(EnchantResult.CANCEL);
			player.sendPacket(SystemMsg.DOES_NOT_FIT_STRENGTHENING_CONDITIONS_OF_THE_SCROLL);
			player.sendActionFailed();
			return;
		}

		int itemType = item.getTemplate().getType2();
		boolean fail = false;
		switch(item.getItemId())
		{
			case 13539:
				if(item.getEnchantLevel() >= Config.ENCHANT_MAX_MASTER_YOGI_STAFF)
					fail = true;
				break;
			case 21580:
				if(item.getEnchantLevel() < 9)
					fail = false;
				else
					fail = true;
				break;
			default:
			{
				if(itemType == ItemTemplate.TYPE2_WEAPON)
				{
					if(Config.ENCHANT_MAX_WEAPON > 0 && item.getEnchantLevel() >= Config.ENCHANT_MAX_WEAPON)
						fail = true;
				}
				else if(itemType == ItemTemplate.TYPE2_SHIELD_ARMOR)
				{	
					if(Config.ENCHANT_MAX_ARMOR > 0 && item.getEnchantLevel() >= Config.ENCHANT_MAX_ARMOR)
						fail = true;
				}	
				else if(itemType == ItemTemplate.TYPE2_ACCESSORY)
				{	
					if(Config.ENCHANT_MAX_JEWELRY > 0 && item.getEnchantLevel() >= Config.ENCHANT_MAX_JEWELRY)
						fail = true;
				}		
				else fail = true;
			}
				break;
		}

		if(!inventory.destroyItem(scroll, 1L) || catalyst != null && !inventory.destroyItem(catalyst, 1L))
		{
			player.sendPacket(EnchantResult.CANCEL);
			player.sendActionFailed();
			return;
		}

		if(fail)
		{
			player.sendPacket(EnchantResult.CANCEL);
			player.sendPacket(SystemMsg.INAPPROPRIATE_ENCHANT_CONDITIONS);
			player.sendActionFailed();
			return;
		}

		int safeEnchantLevel = item.getTemplate().getBodyPart() == ItemTemplate.SLOT_FULL_ARMOR ? Config.SAFE_ENCHANT_FULL_BODY : Config.SAFE_ENCHANT_COMMON;

		double chance;
		if(item.getEnchantLevel() < safeEnchantLevel)
			chance = 100;
		else if(itemType == ItemTemplate.TYPE2_WEAPON)
		{
			if(Config.USE_ALT_ENCHANT)
			{
				if(ItemFunctions.isCrystallEnchantScroll(scrollId))
					chance = item.getEnchantLevel() > Config.ENCHANT_WEAPON_FIGHT_CRYSTAL.size() ? Config.ENCHANT_WEAPON_FIGHT_CRYSTAL.get(Config.ENCHANT_WEAPON_FIGHT_CRYSTAL.size() - 1) : Config.ENCHANT_WEAPON_FIGHT_CRYSTAL.get(item.getEnchantLevel());
				else if(ItemFunctions.isBlessedEnchantScroll(scrollId))
					chance = item.getEnchantLevel() > Config.ENCHANT_WEAPON_FIGHT_BLESSED.size() ? Config.ENCHANT_WEAPON_FIGHT_BLESSED.get(Config.ENCHANT_WEAPON_FIGHT_BLESSED.size() - 1) : Config.ENCHANT_WEAPON_FIGHT_BLESSED.get(item.getEnchantLevel());
				else
					chance = item.getEnchantLevel() > Config.ENCHANT_WEAPON_FIGHT.size() ? Config.ENCHANT_WEAPON_FIGHT.get(Config.ENCHANT_WEAPON_FIGHT.size() - 1) : Config.ENCHANT_WEAPON_FIGHT.get(item.getEnchantLevel());
			}
                        else if((Config.SERVICES_RATE_TYPE != Bonus.NO_BONUS && player.getNetConnection().getBonus() > 1) && Config.USE_ALT_ENCHANT_PA)
                        {
                                if(ItemFunctions.isCrystallEnchantScroll(scrollId))
					chance = item.getEnchantLevel() > Config.ENCHANT_WEAPON_FIGHT_CRYSTAL_PA.size() ? Config.ENCHANT_WEAPON_FIGHT_CRYSTAL_PA.get(Config.ENCHANT_WEAPON_FIGHT_CRYSTAL_PA.size() - 1) : Config.ENCHANT_WEAPON_FIGHT_CRYSTAL_PA.get(item.getEnchantLevel());
				else if(ItemFunctions.isBlessedEnchantScroll(scrollId))
					chance = item.getEnchantLevel() > Config.ENCHANT_WEAPON_FIGHT_BLESSED_PA.size() ? Config.ENCHANT_WEAPON_FIGHT_BLESSED_PA.get(Config.ENCHANT_WEAPON_FIGHT_BLESSED_PA.size() - 1) : Config.ENCHANT_WEAPON_FIGHT_BLESSED_PA.get(item.getEnchantLevel());
				else
					chance = item.getEnchantLevel() > Config.ENCHANT_WEAPON_FIGHT_PA.size() ? Config.ENCHANT_WEAPON_FIGHT_PA.get(Config.ENCHANT_WEAPON_FIGHT_PA.size() - 1) : Config.ENCHANT_WEAPON_FIGHT_PA.get(item.getEnchantLevel());
                        }        
			else if(Config.SERVICES_RATE_TYPE != Bonus.NO_BONUS && player.getNetConnection().getBonus() > 1)
			{
				if (ItemFunctions.isBlessedEnchantScroll(scrollId)) chance = Config.ENCHANT_CHANCE_WEAPON_BLESS_PA;
				else chance = ItemFunctions.isCrystallEnchantScroll(scrollId) ? Config.ENCHANT_CHANCE_CRYSTAL_WEAPON_PA : Config.ENCHANT_CHANCE_WEAPON_PA;
			}
			else if (ItemFunctions.isBlessedEnchantScroll(scrollId)) chance = Config.ENCHANT_CHANCE_WEAPON_BLESS;
			else chance = ItemFunctions.isCrystallEnchantScroll(scrollId) ? Config.ENCHANT_CHANCE_CRYSTAL_WEAPON : Config.ENCHANT_CHANCE_WEAPON;
		}
		else if(itemType == ItemTemplate.TYPE2_SHIELD_ARMOR)
			if(Config.USE_ALT_ENCHANT)
			{
				if(ItemFunctions.isCrystallEnchantScroll(scrollId))
					chance = item.getEnchantLevel() > Config.ENCHANT_ARMOR_CRYSTAL.size() ? Config.ENCHANT_ARMOR_CRYSTAL.get(Config.ENCHANT_ARMOR_CRYSTAL.size() - 1) : Config.ENCHANT_ARMOR_CRYSTAL.get(item.getEnchantLevel());
				else if(ItemFunctions.isBlessedEnchantScroll(scrollId))
					chance = item.getEnchantLevel() > Config.ENCHANT_ARMOR_BLESSED.size() ? Config.ENCHANT_ARMOR_BLESSED.get(Config.ENCHANT_ARMOR_BLESSED.size() - 1) : Config.ENCHANT_ARMOR_BLESSED.get(item.getEnchantLevel());
				else
					chance = item.getEnchantLevel() > Config.ENCHANT_ARMOR.size() ? Config.ENCHANT_ARMOR.get(Config.ENCHANT_ARMOR.size() - 1) : Config.ENCHANT_ARMOR.get(item.getEnchantLevel());
			}
                        else if((Config.SERVICES_RATE_TYPE != Bonus.NO_BONUS && player.getNetConnection().getBonus() > 1) && Config.USE_ALT_ENCHANT_PA)
                        {
				if(ItemFunctions.isCrystallEnchantScroll(scrollId))
					chance = item.getEnchantLevel() > Config.ENCHANT_ARMOR_CRYSTAL_PA.size() ? Config.ENCHANT_ARMOR_CRYSTAL_PA.get(Config.ENCHANT_ARMOR_CRYSTAL_PA.size() - 1) : Config.ENCHANT_ARMOR_CRYSTAL_PA.get(item.getEnchantLevel());
				else if(ItemFunctions.isBlessedEnchantScroll(scrollId))
					chance = item.getEnchantLevel() > Config.ENCHANT_ARMOR_BLESSED_PA.size() ? Config.ENCHANT_ARMOR_BLESSED_PA.get(Config.ENCHANT_ARMOR_BLESSED_PA.size() - 1) : Config.ENCHANT_ARMOR_BLESSED_PA.get(item.getEnchantLevel());
				else
					chance = item.getEnchantLevel() > Config.ENCHANT_ARMOR_PA.size() ? Config.ENCHANT_ARMOR_PA.get(Config.ENCHANT_ARMOR_PA.size() - 1) : Config.ENCHANT_ARMOR_PA.get(item.getEnchantLevel());
			}   
			else if(Config.SERVICES_RATE_TYPE != Bonus.NO_BONUS && player.getNetConnection().getBonus() > 1)
			{
				if (ItemFunctions.isBlessedEnchantScroll(scrollId)) chance = Config.ENCHANT_CHANCE_ARMOR_BLESS_PA;
				else chance = ItemFunctions.isCrystallEnchantScroll(scrollId) ? Config.ENCHANT_CHANCE_CRYSTAL_ARMOR_PA : Config.ENCHANT_CHANCE_ARMOR_PA;
			}
			else if (ItemFunctions.isBlessedEnchantScroll(scrollId)) chance = Config.ENCHANT_CHANCE_ARMOR_BLESS;
			else chance = ItemFunctions.isCrystallEnchantScroll(scrollId) ? Config.ENCHANT_CHANCE_CRYSTAL_ARMOR : Config.ENCHANT_CHANCE_ARMOR;
		else if(itemType == ItemTemplate.TYPE2_ACCESSORY)
			if(Config.USE_ALT_ENCHANT)
			{
				if(ItemFunctions.isCrystallEnchantScroll(scrollId))
					chance = item.getEnchantLevel() > Config.ENCHANT_ARMOR_JEWELRY_CRYSTAL.size() ? Config.ENCHANT_ARMOR_JEWELRY_CRYSTAL.get(Config.ENCHANT_ARMOR_JEWELRY_CRYSTAL.size() - 1) : Config.ENCHANT_ARMOR_JEWELRY_CRYSTAL.get(item.getEnchantLevel());
				else if(ItemFunctions.isBlessedEnchantScroll(scrollId))
					chance = item.getEnchantLevel() > Config.ENCHANT_ARMOR_JEWELRY_BLESSED.size() ? Config.ENCHANT_ARMOR_JEWELRY_BLESSED.get(Config.ENCHANT_ARMOR_JEWELRY_BLESSED.size() - 1) : Config.ENCHANT_ARMOR_JEWELRY_BLESSED.get(item.getEnchantLevel());
				else
					chance = item.getEnchantLevel() > Config.ENCHANT_ARMOR_JEWELRY.size() ? Config.ENCHANT_ARMOR_JEWELRY.get(Config.ENCHANT_ARMOR_JEWELRY.size() - 1) : Config.ENCHANT_ARMOR_JEWELRY.get(item.getEnchantLevel());
			}
			else if((Config.SERVICES_RATE_TYPE != Bonus.NO_BONUS && player.getNetConnection().getBonus() > 1) && Config.USE_ALT_ENCHANT_PA)
                        {
				if(ItemFunctions.isCrystallEnchantScroll(scrollId))
					chance = item.getEnchantLevel() > Config.ENCHANT_ARMOR_JEWELRY_CRYSTAL_PA.size() ? Config.ENCHANT_ARMOR_JEWELRY_CRYSTAL_PA.get(Config.ENCHANT_ARMOR_JEWELRY_CRYSTAL_PA.size() - 1) : Config.ENCHANT_ARMOR_JEWELRY_CRYSTAL_PA.get(item.getEnchantLevel());
				else if(ItemFunctions.isBlessedEnchantScroll(scrollId))
					chance = item.getEnchantLevel() > Config.ENCHANT_ARMOR_JEWELRY_BLESSED_PA.size() ? Config.ENCHANT_ARMOR_JEWELRY_BLESSED_PA.get(Config.ENCHANT_ARMOR_JEWELRY_BLESSED_PA.size() - 1) : Config.ENCHANT_ARMOR_JEWELRY_BLESSED_PA.get(item.getEnchantLevel());
				else
					chance = item.getEnchantLevel() > Config.ENCHANT_ARMOR_JEWELRY_PA.size() ? Config.ENCHANT_ARMOR_JEWELRY_PA.get(Config.ENCHANT_ARMOR_JEWELRY_PA.size() - 1) : Config.ENCHANT_ARMOR_JEWELRY_PA.get(item.getEnchantLevel());
			}    
                        else if(Config.SERVICES_RATE_TYPE != Bonus.NO_BONUS && player.getNetConnection().getBonus() > 1)
			{
				if (ItemFunctions.isBlessedEnchantScroll(scrollId)) chance = Config.ENCHANT_CHANCE_ACCESSORY_BLESS_PA;
				else chance = ItemFunctions.isCrystallEnchantScroll(scrollId) ? Config.ENCHANT_CHANCE_CRYSTAL_ACCESSORY_PA : Config.ENCHANT_CHANCE_ACCESSORY_PA;
			}
			else if (ItemFunctions.isBlessedEnchantScroll(scrollId)) chance = Config.ENCHANT_CHANCE_ACCESSORY_BLESS;
			else chance = ItemFunctions.isCrystallEnchantScroll(scrollId) ? Config.ENCHANT_CHANCE_CRYSTAL_ACCESSORY : Config.ENCHANT_CHANCE_ACCESSORY;
		else
		{
			player.sendPacket(EnchantResult.CANCEL);
			player.sendActionFailed();
			return;
		}

		if(ItemFunctions.isDivineEnchantScroll(scrollId)) // Item Mall divine
			chance = 100;
		else if(ItemFunctions.isItemMallEnchantScroll(scrollId)) // Item Mall normal/ancient
			chance += 10;

		if(catalyst != null)
			chance += ItemFunctions.getCatalystPower(catalyst.getItemId());

		if(scrollId == 13540)
			chance = item.getEnchantLevel() < Config.SAFE_ENCHANT_MASTER_YOGI_STAFF ? 100 : Config.ENCHANT_CHANCE_MASTER_YOGI_STAFF;
		else if(scrollId == 21581 || scrollId == 21582)
			if(item.getEnchantLevel() < 9)
				chance = item.getEnchantLevel() < 3 ? 100 : Config.ENCHANT_CHANCE_CRYSTAL_ARMOR_OLF;
			else
			{
				player.sendPacket(EnchantResult.CANCEL);
				player.sendActionFailed();
				return;
			}

		boolean equipped = false;
		if(equipped = item.isEquipped())
			inventory.unEquipItem(item);
		if(Rnd.chance(chance))
		{
			item.setEnchantLevel(item.getEnchantLevel() + 1);
			item.setJdbcState(JdbcEntityState.UPDATED);
			item.update();

			if(equipped)
				inventory.equipItem(item);

			player.sendPacket(new InventoryUpdate().addModifiedItem(item));

			player.sendPacket(EnchantResult.SUCESS);

			if(scrollId == 13540 && item.getEnchantLevel() > 3 || Config.SHOW_ENCHANT_EFFECT_RESULT)
				player.broadcastPacket(new MagicSkillUse(player, player, 5965, 1, 500, 1500));
		}
		else if(ItemFunctions.isBlessedEnchantScroll(scrollId)) // фейл, но заточка блесед
		{
			item.setEnchantLevel(Config.SAFE_ENCHANT_LVL);
			item.setJdbcState(JdbcEntityState.UPDATED);
			item.update();

			if(equipped)
				inventory.equipItem(item);

			player.sendPacket(new InventoryUpdate().addModifiedItem(item));
			player.sendPacket(SystemMsg.THE_BLESSED_ENCHANT_FAILED);
			player.sendPacket(EnchantResult.BLESSED_FAILED);
		}
		else if(ItemFunctions.isAncientEnchantScroll(scrollId) || ItemFunctions.isDestructionWpnEnchantScroll(scrollId) || ItemFunctions.isDestructionArmEnchantScroll(scrollId)) // фейл, но заточка ancient или destruction
			player.sendPacket(EnchantResult.ANCIENT_FAILED);
		else
		// фейл, разбиваем вещь
		{
			if(item.isEquipped())
				player.sendDisarmMessage(item);

			Log.LogItem(player, Log.EnchantFail, item);

			if(!inventory.destroyItem(item, 1L))
			{
				player.sendPacket(new SystemMessage(SystemMessage.THE_ENCHANTMENT_HAS_FAILED_YOUR_S1_HAS_BEEN_CRYSTALLIZED).addItemName(item.getItemId()));
				player.sendActionFailed();
				return;
			}

			if(crystalId > 0 && item.getTemplate().getCrystalCount() > 0)
			{
				int crystalAmount = (int) (item.getTemplate().getCrystalCount() * 0.87);
				if(item.getEnchantLevel() > 3)
					crystalAmount += item.getTemplate().getCrystalCount() * 0.25 * (item.getEnchantLevel() - 3);
				if(crystalAmount < 1)
					crystalAmount = 1;

				player.sendPacket(new EnchantResult(1, crystalId, crystalAmount));
				ItemFunctions.addItem(player, crystalId, crystalAmount, true);
			}
			else
				player.sendPacket(EnchantResult.FAILED_NO_CRYSTALS);

			if(scrollId == 13540 || Config.SHOW_ENCHANT_EFFECT_RESULT)
				player.broadcastPacket(new MagicSkillUse(player, player, 5949, 1, 500, 1500));
		}
	}
}