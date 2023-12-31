package l2ft.gameserver.network.l2.c2s;

import l2ft.gameserver.model.Player;
import l2ft.gameserver.model.items.ItemInstance;
import l2ft.gameserver.model.items.PcInventory;
import l2ft.gameserver.network.l2.s2c.ExPutEnchantSupportItemResult;
import l2ft.gameserver.utils.ItemFunctions;

public class RequestExTryToPutEnchantSupportItem extends L2GameClientPacket
{
	private int _itemId;
	private int _catalystId;

	@Override
	protected void readImpl()
	{
		_catalystId = readD();
		_itemId = readD();
	}

	@Override
	protected void runImpl()
	{
		Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		PcInventory inventory = activeChar.getInventory();
		ItemInstance itemToEnchant = inventory.getItemByObjectId(_itemId);
		ItemInstance catalyst = inventory.getItemByObjectId(_catalystId);

		if(ItemFunctions.checkCatalyst(itemToEnchant, catalyst))
			activeChar.sendPacket(new ExPutEnchantSupportItemResult(1));
		else
			activeChar.sendPacket(new ExPutEnchantSupportItemResult(0));
	}
}