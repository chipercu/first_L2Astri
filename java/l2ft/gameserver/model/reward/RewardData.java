package l2ft.gameserver.model.reward;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import l2ft.commons.math.SafeMath;
import l2ft.commons.util.Rnd;
import l2ft.gameserver.Config;
import l2ft.gameserver.data.xml.holder.ItemHolder;
import l2ft.gameserver.model.Player;
import l2ft.gameserver.templates.item.ItemTemplate;

public class RewardData implements Cloneable
{
	private ItemTemplate _item;
	private boolean _notRate = false; // Рейты к вещи не применяются
	
	private long _mindrop;
	private long _maxdrop;
	private double _chance;
	private double _chanceInGroup;
	
	public RewardData(int itemId)
	{
		_item = ItemHolder.getInstance().getTemplate(itemId);
		if(_item.isArrow() // стрелы не рейтуются
				|| (Config.NO_RATE_EQUIPMENT && _item.isEquipment()) // отключаемая рейтовка эквипа
				|| (Config.NO_RATE_KEY_MATERIAL && _item.isKeyMatherial()) // отключаемая рейтовка ключевых материалов
				|| (Config.NO_RATE_RECIPES && _item.isRecipe()) // отключаемая рейтовка рецептов
				|| ArrayUtils.contains(Config.NO_RATE_ITEMS, itemId) // индивидаульная отключаемая рейтовка для списка предметов
				|| (Config.NO_RATE_HERBS && _item.isHerb()) // отключаемая рейтовка хербов
				|| (Config.NO_RATE_ENCHANT_SCROLL && _item.isEnchantScroll())
				|| (Config.NO_RATE_ATT && _item.isAttributeCrystal())
				|| (Config.NO_RATE_ATT && _item.isAttributeJewel())
				|| (Config.NO_RATE_ATT && _item.isAttributeStone())
				|| (Config.NO_RATE_ATT && _item.isAttributeEnergy())
				|| (Config.NO_RATE_LIFE_STONE && _item.isLifeStone())
				|| (Config.NO_RATE_CODEX_BOOK && _item.isCodexBook())
				|| (Config.NO_RATE_FORGOTTEN_SCROLL && _item.isForgottenScroll()))
			_notRate = true;
	}

	public RewardData(int itemId, long min, long max, double chance)
	{
		this(itemId);
		_mindrop = min;
		_maxdrop = max;
		_chance = chance;
	}

	public boolean notRate()
	{
		return _notRate;
	}
	
	public void setNotRate(boolean notRate)
	{
		_notRate = notRate;
	}

	public int getItemId()
	{
		return _item.getItemId();
	}

	public ItemTemplate getItem()
	{
		return _item;
	}

	public long getMinDrop()
	{
		return _mindrop;
	}

	public long getMaxDrop()
	{
		return _maxdrop;
	}

	public double getChance()
	{
		return _chance;
	}

	public void setMinDrop(long mindrop)
	{
		_mindrop = mindrop;
	}

	public void setMaxDrop(long maxdrop)
	{
		_maxdrop = maxdrop;
	}

	public void setChance(double chance)
	{
		_chance = chance;
	}

	public void setChanceInGroup(double chance)
	{
		_chanceInGroup = chance;
	}

	public double getChanceInGroup()
	{
		return _chanceInGroup;
	}

	@Override
	public String toString()
	{
		return "ItemID: " + getItem() + " Min: " + getMinDrop() + " Max: " + getMaxDrop() + " Chance: " + getChance() / 10000.0 + "%";
	}

	@Override
	public RewardData clone()
	{
		return new RewardData(getItemId(), getMinDrop(), getMaxDrop(), getChance());
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof RewardData)
		{
			RewardData drop = (RewardData) o;
			return drop.getItemId() == getItemId();
		}
		return false;
	}

	/**
	 * Подсчет шанса выпадения этой конкретной вещи
	 * Используется в эвентах и некоторых специальных механизмах
	 * @param player игрок (его бонус влияет на шанс)
	 * @param mod (просто множитель шанса)
	 * @return информация о выпавшей вещи
	 */
	public List<RewardItem> roll(Player player, double mod)
	{
		double rate = 1.0;
		if(_item.isAdena())
			rate = Config.RATE_DROP_ADENA * player.getRateAdena();
		else if(_item.isHerb())
			rate = Config.RATE_DROP_HERBS * 1.;
		else
			rate = Config.RATE_DROP_ITEMS * (player != null ? player.getRateItems() : 1.);
		
		return roll(rate * mod);
	}
	
	/**
	 * Подсчет шанса выпадения этой конкретной вещи
	 * Используется в эвентах и некоторых специальных механизмах
	 * @param rate множитель количества
	 * @return информация о выпавшей вещи
	 */
	public List<RewardItem> roll(double rate)
	{
		double mult = Math.ceil(rate);

		List<RewardItem> ret = new ArrayList<>(1);
		RewardItem t = null;
		long count;
		for(int n = 0; n < mult; n++)
		{
			if(Rnd.get(RewardList.MAX_CHANCE) <= _chance * Math.min(rate - n, 1.0))
			{
				if(getMinDrop() >= getMaxDrop())
					count = getMinDrop();
				else
					count = Rnd.get(getMinDrop(), getMaxDrop());

				if(t == null)
				{
					ret.add(t = new RewardItem(_item.getItemId()));
					t.count = count;
				}
				else
					t.count = SafeMath.addAndLimit(t.count, count);
			}
		}

		return ret;
	}
}