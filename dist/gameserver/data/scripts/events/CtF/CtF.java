package events.CtF;

import java.util.ArrayList;
import java.util.List;

import javolution.util.FastMap;

import l2ft.gameserver.Announcements;
import l2ft.gameserver.ai.CtrlEvent;
import l2ft.gameserver.data.xml.holder.InstantZoneHolder;
import l2ft.gameserver.data.xml.holder.ItemHolder;
import l2ft.gameserver.ThreadPoolManager;
import l2ft.gameserver.instancemanager.ReflectionManager;
import l2ft.gameserver.listener.actor.player.OnPlayerExitListener;
import l2ft.gameserver.model.base.ClassId;
import l2ft.gameserver.model.Creature;
import l2ft.gameserver.model.Effect;
import l2ft.gameserver.model.GameObject;
import l2ft.gameserver.model.GameObjectsStorage;
import l2ft.gameserver.model.Player;
import l2ft.gameserver.model.Skill;
import l2ft.gameserver.model.Summon;
import l2ft.gameserver.model.Territory;
import l2ft.gameserver.model.entity.Hero;
import l2ft.gameserver.model.entity.Reflection;
import l2ft.gameserver.model.entity.olympiad.Olympiad;
import l2ft.gameserver.model.entity.events.GameEvent;
import l2ft.gameserver.model.entity.events.GameEventManager;
import l2ft.gameserver.model.instances.NpcInstance;
import l2ft.gameserver.model.items.ItemInstance;
import l2ft.gameserver.network.l2.components.ChatType;
import l2ft.gameserver.network.l2.components.CustomMessage;
import l2ft.gameserver.network.l2.s2c.ChangeWaitType;
import l2ft.gameserver.network.l2.s2c.ExShowScreenMessage;
import l2ft.gameserver.network.l2.s2c.Revive;
import l2ft.gameserver.network.l2.s2c.SkillList;
import l2ft.gameserver.scripts.ScriptFile;
import l2ft.gameserver.skills.effects.EffectTemplate;
import l2ft.gameserver.stats.Env;
import l2ft.gameserver.tables.SkillTable;
import l2ft.gameserver.templates.InstantZone;
import l2ft.gameserver.utils.Location;
import l2ft.gameserver.utils.ItemFunctions;
import l2ft.gameserver.utils.ReflectionUtils;
import l2ft.gameserver.utils.Strings;
import l2ft.gameserver.utils.GArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.ArrayUtils;

public class CtF extends GameEvent implements ScriptFile, OnPlayerExitListener
{
	private static final Logger _log = LoggerFactory.getLogger(CtF.class);

	private int _state = 0;
	private static CtF _instance;
	private FastMap<Player, Integer> _participants = new FastMap<Player, Integer>();
	private FastMap<Player, List<Effect>> returnBuffs = new FastMap<Player, List<Effect>>();
	private List<Effect> _effects;
	private int[] _score;
	public long startBattle = 0L;
	private Reflection _ref;
	private InstantZone _instantZone;
	private NpcInstance redFlag = null;
	private NpcInstance blueFlag = null;

	public CtF()
	{
		_instance = this;
	}

	public static CtF getInstance()
	{
		if (_instance == null)
			_instance = new CtF();
		return _instance;
	}

	@Override
	public int getState()
	{
		return _state;
	}

	@Override
	public String getName()
	{
		return "Capture The Flag";
	}

	public long getNextTime()
	{
		long next_time = getConfigs().START_TIME;

		while (next_time <= System.currentTimeMillis() / 1000)
		{
			getConfigs().START_TIME += 86400;
			setNextEvent();
			next_time = getConfigs().START_TIME;
		}

		return next_time;
	}

	public void setNextEvent()
	{
		if (CTFConfig._configs != null && CTFConfig._configs.size() > 1)
			CTFConfig._configs.sort();
	}

	public Configs getConfigs()
	{
		return CTFConfig._configs != null ? CTFConfig._configs.get(0) : null;
	}

	@Override
	public boolean canUseItem(Player actor, ItemInstance item)
	{
		if(_state == 2)
		{
			if((item.isHeroWeapon() && !getConfigs().ALLOW_HERO_WEAPONS) || ArrayUtils.contains(getConfigs().getRestictId(), item.getItemId()))
			{
				actor.sendMessage(actor.isLangRus() ? "Запрещено использовать во время ивентов." : "You may not use during the events.");
				return false;
			}
		}
		return true;
	}

	public void onLoad()
	{
		CTFConfig.load();
		GameEventManager.getInstance().registerEvent(getInstance());
		_log.info("Loaded Event: CTF");
		_state = 0;
	}

	public void onReload()
	{
		if (_ref != null)
			_ref.clearReflection(1, false);
		if (CTFConfig._configs.size() > 0)
			CTFConfig._configs.clear();
		if (_state != 0)
			finish();
		onLoad();
	}

	public void onShutdown()
	{
		_state = 0;
	}

	public boolean register(Player player)
	{
		if(!canRegister(player, true))
		{	
			return false;
		}

		player.setPvPTeam(TeamWithMinPlayers());
		_participants.put(player, Integer.valueOf(0));
		player.sendMessage(new CustomMessage("scripts.events.CtF.CtF.YouRegistred", player, new Object[0]));
		player._event = this;
		return true;
	}

	public void addPlayer()
	{
		registerPlayer();
	}

	public void registerPlayer()
	{
		Player player = getSelf();
		GameEvent event = GameEventManager.getInstance().findEvent("Capture The Flag");
		event.register(player);
	}
	
	public void unreg(Player player)
	{
		if (player == null)
			return;

		if (_state == 2 || !isParticipant(player))
		{
			player.sendMessage(new CustomMessage("scripts.events.CtF.CtF.YouCancelRegistration", player, new Object[0]));
			player.setPvPTeam(0);
			player.allowPvPTeam();
			player._event = null;
			return;
		}

		_participants.remove(player);
		player.setPvPTeam(0);
		player.allowPvPTeam();
		player._event = null;
		player.sendMessage(new CustomMessage("scripts.events.CtF.CtF.YouRegistrationCanceled", player, new Object[0]));
	}

	public void remove(Player player)
	{
		if (player == null)
			return;
		if (_participants.containsKey(player)) 
			_participants.remove(player);

		player.setPvPTeam(0);
		player.allowPvPTeam();
		player._event = null;
		player.sendMessage(new CustomMessage("scripts.events.CtF.CtF.YouDisqualified", player, new Object[0]));
	}

	public boolean canRegister(Player player, boolean first)
	{
		if(getConfigs().ALLOW_TAKE_ITEM)
		{
			long take_item_count = getItemCount(player, getConfigs().TAKE_ITEM_ID);
			String name_take_items = ItemHolder.getInstance().getTemplate(getConfigs().TAKE_ITEM_ID).getName();
			if(take_item_count > 0)
			{
				if((int)take_item_count < getConfigs().TAKE_COUNT)
				{
					player.sendMessage("Недостаточно" + name_take_items + "для участия.");
					return false;
				}
			}
			else
			{
				player.sendMessage("У Вас нет " + name_take_items + ", требуется для участия.");
				return false;
			}
		}
		if(first && _state != 1)
		{
			player.sendMessage("Процесс регистрации не активен.");
			return false;
		}
		if(first && isParticipant(player))
		{
			player.sendMessage("Вы уже являетесь участником этого эвента.");
			return false;
		}
		if(player.isMounted())
		{
			player.sendMessage("Отзовите питомца.");
			return false;
		}
		if(player.isInDuel())
		{
			player.sendMessage("Вы должны завершить дуель.");
			return false;
		}
		if(player.getLevel() < getConfigs().MIN_LEVEL || player.getLevel() > getConfigs().MAX_LEVEL)
		{
			player.sendMessage("Вы не подходите для участия в эвенте с таким уровнем.");
			return false;
		}
		if(first && player._event != null)
		{
			player.sendMessage("Вы уже зарегестрированы на другом эвенте.");
			return false;
		}
		if(first && (player.isInOlympiadMode() || Olympiad.isRegistered(player)))
		{
			player.sendMessage("Вы уже зарегестрированы на Олимпиаде.");
			return false;
		}
		if(player.isInParty() && player.getParty().isInDimensionalRift())
		{
			player.sendMessage("Вы уже зарегестрированы на другом эвенте.");
			return false;
		}
		if(player.isTeleporting())
		{
			player.sendMessage("Вы находитесь в процессе телепортации.");
			return false;
		}
		if(first && _participants.size() >= getConfigs().MAX_PARTICIPANTS)
		{
			player.sendMessage("Достигнуто максимальное кол-во участников.");
			return false;
		}
		if(player.isCursedWeaponEquipped())
		{
			player.sendMessage("С проклятым оружием на эвент нельзя.");
			return false;
		}
		if(player.getKarma() > 0)
		{
			player.sendMessage("PK не может учавствовать в эвенте.");
			return false;
		}
		return true;
	}

	public boolean canTeleport(Player player)
	{
		if(player.isCursedWeaponEquipped())
		{
			player.sendMessage("С проклятым оружием на эвент нельзя.");
			return false;
		}
		if(player.getKarma() > 0)
		{
			player.sendMessage("PK не может учавствовать в эвенте.");
			return false;
		}
		return true;
	}

	public int getCountPlayers()
	{
		return _participants.size();
	}

	public void canRegisters()
	{
		if(_participants != null)
			for(Player player : _participants.keySet())
				if(!canRegister(player, false))
					player.sendMessage("Если все условия не будут соблюдены - вы будите дисквалифицированы");
	}	

	public boolean isParticipant(Player player)
	{
		return _participants.containsKey(player);
	}

	public int TeamWithMinPlayers()
	{
		int[] count = new int[getConfigs().TEAM_COUNTS + 1];

		for(Player player : _participants.keySet())
		{
			count[player.getPvPTeam()] += 1;
		}
		int min = count[1];

		for(int i = 1; i < count.length; i++)
		{
			min = Math.min(min, count[i]);
		}
		for(int i = 1; i < count.length; i++)
		{
			if (count[i] != min) continue; min = i;
		}
		return min;
	}

	public void sayToAll(String adress, String[] replacements, boolean all)
	{
		if(all)
		{
			Announcements.getInstance().announceByCustomMessage(adress, replacements);
		}
		else
			for(Player player : _participants.keySet())
				Announcements.getInstance().announceToPlayerByCustomMessage(player, adress, replacements, ChatType.CRITICAL_ANNOUNCE);
	}

	public void question()
	{
		for(Player player : GameObjectsStorage.getAllPlayersForIterate())
		{
			if(player != null && ((player.getLevel() >= getConfigs().MIN_LEVEL && player.getLevel() <= getConfigs().MAX_LEVEL) || player.getReflection().getId() <= 0 || !player.isInOlympiadMode() || !Olympiad.isRegistered(player) || !player.isInOfflineMode()))
			{
				player.scriptRequest(new CustomMessage("scripts.events.CtF.CtF.AskPlayer", player).toString(), "events.CtF.CtF:registerPlayer", new Object[0]);
			}
		}
	}
	
	public void startRegistration()
	{
		_state = 1;
		sayToAll("scripts.events.CtF.CtF.AnnounceRegistrationStarted", new String[] { getName() , "" + getConfigs().MIN_LEVEL, "" + getConfigs().MAX_LEVEL}, true);
		question();		
		
		_score = new int[getConfigs().TEAM_COUNTS];

		if(getConfigs().TIME_TO_START_BATTLE >= 30)
			ThreadPoolManager.getInstance().schedule(new StartMessages("scripts.events.CtF.CtF.EventStartOver", new String[] { "30" }), (getConfigs().TIME_TO_START_BATTLE - 30) * 1000);
		if(getConfigs().TIME_TO_START_BATTLE >= 10)
			ThreadPoolManager.getInstance().schedule(new StartMessages("scripts.events.CtF.CtF.EventStartOver", new String[] { "10" }), (getConfigs().TIME_TO_START_BATTLE - 10) * 1000);
		for(int i = 5; i >= 1; i--)
		{
			if (getConfigs().TIME_TO_START_BATTLE - i >= i)
				ThreadPoolManager.getInstance().schedule(new StartMessages("scripts.events.CtF.CtF.EventStartOver", new String[] { Integer.toString(i) }), (getConfigs().TIME_TO_START_BATTLE - i) * 1000);
		}
		ThreadPoolManager.getInstance().schedule(new TaskVoid("canRegisters", null), (getConfigs().TIME_TO_START_BATTLE - 10) * 1000);
		ThreadPoolManager.getInstance().schedule(new TaskVoid("start", null), getConfigs().TIME_TO_START_BATTLE * 1000);
	}

	private void initRef()
	{
		_ref = new Reflection();
		_instantZone = InstantZoneHolder.getInstance().getInstantZone(602);
		_ref.init(_instantZone);
	}

	public void start()
	{
		initRef();

		if(_state == 0)
		{
			startRegistration();
		}
		else if(_state == 1)
		{
			if(getCountPlayers() >= getConfigs().MIN_PARTICIPANTS)
			{
				ReflectionUtils.getDoor(24190002).closeMe();
				ReflectionUtils.getDoor(24190003).closeMe();
				//checkPlayers();
				ThreadPoolManager.getInstance().schedule(new go(), getConfigs().PAUSE_TIME * 1000);
				sayToAll("scripts.events.CtF.CtF.AnnounceTeleportToColiseum", new String[0], true);
				_state = 2;
				teleportPlayersToColiseum();

				if(redFlag != null)
					redFlag.deleteMe();
				if(blueFlag != null)
					blueFlag.deleteMe();
				blueFlag = spawn(getConfigs().FLAG_COORDS.get(0), 35426, _ref);
				redFlag = spawn(getConfigs().FLAG_COORDS.get(1), 35423, _ref);
			}
			else
			{
				sayToAll("scripts.events.CtF.CtF.AnnounceEventCanceled", new String[] { getName() }, true);
				for(Player player : _participants.keySet())
				{
					player.setPvPTeam(0);
					player.allowPvPTeam();
					player._event = null;
				}
				_participants.clear();
				_state = 0;
				abort();
			}
		}
		else
		{
			sayToAll("scripts.events.CtF.CtF.AnnounceStartError", new String[0], true);
		}
	}

	public void finish()
	{
		sayToAll("scripts.events.CtF.CtF.AnnounceEventEnd", new String[0], false);

		if (_state == 2)
		{
			int WinTeam = -1;
			int max = 0;
			int count = 0;

			for(int i = 0; i < _score.length - 1; i++)
			{
				max = Math.max(_score[i], _score[(i + 1)]);
			}
			for (int i = 0; i < _score.length; i++)
			{
				if (_score[i] != max)
					continue;
				WinTeam = i;
				count++;
			}

			if(count != 1 || WinTeam == -1 || _score[WinTeam] == 0)
			{
				sayToAll("scripts.events.CtF.CtF.EventDraw", new String[0], false);
			}
			else
			{
				rewardToWinTeam(WinTeam);
			}

			ThreadPoolManager.getInstance().schedule(new TaskVoid("restorePlayers", null), 1000);
			ThreadPoolManager.getInstance().schedule(new TaskVoid("teleportPlayersToSavedCoords", null), 3000);
		}
		ReflectionUtils.getDoor(24190002).openMe();
		ReflectionUtils.getDoor(24190003).openMe();
		ThreadPoolManager.getInstance().schedule(new TaskVoid("clearAll", null), 3500);
		GameEventManager.getInstance().nextEvent();
		_state = 0;
	}

	public void abort()
	{
		finish();
		if (_state > 0)
			sayToAll("scripts.events.CtF.CtF.EventCompletedManually", new String[] { getName() }, true);
	}

	public void onLogout(Player player)
  	{
		if (player == null || player.getPvPTeam() < 1)
		{
			return;
		}

		if (_state == 1 && _participants.containsKey(player))
		{
			unreg(player);
			player.setPvPTeam(0);
			player.allowPvPTeam();
			player._event = null;
			return;
		}

		if (_state == 2 && _participants.containsKey(player))
		{
			try
			{
				if(player != null)
					player.teleToLocation(player._stablePoint, ReflectionManager.DEFAULT);
				remove(player);
				player.setPvPTeam(0);
				player.allowPvPTeam();
				player._event = null;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
  	}

	public void teleportPlayersToSavedCoords()
	{
		for (Player player : _participants.keySet())
		{
			teleportPlayerToSavedCoords(player);
		}
	}

	public void teleportPlayerToSavedCoords(Player player)
	{
		try
		{
			if(player == null)
				return;
			if(player._stablePoint == null) // игрока не портнуло на стадион
				return;

			player.getEffectList().stopAllEffects();

			ItemInstance[] arr = player.getInventory().getItems();
			int len = arr.length;
			for (int i = 0; i < len; i++)
			{
				ItemInstance _item = arr[i];
				if(_item.getItemId() == 13560 || _item.getItemId() == 13561)
				{
					player.unsetVar("CtF_Flag");
					player.getInventory().destroyItem(_item, 1);
					player.broadcastUserInfo(true);
				}
			}

			player.unsetVar("CtF_Flag");
			player.setPvPTeam(0);
			player.allowPvPTeam();
			removeFlag(player);
			player._event = null;
			if(player.getPet() != null)
			{
				Summon summon = player.getPet();
				summon.unSummon();
			}
			if (getConfigs().STOP_ALL_EFFECTS)
				ThreadPoolManager.getInstance().schedule(new TaskVoid("returnBuffsToPlayers", player), 500L);
			player.removeInstanceReuse(_instantZone.getId());
			player.teleToLocation(player._stablePoint, ReflectionManager.DEFAULT);
			player._stablePoint = null;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void doDie(Creature killer, Creature self)
	{
		if (self == null || killer == null)
			return;
		if ((self instanceof Player) && (killer instanceof Player))
		{
			Player player = (Player)self;
			Player kill = (Player)killer;
			if (_participants.containsKey(player))
				_participants.put(player, _participants.get(player) + 1);
			if(getConfigs().ALLOW_KILL_BONUS && _participants.containsKey(kill))
				addItem(kill, getConfigs().KILL_BONUS_ID, getConfigs().KILL_BONUS_COUNT);
			if (_state == 2 && player.getPvPTeam() > 0 && kill.getPvPTeam() > 0 && _participants.containsKey(player) && _participants.containsKey(kill))
			{
				player.setFakeDeath(true);
				player.getAI().notifyEvent(CtrlEvent.EVT_FAKE_DEATH, null, null);
				player.broadcastPacket(new ChangeWaitType(player, ChangeWaitType.WT_START_FAKEDEATH));
				player.broadcastCharInfo();
				player.abortCast(true, false);
				player.abortAttack(true, false);
				player.sendMessage(new CustomMessage("scripts.events.CtF.CtF.YouDead", player, new Object[0]).add(new Object[] { getConfigs().RESURRECTION_TIME }));
				ThreadPoolManager.getInstance().schedule(new TaskVoid("ResurrectionPlayer", player), getConfigs().RESURRECTION_TIME * 1000);
			}
			player.unsetVar("CtF_Flag");
			dropFlag(player, true);
		}
	}

	public void teleportPlayersToColiseum()
	{
		for(Player player : _participants.keySet())
		{
			if (!canRegister(player, false))
			{
				remove(player);
				continue;
			}
			if(getConfigs().ALLOW_TAKE_ITEM)
				removeItem(player, getConfigs().TAKE_ITEM_ID, (long)getConfigs().TAKE_COUNT);
			ItemInstance wpn = player.getActiveWeaponInstance();
			if(wpn != null && wpn.isHeroWeapon() && !getConfigs().ALLOW_HERO_WEAPONS)
			{
				player.getInventory().unEquipItem(wpn);
				player.abortAttack(true, true);
			}
			unRide(player);
			if(getConfigs().STOP_ALL_EFFECTS)
				removeBuff(player);
			if(player.getParty() != null)
				player.leaveParty();
			player.allowPvPTeam();
			player._stablePoint = player._stablePoint == null ? player.getReflection().getReturnLoc() == null ? player.getLoc() : player.getReflection().getReturnLoc() : player._stablePoint;
			Reflection ref = _ref;
			InstantZone instantZone = ref.getInstancedZone();
			Location tele = Location.findPointToStay(instantZone.getTeleportCoords().get(player.getPvPTeam() - 1), 150, 200, ref.getGeoIndex());
			player.teleToLocation(tele, ref);
			restorePlayer(player);
			player.sendPacket(new ExShowScreenMessage("Через несколько секунд бой начнется!", getConfigs().PAUSE_TIME * 700, ExShowScreenMessage.ScreenMessageAlign.MIDDLE_CENTER, true));
		}
		paralyzePlayers();
	}

	public void removeBuff(Player player)
	{
		if(player != null)
		{
			List<Effect> effectList = player.getEffectList().getAllEffects();
			_effects = new ArrayList<Effect>(effectList.size());

			if (player.isCastingNow())
			{
				player.abortCast(true, true);
			}
			for(Effect $effect : effectList)
			{
				Effect effect = $effect.getTemplate().getEffect(new Env($effect.getEffector(), $effect.getEffected(), $effect.getSkill()));
				effect.setCount($effect.getCount());
				effect.setPeriod($effect.getCount() == 1 ? $effect.getPeriod() - $effect.getTime() : $effect.getPeriod());
				_effects.add(effect);
			}
			if(player.getPet() != null)
			{
				Summon summon = player.getPet();
				summon.unSummon();
			}
			returnBuffs.put(player, _effects);
			player.getEffectList().stopAllEffects();
		}
	}

	public void returnBuffsToPlayers(Player player)
	{
		for(Effect e : returnBuffs.get(player))
			player.getEffectList().addEffect(e);
	}

	public void paralyzePlayers()
	{
		Skill revengeSkill = SkillTable.getInstance().getInfo(4515, 1);
		for (Player player : _participants.keySet())
		{
			player.getEffectList().stopEffect(1411);
			revengeSkill.getEffects(player, player, false, false);
			if (player.getPet() != null)
				revengeSkill.getEffects(player, player.getPet(), false, false);
		}
	}

	public void unParalyzePlayers()
	{
		for (Player player : _participants.keySet())
		{
			player.getEffectList().stopEffect(4515);
			if (player.getPet() != null)
				player.getPet().getEffectList().stopEffect(4515);
			if(player.isInParty())
				player.leaveParty();
		}
	}

	public void restorePlayer(Player player)
	{
		ClassId nclassId = ClassId.VALUES[player.getClassId().getId()];
		if(player.isFakeDeath())
		{
			player.setFakeDeath(false);
			player.broadcastPacket(new ChangeWaitType(player, ChangeWaitType.WT_STOP_FAKEDEATH));
			player.broadcastPacket(new Revive(player));
			player.broadcastCharInfo();
		}
		if(nclassId.isMage())
			playerBuff(player, getConfigs().LIST_MAGE_MAG_SUPPORT);
		else
			playerBuff(player, getConfigs().LIST_MAGE_FAITER_SUPPORT);
		player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
		player.setCurrentCp(player.getMaxCp());
	}

	public void restorePlayers()
	{
		for (Player player : _participants.keySet())
		{
			if(player.isFakeDeath())
			{
				player.setFakeDeath(false);
				player.broadcastPacket(new ChangeWaitType(player, ChangeWaitType.WT_STOP_FAKEDEATH));
				player.broadcastPacket(new Revive(player));
				player.broadcastCharInfo();
			}
			player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
		}
	}

	public void ResurrectionPlayer(Player player)
	{
		if (player._event == null || _state != 2 || !_participants.containsKey(player))
			return;
		Reflection ref = _ref;
		InstantZone instantZone = ref.getInstancedZone();
		Location tele = Location.findPointToStay(instantZone.getTeleportCoords().get(player.getPvPTeam() - 1), 150, 200, ref.getGeoIndex());
		player.teleToLocation(tele, ref);
		restorePlayer(player);
	}

	private void clearAll()
	{
		for(Player player : _participants.keySet())
		{
			player.setPvPTeam(0);
			player.allowPvPTeam();
			player._event = null;
		}
		_participants.clear();

		if (redFlag != null)
			redFlag.deleteMe();
		if (blueFlag != null)
			blueFlag.deleteMe();
	}

	public void rewardToWinTeam(int WinTeam)
	{
		WinTeam++;
		for (Player player : _participants.keySet())
		{
			if ((player != null) && (player.getPvPTeam() == WinTeam))
				for(int i = 0; i < getConfigs().getRewardId().length; i++)
					addItem(player, getConfigs().getRewardId()[i], getConfigs().getRewardCount()[i]);
		}
		sayToAll("scripts.events.CtF.CtF.EventWin", new String[] { getConfigs().TEAM_NAME.get(WinTeam - 1), "Флагов", Integer.toString(_score[(WinTeam - 1)]) }, false);
	}

	public StringBuffer getInformation(Player player)
	{
		int need_score = getConfigs().NEED_SCORE;
		long min = (getConfigs().START_TIME - System.currentTimeMillis() / 1000L) / 60L;
		String time = min + " минут";
		String reward = "";//getConfigs().REWARD_COUNT + " " + ItemHolder.getInstance().getTemplate(getConfigs().REWARD_ITEM_ID).getName();

		StringBuffer content = new StringBuffer();
		content.append("<table width=425 cellspacing=0>");
		content.append("<tr><td align=center>Эвент: <font color=LEVEL>").append(getName()).append("</font></td></tr>");
		content.append("<tr><td align=center>Тех. победа: <font color=LEVEL>").append(need_score).append(" флагов</font></td></tr>");
		content.append("<tr><td align=center>Приз: <font color=LEVEL>").append(reward).append("</font></td></tr>");
		content.append("<tr><td align=center>Кол-во команд: <font color=LEVEL>").append(getConfigs().TEAM_COUNTS).append("</font></td></tr>");
		content.append("<tr><td align=center>Мин/Макс участников: <font color=LEVEL>").append(getConfigs().MIN_PARTICIPANTS).append("/").append(getConfigs().MAX_PARTICIPANTS).append("</font></td></tr>");
		if (_state == 1)
		{
			content.append("<tr><td align=center>");
			if (_participants == null || !_participants.containsKey(player))
				content.append(Strings.htmlButton("Зарегестрироваться", "bypass -h _bbseventreg;" + getName(), 120, 25));
			else 
				content.append(Strings.htmlButton("Отмена", "bypass -h _bbseventunreg;", 120, 25));
			content.append("</td></tr>");
		}
		else
		{
			content.append("<tr><td align=center>Начало через: <font color=LEVEL>").append(time).append("</font></td></tr>");
		}
		content.append("</table>");
		return content;
	}

	private void addFlag(Player player)
	{
		int flagId = player.getPvPTeam() == 1 ? 13560 : 13561;
		ItemInstance item = ItemFunctions.createItem(flagId);
		item.setCustomType1(77);
		item.setCustomFlags(354);
		player.getInventory().addItem(item);
		player.getInventory().equipItem(item);
		player.sendChanges();
		player.setVar("CtF_Flag", "1", -1);
		if (flagId == 13561)
			blueFlag.decayMe();
		else redFlag.decayMe();
	}

	private void removeFlag(Player player)
	{
		if (player != null && player.isTerritoryFlagEquipped())
		{
			ItemInstance[] arr = player.getInventory().getItems();
			int len = arr.length;
			for (int i = 0; i < len; i++)
			{
				ItemInstance _item = arr[i];
				if((_item.getItemId() == 13560 || _item.getItemId() == 13561) && _item.getCustomType1() == 77)
				{
					player.unsetVar("CtF_Flag");
					_item.setCustomFlags(0);
					player.getInventory().destroyItem(_item, 1);
					player.broadcastUserInfo(true);
				}
			}
		}
	}

	private void dropFlag(Player player, boolean onBase)
	{
		if (player != null && player.isTerritoryFlagEquipped())
		{
			ItemInstance flag = player.getActiveWeaponInstance();
			if (flag != null && flag.getCustomType1() == 77)
			{
				removeFlag(player);

				if(flag.getItemId() == 13561)
				{
					blueFlag.setXYZInvisible(onBase ? getConfigs().FLAG_COORDS.get(0) : player.getLoc());
					blueFlag.spawnMe();
				}
				else if(flag.getItemId() == 13560)
				{
					redFlag.setXYZInvisible(onBase ? getConfigs().FLAG_COORDS.get(1) : player.getLoc());
					redFlag.spawnMe();
				}
			}
		}
	}

	@Override
	public boolean talkWithNpc(Player player, NpcInstance npc)
	{
		if (_state > 0 && player != null && _participants.containsKey(player))
		{
			if (npc.getNpcId() == 35426)
			{
				if (player.isTerritoryFlagEquipped() && player.getPvPTeam() == 1)
				{
					flagToBase(player);
					player.unsetVar("CtF_Flag");
				}
				else if (!player.isTerritoryFlagEquipped() && player.getPvPTeam() == 2 && npc.isVisible())
					addFlag(player);
				return true;
			}

			if (npc.getNpcId() == 35423)
			{
				if (player.isTerritoryFlagEquipped() && player.getPvPTeam() == 2)
				{
					flagToBase(player);
					player.unsetVar("CtF_Flag");
				}
				else if (!player.isTerritoryFlagEquipped() && player.getPvPTeam() == 1 && npc.isVisible())
					addFlag(player);
				return true;
			}
		}
		return false;
	}

	public void flagToBase(Player player)
	{
		dropFlag(player, true);
		player.unsetVar("CtF_Flag");
		_score[(player.getPvPTeam() - 1)] += 1;
		sayToAll("scripts.events.CtF.CtF.FlagToBase", new String[] { player.getName(), getConfigs().TEAM_NAME.get(player.getPvPTeam() - 1) }, false);
	}

	class TaskVoid implements Runnable
	{
		String _name;
		Player _player;

		TaskVoid(String name, Player player)
		{
			_name = name;
			_player = player;
		}

		public void run()
		{
			if (_name.equals("canRegisters"))
				canRegisters();
			else if (_name.equals("start"))
				start();
			else if (_name.equals("restorePlayers"))
				restorePlayers();
			else if (_name.equals("returnBuffsToPlayers"))
				returnBuffsToPlayers(_player);
			else if (_name.equals("teleportPlayersToSavedCoords"))
				teleportPlayersToSavedCoords();
			else if (_name.equals("clearAll"))
				clearAll();
			else if (_name.equals("ResurrectionPlayer"))
				ResurrectionPlayer(_player);
		}
	}

	class StartMessages	implements Runnable
	{
		String _adress;
		String[] _replacing;

		StartMessages(String adress, String[] replacing)
		{
			_adress = adress;
			_replacing = replacing;
		}

		public void run()
		{
			if (_state == 1)
				sayToAll(_adress, _replacing, true);
		}
	}

	public class go implements Runnable
	{
		public go()
		{}

		public void run()
		{
			unParalyzePlayers();
			int time = getConfigs().TIME_TO_END_BATTLE;

			sayToAll("scripts.events.CtF.CtF.RoundStarted", new String[0], false);

			while(time >= 0 && _state == 2)
			{
				int sec = time - time / 60 * 60;

				for (Player player : _participants.keySet())
				{
					String message = "Очков: " + _score[(player.getPvPTeam() - 1)] + " из " + getConfigs().NEED_SCORE;
					message = message + "\nКоманда: " + getConfigs().TEAM_NAME.get(player.getPvPTeam() - 1);

					if (sec < 10)
						message = message + "\nОсталось: " + time / 60 + ":0" + sec;
					else {
						message = message + "\nОсталось: " + time / 60 + ":" + sec;
					}
					player.sendPacket(new ExShowScreenMessage(message, 2000, ExShowScreenMessage.ScreenMessageAlign.BOTTOM_RIGHT, false));
					if(getCountPlayers() <= 1 || _score[(player.getPvPTeam() - 1)] == getConfigs().NEED_SCORE)
						finish();
				}
				try
				{
					Thread.sleep(1000);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}

				time--;
			}

			finish();
    	}
	}

	private void playersBuff()
	{
		for(Player player : _participants.keySet())
		{
			ClassId nclassId = ClassId.VALUES[player.getClassId().getId()];
			if(nclassId.isMage())
				playerBuff(player, getConfigs().LIST_MAGE_MAG_SUPPORT);
			else
				playerBuff(player, getConfigs().LIST_MAGE_FAITER_SUPPORT);
		}
	}

	private void playerBuff(Player player, GArray<Integer> list)
	{
		int time = getConfigs().TIME_MAGE_SUPPORT;
		Summon pet = player.getPet();
		Skill skill = null;

		for(int i : list)
		{
			int lvl = SkillTable.getInstance().getBaseLevel(i);
			
			skill = SkillTable.getInstance().getInfo(i, lvl);
			if(pet != null)
				for(EffectTemplate et : skill.getEffectTemplates())
				{	
					Env env = new Env(pet, pet, skill);
					Effect effect = et.getEffect(env);
					effect.setPeriod(time * 60000);
					pet.getEffectList().addEffect(effect);
					pet.updateEffectIcons();
				}
			else
				for(EffectTemplate et : skill.getEffectTemplates())
				{	
					Env env = new Env(player, player, skill);
					Effect effect = et.getEffect(env);
					effect.setPeriod(time * 60000);
					player.getEffectList().addEffect(effect);
					player.sendChanges();
					player.updateEffectIcons();
				}
		}
	}

	@Override
	public boolean canAttack(Creature attacker, Creature target)
	{
		if(_state == 2)
		{
			if(attacker.getTeam() == target.getTeam())
				return false;
			if(target.isFakeDeath())
				return false;
		}
		return true;
	}

	@Override
	public boolean canUseSkill(Creature caster, Creature target, Skill skill)
	{
		if(_state == 2)
		{
			if(skill.isHeroic() && !getConfigs().ALLOW_HERO_WEAPONS)
			{
				caster.sendMessage(caster.getPlayer().isLangRus() ? "Запрещено использовать во время ивентов." : "You may not use during the events.");
				return false;
			}
			if(target.isFakeDeath())
				return false;
		}
		return true;
	}

	@Override
	public String minLvl()
	{
		return "" + getConfigs().MIN_LEVEL;
	}

	@Override
	public String maxLvl()
	{
		return "" + getConfigs().MAX_LEVEL;
	}

	@Override
	public void onPlayerExit(Player player)
	{
		if(player.getPvPTeam() == 0)
			return;

		// Вышел или вылетел во время эвента
		if(_state > 1 && player != null && player.getPvPTeam() != 0 && _participants.containsKey(player))
		{
			try
			{
				if(player != null)
					player.teleToLocation(player._stablePoint, ReflectionManager.DEFAULT);
				remove(player);
				player.setPvPTeam(0);
				player.allowPvPTeam();
				player._event = null;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private void checkPlayers()
	{
		for(Player player : _participants.keySet())
		{
			//if(player)
		}
	}
}