package l2ft.geoserver.util;

import java.io.File;

public final class Util
{
	// some sys info utils
	public static int getAvailableProcessors()
	{
		Runtime rt = Runtime.getRuntime();
		return rt.availableProcessors();
	}

	public static String getOSName()
	{
		return System.getProperty("os.name");
	}

	public static String getOSVersion()
	{
		return System.getProperty("os.version");
	}

	public static String getOSArch()
	{
		return System.getProperty("os.arch");
	}

	public static String[] getMemUsage()
	{
		double maxMem = (Runtime.getRuntime().maxMemory() / 1024); // maxMemory is the upper limit the jvm can use
		double allocatedMem = (Runtime.getRuntime().totalMemory() / 1024); // totalMemory the size of the current allocation pool
		double nonAllocatedMem = maxMem - allocatedMem; // non allocated memory till jvm limit
		double cachedMem = (Runtime.getRuntime().freeMemory() / 1024); // freeMemory the unused memory in the allocation pool
		double usedMem = allocatedMem - cachedMem; // really used memory
		double useableMem = maxMem - usedMem; // allocated, but non-used and non-allocated memory
		return new String[]
		{
				" - AllowedMemory: " + ((int) (maxMem)) + " KB",
				"Allocated: " + ((int) (allocatedMem)) + " KB (" + (((double) (Math.round(allocatedMem / maxMem * 1000000))) / 10000) + "%)",
				"Non-Allocated: " + ((int) (nonAllocatedMem)) + " KB (" + (((double) (Math.round(nonAllocatedMem / maxMem * 1000000))) / 10000) + "%)",
				"- AllocatedMemory: " + ((int) (allocatedMem)) + " KB",
				"Used: " + ((int) (usedMem)) + " KB (" + (((double) (Math.round(usedMem / maxMem * 1000000))) / 10000) + "%)",
				"Unused (cached): " + ((int) (cachedMem)) + " KB (" + (((double) (Math.round(cachedMem / maxMem * 1000000))) / 10000) + "%)",
				"- UseableMemory: " + ((int) (useableMem)) + " KB (" + (((double) (Math.round(useableMem / maxMem * 1000000))) / 10000) + "%)"
		};
	}

	public static String getRelativePath(File base, File file)
	{
		return file.toURI().getPath().substring(base.toURI().getPath().length());
	}

	public static void printSection(String s)
	{
		int maxlength = 79;
		s = "-[ " + s + " ]";
		int slen = s.length();
		if (slen > maxlength)
		{
			System.out.println(s);
			return;
		}
		int i;
		for (i = 0; i < (maxlength - slen); i++)
			s = "=" + s;
		System.out.println(s);
	}
}