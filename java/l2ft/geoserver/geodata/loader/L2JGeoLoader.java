package l2ft.geoserver.geodata.loader;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2ft.geoserver.geodata.GeoEngine;

public class L2JGeoLoader extends AbstractGeoLoader
{
	final static Logger				_log	= LoggerFactory.getLogger(L2JGeoLoader.class);

	private static final Pattern	PATTERN	= Pattern.compile("[\\d]{2}_[\\d]{2}.l2j");

	protected byte[][] parse(byte[] data)
	{
		if (data.length <= 196608) // 256 * 256 * 3 - it's minimal size of geodata (whole region with flat blocks)
			return null;

		byte[][] blocks = new byte[65536][]; // 256 * 256

		// Indexing geo files, so we will know where each block starts

		int index = 0;

		for (int block = 0, n = blocks.length; block < n; block++)
		{
			byte type = data[index];
			index++;

			byte[] geoBlock;
			switch (type)
			{
			case GeoEngine.BLOCKTYPE_FLAT:
				geoBlock = new byte[2 + 1];

				geoBlock[0] = type;
				geoBlock[1] = data[index];
				geoBlock[2] = data[index + 1];

				blocks[block] = geoBlock;
				index += 2;
				break;

			case GeoEngine.BLOCKTYPE_COMPLEX:
				if (index < data.length)
				{
					geoBlock = new byte[128 + 1];

					geoBlock[0] = type;
					System.arraycopy(data, index, geoBlock, 1, 128);

					index += 128;

					blocks[block] = geoBlock;
				}
				else
					_log.warn("L2JGeoLoader.parse BLOCKTYPE_COMPLEX format corrupt, skipped. index="+index+" data.length="+data.length);
				break;
				
			case GeoEngine.BLOCKTYPE_MULTILEVEL:
				int orgIndex = index;

				for (int b = 0; b < 64; b++)
				{
					byte layers = data[index];
					index += (layers << 1) + 1;
				}

				int diff = index - orgIndex;

				geoBlock = new byte[diff + 1];

				geoBlock[0] = type;
				System.arraycopy(data, orgIndex, geoBlock, 1, diff);

				blocks[block] = geoBlock;
				break;
			default:
				_log.error("GeoEngine: invalid block type: " + type);
			}
		}

		return blocks;
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