package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
// import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State;
// import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData;

public class espc_AlternatorStats extends BaseShipSystemScript {

	private ShipAPI ship;
	private boolean initialized = false;
	private float ballisticDPS = 0f;
	private float energyDPS = 0f;
	private boolean isEnergy = true;
	private boolean lastIdle = true;
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (stats.getEntity() == null)
			return;

		CombatEngineAPI combatEngine = Global.getCombatEngine();
		float amount = combatEngine.getElapsedInLastFrame();
		if (amount == 0f)
			return;
		
		if (!initialized) {
			initialized = true;
			ship = (ShipAPI) stats.getEntity();
			for (WeaponAPI weapon : ship.getAllWeapons()) {
				if (weapon.getType() == WeaponType.BALLISTIC)
					ballisticDPS += weapon.getDerivedStats().getSustainedDps() 
						* (weapon.getDamageType().equals(DamageType.FRAGMENTATION) ? 0.5f : 1f);
				else if (weapon.getType() == WeaponType.ENERGY)
					energyDPS += weapon.getDerivedStats().getSustainedDps()
						* (weapon.getDamageType().equals(DamageType.FRAGMENTATION) ? 0.5f : 1f);
				
			}
			// Global.getLogger(espc_AlternatorStats.class).info("balDPS: " + ballisticDPS);
			// Global.getLogger(espc_AlternatorStats.class).info("enDPS: " + energyDPS);
		}
		
		if (state == State.OUT && lastIdle) {
			lastIdle = false;
			isEnergy = !isEnergy;
		} else if (state == State.IDLE && !lastIdle)
			lastIdle = true;
		
		if (isEnergy) {
			stats.getEnergyRoFMult().modifyFlat(id, (energyDPS > 0f ? ballisticDPS/energyDPS * (1f - effectLevel) : 0f));
			// if (state == State.OUT && ballisticDPS > 0f)
			// 	stats.getBallisticRoFMult().modifyFlat(id, (ballisticDPS > 0f ? energyDPS/ballisticDPS * (effectLevel) : 0f));
			// else
				stats.getBallisticRoFMult().unmodify(id);
		}
		else {
			stats.getBallisticRoFMult().modifyFlat(id, (ballisticDPS > 0f ? energyDPS/ballisticDPS * (1f - effectLevel) : 0f));
			// if (state == State.OUT && energyDPS > 0f)
			// 	stats.getEnergyRoFMult().modifyFlat(id, (energyDPS > 0f ? ballisticDPS/energyDPS * (effectLevel) : 0f));
			// else
				stats.getEnergyRoFMult().unmodify(id);
		}
		
		for (WeaponAPI weapon : ship.getAllWeapons()) {
			if (weapon.getType() == WeaponType.BALLISTIC && isEnergy ||
				weapon.getType() == WeaponType.ENERGY && !isEnergy
			)
			weapon.setForceNoFireOneFrame(true);
		}

	}
	
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getBallisticRoFMult().unmodify(id);
		stats.getEnergyRoFMult().unmodify(id);
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		
		if (index == 0)  {
			if (isEnergy)
				return new StatusData("ballistic weapons disabled", true);
			else
				return new StatusData("energy weapons disabled", true);
		}
		else if (index == 1) {
			if (isEnergy) {
				if (energyDPS > 0f)
					return new StatusData("energy rate of fire +" + 
						(int) (ballisticDPS/energyDPS * (1f - effectLevel) * 100f)
						 + "%",false);
				else
					return new StatusData("no energy weapons", true);
			} else {
				if (ballisticDPS > 0f)
					return new StatusData("ballistic rate of fire +" + 
							(int) (energyDPS/ballisticDPS * (1f - effectLevel) * 100f)
							 + "%",false);
				else
					return new StatusData("no ballistic weapons", true);
			}
		}
		return null;
	}
}
