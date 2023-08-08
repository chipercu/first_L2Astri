package l2ft.gameserver.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.log4j.Logger;

import l2ft.gameserver.model.base.InvisibleType;
import l2ft.gameserver.Config;
import l2ft.gameserver.ThreadPoolManager;
import l2ft.gameserver.database.DatabaseFactory;
import l2ft.gameserver.ai.CtrlIntention;
import l2ft.gameserver.ai.PlayableAI.nextAction;
import l2ft.gameserver.tables.ClanTable;
import l2ft.gameserver.templates.item.CreateItem;
import l2ft.gameserver.model.items.ItemInstance;
import l2ft.gameserver.utils.ItemFunctions;
import l2ft.gameserver.model.GameObjectsStorage;
import l2ft.gameserver.utils.Location;
import l2ft.gameserver.model.Player;
import l2ft.gameserver.templates.item.WeaponTemplate.WeaponType;
import l2ft.gameserver.network.l2.s2c.MagicSkillUse;
import l2ft.commons.util.Rnd;

public class PhantomPlayers
{ 
	private static final Logger _log= Logger.getLogger(PhantomPlayers.class.getName());
	private static String _phantomAcc = Config.PHANTOM_PLAYERS_AKK;
	private static int _PhantomsCount = 0;
	private static int _PhantomsLimit = 0;
	private static int _setsCount = 0;
	private static int _setsCountClan = 0;
	private volatile int _PhantomsTownTotal = 0;
	private static int _nameColCount = 0;
	private static int _titleColCount = 0;
	private static FastList<Integer> _nameColors = new FastList<Integer>();
	private static FastList<Integer> _titleColors = new FastList<Integer>();
	private static FastList<L2Set> _sets = new FastList<L2Set>();
	private static int _setsArcherCount = 0;
	private static FastList<L2Set> _setsArcher = new FastList<L2Set>();
	private static PhantomPlayers _instance;
	private static int _setsOlyCount = 0;
	private static FastList<L2Set> _setsOly = new FastList<L2Set>();
	private static int _locsCount = 0;
	private static FastList<Location> _PhantomsTownLoc = new FastList<Location>();
	private static FastMap<Integer, L2Fantome> _phantoms = new FastMap<Integer, L2Fantome>();
	private static int _PhantomsEnchPhsCount = 0;
	private static FastList<String> _PhantomsEnchPhrases = new FastList<String>();
	private static int _PhantomsLastPhsCount = 0;
	private static FastList<String> _PhantomsLastPhrases = new FastList<String>();
	private static Map<Integer, ConcurrentLinkedQueue<Player>> _PhantomsTown = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Player>>();
	private static Map<Integer, ConcurrentLinkedQueue<Player>> _PhantomsTownClan = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Player>>();
	private static Map<Integer, ConcurrentLinkedQueue<Integer>> _PhantomsTownClanList = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Integer>>();

	public static PhantomPlayers getInstance()
	{ 
		return _instance;
	}

	public static void init()
	{
		_instance = new PhantomPlayers();
		_instance.load();
	}

	private void load()
	{
		parceArmors();
		parceArcherArmors();
		parceOlyArmors();
		parceColors();
		cacheLastPhrases();
		if(Config.ALLOW_PHANTOM_PLAYERS)
		{
			parceTownLocs();
			parceTownClans();
			//parceTownRecs();
			cacheFantoms();
			cacheEnchantPhrases();
			_PhantomsLimit = Config.PHANTOM_PLAYERS_COUNT_FIRST + Config.PHANTOM_PLAYERS_COUNT_NEXT + 10;
			_PhantomsTown.put(1, new ConcurrentLinkedQueue<Player>());
			_PhantomsTown.put(2, new ConcurrentLinkedQueue<Player>());
		}
	}

	private void cacheFantoms(){ new Thread(new Runnable()
    {
		public void run()
		{
			String name = "";
			String new_name = "";
			Connection con = null;
			Player fantom = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				con.setTransactionIsolation(1);
				PreparedStatement st = con.prepareStatement("SELECT obj_Id,char_name,title,x,y,z,clanid FROM characters WHERE account_name = ?");
				st.setString(1, _phantomAcc);
				ResultSet rs = st.executeQuery();
				rs.setFetchSize(250);
				while(rs.next())
				{
					name = rs.getString("char_name");
					_phantoms.put(Integer.valueOf(rs.getInt("obj_Id")), new L2Fantome(name, rs.getString("title"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), rs.getInt("clanid")));
				}
				st.close();
				rs.close();
				con.close();
				_log.info("PhantomPlayers: Cached " + _phantoms.size() + " players.");
			}
			catch (Exception e)
			{
				_log.warn("PhantomPlayers: could not load chars from DB: " + e);
			}
			finally
			{
				if(con != null)
					con = null;
			}
			if (!_phantoms.isEmpty())
			{
				ThreadPoolManager.getInstance().schedule(new FantomTask(1), Config.PHANTOM_PLAYERS_DELAY_FIRST * 1000);
			}
		}}).start();
	}

	private void parceArmors()
	{
		if (!_sets.isEmpty())
			_sets.clear();

		LineNumberReader lnr = null;
		BufferedReader br = null;
		FileReader fr = null;
		try
		{
			File Data = new File("./config/phantom/town_sets.ini");
			if (!Data.exists())
				return;
			fr = new FileReader(Data);
			br = new BufferedReader(fr);
			lnr = new LineNumberReader(br);
			String line;
			while ((line = lnr.readLine()) != null)
			{
				if(line.trim().length() == 0 || line.startsWith("#"))
					continue;
				String[] items = line.split(",");
				int custom = 0;
				try
				{
					custom = Integer.parseInt(items[5]);
				}
				catch (Exception e)
				{
					custom = 0;
				}
				_sets.add(new L2Set(Integer.parseInt(items[0]), Integer.parseInt(items[1]), Integer.parseInt(items[2]), Integer.parseInt(items[3]), Integer.parseInt(items[4]), Integer.parseInt(items[5]), custom));
			}
			_setsCount = _sets.size() - 1;
			_log.info("Load " + _setsCount + " phantom armor sets");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (fr != null)
					fr.close();
				if (br != null)
					br.close();
				if (lnr != null)
					lnr.close();
			}
			catch(Exception e)
			{
			}
		}
	}

	private void parceArcherArmors()
	{
		if (!_setsArcher.isEmpty())
			_setsArcher.clear();

		LineNumberReader lnr = null;
		BufferedReader br = null;
		FileReader fr = null;
		try
		{
			File Data = new File("./config/phantom/archer_sets.ini");
			if (!Data.exists())
				return;
			fr = new FileReader(Data);
			br = new BufferedReader(fr);
			lnr = new LineNumberReader(br);
			String line;
			while ((line = lnr.readLine()) != null)
			{
				if(line.trim().length() == 0 || line.startsWith("#"))
					continue;
				String[] items = line.split(",");
				int custom = 0;
				try
				{
					custom = Integer.parseInt(items[5]);
				}
				catch (Exception e)
				{
					custom = 0;
				}
				_setsArcher.add(new L2Set(Integer.parseInt(items[0]), Integer.parseInt(items[1]), Integer.parseInt(items[2]), Integer.parseInt(items[3]), Integer.parseInt(items[4]), Integer.parseInt(items[5]), custom));
			}
			_setsArcherCount = _setsArcher.size() - 1;
			_log.info("Load " + _setsArcherCount + " Aecher phantom armor sets");
		}
		catch(IOException | NumberFormatException e)
		{
		}
		finally
		{
			try
			{
				if (fr != null)
					fr.close();
				if (br != null)
					br.close();
				if (lnr != null)
					lnr.close();
			}
			catch(Exception e)
			{
			}
		}
	}

	private void parceOlyArmors()
	{
		if (!_setsOly.isEmpty())
			_setsOly.clear();

		LineNumberReader lnr = null;
		BufferedReader br = null;
		FileReader fr = null;
		try
		{
			File Data = new File("./config/phantom/oly_sets.ini");
			if (!Data.exists())
				return;
			fr = new FileReader(Data);
			br = new BufferedReader(fr);
			lnr = new LineNumberReader(br);
			String line;
			while ((line = lnr.readLine()) != null)
			{
				if(line.trim().length() == 0 || line.startsWith("#"))
					continue;
				String[] items = line.split(",");
				int custom = 0;
				try
				{
					custom = Integer.parseInt(items[6]);
				}
				catch (Exception e)
				{
					custom = 0;
				}
				_setsOly.add(new L2Set(Integer.parseInt(items[0]), Integer.parseInt(items[1]), Integer.parseInt(items[2]), Integer.parseInt(items[3]), Integer.parseInt(items[4]), Integer.parseInt(items[5]), custom));
			}
			_setsOlyCount = _setsOly.size() - 1;
			_log.info("Load " + _setsOlyCount + " phantom Only armor sets");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (fr != null)
					fr.close();
				if (br != null)
					br.close();
				if (lnr != null)
					lnr.close();
			}
			catch(Exception e)
			{
			}
		}
	}

	private void parceTownClans()
	{
		LineNumberReader lnr = null;
		BufferedReader br = null;
		FileReader fr = null;
		try
		{
			File Data = new File("./config/phantom/town_clans.ini");
				if (!Data.exists())
					return;
			fr = new FileReader(Data);
			br = new BufferedReader(fr);
			lnr = new LineNumberReader(br);
			int clanId = 0;
			String line;
			while((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0 || line.startsWith("#"))
					continue;
				String[] items = line.split(":");
				clanId = Integer.parseInt(items[0]);
				String[] pls = items[1].split(",");
				ConcurrentLinkedQueue players = new ConcurrentLinkedQueue();
				for (String plid : pls)
					players.add(Integer.valueOf(Integer.parseInt(plid)));
				_PhantomsTownClanList.put(Integer.valueOf(clanId), players);
			}
			_setsCountClan = _PhantomsTownClanList.size() - 1;
			_log.info("Load " + _setsCountClan + " phantom Clans");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (fr != null)
					fr.close();
				if (br != null)
					br.close();
				if (lnr != null)
					lnr.close();
			}
			catch (Exception e1)
			{
			}
		}
	}

	private void parceTownLocs()
	{
		_PhantomsTownLoc.clear();
		LineNumberReader lnr = null;
		BufferedReader br = null;
		FileReader fr = null;
		try
		{
			File Data = new File("./config/phantom/town_locs.ini");
			if (!Data.exists())
				return;
			fr = new FileReader(Data);
			br = new BufferedReader(fr);
			lnr = new LineNumberReader(br);
			String line;
			while ((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0 || line.startsWith("#"))
					continue;
				String[] items = line.split(",");
				_PhantomsTownLoc.add(new Location(Integer.parseInt(items[0]), Integer.parseInt(items[1]), Integer.parseInt(items[2])));
			}
			_locsCount = _PhantomsTownLoc.size() - 1;
			_log.info("Load " + _locsCount + " phantom Town Locations");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		} 
		finally
		{
			try
			{
				if (fr != null)
					fr.close();
				if (br != null)
					br.close();
				if (lnr != null)
					lnr.close();
			}
			catch (Exception e1)
			{
			}
		}
	}

	public void startWalk(Player phantom)
	{
		ThreadPoolManager.getInstance().schedule(new PhantomWalk(phantom), 15000);
	}

	private class PhantomWalk implements Runnable
	{
		Player _phantom;
		public PhantomWalk(Player phantom)
		{
			_phantom = phantom;
		}

		public void run()
		{
			if(_phantom.getAI().getIntention() != CtrlIntention.AI_INTENTION_ATTACK)
				_phantom.rndWalk();
			startWalk(_phantom);
		}
	}

	private void cacheEnchantPhrases()
	{
		_PhantomsEnchPhrases.clear();
		LineNumberReader lnr = null;
		BufferedReader br = null;
		FileReader fr = null;
		try
		{
			File Data = new File("./config/phantom/phrases_enchant.txt");
			if (!Data.exists())
				return;
			fr = new FileReader(Data);
			br = new BufferedReader(fr);
			lnr = new LineNumberReader(br);
			String line;
			while ((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0 || line.startsWith("#"))
					continue;
				_PhantomsEnchPhrases.add(line);
			}
			_PhantomsEnchPhsCount = _PhantomsEnchPhrases.size() - 1;
			_log.info("Load " + _PhantomsEnchPhsCount + " phantom Ench");
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (fr != null)
					fr.close();
				if (br != null)
					br.close();
				if (lnr != null)
					lnr.close();
			}
			catch (Exception e1)
			{
			}
		}
	}

	private void cacheLastPhrases()
	{
		_PhantomsLastPhrases.clear();
		LineNumberReader lnr = null;
		BufferedReader br = null;
		FileReader fr = null;
		try
		{
			File Data = new File("./config/phantom/phrases_last.ini");
			if (!Data.exists())
				return;
			fr = new FileReader(Data);
			br = new BufferedReader(fr);
			lnr = new LineNumberReader(br);
			String line;
			while ((line = lnr.readLine()) != null)
			{
				if (line.trim().length() == 0 || line.startsWith("#"))
					continue;
				_PhantomsLastPhrases.add(line);
			}
			_PhantomsLastPhsCount = _PhantomsLastPhrases.size() - 1;
			_log.info("Load " + _PhantomsLastPhsCount + " phantom Last");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (fr != null)
					fr.close();
				if (br != null)
					br.close();
				if (lnr != null)
					lnr.close();
			}
			catch (Exception e1)
			{
			}
		}
	}

	private void parceColors()
	{
		_nameColors = Config.PHANTOM_PLAYERS_NAME_CLOLORS;
		_titleColors = Config.PHANTOM_PLAYERS_TITLE_CLOLORS;
		_nameColCount = _nameColors.size() - 1;
		_titleColCount = _titleColors.size() - 1;
	}

	private L2Set getRandomSet()
	{
		return _sets.get(Rnd.get(_setsCount));
	}

	private L2Set getRandomArcherSet()
	{
		return _setsArcher.get(Rnd.get(_setsArcherCount));
	}

	private int getRandomPhantom()
	{
		return Rnd.get(600000000, 600006081);
	}

	private int getRandomPhantomNext()
	{
		int obj = 0;
		for (int i = 6; i > 0; i--)
		{
			obj = Rnd.get(600000000, 600006081);
			if (!_PhantomsTown.get(1).contains(obj) && !_PhantomsTown.get(2).contains(obj))
				return obj;
		}
		return getRandomPhantomNext();
	}
	private int getRandomClan()
	{
		return Rnd.get(600000000, 600006081);
	}

	private Location getRandomLoc()
	{
		Location loc = null;
		if(loc == null)
			loc = _PhantomsTownLoc.get(Rnd.get(0, _locsCount));
		return loc;
	}

	static class L2Set
	{
		public int _body;
		public int _gaiters;
		public int _gloves;
		public int _boots;
		public int _weapon;
		public int _custom;
		public int _grade;
		L2Set(int bod, int gaiter, int glove, int boot, int weapon, int grade, int custom)
		{
			_body = bod;
			_gaiters = gaiter;
			_gloves = glove;
			_boots = boot;
			_weapon = weapon;
			_grade = grade;
			_custom = custom;
		}
	}

	static class L2Fantome
	{
		public String name;
		public String title;
		public int x;
		public int y;
		public int z;
		public int clanId;
		L2Fantome(String name, String title, int x, int y, int z, int clanId)
		{
			this.name = name;
			this.title = title;
			this.x = x;
			this.y = y;
			this.z = z;
			this.clanId = clanId;
		}
	}

	public class FantomTask implements Runnable
	{
		public int _task;
		public FantomTask(int task)
		{
			_task = task;
		}
		public void run()
		{
			int count = 0;
			int count2 = 0;
			int PhantomObjId = 0;
			int PhantomObjId2 = 0;
			switch (_task)
			{
				case 1:
				{
					_log.info("PhantomPlayers: 1st wave, spawn started.");
					while(count < Config.PHANTOM_PLAYERS_COUNT_FIRST)
					{
						Player fantom = null;
						PhantomObjId = getRandomPhantomNext();
						if(!(_PhantomsTown.get(1).contains(PhantomObjId)))
						{
							fantom = loadPhantom(PhantomObjId);
							if(fantom == null)
								continue;
							_PhantomsTown.get(1).add(fantom);
							if(Config.PHANTOM_PLAYERS_SOULSHOT_ANIM && Rnd.get(100) < 45)
							{
								try
								{
									Thread.sleep(900);
								}
								catch (InterruptedException e)
								{}
								if (Rnd.get(100) < 3)
									fantom.sitDown(null);
								fantom.broadcastPacket(new MagicSkillUse(fantom, fantom, 2154, 1, 0, 0));
								try
								{
									Thread.sleep(300);
								}
								catch(InterruptedException e)
								{
								}
								fantom.broadcastPacket(new MagicSkillUse(fantom, fantom, 2164, 1, 0, 0));
							}
							try
							{
								Thread.sleep(Config.PHANTOM_PLAYERS_DELAY_SPAWN_FIRST);
							}
							catch (InterruptedException e)
							{
							}
							count++;
						}
					}
					_log.info("FPhantomPlayers: 1st wave, spawned " + count + " players.");
					//Online.getInstance().checkMaxOnline();
					ThreadPoolManager.getInstance().schedule(new FantomTaskDespawn(1), Config.PHANTOM_PLAYERS_DESPAWN_FIRST);
					ThreadPoolManager.getInstance().schedule(new FantomTask(2), Config.PHANTOM_PLAYERS_DELAY_NEXT);
					ThreadPoolManager.getInstance().schedule(new Social(), 12000L);
					ThreadPoolManager.getInstance().schedule(new CheckCount(), 300000L);
					break;
				}
				case 2:
				{
					_log.info("PhantomPlayers: 2nd wave, spawn started.");
					while (count2 < Config.PHANTOM_PLAYERS_COUNT_NEXT)
					{
						Player fantom2 = null;
						PhantomObjId2 = getRandomPhantomNext();
						if(!(_PhantomsTown.get(1).contains(PhantomObjId2)) || !(_PhantomsTown.get(2).contains(PhantomObjId2)))
						{
							fantom2 = loadPhantom(PhantomObjId2);
							if(fantom2 == null)
								continue;
							_PhantomsTown.get(2).add(fantom2);
							if(Config.PHANTOM_PLAYERS_SOULSHOT_ANIM && Rnd.get(100) < 45)
							{
								try
								{
									Thread.sleep(900L);
								}
								catch (InterruptedException e) {
								}
								if (Rnd.get(100) < 3)
									fantom2.sitDown(null);
								fantom2.broadcastPacket(new MagicSkillUse(fantom2, fantom2, 2154, 1, 0, 0));
								try
								{
									Thread.sleep(300L);
								}
								catch (InterruptedException e) {
								}
								fantom2.broadcastPacket(new MagicSkillUse(fantom2, fantom2, 2164, 1, 0, 0));
							}
							try
							{
								Thread.sleep(Config.PHANTOM_PLAYERS_DELAY_SPAWN_NEXT);
							}
							catch (InterruptedException e) {
							}
							count2++;
						}
					}
					_log.info("PhantomPlayers: 2nd wave, spawned " + count2 + " players.");
					//Online.getInstance().checkMaxOnline();
					ThreadPoolManager.getInstance().schedule(new FantomTaskDespawn(2), Config.PHANTOM_PLAYERS_DESPAWN_NEXT);
				}
			}
		}
	}

	public class FantomTaskDespawn implements Runnable
	{
		public int _task;
		public FantomTaskDespawn(int task)
		{
			_task = task;
		}
		public void run()
		{
			Location loc = null;
			Player next = null;
			ConcurrentLinkedQueue<Player> players = _PhantomsTown.get(_task);
			for (Player fantom : players)
			{
				if (fantom == null)
					continue;
				loc = fantom.getPhantomLoc();
				//new Disconnection(fantom).defaultSequence(false);
				fantom.setOnlineStatus(false);
				_PhantomsTown.get(_task).remove(fantom);
				try
				{
					Thread.sleep(_task == 1 ? Config.PHANTOM_PLAYERS_DELAY_DESPAWN_FIRST : Config.PHANTOM_PLAYERS_DELAY_DESPAWN_NEXT);
				}
				catch(InterruptedException e){
				}
				if(_PhantomsTownTotal > _PhantomsLimit)
					continue;
				int nextObjId = getRandomPhantomNext();
				if(!(_PhantomsTown.get(_task).contains(nextObjId)))
				{
					next = loadPhantom(nextObjId);
					if(next == null)
						continue;
					_PhantomsTown.get(_task).add(next);
					if(Config.PHANTOM_PLAYERS_SOULSHOT_ANIM && Rnd.get(100) < 45)
					{
						try
						{
							Thread.sleep(900L);
						}
						catch (InterruptedException e) {
						}
						if (Rnd.get(100) < 3)
							next.sitDown(null);
						next.broadcastPacket(new MagicSkillUse(next, next, 2154, 1, 0, 0));
						try
						{
							Thread.sleep(300L);
						}
						catch (InterruptedException e) {
						}
						next.broadcastPacket(new MagicSkillUse(next, next, 2164, 1, 0, 0));
					}
					try
					{
						Thread.sleep(100L);
					}
					catch (InterruptedException e)
					{}
				}
			}
			loc = null;
			next = null;
			ThreadPoolManager.getInstance().schedule(new FantomTaskDespawn(1), _task == 1 ? Config.PHANTOM_PLAYERS_DESPAWN_FIRST : Config.PHANTOM_PLAYERS_DESPAWN_NEXT);
		}
	}

	public class CheckCount implements Runnable
	{
		public CheckCount()
		{
		}
        @Override
		public void run()
		{
			for (Map.Entry<Integer, ConcurrentLinkedQueue<Player>> entry : _PhantomsTown.entrySet())
			{
				Integer wave = entry.getKey();
				ConcurrentLinkedQueue<Player> players = entry.getValue();
				if (wave == null || players == null || players.isEmpty())
					continue;
				int limit = wave.intValue() == 1 ? Config.PHANTOM_PLAYERS_COUNT_FIRST : Config.PHANTOM_PLAYERS_COUNT_NEXT;
				int overflow = players.size() - limit;
				if (overflow < 1)
					continue;
				for (Player fantom : players)
				{
					//new Disconnection(fantom).defaultSequence(false);
					fantom.setOnlineStatus(false);
					_PhantomsTown.get(wave).remove(fantom);
					overflow--;
					if (overflow == 0)
						break;
				}
			}
			ThreadPoolManager.getInstance().schedule(new CheckCount(), 300000L);
		}
	}

	public class Social implements Runnable
	{
		public Social()
		{
		}

        @Override
		public void run()
		{
			TextBuilder tb = new TextBuilder();
			for(Map.Entry<Integer, ConcurrentLinkedQueue<Player>> entry : _PhantomsTown.entrySet())
			{
				Integer wave = entry.getKey();
				ConcurrentLinkedQueue<Player> players = entry.getValue();
				if (wave == null || players == null || players.isEmpty())
					continue;
				int count = 0;
				for (Player player : players)
				{
					if (Rnd.get(100) < 65)
					{
						switch (Rnd.get(2))
						{
							case 0:
							case 1:
								ItemInstance wpn = player.getActiveWeaponInstance();
								int enhchant = wpn.getEnchantLevel();
								int nextench = enhchant + 1;
								if(Rnd.get(100) < 45 && enhchant <= Config.ENCHANT_MAX_WEAPON)
									wpn.setEnchantLevel(nextench);
								else if(Rnd.get(100) < 70)
								{
									wpn.setEnchantLevel(3);
									if(nextench > 13 && Rnd.get(100) < 2)
									{
										tb.append("!");
										for(int i = Rnd.get(2, 13); i > 0; i--)
											tb.append("!");
										//player.sayString(getRandomEnchantPhrase() + tb.toString(), 1);
										tb.clear();
									}
								}
								//player.sendItems(true);
								player.broadcastUserInfo(true);
								break;
							case 2:
								if (Rnd.get(100) >= 5)
									break;
								player.moveToLocation(player.getX() + Rnd.get(30), player.getY() + Rnd.get(30), player.getZ(), 40, true);
								player.getAI().setNextAction(nextAction.INTERACT, null, null, false, false);
						}
						try
						{
							Thread.sleep(Rnd.get(500, 1500));
						}
						catch (InterruptedException e)
						{
						}
						count++;
					}
					if (count > 55)
						break;
				}
			}
			tb.clear();
			tb = null;
			ThreadPoolManager.getInstance().schedule(new Social(), 12000L);
		}
	}

	private Player loadPhantom(int objId)
	{
		int nbPlayerIG = GameObjectsStorage.getAllPlayersCount();
		if(nbPlayerIG < Config.MAXIMUM_ONLINE_USERS)
		{
			L2Fantome phantom = _phantoms.get(objId);
			if(phantom == null)
				return null;
			//Collection<Player> allPlayers = GameObjectsStorage.getAllPlayers();
			//Player[] players = allPlayers.toArray(new Player[allPlayers.size()]);		
			//for (int i = 0; i < players.length; i++)
			//{
			//	if(objId == players[i].getObjectId())
			//		return null;
			//}
			L2Set set = getRandomSet();
			ItemInstance body = null;
			ItemInstance gaiters = null;
			ItemInstance gloves = null;
			ItemInstance boots = null;
			ItemInstance weapon = null;
			if(set._body != 0)
				body = ItemFunctions.createItem(set._body);
			if(set._gaiters != 0)
				gaiters = ItemFunctions.createItem(set._gaiters);
			if(set._gloves != 0)
				gloves = ItemFunctions.createItem(set._gloves);
			if(set._boots != 0)
				boots = ItemFunctions.createItem(set._boots);
			if(set._weapon != 0)
				weapon = ItemFunctions.createItem(set._weapon);
			ItemInstance custom = null;
			int grade = set._grade;
			int setLevel = 1;
			int classId = 0;
			if(grade == 0)
				setLevel = Rnd.get(1, 19);
			if(grade == 1)
				setLevel = Rnd.get(20, 39);
			if(grade == 2)
				setLevel = Rnd.get(40, 51);
			if(grade == 3)
				setLevel = Rnd.get(52, 60);
			if(grade == 4)
				setLevel = Rnd.get(61, 75);
			if(grade == 5)
				setLevel = Rnd.get(76, 80);
			Player fantom = Player.restorePhantom(objId, setLevel, classId, false);
			//Player fantom = Player.restore(268481801);
			fantom.setOfflineMode(false);
			fantom.setIsOnline(true);
			fantom.updateOnlineStatus();
			//fantom.setNameColor(getNameColor());
			//fantom.setTitleColor(getTitleColor());
			//fantom.setOfflineMode(false);
			Location loc = getRandomLoc();
			fantom.setPhantomLoc(loc.getX(), loc.getY(), loc.getZ());
			fantom.setXYZ(loc.getX()+ Rnd.get(60), loc.getY() + Rnd.get(60), loc.getZ());
			Location loc1 = new Location(loc.getX()+ Rnd.get(350), loc.getY() + Rnd.get(350), loc.getZ());
			fantom.setOnlineStatus(true);
			fantom.setInvisibleType(InvisibleType.NONE);

			fantom.setNonAggroTime(Long.MAX_VALUE);
			fantom.spawnMe(loc1);
			//if (Rnd.get(100) < 40)
			//	fantom.setClan(ClanTable.getInstance().getClan(getRandomClan()));

			if(body != null)
			{
				fantom.getInventory().addItem(body);
				fantom.getInventory().equipItem(body);
			}
			if(gaiters != null)
			{
				fantom.getInventory().addItem(gaiters);
				fantom.getInventory().equipItem(gaiters);
			}
			if(gloves != null)
			{
				fantom.getInventory().addItem(gloves);
				fantom.getInventory().equipItem(gloves);
			}
			if(boots != null)
			{
				fantom.getInventory().addItem(boots);
				fantom.getInventory().equipItem(boots);
			}

			//int[] classIdArcher = {92, 102, 109};

			//if (set._custom > 0)
			//{
			//	custom = ItemFunctions.createItem(set._custom);
			//	fantom.getInventory().addItem(custom);
			//	fantom.getInventory().equipItem(custom);
			//}

			if(weapon != null)
				weapon.setEnchantLevel(Rnd.get(Config.PHANTOM_PLAYERS_ENCHANT_MIN, Config.PHANTOM_PLAYERS_ENCHANT_MAX));
			//if(Rnd.get(100) < 30)
			//	weapon.setAugmentation(new L2Augmentation(1067847165, 3250, 1));
			//if(weapon.getItemType() == WeaponType.BOW && (fantom.getClassId().getId() != 92 || fantom.getClassId().getId() != 102 || fantom.getClassId().getId() != 109))
			//{
			//	fantom.setClassId(classIdArcher[Rnd.get(classIdArcher.length)], true, false);
				//fantom.initAggro(true, true);
			//}
			if(weapon != null)
			{
				fantom.getInventory().addItem(weapon);
				fantom.getInventory().equipItem(weapon);
			}

			//fantom.broadcastUserInfo(true);
			fantom.broadcastCharInfo();
			fantom.rndWalk();
			//fantom.setOnlineStatusPhantom(true);
			//RegionBBSManager.getInstance().changeCommunityBoard();
			//startWalk(fantom);
			return fantom;
		}
		return null;
	}

	private String getRandomEnchantPhrase()
	{
		return _PhantomsEnchPhrases.get(Rnd.get(_PhantomsEnchPhsCount));
	}

	public String getRandomLastPhrase()
	{
		return _PhantomsLastPhrases.get(Rnd.get(_PhantomsLastPhsCount));
	}

	private int getNameColor()
	{
		return (_nameColors.get(Rnd.get(_nameColCount))).intValue();
	}

	private int getTitleColor()
	{
		return (_titleColors.get(Rnd.get(_titleColCount))).intValue();
	}
}