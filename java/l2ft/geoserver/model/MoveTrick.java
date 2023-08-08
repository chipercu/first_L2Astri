package l2ft.geoserver.model;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class MoveTrick implements Serializable
{
	public int _dist;
	public int _height;
	
	public MoveTrick(int dist, int height)
	{
		_dist = dist;
		_height = height;
	}
}
