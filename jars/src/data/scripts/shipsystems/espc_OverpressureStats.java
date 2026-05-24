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
	private static final float AI_FLUX_BACK = 0.6f;
	private static final float AI_FLUX_FORCE_ENGAGE = 0.4f;
	
	private boolean pluginInit = false;
	private ShipAIConfig origConfig = null;
	private boolean timidOrCautious = false;
	private List<WeaponGroupAPI> groups = null;
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (stats.getEntity() == null)
			return;
		
		if (!pluginInit) {
			pluginInit = true;
			Global.getCombatEngine().addPlugin(new espc_OverpressureVFX((ShipAPI) stats.getEntity()));
		}

		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (origConfig == null && ship.getShipAI() != null && ship.getShipAI().getConfig() != null) {
			if (ship.getShipAI().getConfig().personalityOverride != null &&
				(ship.getShipAI().getConfig().personalityOverride.equals(Personalities.TIMID) || 
				ship.getShipAI().getConfig().personalityOverride.equals(Personalities.CAUTIOUS)))
				timidOrCautious = true;
			else {
				ShipAIConfig config = ship.getShipAI().getConfig();
				origConfig = config.clone();
				groups = new ArrayList<WeaponGroupAPI>();
				for (WeaponGroupAPI g : ship.getWeaponGroupsCopy()) {
					boolean matching = true;
					for (WeaponAPI wep : g.getWeaponsCopy())
						if (matching && !wep.getType().equals(WeaponType.BALLISTIC))
							matching = false;
					
					if (matching)
						groups.add(g);
				}
			}
		}
		
		stats.getBallisticRoFMult().modifyMult(id, 1f + ROF_BONUS * effectLevel);
		stats.getBallisticWeaponDamageMult().modifyMult(id, 1f + DAMAGE_BONUS * effectLevel);
		stats.getBallisticProjectileSpeedMult().modifyMult(id, 1f + VELOCITY_BONUS * effectLevel);
		stats.getBallisticWeaponRangeBonus().modifyPercent(id, RANGE_BONUS * 100f);
		
		// as beams are contiguous, we can emulate the systems' effects on them with the ROF mult
		stats.getBeamWeaponDamageMult().modifyMult(id, 1f + ROF_BONUS * effectLevel);
		stats.getBeamWeaponFluxCostMult().modifyMult(id, 1f + ROF_BONUS * effectLevel);
		stats.getBeamWeaponRangeBonus().modifyPercent(id, RANGE_BONUS * 100f);

		if (timidOrCautious)
			return;
		
		if (origConfig != null &&
			((ShipAPI) stats.getEntity()).getShipAI() != null) {
			ShipAIConfig config = ship.getShipAI().getConfig();
			if (config != null) {
				if (effectLevel > 0.75f && ship.getFluxLevel() < AI_FLUX_BACK &&
					ship.getHullLevel() > AI_HULL_BACK) {
					config.personalityOverride = Personalities.RECKLESS;
					config.alwaysStrafeOffensively = true;
					if (ship.getFluxLevel() < AI_FLUX_FORCE_ENGAGE)
						config.backingOffWhileNotVentingAllowed = false;
					else
						config.backingOffWhileNotVentingAllowed = true;
					config.turnToFaceWithUndamagedArmor = false;
					config.burnDriveIgnoreEnemies = true;
					for (WeaponGroupAPI g : groups) {
						if (!g.isAutofiring())
							g.toggleOn();
						if (ship.getSelectedGroupAPI() != null && ship.getSelectedGroupAPI().equals(g))
							ship.resetSelectedGroup();
					}
				} else {
					config.copyFrom(origConfig);
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
		if (timidOrCautious)
			return;
		if (origConfig != null && ((ShipAPI) stats.getEntity()).getShipAI() != null) {
			ShipAIConfig config = ((ShipAPI) stats.getEntity()).getShipAI().getConfig();
			if (config != null) {
				config.copyFrom(origConfig);
			}
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
