package data.scripts.world.systems;

// import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
// import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;

import data.scripts.util.espc_MarketUtil;

import java.awt.*;

public class espc_GenHalestar implements SectorGeneratorPlugin
{
    @Override
    public void generate(SectorAPI sector)
    {
        StarSystemAPI system = sector.createStarSystem("espc_halestar");
        system.setBaseName("Halestar");

        system.getLocation().set(-22800, 10205);
        // system.setBackgroundTextureFilename("graphics/backgrounds/background2.jpg");
        // system.setBackgroundTextureFilename("graphics/backgrounds/background5.jpg");
        PlanetAPI systemStar = system.initStar("espc_halestar_star", StarTypes.WHITE_DWARF, 200f, 300f);
        
        // proc-gen orbit time: radius / (20f + rand(5f))
        PlanetAPI lunron = system.addPlanet(
        		"espc_lunron",
        		systemStar,
        		"Lunron Saba",
        		"espc_lunron",
        		30f,
        		190f,
        		6000f,
        		390f);
        lunron.setFaction("epsilpac");
        lunron.setCustomDescriptionId("espc_lunron");
        lunron.setInteractionImage("illustrations", "espc_lunron_illustration");
        // lunron.setInteractionImage("illustrations", "espc_lunron_illu");
        // lunron.applySpecChanges();
        
        PlanetAPI giver = system.addPlanet(
        		"espc_giver",
        		systemStar,
        		"Giver's Gaze",
        		"espc_giver",
        		130f,
        		260f,
        		4500f,
        		255f);
        giver.setFaction("epsilpac");
        giver.setCustomDescriptionId("espc_giver");
        
        PlanetAPI bruniel = system.addPlanet(
        		"espc_bruniel",
        		giver,
        		"Bruniel",
        		"espc_bruniel",
        		50f,
        		90f,
        		550f,
        		50f);
        bruniel.setFaction("epsilpac");
        bruniel.setCustomDescriptionId("espc_bruniel");
        bruniel.setInteractionImage("illustrations", "espc_bruniel_illustration");
        
        PlanetAPI falris = system.addPlanet(
        		"espc_falris",
        		systemStar,
        		"Falris",
        		"espc_falris",
        		20f,
        		105f,
        		1900f,
        		120f);
        falris.setFaction("epsilpac");
        falris.setCustomDescriptionId("espc_falris");
        falris.setInteractionImage("illustrations", "espc_falris_illustration");

        SectorEntityToken halestar_commRelay = system.addCustomEntity(
                "espc_halestar_comm",
                "Halestar Relay",
                Entities.COMM_RELAY_MAKESHIFT,
                "epsilpac"
        );
        halestar_commRelay.setCircularOrbit(systemStar, 10f, 7900f, 505f);

        system.addAsteroidBelt(
                systemStar,
                50,
                3200f,
                200f,
                160f,
                240f,
                Terrain.ASTEROID_BELT,
                "Hale Ring"
        );
        
        
        system.addRingBand(systemStar,
                "misc",
                "rings_asteroids0",
                256f,
                2,
                Color.WHITE,
                180f,
                3200f,
                200f,
                null,
                null
        );
		system.addRingBand(systemStar, "misc", "rings_dust0", 256f, 3, Color.white, 256f, 3150f - 50, 200f, null, null);
		system.addRingBand(systemStar, "misc", "rings_asteroids0", 256f, 3, Color.white, 256f, 3250f, 200f, null, null);

        JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint("espc_halestar_jump", "Halestar Inner Portal");
        jumpPoint.setOrbit(Global.getFactory().createCircularOrbit(systemStar, 180f, 2600f, 75f));
        jumpPoint.setRelatedPlanet(falris);
        jumpPoint.setStandardWormholeToHyperspaceVisual();
        system.addEntity(jumpPoint);
		
        JumpPointAPI jumpPointOuter = Global.getFactory().createJumpPoint("espc_halestar_jump", "Fringe Jump-Point");
        jumpPointOuter.setOrbit(Global.getFactory().createCircularOrbit(systemStar, 70f, 8700f, 400f));
        // jumpPointOuter.setRelatedPlanet(lunron);
        jumpPointOuter.setStandardWormholeToHyperspaceVisual();
        system.addEntity(jumpPointOuter);

        Misc.initConditionMarket(giver);
        MarketAPI giverEmptyMarket = giver.getMarket();
        giverEmptyMarket.addCondition(Conditions.VOLATILES_PLENTIFUL);
        giverEmptyMarket.addCondition(Conditions.EXTREME_WEATHER);
        giverEmptyMarket.addCondition(Conditions.TOXIC_ATMOSPHERE);
        giverEmptyMarket.addCondition(Conditions.DENSE_ATMOSPHERE);
        giverEmptyMarket.addCondition(Conditions.HIGH_GRAVITY);
        for (MarketConditionAPI condition : giverEmptyMarket.getConditions()) {
            condition.setSurveyed(true);
        }
        giverEmptyMarket.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
        
        MarketAPI lunronMarket = espc_MarketUtil.makeMarket("espc_lunron_market", lunron, 6,
        	new String[] {
        		Conditions.POPULATION_6,
        		Conditions.TOXIC_ATMOSPHERE,
        		Conditions.MILD_CLIMATE,
        		Conditions.FARMLAND_BOUNTIFUL,
        		Conditions.ORGANICS_ABUNDANT,
        		Conditions.ORE_MODERATE,
        		Conditions.RARE_ORE_SPARSE,
        	},
        	new String[][] {
        		{Industries.POPULATION},
        		{Industries.MEGAPORT, 
        			//Items.FULLERENE_SPOOL
        		},
        		{Industries.BATTLESTATION_HIGH},
        		{Industries.HEAVYBATTERIES},
        		{Industries.PATROLHQ},
        		{Industries.ORBITALWORKS, Items.PRISTINE_NANOFORGE},
        		{Industries.FARMING},
        		{Industries.MINING},
        		{"commerce", Items.DEALMAKER_HOLOSUITE},
        	},
        	new String[] {
            	"espc_open_market",
            	Submarkets.GENERIC_MILITARY,
            	// "espc_generic_military",
            	"espc_black_market",
            	Submarkets.SUBMARKET_STORAGE
        	}
        );
        
        sector.getEconomy().addMarket(lunronMarket, true);
        
        MarketAPI brunielMarket = espc_MarketUtil.makeMarket("espc_bruniel_market", bruniel, 5,
        	new String[] {
        		Conditions.POPULATION_5,
        		Conditions.HOT,
        		Conditions.THIN_ATMOSPHERE,
        		Conditions.EXTREME_WEATHER,
        		Conditions.VOLATILES_DIFFUSE,
        		Conditions.ORE_MODERATE,
        	},
        	new String[][] {
        		{Industries.POPULATION},
        		{Industries.SPACEPORT 
        			// ,Items.FULLERENE_SPOOL
        		},
        		{Industries.MINING, Items.MANTLE_BORE},
        		{Industries.LIGHTINDUSTRY},
        		{Industries.REFINING
        			// , Items.CATALYTIC_CORE
        		},
        	},
        	new String[] {
            	"espc_open_market",
            	// "espc_generic_military",
            	"espc_black_market",
            	Submarkets.SUBMARKET_STORAGE
            }
        );

        SectorEntityToken brunielStation = system.addCustomEntity(
        	"espc_bruniel_station", "Bruniel Station", "espc_station_bruniel", "epsilpac");
        brunielStation.setFaction("epsilpac");
        brunielStation.setMarket(brunielMarket);
        brunielStation.setCircularOrbitPointingDown(bruniel, 70f, 160f, 40f);
        // brunielStation.setCustomDescriptionId("espc_station_bruniel");

        sector.getEconomy().addMarket(brunielMarket, true);
        
        MarketAPI falrisMarket = espc_MarketUtil.makeMarket("espc_falris_market", falris, 4,
            	new String[] {
            		Conditions.POPULATION_4,
            		Conditions.VERY_HOT,
            		Conditions.EXTREME_TECTONIC_ACTIVITY,
            		Conditions.NO_ATMOSPHERE,
            		Conditions.ORE_RICH,
            		Conditions.RARE_ORE_ABUNDANT,
            	},
            	new String[][] {
            		{Industries.POPULATION},
            		{Industries.MEGAPORT},
            		{Industries.WAYSTATION},
            		{Industries.HEAVYBATTERIES},
            		{Industries.HIGHCOMMAND, Items.CRYOARITHMETIC_ENGINE},
            		{Industries.BATTLESTATION_HIGH},
            		{Industries.FUELPROD, Items.SYNCHROTRON},
            	},
            	new String[] {
                	"espc_open_market",
                	Submarkets.GENERIC_MILITARY,
                	// "espc_generic_military",
                	"espc_black_market",
                	Submarkets.SUBMARKET_STORAGE
                }
            );
        
        falrisMarket.setImmigrationIncentivesOn(true);
        sector.getEconomy().addMarket(falrisMarket, true);

        /*
        if (Global.getSettings().getModManager().isModEnabled("IndEvo")) {
        	
        }*/
        
        // system.autogenerateHyperspaceJumpPoints();
		system.autogenerateHyperspaceJumpPoints(true, false);
        system.generateAnchorIfNeeded();
		Misc.setAllPlanetsSurveyed(system, true);

        HyperspaceTerrainPlugin hyperspacePlugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
        new NebulaEditor(hyperspacePlugin).clearArc(system.getLocation().x, system.getLocation().y, 0,
        	hyperspacePlugin.getTileSize() * 2f + system.getMaxRadiusInHyperspace(), 0f, 360f, 0.25f
        );
    }
    
}
