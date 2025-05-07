package data.scripts.shipsystems;

import java.awt.Color;
import java.util.EnumSet;

// import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.WeaponSlotAPI;

public class espc_HarmonizerStats extends BaseShipSystemScript {

	private static final float[] ROF_BONUSES = {0.25f, 0.7f};
	private static final float[] FLUX_USE_BONUSES = {0.25f, 0.7f};
	
	// effectiveness of the system as a fraction of the original if the type of largest weapon is mixed.
	
	private int ballisticBoost = -1, energyBoost = -1;
	// while we're not designing ships with only smalls to use this system(basically glorified weapon boost with minimal build considerations),
	// there's still the annoying edge case of players omitting certain sizes themselves lol
	// 0 = small, 1 = med, 2 = large
	private int largestSize = -1;
	private int largestCount = 0;
	private int usableState = -1;
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		
		if (stats.getEntity() == null)
			return;
		
		if (largestSize == -1) {
			largestSize = 0;
			ShipAPI ship = (ShipAPI) stats.getEntity();
			
			for (WeaponSlotAPI mount : ship.getHullSpec().getAllWeaponSlotsCopy()) {
				/*
				largestSize = (mount.getSlotSize() == WeaponSize.MEDIUM) ? 1 : largestSize;
				largestSize = (mount.getSlotSize() == WeaponSize.LARGE) ? 2 : largestSize;
				if (largestSize == 2)
					break; */
				if (mount.getSlotSize() == WeaponSize.MEDIUM && largestSize < 2) {
					if (largestSize != 1)
						largestSize = 1;
					largestCount++;
				} else if (mount.getSlotSize() == WeaponSize.LARGE) {
					if (largestSize < 2) {
						largestSize = 2;
						largestCount = 0;
					}
					largestCount++;
				}
			}
			
			for (WeaponAPI weapon : ship.getAllWeapons()) {
				if (largestSize == 2 && weapon.getSize() == WeaponSize.LARGE ||
					largestSize == 1 && weapon.getSize() == WeaponSize.MEDIUM ||
					largestSize == 0) {
					if (weapon.getType() == WeaponType.BALLISTIC)
						energyBoost = energyBoost + (3 - largestCount);
					else if (weapon.getType() == WeaponType.ENERGY)
						ballisticBoost = ballisticBoost + (3 - largestCount);
				}
			}
		} else {
			if (ballisticBoost > -1) {
				stats.getBallisticRoFMult().modifyMult(id, 1f + ROF_BONUSES[ballisticBoost] * effectLevel);
				stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1f - FLUX_USE_BONUSES[ballisticBoost] * effectLevel);
			}
			if (energyBoost > -1) {
				stats.getEnergyRoFMult().modifyMult(id, 1f + ROF_BONUSES[energyBoost] * effectLevel);
				stats.getEnergyWeaponFluxCostMod().modifyMult(id, 1f - FLUX_USE_BONUSES[energyBoost] * effectLevel);
			}

	        ((ShipAPI) stats.getEntity()).setWeaponGlow(
	        	effectLevel * Math.min(((float) Math.max(ballisticBoost, energyBoost) + 1f) * 0.8f, 1.25f), 
	        	new Color(255, 120, 0, 155), 
	        	ballisticBoost > -1 && energyBoost > -1 ? 
	        		EnumSet.of(WeaponType.ENERGY, WeaponType.BALLISTIC) :
	        		EnumSet.of(energyBoost > -1 ? WeaponType.ENERGY : WeaponType.BALLISTIC));
		}
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		ballisticBoost = -1;
		energyBoost = -1;
		largestSize = -1;
		largestCount = 0;
		stats.getBallisticRoFMult().unmodify(id);
		stats.getBallisticWeaponFluxCostMod().unmodify(id);
		stats.getEnergyRoFMult().unmodify(id);
		stats.getEnergyWeaponFluxCostMod().unmodify(id);
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			if (ballisticBoost > -1)
		return new StatusData("ballistic rate of fire +" + (int) (ROF_BONUSES[ballisticBoost] * 100f * effectLevel) + "%", false);
			else if (energyBoost > -1)
				return new StatusData("energy rate of fire +" + (int) (ROF_BONUSES[energyBoost] * 100f * effectLevel) + "%", false);
		} else if (index == 1) {
			if (ballisticBoost > -1)
				return new StatusData("ballistic flux use -" + (int) (FLUX_USE_BONUSES[ballisticBoost] * 100f * effectLevel) + "%", false);
			else if (energyBoost > -1)
				return new StatusData("energy flux use -" + (int) (FLUX_USE_BONUSES[energyBoost] * 100f * effectLevel) + "%", false);
		} else if (index == 2 && ballisticBoost > -1 && energyBoost > -1)
			return new StatusData("energy rate of fire +" + (int) (ROF_BONUSES[energyBoost] * 100f * effectLevel) + "%", false);
		else if (index == 3 && ballisticBoost > -1 && energyBoost > -1)
			return new StatusData("energy flux use -" + (int) (FLUX_USE_BONUSES[energyBoost] * 100f * effectLevel) + "%", false);
		return null;
	}
	
	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (usableState == 0)
			return "INVALID LOADOUT";
		
		if (system.getState().equals(SystemState.IDLE))
			return "READY";
		else if (system.getState().equals(SystemState.ACTIVE) ||
			system.getState().equals(SystemState.IN))
			return "ACTIVE";
		else
			return "";
	}
	
	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		if (usableState == -1) {
			WeaponSize largestMount = WeaponSize.MEDIUM;
			for (WeaponSlotAPI mount : ship.getHullSpec().getAllWeaponSlotsCopy()) {
				if (mount.getSlotSize() == WeaponSize.LARGE) {
					largestMount = WeaponSize.LARGE;
					break;
				}
			}
			int largeBallisticCount = 0;
			int largeEnergyCount = 0;
			int ballisticCount = 0;
			int energyCount = 0;
			for (WeaponAPI wep : ship.getAllWeapons()) {
				if (wep.isPermanentlyDisabled() || wep.isDecorative())
					continue;
				if (wep.getType().equals(WeaponType.BALLISTIC)) {
					if (wep.getSize().equals(largestMount))
						largeBallisticCount++;
					else
						ballisticCount++;
				} else if (wep.getType().equals(WeaponType.ENERGY)) {
					if (wep.getSize().equals(largestMount))
						largeEnergyCount++;
					else
						energyCount++;
				}
				if (largeBallisticCount > 0 && energyCount > 0 ||
					largeEnergyCount > 0 && ballisticCount > 0) {
					usableState = 1;
					return true;
				}
				usableState = 0;
			}
		} else if (usableState == 1)
			return true;
		return false;
	}
}
