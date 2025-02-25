package data.scripts.shipsystems;

import java.awt.Color;
import java.util.EnumSet;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
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
	
	private static final float BONUS_MAX = 2f;
	
	public float getBallisticDPS() {
		return ballisticDPS;
	}
	public float getEnergyDPS() {
		return energyDPS;
	}
	public boolean getMode() {
		return isEnergy;
	}
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (stats.getEntity() == null)
			return;
		
		if (!Global.getCurrentState().equals(GameState.COMBAT))
			return;

		CombatEngineAPI combatEngine = Global.getCombatEngine();
		float amount = combatEngine.getElapsedInLastFrame();
		if (amount == 0f)
			return;
		
		if (!initialized) {
			initialized = true;
			ship = (ShipAPI) stats.getEntity();
			for (WeaponAPI weapon : ship.getAllWeapons()) {
				if (weapon.getType() == WeaponType.BALLISTIC) {
					ballisticDPS += weapon.getDerivedStats().getSustainedDps() 
						* (weapon.getDamageType().equals(DamageType.FRAGMENTATION) ? 0.5f : 1f) *
						(weapon.hasAIHint(AIHints.PD) && !weapon.hasAIHint(AIHints.PD_ALSO) ? 0.5f : 1f);
					/*
					weapon.getSprite().setColor(new Color(
						0.8f, 0.8f, 0.8f));
					if (weapon.getBarrelSpriteAPI()!= null)
						weapon.getBarrelSpriteAPI().setColor(new Color(
							0.8f, 0.8f, 0.8f));
					if (weapon.getUnderSpriteAPI() != null)
						weapon.getUnderSpriteAPI().setColor(new Color(
							0.8f, 0.8f, 0.8f));
					*/
				}
				else if (weapon.getType() == WeaponType.ENERGY)
					energyDPS += weapon.getDerivedStats().getSustainedDps()
						* (weapon.getDamageType().equals(DamageType.FRAGMENTATION) ? 0.5f : 1f) *
						(weapon.hasAIHint(AIHints.PD) && !weapon.hasAIHint(AIHints.PD_ALSO) ? 0.5f : 1f);
				
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
			stats.getEnergyRoFMult().modifyFlat(id, (energyDPS > 0f ? Math.min(ballisticDPS/energyDPS, BONUS_MAX) * (1f - effectLevel) : 0f));
			// if (state == State.OUT && ballisticDPS > 0f)
			// 	stats.getBallisticRoFMult().modifyFlat(id, (ballisticDPS > 0f ? energyDPS/ballisticDPS * (effectLevel) : 0f));
			// else
			stats.getBallisticRoFMult().unmodify(id);
		}
		else {
			stats.getBallisticRoFMult().modifyFlat(id, (ballisticDPS > 0f ? Math.min(energyDPS/ballisticDPS, BONUS_MAX) * (1f - effectLevel) : 0f));
			// if (state == State.OUT && energyDPS > 0f)
			// 	stats.getEnergyRoFMult().modifyFlat(id, (energyDPS > 0f ? ballisticDPS/energyDPS * (effectLevel) : 0f));
			// else
			stats.getEnergyRoFMult().unmodify(id);
		}
		if (ship.getFluxTracker().isOverloaded()) {
	        ship.setWeaponGlow(0f,
		    	new Color(255, 120, 0, 155), 
				EnumSet.of(WeaponType.ENERGY));
	        ship.setWeaponGlow(0f, 
	        	new Color(255, 120, 0, 155), 
	        	EnumSet.of(WeaponType.BALLISTIC));
			return;
		} else if (ballisticDPS > 0f && energyDPS > 0f){
			if (isEnergy) {
		        ship.setWeaponGlow((1f - effectLevel) * (Math.min(ballisticDPS/energyDPS, BONUS_MAX)/BONUS_MAX * 1.5f),
			    	new Color(255, 120, 0, 155), 
					EnumSet.of(WeaponType.ENERGY));
		        ship.setWeaponGlow(effectLevel * (Math.min(energyDPS/ballisticDPS, BONUS_MAX)/BONUS_MAX * 1.5f), 
		        	new Color(255, 120, 0, 155), 
		        	EnumSet.of(WeaponType.BALLISTIC));
			} else {
			    ship.setWeaponGlow((1f - effectLevel) * (Math.min(energyDPS/ballisticDPS, BONUS_MAX)/BONUS_MAX * 1.5f), 
					new Color(255, 120, 0, 155), 
					EnumSet.of(WeaponType.BALLISTIC));
		        ship.setWeaponGlow(effectLevel * (Math.min(ballisticDPS/energyDPS, BONUS_MAX)/BONUS_MAX * 1.5f),
		        	new Color(255, 120, 0, 155), 
			        EnumSet.of(WeaponType.ENERGY));
			}
		}
		
		for (WeaponAPI weapon : ship.getAllWeapons()) {
			// float in = Math.min(1f, 1.9f - effectLevel);
			// float out = Math.max(0.8f, effectLevel);
			if (weapon.getType() == WeaponType.BALLISTIC && isEnergy ||
				weapon.getType() == WeaponType.ENERGY && !isEnergy
			) {
				weapon.stopFiring();
				if (weapon.getCooldown() > 0f)
					weapon.setRemainingCooldownTo(weapon.getCooldownRemaining());
				weapon.setForceNoFireOneFrame(true);
				/*
				if (effectLevel > 0.7f) {
					weapon.getSprite().setColor(new Color(
						out, out, out));
					if (weapon.getBarrelSpriteAPI()!= null)
						weapon.getBarrelSpriteAPI().setColor(new Color(
							out, out, out));
					if (weapon.getUnderSpriteAPI() != null)
						weapon.getUnderSpriteAPI().setColor(new Color(
							out, out, out));
				}
				else if (effectLevel > 0.7f) {
					weapon.getSprite().setColor(new Color(
						in, in, in));
					if (weapon.getBarrelSpriteAPI()!= null)
						weapon.getBarrelSpriteAPI().setColor(new Color(
							in, in, in));
					if (weapon.getUnderSpriteAPI() != null)
						weapon.getUnderSpriteAPI().setColor(new Color(
							in, in, in));
				}
				*/
			}
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
						(int) (Math.min(ballisticDPS/energyDPS, BONUS_MAX) * (1f - effectLevel) * 100f)
						 + "%",false);
				else
					return new StatusData("no energy weapons", true);
			} else {
				if (ballisticDPS > 0f)
					return new StatusData("ballistic rate of fire +" + 
							(int) (Math.min(energyDPS/ballisticDPS, BONUS_MAX) * (1f - effectLevel) * 100f)
							 + "%",false);
				else
					return new StatusData("no ballistic weapons", true);
			}
		}
		return null;
	}
}
