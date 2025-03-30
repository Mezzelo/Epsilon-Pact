package data.scripts.campaign.backgrounds;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.espc_ModPlugin;
import data.scripts.campaign.listeners.espc_PactFleetSpawnListener;
import exerelin.campaign.backgrounds.BaseCharacterBackground;
import exerelin.utilities.NexFactionConfig;
import lunalib.lunaSettings.LunaSettings;

public class espc_RealHumanBeingBackground extends BaseCharacterBackground {

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
        return Global.getSettings().getMissionScore("espc_looseends") >= scoreReq;
        
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
        Global.getSector().getPlayerPerson().getStats().setSkillLevel("espc_realHumanBeingBg_skill", 1.0f);
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
		Global.getSector().getPlayerFleet().getFlagship().getRepairTracker().setCR(0.7f);
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
        	requirePerfectScore() ? "Complete the mission Loose Ends with a perfect score to unlock this background." :
        		"Complete the mission Loose Ends to unlock this background.", 
        	0f,
        	Misc.getTextColor(), 
        	Misc.getHighlightColor(),
        	"Loose Ends");
    }
    
    @Override
    public String getTitle(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        return spec.title + (isUnlocked() ? "" : " [LOCKED]");
    }

    @Override
    public String getLongDescription(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        if (!isUnlocked()) 
        	return "This background is locked.";

        return "Start with the bonuses of the Automated Ships skill and the ability to pilot and transfer between automated ships. " + 
        	"Your ability to recruit officers is severely hampered.";
    }
    @Override
    public String getIcon(FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
    	if (isUnlocked())
    		return spec.iconPath;
    	return Global.getSettings().getSpriteName(
			"icons", "espc_realHumanBeingBgIconLocked");
    }
    
    
    @Override
    public void addTooltipForSelection(TooltipMakerAPI tooltip, FactionSpecAPI factionSpec, NexFactionConfig factionConfig, Boolean expanded) {
        super.addTooltipForSelection(tooltip, factionSpec, factionConfig, expanded);
        if (expanded && isUnlocked()) {
            tooltip.addSpacer(10f);
            tooltip.addPara(
            	"You can now commandeer automated ships, but %s. "
            	+ "Your base maximum officer count is %s.",
            	0f,
            	Misc.getTextColor(), 
            	Misc.getNegativeHighlightColor(),
            	"any non-automated ship you commandeer will be reduced to 0 CR, except via Neural Interface", "reduced to 0");
            tooltip.addSpacer(10f);
            tooltip.addPara(
            	"You start with %s automated ship points and the ability to recover automated ships. "
            	+ "It is possible to learn the %s skill itself, gaining an additional 120 points.",
            	0f,
            	Misc.getTextColor(), 
            	Misc.getHighlightColor(),
            	"360", "Automated Ships");
        }
    }
    
    @Override
    public void addTooltipForIntel(TooltipMakerAPI tooltip, FactionSpecAPI factionSpec, NexFactionConfig factionConfig) {
        super.addTooltipForIntel(tooltip, factionSpec, factionConfig);
        tooltip.addSpacer(10f);
        tooltip.addPara(
        	"You can now commandeer automated ships, but %s. "
        	+ "Your base maximum officer count is %s.",
        	0f,
        	Misc.getTextColor(), 
        	Misc.getNegativeHighlightColor(),
        	"any non-automated ship you commandeer will be reduced to 0 CR, except via Neural Interface", "reduced to 0");
        tooltip.addSpacer(10f);
        tooltip.addPara(
        	"You start with %s automated ship points and the ability to recover automated ships. "
        	+ "It is possible to learn the %s skill itself, gaining an additional 120 points.",
        	0f,
        	Misc.getTextColor(), 
        	Misc.getHighlightColor(),
        	"360", "Automated Ships");
    }

}
