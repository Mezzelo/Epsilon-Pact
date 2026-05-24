package data.scripts.campaign.backgrounds;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.espc_ModPlugin;
import data.scripts.campaign.skills.espc_HumbleTastesBg_Skill;
import data.scripts.util.MezzUtils;
import lunalib.lunaSettings.LunaSettings;

import exerelin.campaign.backgrounds.BaseCharacterBackground;
import exerelin.utilities.NexFactionConfig;

public class espc_HumbleTastesBackground extends BaseCharacterBackground {
	
	private boolean requirePerfectScore() {
		if (!espc_ModPlugin.hasLuna())
			return true;
		
    	String req = LunaSettings.getString("epsilonpact", "espc_BGScoreRequirements");
    	return req.equals(MezzUtils.getString("espc_settings", "scorereq_perfect"));
    	
	}

	private boolean isUnlocked() {
    	int scoreReq = 100;
    	if (espc_ModPlugin.hasLuna()) {
    		String req = LunaSettings.getString("epsilonpact", "espc_BGScoreRequirements");
    		if (req.equals(MezzUtils.getString("espc_settings", "scorereq_none")))
    			return true;
    		else if (req.equals(MezzUtils.getString("espc_settings", "scorereq_complete")))
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
        Global.getSector().getPlayerPerson().getStats().setSkillLevel("espc_second_wind", 2.0f);
        Global.getSector().getPlayerPerson().getStats().setSkillLevel("espc_running_hot", 2.0f);
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
            requirePerfectScore() ? MezzUtils.getString("espc_nexbackgrounds", "missionreqperfect") :
            	MezzUtils.getString("espc_nexbackgrounds", "missionreq"), 
            0f,
        	Misc.getTextColor(), 
        	Misc.getHighlightColor(),
        	MezzUtils.getString("espc_missionnames", "remise"));
    }
    
    @Override
    public String getTitle(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        return spec.title + (isUnlocked() ? "" : " " + MezzUtils.getString("espc_nexbackgrounds", "lockedName"));
    }

    @Override
    public String getLongDescription(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        if (!isUnlocked()) 
        	return MezzUtils.getString("espc_nexbackgrounds", "locked");

        return MezzUtils.getString("espc_nexbackgrounds", "humbleTastes_shortdesc");
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
            	MezzUtils.getString("espc_nexbackgrounds", "humbleTastes_desc1-1"),
               	0f,
               	Misc.getTextColor(), 
               	Misc.getHighlightColor(),
               	MezzUtils.getString("espc_nexbackgrounds", "humbleTastes_desc1-2"));
            PersonAPI personForSkills = Global.getFactory().createPerson();
            personForSkills.getStats().setSkillLevel("espc_dancing_steps", 2f);
            personForSkills.getStats().setSkillLevel("espc_running_hot", 2f);
            personForSkills.getStats().setSkillLevel("espc_second_wind", 2f);
            personForSkills.getStats().setSkillLevel("espc_underdog", 2f);
            tooltip.addSkillPanel(personForSkills, 0f);
            tooltip.addSpacer(10f);
            tooltip.addPara(
            	MezzUtils.getString("espc_nexbackgrounds", "humbleTastes_desc2-1"), 
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
        	MezzUtils.getString("espc_nexbackgrounds", "humbleTastes_desc1-1"),
        	0f,
        	Misc.getTextColor(), 
        	Misc.getHighlightColor(),
        	MezzUtils.getString("espc_nexbackgrounds", "humbleTastes_desc1-2"));
        PersonAPI personForSkills = Global.getFactory().createPerson();
        personForSkills.getStats().setSkillLevel("espc_dancing_steps", 2f);
        personForSkills.getStats().setSkillLevel("espc_running_hot", 2f);
        personForSkills.getStats().setSkillLevel("espc_second_wind", 2f);
        personForSkills.getStats().setSkillLevel("espc_underdog", 2f);
        tooltip.addSkillPanel(personForSkills, 0f);
        tooltip.addSpacer(10f);
        tooltip.addPara(
        	MezzUtils.getString("espc_nexbackgrounds", "humbleTastes_desc2-1"), 
            0f,
            Misc.getTextColor(), 
            Misc.getNegativeHighlightColor(),
            (int) espc_HumbleTastesBg_Skill.CR_PENALTY_PER_DP + "%", 
            (int) espc_HumbleTastesBg_Skill.DP_THRESHOLD + "");
    }

}
