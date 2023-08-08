package l2ft.geoserver.geodata.loader;

import java.io.ByteArrayOutputStream;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2ft.geoserver.geodata.GeoEngine;

public class OffGeoLoader extends AbstractGeoLoader
{
	final static Logger				_log	= LoggerFactory.getLogger(OffGeoLoader.class);

	private static final Pattern	PATTERN	= Pattern.compile("[\\d]{2}_[\\d]{2}_conv.dat");

	protected byte[][] parse(byte[] data)
	{
		if (data.length <= 393234) // 18 + ((256 * 256) * (2 * 3)) - it's minimal size of geodata (whole region with flat blocks)
			return null;

		// Indexing geo files, so we will know where each block starts
		int index = 18; // Skip firs 18 bytes, they have nothing with data;

		byte[][] blocks = new byte[65536][]; // 256 * 256

		for (int block = 0, n = blocks.length; block < n; block++)
		{
			short type = makeShort(data[index + 1], data[index]);
			index += 2;

			byte[] geoBlock;
			if (type == 0)
			{
				geoBlock = new byte[2 + 1];

				geoBlock[0] = GeoEngine.BLOCKTYPE_FLAT;
				geoBlock[1] = data[index + 2];
				geoBlock[2] = data[index + 3];

				blocks[block] = geoBlock;
				index += 4;
			}
			else if (type == 0x0040)
			{
				if (index < data.length)
				{
					geoBlock = new byte[128 + 1];

					geoBlock[0] = GeoEngine.BLOCKTYPE_COMPLEX;
					System.arraycopy(data, index, geoBlock, 1, 128);

					index += 128;

					blocks[block] = geoBlock;
				}
				else
					_log.warn("OffGeoLoader.parse BLOCKTYPE_COMPLEX(type == 0x0040) format corrupt, skipped. index="+index+" data.length="+data.length);
			}
			else
			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				baos.write(GeoEngine.BLOCKTYPE_MULTILEVEL);

				for (int b = 0; b < 64; b++)
				{
					byte layers = (byte) makeShort(data[index + 1], data[index]);

					index += 2;

					baos.write(layers);
					for (int i = 0; i < layers << 1; i++)
						baos.write(data[index++]);
				}

				blocks[block] = baos.toByteArray();
			}
		}

		return blocks;
	}

	protected short makeShort(byte b1, byte b0)
	{
		return (short) (b1 << 8 | b0 & 0xff);
	}

	public Pattern getPattern()
	{
		return PATTERN;
	}

	public byte[] convert(byte[] data)
	{
		return data;
	}
}