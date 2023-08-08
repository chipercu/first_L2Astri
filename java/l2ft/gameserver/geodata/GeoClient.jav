package l2ft.gameserver.geodata;

import java.rmi.RemoteException;

import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2ft.gameserver.Config;
import l2ft.gameserver.model.GameObject;
import l2ft.gameserver.model.Creature;
import l2ft.commons.util.Rnd;

import l2ft.geoserver.geodata.GeoEngine;
import l2ft.geoserver.geodata.PathFindBuffers;
import l2ft.geoserver.model.GeoCollision;
import l2ft.geoserver.model.Location;
import l2ft.geoserver.model.MoveTrick;

public class GeoClient 
{
	final static Logger						_log			= LoggerFactory.getLogger(GeoClient.class);
	private static GeoClient				instance;
	public static final int					seeUp = 16;
	private GeoEngine						geoEngine;

	public static GeoClient getInstance()
	{
		if (instance == null)
			try
			{
				new GeoClient();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Can't init geoclient.");
				System.exit(0);
			}
		return instance;
	}

	public GeoClient() throws RemoteException
	{
		instance = this;

		if (Config.ALLOW_GEODATA)
			initLocal();
		else	
			initFake();
	}

	public void initFake()
	{
		_log.info("GeoData: Disabled");
	}

	public void initLocal()
	{
		PathFindBuffers.initBuffers("8x100;8x128;8x192;4x256;2x320;2x384;1x500");
		GeoEngine.loadGeo();
		geoEngine = new GeoEngine();
		_log.info("GeoData: GeoEngine Local Started");
	}


	public Vector<Location> pathFind(int x, int y, int z, Location pos)
	{
		if (Config.ALLOW_GEODATA)
			return geoEngine.pathFind(x, y, z, pos.getX(), pos.getY(), pos.getZ());
		else
			return new Vector<Location>();
	}

	public boolean canSeeTarget(Creature actor, Creature target)
	{
		if (actor == null || target == null)
			return true;
		
		if (actor == target)
			return true;
		
		if (actor.checkIfDoorsBetween(target.getLoc(), target))
			return false;

		return canSeeTarget((int)actor.getX(), (int)actor.getY(), (int)actor.getZ(), (int)target.getX(), (int)target.getY(), (int)target.getZ(), actor.isFlying(), (int)actor.getColHeight(), (int)target.getColHeight());
	}
	
	public boolean canSeeTarget(Creature actor, GameObject target)
	{
		if (actor == null || target == null)
			return true;
		
		if (target instanceof Creature && actor == target)
			return true;
		
		if (actor.checkIfDoorsBetween(target.getLoc(), target))
			return false;

		return canSeeTarget((int)actor.getX(), (int)actor.getY(), (int)actor.getZ(), (int)target.getX(), (int)target.getY(), (int)target.getZ(), actor.isFlying(), (int)actor.getColHeight(), 16);
	}

	public boolean canSeeTarget(Creature actor, Location pos)
	{
		if (actor == null || pos == null)
			return true;
		
		if (actor.checkIfDoorsBetween(pos, null))
			return false;

		return canSeeTarget((int)actor.getX(), (int)actor.getY(), (int)actor.getZ(), (int)pos.getX(), (int)pos.getY(), (int)pos.getZ(), actor.isFlying(), (int)actor.getColHeight(), 16);
	}
	
	public boolean canSeeTarget(Creature actor, int tx, int ty, int tz, boolean inAir)
	{
		if (actor == null)
			return true;
		
		if (actor instanceof Creature && actor.checkIfDoorsBetween(new Location(tx, ty, tz), null))
			return false;
		
		return canSeeTarget((int)actor.getX(), (int)actor.getY(), (int)actor.getZ(), tx, ty, tz, inAir, (int)actor.getColHeight(), 16);
	}
	
	public Location moveInWaterCheck(Creature actor, Location toPos)
	{
		if (actor == null)
			return null;
		
		if (actor instanceof Creature && actor.checkIfDoorsBetween(toPos, null))
			return null;
		
		return moveInWaterCheck((int)actor.getX(), (int)actor.getY(), (int)actor.getZ(), (int)toPos.getX(), (int)toPos.getY(), (int)toPos.getZ(), 0, 0);
	}	
	
	private boolean canSeeTarget(int x, int y, int z, int tx, int ty, int tz, boolean inAir, int colHeightActor, int colHeightTarget)
	{
		//_log.info("canSeeTarget x="+x+" y="+y+" z="+z+" tx="+tx+" ty="+ty+" tz="+tz+" inAir="+inAir+" colHeightActor="+colHeightActor+" colHeightTarget="+colHeightTarget);
		if (Config.ALLOW_GEODATA)
			return geoEngine.canSeeTarget(x, y, z, tx, ty, tz, inAir, colHeightActor, colHeightTarget);
		else
			return true;
	}

	public int getHeight(int x, int y, int z)
	{
		if (Config.ALLOW_GEODATA)
			return geoEngine.getHeight(x, y, z);
		else
			return z;
	}

	public int getHeight(Location loc)
	{
		return getHeight(loc.getX(), loc.getY(), loc.getZ());
	}

	public Location moveCheckWithoutDoors(Location fromPos, Location toPos, boolean returnPrev)
	{
		return moveCheck(fromPos.getX(), fromPos.getY(), fromPos.getZ(), toPos.getX(), toPos.getY(), toPos.getZ(), returnPrev);
	}
	
	public Location moveCheck(Creature actor, Location toPos, boolean returnPrev)
	{
		if (actor == null || actor.checkIfDoorsBetween(toPos, null))
			return null;
		
		return moveCheck(actor.getX(), actor.getY(), actor.getZ(), toPos.getX(), toPos.getY(), toPos.getZ(), returnPrev);
	}
	
	private Location moveCheck(int x, int y, int z, int tx, int ty, int tz, boolean returnPrev)
	{
		if (Config.ALLOW_GEODATA)
			return geoEngine.moveCheck(x, y, z, tx, ty, tz, returnPrev);
		else
			return new Location(tx, ty, tz);
	}

	public boolean canMoveToCoord(Creature actor, Location pos, boolean returnPrev)
	{
		if (actor.checkIfDoorsBetween(pos, null))
			return false;
		
		return canMoveToCoord(actor.getX(), actor.getY(), actor.getZ(), pos.getX(), pos.getY(), pos.getZ(), returnPrev);
	}
	
	public boolean canMoveToCoord(Creature actor, int tx, int ty, int tz, boolean returnPrev)
	{
		if (actor.checkIfDoorsBetween(new Location(tx, ty, tz), null))
			return false;
		
		return canMoveToCoord(actor.getX(), actor.getY(), actor.getZ(), tx, ty, tz, returnPrev);
	}
	
	private boolean canMoveToCoord(int x, int y, int z, int tx, int ty, int tz, boolean returnPrev)
	{
		if (Config.ALLOW_GEODATA)
			return geoEngine.canMoveToCoord(x, y, z, tx, ty, tz, returnPrev);
		else
			return true;
	}
	
	public MoveTrick[] canMoveAdvanced(Creature actor, Location pos, boolean returnPrev)
	{
		if (actor.checkIfDoorsBetween(pos, null))
			return null;
		
		return canMoveAdvanced(actor.getX(), actor.getY(), actor.getZ(), pos.getX(), pos.getY(), pos.getZ(), returnPrev);
	}
	
	private MoveTrick[] canMoveAdvanced(int x, int y, int z, int tx, int ty, int tz, boolean returnPrev)
	{
		if (Config.ALLOW_GEODATA)
			return geoEngine.canMoveAdvanced(x, y, z, tx, ty, tz, returnPrev);
		else
		{
	    	int dx1 = x - tx;
			int dy1 = y - ty;
			int dist = (int)Math.sqrt(dx1 * dx1 + dy1 * dy1);
			MoveTrick[]  result = {new MoveTrick(dist, tz)};
			return result;
		}
	}

	public Location moveCheckForAI(GameObject cha, GameObject target)
	{
		return moveCheckForAI(new Location(cha.getX(), cha.getY(), cha.getZ()), new Location(target.getX(), target.getY(), target.getZ()));
	}

	public Location moveCheckForAI(Location loc1, Location loc2)
	{
		if (Config.ALLOW_GEODATA)
			return geoEngine.moveCheckForAI(loc1, loc2);
		else
			return loc2;
	}

	public short getNSWE(int x, int y, int z)
	{
		if (Config.ALLOW_GEODATA)
			return geoEngine.getNSWE(x, y, z);
		else
			return 15; //ALL
	}

	public MoveTrick[] canMoveToCoordWithCollision(int x, int y, int z, int tx, int ty, int tz, boolean returnPrev)
	{
		if (Config.ALLOW_GEODATA)
			return geoEngine.canMoveToTargetWithCollision(x, y, z, tx, ty, tz, returnPrev, false);
		else
		{
	    	int dx1 = x - tx;
			int dy1 = y - ty;
			int dist = (int)Math.sqrt(dx1 * dx1 + dy1 * dy1);
	        MoveTrick[]  result = {new MoveTrick(dist, tz)};
			return result;
		}
	}
	
	private Location moveInWaterCheck(int x, int y, int z, int tx, int ty, int tz, int colHeightActor, int colHeightTarget)
	{
		//_log.info("canSeeTarget x="+x+" y="+y+" z="+z+" tx="+tx+" ty="+ty+" tz="+tz+" inAir="+inAir+" colHeightActor="+colHeightActor+" colHeightTarget="+colHeightTarget);
		if (Config.ALLOW_GEODATA)
			return geoEngine.moveInWaterCheck(x, y, z, tx, ty, tz, colHeightActor, colHeightTarget);
		else
			return new Location(tx,ty,tz);
	}

	/**
	 * @param x
	 * @param y
	 *
	 * @return Geo Block Type
	 */
	public short getType(int x, int y)
	{
		if (Config.ALLOW_GEODATA)
			return geoEngine.getType(x, y);
		else
			return 0;
	}

	public static Location coordsRandomize(Creature actor, int x, int y, int z, int heading, int radius_min, int radius_max, boolean GeoZCorrect)
	{
		if (radius_max == 0 || radius_max < radius_min)
			return new Location(x, y, z, heading);

		if (actor.isFlying())
			return new Location(x, y, z, heading);
		
		Location newLoc = null;
		
		for (int i = 0; i < 10; i++)
		{
			int radius = Rnd.get(radius_min, radius_max);
			double angle = Rnd.nextDouble() * 2 * Math.PI;
			
			newLoc = new Location((int) (x + radius * Math.cos(angle)), (int) (y + radius * Math.sin(angle)), z, heading);
			
			if (GeoZCorrect)
				newLoc.setZ(getInstance().getSpawnHeight(newLoc.getX(), newLoc.getY(), newLoc.getZ()));

			if (actor.checkIfDoorsBetween(newLoc, null))
				continue;
			
			if (getInstance().canMoveToCoord(x, y, z, newLoc.getX(), newLoc.getY(), newLoc.getZ(), true))
				break;
		}
		
		return newLoc;
	}

	public static Location coordsRandomize(int x, int y, int z, int heading, int radius_min, int radius_max, boolean GeoZCorrect)
	{
		if (radius_max == 0 || radius_max < radius_min)
			return new Location(x, y, z, heading);

		Location newLoc = null;
		
		for (int i = 0; i < 10; i++)
		{
			int radius = Rnd.get(radius_min, radius_max);
			double angle = Rnd.nextDouble() * 2 * Math.PI;
			
			newLoc = new Location((int) (x + radius * Math.cos(angle)), (int) (y + radius * Math.sin(angle)), z, heading);
			
			if (GeoZCorrect)
				newLoc.setZ(getInstance().getSpawnHeight(newLoc.getX(), newLoc.getY(), newLoc.getZ()));

			if (getInstance().canMoveToCoord(x, y, z, newLoc.getX(), newLoc.getY(), newLoc.getZ(), true))
				break;
		}
		
		return newLoc;
	}
	
	/*public Location findPointToStay(int x, int y, int z, int j, int k)
	{
		Location pos = new Location(x, y, z);
		for (int i = 0; i < 100; i++)
		{
			pos = coordsRandomize(x, y, z, 0, j, k);
			if (canMoveToCoord(x, y, z, pos.getX(), pos.getY(), pos.getZ()) && canMoveToCoord(pos.getX(), pos.getY(), pos.getZ(), x, y, z))
				break;
		}
		return pos;
	}*/

	public int getSpawnHeight(Creature cha)
	{
		return getSpawnHeight(cha.getX(), cha.getY(), cha.getZ());
	}
	
	public int getSpawnHeight(Location loc)
	{
		return getSpawnHeight(loc.getX(), loc.getY(), loc.getZ());
	}
	
	public int getSpawnHeight(int x, int y, int zmin)
	{
		if (Config.ALLOW_GEODATA)
			return geoEngine.getSpawnHeight(x, y, zmin);
		else
			return zmin;
	}
	
	public void applyGeoCollision(GeoCollision collision)
	{
		if (Config.ALLOW_GEODATA && 1 == 0)
			geoEngine.applyGeoCollision(collision);
	}
	
	public void removeGeoCollision(GeoCollision collision)
	{
		if (Config.ALLOW_GEODATA && 1 == 0)
			geoEngine.removeGeoCollision(collision);
	}
}