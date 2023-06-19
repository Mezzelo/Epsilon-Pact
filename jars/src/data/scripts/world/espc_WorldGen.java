package data.scripts.world;

// import com.fs.starfarer.api.Global;
// import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
// import com.fs.starfarer.api.campaign.FleetAssignment;
// import com.fs.starfarer.api.campaign.RepLevel;
// import com.fs.starfarer.api.campaign.SectorEntityToken;
// import com.fs.starfarer.api.campaign.econ.MarketAPI;
// import com.fs.starfarer.api.characters.FullName;
// import com.fs.starfarer.api.characters.PersonAPI;
// import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
// import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
// import com.fs.starfarer.api.impl.campaign.ids.Personalities;
// import com.fs.starfarer.api.impl.campaign.ids.Ranks;
// import com.fs.starfarer.api.impl.campaign.ids.Stats;
// import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
// import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
// import data.scripts.world.systems.Diableavionics_fob;
// import data.scripts.world.systems.Diableavionics_outerTerminus;
// import data.scripts.world.systems.Diableavionics_stagging;

import data.scripts.espc_ModPlugin;
import data.scripts.world.systems.espc_GenHalestar;
import data.scripts.world.systems.espc_GenKhemsala;
import exerelin.campaign.SectorManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class espc_WorldGen implements SectorGeneratorPlugin {

	private boolean nexRandom = false;
	// private boolean isNewGame = false;
    
    public void generate(SectorAPI sector, boolean isNewGame, boolean nexRandom) {
    	this.nexRandom = nexRandom;
    	// this.isNewGame = isNewGame;
    	this.generate(sector);
    }
    
    private void initRelations(SectorAPI sector, FactionAPI faction) {

        for (FactionAPI thisFac : sector.getAllFactions())
        	faction.setRelationship(thisFac.getId(), -0.15f);
        
        faction.setRelationship(Factions.PLAYER, -0.1f);
        faction.setRelationship(Factions.INDEPENDENT, 0.0f);
        faction.setRelationship(Factions.PIRATES, -0.5f);
        faction.setRelationship(Factions.PERSEAN, -0.15f);
        faction.setRelationship(Factions.TRITACHYON, -0.2f);
        faction.setRelationship(Factions.HEGEMONY, -0.25f);
        
        faction.setRelationship(Factions.DIKTAT, -0.4f);
        faction.setRelationship(Factions.LIONS_GUARD, -0.4f);
        
        faction.setRelationship(Factions.KOL, -0.2f);
        faction.setRelationship(Factions.LUDDIC_CHURCH, -0.2f);
        faction.setRelationship(Factions.LUDDIC_PATH, -0.8f);
        
        faction.setRelationship(Factions.REMNANTS, 1.0f);
        faction.setRelationship(Factions.DERELICT, 0.8f);
        faction.setRelationship(Factions.OMEGA, -0.5f);
        
        // feel free to lmk if you think any of these are off this is more or less best judgment over cursory knowledge
        faction.setRelationship("al_ars", 0.0f);
        faction.setRelationship("apex_design", 0.1f);
        faction.setRelationship("blackrock_driveyards", -0.1f);
        faction.setRelationship("blade_breakers", -0.5f);
        faction.setRelationship("brighton", -0.15f);
        faction.setRelationship("cabal", -0.6f);
        faction.setRelationship("COPS", -0.25f);
        faction.setRelationship("dassault_mikoyan", 0.0f);
        faction.setRelationship("diableavionics", -0.4f);
        faction.setRelationship("draco", -0.25f);
        faction.setRelationship("fang", -0.25f);
        faction.setRelationship("fpe", -0.25f);
        faction.setRelationship("gmda", -0.25f);
        faction.setRelationship("gmda_patrol", -0.25f);
        faction.setRelationship("ii_imperial_guard", -0.25f);
        faction.setRelationship("IndEvo_derelict", 0.8f);
        faction.setRelationship("interstellarimperium", -0.25f);
        faction.setRelationship("ironsentinel", -0.25f);
        faction.setRelationship("ironshell", -0.25f);
        faction.setRelationship("hiigaran_descendants", -0.05f);
        faction.setRelationship("hmi", -0.15f);
        faction.setRelationship("kadur_remnant", -0.15f);
        faction.setRelationship("kingdom_of_terra", -0.25f);
        faction.setRelationship("magellan_leveller", 0.1f);
        faction.setRelationship("magellan_protectorate", -0.1f);
        faction.setRelationship("mayasura", 0.0f);
        faction.setRelationship("neutrinocorp", -0.1f);
        faction.setRelationship("ORA", 0f);
        faction.setRelationship("pearson_exotronics", 0f);
        faction.setRelationship("roider", 0.05f);
        faction.setRelationship("tahlan_legioinfernalis", -0.75f);
        faction.setRelationship("templars", -0.5f);
        faction.setRelationship("tiandong", 0.0f);
        faction.setRelationship("the_cartel", 0.0f);
        faction.setRelationship("scalartech", 0f);
        faction.setRelationship("sevencorp", 0.05f);
        faction.setRelationship("star_federation", 0.05f);
        faction.setRelationship("SCY", 0f);
        faction.setRelationship("sylphon", 0.05f);
        faction.setRelationship("uaf", -0.2f);
        faction.setRelationship("unitedpamed", 0.0f);
        faction.setRelationship("xhanempire", -0.6f);
    }
    
    @Override
    public void generate(SectorAPI sector) {
        initRelations(sector, sector.getFaction("epsilpac"));

        SharedData.getData().getPersonBountyEventData().addParticipatingFaction("epsilpac");
        // Global.getSector().getFaction("epsilrem").setShowInIntelTab(false);
        
        if (!nexRandom){
            new espc_GenHalestar().generate(Global.getSector());
            new espc_GenKhemsala().generate(Global.getSector());
        }
        
    }
}