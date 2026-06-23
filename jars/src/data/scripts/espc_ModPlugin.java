package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin.PickPriority;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.enc.EncounterManager;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.util.Misc;
import com.thoughtworks.xstream.XStream;

import java.util.Set;

// import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;

// import data.campaign.ids.espc_People;
import data.scripts.ai.espc_FinneganAI;
import data.scripts.campaign.enc.espc_OutsideSystemEpsilpacEPEC;
import data.scripts.campaign.enc.espc_SlipstreamEpsilpacEPEC;
import data.scripts.campaign.intel.events.PactProgressionEventIntel;
import data.scripts.campaign.listeners.espc_CargoListener;
import data.scripts.campaign.listeners.espc_ColonyInteractionListener;
import data.scripts.campaign.listeners.espc_EconomyListener;
import data.scripts.campaign.listeners.espc_PactFleetSpawnListener;
import data.scripts.campaign.plugins.espc_CampaignPlugin;
import data.scripts.util.MezzUtils;
import data.scripts.world.espc_WorldGen;
import data.scripts.world.espc_People;
// import com.fs.starfarer.api.PluginPick;
// import data.scripts.world.systems.espc_GenHalestar;
// import data.scripts.world.systems.espc_GenKhemsala;
import exerelin.campaign.SectorManager;
import exerelin.campaign.backgrounds.CharacterBackgroundUtils;

public class espc_ModPlugin extends BaseModPlugin {

    private static boolean hasNex = false;
    private static boolean hasGlib = false;
    private static boolean hasLuna = false;
    
    // for code divers: this is just for mission ai selection in testing, see pickShipAI
    private static boolean testMode = false;

    @Override
    public void onApplicationLoad() {
    	EncounterManager.CREATORS.add(new espc_SlipstreamEpsilpacEPEC());
    	EncounterManager.CREATORS.add(new espc_OutsideSystemEpsilpacEPEC());

        hasNex = Global.getSettings().getModManager().isModEnabled("nexerelin");
        hasLuna = Global.getSettings().getModManager().isModEnabled("lunalib");
        testMode = testMode && Global.getSettings().getModManager().isModEnabled("espcdev");
        {
            if (!Global.getSettings().getModManager().isModEnabled("lw_lazylib"))
                throw new RuntimeException(
                	MezzUtils.getString("espc_miscui", "lazylibreq"));
            
            if (!Global.getSettings().getModManager().isModEnabled("MagicLib"))
                throw new RuntimeException(
                		MezzUtils.getString("espc_miscui", "magiclibreq"));
            
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

	@Override
	public void onAboutToLinkCodexEntries() {
		
    	CodexDataV2.makeRelated(CodexDataV2.getWeaponEntryId("espc_amflamer"),
    		CodexDataV2.getWeaponEntryId("espc_amflamersolo"));
    	CodexDataV2.makeRelated(CodexDataV2.getWeaponEntryId("espc_remdriver"),
        		CodexDataV2.getWeaponEntryId("espc_remmortar"));
    	CodexDataV2.makeRelated(CodexDataV2.getWeaponEntryId("espc_as"),
    		CodexDataV2.getWeaponEntryId("espc_aa"));
    	CodexDataV2.makeRelated(CodexDataV2.getShipEntryId("espc_ember"),
        		CodexDataV2.getWeaponEntryId("espc_amflamer"));
    	CodexDataV2.makeRelated(CodexDataV2.getShipEntryId("picket"),
    		CodexDataV2.getShipEntryId("espc_picket"));
    	CodexDataV2.makeRelated(CodexDataV2.getShipEntryId("sentry"),
    		CodexDataV2.getShipEntryId("espc_sentry"));
    	CodexDataV2.makeRelated(CodexDataV2.getShipEntryId("warden"),
    		CodexDataV2.getShipEntryId("espc_warden"));
    	CodexDataV2.makeRelated(CodexDataV2.getShipEntryId("bastillon"),
    		CodexDataV2.getShipEntryId("espc_bastillon"));
    	CodexDataV2.makeRelated(CodexDataV2.getShipEntryId("berserker"),
    		CodexDataV2.getShipEntryId("espc_berserker"));
    	CodexDataV2.makeRelated(CodexDataV2.getShipEntryId("rampart"),
    		CodexDataV2.getShipEntryId("espc_rampart"));
    	CodexDataV2.makeRelated(CodexDataV2.getFighterEntryId("borer_wing"),
    		CodexDataV2.getFighterEntryId("espc_borer_wing"));
    	CodexDataV2.makeRelated(CodexDataV2.getShipEntryId("espc_bastillon"),
        	CodexDataV2.getHullmodEntryId("pointdefenseai"));

    	CodexDataV2.makeRelated(CodexDataV2.getShipEntryId("espc_bastillon"),
        	CodexDataV2.getHullmodEntryId("pointdefenseai"));
    	CodexDataV2.makeRelated(CodexDataV2.getShipSystemEntryId("espc_Alternator"),
        		CodexDataV2.getHullmodEntryId("espc_alternatorinfomod"));
    	CodexDataV2.makeRelated(CodexDataV2.getShipSystemEntryId("espc_Salvo"),
        		CodexDataV2.getHullmodEntryId("espc_salvoinfomod"));

    	CodexDataV2.makeRelated(CodexDataV2.getShipSystemEntryId("espc_InverseSkimmer"),
    		CodexDataV2.getShipSystemEntryId("displacer"));
    	CodexDataV2.makeRelated(CodexDataV2.getShipSystemEntryId("espc_WideInverseSkimmer"),
        	CodexDataV2.getShipSystemEntryId("displacer"));
    	CodexDataV2.makeRelated(CodexDataV2.getShipSystemEntryId("espc_InverseSkimmer"),
            CodexDataV2.getShipSystemEntryId("espc_WideInverseSkimmer"));
    	CodexDataV2.makeRelated(CodexDataV2.getShipSystemEntryId("espc_RemoteBurn"),
                CodexDataV2.getShipSystemEntryId("microburn"));
    	CodexDataV2.makeRelated(CodexDataV2.getShipSystemEntryId("espc_NovaStrike"),
            CodexDataV2.getShipSystemEntryId("nova_burst"));
    	
    	CodexDataV2.makeRelated(CodexDataV2.getShipSystemEntryId("espc_InverseSkimmer"),
        	CodexDataV2.getHullmodEntryId("espc_pactskin"));
        CodexDataV2.makeRelated(CodexDataV2.getShipSystemEntryId("espc_WideInverseSkimmer"),
            CodexDataV2.getHullmodEntryId("espc_pactskin"));
        CodexDataV2.makeRelated(CodexDataV2.getShipSystemEntryId("espc_RemoteBurn"),
        	CodexDataV2.getHullmodEntryId("espc_pactskin"));
        CodexDataV2.makeRelated(CodexDataV2.getShipSystemEntryId("espc_NovaStrike"),
        	CodexDataV2.getHullmodEntryId("espc_pactskin"));
    	
	}

	@Override
	public void onCodexDataGenerated() {
		
		Set<String> entryIds = CodexDataV2.getEntry(CodexDataV2.getCommodityEntryId(Commodities.DOMESTIC_GOODS)).getRelatedEntryIds();
		for (String entry : entryIds)
			CodexDataV2.makeUnrelated(CodexDataV2.getCommodityEntryId("espc_vintage"), entry);
		
		entryIds = CodexDataV2.getEntry(CodexDataV2.getCommodityEntryId(Commodities.LUXURY_GOODS)).getRelatedEntryIds();
		for (String entry : entryIds)
			if (CodexDataV2.getEntry(entry).getParam() instanceof CommoditySpecAPI)
				CodexDataV2.makeRelated(CodexDataV2.getCommodityEntryId("espc_vintage"), entry);

		CodexDataV2.makeUnrelated(CodexDataV2.getCommodityEntryId("espc_vintage"), 
			CodexDataV2.getCommodityEntryId(Commodities.DOMESTIC_GOODS));
		CodexDataV2.makeRelated(CodexDataV2.getCommodityEntryId("espc_vintage"), 
			CodexDataV2.getCommodityEntryId(Commodities.LUXURY_GOODS));
		CodexDataV2.makeRelated(CodexDataV2.getCommodityEntryId("espc_vintage"), 
			CodexDataV2.getIndustryEntryId("espc_enclaves"));
		
    	
	}
	
	@Override
	public void configureXStream(XStream x) {
		super.configureXStream(x);
		x.alias("EspcCargoListener", espc_CargoListener.class);
	}
    
    public static boolean hasNex() {
    	return hasNex;
    }
    
    public static boolean hasGlib() {
    	return hasGlib;
    }

    public static boolean hasLuna() {
    	return hasLuna;
    }

    @Override
    public void onNewGame() {
        new espc_WorldGen().generate(Global.getSector(), true,
        	(hasNex && !SectorManager.getManager().isCorvusMode())
        );
        if (MezzUtils.getSetting("DoctrinalProgression")) {
            // PactProgressionEventIntel intel = new PactProgressionEventIntel();
            // Global.getSector().getIntelManager().addIntel(intel);
        }
    }
    
	@Override
	public void onNewGameAfterProcGen() {
    	// if (espc_generateDerelicts)
	}
	
	@Override
	public void onNewGameAfterEconomyLoad() {
		espc_People.create(true, hasNex && !SectorManager.getManager().isCorvusMode());
	}
	
	@Override
	public void onGameLoad(boolean isNewGame) {
		if (!SharedData.getData().getPersonBountyEventData().getParticipatingFactions().contains(MezzUtils.factionIdPact)) {
	        new espc_WorldGen().generate(Global.getSector(), isNewGame,
	        	(hasNex && !SectorManager.getManager().isCorvusMode())
	        );
	        if (!isNewGame)
	        	espc_People.create(false, hasNex && !SectorManager.getManager().isCorvusMode());
		}
		Global.getSector().addTransientListener(
			new espc_PactFleetSpawnListener(false)
		);
		Global.getSector().addTransientListener(
			new espc_EconomyListener(false,
				(hasNex && !SectorManager.getManager().isCorvusMode())
			)
		);
		espc_CargoListener.getInstance();
		
		if (!Global.getSector().getListenerManager().hasListenerOfClass(espc_ColonyInteractionListener.class))
			Global.getSector().getListenerManager().addListener(new espc_ColonyInteractionListener());
		
		if (hasNex) {
			if ((CharacterBackgroundUtils.isBackgroundActive("espc_realHumanBeing") ||
			CharacterBackgroundUtils.isBackgroundActive("espc_realHumbleBeing"))) {
				Global.getSector().registerPlugin(new espc_CampaignPlugin());
			}
			if (CharacterBackgroundUtils.isBackgroundActive("espc_realHumanBeing") ||
				CharacterBackgroundUtils.isBackgroundActive("espc_realHumbleBeing")) {
				// not the nicest solution, but one i've come to learn exists.  this is a silly feature anyways.
				if (MezzUtils.getSetting("MeatlessSalvage")) {
					SalvageEntity.BASE_CREW = 0;
				} else if (SalvageEntity.BASE_CREW == 0)
					SalvageEntity.BASE_CREW = 30;
			}
		}
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
    	if (!MezzUtils.getSetting("CustomShipAI") && ship.getOwner() == 0 && !ship.isAlly())
    		return null;
        if (ship.getHullSpec().getHullId().contains("espc_chorale")) {
            ShipAIConfig config = new ShipAIConfig();
            if (ship.getCaptain() == null || ship.getCaptain() != null && ship.getCaptain().isDefault()) {
            	config.personalityOverride = Personalities.AGGRESSIVE;
            }
            config.turnToFaceWithUndamagedArmor = false;
            config.alwaysStrafeOffensively = true;
            return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, config), PickPriority.MOD_SET);
        } else if (ship.getHullSpec().getHullId().contains("wolf_espc")) {
        	ShipAIConfig config = new ShipAIConfig();
        	config.turnToFaceWithUndamagedArmor = false;
        } else if (ship.getHullSpec().getHullId().contains("espc_observer") &&
        	!ship.getHullSpec().getHullId().contains("espc_observer_tt") ||
            ship.getHullSpec().getBaseHullId().equals("espc_amanuensis") ||
        	ship.getHullSpec().getBaseHullId().equals("espc_flagbearer") ||
        	ship.getHullSpec().getHullId().contains("espc_jackalope")) {
        	if (ship.getCaptain() != null && !ship.getCaptain().isDefault()) {
            	return null;
        	}
            ShipAIConfig config = new ShipAIConfig();
            config.personalityOverride = Personalities.AGGRESSIVE;
			config.turnToFaceWithUndamagedArmor = !ship.getHullSpec().getHullId().contains("espc_jackalope");
            return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, config), PickPriority.MOD_SET);
        } else if (member != null && Misc.isAutomated(ship) && member.getFleetData() != null && member.getFleetData().getFleet() != null &&
        	member.getFleetData().getFleet().getFaction() != null &&
        	// won't catch nex allied fleets, but whatever.
        	(member.getFleetData().getFleet().getFaction().getId().contains(MezzUtils.factionIdPact) ||
        	(member.getFleetData().getFleet().getFaction().getId().contains(Factions.PIRATES)) 
        		&& ship.getName().contains(Global.getSector().getFaction(MezzUtils.factionIdPact).getShipNamePrefix()))
        	&& !ship.getHullSpec().getBaseHullId().equals("espc_rampart")
        	// && !ship.getHullSpec().getHullId().contains("radiant")
        	) {
            ShipAIConfig config = new ShipAIConfig();
            config.personalityOverride = ship.hasLaunchBays() ? Personalities.STEADY : Personalities.AGGRESSIVE;
            if (ship.getHullSpec().getHullId().contains("lumen_espc") ||
                ship.getHullSpec().getHullId().contains("radiant_espc"))
                config.turnToFaceWithUndamagedArmor = false;
            return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, config), PickPriority.MOD_SPECIFIC);
        } else if (Misc.isAutomated(ship) &&
        	ship.getCaptain() != null && ship.getCaptain().getFaction() != null &&
           	(ship.getCaptain().getFaction().getId().contains(MezzUtils.factionIdPact) ||
           	(ship.getCaptain().getFaction().getId().contains(Factions.PIRATES)) && 
           	ship.getName().contains(Global.getSector().getFaction(MezzUtils.factionIdPact).getShipNamePrefix()))
           	&& !ship.getHullSpec().getBaseHullId().equals("espc_rampart")
        	// && !ship.getHullSpec().getHullId().contains("radiant")
           	) {
            ShipAIConfig config = new ShipAIConfig();
            // config.personalityOverride = ship.getCaptain().getPersonalityAPI().getId();
            config.personalityOverride = Personalities.AGGRESSIVE;
            if (ship.getHullSpec().getHullId().contains("lumen_espc") ||
            	ship.getHullSpec().getHullId().contains("radiant_espc"))
            	config.turnToFaceWithUndamagedArmor = false;
            return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, config), PickPriority.MOD_SPECIFIC);
        }
        if (!testMode)
        	return null;
        // overrides for use in mission balance testing, to replicate desired campaign behaviour
        if (Misc.isAutomated(ship) &&
       		ship.getName() != null && ship.getName().contains(Global.getSector().getFaction(MezzUtils.factionIdPact).getShipNamePrefix()) 
       		&& !ship.hasLaunchBays()
	    	&& !ship.getHullSpec().getBaseHullId().equals("espc_rampart")
	    	// && !ship.getHullSpec().getHullId().contains("radiant") 
        	) {
	        ShipAIConfig config = new ShipAIConfig();
	        config.personalityOverride = Personalities.AGGRESSIVE;
            if (ship.getHullSpec().getHullId().contains("lumen_espc") ||
                ship.getHullSpec().getHullId().contains("radiant_espc"))
                config.turnToFaceWithUndamagedArmor = false;
	        return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, config), PickPriority.MOD_SPECIFIC);
	    }
        else if (ship.getName() != null 
        	&& ship.getName().contains(Global.getSector().getFaction(MezzUtils.factionIdPact).getShipNamePrefix())
        	&& !ship.hasLaunchBays()
    	    // && !ship.getHullSpec().getHullId().contains("radiant") 
        	) {
	    	ShipAIConfig config = new ShipAIConfig();
	        config.personalityOverride = Personalities.AGGRESSIVE;
	        return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, config), PickPriority.MOD_SET);
	    }
        
        /* else if (ship.getHullSpec().getHullId().contains("espc_serenade")) {
            ShipAIConfig config = new ShipAIConfig();
            // config.alwaysStrafeOffensively = false;
            // config.backingOffWhileNotVentingAllowed = false;
            // config.turnToFaceWithUndamagedArmor = false;
            return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, config), PickPriority.MOD_SET);
        }*/
       return null;
    }
    

}
