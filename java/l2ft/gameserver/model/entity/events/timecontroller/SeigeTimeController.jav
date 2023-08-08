package l2ft.gameserver.model.entity.events.timecontroller;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import l2ft.gameserver.model.entity.events.impl.CastleSiegeEvent;

public class SeigeTimeController
{
	private static SeigeTimeController _instance;
	private Future<?> _nextCastleSiegeDateSetTask = null;
	private Future<?> _nextTWSiegeDateSetTask = null;

	public static SeigeTimeController getInstance()
	{
		if(_instance == null)
			_instance = new SeigeTimeController();
		return _instance;
	}

	public SeigeTimeController(CastleSiegeEvent castleSeige, boolean generate)
	{
		if(generate)
			generateNextSiegeDates();
	}

	private class NextSiegeDateSet extends RunnableImpl
	{
		@Override
		public void runImpl() throws Exception
		{
			setNextSiegeTime();
		}
	}

	/**
	 * Ставит осадное время вручну, вызывается с пакета {@link l2ft.gameserver.network.l2.c2s.RequestSetCastleSiegeTime}
	 * @param id
	 */
	public void setNextSiegeTime(int id)
	{
		if(!_nextSiegeTimes.contains(id) || _nextSiegeDateSetTask == null)
			return;

		_nextSiegeTimes = Containers.EMPTY_INT_SET;
		_nextSiegeDateSetTask.cancel(false);
		_nextSiegeDateSetTask = null;

		setNextSiegeTime(id * 1000L);
	}

	public void setNextSiegeTime()
	{
		if(_nextSiegeDateSetTask == null)
			return;

		_nextSiegeTimes = Containers.EMPTY_INT_SET;
		_nextSiegeDateSetTask.cancel(false);
		_nextSiegeDateSetTask = null;

		setNextSiegeTime(id * 1000L);
	}

	/**
	 * Генерирует даты для следующей осады замка, и запускает таймер на автоматическую установку даты
	 */
	public void generateNextSiegeDates()
	{
		if(getResidence().getSiegeDate().getTimeInMillis() != 0)
			return;

		final Calendar calendar = (Calendar) Config.CASTLE_VALIDATION_DATE.clone();
		calendar.set(Calendar.DAY_OF_WEEK, _dayOfWeek);
		calendar.set(Calendar.HOUR_OF_DAY, _hourOfDay);
		calendar.add(Calendar.WEEK_OF_YEAR, 1);
		validateSiegeDate(calendar, 2);

		_nextSiegeTimes = new TreeIntSet();

		for(int h : Config.CASTLE_SELECT_HOURS)
		{
			calendar.set(Calendar.HOUR_OF_DAY, h);
			_nextSiegeTimes.add((int) (calendar.getTimeInMillis() / 1000L));
		}

		long diff = getResidence().getOwnDate().getTimeInMillis() + DAY_IN_MILISECONDS - System.currentTimeMillis();
		_nextCastleSiegeDateSetTask = ThreadPoolManager.getInstance().schedule(new NextSiegeDateSet(), diff);
	}
}