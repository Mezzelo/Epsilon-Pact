package data.scripts.campaign.backgrounds;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.espc_ModPlugin;
import data.scripts.campaign.skills.espc_HumbleTastesBg_Skill;
import lunalib.lunaSettings.LunaSettings;

import exerelin.campaign.backgrounds.BaseCharacterBackground;
import exerelin.utilities.NexFactionConfig;

public class espc_HumbleTastesBackground extends BaseCharacterBackground {
	
	private boolean requirePerfectScore() {
		if (!espc_ModPlugin.hasLuna())
			return true;
		
    	String req = LunaSettings.getString("epsilonpact", "espc_BGScoreRequirements");
    	return req.equals("Perfect");
    	
	}

	private boolean isUnlocked() {
    	int scoreReq = 100;
    	if (espc_ModPlugin.hasLuna()) {
    		String req = LunaSettings.getString("epsilonpact", "espc_BGScoreRequirements");
    		if (req.equals("None"))
    			return true;
    		else if (req.equals("Complete"))
    			scoreReq = 1;
    	}
        return Global.getSettings().getMissionScore("espc_newmoon") >= scoreReq;
        
	}
    @Override
    public float getOrder() {
        if (!isUnlocked()) 
        	return (float) (Integer.MAX_VALUE - spec.order);
        return spec.order;
    }

    @Override
    public boolean shouldShowInSelection(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        return true;
    }

    public void onNewGameAfterTimePass(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        Global.getSector().getPlayerPerson().getStats().setSkillLevel("espc_dancing_steps", 2.0f);
        Global.getSector().getPlayerPerson().getStats().setSkillLevel("espc_underdog", 2.0f);
        Global.getSector().getPlayerPerson().getStats().setSkillLevel("espc_second_wind", 1.0f);
        Global.getSector().getPlayerPerson().getStats().setSkillLevel("espc_running_hot", 1.0f);
        Global.getSector().getPlayerPerson().getStats().setSkillLevel("espc_humbleTastesBg_skill", 1.0f);
        // Global.getSector().getPlayerPerson().getStats().setLevel(3);
    }   

    @Override
    public boolean canBeSelected(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
    	return isUnlocked();
    }

    @Override
    public void canNotBeSelectedReason(TooltipMakerAPI tooltip, FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        tooltip.addPara(
        	requirePerfectScore() ? "Complete the mission New Moon with a perfect score to unlock this background." :
        		"Complete the mission New Moon to unlock this background.", 
        	0f,
        	Misc.getTextColor(), 
        	Misc.getHighlightColor(),
        	"New Moon");
    }
    
    @Override
    public String getTitle(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        return spec.title + (isUnlocked() ? "" : " [LOCKED]");
    }

    @Override
    public String getLongDescription(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        if (!isUnlocked()) 
        	return "This background is locked.";

        return "Start with four combat skills that you cannot normally obtain.\n\nAny ship you pilot loses " + 
        	(int) espc_HumbleTastesBg_Skill.CR_PENALTY_PER_DP + "% maximum CR for every base DP over " +
        	espc_HumbleTastesBg_Skill.DP_THRESHOLD + ", and its CR is capped to its maximum in combat.";
        	// + "\n\nYou start at level 3, effectively lowering your maximum level by 2.";
    }
    @Override
    public String getIcon(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
    	if (isUnlocked())
    		return spec.iconPath;
    	return Global.getSettings().getSpriteName(
			"icons", "espc_humbleTastesBgIconLocked");
    }

}
