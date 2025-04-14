package data.scripts.world.systems;

// import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
// import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
// import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;

import data.scripts.util.espc_MarketUtil;

public class espc_GenKhemsala implements SectorGeneratorPlugin
{
    @Override
    public void generate(SectorAPI sector)
    {
        StarSystemAPI system = sector.createStarSystem("espc_khemsala");
        system.setBaseName("Khemsala");

        system.getLocation().set(-26000, 6500);
        // system.setBackgroundTextureFilename("graphics/backgrounds/background2.jpg");
        // system.setBackgroundTextureFilename("graphics/backgrounds/background5.jpg");
        // make star
        PlanetAPI systemStar = system.initStar("espc_khemsala_star", StarTypes.YELLOW, 600f, 750f);
        // make planets
        
        PlanetAPI tocquiera = system.addPlanet(
        		"espc_tocquiera",
        		systemStar,
        		"Tocquiera",
        		"espc_tocquiera",
        		30f,
        		210f,
        		3900f,
        		260f);
        tocquiera.setFaction("epsilpac");
        tocquiera.setCustomDescriptionId("espc_tocquiera");
        tocquiera.setInteractionImage("illustrations", "espc_tocquiera_illustration");

        SectorEntityToken khemsala_commRelay = system.addCustomEntity(
                "espc_khemsala_comm",
                "Khemsala Relay",
                Entities.COMM_RELAY_MAKESHIFT,
                "epsilpac"
        );
        khemsala_commRelay.setCircularOrbit(systemStar, 20f, 4800f, 270f);

        JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint("espc_khemsala_jump", "Khemsala Inner Portal");
        jumpPoint.setOrbit(Global.getFactory().createCircularOrbit(systemStar, 0f, 2500f, 125f));
        jumpPoint.setRelatedPlanet(tocquiera);
        jumpPoint.setStandardWormholeToHyperspaceVisual();
        system.addEntity(jumpPoint);
        
        MarketAPI tocquieraMarket = espc_MarketUtil.makeMarket("espc_tocquiera_market", tocquiera, 5,
        	new String[] {
        		Conditions.POPULATION_5,
        		Conditions.HABITABLE,
        		// Conditions.FREE_PORT,
        		Conditions.RUINS_EXTENSIVE,
        		Conditions.FARMLAND_RICH,
        		Conditions.ORGANICS_ABUNDANT,
        		Conditions.ORE_ABUNDANT,
        		Conditions.RARE_ORE_MODERATE,
        		Conditions.DECIVILIZED_SUBPOP,
        	},
        	new String[][] {
        		{Industries.POPULATION},
        		{Industries.SPACEPORT},
        		{Industries.WAYSTATION},
        		{Industries.STARFORTRESS_HIGH},
        		{Industries.PATROLHQ},
        		{Industries.FARMING},
        		{Industries.TECHMINING},
        		{"commerce"},
        		{"espc_enclaves"},
        	},
        	new String[] {
               	"espc_open_market",
                Submarkets.GENERIC_MILITARY,
                Submarkets.SUBMARKET_BLACK,
        		Submarkets.SUBMARKET_STORAGE
        	}
        );

        /*SectorEntityToken tocquieraStation = system.addCustomEntity("espc_tocquiera_station", "Tocquiera Station", "station_hightech3", "epsilpac");
        tocquieraStation.setMarket(tocquieraMarket);
        tocquieraStation.setCircularOrbitPointingDown(tocquiera, 30f, 250f, 60f);
        tocquieraStation.setCustomDescriptionId("espc_tocquiera_station_description");
        tocquieraStation.setInteractionImage("illustrations", "espc_tocquiera_station_illustration"); */
        
        sector.getEconomy().addMarket(tocquieraMarket, true);
        /*
        if (Global.getSettings().getModManager().isModEnabled("IndEvo")) {
        	
        }*/
        
        system.autogenerateHyperspaceJumpPoints();
        system.generateAnchorIfNeeded();

        HyperspaceTerrainPlugin hyperspacePlugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
        new NebulaEditor(hyperspacePlugin).clearArc(system.getLocation().x, system.getLocation().y, 0,
        	hyperspacePlugin.getTileSize() * 2f + system.getMaxRadiusInHyperspace(), 0f, 360f, 0.25f
        );
    }
}
