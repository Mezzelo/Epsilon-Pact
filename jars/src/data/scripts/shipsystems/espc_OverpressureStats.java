package data.scripts.shipsystems;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
// import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
// import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
//vimport com.fs.starfarer.api.util.Misc;

import data.scripts.util.MezzUtils;

public class espc_OverpressureStats extends BaseShipSystemScript {

	public static final float ROF_BONUS = -0.33f;
	public static final float DAMAGE_BONUS = 1.5f;
	public static final float VELOCITY_BONUS = 0.33f;
	public static final float RANGE_BONUS = 0.15f;
	
	private static final float AI_HULL_BACK = 0.35f;
	private static final float AI_FLUX_BACK = 0.3f;
	// private static final float AI_FLUX_FORCE_ENGAGE = 0.4f;
	
	private boolean pluginInit = false;
	private List<WeaponGroupAPI> groups = null;

	private ShipAIConfig origConfig = null;
	private int aiStateLast = 0;
	private static final float THINK_INTERVAL = 0.1f;
	private float aiThinkTime = THINK_INTERVAL;
	
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (stats.getEntity() == null)
			return;
		
		if (!pluginInit) {
			pluginInit = true;
			groups = new ArrayList<WeaponGroupAPI>();
			for (WeaponGroupAPI g : ((ShipAPI) stats.getEntity()).getWeaponGroupsCopy()) {
				boolean matching = true;
				for (WeaponAPI wep : g.getWeaponsCopy())
					if (matching && !wep.getType().equals(WeaponType.BALLISTIC))
						matching = false;
				
				if (matching)
					groups.add(g);
			}
			Global.getCombatEngine().addPlugin(new espc_OverpressureVFX((ShipAPI) stats.getEntity()));
		}

		ShipAPI ship = (ShipAPI) stats.getEntity();
		
		stats.getBallisticRoFMult().modifyMult(id, 1f + ROF_BONUS * effectLevel);
		stats.getBallisticWeaponDamageMult().modifyMult(id, 1f + DAMAGE_BONUS * effectLevel);
		stats.getBallisticProjectileSpeedMult().modifyMult(id, 1f + VELOCITY_BONUS * effectLevel);
		stats.getBallisticWeaponRangeBonus().modifyPercent(id, RANGE_BONUS * 100f);
		
		// as beams are contiguous, we can emulate the systems' effects on them with the ROF mult
		stats.getBeamWeaponDamageMult().modifyMult(id, 1f + ROF_BONUS * effectLevel);
		stats.getBeamWeaponFluxCostMult().modifyMult(id, 1f + ROF_BONUS * effectLevel);
		stats.getBeamWeaponRangeBonus().modifyPercent(id, RANGE_BONUS * 100f);

		if (aiStateLast == -2) {
			
		} else if (ship.getAI() == null) {
    		aiStateLast = -1;
    	} else if (origConfig == null) {
    		if (ship.getShipAI().getConfig() != null) {
				if ((!MezzUtils.getSetting("CustomShipAI") && ship.getOwner() == 0 && !ship.isAlly()) 
					|| Personalities.TIMID.equals(ship.getShipAI().getConfig().personalityOverride) || 
					Personalities.CAUTIOUS.equals(ship.getShipAI().getConfig().personalityOverride)) {
					aiStateLast = -2;
				} else {
					ShipAIConfig config = ship.getShipAI().getConfig();
					origConfig = config.clone();
				}
			}
    	} else {
    		aiThinkTime -= Global.getCombatEngine().getElapsedInLastFrame();
    		if (aiStateLast == -1 || aiThinkTime <= 0f) {
    			aiThinkTime = THINK_INTERVAL;
    			ShipAIConfig config = ship.getShipAI().getConfig();
    			if (config != null) {
    				if (effectLevel > 0.75f && aiStateLast != 1 && ship.getFluxLevel() < AI_FLUX_BACK &&
    					ship.getHullLevel() > AI_HULL_BACK &&
    					ship.getMutableStats().getShieldDamageTakenMult().computeMultMod() < 1.25f) {
    					/*
    					if (ship.getFluxLevel() < AI_FLUX_FORCE_ENGAGE)
    						config.backingOffWhileNotVentingAllowed = false;
    					else
    						config.backingOffWhileNotVentingAllowed = true; */
    					config.personalityOverride = Personalities.RECKLESS;
    					config.alwaysStrafeOffensively = true;
    					config.turnToFaceWithUndamagedArmor = false;
    					config.burnDriveIgnoreEnemies = true;
    					// if (aiStateLast != -1)
    					// 	ship.getShipAI().forceCircumstanceEvaluation();
    					aiStateLast = 1;
    				} else if (aiStateLast != 0 && (ship.getFluxLevel() >= AI_FLUX_BACK ||
    						ship.getHullLevel() <= AI_HULL_BACK ||
    						ship.getMutableStats().getShieldDamageTakenMult().computeMultMod() >= 1.25f)){
    					config.copyFrom(origConfig);
    					if (aiStateLast != -1)
    						ship.getShipAI().forceCircumstanceEvaluation();
    					aiStateLast = 0;
    				}

    	    		if (effectLevel > 0.75f && ship.getHullLevel() > AI_HULL_BACK) {
    					for (WeaponGroupAPI g : groups) {
    						if (!g.isAutofiring())
    							g.toggleOn();
    						if (ship.getSelectedGroupAPI() != null && ship.getSelectedGroupAPI().equals(g))
    							ship.resetSelectedGroup();
    					}
    	    		}
    			}
    		}
    	}
		
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getBallisticRoFMult().unmodify(id);
		stats.getBallisticWeaponDamageMult().unmodify(id);
		stats.getBallisticProjectileSpeedMult().unmodify(id);
		stats.getBeamWeaponDamageMult().unmodify(id);
		stats.getBeamWeaponFluxCostMult().unmodify(id);
		stats.getBallisticWeaponRangeBonus().unmodify(id);
		stats.getBeamWeaponRangeBonus().unmodify(id);
		if (aiStateLast < 1)
			return;
		else if (((ShipAPI) stats.getEntity()).getShipAI() != null) {
			((ShipAPI) stats.getEntity()).getShipAI().getConfig().copyFrom(origConfig);
			aiStateLast = 0;
			aiThinkTime = THINK_INTERVAL;
			((ShipAPI) stats.getEntity()).getShipAI().forceCircumstanceEvaluation();
		}
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 3)
			return new StatusData(String.format(MezzUtils.getString("espc_shipsystem", "ballistic_damage"),
				((int) (effectLevel * DAMAGE_BONUS * 100)) + "%"), false);
		else if (index == 2)
			return new StatusData(String.format(MezzUtils.getString("espc_shipsystem", "ballistic_speed"),
				((int) (effectLevel * VELOCITY_BONUS * 100)) + "%"), false);
		else if (index == 1)
			return new StatusData(String.format(MezzUtils.getString("espc_shipsystem", "ballistic_range"),
				((int) (effectLevel * RANGE_BONUS * 100)) + "%"), false);
		else if (index == 0)
			return new StatusData(String.format(MezzUtils.getString("espc_shipsystem", "ballistic_rof_malus"),
				((int) (effectLevel * ROF_BONUS * -100)) + "%"), true);
		
		return null;
	}
}
