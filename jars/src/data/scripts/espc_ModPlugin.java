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
import com.fs.starfarer.api.impl.campaign.shared.SharedData;

// import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;

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

    public static boolean hasNex = false;
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
		// check if fac is in 
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

    	// ai fixes for goofily-built hardpoint broadside ships
    	// offense is defense for these ships - them attempting to maneuver tends to lose them fights the player wouldn't.
    	// for the serenade in particular, attempting to 180 on a battleship without a movement system is a death sentence.
		// doesn't really stop it from testing but worth a shot.
    	
    	// ig this also affects the sindrian one.  phil can go fuck himself lmao i'm too lazy/spiteful to make an exception.
        if (ship.getHullSpec().getBaseHullId().equals("espc_chorale")) {
            ShipAIConfig config = new ShipAIConfig();
            config.alwaysStrafeOffensively = true;
            config.backingOffWhileNotVentingAllowed = true;
            config.turnToFaceWithUndamagedArmor = false;
            return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, config), PickPriority.MOD_SET);
        } else if (ship.getHullSpec().getBaseHullId().equals("espc_serenade")) {
            ShipAIConfig config = new ShipAIConfig();
            // config.alwaysStrafeOffensively = false;
            config.backingOffWhileNotVentingAllowed = false;
            config.turnToFaceWithUndamagedArmor = false;
            return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, config), PickPriority.MOD_SET);
        }
        	return null;
    }

}
