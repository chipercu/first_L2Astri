package l2ft.geoserver.model;

public interface GeoShape
{
	public boolean isInside(int x, int y);
	public int getXmax();
	public int getXmin();
	public int getYmax();
	public int getYmin();
	public int getZmax();
	public int getZmin();
}
