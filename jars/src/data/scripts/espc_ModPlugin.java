package data.scripts;

// import java.io.IOException;

// import org.json.JSONException;
// import org.json.JSONObject;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin.PickPriority;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.util.Misc;

// import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;

import data.campaign.listeners.espc_ColonyInteractionListener;
import data.campaign.listeners.espc_EconomyListener;
import data.campaign.listeners.espc_PactFleetSpawnListener;
// import data.campaign.ids.espc_People;
import data.scripts.ai.espc_FinneganAI;
import data.scripts.world.espc_WorldGen;
import data.scripts.world.espc_People;
// import com.fs.starfarer.api.PluginPick;
// import data.scripts.world.systems.espc_GenHalestar;
// import data.scripts.world.systems.espc_GenKhemsala;
import exerelin.campaign.SectorManager;

public class espc_ModPlugin extends BaseModPlugin {

    private static boolean hasNex = false;
    private static boolean hasGlib = false;
    public static boolean espc_generateDerelicts = false;
    public static boolean espc_modifyExplorarium = true;

    @Override
    public void onApplicationLoad() {

        hasNex = Global.getSettings().getModManager().isModEnabled("nexerelin");
        {
            if (!Global.getSettings().getModManager().isModEnabled("lw_lazylib"))
                throw new RuntimeException("Epsilon Pact requires LazyLib to run - http://fractalsoftworks.com/forum/index.php?topic=5444");
            
            if (!Global.getSettings().getModManager().isModEnabled("MagicLib"))
                throw new RuntimeException("Epsilon Pact requires MagicLib to run - http://fractalsoftworks.com/forum/index.php?topic=13718.0");
            
            if (Global.getSettings().getModManager().isModEnabled("shaderLib")) {
            	hasGlib = true;
                ShaderLib.init();
                LightData.readLightDataCSV("data/lights/espc_light_data.csv");
                TextureData.readTextureDataCSV("data/lights/espc_texture_data.csv");
            }
            /*
            try {
                JSONObject setting = Global.getSettings().loadJSON("epsilon_pact_SETTINGS.json");
                espc_generateDerelicts = setting.getBoolean("espc_generateDerelicts");
                espc_modifyExplorarium = setting.getBoolean("espc_modifyExplorarium");
            } catch (IOException e) {
            	throw new RuntimeException("Unable to load settings file - please verify epsilon_pact_SETTINGS.json is in this mod's root folder");
            } catch (JSONException e) {
            	throw new RuntimeException("Unable to load settings file - please verify epsilon_pact_SETTINGS.json is in this mod's root folder");
            } */
        }

    }
    
    public static boolean hasNex() {
    	return hasNex;
    }
    
    public static boolean hasGlib() {
    	return hasGlib;
    }
    

    @Override
    public void onNewGame() {
        new espc_WorldGen().generate(Global.getSector(), true,
        	(hasNex && !SectorManager.getManager().isCorvusMode())
        );
    }
    
	@Override
	public void onNewGameAfterProcGen() {
    	// if (espc_generateDerelicts)
	}
	
	@Override
	public void onNewGameAfterEconomyLoad() {
		if (!hasNex || SectorManager.getManager().isCorvusMode()) {
			espc_People.create(true);
		}
	}
	
	@Override
	public void onGameLoad(boolean isNewGame) {
		if (!SharedData.getData().getPersonBountyEventData().getParticipatingFactions().contains("epsilpac")) {
	        new espc_WorldGen().generate(Global.getSector(), isNewGame,
	        	(hasNex && !SectorManager.getManager().isCorvusMode())
	        );
	        if (!isNewGame && (!hasNex || SectorManager.getManager().isCorvusMode()))
	        	espc_People.create(false);
		}
		Global.getSector().addTransientListener(
			new espc_PactFleetSpawnListener(false)
		);
		Global.getSector().addTransientListener(
			new espc_EconomyListener(false,
				(hasNex && !SectorManager.getManager().isCorvusMode())
			)
		);
		if (!Global.getSector().getListenerManager().hasListenerOfClass(espc_ColonyInteractionListener.class))
			Global.getSector().getListenerManager().addListener(new espc_ColonyInteractionListener());
	}
	
	@Override
	public PluginPick<MissileAIPlugin> pickMissileAI (MissileAPI missile, ShipAPI ship) {
		if (missile.getProjectileSpecId().equals("espc_procession_missile"))
			return null;
			// return new PluginPick<MissileAIPlugin>(new MissileAIPlugin(), PickPriority.MOD_SPECIFIC);
		else if (missile.getProjectileSpecId().equals("espc_finnegan_shot"))
			return new PluginPick<MissileAIPlugin>(
				new espc_FinneganAI(missile, ship), PickPriority.MOD_SPECIFIC
			);
		else
			return null;
	}
	
	
    @Override
    public PluginPick<ShipAIPlugin> pickShipAI(FleetMemberAPI member, ShipAPI ship) {

        if (ship.getHullSpec().getBaseHullId().equals("espc_chorale")) {
            ShipAIConfig config = new ShipAIConfig();
            // i am so tired of fiddling with this game's wonky ass black box ai.
            if (ship.getCaptain() == null || ship.getCaptain() != null && ship.getCaptain().getNameString().equals(""))
            	config.personalityOverride = Personalities.AGGRESSIVE;
            config.turnToFaceWithUndamagedArmor = false;
            config.alwaysStrafeOffensively = true;
            return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, config), PickPriority.MOD_SET);
        } else if (ship.getHullSpec().getBaseHullId().equals("espc_observer") ||
        	ship.getHullSpec().getBaseHullId().equals("espc_flagbearer") ||
        	ship.getHullSpec().getBaseHullId().equals("espc_jackalope")) {
        	if (ship.getCaptain() != null && !ship.getCaptain().getNameString().equals("")) {
            	return null;
        	}
            ShipAIConfig config = new ShipAIConfig();
            config.personalityOverride = Personalities.AGGRESSIVE;
            return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, config), PickPriority.MOD_SET);
        } else if (member != null && Misc.isAutomated(ship) && member.getFleetData() != null && member.getFleetData().getFleet() != null &&
        	member.getFleetData().getFleet().getFaction() != null &&
        	member.getFleetData().getFleet().getFaction().getId().equals("epsilpac") &&
        	!ship.hasLaunchBays() &&
        	!ship.getHullSpec().getBaseHullId().equals("espc_rampart") &&
        	!ship.getHullSpec().getBaseHullId().equals("radiant")) {
            ShipAIConfig config = new ShipAIConfig();
            config.personalityOverride = Personalities.AGGRESSIVE;
            return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, config), PickPriority.MOD_SPECIFIC);
            
        }
        /*  -- overrides for use in balance testing, to replicate desired campaign behaviour --
 
        else if (Misc.isAutomated(ship) &&
       		ship.getName() != null && ship.getName().contains("EPS") && !ship.hasLaunchBays()
	    	&& !ship.getHullSpec().getBaseHullId().equals("espc_rampart")
	    	&& !ship.getHullSpec().getBaseHullId().equals("radiant")) {
	        ShipAIConfig config = new ShipAIConfig();
	        config.personalityOverride = Personalities.AGGRESSIVE;
	        return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, config), PickPriority.MOD_SPECIFIC);
	    } else if (ship.getName() != null && ship.getName().contains("EPS") && !ship.hasLaunchBays()) {
	        ShipAIConfig config = new ShipAIConfig();
	        config.personalityOverride = Personalities.AGGRESSIVE;
	        return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, config), PickPriority.MOD_SET);
	    }
	    */
        
        /* else if (ship.getHullSpec().getBaseHullId().equals("espc_serenade")) {
            ShipAIConfig config = new ShipAIConfig();
            // config.alwaysStrafeOffensively = false;
            // config.backingOffWhileNotVentingAllowed = false;
            // config.turnToFaceWithUndamagedArmor = false;
            return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, config), PickPriority.MOD_SET);
        }*/
       return null;
    }
    

}
