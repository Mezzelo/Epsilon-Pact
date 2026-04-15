package data.scripts.sic.espc;

import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;
import second_in_command.specs.SCCategorySpec;
import second_in_command.specs.SCOfficer;

public class espc_TravelLight extends SCBaseSkillPlugin {
	
	private static float MAX_SUPPLY_CAPACITY_START = 2500f;
	private static float MAX_SUPPLY_CAPACITY = 3500f;
	
	private static float MAX_FUEL_CAPACITY_START = 2000f;
	private static float MAX_FUEL_CAPACITY = 3000f;

	private static float SUPPLY_USE_BONUS = 40f;
	private static float FUEL_USE_BONUS = 40f;
	
	private class CapacityStats {
		public float cargo = 0f;
		public float fuel = 0f;
		public float cargoMult = 1f;
		public float fuelMult = 1f;
		public int burnMod = 0;
		private int capitals = 0;
		
		public CapacityStats(FleetDataAPI fleetData) {
			if (fleetData != null) {
				for (FleetMemberAPI curr : fleetData.getMembersListCopy()) {
					cargo += curr.getCargoCapacity();
					fuel += curr.getFuelCapacity();
					if (curr.getHullSpec().getHullSize().equals(HullSize.CAPITAL_SHIP))
						capitals++;
				}
				if (cargo > MAX_SUPPLY_CAPACITY)
					cargoMult = 0f;
				else if (cargo > MAX_SUPPLY_CAPACITY_START)
					cargoMult = 1f - (cargo - MAX_SUPPLY_CAPACITY_START)/(MAX_SUPPLY_CAPACITY - MAX_SUPPLY_CAPACITY_START);
				else
					burnMod += 2;
				if (fuel > MAX_FUEL_CAPACITY)
					fuelMult = 0f;
				else if (fuel > MAX_FUEL_CAPACITY_START)
					fuelMult = 1f - (fuel - MAX_FUEL_CAPACITY_START)/(MAX_FUEL_CAPACITY - MAX_FUEL_CAPACITY_START);
				else
					burnMod += 2;
			} else {
				cargoMult = 0f;
				fuelMult = 0f;
			}
			if (burnMod == 4)
				burnMod = 3;
			burnMod -= capitals;
		}
	}
	
	private boolean hasLogisticsOfficer(SCData data) {
    	for (SCOfficer scOfficer : data.getActiveOfficers())
    		for (SCCategorySpec cat : scOfficer.getAptitudeSpec().getCategories())
    			if (cat.getId().equals("sc_cat_logistical"))
    				return true;
		return false;
	}
	
	
    @Override
    public String getAffectsString() {
        return "fleet";
    }
    
    /*
    @Override
    public void applyEffectsAfterShipCreation(SCData data, ShipAPI ship, ShipVariantAPI variant, String id) {
    	if (ship.getFleetMember() == null || ship.getFleetMember().getFleetData() == null)
    		return;
    	
    	if (hasLogisticsOfficer(data)) {
    		ship.getMutableStats().getSuppliesPerMonth().unmodify(id);
    		ship.getMutableStats().getFuelUseMod().unmodify(id);
    		return;
    	}
    	
    	Float cargoMult = 
    		(Float) ship.getFleetMember().getFleetData().getCacheClearedOnSync().get("espc_sc_pact_travel_light_cargo");
    	if (cargoMult == null) {
    		CapacityStats cStats = new CapacityStats(ship.getFleetMember().getFleetData());
    		ship.getFleetMember().getFleetData().getCacheClearedOnSync().put(
    			"espc_sc_pact_travel_light_cargo", cStats.cargoMult);
    		ship.getFleetMember().getFleetData().getCacheClearedOnSync().put(
               	"espc_sc_pact_travel_light_fuel", cStats.fuelMult);
    		ship.getMutableStats().getSuppliesPerMonth().modifyPercent(id, -SUPPLY_USE_BONUS * cStats.cargoMult);
    		ship.getMutableStats().getFuelUseMod().modifyPercent(id, -FUEL_USE_BONUS * cStats.fuelMult);
    	} else {
    		ship.getMutableStats().getSuppliesPerMonth().modifyPercent(id, -SUPPLY_USE_BONUS * cargoMult);
    		ship.getMutableStats().getFuelUseMod().modifyPercent(id, -FUEL_USE_BONUS * 
            	(Float) ship.getMutableStats().getFleetMember().getFleetData().getCacheClearedOnSync().get("espc_sc_pact_travel_light_fuel"));
    	}
    } */

    
    @Override
    public void applyEffectsBeforeShipCreation(SCData data, MutableShipStatsAPI stats, ShipVariantAPI variant, ShipAPI.HullSize hullSize, String id) {
    	if (hasLogisticsOfficer(data))
    		return;
    	if (stats.getFleetMember() == null || stats.getFleetMember().getFleetData() == null)
    		return;
    	
    	Float cargoMult = 
    		(Float) stats.getFleetMember().getFleetData().getCacheClearedOnSync().get("espc_sc_pact_travel_light_cargo");
    	if (cargoMult == null) {
    		CapacityStats cStats = new CapacityStats(stats.getFleetMember().getFleetData());
    		stats.getFleetMember().getFleetData().getCacheClearedOnSync().put(
    			"espc_sc_pact_travel_light_cargo", cStats.cargoMult);
        	stats.getFleetMember().getFleetData().getCacheClearedOnSync().put(
           		"espc_sc_pact_travel_light_fuel", cStats.fuelMult);
    		stats.getSuppliesPerMonth().modifyPercent(id, -SUPPLY_USE_BONUS * cStats.cargoMult);
    		stats.getFuelUseMod().modifyPercent(id, -FUEL_USE_BONUS * cStats.fuelMult);
    	} else {
            stats.getSuppliesPerMonth().modifyPercent(id, -SUPPLY_USE_BONUS * 
                (Float) stats.getFleetMember().getFleetData().getCacheClearedOnSync().get("espc_sc_pact_travel_light_cargo"));
            stats.getFuelUseMod().modifyPercent(id, -FUEL_USE_BONUS * 
            	(Float) stats.getFleetMember().getFleetData().getCacheClearedOnSync().get("espc_sc_pact_travel_light_fuel"));
    	}
    }
    
    @Override
    public void advance(SCData data, Float amount) {
    	if (data.getFleet() == null || data.getFleet().getFleetData() == null)
    		return;
    	
    	if (hasLogisticsOfficer(data)) {
    		data.getFleet().getStats().getFleetwideMaxBurnMod().unmodify("espc_sc_pact_travel_light");
    		return;
    	}
    	
    	Integer burnMod = 
        	(Integer) data.getFleet().getFleetData().getCacheClearedOnSync().get("espc_sc_pact_travel_light_burn");
        if (burnMod == null) {
        	CapacityStats cStats = new CapacityStats(data.getFleet().getFleetData());
        	data.getFleet().getFleetData().getCacheClearedOnSync().put(
               	"espc_sc_pact_travel_light_burn", cStats.burnMod);
        	
        	data.getFleet().getStats().getFleetwideMaxBurnMod().modifyFlat(
        		"espc_sc_pact_travel_light", cStats.burnMod, "Travel Light");
        } else
        	data.getFleet().getStats().getFleetwideMaxBurnMod().modifyFlat(
        		"espc_sc_pact_travel_light", burnMod, "Travel Light");
    }
    
    @Override
    public void onActivation(SCData data) {
    	if (data.getFleet() == null || data.getFleet().getFleetData() == null)
    		return;
    	if (hasLogisticsOfficer(data))
    		return;

    	CapacityStats cStats = new CapacityStats(data.getFleet().getFleetData());
    	data.getFleet().getFleetData().getCacheClearedOnSync().put(
           	"espc_sc_pact_travel_light_burn", cStats.burnMod);
    	
    	data.getFleet().getStats().getFleetwideMaxBurnMod().modifyFlat(
    		"espc_sc_pact_travel_light", cStats.burnMod, "Travel Light");
    }
    
    @Override
    public void onDeactivation(SCData data) {
		data.getFleet().getStats().getFleetwideMaxBurnMod().unmodify("espc_sc_pact_travel_light");
    }

	@Override
	public void addTooltip(SCData data, TooltipMakerAPI tooltip) {
    	CapacityStats cStats;
    	
    	if (data.getFleet() != null && data.getFleet().getFleetData() != null) {
    		cStats = new CapacityStats(data.getFleet().getFleetData());
        	data.getFleet().getFleetData().getCacheClearedOnSync().put(
        		"espc_sc_pact_travel_light_cargo", cStats.cargoMult);
        	data.getFleet().getFleetData().getCacheClearedOnSync().put(
            	"espc_sc_pact_travel_light_cargo", cStats.fuelMult);
    	} else {
    		cStats = new CapacityStats(null);
    	}

    	if (cStats.cargo <= 0f) {
			tooltip.addPara("-%s monthly supply consumption for ship maintenance", 
				0f, Misc.getHighlightColor(), Misc.getHighlightColor(), 
				(int) (SUPPLY_USE_BONUS * cStats.cargoMult) + "%", (int) SUPPLY_USE_BONUS + "%");
			tooltip.addPara(BaseIntelPlugin.BULLET + 
               	"Maximum at %s or less base cargo capacity, diminishing fully at %s cargo capacity", 0f, 
               	Misc.getTextColor(), Misc.getHighlightColor(), 
    			(int) MAX_SUPPLY_CAPACITY_START + "", (int) MAX_SUPPLY_CAPACITY + "");
			tooltip.addSpacer(10f);
			tooltip.addPara("-%s fuel usage", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(), 
				(int) (FUEL_USE_BONUS * cStats.fuelMult) + "%", (int) FUEL_USE_BONUS + "%");
			tooltip.addPara(BaseIntelPlugin.BULLET + 
				"Maximum at %s or less base fuel capacity, diminishing fully at %s fuel capacity", 0f, 
				Misc.getTextColor(), Misc.getHighlightColor(), 
				(int) MAX_FUEL_CAPACITY_START + "", (int) MAX_FUEL_CAPACITY + "");
    	} else {
			tooltip.addPara("-%s monthly supply consumption for ship maintenance (maximum: %s)", 
				0f, Misc.getHighlightColor(), Misc.getHighlightColor(), 
				(int) (SUPPLY_USE_BONUS * cStats.cargoMult) + "%", (int) SUPPLY_USE_BONUS + "%");
			tooltip.addPara(BaseIntelPlugin.BULLET + 
	             "Maximum at %s or less base cargo capacity, diminishing fully at %s cargo capacity. Your fleet has %s base cargo capacity", 0f, 
	             Misc.getTextColor(), Misc.getHighlightColor(), 
	    		(int) MAX_SUPPLY_CAPACITY_START + "", (int) MAX_SUPPLY_CAPACITY + "", (int) cStats.cargo + "");
			tooltip.addPara(BaseIntelPlugin.BULLET + 
				" maximum burn if at %s or less base cargo capacity", 0f, 
				Misc.getTextColor(), Misc.getHighlightColor(), 
				"+1", "" + (int) MAX_SUPPLY_CAPACITY_START);
			tooltip.addSpacer(10f);
			tooltip.addPara("-%s fuel usage (maximum: %s)", 0f, Misc.getHighlightColor(), Misc.getHighlightColor(), 
				(int) (FUEL_USE_BONUS * cStats.fuelMult) + "%", (int) FUEL_USE_BONUS + "%");
			tooltip.addPara(BaseIntelPlugin.BULLET + 
				"Maximum at %s or less fuel capacity, diminishing fully at %s base fuel capacity. Your fleet has %s base fuel capacity", 0f, 
				Misc.getTextColor(), Misc.getHighlightColor(), 
				(int) MAX_FUEL_CAPACITY_START + "", (int) MAX_FUEL_CAPACITY + "", (int) cStats.fuel + "");
			tooltip.addPara(BaseIntelPlugin.BULLET + 
				" maximum burn if at %s or less base fuel capacity", 0f, 
				Misc.getTextColor(), Misc.getHighlightColor(), 
				"+1", "" + (int) MAX_FUEL_CAPACITY_START);
    	}
		tooltip.addSpacer(10f);
        tooltip.addPara("%s maximum burn level for each capital ship in fleet", 
                0f, Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor(), 
                "-1", (int) MAX_FUEL_CAPACITY_START + "%");
        
        tooltip.addSpacer(10f);
        tooltip.addPara("Skill has no effect if a Logistical executive officer is in use", 
                0f, Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor());
        
		
	}
}