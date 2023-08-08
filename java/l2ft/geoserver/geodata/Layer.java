package l2ft.geoserver.geodata;

public class Layer
{
	public short	height;
	public byte		nswe;

	public Layer(short h, byte n)
	{
		height = h;
		nswe = n;
	}
}