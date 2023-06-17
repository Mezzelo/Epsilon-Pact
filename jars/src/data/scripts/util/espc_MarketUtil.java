package data.scripts.util;
// shorthand because i've no self control.

import java.util.ArrayList;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.campaign.PlanetAPI;
// import com.fs.starfarer.api.impl.campaign.ids.Conditions;
// import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.campaign.SectorEntityToken;

public class espc_MarketUtil {
	
	public static MarketAPI makeMarket(String id,
			SectorEntityToken token,
			int marketSize,
			String[] marketConditions,
			String[][] marketIndustries,
			String[] submarkets
		) {
		MarketAPI newMarket = Global.getFactory().createMarket(id, token.getName(), marketSize);
        newMarket.setFactionId(token.getFaction().getId());
		newMarket.setPrimaryEntity(token);
        newMarket.getTariff().modifyFlat("default_tariff", newMarket.getFaction().getTariffFraction());

		// Global.getLogger(espc_MarketFactory.class).info("gen market: " + newMarket.getId());
		
        for (String newCondition : marketConditions) {
            newMarket.addCondition(newCondition);
        }
        for (int i = 0; i < marketIndustries.length; i++) {
        	if (marketIndustries[i].length == 1)
        		newMarket.addIndustry(marketIndustries[i][0]);
        	else {
        		ArrayList<String> items = new ArrayList<String>();
        		for (int g = 1; g < marketIndustries[i].length; g++) {
        			items.add(marketIndustries[i][g]);
        		}
        		newMarket.addIndustry(marketIndustries[i][0], items);
        	}
        }
        for (String newSubmarket : submarkets) {
            newMarket.addSubmarket(newSubmarket);
        }
		
        for (MarketConditionAPI condition : newMarket.getConditions()) {
            condition.setSurveyed(true);
        }
        newMarket.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
        token.setMarket(newMarket);
        newMarket.reapplyIndustries();
        
		return newMarket;
	}
	
	public static MarketAPI makeEmptyMarket(PlanetAPI planet, String[] marketConditions) {
        Misc.initConditionMarket(planet);
		MarketAPI newMarket = planet.getMarket();
        newMarket.setFactionId(planet.getFaction().getId());
        
        for (String newCondition : marketConditions) {
            newMarket.addCondition(newCondition);
        }
        for (MarketConditionAPI condition : newMarket.getConditions()) {
            condition.setSurveyed(true);
        }
		return newMarket;
	}
			
			
	
}