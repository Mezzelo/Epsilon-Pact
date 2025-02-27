package data.scripts.campaign.backgrounds;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.characters.PersonAPI;
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
        return Global.getSettings().getMissionScore("espc_principle") >= scoreReq;
        
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
            requirePerfectScore() ? "Complete the mission Remise with a perfect score to unlock this background." :
            	"Complete the mission Remise to unlock this background.", 
            0f,
        	Misc.getTextColor(), 
        	Misc.getHighlightColor(),
        	"Remise");
    }
    
    @Override
    public String getTitle(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        return spec.title + (isUnlocked() ? "" : " [LOCKED]");
    }

    @Override
    public String getLongDescription(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        if (!isUnlocked()) 
        	return "This background is locked.";

        return "Start with four combat skills that you cannot normally obtain. You are severely penalized for piloting"
        	+ " anything more formidable than a Wolf.";
    }
    
    @Override
    public String getIcon(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
    	if (isUnlocked())
    		return spec.iconPath;
    	return Global.getSettings().getSpriteName(
			"icons", "espc_humbleTastesBgIconLocked");
    }
    
    @Override
    public void addTooltipForSelection(TooltipMakerAPI tooltip, FactionSpecAPI factionSpec, NexFactionConfig factionConfig, Boolean expanded) {
        super.addTooltipForSelection(tooltip, factionSpec, factionConfig, expanded);
        if (expanded && isUnlocked()) {
            tooltip.addSpacer(10f);
            tooltip.addPara(
               	"Start with the following skills. These do not contribute to your spent skill points or lower your level cap.",
               	0f,
               	Misc.getTextColor(), 
               	Misc.getHighlightColor(),
               	"do not");
            PersonAPI personForSkills = Global.getFactory().createPerson();
            personForSkills.getStats().setSkillLevel("espc_dancing_steps", 2f);
            personForSkills.getStats().setSkillLevel("espc_running_hot", 1f);
            personForSkills.getStats().setSkillLevel("espc_second_wind", 1f);
            personForSkills.getStats().setSkillLevel("espc_underdog", 2f);
            tooltip.addSkillPanel(personForSkills, 0f);
            tooltip.addSpacer(10f);
            tooltip.addPara(
            	"Any ship you pilot loses %s maximum CR for every base DP over %s, " +
            	"and its CR is capped to its maximum in combat.", 
                0f,
                Misc.getTextColor(), 
                Misc.getNegativeHighlightColor(),
                (int) espc_HumbleTastesBg_Skill.CR_PENALTY_PER_DP + "%", 
                (int) espc_HumbleTastesBg_Skill.DP_THRESHOLD + "");
        }
    }
    
    @Override
    public void addTooltipForIntel(TooltipMakerAPI tooltip, FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        super.addTooltipForIntel(tooltip, factionSpec, factionConfig);
        tooltip.addSpacer(10f);
        tooltip.addPara(
        	"Start with the following skills. These do not contribute to your spent skill points or lower your level cap.",
        	0f,
        	Misc.getTextColor(), 
        	Misc.getHighlightColor(),
        	"do not");
        PersonAPI personForSkills = Global.getFactory().createPerson();
        personForSkills.getStats().setSkillLevel("espc_dancing_steps", 2f);
        personForSkills.getStats().setSkillLevel("espc_running_hot", 1f);
        personForSkills.getStats().setSkillLevel("espc_second_wind", 1f);
        personForSkills.getStats().setSkillLevel("espc_underdog", 2f);
        tooltip.addSkillPanel(personForSkills, 0f);
        tooltip.addSpacer(10f);
        tooltip.addPara(
        	"Any ship you pilot loses %s maximum CR for every base DP over %s, " +
        	"and its CR is capped to its maximum in combat.", 
            0f,
            Misc.getTextColor(), 
            Misc.getNegativeHighlightColor(),
            (int) espc_HumbleTastesBg_Skill.CR_PENALTY_PER_DP + "%", 
            (int) espc_HumbleTastesBg_Skill.DP_THRESHOLD + "");
    }

}
