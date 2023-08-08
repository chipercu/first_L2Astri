package l2ft.geoserver.geodata.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGeoLoader implements GeoLoader
{
	final static Logger				_log				= LoggerFactory.getLogger(AbstractGeoLoader.class);

	private static final Pattern	SCANNER_DELIMITER	= Pattern.compile("([_|\\.]){1}");

	public boolean isAcceptable(File file)
	{
		if (!file.exists())
		{
			_log.info("Geo Engine: File " + file.getName() + " was not loaded!!! Reason: file doesn't exists.");
			return false;
		}

		if (file.isDirectory())
		{
			_log.info("Geo Engine: File " + file.getName() + " was not loaded!!! Reason: file is directory.");
			return false;
		}

		if (file.isHidden())
		{
			_log.info("Geo Engine: File " + file.getName() + " was not loaded!!! Reason: file is hidden.");
			return false;
		}

		if (file.length() > Integer.MAX_VALUE)
		{
			_log.info("Geo Engine: File " + file.getName() + " was not loaded!!! Reason: file is to big.");
			return false;
		}

		if (!getPattern().matcher(file.getName()).matches())
		{
			if (_log.isDebugEnabled())
				_log.info(getClass().getSimpleName() + ": can't load file: " + file.getName() + "!!! Reason: pattern missmatch");
			return false;
		}

		return true;
	}

	public GeoFileInfo readFile(File file)
	{

		_log.info(getClass().getSimpleName() + ": loading geodata file: " + file.getName());

		FileInputStream fis = null;
		byte[] data = null;
		try
		{
			fis = new FileInputStream(file);
			data = new byte[fis.available()];
			int readed = fis.read(data);
			if (readed != data.length)
				_log.warn("Not fully readed file?");
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			return null;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
		finally
		{
			try
			{
				if (fis != null)
					fis.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		GeoFileInfo geoFileInfo = createGeoFileInfo(file);
		geoFileInfo.setData(parse(convert(data)));
		return geoFileInfo;
	}

	protected GeoFileInfo createGeoFileInfo(File file)
	{
		Scanner scanner = new Scanner(file.getName());
		scanner.useDelimiter(SCANNER_DELIMITER);
		int ix = scanner.nextInt();
		int iy = scanner.nextInt();
		scanner.close();

		GeoFileInfo geoFileInfo = new GeoFileInfo();
		geoFileInfo.setX(ix);
		geoFileInfo.setY(iy);
		return geoFileInfo;
	}

	protected abstract byte[][] parse(byte[] data);

	public abstract Pattern getPattern();

	public abstract byte[] convert(byte[] data);
}