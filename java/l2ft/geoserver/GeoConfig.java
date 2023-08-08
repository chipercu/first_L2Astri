package l2ft.geoserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.LogManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import l2ft.config.L2Properties;
import l2ft.commons.configuration.ExProperties;
import l2ft.geoserver.geodata.PathFindBuffers;

public final class GeoConfig
{
	private final static Logger	_log				= LoggerFactory.getLogger(GeoConfig.class);
	public static final String	CONFIGURATION_FILE	= "./config/geoserver.properties";

	public static int			GEODATA_MODE;
	public static String		SERVER_BIND_HOST;
	public static int			PORT;
	public static boolean		PATH_CLEAN 			= true;
    public static final int 	MAP_MIN_X 			= -327680;
	public static final int		MAP_MAX_X			= 229376;
	public static final int		MAP_MIN_Y			= -262144;
	public static final int		MAP_MAX_Y			= 294912;
	public static final int		MAP_MIN_Z			= -32768;
	public static final int		MAP_MAX_Z			= 32767;
	public static int			PURGE_INTERVAL;
	public static int			MAX_Z_DIFF = 64;//64(96)
	public static boolean		ALLOW_FALL_FROM_WALLS = false;
	public static int			MIN_LAYER_HEIGHT = 64;//64(96)
	public static int			PATHFIND_MAX_Z_DIFF = 32;//32(48)
	public static double		WEIGHT1	= 0.5;
	public static double		WEIGHT2	= 2.0;
	public static double		WEIGHT3	= 1.0;
	public static boolean		PATHFIND_DIAGONAL = false;
	public static int			TRICK_HEIGHT = 16;//16
	public static boolean		DEBUG = false;

	public static void loadConfiguration()
	{
		_log.info("loading " + CONFIGURATION_FILE);
		try
		{
			ExProperties serverSettings = load("./" + CONFIGURATION_FILE);
			SERVER_BIND_HOST = serverSettings.getProperty("GeoServerHost");
			PORT = serverSettings.getProperty("GeoPort", 0);
			GEODATA_MODE = serverSettings.getProperty("GeoData", 0);
			PATH_CLEAN = serverSettings.getProperty("PathClean", true);
			PURGE_INTERVAL = serverSettings.getProperty("PurgeInterval", 60);
			
			ALLOW_FALL_FROM_WALLS = serverSettings.getProperty("AllowFallFromWalls", false);
			PATHFIND_DIAGONAL = serverSettings.getProperty("PathFindDiagonal", false);
			MAX_Z_DIFF = serverSettings.getProperty("MaxZDiff", 64);
			MIN_LAYER_HEIGHT = serverSettings.getProperty("MinLayerHeight", 64);
			PATHFIND_MAX_Z_DIFF = serverSettings.getProperty("PathFindMaxZDiff", 32);
			TRICK_HEIGHT = serverSettings.getProperty("MinTrickHeight", 16);
			
			PathFindBuffers.initBuffers("8x100;8x128;8x192;4x256;2x320;2x384;1x500");
		}
		catch (Exception e)
		{
			_log.error(e.getMessage(), e);
			throw new Error("Failed to Load " + CONFIGURATION_FILE + " File.");
		}
	}

	public static ExProperties load(String filename)
	{
		return load(new File(filename));
	}

	public static ExProperties load(File file)
	{
		ExProperties result = new ExProperties();

		try
		{
			result.load(file);
		}
		catch(IOException e)
		{
			_log.error("Error loading config : " + file.getName() + "!");
		}

		return result;
	}

	public static final String	LOG_FILE		= "./config/logging.properties";
	final static String			LOG_FOLDER		= "log";							// Name of folder for log file
	final static String			LOG_FOLDER_GAME	= "game";

	public static void loadLogConfig()
	{
		try
		{
			InputStream is = new FileInputStream(new File(LOG_FILE));
			LogManager.getLogManager().readConfiguration(is);
			is.close();
		}
		catch (Exception e)
		{
			throw new Error("Failed to Load logging.properties File.");
		}
		_log.info("logging initialized");
		File logFolder = new File(LOG_FOLDER);
		logFolder.mkdir();
	}

}