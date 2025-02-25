package data.scripts.campaign.econ;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class espc_AI_Population extends BaseMarketConditionPlugin implements MarketImmigrationModifier {
	
	public static int STABILITY_PENALTY = 1;
	
	public void apply(String id) {
		super.apply(id);
		
		market.getStability().modifyFlat(id, -STABILITY_PENALTY, "AI Sub-Population");
		market.addTransientImmigrationModifier(this);
	}

	public void unapply(String id) {
		super.unapply(id);
		market.getStability().unmodify(id);
		market.removeTransientImmigrationModifier(this);
	}

	public void modifyIncoming(MarketAPI market, PopulationComposition incoming) {
		
	}
	
	protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
		super.createTooltipAfterDescription(tooltip, expanded);
		
		tooltip.addPara("%s stability", 
				10f, Misc.getHighlightColor(),
				"-" + STABILITY_PENALTY);
	}
}