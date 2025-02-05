package data.campaign.econ;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class espc_Enclaves extends BaseIndustry implements MarketImmigrationModifier {

	public static float DEFENSE_BONUS_MILITARY = 7f;
	public static int STABILITY_MOD = -6;
	
	public void apply() {
		super.apply(true);
		boolean hasSpeaker = market.getAdmin().getStats().getSkillLevel("espc_voice") > 0;
		market.getStability().modifyFlat(getModId(), STABILITY_MOD, getNameForModifier());

		demand(Commodities.DOMESTIC_GOODS, 4);
		demand(Commodities.MARINES, 3);
		demand(Commodities.SUPPLIES, 2);
		demand(Commodities.HAND_WEAPONS, 2);
		
		supply(Commodities.LUXURY_GOODS, 4);
		supply(Commodities.FOOD, 2);
		supply(Commodities.DRUGS, 2);
		supply(Commodities.ORGANS, 1);
		supply(Commodities.CREW, 1);

		if (hasSpeaker) {
			float bonus = DEFENSE_BONUS_MILITARY;
			float mult = getDeficitMult(Commodities.SUPPLIES);
			String extra = "";
			if (mult != 1) {
				String com = getMaxDeficit(Commodities.SUPPLIES).one;
				extra = " (" + getDeficitText(com).toLowerCase() + ")";
			}
			market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD)
				.modifyMult(getModId(), 1f + bonus/2f * mult + bonus/2f, getNameForModifier() + extra);
		}
		//int size = market.getSize();
		//demand(Commodities.HEAVY_MACHINERY, size);
	}

	
	@Override
	public void unapply() {
		super.unapply();
		market.getStability().unmodify(getModId());
		market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyMult(getModId());
	}
	
	public void modifyIncoming(MarketAPI market, PopulationComposition incoming) {
		float bonus = 5f;
		incoming.getWeight().modifyFlat(getModId(), bonus, getNameForModifier());
	}

	@Override
	protected boolean hasPostDemandSection(boolean hasDemand, IndustryTooltipMode mode) {
		return mode != IndustryTooltipMode.NORMAL || isFunctional();
	}
	
	@Override
	protected void addPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, IndustryTooltipMode mode) {
		//if (mode == IndustryTooltipMode.NORMAL && isFunctional()) {
		if (mode != IndustryTooltipMode.NORMAL || isFunctional()) {
			
			boolean hasSpeaker = market.getAdmin().getStats().getSkillLevel("espc_voice") > 0;
			tooltip.addPara("Population growth: %s", 10f, Misc.getHighlightColor(), "+5");
			tooltip.addPara("Stability penalty: %s", 10f, Misc.getNegativeHighlightColor(), "" + STABILITY_MOD);
			if (hasSpeaker)
				addGroundDefensesImpactSection(tooltip, DEFENSE_BONUS_MILITARY, 
					Commodities.SUPPLIES);
			
		}
	}
	
	/*
	
	@Override
	public boolean isAvailableToBuild() {
//		return false;
	}


	@Override
	public boolean showWhenUnavailable() {
		return false;
	}
	*/
	@Override
	public boolean canImprove() {
		return false;
	}
	/*
	
	public void addImproveDesc(TooltipMakerAPI info, ImprovementDescriptionMode mode) {
	}*/
	
}