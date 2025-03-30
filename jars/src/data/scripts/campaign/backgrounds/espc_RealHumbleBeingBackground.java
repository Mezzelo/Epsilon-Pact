package data.scripts.campaign.backgrounds;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.espc_ModPlugin;
import data.scripts.campaign.listeners.espc_PactFleetSpawnListener;
import data.scripts.campaign.skills.espc_HumbleTastesBg_Skill;
import exerelin.campaign.backgrounds.BaseCharacterBackground;
import exerelin.utilities.NexFactionConfig;
import lunalib.lunaSettings.LunaSettings;

public class espc_RealHumbleBeingBackground extends BaseCharacterBackground {

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
        return Global.getSettings().getMissionScore("espc_looseends") >= scoreReq &&
        	Global.getSettings().getMissionScore("espc_principle") >= scoreReq;
        
	}
    @Override
    public float getOrder() {
        if (!isUnlocked()) 
        	return (float) (Integer.MAX_VALUE - spec.order);
        return spec.order;
    }

    @Override
    public boolean shouldShowInSelection(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
    	if (!espc_ModPlugin.hasLuna())
    		return false;
    	else
    		return espc_ModPlugin.hasLuna() && LunaSettings.getBoolean("epsilonpact", "espc_RealHumbleBeing");
    }

    public void onNewGameAfterTimePass(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        Global.getSector().getPlayerPerson().getStats().setSkillLevel("espc_dancing_steps", 2.0f);
        Global.getSector().getPlayerPerson().getStats().setSkillLevel("espc_underdog", 2.0f);
        Global.getSector().getPlayerPerson().getStats().setSkillLevel("espc_second_wind", 1.0f);
        Global.getSector().getPlayerPerson().getStats().setSkillLevel("espc_running_hot", 1.0f);
        Global.getSector().getPlayerPerson().getStats().setSkillLevel("espc_humbleTastesBg_skill", 1.0f);
        
        Global.getSector().getPlayerPerson().getStats().setSkillLevel("espc_realHumbleBeingBg_skill", 1.0f);
        Global.getSector().getPlayerPerson().setAICoreId("espc_meCore");

        Global.getSector().getPlayerFleet().getCargo().addItems(
        	CargoAPI.CargoItemType.RESOURCES, "espc_meCore", 1
        );
        Global.getSector().getPlayerFleet().getCargo().addItems(
        	CargoAPI.CargoItemType.RESOURCES, Commodities.SUPPLIES, 100
        );
        
        Global.getSector().getPlayerFaction().setRelationship(Factions.DERELICT, 0.25f);
        Global.getSector().getPlayerFaction().setRelationship(Factions.REMNANTS, 0.25f);

		FleetMemberAPI ship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, 
			"glimmer_Assault");
		ship.getCrewComposition().setCrew(100000);
		ship.getRepairTracker().setCR(0.7f);
		Global.getSector().getPlayerFleet().getFleetData().addFleetMember(ship);
        
        if (espc_ModPlugin.hasLuna() && !LunaSettings.getBoolean("epsilonpact", "espc_AIBGPortraits")) {
        	return;
        }
        if (Global.getSector().getPlayerPerson().getGender() == Gender.FEMALE)
	        Global.getSector().getPlayerPerson().setPortraitSprite(Global.getSettings().getSpriteName(
				"characters", "espc_" + espc_PactFleetSpawnListener.portraitList[Misc.random.nextInt(
					4)]));
        else if (Global.getSector().getPlayerPerson().getGender() == Gender.MALE)
	        Global.getSector().getPlayerPerson().setPortraitSprite(Global.getSettings().getSpriteName(
				"characters", "espc_" + espc_PactFleetSpawnListener.portraitList[Misc.random.nextInt(
					3) + 4]));
        else
	        Global.getSector().getPlayerPerson().setPortraitSprite(Global.getSettings().getSpriteName(
				"characters", "espc_" + espc_PactFleetSpawnListener.portraitList[Misc.random.nextInt(
					espc_PactFleetSpawnListener.portraitList.length - 2)]));
        Global.getSector().getCharacterData().setPortraitName(Global.getSector().getPlayerPerson().getPortraitSprite());
    }
    
    @Override
    public boolean canBeSelected(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
    	return isUnlocked();
    }

    @Override
    public void canNotBeSelectedReason(TooltipMakerAPI tooltip, FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        tooltip.addPara(
        	requirePerfectScore() ? "Complete the missions Loose Ends and Remise with perfect scores to unlock this background." :
        		"Complete the missions Loose Ends and Remise to unlock this background.", 
        	0f,
        	Misc.getTextColor(), 
        	Misc.getHighlightColor(),
        	"Loose Ends", "Remise");
    }
    
    @Override
    public String getTitle(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        return spec.title + (isUnlocked() ? "" : " [LOCKED]");
    }

    @Override
    public String getLongDescription(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        if (!isUnlocked()) 
        	return "This background is locked.";

        return "It's a lot. Expand the tooltip.";
    }
    @Override
    public String getIcon(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
    	if (isUnlocked())
    		return spec.iconPath;
    	return Global.getSettings().getSpriteName(
			"icons", "espc_realHumbleBeingBgIconLocked");
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
            	"You can now commandeer automated ships, but %s.",
            	0f,
            	Misc.getTextColor(), 
            	Misc.getNegativeHighlightColor(),
            	"any non-automated ship you commandeer will be reduced to 0 CR, unless via Neural Interface");
            tooltip.addPara(
            	"Any ship you pilot loses %s maximum CR for every base DP over %s, " +
            	"and its CR is capped to its maximum in combat.", 
                0f,
                Misc.getTextColor(), 
                Misc.getNegativeHighlightColor(),
                (int) espc_HumbleTastesBg_Skill.CR_PENALTY_PER_DP + "%", 
                (int) espc_HumbleTastesBg_Skill.DP_THRESHOLD + "");
            
            tooltip.addSpacer(10f);
            tooltip.addPara(
                "Your maximum officer level is increased by 1. However, your base maximum officer count is reduced to 0.",
            	0f,
            	Misc.getTextColor(), 
            	Misc.getNegativeHighlightColor(),
            	"reduced to 0");
            tooltip.addPara(
            	"Officers are no longer available to recruit from promotion or comms directories, except on %s. " +
            	"Officers hired this way decrease the initial penalty by 1 each.", 
            	0f,
            	Misc.getTextColor(), 
            	Misc.getHighlightColor(),
            	"Pact markets");
            tooltip.addSpacer(10f);
            tooltip.addPara(
            	"You start with %s automated ship points and the ability to recover automated ships. "
            	+ "It is possible to learn the %s skill itself, effectively doubling your maximum automated ship"
            	+ " points.", 
            	0f,
            	Misc.getTextColor(), 
            	Misc.getHighlightColor(),
            	"120", "Automated Ships");
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
        	"You can now commandeer automated ships, but %s.",
        	0f,
        	Misc.getTextColor(), 
        	Misc.getNegativeHighlightColor(),
        	"any non-automated ship you commandeer will be reduced to 0 CR, unless via Neural Interface");
        tooltip.addPara(
        	"Any ship you pilot loses %s maximum CR for every base DP over %s, " +
        	"and its CR is capped to its maximum in combat.", 
            0f,
            Misc.getTextColor(), 
            Misc.getNegativeHighlightColor(),
            (int) espc_HumbleTastesBg_Skill.CR_PENALTY_PER_DP + "%", 
            (int) espc_HumbleTastesBg_Skill.DP_THRESHOLD + "");
        
        tooltip.addSpacer(10f);
        tooltip.addPara(
            "Your maximum officer level is increased by 1. However, your base maximum officer count is reduced to 0.",
        	0f,
        	Misc.getTextColor(), 
        	Misc.getNegativeHighlightColor(),
        	"reduced to 0");
        tooltip.addPara(
        	"Officers are no longer available to recruit from promotion or comms directories, except on %s. " +
        	"Officers hired this way decrease the initial penalty by 1 each.", 
        	0f,
        	Misc.getTextColor(), 
        	Misc.getHighlightColor(),
        	"Pact markets");
        tooltip.addSpacer(10f);
        tooltip.addPara(
			"You start with %s automated ship points and the ability to recover automated ships. "
			+ "It is possible to learn the %s skill itself, effectively doubling your maximum automated ship"
			+ " points.", 
			0f,
			Misc.getTextColor(), 
			Misc.getHighlightColor(),
			"120", "Automated Ships");
    }

}
