package l2ft.geoserver.geodata;

import java.util.Vector;

import l2ft.geoserver.GeoConfig;
import l2ft.geoserver.geodata.PathFindBuffers.GeoNode;
import l2ft.geoserver.model.Location;

public class PathFind
{
	private static final byte NSWE_NONE = 0, EAST = 1, WEST = 2, SOUTH = 4, NORTH = 8, NSWE_ALL = 15;
	private Vector<Location>	path;
	private PathFindBuffers.PathFindBuffer buff;

	//final long					time = System.currentTimeMillis();

    public PathFind(int x, int y, int z, int destX, int destY, int destZ, GeoEngine engine)
    {
        Location startpoint = engine.moveCheckWithCollision(x, y, z, destX, destY, true, true);
        Location endpoint = new Location(destX, destY, destZ);
        //Location native_endpoint = new Location(destX, destY, destZ);
        //Location endpoint = Math.abs(destZ - z) <= 200 ? engine.moveCheckBackwardWithCollision(destX, destY, destZ, startpoint.getX(), startpoint.getY(), true) : new Location(destX, destY, destZ);
        startpoint.world2geo();
        //native_endpoint.world2geo();
        endpoint.world2geo();
        startpoint.setZ(engine.getHeightGeo(startpoint.getX(), startpoint.getY(), startpoint.getZ()));
        endpoint.setZ(engine.getHeightGeo(endpoint.getX(), endpoint.getY(), endpoint.getZ()));
        int xdiff = Math.abs(endpoint.getX() - startpoint.getX());
        int ydiff = Math.abs(endpoint.getY() - startpoint.getY());
        if(xdiff == 0 && ydiff == 0)
        {
            if(Math.abs(endpoint.getZ() - startpoint.getZ()) < 32)
                path = new Vector<Location>();
            return;
        }
        if((buff = PathFindBuffers.alloc(64 + 2 * Math.max(xdiff, ydiff), startpoint, endpoint)) != null)
        {
            path = findPath();
            buff.free();
        }
    }

    public Vector<Location> findPath()
    {
        buff.firstNode = PathFindBuffers.GeoNode.initNode(buff, buff.startpoint.getX() - buff.offsetX, buff.startpoint.getY() - buff.offsetY, buff.startpoint);
        buff.firstNode.closed = true;
        PathFindBuffers.GeoNode nextNode = buff.firstNode;
        PathFindBuffers.GeoNode finish = null;
		int i = buff.info.maxIterations;
		while(nextNode != null && i-- > 0)
		{
			if((finish = handleNode(nextNode)) != null)
				return tracePath(finish);
			nextNode = getBestOpenNode();
		}
        return null;
    }

    private PathFindBuffers.GeoNode getBestOpenNode()
    {
        PathFindBuffers.GeoNode bestNodeLink = null;
        PathFindBuffers.GeoNode oldNode = buff.firstNode;
        for(PathFindBuffers.GeoNode nextNode = buff.firstNode.link; nextNode != null; nextNode = oldNode.link)
        {
            if(bestNodeLink == null || nextNode.score < bestNodeLink.link.score)
                bestNodeLink = oldNode;
            oldNode = nextNode;
        }

        if(bestNodeLink != null)
        {
            bestNodeLink.link.closed = true;
            PathFindBuffers.GeoNode bestNode = bestNodeLink.link;
            bestNodeLink.link = bestNode.link;
            if(bestNode == buff.currentNode)
                buff.currentNode = bestNodeLink;
            return bestNode;
        }
        return null;
    }

    private Vector<Location> tracePath(GeoNode f)
    {
    	Vector<Location> locations = new Vector<Location>();
        do
        {
            locations.add(0, f.getLoc());
            f = f.parent;
        } while(f.parent != null);
        return locations;
    }

    public PathFindBuffers.GeoNode handleNode(GeoNode node)
    {
        PathFindBuffers.GeoNode result = null;
        int clX = node._x;
        int clY = node._y;
        short clZ = node._z;
        if (!getHeightAndNSWE(clX, clY, clZ)) return result;
        short NSWE = buff.hNSWE[1];

		if(GeoConfig.PATHFIND_DIAGONAL)
		{
			// Юго-восток
			if((NSWE & SOUTH) == SOUTH && (NSWE & EAST) == EAST)
			{
				if (getHeightAndNSWE(clX + 1, clY, clZ))
				{
					if((buff.hNSWE[1] & SOUTH) == SOUTH)
					{
						if (getHeightAndNSWE(clX, clY + 1, clZ))
						{
							if((buff.hNSWE[1] & EAST) == EAST)
							{
								result = getNeighbour(clX + 1, clY + 1, node, true);
								if(result != null)
									return result;
							}
						}
					}
				}
			}

			// Юго-запад
			if((NSWE & SOUTH) == SOUTH && (NSWE & WEST) == WEST)
			{
				if (getHeightAndNSWE(clX - 1, clY, clZ))
				{
					if((buff.hNSWE[1] & SOUTH) == SOUTH)
					{
						if (getHeightAndNSWE(clX, clY + 1, clZ))
						{
							if((buff.hNSWE[1] & WEST) == WEST)
							{
								result = getNeighbour(clX - 1, clY + 1, node, true);
								if(result != null)
									return result;
							}
						}
					}
				}
			}

			// Северо-восток
			if((NSWE & NORTH) == NORTH && (NSWE & EAST) == EAST)
			{
				if (getHeightAndNSWE(clX + 1, clY, clZ))
				{
					if((buff.hNSWE[1] & NORTH) == NORTH)
					{
						if (getHeightAndNSWE(clX, clY - 1, clZ))
						{
							if((buff.hNSWE[1] & EAST) == EAST)
							{
								result = getNeighbour(clX + 1, clY - 1, node, true);
								if(result != null)
									return result;
							}
						}
					}
				}
			}

			// Северо-запад
			if((NSWE & NORTH) == NORTH && (NSWE & WEST) == WEST)
			{
				if (getHeightAndNSWE(clX - 1, clY, clZ))
				{
					if((buff.hNSWE[1] & NORTH) == NORTH)
					{
						if (getHeightAndNSWE(clX, clY - 1, clZ))
						{
							if((buff.hNSWE[1] & WEST) == WEST)
							{
								result = getNeighbour(clX - 1, clY - 1, node, true);
								if(result != null)
									return result;
							}
						}
					}
				}
			}
		}
		
		// Восток
		if((NSWE & EAST) == EAST)
		{
			result = getNeighbour(clX + 1, clY, node, false);
			if(result != null)
				return result;
		}

		// Запад
		if((NSWE & WEST) == WEST)
		{
			result = getNeighbour(clX - 1, clY, node, false);
			if(result != null)
				return result;
		}

		// Юг
		if((NSWE & SOUTH) == SOUTH)
		{
			result = getNeighbour(clX, clY + 1, node, false);
			if(result != null)
				return result;
		}

		// Север
		if((NSWE & NORTH) == NORTH)
			result = getNeighbour(clX, clY - 1, node, false);
        return result;
    }

    public PathFindBuffers.GeoNode getNeighbour(int x, int y, PathFindBuffers.GeoNode from, boolean d)
    {
		int nX = x - buff.offsetX, nY = y - buff.offsetY;

		if(nX >= buff.info.MapSize || nX < 0 || nY >= buff.info.MapSize || nY < 0)
			return null;

		boolean isOldNull = PathFindBuffers.GeoNode.isNull(buff.nodes[nX][nY]);

		if(!isOldNull && buff.nodes[nX][nY].closed)
			return null;

        //PathFindBuffers.GeoNode n = isOldNull ? PathFindBuffers.GeoNode.initNode(buff, nX, nY, x, y, from._z, from) : buff.tempNode.reuse(buff.nodes[nX][nY], from);
		PathFindBuffers.GeoNode n = ((isOldNull) ? PathFindBuffers.GeoNode.initNode(buff, nX, nY, x, y, from._z, 0 /*GeoConfig.WEIGHT1*/, from) : buff.tempNode.reuse(buff.nodes[nX][nY], from));

		int height = Math.abs(n._z - from._z);

        if(height > GeoConfig.PATHFIND_MAX_Z_DIFF || n._nswe == NSWE_NONE)
			return null;

		double weight = d ? 1.414213562373095 * GeoConfig.WEIGHT1 : GeoConfig.WEIGHT1;

		if(n._nswe != NSWE_ALL || height > 16)
			weight += GeoConfig.WEIGHT2;
		if (getHeightAndNSWE(x + 1, y, n._z))
		{
			if(buff.hNSWE[1] != NSWE_ALL || Math.abs(n._z - buff.hNSWE[0]) > 16)
				weight += GeoConfig.WEIGHT3;
		}
		if (getHeightAndNSWE(x - 1, y, n._z))
		{
			if(buff.hNSWE[1] != NSWE_ALL || Math.abs(n._z - buff.hNSWE[0]) > 16)
				weight += GeoConfig.WEIGHT3;
		}
		if (getHeightAndNSWE(x, y + 1, n._z))
		{
			if(buff.hNSWE[1] != NSWE_ALL || Math.abs(n._z - buff.hNSWE[0]) > 16)
				weight += GeoConfig.WEIGHT3;
		}
		if (getHeightAndNSWE(x, y - 1, n._z))
		{
			if(buff.hNSWE[1] != NSWE_ALL || Math.abs(n._z - buff.hNSWE[0]) > 16)
				weight += GeoConfig.WEIGHT3;
		}

		int diffx = buff.endpoint.getX() - x;
		int diffy = buff.endpoint.getY() - y;
		int dz = Math.abs(buff.endpoint.getZ() - n._z);

		n.moveCost += from.moveCost + weight;
        //n.score = n.moveCost + Math.sqrt(diffx * diffx + diffy * diffy + (dz * dz) / 256);
        n.score = n.moveCost + (GeoConfig.PATHFIND_DIAGONAL ? Math.sqrt(diffx * diffx + diffy * diffy + (dz * dz) / 256) : Math.abs(diffx) + Math.abs(diffy) + dz / 16);
		if(x == buff.endpoint.getX() && y == buff.endpoint.getY() && dz < GeoConfig.PATHFIND_MAX_Z_DIFF)
			return n;
		if(isOldNull)
		{
			if(buff.currentNode == null)
				buff.firstNode.link = n;
			else
				buff.currentNode.link = n;
			buff.currentNode = n;
		}
		else if(n.moveCost < buff.nodes[nX][nY].moveCost)
			buff.nodes[nX][nY].copy(n);
		return null;
    }

    private boolean getHeightAndNSWE(int x, int y, short z)
    {
        int nX = x - buff.offsetX;
        int nY = y - buff.offsetY;
        if(nX >= buff.info.MapSize || nX < 0 || nY >= buff.info.MapSize || nY < 0)
        {
            buff.hNSWE[1] = 0;
            return true;
        }
        PathFindBuffers.GeoNode n = buff.nodes[nX][nY];
        if(n == null)
            n = PathFindBuffers.GeoNode.initNodeGeo(buff, nX, nY, x, y, z);
        
        if (n != null && (n._z - z) <= 16)
        {
        	buff.hNSWE[0] = n._z;
        	buff.hNSWE[1] = n._nswe;
        	return true;
        }
        else
        	return false;
    }

	public Vector<Location> getPath()
	{
		return path;
	}
}