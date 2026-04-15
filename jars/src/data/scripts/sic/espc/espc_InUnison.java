package data.scripts.sic.espc;

import java.util.HashMap;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;

public class espc_InUnison extends SCBaseSkillPlugin {
	
	private static float FRIGATE_PPT_BONUS = 180f;
	private static float FRIGATE_PPT_MAX = 420f;
	private static float FRIGATE_CR_BONUS = 30f;
	private static float FRIGATE_DAMAGE_BONUS = 20f;
	
	private static float DESTROYER_PPT_BONUS = 60f;
	private static float DESTROYER_PPT_MAX = 420f;
	private static float DESTROYER_CR_BONUS = 30f;
	private static float DESTROYER_DAMAGE_BONUS = 10f;
	
	private static float CAPITAL_PPT_MALUS = 120f;
	private static float CAPITAL_PPT_MIN = 480f;
	
	
	private boolean statHasMalus(StatBonus bonus) {
    	HashMap<String, StatMod> bonuses = bonus.getFlatBonuses();
    	for (String key : bonuses.keySet())
    		if (bonuses.get(key).value < 0f)
    			return true;
    	bonuses = bonus.getMultBonuses();
    	for (String key : bonuses.keySet())
    		if (bonuses.get(key).value < 0f)
    			return true;
    	bonuses = bonus.getPercentBonuses();
    	for (String key : bonuses.keySet())
    		if (bonuses.get(key).value < 0f)
    			return true;
		return false;
	}
	
	
    @Override
    public String getAffectsString() {
        return "all non-cruiser ships in the fleet";
    }

    @Override
    public void applyEffectsBeforeShipCreation(SCData data, MutableShipStatsAPI stats, ShipVariantAPI variant, ShipAPI.HullSize hullSize, String id) {
    	
    	if (data.hasAptitudeInFleet("sc_smallcraft") || stats.getFleetMember() == null || stats.getFleetMember().isPhaseShip() ||
    		hullSize.equals(HullSize.CRUISER))
    		return;
    	
    	if (hullSize.equals(HullSize.FRIGATE)) {
    		stats.getDamageToDestroyers().modifyPercent(id, FRIGATE_DAMAGE_BONUS);
    		stats.getDamageToCruisers().modifyPercent(id, FRIGATE_DAMAGE_BONUS);
    		stats.getDamageToCapital().modifyPercent(id, FRIGATE_DAMAGE_BONUS);
			stats.getCRPerDeploymentPercent().modifyPercent(id, -FRIGATE_CR_BONUS);
			stats.getPeakCRDuration().modifyFlat(id, Math.max(0f, Math.min(FRIGATE_PPT_BONUS, 
				FRIGATE_PPT_MAX - stats.getFleetMember().getHullSpec().getNoCRLossTime()
			)));
    	} else if (hullSize.equals(HullSize.DESTROYER)) {
    		stats.getDamageToCruisers().modifyPercent(id, DESTROYER_DAMAGE_BONUS);
    		stats.getDamageToCapital().modifyPercent(id, DESTROYER_DAMAGE_BONUS);
			stats.getCRPerDeploymentPercent().modifyPercent(id, -DESTROYER_CR_BONUS);
			stats.getPeakCRDuration().modifyFlat(id, Math.max(0f, Math.min(DESTROYER_PPT_BONUS, 
				DESTROYER_PPT_MAX - stats.getFleetMember().getHullSpec().getNoCRLossTime()
			)));
		} else if (hullSize.equals(HullSize.CAPITAL_SHIP)) {
			stats.getCRPerDeploymentPercent().modifyPercent(id, -CAPITAL_PPT_MALUS);
			stats.getPeakCRDuration().modifyFlat(id, Math.min(0f, Math.max(-CAPITAL_PPT_MALUS, 
				CAPITAL_PPT_MIN - stats.getFleetMember().getHullSpec().getNoCRLossTime()
			)));
		}
    }
    
    @Override
    public void applyEffectsAfterShipCreation(SCData data, ShipAPI ship, ShipVariantAPI variant, String id) {
    	if (ship.getFleetMember() == null || ship.getFleetMember().isPhaseShip() ||
    		ship.getHullSize().equals(HullSize.CRUISER))
    		return;

    	if (statHasMalus(ship.getMutableStats().getPeakCRDuration()) ||
    		statHasMalus(ship.getMutableStats().getCRLossPerSecondPercent())) {
    		
    		ship.getMutableStats().getCRPerDeploymentPercent().unmodify(id);
    		ship.getMutableStats().getPeakCRDuration().unmodify(id);
    		if (ship.getHullSize().equals(HullSize.CAPITAL_SHIP))
    			return;
    		ship.getMutableStats().getDamageToCruisers().unmodify(id);
    		ship.getMutableStats().getDamageToCapital().unmodify(id);
    		if (ship.getHullSize().equals(HullSize.FRIGATE))
        		ship.getMutableStats().getDamageToDestroyers().unmodify(id);
    	}
    }

	@Override
	public void addTooltip(SCData data, TooltipMakerAPI tooltip) {
        tooltip.addPara("-%s CR deployment cost for frigates and destroyers", 
            0f, Misc.getHighlightColor(), Misc.getHighlightColor(), 
            (int) FRIGATE_CR_BONUS + "%");
        tooltip.addSpacer(10f);
        tooltip.addPara("+%s increased damage against larger hullsizes for frigates", 
            0f, Misc.getHighlightColor(), Misc.getHighlightColor(), 
            (int) FRIGATE_DAMAGE_BONUS + "%");
        tooltip.addPara("+%s increased damage against larger hullsizes for destroyers", 
           	0f, Misc.getHighlightColor(), Misc.getHighlightColor(), 
            (int) DESTROYER_DAMAGE_BONUS + "%");
        tooltip.addSpacer(10f);
        tooltip.addPara("+%s seconds peak operating time if frigate, up to %s seconds", 
        	0f, Misc.getHighlightColor(), Misc.getHighlightColor(), 
        	(int) FRIGATE_PPT_BONUS + "", (int) FRIGATE_PPT_MAX + "");
        tooltip.addPara("+%s seconds peak operating time if destroyer, up to %s seconds", 
            0f, Misc.getHighlightColor(), Misc.getHighlightColor(), 
            (int) DESTROYER_PPT_BONUS + "", (int) DESTROYER_PPT_MAX + "");
        tooltip.addPara("-%s seconds peak operating time if capital, down to %s seconds", 
           	0f, Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor(), 
            (int) CAPITAL_PPT_MALUS + "", (int) CAPITAL_PPT_MIN + "");
        tooltip.addSpacer(10f);
        tooltip.addPara("Skill has no benefits on phase ships, ships with any PPT penalties, or if a Smallcraft executive officer is in use", 
        	0f, Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor());
		
	}
}