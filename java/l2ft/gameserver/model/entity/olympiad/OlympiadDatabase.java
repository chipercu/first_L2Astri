package l2ft.gameserver.model.entity.olympiad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2ft.commons.dbutils.DbUtils;
import l2ft.gameserver.Announcements;
import l2ft.gameserver.Config;
import l2ft.gameserver.dao.OlympiadNobleDAO;
import l2ft.gameserver.database.DatabaseFactory;
import l2ft.gameserver.instancemanager.ServerVariables;
import l2ft.gameserver.model.base.ClassId;
import l2ft.gameserver.network.l2.components.SystemMsg;
import l2ft.gameserver.network.l2.s2c.SystemMessage2;
import l2ft.gameserver.templates.StatsSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OlympiadDatabase
{
	private static final Logger _log = LoggerFactory.getLogger(OlympiadDatabase.class);

	public static synchronized void loadNoblesRank()
	{
		Olympiad._noblesRank = new ConcurrentHashMap<Integer, Integer>();
		Map<Integer, Integer> tmpPlace = new HashMap<Integer, Integer>();

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(OlympiadNobleDAO.GET_ALL_CLASSIFIED_NOBLESS);
			rset = statement.executeQuery();
			int place = 1;
			while(rset.next())
				tmpPlace.put(rset.getInt(Olympiad.CHAR_ID), place++);

		}
		catch(Exception e)
		{
			_log.error("Olympiad System: Error!", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		int rank1 = (int) Math.round(tmpPlace.size() * 0.01);
		int rank2 = (int) Math.round(tmpPlace.size() * 0.10);
		int rank3 = (int) Math.round(tmpPlace.size() * 0.25);
		int rank4 = (int) Math.round(tmpPlace.size() * 0.50);

		if(rank1 == 0)
		{
			rank1 = 1;
			rank2++;
			rank3++;
			rank4++;
		}

		for(int charId : tmpPlace.keySet())
			if(tmpPlace.get(charId) <= rank1)
				Olympiad._noblesRank.put(charId, 1);
			else if(tmpPlace.get(charId) <= rank2)
				Olympiad._noblesRank.put(charId, 2);
			else if(tmpPlace.get(charId) <= rank3)
				Olympiad._noblesRank.put(charId, 3);
			else if(tmpPlace.get(charId) <= rank4)
				Olympiad._noblesRank.put(charId, 4);
			else
				Olympiad._noblesRank.put(charId, 5);
	}

	/**
	 * Сбрасывает информацию о ноблесах, сохраняя очки за предыдущий период
	 */
	public static synchronized void cleanupNobles()
	{
		_log.info("Olympiad: Calculating last period...");
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(OlympiadNobleDAO.OLYMPIAD_CALCULATE_LAST_PERIOD);
			statement.setInt(1, Config.OLYMPIAD_BATTLES_FOR_REWARD);
			statement.execute();
			DbUtils.close(statement);

			statement = con.prepareStatement(OlympiadNobleDAO.OLYMPIAD_CLEANUP_NOBLES);
			statement.setInt(1, Config.OLYMPIAD_POINTS_DEFAULT);
			statement.execute();
		}
		catch(Exception e)
		{
			_log.error("Olympiad System: Couldn't calculate last period!", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}

		for(Integer nobleId : Olympiad._nobles.keySet())
		{
			StatsSet nobleInfo = Olympiad._nobles.get(nobleId);
			int points = nobleInfo.getInteger(Olympiad.POINTS);
			int compDone = nobleInfo.getInteger(Olympiad.COMP_DONE);
			nobleInfo.set(Olympiad.POINTS, Config.OLYMPIAD_POINTS_DEFAULT);
			if(compDone >= Config.OLYMPIAD_BATTLES_FOR_REWARD)
			{
				nobleInfo.set(Olympiad.POINTS_PAST, points);
				nobleInfo.set(Olympiad.POINTS_PAST_STATIC, points);
			}
			else
			{
				nobleInfo.set(Olympiad.POINTS_PAST, 0);
				nobleInfo.set(Olympiad.POINTS_PAST_STATIC, 0);
			}
			nobleInfo.set(Olympiad.COMP_DONE, 0);
			nobleInfo.set(Olympiad.COMP_WIN, 0);
			nobleInfo.set(Olympiad.COMP_LOOSE, 0);
			nobleInfo.set(Olympiad.GAME_CLASSES_COUNT, 0);
			nobleInfo.set(Olympiad.GAME_NOCLASSES_COUNT, 0);
			nobleInfo.set(Olympiad.GAME_TEAM_COUNT, 0);
		}
	}

	public static List<String> getClassLeaderBoard(int classId)
	{
		List<String> names = new ArrayList<String>();

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(classId == 132 ? OlympiadNobleDAO.GET_EACH_CLASS_LEADER_SOULHOUND : OlympiadNobleDAO.GET_EACH_CLASS_LEADER);
			statement.setInt(1, classId);
			rset = statement.executeQuery();
			while(rset.next())
				names.add(rset.getString(Olympiad.CHAR_NAME));
		}
		catch(Exception e)
		{
			_log.error("Olympiad System: Couldnt get heros from db!", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		return names;
	}

	public static synchronized void sortHerosToBe()
	{
		if(Olympiad._period != 1)
			return;

		Olympiad._heroesToBe = new ArrayList<StatsSet>();

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			StatsSet hero;

			for(ClassId id : ClassId.VALUES)
			{
				if(id.getId() == 133)
					continue;
				if(id.level() == 3)
				{
					statement = con.prepareStatement(id.getId() == 132 ? OlympiadNobleDAO.OLYMPIAD_GET_HEROS_SOULHOUND : OlympiadNobleDAO.OLYMPIAD_GET_HEROS);
					statement.setInt(1, id.getId());
					statement.setInt(2, Config.OLYMPIAD_BATTLES_FOR_REWARD);
					rset = statement.executeQuery();

					if(rset.next())
					{
						hero = new StatsSet();
						hero.set(Olympiad.CLASS_ID, id.getId());
						hero.set(Olympiad.CHAR_ID, rset.getInt(Olympiad.CHAR_ID));
						hero.set(Olympiad.CHAR_NAME, rset.getString(Olympiad.CHAR_NAME));

						Olympiad._heroesToBe.add(hero);
					}
					DbUtils.close(statement, rset);
				}
			}
		}
		catch(Exception e)
		{
			_log.error("Olympiad System: Couldnt heros from db!", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public static synchronized void saveNobleData(int nobleId)
	{
		OlympiadNobleDAO.getInstance().replace(nobleId);
	}

	public static synchronized void saveNobleData()
	{
		if(Olympiad._nobles == null)
			return;
		for(Integer nobleId : Olympiad._nobles.keySet())
			saveNobleData(nobleId);
	}

	public static synchronized void setNewOlympiadEnd()
	{
		Calendar currentTime = Calendar.getInstance();
		if (!nextEndDay(currentTime))
			return;
		currentTime.set(Calendar.HOUR_OF_DAY, 00);
		currentTime.set(Calendar.MINUTE, 00);
		Olympiad._olympiadEnd = currentTime.getTimeInMillis();

		Calendar nextChange = Calendar.getInstance();
		Olympiad._nextWeeklyChange = nextChange.getTimeInMillis() + Config.ALT_OLY_WPERIOD;

		Olympiad._isOlympiadEnd = false;
		Announcements.getInstance().announceToAll(new SystemMessage2(SystemMsg.OLYMPIAD_PERIOD_S1_HAS_STARTED).addInteger(Olympiad._currentCycle));
	}

	public static boolean nextEndDay(Calendar currentTime)
	{
		int nextDay = 0;
		for (int i : Config.ALT_OLY_DATE_END)
		{
			if ((i > currentTime.get(5)) && (i < currentTime.getMaximum(5)))
			nextDay = i;
		}
		if (nextDay == 0)
			nextDay = Config.ALT_OLY_DATE_END[0];
		if ((nextDay > 0) && (nextDay <= currentTime.getMaximum(5)))
		{
			do
				currentTime.add(5, 1);
			while (currentTime.get(5) != nextDay);

			return true;
		}

		_log.error("Olympiad not is not initialized. Bad config 'AltOlyDateEnd'.");
		return false;
	}

	public static void save()
	{
		saveNobleData();
		ServerVariables.set("Olympiad_CurrentCycle", Olympiad._currentCycle);
		ServerVariables.set("Olympiad_Period", Olympiad._period);
		ServerVariables.set("Olympiad_End", Olympiad._olympiadEnd);
		ServerVariables.set("Olympiad_ValdationEnd", Olympiad._validationEnd);
		ServerVariables.set("Olympiad_NextWeeklyChange", Olympiad._nextWeeklyChange);
	}
}