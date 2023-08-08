package l2ft.gameserver.templates.spawn;

import l2ft.gameserver.utils.Location;

public interface SpawnRange
{
	Location getRandomLoc(int geoIndex);
}
