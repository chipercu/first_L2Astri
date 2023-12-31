package l2ft.gameserver.model.reward;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import l2ft.commons.math.SafeMath;
import l2ft.commons.util.Rnd;
import l2ft.gameserver.Config;
import l2ft.gameserver.model.Player;

public class RewardGroup implements Cloneable
{
	private double _chance;
	private boolean _isAdena = false; // Шанс фиксирован, растет только количество
	private boolean _isHerb = false;
	private boolean _rate_rb = false;
	private boolean _isRaid = false;
	private boolean _notRate = false; // Рейты вообще не применяются
	private List<RewardData> _items = new ArrayList<>();
	private double _chanceSum;

	public RewardGroup(double chance)
	{
		if(chance > RewardList.MAX_CHANCE)
			chance = RewardList.MAX_CHANCE;
		setChance(chance);
	}

	public boolean notRate()
	{
		return _notRate;
	}

	public void setNotRate(boolean notRate)
	{
		_notRate = notRate;
	}
	
	public double getChance()
	{
		return _chance;
	}

	public void setChance(double chance)
	{
		_chance = chance;
	}

	public boolean isAdena()
	{
		return _isAdena;
	}
	
	public void setIsAdena(boolean isAdena)
	{
		_isAdena = isAdena;
	}

	public boolean isHerb()
	{
		return _isHerb;
	}
	
	public void setIsHerb(boolean isHerb)
	{
		_isHerb = isHerb;
	}

	public void addData(RewardData item)
	{
		if(item.getItem().isAdena())
			setIsAdena(true);
		if(item.getItem().isHerb())
			setIsHerb(true);
		_chanceSum += item.getChance();
		item.setChanceInGroup(_chanceSum);
		_items.add(item);
	}

	/**
	 * Возвращает список вещей
	 */
	public List<RewardData> getItems()
	{
		return _items;
	}

	/**
	 * Возвращает полностью независимую копию группы
	 */
	@Override
	public RewardGroup clone()
	{
		RewardGroup ret = new RewardGroup(_chance);
		for(RewardData i : _items)
			ret.addData(i.clone());
		return ret;
	}

	/**
	 * Функция используется в основном механизме расчета дропа, выбирает одну/несколько вещей из группы, в зависимости от рейтов
	 * 
	 */
	public List<RewardItem> roll(RewardType type, Player player, double mod, boolean isRaid, boolean isSiegeGuard, boolean isChampion)
	{
		switch(type)
		{
			case NOT_RATED_GROUPED:
			case NOT_RATED_NOT_GROUPED:{
				_isRaid = false;
				return rollItems(mod, 1.0, 1.0);}
			case SWEEP:{
				_isRaid = false;
				return rollItems(mod, Config.RATE_DROP_SPOIL, player.getRateSpoil());}
			case RATED_GROUPED:
				if(_isAdena)
					return rollAdena(mod, Config.RATE_DROP_ADENA, player.getRateAdena());
				if(_isHerb){
					_isRaid = false;
					return rollItems(mod, Config.RATE_DROP_HERBS, 1.0);}
				if(isRaid){
					_isRaid = true;
					_notRate = Config.NO_RATE_RAIDBOSS;
					return rollItems(mod, Config.RATE_DROP_RAIDBOSS, 1.0);}
				if(isSiegeGuard){
					_isRaid = false;
					_notRate = Config.NO_RATE_SIEGE_GUARD;
					return rollItems(mod, Config.RATE_DROP_SIEGE_GUARD, 1.0);}
				_isRaid = false;
				return rollItems(mod, Config.RATE_DROP_ITEMS, player.getRateItems());
			default:
				return Collections.emptyList();
		}
	}

	public List<RewardItem> rollItems(double mod, double baseRate, double playerRate)
	{
		if(mod <= 0)
			return Collections.emptyList();

		double rate;
		if(_notRate)
			rate = Math.min(mod, 1.0);
		else{
			if(_isRaid)
				rate = baseRate;
			else
				rate = baseRate * playerRate * mod;}

		double mult = Math.ceil(rate);
		double reite;
		if(Config.ALT_DROP_RATE)
			reite = rate;
		else
			reite = 1.;

		List<RewardItem> ret = new ArrayList<>((int) (mult * _items.size()));
		for(long n = 0; n < mult; n++)
			if(Rnd.get(1, RewardList.MAX_CHANCE) <= _chance * Math.min(rate - n, 1.0))
				rollFinal(_items, ret, reite, Math.max(_chanceSum, RewardList.MAX_CHANCE));
		return ret;
	}

	private List<RewardItem> rollAdena(double mod, double baseRate, double playerRate)
	{
		double chance = _chance;
		if(mod > 10)
		{
			mod *= _chance / RewardList.MAX_CHANCE;
			chance = RewardList.MAX_CHANCE;
		}

		if(mod <= 0)
			return Collections.emptyList();

		if(Rnd.get(1, RewardList.MAX_CHANCE) > chance)
			return Collections.emptyList();

		double rate = baseRate * playerRate * mod;

		List<RewardItem> ret = new ArrayList<>(_items.size());
		rollFinal(_items, ret, rate, Math.max(_chanceSum, RewardList.MAX_CHANCE));
		for(RewardItem i : ret)
			i.isAdena = true;

		return ret;
	}

	private void rollFinal(List<RewardData> items, List<RewardItem> ret, double mult, double chanceSum)
	{
		// перебираем все вещи в группе и проверяем шанс
		int chance = Rnd.get(0, (int) chanceSum);
		long count;

		for(RewardData i : items)
		{
			if(chance < i.getChanceInGroup() && chance > i.getChanceInGroup() - i.getChance())
			{
			
				double imult;
				if(Config.ALT_DROP_RATE)
				{
					if(i.notRate()) 
						imult = 1.0; 
					else 
					{ 
						if(isAdena()) 
							imult = mult; 
						else 
							imult = Rnd.get(1, Math.round(mult)); 
					}
				}
				else
					imult = i.notRate() ? 1.0 : mult;
				
				if(i.getMinDrop() >= i.getMaxDrop())
				{
					count = Math.round(i.getMinDrop() * imult);
					if((double) count > imult && !isAdena() && Config.ALT_DROP_RATE) 
						count = count / (long) 2.4;
				}
				else
				{
					count = Rnd.get(Math.round(i.getMinDrop() * imult), Math.round(i.getMaxDrop() * imult));
					if((double) count > imult && !isAdena() && Config.ALT_DROP_RATE) 
						count = count / (long) 2.4;
				} 

				RewardItem t = null;

				for(RewardItem r : ret)
					if(i.getItemId() == r.itemId)
					{
						t = r;
						break;
					}

				if(t == null)
				{
					ret.add(t = new RewardItem(i.getItemId()));
					t.count = count;
				}
				else if(!i.notRate())
				{
					t.count = SafeMath.addAndLimit(t.count, count);
				}

				break;
			}
		}
	}
}