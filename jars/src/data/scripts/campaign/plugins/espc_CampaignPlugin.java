package data.scripts.campaign.plugins;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.*;

import data.scripts.plugin.espc_MeCorePlugin;

public class espc_CampaignPlugin extends BaseCampaignPlugin {

	public String getId() {
		return "espc_CampaignPlugin";
	}
	
	public PluginPick<AICoreOfficerPlugin> pickAICoreOfficerPlugin(String commodityId) {
		if (commodityId.equals("espc_meCore"))
			return new PluginPick<AICoreOfficerPlugin>(new espc_MeCorePlugin(), PickPriority.MOD_SET);
		return null;
	}

}
