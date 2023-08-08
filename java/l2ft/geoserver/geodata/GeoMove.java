package l2ft.geoserver.geodata;

import java.util.Vector;

import l2ft.geoserver.GeoConfig;
import l2ft.geoserver.model.Location;

public class GeoMove
{
	private final GeoEngine	engine;

	public GeoMove(GeoEngine engine)
	{
		this.engine = engine;
	}

	public Vector<Location> pathFind(int x, int y, int z, int tx, int ty, int tz)
	{
		Location target = new Location(tx, ty, tz);
        z = engine.getHeight(x, y, z);
        if(Math.abs(z - target.getZ()) > 256)
            return null;
        target.setZ(engine.getHeight(target));
        PathFind n = new PathFind(x, y, z, target.getX(), target.getY(),target.getZ(), engine);
        if(n.getPath() == null || n.getPath().isEmpty())
            return null;
		for (Location p : n.getPath())
			p.geo2world();

		//_log.info("pathFind start loc x="+x+" y="+y+" tx="+tx+" ty="+ty);
        n.getPath().add(0, new Location(x, y, z));//first for check path
		n.getPath().addElement(target);
		
        if(GeoConfig.PATH_CLEAN)
            pathClean(n.getPath());
        
        if (n.getPath().size() > 0)
        	n.getPath().removeElementAt(0);//first
        return n.getPath();
	}

    private void pathClean(Vector<Location> path)
    {
    	//_log.info("pathClean path.size()="+path.size());
        int size = path.size();
        if(size > 2)
        {
            for(int i = 2; i < size; i++)
            {
                Location p3 = path.elementAt(i);
                Location p2 = path.elementAt(i - 1);
                Location p1 = path.elementAt(i - 2);
                if(p1.equals(p2) || p3.equals(p2) || IsPointInLine(p1.getX(), p1.getY(), p3.getX(), p3.getY(), p2))
                {
                    path.remove(i - 1);
                    size--;
                    i = Math.max(2, i - 2);
                }
            }

        }
    	//_log.info("pathClean 1 path.size()="+path.size());
        for(int current = 0; current < path.size() - 2; current++)
        {
            for(int sub = current + 2; sub < path.size(); sub++)
            {
                Location one = path.elementAt(current);
                Location two = path.elementAt(sub);
                if(!one.equals(two) && !engine.canMoveToCoord(one.getX(), one.getY(), one.getZ(), two.getX(), two.getY(), two.getZ(), false))
                   	continue;
                
                for(; current + 1 < sub; sub--)
                    path.remove(current + 1);
            }
        }
    	//_log.info("pathClean 2 path.size()="+path.size());
    	
        for(int current = 0; current < path.size() - 2; current++)
        {
            Location one = path.elementAt(current);
            Location two = path.elementAt(current + 1);
            two.setTricks(engine.canMoveToTargetWithCollision(one.getX(), one.getY(), one.getZ(), two.getX(), two.getY(), two.getZ(), false, true));
        	//_log.info("pathClean 2 current="+current+" one x="+one.getX()+" y="+one.getY()+" two x="+two.getX()+" y="+two.getY()+" two.getTricks()="+(two.getTricks() != null));
            if (two.getTricks() == null)
            {
            	//_log.info("pathClean path fail at current="+current+" to "+(current + 1)+", not move");
            	if (current == 0)
            		path.clear();
            	else
            	{
                    for(int sub = current + 1; (current + 1) < path.size(); sub++)
                    	path.remove(current + 1);
            	}
            	//_log.info("pathClean path clear path.size()="+path.size());
        		return;
            }
        }
    	//_log.info("pathClean 3 path.size()="+path.size());
    }
    
	private static boolean IsPointInLine(int x1, int y1, int x2, int y2, Location P)
	{
		if(x1 == x2 && x2 == P.getX() || y1 == y2 && y2 == P.getY())
			return true;
		return (x1 - P.getX()) * (y1 - P.getY()) == (P.getX() - x2) * (P.getY() - y2);
	}
}