package data.scripts.sic.espc;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import second_in_command.SCData;
import second_in_command.specs.SCAptitudeSection;
import second_in_command.specs.SCBaseAptitudePlugin;

public class espc_AptitudePact extends SCBaseAptitudePlugin {
    
    @Override
    public void addCodexDescription(TooltipMakerAPI tooltip) {
        tooltip.addPara(
        	"The Pact aptitude is combat-focused, centered around their eccentric doctrine. "
        	+ "It empowers an irregular, lightweight fleet composition to dictate the terms of engagement and "
        	+ "strike well above its weight. ",
            0f, Misc.getTextColor(), Misc.getHighlightColor(), "Pact", "");
        tooltip.addSpacer(10f);
        tooltip.addPara(
            "As well as enabling the use of Automated ships, this aptitude also grants greater access to their unique skillset.",
            0f, Misc.getTextColor(), Misc.getHighlightColor(), "automated ships", "");
    }

    @Override
    public String getOriginSkillId() {
        return "espc_sc_pact_in_harmony";
    }
    
    @Override
    public void createSections() {

    	SCAptitudeSection section = new SCAptitudeSection(true, 0, "combat1");
        section.addSkill("espc_sc_pact_against_the_odds");
        section.addSkill("espc_sc_pact_clear_skies");
        section.addSkill("espc_sc_pact_something_different");
        addSection(section);
        
        section = new SCAptitudeSection(true, 2, "combat2");
        section.addSkill("espc_sc_pact_in_unison");
        section.addSkill("espc_sc_pact_travel_light");
        // section.addSkill("espc_sc_pact_from_the_dark");
        section.addSkill("espc_sc_pact_all_walks");
        section.addSkill("espc_sc_pact_strike_twice");
        addSection(section);

        section = new SCAptitudeSection(false, 4, "combat3");
        section.addSkill("espc_sc_pact_aces_high");
        section.addSkill("espc_sc_pact_beyond_perfection");
        addSection(section);

    }

    @Override
    public Float getCryopodSpawnWeight(StarSystemAPI System) {
        return 0f;
    }

    @Override
    public Boolean guaranteePick(CampaignFleetAPI fleet) {
    	if (true)
    		return false;
    	if (fleet.getFaction() == null)
    		return false;
    	if (fleet.getFaction().getId() != null && 
        	fleet.getFaction().getId().equals("epsilpac")) // bounty fleet/nex allied fleets
    		return true;
    	return false;
    }
    
    @Override
    public Float getNPCFleetSpawnWeight(SCData data, CampaignFleetAPI fleet) {
    	if (true)
    		return 0f;
    	
    	if (fleet.getFaction() == null)
    		return 0f;
    	if (fleet.getFaction().getId() != null && 
    		fleet.getFaction().getId().equals("epsilpac")) // bounty fleet/nex allied fleets
    		return Float.MAX_VALUE;
    	return 0f;
    }
    
    public Float getMarketSpawnweight(MarketAPI market) {
    	if (true)
    		return 0f;
    	if (market.getFaction() == null)
    		return 0f;
    	if (market.getFaction().getId().equals("epsilpac"))
    		return 3.5f;
    	return 0f;
    }
}