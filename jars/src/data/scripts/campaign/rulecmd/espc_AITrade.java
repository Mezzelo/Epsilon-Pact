package data.scripts.campaign.rulecmd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.CargoPickerListener;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.CustomRepImpact;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActionEnvelope;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;

/**
 * NotifyEvent $eventHandle <params> 
 * 
 */
public class espc_AITrade extends BaseCommandPlugin {
	
	private final float MIN_REP_EXCHANGE = 12f;
	
	private final float ALPHA_REP_VAL = 8f;
	private final float BETA_REP_VAL = 5f;
	private final float GAMMA_REP_VAL = 3f;
	private final float CONSTANT_REP_VAL = 4f;
	
	private boolean isConstant = false;
	// 0 = turn in, 1 = ships, 2 = weapons
	private int tradeType = 0;
	private float coreCredits = 0f;
	
	private final Map<String, Integer> hullMap;
	{
    	hullMap = new HashMap<String, Integer>();
    	hullMap.put("espc_flagbearer", 8);
    	hullMap.put("espc_songbird", 5);
    	hullMap.put("espc_jackalope", 3);
    	hullMap.put("espc_rondel", 3);
    	hullMap.put("espc_opossum", 3);
    	
    	hullMap.put("espc_militia", 3);
    	hullMap.put("espc_ember", 3);
    	
    	hullMap.put("espc_pilgrim", 8);
    	hullMap.put("espc_observer", 8);
    	hullMap.put("espc_chorale", 8);
    	
    	hullMap.put("espc_amanuensis", 12);
	}
	
	private final Map<String, Integer> weaponMap;
	{
    	weaponMap = new HashMap<String, Integer>();
    	weaponMap.put("espc_minimir", 3);
    	weaponMap.put("espc_minimirdual", 4);
    	weaponMap.put("espc_remdriver", 4);
    	weaponMap.put("espc_remmortar", 4);
    	weaponMap.put("espc_finnegan", 4);
    	weaponMap.put("espc_mkl", 6);
    	weaponMap.put("espc_gatling", 6);
    	weaponMap.put("espc_flak", 6);
    	
    	weaponMap.put("espc_riftspear", 8);
    	weaponMap.put("espc_riftpike", 12);
	}
	
	protected CampaignFleetAPI playerFleet;
	protected SectorEntityToken entity;
	protected FactionAPI playerFaction;
	protected FactionAPI entityFaction;
	protected TextPanelAPI text;
	protected OptionPanelAPI options;
	protected CargoAPI playerCargo;
	protected MemoryAPI memory;
	protected InteractionDialogAPI dialog;
	protected Map<String, MemoryAPI> memoryMap;
	protected PersonAPI person;
	protected FactionAPI faction;

    @Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		
		this.dialog = dialog;
		this.memoryMap = memoryMap;
		
		String command = params.get(0).getString(memoryMap);
		if (command == null) return false;
		
		memory = getEntityMemory(memoryMap);
		
		entity = dialog.getInteractionTarget();
		text = dialog.getTextPanel();
		options = dialog.getOptionPanel();
		
		playerFleet = Global.getSector().getPlayerFleet();
		playerCargo = playerFleet.getCargo();
		
		playerFaction = Global.getSector().getPlayerFaction();
		entityFaction = entity.getFaction();
		
		person = dialog.getInteractionTarget().getActivePerson();
		faction = person.getFaction();
		
		coreCredits = Global.getSector().getMemoryWithoutUpdate().getFloat("$espcBPCredit");
		if (command.equals("epsilpacSelectCoresShips")) {
			isConstant = false;
			tradeType = 
				(entityFaction.getRelationship(Factions.PLAYER) * 100f < MIN_REP_EXCHANGE ||
				coreCredits < MIN_REP_EXCHANGE) ? 
				0 : 1;
			selectCores();
		} else if (command.equals("epsilpacSelectCoresWeapons")) {
			isConstant = true;
			tradeType = 
				(entityFaction.getRelationship(Factions.PLAYER) * 100f < MIN_REP_EXCHANGE ||
				coreCredits < MIN_REP_EXCHANGE) ? 
				0 : 2;
			selectCores();
		} else if (command.equals("epsilpacSelectCoresBoth")) {
			isConstant = true;
			tradeType = 
				(entityFaction.getRelationship(Factions.PLAYER) * 100f < MIN_REP_EXCHANGE ||
				coreCredits < MIN_REP_EXCHANGE) ? 
				0 : 3;
			selectCores();
		}
		
		return true;
	}

	protected void selectCores() {
		CargoAPI copy = Global.getFactory().createCargo(false);
		//copy.addAll(cargo);
		//copy.setOrigSource(playerCargo);
		for (CargoStackAPI stack : playerCargo.getStacksCopy()) {
			CommoditySpecAPI spec = stack.getResourceIfResource();
			if (spec != null && 
				(spec.getId().equals(Commodities.ALPHA_CORE) ||
				spec.getId().equals(Commodities.BETA_CORE) ||
				spec.getId().equals(Commodities.GAMMA_CORE))) {
				copy.addFromStack(stack);
			}
		}
		
		if ((tradeType == 1 || tradeType == 3)) {
			for (String item : hullMap.keySet()) {
				if (!Global.getSector().getMemoryWithoutUpdate().contains(("$espcPurchasedBP_" + item)))
					copy.addSpecial(new SpecialItemData(Items.SHIP_BP, item), 1);
			}
		}
		if ((tradeType == 2 || tradeType == 3)) {
			for (String item : weaponMap.keySet()) {
				if (!Global.getSector().getMemoryWithoutUpdate().contains(("$espcPurchasedBP_" + item)))
					copy.addSpecial(new SpecialItemData(Items.WEAPON_BP, item), 1);
			}
		}
		copy.sort();
		
		final float width = 310f;
		dialog.showCargoPickerDialog(
			"Select AI cores to turn in",
			"Confirm",
			"Cancel",
			true,
			width,
			copy,
			new CargoPickerListener() {
			public void pickedCargo(CargoAPI cargo) {
				cargo.sort();
				
				float repChange = computeCoreReputationValue(cargo, isConstant);
				if (tradeType != 0)
					repChange -= computeBPWorth(cargo);
				
				if (repChange < 0f || (repChange == 0f && tradeType == 0))
					return;
					
				int alphaCores = 0;
				int betaCores = 0;
				int gammaCores = 0;
				for (CargoStackAPI stack : cargo.getStacksCopy()) {
					CommoditySpecAPI spec = stack.getResourceIfResource();
					if (spec != null && spec.getDemandClass().equals(Commodities.AI_CORES)) {
						if (spec.getId().equals(Commodities.ALPHA_CORE))
							alphaCores += stack.getSize();
						else if (spec.getId().equals(Commodities.BETA_CORE))
							betaCores += stack.getSize();
						else if (spec.getId().equals(Commodities.GAMMA_CORE))
							gammaCores += stack.getSize();
						playerCargo.removeItems(stack.getType(), stack.getData(), stack.getSize());
						if (stack.isCommodityStack()) { // should be always, but just in case
							AddRemoveCommodity.addCommodityLossText(stack.getCommodityId(), (int) stack.getSize(), text);
						}
					} else if (stack.isSpecialStack()) {
						Global.getSector().getMemoryWithoutUpdate().set(
							("$espcPurchasedBP_" + stack.getSpecialDataIfSpecial().getData()), true
						);
						playerCargo.addItems(stack.getType(), stack.getData(), stack.getSize());
						AddRemoveCommodity.addStackGainText(stack, text);
						if (hullMap.containsKey(stack.getSpecialDataIfSpecial().getData())) {
							FleetMemberAPI ship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, 
								stack.getSpecialDataIfSpecial().getData() + "_Hull");
							ship.getCrewComposition().setCrew(100000);
							ship.getRepairTracker().setCR(0.7f);
							Global.getSector().getPlayerFleet().getFleetData().addFleetMember(ship);
							text.setFontSmallInsignia();
							text.addParagraph(
								"Gained " + ship.getVariant().getFullDesignationWithHullNameForShip(), Misc.getPositiveHighlightColor());
							text.highlightInLastPara(
								Misc.getHighlightColor(), ship.getVariant().getFullDesignationWithHullNameForShip());
							text.setFontInsignia();
						} else if (weaponMap.containsKey(stack.getSpecialDataIfSpecial().getData())) {
							playerCargo.addItems(CargoItemType.WEAPONS, stack.getSpecialDataIfSpecial().getData(), 2);
							text.setFontSmallInsignia();
							text.addParagraph("Gained " + 2 + Strings.X + " " + 
								Global.getSettings().getWeaponSpec(stack.getSpecialDataIfSpecial().getData()
								).getWeaponName() + "", Misc.getPositiveHighlightColor());
							text.highlightInLastPara(Misc.getHighlightColor(), 2 + Strings.X);
							text.setFontInsignia();
						}
					}
				}
				// float bpValue = computeCoreBPValue(cargo, isConstant);
				// float bpWorth = computeBPWorth(cargo, isConstant);
				
				if (repChange >= 1f) {
					CustomRepImpact impact = new CustomRepImpact();
					impact.delta = repChange * 0.01f;
					impact.limit = RepLevel.WELCOMING;
					Global.getSector().adjustPlayerReputation(
						new RepActionEnvelope(RepActions.CUSTOM, impact,
							null, text, true), 
							faction.getId());
					
					impact.delta *= 0.25f;
					impact.limit = isConstant ? RepLevel.WELCOMING : RepLevel.FAVORABLE;
					if (impact.delta >= 0.01f) {
						Global.getSector().adjustPlayerReputation(
							new RepActionEnvelope(RepActions.CUSTOM, impact,
								null, text, true), 
									person);
					}
					

					Global.getSector().getMemoryWithoutUpdate().set(
						"$espcBPCredit", 
						Global.getSector().getMemoryWithoutUpdate().getFloat("$espcBPCredit")
						+ repChange);
					Global.getSector().getMemoryWithoutUpdate().set(
						"$espcAlphaCores", 
						Global.getSector().getMemoryWithoutUpdate().getInt("$espcAlphaCores")
						+ (int) alphaCores);
					Global.getSector().getMemoryWithoutUpdate().set(
						"$espcBetaCores", 
						Global.getSector().getMemoryWithoutUpdate().getInt("$espcBetaCores")
						+ (int) betaCores);
					Global.getSector().getMemoryWithoutUpdate().set(
						"$espcGammaCores", 
						Global.getSector().getMemoryWithoutUpdate().getInt("$espcGammaCores")
						+ (int) gammaCores);
				}
				
				FireBest.fire(null, dialog, memoryMap, "AICoresTurnedIn");
			}
			public void cancelledCargoSelection() {
			}
			public void recreateTextPanel(
				TooltipMakerAPI panel,
				CargoAPI cargo,
				CargoStackAPI pickedUp,
				boolean pickedUpFromSource,
				CargoAPI combined)
			{
			
				// float bpValue = computeCoreBPValue(combined);
				// float bpWorth = computeBPWorth(combined);
				float repChange = computeCoreReputationValue(combined, isConstant);
				int worth = 0;
				if (tradeType != 0)
					worth = computeBPWorth(combined);
				
				// float pad = 3f;
				// float small = 5f;
				float opad = 10f;

				panel.setParaFontOrbitron();
				panel.addPara(Misc.ucFirst(faction.getDisplayName()), faction.getBaseUIColor(), 1f);
				//panel.addTitle(Misc.ucFirst(faction.getDisplayName()), faction.getBaseUIColor());
				//panel.addPara(faction.getDisplayNameLong(), faction.getBaseUIColor(), opad);
				//panel.addPara(faction.getDisplayName() + " (" + entity.getMarket().getName() + ")", faction.getBaseUIColor(), opad);
				panel.setParaFontDefault();
				
				panel.addImage(faction.getLogo(), width * 1f, 3f);
				
				
				//panel.setParaFontColor(Misc.getGrayColor());
				//panel.setParaSmallInsignia();
				//panel.setParaInsigniaLarge();
				/*
				panel.addPara("Compared to dealing with other factions, turning AI cores in to " + 
						faction.getDisplayNameLongWithArticle() + " " +
						"will result in:", opad);
				panel.beginGridFlipped(width, 1, 40f, 10f);
				//panel.beginGrid(150f, 1);
				// panel.addToGrid(0, 0, "Bounty value", "" + (int)(valueMult * 100f) + "%");
				panel.addToGrid(0, 1, "Reputation gain", "" + (int)(repMult * 100f) + "%");
				panel.addGrid(pad);
				*/

				if (tradeType == 0) {
					panel.addPara("If you turn in the selected AI cores, your standing with "
						+ faction.getDisplayNameWithArticle() + " will improve by %s points.",
						opad * 1f, Misc.getHighlightColor(),
						"" + (int) repChange);
					if (coreCredits < 25) {
						panel.addPara("A reputation of %s is necessary to begin exchanging for blueprints, and you must have"
							+ " exchanged as much worth of cores.",
							opad * 1f, Misc.getHighlightColor(),
							"" + (int) MIN_REP_EXCHANGE);
						panel.addPara("Your reputation after this exchange will be %s, and you will have exchanged"
							+ " %s reputation worth of cores.",
							opad * 1f, Misc.getHighlightColor(),
							"" + (int) (entityFaction.getRelationship(Factions.PLAYER) * 100f + repChange),
							"" + (int) (coreCredits + repChange));
					} else {
						panel.addPara("A reputation of %s is necessary to begin exchaning for blueprints.",
								opad * 1f, Misc.getHighlightColor(),
								"" + (int) MIN_REP_EXCHANGE);
							panel.addPara("Your reputation after this exchange will be %s.",
								opad * 1f, Misc.getHighlightColor(),
								"" + (int) (entityFaction.getRelationship(Factions.PLAYER) * 100f + repChange));
					}
				} else {
					panel.addPara("The current trade will result in a reputation change with "
						+ faction.getDisplayNameWithArticle() + " of %s points.",
						opad * 1f, Misc.getHighlightColor(),
						"" + (int) (repChange - worth));
					panel.addPara("If trading for ship or weapon blueprints, you will also receive the respective ship or two copies of the weapon.",
							opad * 1f, Misc.getHighlightColor());
					panel.addPara("Your selected blueprints are worth %s points.",
						opad * 1f, Misc.getHighlightColor(),
						"" + (int) worth);
					panel.addPara("There must be a net exchange of zero or greater to complete this trade.",
						opad * 1f, Misc.getHighlightColor());
				}
				
				//panel.addPara("Bounty: %s", opad, Misc.getHighlightColor(), Misc.getWithDGS(bounty) + Strings.C);
				//panel.addPara("Reputation: %s", pad, Misc.getHighlightColor(), "+12");
			}
		});
	}
	
	protected float computeCoreReputationValue(CargoAPI cargo, boolean constant) {
		float rep = 0;
		for (CargoStackAPI stack : cargo.getStacksCopy()) {
			CommoditySpecAPI spec = stack.getResourceIfResource();
			if (spec == null)
				continue;
			if (!spec.getDemandClass().equals(Commodities.AI_CORES))
				continue;
			if (constant) {
				rep += CONSTANT_REP_VAL * stack.getSize();
			} else if (spec.getId().equals(Commodities.ALPHA_CORE)) {
				rep += ALPHA_REP_VAL * stack.getSize();
			} else if (spec.getId().equals(Commodities.BETA_CORE)) {
				rep += BETA_REP_VAL * stack.getSize();
			} else if (spec.getId().equals(Commodities.GAMMA_CORE)) {
				rep += GAMMA_REP_VAL * stack.getSize();
			}
		}
		//if (rep < 1f) rep = 1f;
		return rep;
	}

	protected int computeBPWorth(CargoAPI cargo) {
		int worth = 0;
		for (CargoStackAPI stack : cargo.getStacksCopy()) {
			CommoditySpecAPI spec = stack.getResourceIfResource();
			if (spec != null && spec.getDemandClass().equals(Commodities.AI_CORES))
				continue;
			if (!stack.isSpecialStack())
				continue;
			if ((tradeType == 1 || tradeType == 3) && hullMap.containsKey(stack.getSpecialDataIfSpecial().getData()))
				worth += hullMap.get(stack.getSpecialDataIfSpecial().getData());
			else if ((tradeType == 2 || tradeType == 3) && weaponMap.containsKey(stack.getSpecialDataIfSpecial().getData()))
				worth += weaponMap.get(stack.getSpecialDataIfSpecial().getData());
			
		}
		return worth;
	}
	
	protected boolean playerHasCores() {
		for (CargoStackAPI stack : playerCargo.getStacksCopy()) {
			CommoditySpecAPI spec = stack.getResourceIfResource();
			if (spec != null && spec.getDemandClass().equals(Commodities.AI_CORES)) {
				return true;
			}
		}
		return false;
	}

	
	
}















