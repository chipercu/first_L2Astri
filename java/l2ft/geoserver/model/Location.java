package l2ft.geoserver.model;

import java.io.Serializable;

import l2ft.geoserver.GeoConfig;

@SuppressWarnings("serial")
public final class Location implements Serializable
{
	private int	_x;
	private int	_y;
	private int	_z;
	private int	_heading;
	private MoveTrick[] _tricks;

	public Location(int x, int y, int z)
	{
		_heading = 0;
		_x = x;
		_y = y;
		_z = z;
	}

	public Location(int x, int y, int z, int heading)
	{
		_x = x;
		_y = y;
		_z = z;
		_heading = heading;
	}

	public synchronized void setTricks(MoveTrick[] mt)
	{
		_tricks = mt;
	}
	
	public MoveTrick[] getTricks()
	{
		return _tricks;
	}
	
	public int getX()
	{
		return _x;
	}

	public int getY()
	{
		return _y;
	}

	public int getZ()
	{
		return _z;
	}
	
	public Location setX(int x)
	{
		_x = x;
		return this;
	}

	public Location setY(int y)
	{
		_y = y;
		return this;
	}

	public Location setZ(int z)
	{
		_z = z;
		return this;
	}
	
    public void set(Location loc)
    {
        _x = loc.getX();
        _y = loc.getY();
        _z = loc.getZ();
        _heading = loc.getHeading();
    }	

	public void setAll(int x, int y, int z)
	{
		_x = x;
		_y = y;
		_z = z;
	}
	
	public int getHeading()
	{
		return _heading;
	}
	
	public Location setHeading(int h)
	{
		_heading = h;
		return this;
	}
	
    public boolean equals(Location loc)
    {
        return loc.getX() == _x && loc.getY() == _y && loc.getZ() == _z;
    }

    public boolean equals(int x, int y, int z)
    {
        return _x == x && _y == y && _z == z;
    }

    public Location geo2world()
    {
		// размер одного блока 16*16 точек, +8*+8 это его средина
        _x = (_x << 4) + GeoConfig.MAP_MIN_X + 8;
        _y = (_y << 4) + GeoConfig.MAP_MIN_Y + 8;
        return this;
    }

    public Location world2geo()
    {
        _x = _x - GeoConfig.MAP_MIN_X >> 4;
        _y = _y - GeoConfig.MAP_MIN_Y >> 4;
        return this;
    }

    public Location(String s) throws IllegalArgumentException
    {
    	_heading = 0;
    	String xyzh[] = s.replaceAll(",", " ").replaceAll(";", " ").replaceAll("  ", " ").trim().split(" ");
    	if(xyzh.length < 3)
    	{
    		throw new IllegalArgumentException((new StringBuilder()).append("Can't parse location from string: ").append(s).toString());
    	}
    	else
    	{
    		_x = Integer.parseInt(xyzh[0]);
    		_y = Integer.parseInt(xyzh[1]);
    		_z = Integer.parseInt(xyzh[2]);
    		_heading = xyzh.length >= 4 ? Integer.parseInt(xyzh[3]) : 0;
    		return;
    	}
    }
}