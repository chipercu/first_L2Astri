package l2ft.gameserver.data.xml.holder;

import gnu.trove.map.hash.TIntObjectHashMap;

import l2ft.commons.data.xml.AbstractHolder;
import l2ft.gameserver.templates.AirshipDock;

public final class AirshipDockHolder extends AbstractHolder
{
	private static final AirshipDockHolder _instance = new AirshipDockHolder();
	private TIntObjectHashMap<AirshipDock> _docks = new TIntObjectHashMap<>(4);

	public static AirshipDockHolder getInstance()
	{
		return _instance;
	}

	public void addDock(AirshipDock dock)
	{
		_docks.put(dock.getId(), dock);
	}

	public AirshipDock getDock(int dock)
	{
		return _docks.get(dock);
	}

	@Override
	public int size()
	{
		return _docks.size();
	}

	@Override
	public void clear()
	{
		_docks.clear();
	}
}
