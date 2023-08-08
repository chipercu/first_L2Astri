package l2ft.geoserver.geodata;

import java.io.File;
import java.util.Vector;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2ft.geoserver.GeoConfig;
import l2ft.geoserver.geodata.loader.GeoFileInfo;
import l2ft.geoserver.geodata.loader.GeoLoader;
import l2ft.geoserver.geodata.loader.GeoLoaderFactory;
import l2ft.geoserver.model.GeoCollision;
import l2ft.geoserver.model.GeoShape;
import l2ft.geoserver.model.Location;
import l2ft.geoserver.model.MoveTrick;
import l2ft.geoserver.util.Util;

public class GeoEngine
{
	final static Logger					_log					= LoggerFactory.getLogger(GeoEngine.class);

	public static final byte			BLOCKTYPE_FLAT			= 0;
	public static final byte			BLOCKTYPE_COMPLEX		= 1;
	public static final byte			BLOCKTYPE_MULTILEVEL	= 2;

	public static final byte			GEODATA_ARRAY_OFFSET_X	= 10;
	public static final byte			GEODATA_ARRAY_OFFSET_Y	= 10;

	public static final byte EAST = 1, WEST = 2, SOUTH = 4, NORTH = 8, NSWE_ALL = 15, NSWE_NONE = 0;
	public static int 					MAX_LAYERS 				= 64;
	private final GeoMove				geoMove					= new GeoMove(this);

	public static final int				geodataSizeX			= GeoConfig.MAP_MAX_X - GeoConfig.MAP_MIN_X + 1 >> 15;// div 2^15 32768
	public static final int				geodataSizeY			= GeoConfig.MAP_MAX_Y - GeoConfig.MAP_MIN_Y + 1 >> 15;

	protected static final byte[][][][]	geodata					= new byte[geodataSizeX][geodataSizeY][][];

	public short getType(int x, int y)
	{
		return NgetType(x - GeoConfig.MAP_MIN_X >> 4, y - GeoConfig.MAP_MIN_Y >> 4);
	}

	public int getHeight(int x, int y, int z)
	{
		return NgetHeight(x - GeoConfig.MAP_MIN_X >> 4, y - GeoConfig.MAP_MIN_Y >> 4, z);
	}

	public int getHeightGeo(int x, int y, int z)
	{
		return NgetHeight(x, y, z);
	}

	public int getHeight(Location pos)
	{
		return NgetHeight(pos.getX() - GeoConfig.MAP_MIN_X >> 4, pos.getY() - GeoConfig.MAP_MIN_Y >> 4, pos.getZ());
	}

    public byte getNSWE(int x, int y, int z)
    {
        return NgetNSWE(x - GeoConfig.MAP_MIN_X >> 4, y - GeoConfig.MAP_MIN_Y >> 4, z);
    }

	public boolean canMoveToCoord(int x, int y, int z, int tx, int ty, int tz, boolean returnPrev)
	{
		return canMoveToTarget(x, y, z, tx, ty, tz, returnPrev);
	}

    public Location moveCheckWithCollision(int x, int y, int z, int tx, int ty)
    {
        return MoveCheck(x, y, z, tx, ty, z, true, false, false, false);
    }

    public Location moveCheckWithCollision(int x, int y, int z, int tx, int ty, boolean returnPrev, boolean pathFind)
    {
        return MoveCheck(x, y, z, tx, ty, z, true, false, returnPrev, pathFind);
    }

    public Location moveCheckBackwardWithCollision(int x, int y, int z, int tx, int ty)
    {
        return MoveCheck(x, y, z, tx, ty, z, true, true, false, false);
    }

    public Location moveCheckBackwardWithCollision(int x, int y, int z, int tx, int ty, boolean returnPrev, boolean pathFind)
    {
        return MoveCheck(x, y, z, tx, ty, z, true, true, returnPrev, pathFind);
    }

	public Location moveCheck(int x, int y, int z, int tx, int ty, int tz, boolean returnPrev)
	{
		return MoveCheck(x, y , z, tx, ty, tz, false, false, returnPrev, false);
	}

	public Location moveCheckForAI(Location loc1, Location loc2)
	{
		return MoveCheck(loc1.getX(), loc1.getY(), loc1.getZ(), loc2.getY(), loc2.getY(), loc2.getZ(), false, false, true, false);
	}

	public Location moveInWaterCheck(int x, int y, int z, int tx, int ty, int tz, int colHeightActor, int colHeightTarget)
	{
		 return MoveInWaterCheck(x, y, z, tx, ty, tz);
        /*z += 64 + colHeightActor;
        tz += 64 + colHeightTarget;
        return canSee(mx, my, z, tmx, tmy, tz, true);*/
	}

	public boolean canSeeTarget(int x, int y, int z, int tx, int ty, int tz, boolean inAir, int colHeightActor, int colHeightTarget)
	{
		return canSeeCoord(x, y, z, tx, ty, tz, inAir, colHeightActor, colHeightTarget);
	}

    public boolean canSeeCoord(int x, int y, int z, int tx, int ty, int tz, boolean inAir, int colHeightActor, int colHeightTarget)
    {
        int mx = x - GeoConfig.MAP_MIN_X >> 4;
        int my = y - GeoConfig.MAP_MIN_Y >> 4;
        int tmx = tx - GeoConfig.MAP_MIN_X >> 4;
        int tmy = ty - GeoConfig.MAP_MIN_Y >> 4;
        z += 64 + colHeightActor;
        tz += 64 + colHeightTarget;
        return canSee(mx, my, z, tmx, tmy, tz, inAir).equals(tmx, tmy, tz) && canSee(tmx, tmy, tz, mx, my, z, inAir).equals(mx, my, z);
    }

	public boolean canMoveToTarget(int x, int y, int z, int tx, int ty, int tz, boolean returnPrev)
	{
		return canMove(x, y, z, tx, ty, tz, returnPrev) == 0;
	}

	public MoveTrick[] canMoveToTargetWithCollision(int x, int y, int z, int tx, int ty, int tz, boolean withCollision, boolean pathFind)
	{
		return canMoveAdv(x, y, z, tx, ty, tz, withCollision, pathFind);
	}

	public MoveTrick[] canMoveAdvanced(int x, int y, int z, int tx, int ty, int tz, boolean returnPrev)
	{
		return canMoveAdv(x, y, z, tx, ty, tz, returnPrev, false);
	}

	/**
	 * @param NSWE
	 * @param x
	 * @param y
	 * @param tx
	 * @param ty
	 *
	 * @return True if NSWE dont block given direction
	 */
	public static boolean checkNSWE(byte NSWE, int x, int y, int tx, int ty)
	{
		// Check NSWE
		if (NSWE == NSWE_ALL)
			return true;
		/*if(NSWE == NSWE_NONE)
			return false;*/

		if (tx > x) // E
		{
			if ((NSWE & EAST) == 0)
				return false;
		}
		else if (tx < x)
		{
			if ((NSWE & WEST) == 0)
				return false;
		}

		if (ty > y) // S
		{
			if ((NSWE & SOUTH) == 0)
				return false;
		}
		else if (ty < y)
		{
			if ((NSWE & NORTH) == 0)
				return false;
		}
		return true;
	}

	private static boolean NLOS_WATER(int x, int y, int z, int next_x, int next_y, int next_z)
	{
		Layer[] layers1 = NGetLayers(x, y);
		Layer[] layers2 = NGetLayers(next_x, next_y);
		if(layers1.length == 0 || layers2.length == 0)
			return true;

		// Находим ближайший к целевой клетке слой
		short z2 = Short.MIN_VALUE;
		for(Layer layer : layers2)
			if(Math.abs(next_z - z2) > Math.abs(next_z - layer.height))
				z2 = layer.height;

		//проверяем пересечение пола или потолка
		if ((z + 64 > z2 && next_z + 64 < z2) || (z + 64 < z2 && next_z + 64 > z2))
			return false;

		// Луч проходит над преградой
		if((next_z + 32) >= z2)
			return true;

		// Либо перед нами стена, ищет слой ниже для уточнения
		short z3 = Short.MIN_VALUE;
		for(Layer layer : layers2)
			if(layer.height < (z2 + 64) && Math.abs(next_z - z3) > Math.abs(next_z - layer.height))
				z3 = layer.height;

		// Ниже нет слоев, значит это стена
		if(z3 == Short.MIN_VALUE)
			return false;

		// Собираем данные о предыдущей клетке, игнорируя верхние слои
		short z1 = Short.MIN_VALUE;
		byte NSWE1 = NSWE_ALL;
		for(Layer layer : layers1)
			if(layer.height < z + 64 && Math.abs(z - z1) > Math.abs(z - layer.height))
			{
				z1 = layer.height;
				NSWE1 = layer.nswe;
			}

		// Невозможная ситуация, но все же...
		if(z1 < -30000)
			return true;

		/*short z4 = Short.MIN_VALUE;
		byte NSWE2 = NSWE_ALL;
		for(Layer layer : layers1)
			if(layer.height < next_z + 64 && Math.abs(next_z - z4) > Math.abs(next_z - layer.height))
			{
				z4 = layer.height;
				NSWE2 = layer.nswe;
			}
		*/
		// Если есть NSWE, то считаем за стену
		return (checkNSWE(NSWE1, x, y, next_x, next_y) /*&& checkNSWE(NSWE2, next_x, next_y, x, y)*/);
	}

	 private static short CheckNoOneLayerInRangeAndFindNearestLowerLayer(short layers[], int z0, int z1)
	 {
		 int z_min = z0;
		 int z_max = z1;
		 if(z0 > z1)
		 {
			 z_min = z1;
			 z_max = z0;
		 }
		 short nearest_layer = Short.MIN_VALUE;
		 short nearest_layer_h = Short.MIN_VALUE;
		 for(int i = 1; i <= layers[0]; i++)
		 {
			 short h = (short)((short)(layers[i] & 0x0fff0) >> 1);
			 if(z_min <= h && h <= z_max)
			 {
				 //if (GeoConfig.DEBUG)
					 //_log.info("CheckNoOneLayerInRangeAndFindNearestLowerLayer z0="+z0+" z1="+z1+" nearest_layer_h="+nearest_layer_h+" nearest_layer="+Short.MIN_VALUE);
				 return Short.MIN_VALUE;
			 }
			 if(h < z0 && nearest_layer_h < h)
			 {
				 nearest_layer_h = h;
				 nearest_layer = layers[i];
			 }
		 }
		 //if (GeoConfig.DEBUG)
		//	 _log.info("CheckNoOneLayerInRangeAndFindNearestLowerLayer z0="+z0+" z1="+z1+" nearest_layer_h="+nearest_layer_h+" nearest_layer="+nearest_layer);
		 return nearest_layer;
	 }

	 /*private static short CheckNoOneLayerInRangeAndFindNearestUpperLayer(short layers[], int height)
	 {
		 int z_min = height;
		 short nearest_layer = Short.MIN_VALUE;
		 for(int i = 1; i <= layers[0]; i++)
		 {
			 short h = (short)((short)(layers[i] & 0x0fff0) >> 1);
			 if(z_min > h) continue;
			 else return layers[i];
		 }
		 return nearest_layer;
	 }*/

    public static boolean canSeeWallCheck(Layer layer, Layer nearest_lower_neighbor, byte directionNSWE)
    {
        return (layer.nswe & directionNSWE) != 0 || layer.height <= nearest_lower_neighbor.height || Math.abs(layer.height - nearest_lower_neighbor.height) < GeoConfig.MAX_Z_DIFF; /*Max diff Z 64*/
    }

    public static boolean canSeeWallCheck(short[] layers, short layer, short nextLayer, byte directionNSWE, int curr_z, boolean air)
    {
        short layerh = (short)((short)(layer & 0x0fff0) >> 1);
		short nextLayerh = (short) ((short) (nextLayer & 0x0fff0) >> 1);
        if(air)
            return layer < curr_z;

        int zdiff = layerh - curr_z;
        int zdiffNext = nextLayerh - curr_z;
        int direct = ((layer & 0x0F) & directionNSWE);
        int directNext = ((nextLayer & 0x0F) & directionNSWE);
		//if (GeoConfig.DEBUG)
			//_log.info("canSeeWallCheck layerh="+layerh+" curr_z="+curr_z+" nextLayerh="+nextLayerh+" zdiff="+zdiff+" directionNSWE="+directionNSWE+/*" direct="+direct+*/" directNext="+directNext+" layer="+layer+" zdiffNext="+zdiffNext);
        //upper LoS
        if (zdiff > GeoConfig.MAX_Z_DIFF)
        	return false;

		//if (GeoConfig.DEBUG)
			//_log.info("canSeeWallCheck direct="+direct+" directNext="+directNext);
        //stop cell direction
        if (direct == 0 && (directNext == 0 && (nextLayerh -layerh) > GeoConfig.MAX_Z_DIFF/2  || directNext != 0 && (zdiffNext > GeoConfig.MAX_Z_DIFF || zdiffNext < -GeoConfig.MAX_Z_DIFF * 2)))
        {
            //land wall
    		if (GeoConfig.DEBUG)
    			_log.info("canSeeWallCheck (layerh - curr_z)="+(layerh - curr_z)+" GeoConfig.MAX_Z_DIFF / 2="+GeoConfig.MAX_Z_DIFF / 2);
            if ((layerh - curr_z) > GeoConfig.MAX_Z_DIFF / 2) 	return false;
            if ((layerh - nextLayerh) > GeoConfig.MAX_Z_DIFF / 4) 	return false;
        }
        return true;
        //return ((layer & 0x0F) & directionNSWE) != 0 || (zdiff > -GeoConfig.MAX_Z_DIFF /*&& zdiff != 0*/); /*Max diff Z 64, skip lower walls*/
    }

	private static Location canSee(int _x, int _y, int _z, int _tx, int _ty, int _tz, boolean air)
	{
		int diff_x = _tx - _x;
		int diff_y = _ty - _y;
		int diff_z = _tz - _z;
		int dx = Math.abs(diff_x);
		int dy = Math.abs(diff_y);
		float steps = Math.max(dx, dy);
		int curr_x = _x;
		int curr_y = _y;
		int curr_z = _z;
		short curr_layers[] = new short[MAX_LAYERS + 1];
		NGetLayers(curr_x, curr_y, curr_layers);
		Location result = new Location(_x, _y, _z);
		if (GeoConfig.DEBUG)
			_log.info("canSee start steps="+steps);
		if(steps == 0.0F)
		{
			if (GeoConfig.DEBUG)
				_log.info("canSee start steps="+steps+" CheckNoOneLayerInRangeAndFindNearestLowerLayer="+(CheckNoOneLayerInRangeAndFindNearestLowerLayer(curr_layers, curr_z, curr_z + diff_z) != Short.MIN_VALUE));
			if(CheckNoOneLayerInRangeAndFindNearestLowerLayer(curr_layers, curr_z, curr_z + diff_z) != Short.MIN_VALUE)
				result.setAll(_tx, _ty, _tz);
			return result;
		}
		float step_x = (float)diff_x / steps;
		float step_y = (float)diff_y / steps;
		float step_z = (float)diff_z / steps;
		int half_step_z = (int)(step_z / 2.0F);
		float next_x = curr_x;
		float next_y = curr_y;
		float next_z = curr_z;
		short tmp_layers[] = new short[MAX_LAYERS + 1];
		/*int startLayer = CheckNoOneLayerInRangeAndFindNearestLowerLayer(curr_layers, curr_z, curr_z + half_step_z);
		int startLayerH = curr_z;
		if (startLayer != Short.MIN_VALUE)
			startLayerH = (short)((short)(startLayer & 0x0fff0) >> 1);*/
		short src_nearest_lower_layer, dst_nearest_lower_layer;
		for(int i = 0; (float)i < steps; i++)
		{
			if(curr_layers[0] == 0)
			{
				if (GeoConfig.DEBUG)
					_log.info("canSee 2 curr_layers[0] == 0");
				result.setAll(_tx, _ty, _tz);
				return result;
			}
			next_x += step_x;
			next_y += step_y;
			next_z += step_z;
			int i_next_x = (int)(next_x + 0.5F);
			int i_next_y = (int)(next_y + 0.5F);
			int i_next_z = (int)(next_z + 0.5F);
			int middle_z = curr_z + half_step_z;
			int error = 0;
			if (GeoConfig.DEBUG)
				_log.info("canSee start curr_x="+curr_x+" curr_y="+curr_y+" curr_z="+curr_z+" next_x="+next_x+" next_y="+next_y+" next_z="+next_z);
			if((src_nearest_lower_layer = CheckNoOneLayerInRangeAndFindNearestLowerLayer(curr_layers, curr_z, middle_z)) == Short.MIN_VALUE)
			{
				if (GeoConfig.DEBUG)
					_log.info("canSee 40 layer corrupt src_nearest_lower_layer="+src_nearest_lower_layer);
				error ++;
				//return result;
			}
			NGetLayers(i_next_x, i_next_y, curr_layers);
			if(curr_layers[0] == 0)
			{
				if (GeoConfig.DEBUG)
					_log.info("canSee 40 curr_layers[0]");
				result.setAll(_tx, _ty, _tz);
				return result;
			}
			if((dst_nearest_lower_layer = CheckNoOneLayerInRangeAndFindNearestLowerLayer(curr_layers, i_next_z, middle_z)) == Short.MIN_VALUE)
			{
				if (GeoConfig.DEBUG)
					_log.info("canSee 40 layer corrupt dst_nearest_lower_layer="+dst_nearest_lower_layer+" error="+error);
				if (error > 0)
					return result;
				else
					error ++;
			}
			if(curr_x == i_next_x)
			{
				if(!canSeeWallCheck(curr_layers, src_nearest_lower_layer, dst_nearest_lower_layer, (i_next_y > curr_y ? SOUTH : NORTH), curr_z, air))
				{
					if (GeoConfig.DEBUG)
						_log.info("canSee 41");
					return result;
				}
			}
			else if(curr_y == i_next_y)
			{
				if(!canSeeWallCheck(curr_layers, src_nearest_lower_layer, dst_nearest_lower_layer, (i_next_x > curr_x ? EAST : WEST), curr_z, air))
				{
					if (GeoConfig.DEBUG)
						_log.info("canSee 42");
					return result;
				}
			}

			if (GeoConfig.DEBUG)
				_log.info("canSee check temp 1");
			NGetLayers(curr_x, i_next_y, tmp_layers);
			if(tmp_layers[0] == 0)
			{
				if (GeoConfig.DEBUG)
					_log.info("canSee tmp 1 curr_layers[0] == 0=");
				result.setAll(_tx, _ty, _tz);
				return result;
			}
			short tmp_nearest_lower_layer;
			if((tmp_nearest_lower_layer = CheckNoOneLayerInRangeAndFindNearestLowerLayer(tmp_layers, i_next_z, middle_z)) == Short.MIN_VALUE)
			{
				if (GeoConfig.DEBUG)
					_log.info("canSee tmp 11 layer corrupt tmp_nearest_lower_layer="+tmp_nearest_lower_layer+" error="+error);
				if (error > 0)
					return result;
				else
					error ++;
			}
			if(!canSeeWallCheck(tmp_layers, src_nearest_lower_layer, tmp_nearest_lower_layer, (i_next_x > curr_x ? EAST : WEST), curr_z, air))
			{
				if (GeoConfig.DEBUG)
					_log.info("canSee tmp 12");
				return result;
			}
			if(!canSeeWallCheck(tmp_layers, tmp_nearest_lower_layer, dst_nearest_lower_layer, (i_next_y > curr_y ? SOUTH : NORTH), curr_z, air))
			{
				if (GeoConfig.DEBUG)
					_log.info("canSee tmp 13");
				return result;
			}

			if (GeoConfig.DEBUG)
				_log.info("canSee check temp 2");
			NGetLayers(i_next_x, curr_y, tmp_layers);
			if(tmp_layers[0] == 0)
			{
				if (GeoConfig.DEBUG)
					_log.info("canSee tmp 2 curr_layers[0] == 0=");
				result.setAll(_tx, _ty, _tz);
				return result;
			}
			if((tmp_nearest_lower_layer = CheckNoOneLayerInRangeAndFindNearestLowerLayer(tmp_layers, i_next_z, middle_z)) == Short.MIN_VALUE)
			{
				if (GeoConfig.DEBUG)
					_log.info("canSee tmp 21 layer corrupt tmp_nearest_lower_layer="+tmp_nearest_lower_layer+" error="+error);
				if (error > 0)
					return result;
				else
					error ++;
			}
			if(!canSeeWallCheck(tmp_layers, src_nearest_lower_layer, tmp_nearest_lower_layer, (i_next_x > curr_x ? EAST : WEST), curr_z, air))
			{
				if (GeoConfig.DEBUG)
					_log.info("canSee tmp 22");
				return result;
			}
			if(!canSeeWallCheck(tmp_layers, tmp_nearest_lower_layer, dst_nearest_lower_layer, (i_next_y > curr_y ? SOUTH : NORTH), curr_z, air))
			{
				if (GeoConfig.DEBUG)
					_log.info("canSee tmp 23");
				return result;
			}
			result.setAll(curr_x, curr_y, curr_z);
			curr_x = i_next_x;
			curr_y = i_next_y;
			curr_z = i_next_z;
		}

		if (GeoConfig.DEBUG)
			_log.info("canSee true");
		result.setAll(_tx, _ty, _tz);
		return result;
	}

	 public static Layer[] NGetLayers(int geoX, int geoY)
	 {
	        byte block[] = getGeoBlockFromGeoCoords(geoX, geoY);
	        if(block == null)
	            return new Layer[0];
	        int index = 0;
	        byte type = block[index];
	        index++;
	        switch(type)
	        {
	        case 0: // '\0'
	        {
	            short height = makeShort(block[index + 1], block[index]);
				height = (short) (height & 0x0fff0);
	            return (new Layer[] {new Layer(height, NSWE_ALL)});
	        }

	        case 1: // '\001'
	        {
	            int cellX = getCell(geoX);
	            int cellY = getCell(geoY);
	            index += (cellX << 3) + cellY << 1;
	            short height = makeShort(block[index + 1], block[index]);
	            return (new Layer[] {
	                new Layer((short)((short)(height & 0xfff0) >> 1), (byte)(height & 0xf))
	            });
	        }

	        case 2: // '\002'
	        {
	            int cellX = getCell(geoX);
	            int cellY = getCell(geoY);
	            for(int offset = (cellX << 3) + cellY; offset > 0; offset--)
	            {
	                byte lc = block[index];
	                index += (lc << 1) + 1;
	            }

	            byte layer_count = block[index];
	            index++;
	            if(layer_count <= 0 || layer_count > MAX_LAYERS)
	                return new Layer[0];
	            Layer layers[] = new Layer[layer_count];
	            while(layer_count > 0)
	            {
	                short height = makeShort(block[index + 1], block[index]);
	                layer_count--;
	                layers[layer_count] = new Layer((short)((short)(height & 0xfff0) >> 1), (byte)(height & 0xf));
	                index += 2;
	            }
	            return layers;
	        }
	        }
	        _log.warn("GeoEngine: Unknown block type");
	        return new Layer[0];
	 }

	 public static void NGetLayers(int geoX, int geoY, short result[])
	 {
	        result[0] = 0;
	        byte block[] = getGeoBlockFromGeoCoords(geoX, geoY);
	        if(block == null)
	            return;
	        int index = 0;
	        byte type = block[index];
	        index++;
	        switch(type)
	        {
	        case 0: // '\0'
	        {
	            short height = makeShort(block[index + 1], block[index]);
	            height &= 0xfff0;
	            result[0]++;
	            result[1] = (short)((short)(height << 1) | NSWE_ALL);
	            return;
	        }

	        case 1: // '\001'
	        {
	            int cellX = getCell(geoX);
	            int cellY = getCell(geoY);
	            index += (cellX << 3) + cellY << 1;
	            short height = makeShort(block[index + 1], block[index]);
	            result[0]++;
	            result[1] = height;
	            return;
	        }

	        case 2: // '\002'
	        {
	            int cellX = getCell(geoX);
	            int cellY = getCell(geoY);
	            for(int offset = (cellX << 3) + cellY; offset > 0; offset--)
	            {
	                byte lc = block[index];
	                index += (lc << 1) + 1;
	            }

	            byte layer_count = block[index];
	            index++;
	            if(layer_count <= 0 || layer_count > MAX_LAYERS)
	                return;
	            result[0] = layer_count;
	            while(layer_count > 0)
	            {
	                result[layer_count] = makeShort(block[index + 1], block[index]);
	                layer_count--;
	                index += 2;
	            }
	            return;
	        }
	        }
	        _log.warn("GeoEngine: Unknown block type");
	 }

	private static Location MoveInWaterCheck(int _x, int _y, int z, int _tx, int _ty, int tz)
	{
        int x = _x - GeoConfig.MAP_MIN_X >> 4;
        int y = _y - GeoConfig.MAP_MIN_Y >> 4;
        int tx = _tx - GeoConfig.MAP_MIN_X >> 4;
        int ty = _ty - GeoConfig.MAP_MIN_Y >> 4;

        int diff_x = tx - x;
        int diff_y = ty - y;
        int diff_z = tz - z;
        int dx = Math.abs(diff_x);
        int dy = Math.abs(diff_y);
        int dz = Math.abs(diff_z);
        float steps = Math.max(dx, dy);
        if(steps == 0.0F)
        {
        	if (dz <= 4)
        		return new Location(_tx, _ty, tz);
        	else
        		return new Location(_x, _y, z);
        }

        int curr_x = x;
        int curr_y = y;
        int curr_z = z;
        float step_x = (float)diff_x / steps;
        float step_y = (float)diff_y / steps;
        float step_z = (float)diff_z / steps;
        float next_x = curr_x;
        float next_y = curr_y;
        float next_z = curr_z;
        for(int i = 0; (float)i < steps; i++)
        {
            next_x += step_x;
            next_y += step_y;
            next_z += step_z;
            int i_next_x = (int)(next_x + 0.5F);
            int i_next_y = (int)(next_y + 0.5F);
            int i_next_z = (int)(next_z + 0.5F);

			if(!NLOS_WATER(curr_x, curr_y, curr_z, i_next_x, i_next_y, i_next_z))
				return new Location(curr_x, curr_y, curr_z).geo2world();

            curr_x = i_next_x;
            curr_y = i_next_y;
            curr_z = i_next_z;
        }

		return new Location(curr_x, curr_y, curr_z).geo2world();
	}

    private static int canMove(int __x, int __y, int _z, int __tx, int __ty, int _tz, boolean withCollision)
    {
        int _x = __x - GeoConfig.MAP_MIN_X >> 4;
        int _y = __y - GeoConfig.MAP_MIN_Y >> 4;
        int _tx = __tx - GeoConfig.MAP_MIN_X >> 4;
        int _ty = __ty - GeoConfig.MAP_MIN_Y >> 4;
        int diff_x = _tx - _x;
        int diff_y = _ty - _y;
        int diff_z = _tz - _z;
        int dx = Math.abs(diff_x);
        int dy = Math.abs(diff_y);
        int dz = Math.abs(diff_z);
        float steps = Math.max(dx, dy);
        if(steps == 0.0F)
        {
        	if (dz <= 4)
        		return 0;
        	else
        		return -5;
        }
        int curr_x = _x;
        int curr_y = _y;
        int curr_z = _z;
        short curr_layers[] = new short[MAX_LAYERS + 1];
        NGetLayers(curr_x, curr_y, curr_layers);
        if(curr_layers[0] == 0)
            return 0;
        float step_x = (float)diff_x / steps;
        float step_y = (float)diff_y / steps;
        float next_x = curr_x;
        float next_y = curr_y;
        short next_layers[] = new short[MAX_LAYERS + 1];
        short temp_layers[] = new short[MAX_LAYERS + 1];
        for(int i = 0; (float)i < steps; i++)
        {
            next_x += step_x;
            next_y += step_y;
            int i_next_x = (int)(next_x + 0.5F);
            int i_next_y = (int)(next_y + 0.5F);
            NGetLayers(i_next_x, i_next_y, next_layers);
            if((curr_z = NcanMoveNext(curr_x, curr_y, curr_z, curr_layers, i_next_x, i_next_y, next_layers, temp_layers, withCollision, GeoConfig.MAX_Z_DIFF)) == 0x80000000)
                return 1;
            short curr_next_switcher[] = curr_layers;
            curr_layers = next_layers;
            next_layers = curr_next_switcher;
            curr_x = i_next_x;
            curr_y = i_next_y;
        }

        diff_z = _tz - curr_z;
        dz = Math.abs(diff_z);
        //_log.info("canMove diff_z="+diff_z+" dz="+dz);
        if(GeoConfig.ALLOW_FALL_FROM_WALLS)
            return diff_z >= GeoConfig.MAX_Z_DIFF ? diff_z * 10000 : 0;
        else
            return dz <= GeoConfig.MAX_Z_DIFF ? 0 : dz * 1000;
    }

    private static MoveTrick[] canMoveAdv(int __x, int __y, int _z, int __tx, int __ty, int _tz, boolean withCollision, boolean pathFind)
    {
    	int _dx1 = __x - __tx;
    	int _dy1 = __y - __ty;
    	int dist = (int)Math.sqrt(_dx1 * _dx1 + _dy1 * _dy1);
        int _x = __x - GeoConfig.MAP_MIN_X >> 4;
        int _y = __y - GeoConfig.MAP_MIN_Y >> 4;
        int _tx = __tx - GeoConfig.MAP_MIN_X >> 4;
        int _ty = __ty - GeoConfig.MAP_MIN_Y >> 4;
        int diff_x = _tx - _x;
        int diff_y = _ty - _y;
        int diff_z = _tz - _z;
        int dx = Math.abs(diff_x);
        int dy = Math.abs(diff_y);
        int dz = Math.abs(diff_z);
        float steps = Math.max(dx, dy);
        MoveTrick[] result = {new MoveTrick(dist, _tz)};
        if(steps == 0.0F)
        {
        	if (GeoConfig.DEBUG)
        		_log.info("canMoveAdv _x="+_x+" _y="+_y+" _tx="+_tx+" _ty="+_ty+" dx="+dx+" dy="+dy+" steps="+steps+" dz="+dz+" NgetHeight(_x, _y, _z) == NgetHeight(_tx, _ty, _tz)");
        	if (NgetHeight(_x, _y, _z) == NgetHeight(_tx, _ty, _tz))
        		return result;
        	else
        		return null;
        }
        int curr_x = _x;
        int curr_y = _y;
        int curr_z = _z;
        int i_next_z = curr_z;
        short curr_layers[] = new short[MAX_LAYERS + 1];
        NGetLayers(curr_x, curr_y, curr_layers);
        if (GeoConfig.DEBUG)
        	_log.info("canMoveAdv _tricks.length="+result.length+" height="+result[0]._height+" dist="+result[0]._dist+" curr_layers[0]="+curr_layers[0]);
        if(curr_layers[0] == 0)
            return result;
        float step_x = (float)diff_x / steps;
        float step_y = (float)diff_y / steps;
        float next_x = curr_x;
        float next_y = curr_y;
        short next_layers[] = new short[MAX_LAYERS + 1];
        short temp_layers[] = new short[MAX_LAYERS + 1];
        int trick_z = curr_z;
        int trickI = 0;
        int MAX_Z_DIFF = GeoConfig.MAX_Z_DIFF;
        if (pathFind) MAX_Z_DIFF = GeoConfig.PATHFIND_MAX_Z_DIFF;
        for(int i = 0; (float)i < steps; i++)
        {
            next_x += step_x;
            next_y += step_y;
            int i_next_x = (int)(next_x + 0.5F);
            int i_next_y = (int)(next_y + 0.5F);
            NGetLayers(i_next_x, i_next_y, next_layers);
            if((i_next_z = NcanMoveNext(curr_x, curr_y, curr_z, curr_layers, i_next_x, i_next_y, next_layers, temp_layers, withCollision, MAX_Z_DIFF)) == 0x80000000)
            {
            	if (GeoConfig.DEBUG)
                	_log.info("canMoveAdv _tricks= null exit NcanMoveNext = 0x80000000 i_next_z="+i_next_z+" curr_x="+curr_x+" curr_y="+curr_y+" curr_layers[0]="+curr_layers[0]+" i_next_x="+i_next_x+" i_next_y="+i_next_y+" next_layers[0]="+next_layers[0]+" temp_layers[0]="+temp_layers[0]+" withCollision="+withCollision+" MAX_Z_DIFF="+MAX_Z_DIFF);
                return null;
            }
            short curr_next_switcher[] = curr_layers;
            curr_layers = next_layers;
            next_layers = curr_next_switcher;
            if (!GeoConfig.ALLOW_FALL_FROM_WALLS && (i_next_z - curr_z) > MAX_Z_DIFF) //up move filed
            	return null;
            else
            	curr_z = i_next_z;
            curr_x = i_next_x;
            curr_y = i_next_y;
            if (Math.abs(curr_z - trick_z) >= GeoConfig.TRICK_HEIGHT)
            {
            	trick_z = curr_z;
            	int dx1 = ((curr_x << 4) + GeoConfig.MAP_MIN_X + 8) - __x;
            	int dy1 = ((curr_y << 4) + GeoConfig.MAP_MIN_Y + 8) - __y;
            	int dist2 = (int)Math.sqrt(dx1 * dx1 + dy1 * dy1);
            	if (trickI == 0)
            	{
                	result[trickI]._dist = 0;
            		result[trickI]._height = _z; /*first trick for many tricks = start Z*/
            	}
            	else
                	result[trickI]._dist = dist2;
            	trickI ++;
            	result = ArrayUtils.add(result, new MoveTrick(dist, trick_z));
                //_log.info("canMoveAdv1 _tricks.length="+result.length+" height="+result[trickI]._height+" dist="+result[trickI]._dist);
            }
        }

        /*diff_z = _tz - curr_z;
        dz = Math.abs(diff_z);
        if(GeoConfig.ALLOW_FALL_FROM_WALLS)
            return diff_z > GeoConfig.MAX_Z_DIFF ? null : result;
        else
            return dz <= GeoConfig.MAX_Z_DIFF ? result : null;*/
        if (result.length == 1 && !GeoConfig.ALLOW_FALL_FROM_WALLS && Math.abs(_tz - curr_z) > MAX_Z_DIFF)
        	return null;

        return result;
    }

    private static Location MoveCheck(int __x, int __y, int _z, int __tx, int __ty, int _tz, boolean withCollision, boolean backwardMove, boolean returnPrev, boolean pathFind)
    {
        int _x = __x - GeoConfig.MAP_MIN_X >> 4;
        int _y = __y - GeoConfig.MAP_MIN_Y >> 4;
        int _tx = __tx - GeoConfig.MAP_MIN_X >> 4;
        int _ty = __ty - GeoConfig.MAP_MIN_Y >> 4;
        int diff_x = _tx - _x;
        int diff_y = _ty - _y;
        int dx = Math.abs(diff_x);
        int dy = Math.abs(diff_y);
        float steps = Math.max(dx, dy);
        if(steps == 0.0F)
        {
        	if (NgetHeight(_x, _y, _z) == NgetHeight(_tx, _ty, _tz))
        		return new Location(__tx, __ty, _tz);
        	else
        		return new Location(__x, __y, _z);
        }
        float step_x = (float)diff_x / steps;
        float step_y = (float)diff_y / steps;
        int curr_x = _x;
        int curr_y = _y;
        int curr_z = _z;
        float next_x = curr_x;
        float next_y = curr_y;
        int i_next_z = curr_z;
        short next_layers[] = new short[MAX_LAYERS + 1];
        short temp_layers[] = new short[MAX_LAYERS + 1];
        short curr_layers[] = new short[MAX_LAYERS + 1];
        NGetLayers(curr_x, curr_y, curr_layers);
        int prev_x = curr_x;
        int prev_y = curr_y;
        int prev_z = curr_z;
        int MAX_Z_DIFF = GeoConfig.MAX_Z_DIFF;
        if (pathFind) MAX_Z_DIFF = GeoConfig.PATHFIND_MAX_Z_DIFF;
        for(int i = 0; (float)i < steps; i++)
        {
            next_x += step_x;
            next_y += step_y;
            int i_next_x = (int)(next_x + 0.5F);
            int i_next_y = (int)(next_y + 0.5F);
            NGetLayers(i_next_x, i_next_y, next_layers);
            if((i_next_z = NcanMoveNext(curr_x, curr_y, curr_z, curr_layers, i_next_x, i_next_y, next_layers, temp_layers, withCollision, MAX_Z_DIFF)) == 0x80000000 || backwardMove && NcanMoveNext(i_next_x, i_next_y, i_next_z, next_layers, curr_x, curr_y, curr_layers, temp_layers, withCollision, MAX_Z_DIFF) == 0x80000000)
                break;
            short curr_next_switcher[] = curr_layers;
            curr_layers = next_layers;
            next_layers = curr_next_switcher;
            if (!GeoConfig.ALLOW_FALL_FROM_WALLS && Math.abs(i_next_z - curr_z) > MAX_Z_DIFF)
            	break;
            if(returnPrev)
            {
                prev_x = curr_x;
                prev_y = curr_y;
                prev_z = curr_z;
            }
            curr_x = i_next_x;
            curr_y = i_next_y;
            curr_z = i_next_z;
        }

        if(returnPrev)
        {
            curr_x = prev_x;
            curr_y = prev_y;
            curr_z = prev_z;
        }

        return (new Location(curr_x, curr_y, curr_z)).geo2world();
    }

    private static int FindNearestLowerLayer(short layers[], int z)
    {
        short nearest_layer_h = -32768;
        int nearest_layer = 0x80000000;
        for(int i = 1; i <= layers[0]; i++)
        {
            short h = (short)((short)(layers[i] & 0xfff0) >> 1);
            if(h < z && nearest_layer_h < h)
            {
                nearest_layer_h = h;
                nearest_layer = layers[i];
            }
        }

        return nearest_layer;
    }

    private static boolean NcanMoveNextExCheck(int x, int y, int h, int nextx, int nexty, int hexth, short temp_layers[], int MAX_Z_DIFF)
    {
        NGetLayers(x, y, temp_layers);
        if(temp_layers[0] == 0)
            return true;
        int temp_layer;
        if((temp_layer = FindNearestLowerLayer(temp_layers, h + MAX_Z_DIFF/*GeoConfig.MIN_LAYER_HEIGHT*/)) == 0x80000000)
            return false;
        short temp_layer_h = (short)((short)(temp_layer & 0xfff0) >> 1);
        if(Math.abs(temp_layer_h - hexth) >= MAX_Z_DIFF || Math.abs(temp_layer_h - h) >= MAX_Z_DIFF)
            return false;
        else
            return checkNSWE((byte)(temp_layer & 0xf), x, y, nextx, nexty);
    }

    public static int NcanMoveNext(int x, int y, int z, short layers[], int next_x, int next_y, short next_layers[], short temp_layers[], boolean withCollision, int MAX_Z_DIFF)
    {
    	if(layers[0] == 0 || next_layers[0] == 0)
            return z;
    	//_log.info("NcanMoveNext z="+z+" FindNearestLowerLayer(layers, z + GeoConfig.MIN_LAYER_HEIGHT)="+FindNearestLowerLayer(layers, z + MAX_Z_DIFF));
        int layer;
        if((layer = FindNearestLowerLayer(layers, z + MAX_Z_DIFF /*GeoConfig.MIN_LAYER_HEIGHT*/)) == 0x80000000)
            return 0x80000000;
        byte layer_nswe = (byte)(layer & 0xf);
        if(!checkNSWE(layer_nswe, x, y, next_x, next_y))
            return 0x80000000;
        short layer_h = (short)((short)(layer & 0xfff0) >> 1);
    	//_log.info("NcanMoveNext z="+z+" FindNearestLowerLayer(next_layers, layer_h + GeoConfig.MIN_LAYER_HEIGHT)="+FindNearestLowerLayer(next_layers, layer_h + MAX_Z_DIFF));
        int next_layer;
        if((next_layer = FindNearestLowerLayer(next_layers, layer_h + MAX_Z_DIFF /*GeoConfig.MIN_LAYER_HEIGHT*/)) == 0x80000000)
            return 0x80000000;
        short next_layer_h = (short)((short)(next_layer & 0xfff0) >> 1);
        if(x == next_x || y == next_y)
        {
            if(withCollision)
            {
                if(x == next_x)
                {
                    NgetHeightAndNSWE(x - 1, y, layer_h, temp_layers);
                    if(Math.abs(temp_layers[0] - layer_h) > 15 || !checkNSWE(layer_nswe, x - 1, y, x, y) || !checkNSWE((byte)temp_layers[1], x - 1, y, x - 1, next_y))
                        return 0x80000000;
                    NgetHeightAndNSWE(x + 1, y, layer_h, temp_layers);
                    if(Math.abs(temp_layers[0] - layer_h) > 15 || !checkNSWE(layer_nswe, x + 1, y, x, y) || !checkNSWE((byte)temp_layers[1], x + 1, y, x + 1, next_y))
                        return 0x80000000;
                    else
                        return next_layer_h;
                }
                else
                {
                    NgetHeightAndNSWE(x, y - 1, layer_h, temp_layers);
                    if(Math.abs(temp_layers[0] - layer_h) >= MAX_Z_DIFF || !checkNSWE(layer_nswe, x, y - 1, x, y) || !checkNSWE((byte)temp_layers[1], x, y - 1, next_x, y - 1))
                        return 0x80000000;
                    NgetHeightAndNSWE(x, y + 1, layer_h, temp_layers);
                    if(Math.abs(temp_layers[0] - layer_h) >= MAX_Z_DIFF || !checkNSWE(layer_nswe, x, y + 1, x, y) || !checkNSWE((byte)temp_layers[1], x, y + 1, next_x, y + 1))
                        return 0x80000000;
                }
            }
            return next_layer_h;
        }
    	//_log.info("NcanMoveNext z="+z+" 1 NcanMoveNextExCheck="+NcanMoveNextExCheck(x, next_y, layer_h, next_x, next_y, next_layer_h, temp_layers, MAX_Z_DIFF));
        if(!NcanMoveNextExCheck(x, next_y, layer_h, next_x, next_y, next_layer_h, temp_layers, MAX_Z_DIFF))
            return 0x80000000;
    	//_log.info("NcanMoveNext z="+z+" 2 NcanMoveNextExCheck="+NcanMoveNextExCheck(next_x, y, layer_h, next_x, next_y, next_layer_h, temp_layers, MAX_Z_DIFF));
        if(!NcanMoveNextExCheck(next_x, y, layer_h, next_x, next_y, next_layer_h, temp_layers, MAX_Z_DIFF))
            return 0x80000000;
        else
            return next_layer_h;
    }

    public static void NgetHeightAndNSWE(int geoX, int geoY, short z, short result[])
    {
        byte block[] = getGeoBlockFromGeoCoords(geoX, geoY);
        if(block == null)
        {
            result[0] = z;
            result[1] = 15;
            return;
        }
        int index = 0;
        short NSWE = 15;
        byte type = block[index];
        index++;
        switch(type)
        {
        case 0: // '\0'
        {
            short height = makeShort(block[index + 1], block[index]);
            result[0] = (short)(height & 0xfff0);
            result[1] = 15;
            return;
        }

        case 1: // '\001'
        {
            int cellX = getCell(geoX);
            int cellY = getCell(geoY);
            index += (cellX << 3) + cellY << 1;
            short height = makeShort(block[index + 1], block[index]);
            result[0] = (short)((short)(height & 0xfff0) >> 1);
            result[1] = (short)(height & 0x0F);
            return;
        }

        case 2: // '\002'
        {
            int cellX = getCell(geoX);
            int cellY = getCell(geoY);
            for(int offset = (cellX << 3) + cellY; offset > 0; offset--)
            {
                byte lc = block[index];
                index += (lc << 1) + 1;
            }

            byte layers = block[index];
            index++;
            if(layers <= 0 || layers > MAX_LAYERS)
            {
                result[0] = z;
                result[1] = 15;
                return;
            }
            short tempz1 = -32768;
            short tempz2 = -32768;
            int index_nswe1 = 0;
            int index_nswe2 = 0;
            int z_nearest_lower_limit = z + GeoConfig.MIN_LAYER_HEIGHT;
            while(layers > 0)
            {
                short height = (short)((short)(makeShort(block[index + 1], block[index]) & 0xfff0) >> 1);
                if(height < z_nearest_lower_limit)
                {
                    if(height > tempz1)
                    {
                        tempz1 = height;
                        index_nswe1 = index;
                    }
                } else
                if(Math.abs(z - height) < Math.abs(z - tempz2))
                {
                    tempz2 = height;
                    index_nswe2 = index;
                }
                layers--;
                index += 2;
            }
            if(index_nswe1 > 0)
            {
                NSWE = makeShort(block[index_nswe1 + 1], block[index_nswe1]);
                NSWE &= 0x0F;
            } else
            if(index_nswe2 > 0)
            {
                NSWE = makeShort(block[index_nswe2 + 1], block[index_nswe2]);
                NSWE &= 0x0F;
            }
            result[0] = tempz1 <= -32768 ? tempz2 : tempz1;
            result[1] = NSWE;
            return;
        }
        }
        _log.warn("GeoEngine: Unknown block type.");
        result[0] = z;
        result[1] = 15;
    }

	private short NgetType(int geoX, int geoY)
	{
		byte[] block = getGeoBlockFromGeoCoords(geoX, geoY);

		if (block == null)
			return 0;

		return block[0];
	}

    private static int NgetHeight(int geoX, int geoY, int z)
    {
        byte block[] = getGeoBlockFromGeoCoords(geoX, geoY);
        if(block == null)
            return z;
        int index = 0;
        byte type = block[index];
        index++;
        switch(type)
        {
        case 0: // '\0'
        {
            short height = makeShort(block[index + 1], block[index]);
            return (short)(height & 0xfff0);
        }

        case 1: // '\001'
        {
            int cellX = getCell(geoX);
            int cellY = getCell(geoY);
            index += (cellX << 3) + cellY << 1;
            short height = makeShort(block[index + 1], block[index]);
            return (short)((short)(height & 0xfff0) >> 1);
        }

        case 2: // '\002'
        {
            int cellX = getCell(geoX);
            int cellY = getCell(geoY);
            for(int offset = (cellX << 3) + cellY; offset > 0; offset--)
            {
                byte lc = block[index];
                index += (lc << 1) + 1;
            }

            byte layers = block[index];
            index++;
            if(layers <= 0 || layers > MAX_LAYERS)
                return (short)z;
            int z_nearest_lower_limit = z + GeoConfig.MIN_LAYER_HEIGHT;
            int z_nearest_lower = 0x80000000;
            int z_nearest = 0x80000000;
            while(layers > 0)
            {
                short height = (short)((short)(makeShort(block[index + 1], block[index]) & 0xfff0) >> 1);
                if(height < z_nearest_lower_limit)
                    z_nearest_lower = Math.max(z_nearest_lower, height);
                else
                if(Math.abs(z - height) < Math.abs(z - z_nearest))
                    z_nearest = height;
                layers--;
                index += 2;
            }
            return z_nearest_lower == 0x80000000 ? z_nearest : z_nearest_lower;
        }
        }
        _log.warn("GeoEngine: Unknown blockType");
        return z;
    }

    public static byte NgetNSWE(int geoX, int geoY, int z)
    {
        byte block[] = getGeoBlockFromGeoCoords(geoX, geoY);
        if(block == null)
            return 15;
        int index = 0;
        byte type = block[index];
        index++;
        switch(type)
        {
        case 0: // '\0'
        {
            return 15;
        }

        case 1: // '\001'
        {
            int cellX = getCell(geoX);
            int cellY = getCell(geoY);
            index += (cellX << 3) + cellY << 1;
            short height = makeShort(block[index + 1], block[index]);
            return (byte)(height & 0x0F);
        }

        case 2: // '\002'
        {
            int cellX = getCell(geoX);
            int cellY = getCell(geoY);
            for(int offset = (cellX << 3) + cellY; offset > 0; offset--)
            {
                byte lc = block[index];
                index += (lc << 1) + 1;
            }

            byte layers = block[index];
            index++;
            if(layers <= 0 || layers > MAX_LAYERS)
                return 15;
            short tempz1 = -32768;
            short tempz2 = -32768;
            int index_nswe1 = 0;
            int index_nswe2 = 0;
            int z_nearest_lower_limit = z + GeoConfig.MIN_LAYER_HEIGHT;
            while(layers > 0)
            {
                short height = (short)((short)(makeShort(block[index + 1], block[index]) & 0xfff0) >> 1);
                if(height < z_nearest_lower_limit)
                {
                    if(height > tempz1)
                    {
                        tempz1 = height;
                        index_nswe1 = index;
                    }
                } else
                if(Math.abs(z - height) < Math.abs(z - tempz2))
                {
                    tempz2 = height;
                    index_nswe2 = index;
                }
                layers--;
                index += 2;
            }
            if(index_nswe1 > 0)
                return (byte)(makeShort(block[index_nswe1 + 1], block[index_nswe1]) & 0x0F);
            if(index_nswe2 > 0)
                return (byte)(makeShort(block[index_nswe2 + 1], block[index_nswe2]) & 0x0F);
            else
                return 15;
        }
        }
        _log.warn("GeoEngine: Unknown block type.");
        return 15;
    }
	protected static short makeShort(byte b1, byte b0)
	{
		return (short) (b1 << 8 | b0 & 0xff);
	}

	protected static int getBlock(int geoPos)
	{
		return (geoPos >> 3) % 256;
	}

	protected static int getCell(int geoPos)
	{
		return geoPos % 8;
	}

	protected static int getBlockIndex(int blockX, int blockY)
	{
		return (blockX << 8) + blockY;
	}

    protected static byte[] getGeoBlockFromGeoCoords(int geoX, int geoY)
	{
		int ix = geoX >> 11;
		int iy = geoY >> 11;

		if (ix < 0 || ix >= geodataSizeX || iy < 0 || iy >= geodataSizeY)
			return null;

		byte[][] region = geodata[ix][iy];

		if (region == null)
			return null;

		int blockX = getBlock(geoX);
		int blockY = getBlock(geoY);

		return region[getBlockIndex(blockX, blockY)];
	}

	public static void loadGeo()
	{
		Util.printSection("Geodata");

		File f = new File("geodata");

		if (!f.exists() || !f.isDirectory())
		{
			_log.info("Geo Engine: Files missing, loading aborted.");
			return;
		}

		for (File q : f.listFiles())
		{
			if (q.isHidden() || q.isDirectory())
				continue;

			GeoLoader geoLoader = GeoLoaderFactory.getInstance().getGeoLoader(q);

			if (geoLoader != null)
			{
				GeoFileInfo geoFileInfo = geoLoader.readFile(q);
				if (geoFileInfo != null)
				{

					int x = geoFileInfo.getX() - GEODATA_ARRAY_OFFSET_X;
					int y = geoFileInfo.getY() - GEODATA_ARRAY_OFFSET_Y;

					if (geodata[x][y] != null && geodata[x][y].length > 0)
						_log.warn("Geodata in region " + geoFileInfo.getX() + "_" + geoFileInfo.getY() + " was replased by " + geoLoader.getClass().getSimpleName());

					addToGeoArray(x, y, geoFileInfo.getData());
				}
			}
		}
	}

	/**
	 * Adds to array of geodata "data" bytes.<br>
	 * Please make sure that array positions are after index offset substraction.<br>
	 * <p>
	 * Example:
	 * <ul>
	 * <li>X: 25 - {@value #GEODATA_ARRAY_OFFSET_X} ({@link #GEODATA_ARRAY_OFFSET_X}) = 10</li>
	 * <li>Y: 25 - {@value #GEODATA_ARRAY_OFFSET_Y} ({@link #GEODATA_ARRAY_OFFSET_Y}) = 15</li>
	 * </ul>
	 *
	 * @see #GEODATA_ARRAY_OFFSET_X
	 * @see #GEODATA_ARRAY_OFFSET_Y
	 *
	 * @param xArrayPos x position of the block in the array
	 * @param yArrayPos y position of the block in the array
	 * @param data bytes that will be setted into geodata array
	 */
	public synchronized static void addToGeoArray(int xArrayPos, int yArrayPos, byte[][] data)
	{
		geodata[xArrayPos][yArrayPos] = data;
	}

	protected static void copyBlock(int geoX, int geoY, int blockIndex)
	{
		int ix = geoX >> 11;
		int iy = geoY >> 11;
    	if (ix < 0 || ix >= geodataSizeX || iy < 0 || iy >= geodataSizeY)
    		return;
    		
		byte[][] region = geodata[ix][iy];

		if (region == null)
		{
			_log.warn("door at null region? [" + ix + "][" + iy + "]");
			return;
		}

		byte[] block = region[blockIndex];
		byte blockType = block[0];

		switch (blockType)
		{
		case BLOCKTYPE_FLAT:
			short height = makeShort(block[2], block[1]);
			height &= 0x0fff0;
			height <<= 1;
			height |= NORTH;
			height |= SOUTH;
			height |= WEST;
			height |= EAST;
			byte[] newblock = new byte[129];
			newblock[0] = BLOCKTYPE_COMPLEX;
			for (int i = 1; i < 129; i += 2)
			{
				newblock[i + 1] = (byte) (height >> 8);
				newblock[i] = (byte) (height & 0x00ff);
			}
			region[blockIndex] = newblock;
		}
	}

	/*public GeoMove getGeoMove()
	{
		return geoMove;
	}*/

	public int getSpawnHeight(int x, int y, int z)
	{
		int zSP = NgetHeight((x - GeoConfig.MAP_MIN_X) >> 4, (y - GeoConfig.MAP_MIN_Y) >> 4, z + (GeoConfig.MIN_LAYER_HEIGHT / 2));
		if (zSP == (z + (GeoConfig.MIN_LAYER_HEIGHT / 2)))
			return z;
		else
			return zSP;
	}

	/*private static byte sign(int x)
	{
		if(x >= 0)
			return +1;
		return -1;
	}*/

	public Vector<Location> pathFind(int x, int y, int z, int tx, int ty, int tz)
	{
		return geoMove.pathFind(x, y, z, tx, ty, tz);
	}
	
	public void applyGeoCollision(GeoCollision collision)
	{
		applyGeoCollisionS(collision);
	}
	
	private static final int geoDoorOffset = 1; 
	private static final int geoDoorOffsetD = 8; 
	
	private static void applyGeoCollisionS(GeoCollision collision)
	{
		GeoShape shape = collision.getShape();
		if(shape.getXmax() == shape.getYmax() && shape.getXmax() == 0)
			throw new RuntimeException("Attempt to add incorrect collision: " + collision);
		
		boolean isFirstTime = false;

		//Размер коллизии в клетках геодаты	
		int minX = shape.getXmin() - GeoConfig.MAP_MIN_X - geoDoorOffsetD >> 4;
		int maxX = shape.getXmax() - GeoConfig.MAP_MIN_X + geoDoorOffsetD >> 4;
		int minY = shape.getYmin() - GeoConfig.MAP_MIN_Y - geoDoorOffsetD >> 4;
		int maxY = shape.getYmax() - GeoConfig.MAP_MIN_Y + geoDoorOffsetD >> 4;
		int minZ = shape.getZmin();
		int maxZ = shape.getZmax();

		if (GeoConfig.DEBUG)
			_log.info("applyGeoCollisionS "+collision+" shape.getXmin()="+shape.getXmin()+" minX="+minX+" shape.getXmax()="+shape.getXmax()+" maxX="+maxX+" shape.getYmin()="+shape.getYmin()+" minY="+minY+" shape.getYmax()="+shape.getYmax()+" maxY="+maxY+" maxX - minX + geoDoorOffset="+(maxX - minX + geoDoorOffset)+" maxY - minY + geoDoorOffset="+(maxY - minY + geoDoorOffset) + " ((maxX << 4) + GeoConfig.MAP_MIN_X)="+((maxX << 4) + GeoConfig.MAP_MIN_X));
		
		byte[][] around = collision.getGeoAround();
		if(around == null)
		{
			isFirstTime = true;

			//Сформируем коллизию
			byte[][] cells = new byte[maxX - minX + geoDoorOffset][maxY - minY + geoDoorOffset];
			for(int gX = minX; gX <= maxX; gX++)
				for(int gY = minY; gY <= maxY; gY++)
				{
					int x = (gX << 4) + GeoConfig.MAP_MIN_X + 8;
					int y = (gY << 4) + GeoConfig.MAP_MIN_Y + 8;

					loop: for(int ax = x; ax < x + geoDoorOffsetD; ax++)
						for(int ay = y; ay < y + geoDoorOffsetD; ay++)
							if(shape.isInside(ax, ay))
							{
								cells[gX - minX][gY - minY] = 1;
								break loop;
							}
				}

			around = new byte[maxX - minX + geoDoorOffset][maxY - minY + geoDoorOffset];
			for(int gX = 0; gX < cells.length; gX++)
				for(int gY = 0; gY < cells[gX].length; gY++)
				{
					if(cells[gX][gY] == 1)
					{
						around[gX][gY] = NSWE_ALL;

						byte _nswe;
						if(gY > 0)
							if(cells[gX][gY - 1] == 0)
							{
								_nswe = around[gX][gY - 1];
								_nswe |= SOUTH;
								around[gX][gY - 1] = _nswe;
							}
						if(gY + 1 < cells[gX].length)
							if(cells[gX][gY + 1] == 0)
							{
								_nswe = around[gX][gY + 1];
								_nswe |= NORTH;
								around[gX][gY + 1] = _nswe;
							}
						if(gX > 0)
							if(cells[gX - 1][gY] == 0)
							{
								_nswe = around[gX - 1][gY];
								_nswe |= EAST;
								around[gX - 1][gY] = _nswe;
							}
						if(gX + 1 < cells.length)
							if(cells[gX + 1][gY] == 0)
							{
								_nswe = around[gX + 1][gY];
								_nswe |= WEST;
								around[gX + 1][gY] = _nswe;
							}
					}
				}

			collision.setGeoAround(around);
		}

		short height;
		byte old_nswe, close_nswe;

		for(int gX = 0; gX < around.length; gX++)
			for(int gY = 0; gY < around[gX].length; gY++)
			{
				if (around[gX][gY] == 0) continue;
				
				int geoX = minX + gX;
				int geoY = minY + gY;

				// Получение индекса блока
				int blockX = getBlock(geoX);
				int blockY = getBlock(geoY);
				int blockIndex = getBlockIndex(blockX, blockY);
				
				// Попытка скопировать блок геодаты, если уже существует, то не скопируется
				if(isFirstTime)
					copyBlock(geoX, geoY, blockIndex);

				byte[] block = getGeoBlockFromGeoCoords(geoX, geoY);
				if(block == null)
					continue;
				
				int cellX = getCell(geoX);
				int cellY = getCell(geoY);

				int index = 0;
				byte blockType = block[index];
				index++;

				switch(blockType)
				{
					case BLOCKTYPE_COMPLEX:
						index += (cellX << 3) + cellY << 1;

						// Получаем высоту клетки
						height = makeShort(block[index + 1], block[index]);
						old_nswe = (byte) (height & 0x0F);
						height &= 0xfff0;
						height >>= 1;

						// подходящий слой не найден
						if(height < minZ || height > maxZ)
							break;

						close_nswe = around[gX][gY];

						if(isFirstTime)
						{
							if(collision.isConcrete())
								close_nswe &= old_nswe;
							else
								close_nswe &= ~old_nswe;
							around[gX][gY] = close_nswe;
						}

						// around
						height <<= 1;
						height &= 0xfff0;
						height |= old_nswe;

						if(collision.isConcrete())
							height &= ~close_nswe;
						else
							height |= close_nswe;

						// Записываем высоту в массив
						block[index + 1] = (byte) (height >> 8);
						block[index] = (byte) (height & 0x00ff);
						break;
					case BLOCKTYPE_MULTILEVEL:
						// Последний валидный индекс для двери
						int neededIndex = -1;

						// Далее следует стандартный механизм получения высоты
						int offset = (cellX << 3) + cellY;
						while(offset > 0)
						{
							byte lc = block[index];
							index += (lc << 1) + 1;
							offset--;
						}
						byte layers = block[index];
						index++;
						if(layers <= 0 || layers > MAX_LAYERS)
							break;
						short temph = Short.MIN_VALUE;
						old_nswe = NSWE_ALL;
						while(layers > 0)
						{
							height = makeShort(block[index + 1], block[index]);
							byte tmp_nswe = (byte) (height & 0x0F);
							height &= 0xfff0;
							height >>= 1;
							int z_diff_last = Math.abs(minZ - temph);
							int z_diff_curr = Math.abs(maxZ - height);
							if(z_diff_last > z_diff_curr)
							{
								old_nswe = tmp_nswe;
								temph = height;
								neededIndex = index;
							}
							layers--;
							index += 2;
						}

						// подходящий слой не найден
						if(temph == Short.MIN_VALUE || (temph < minZ || temph > maxZ))
							break;

						close_nswe = around[gX][gY];

						if(isFirstTime)
						{
							if(collision.isConcrete())
								close_nswe &= old_nswe;
							else
								close_nswe &= ~old_nswe;
							around[gX][gY] = close_nswe;
						}

						// around
						temph <<= 1;
						temph &= 0xfff0;
						temph |= old_nswe;
						if(collision.isConcrete())
							temph &= ~close_nswe;
						else
							temph |= close_nswe;

						// записываем высоту
						block[neededIndex + 1] = (byte) (temph >> 8);
						block[neededIndex] = (byte) (temph & 0x00ff);
						break;
				}
			}
	}
	
	public void removeGeoCollision(GeoCollision collision)
	{
		removeGeoCollisionS(collision);
	}
	
	private static void removeGeoCollisionS(GeoCollision collision)
	{
		GeoShape shape = collision.getShape();

		byte[][] around = collision.getGeoAround();
		if(around == null)
			throw new RuntimeException("Attempt to remove unitialized collision: " + collision);

		//Размер коллизии в клетках геодаты	
		int minX = shape.getXmin() - GeoConfig.MAP_MIN_X - geoDoorOffsetD >> 4;
		int minY = shape.getYmin() - GeoConfig.MAP_MIN_Y - geoDoorOffsetD >> 4;
		int minZ = shape.getZmin();
		int maxZ = shape.getZmax();

		short height;
		byte old_nswe;

		for(int gX = 0; gX < around.length; gX++)
		{
			for(int gY = 0; gY < around[gX].length; gY++)
			{
				if (around[gX][gY] == 0) continue;
				
				int geoX = minX + gX;
				int geoY = minY + gY;

				byte[] block = getGeoBlockFromGeoCoords(geoX, geoY);
				if(block == null)
					continue;
				
				int cellX = getCell(geoX);
				int cellY = getCell(geoY);

				int index = 0;
				byte blockType = block[index];
				index++;

				switch(blockType)
				{
					case BLOCKTYPE_COMPLEX:
						index += (cellX << 3) + cellY << 1;

						// Получаем высоту клетки
						height = makeShort(block[index + 1], block[index]);
						old_nswe = (byte) (height & 0x0F);
						height &= 0xfff0;
						height >>= 1;

						// подходящий слой не найден
						if(height < minZ || height > maxZ)
							break;

						// around
						height <<= 1;
						height &= 0xfff0;
						height |= old_nswe;
						if(collision.isConcrete())
							height |= around[gX][gY];
						else
							height &= ~around[gX][gY];

						// Записываем высоту в массив
						block[index + 1] = (byte) (height >> 8);
						block[index] = (byte) (height & 0x00ff);
						break;
					case BLOCKTYPE_MULTILEVEL:
						// Последний валидный индекс для двери
						int neededIndex = -1;

						// Далее следует стандартный механизм получения высоты
						int offset = (cellX << 3) + cellY;
						while(offset > 0)
						{
							byte lc = block[index];
							index += (lc << 1) + 1;
							offset--;
						}
						byte layers = block[index];
						index++;
						if(layers <= 0 || layers > MAX_LAYERS)
							break;
						short temph = Short.MIN_VALUE;
						old_nswe = NSWE_ALL;
						while(layers > 0)
						{
							height = makeShort(block[index + 1], block[index]);
							byte tmp_nswe = (byte) (height & 0x0F);
							height &= 0xfff0;
							height >>= 1;
							int z_diff_last = Math.abs(minZ - temph);
							int z_diff_curr = Math.abs(maxZ - height);
							if(z_diff_last > z_diff_curr)
							{
								old_nswe = tmp_nswe;
								temph = height;
								neededIndex = index;
							}
							layers--;
							index += 2;
						}

						// подходящий слой не найден						
						if(temph == Short.MIN_VALUE || (temph < minZ || temph > maxZ))
							break;

						// around
						temph <<= 1;
						temph &= 0xfff0;
						temph |= old_nswe;
						if(collision.isConcrete())
							temph |= around[gX][gY];
						else
							temph &= ~around[gX][gY];

						// записываем высоту
						block[neededIndex + 1] = (byte) (temph >> 8);
						block[neededIndex] = (byte) (temph & 0x00ff);
						break;
				}
			}
		}
	}
}