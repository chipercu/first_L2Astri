package l2ft.geoserver.geodata.loader;

import java.io.File;

public interface GeoLoader
{
	public boolean isAcceptable(File file);
	public GeoFileInfo readFile(File file);
}