package data.scripts.campaign.submarkets;

import com.fs.starfarer.api.util.Misc;

public class espc_MarketOverrideList {
	public static String[] overrideList = {
		"espc_ember",
		"espc_militia"
	};
	public static String[] replaceList = {
		"mudskipper",
		"wayfarer",
		"hermes",
		"mercury",
		"shepherd",
		"kite",
		"ox",
		"dram",
		"buffalo",
		"crig",
		"mule",
		"tarsus",
		"condor",
		"gemini",
		"nebula",
		"phaeton",
		"venture",
		"apogee",
	};
	
	public static float overrideChance = 0.9f;
	
	public static boolean isOverride(String hullId) {
		for (int i = 0; i < overrideList.length; i++) {
			if (hullId.equals(overrideList[i]))
				return true;
		}
		return false;
	}
	
	public static String getHull() {
		return replaceList[Misc.random.nextInt(replaceList.length)];
	}
}
