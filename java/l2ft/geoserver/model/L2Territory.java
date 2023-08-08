package l2ft.geoserver.model;

import l2ft.commons.util.Rnd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Polygon;
import java.io.Serializable;

@SuppressWarnings("serial")
public class L2Territory implements Serializable
{
	private final static Logger	_log	= LoggerFactory.getLogger(L2Territory.class);

	protected class Point implements Serializable
	{
		protected int	_x, _y, _zmin, _zmax;

		Point(int x, int y, int zmin, int zmax)
		{
			_x = x;
			_y = y;
			_zmin = zmin;
			_zmax = zmax;
		}
	}

	private Point[]			_points;
	private Polygon			_poly;
	private String			_name;
	private int				_xMin;
	private int				_xMax;
	private int				_yMin;
	private int				_yMax;
	private int				_zMin;
	private int				_zMax;

	public L2Territory(String name)
	{
		_poly = new Polygon();
		_points = new Point[0];
		_name = name;
		_xMin = 999999;
		_xMax = -999999;
		_yMin = 999999;
		_yMax = -999999;
		_zMin = 999999;
		_zMax = -999999;
	}

	public void add(Location loc)
	{
		add(loc.getX(), loc.getY(), loc.getZ(), loc.getZ());
	}

	public synchronized void add(int x, int y, int zmin, int zmax)
	{
		if(zmax == -1)
		{
			zmin = zmin - 50;
			zmax = zmin + 100;
		}
		Point[] newPoints = new Point[_points.length + 1];
		System.arraycopy(_points, 0, newPoints, 0, _points.length);
		newPoints[_points.length] = new Point(x, y, zmin, zmax);
		_points = newPoints;

		_poly.addPoint(x, y);

		if(x < _xMin)
			_xMin = x;
		if(y < _yMin)
			_yMin = y;
		if(x > _xMax)
			_xMax = x;
		if(y > _yMax)
			_yMax = y;
		if(zmin < _zMin)
			_zMin = zmin;
		if(zmax > _zMax)
			_zMax = zmax;
	}

	public void print()
	{
		for (Point p : _points)
			_log.debug("(" + p._x + "," + p._y + ")");
	}

	public boolean isInside(int x, int y)
	{
		return _poly.contains(x, y);
	}

	public boolean isInside(int x, int y, int z)
	{
		return z >= _zMin && z <= _zMax && _poly.contains(x, y);
	}

	public boolean isInside(Location loc)
	{
		return isInside(loc.getX(), loc.getY(), loc.getZ());
	}
	
	public boolean isIntersect(int x, int y, Point p1, Point p2)
	{
		double dy1 = p1._y - y;
		double dy2 = p2._y - y;

		if (Math.signum(dy1) == Math.signum(dy2))
			return false;

		double dx1 = p1._x - x;
		double dx2 = p2._x - x;

		if (dx1 >= 0 && dx2 >= 0)
			return true;

		if (dx1 < 0 && dx2 < 0)
			return false;

		double dx0 = (dy1 * (p1._x - p2._x)) / (p1._y - p2._y);

		return dx0 <= dx1;
	}
	
	public int[] getRandomPoint()
	{
		int[] p = new int[3];
		for (int i = 0; i < 100; i++)
		{
			p[0] = Rnd.get(_xMin, _xMax);
			p[1] = Rnd.get(_yMin, _yMax);
			if (_poly.contains(p[0], p[1]))
				break;
			// Для отлова проблемных территорый, вызывающих сильную нагрузку
			if(i == 80)
				_log.warn("L2Territory: Heavy territory " + _name + ", need manual correction");
		}
		p[2] = _zMin + (_zMax - _zMin)/2;
		return p;
	}

	public int getXmax()
	{return _xMax;}

	public int getXmin()
	{return _xMin;}

	public int getYmax()
	{return _yMax;}

	public int getYmin()
	{return _yMin;}

	public int getZmin()
	{return _zMin;}

	public int getZmax()
	{return _zMax;}
	
	public String getName()
	{ return _name;}
}