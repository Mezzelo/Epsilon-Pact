package data.scripts.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.plugins.impl.BaseAutofitPlugin;
import com.fs.starfarer.api.plugins.impl.CoreAutofitPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

public class espc_PactAutofitPlugin extends BaseAutofitPlugin {

	public static int PRIORITY = 1000;
	
	protected static Map<String, Category> reusableCategories = null; 
	
	public static class Category {
		public String base;
		public Set<String> tags = new HashSet<String>();
		
		public List<String> fallback = new ArrayList<String>();
		
		public Category(String base, Map<String, Category> categories) {
			this.base = base;
			
			categories.put(base, this);
			for (int i = 0; i < 100; i++) {
				String id = base + i;
				tags.add(id);
				categories.put(id, this);
			}
		}
		
		public void addFallback(String ... categories) {
			for (String catId : categories) {
				fallback.add(catId);
			}
		}
	}
	
	protected List<AutofitOption> options = new ArrayList<AutofitOption>();
	
	protected Map<String, Category> categories = new LinkedHashMap<String, Category>();
	
	protected Map<WeaponSpecAPI, List<String>> altWeaponCats = new LinkedHashMap<WeaponSpecAPI, List<String>>();
	protected Map<FighterWingSpecAPI, List<String>> altFighterCats = new LinkedHashMap<FighterWingSpecAPI, List<String>>();
	
	protected boolean debug = false;
	protected PersonAPI fleetCommander;
	protected MutableCharacterStatsAPI stats;
	
	protected Random random;
	
	protected long weaponFilterSeed = 0;
	protected String emptyWingTarget = null;
	
	public Random getRandom() {
		return random;
	}

	public void setRandom(Random random) {
		this.random = random;
	}

	public boolean isChecked(String id) {
		return false;
	}
	
	public void setChecked(String id, boolean checked) {
		return;
	}
	
	public espc_PactAutofitPlugin(PersonAPI fleetCommander) {
		this.fleetCommander = fleetCommander;
		if (fleetCommander != null) stats = fleetCommander.getStats();
		
		//reusableCategories = null;
		if (reusableCategories != null) {
			categories = reusableCategories;
		} else {
			new Category(CoreAutofitPlugin.KINETIC, categories).addFallback(
				CoreAutofitPlugin.KINETIC, CoreAutofitPlugin.ENERGY, CoreAutofitPlugin.HE,
				CoreAutofitPlugin.BEAM,  CoreAutofitPlugin.PD, CoreAutofitPlugin.ROCKET,
				CoreAutofitPlugin.MISSILE, CoreAutofitPlugin.UTILITY, CoreAutofitPlugin.STRIKE);
			new Category(CoreAutofitPlugin.HE, categories).addFallback(
				CoreAutofitPlugin.HE, CoreAutofitPlugin.ENERGY, CoreAutofitPlugin.KINETIC,
				CoreAutofitPlugin.BEAM, CoreAutofitPlugin.PD, CoreAutofitPlugin.ROCKET,
				CoreAutofitPlugin.MISSILE, CoreAutofitPlugin.UTILITY, CoreAutofitPlugin.STRIKE);
			new Category(CoreAutofitPlugin.ENERGY, categories).addFallback(
				CoreAutofitPlugin.ENERGY, CoreAutofitPlugin.KINETIC, CoreAutofitPlugin.HE,
				CoreAutofitPlugin.BEAM, CoreAutofitPlugin.PD, CoreAutofitPlugin.ROCKET,
				CoreAutofitPlugin.MISSILE, CoreAutofitPlugin.UTILITY, CoreAutofitPlugin.STRIKE);
			new Category(CoreAutofitPlugin.PD, categories).addFallback(
				CoreAutofitPlugin.PD, CoreAutofitPlugin.BEAM, CoreAutofitPlugin.HE,
				CoreAutofitPlugin.KINETIC, CoreAutofitPlugin.UTILITY, CoreAutofitPlugin.ROCKET,
				CoreAutofitPlugin.MISSILE, CoreAutofitPlugin.STRIKE);
			new Category(CoreAutofitPlugin.BEAM, categories).addFallback(
				CoreAutofitPlugin.BEAM, CoreAutofitPlugin.ENERGY, CoreAutofitPlugin.HE,
				CoreAutofitPlugin.KINETIC, CoreAutofitPlugin.ROCKET, CoreAutofitPlugin.MISSILE,
				CoreAutofitPlugin.UTILITY, CoreAutofitPlugin.STRIKE);
			
			new Category(CoreAutofitPlugin.STRIKE, categories).addFallback(
				CoreAutofitPlugin.STRIKE, CoreAutofitPlugin.MISSILE, CoreAutofitPlugin.ROCKET,
				CoreAutofitPlugin.HE, CoreAutofitPlugin.ENERGY, CoreAutofitPlugin.KINETIC, 
				CoreAutofitPlugin.UTILITY, CoreAutofitPlugin.BEAM, CoreAutofitPlugin.PD);
			new Category(CoreAutofitPlugin.MISSILE, categories).addFallback(
				CoreAutofitPlugin.MISSILE, CoreAutofitPlugin.STRIKE, CoreAutofitPlugin.ROCKET,
				CoreAutofitPlugin.HE, CoreAutofitPlugin.ENERGY, CoreAutofitPlugin.KINETIC, 
				CoreAutofitPlugin.UTILITY, CoreAutofitPlugin.BEAM, CoreAutofitPlugin.PD);
			new Category(CoreAutofitPlugin.UTILITY, categories).addFallback(
				CoreAutofitPlugin.UTILITY, CoreAutofitPlugin.MISSILE, CoreAutofitPlugin.ROCKET,
				CoreAutofitPlugin.STRIKE, CoreAutofitPlugin.HE, CoreAutofitPlugin.KINETIC, 
				CoreAutofitPlugin.ENERGY, CoreAutofitPlugin.BEAM, CoreAutofitPlugin.PD);
			new Category(CoreAutofitPlugin.ROCKET, categories).addFallback(
				CoreAutofitPlugin.ROCKET, CoreAutofitPlugin.UTILITY, CoreAutofitPlugin.MISSILE,
				CoreAutofitPlugin.STRIKE, CoreAutofitPlugin.HE, CoreAutofitPlugin.ENERGY,
				CoreAutofitPlugin.KINETIC, CoreAutofitPlugin.BEAM, CoreAutofitPlugin.PD);
			
			new Category(CoreAutofitPlugin.INTERCEPTOR, categories).addFallback(
				CoreAutofitPlugin.INTERCEPTOR, CoreAutofitPlugin.FIGHTER, CoreAutofitPlugin.SUPPORT, CoreAutofitPlugin.BOMBER);
			new Category(CoreAutofitPlugin.BOMBER, categories).addFallback(
				CoreAutofitPlugin.BOMBER, CoreAutofitPlugin.FIGHTER, CoreAutofitPlugin.INTERCEPTOR, CoreAutofitPlugin.SUPPORT);
			new Category(CoreAutofitPlugin.FIGHTER, categories).addFallback(
				CoreAutofitPlugin.FIGHTER, CoreAutofitPlugin.INTERCEPTOR, CoreAutofitPlugin.BOMBER, CoreAutofitPlugin.SUPPORT);
			new Category(CoreAutofitPlugin.SUPPORT, categories).addFallback(
				CoreAutofitPlugin.SUPPORT, CoreAutofitPlugin.INTERCEPTOR, CoreAutofitPlugin.FIGHTER, CoreAutofitPlugin.BOMBER);
			
			reusableCategories = categories;
		}
	}
	
	
	protected void stripWeapons(ShipVariantAPI current, AutofitPluginDelegate delegate) {
		for (String id : current.getFittedWeaponSlots()) {
			WeaponSlotAPI slot = current.getSlot(id);
			if (slot.isDecorative() || slot.isBuiltIn() || slot.isHidden() ||
				slot.isSystemSlot() || slot.isStationModule()) continue;
			clearWeaponSlot(slot, delegate, current);
		}
	}
	
	protected void stripFighters(ShipVariantAPI current, AutofitPluginDelegate delegate) {
		int numBays = 20; // well above whatever it might actually be
		for (int i = 0; i < numBays; i++) {
			if (current.getWingId(i) != null) {
				clearFighterSlot(i, delegate, current);
			}
		}
	}
	
	protected Map<String, AvailableWeapon> fittedWeapons = new HashMap<String, AvailableWeapon>();
	protected Map<String, AvailableFighter> fittedFighters = new HashMap<String, AvailableFighter>();
	
	protected Set<String> availableMods;
	protected Set<String> slotsToSkip = new HashSet<String>();
	protected Set<Integer> baysToSkip = new HashSet<Integer>();
	protected boolean fittingModule = false;
	protected int missilesWithAmmoOnCurrent = 0;
	public void doFit(ShipVariantAPI current, ShipVariantAPI target, int maxSMods, AutofitPluginDelegate delegate) {
		if (!fittingModule) {
			fittedWeapons.clear();
			fittedFighters.clear();
			
			availableMods = new LinkedHashSet<String>(delegate.getAvailableHullmods());
		}
		
		current.setMayAutoAssignWeapons(false);
		current.getStationModules().putAll(target.getStationModules());

		int index = 0;
		for (String slotId : current.getStationModules().keySet()) {
			ShipVariantAPI moduleCurrent = current.getModuleVariant(slotId);
			boolean forceClone = false;
			if (moduleCurrent == null) {
				// when the target variant is not stock and has custom variants for the modules, grab them
				forceClone = true;
				moduleCurrent = target.getModuleVariant(slotId);
				//continue;
			}
			if (moduleCurrent == null) {
				String variantId = current.getHullVariantId();
				throw new RuntimeException("Module variant for slotId [" + slotId + "] not found for " +
										  "variantId [" + variantId + "] of hull [" + current.getHullSpec().getHullId() + "]");
				//continue;
			}
			if (moduleCurrent.isStockVariant() || forceClone) {
				moduleCurrent = moduleCurrent.clone();
				moduleCurrent.setSource(VariantSource.REFIT);
				if (!forceClone) {
					moduleCurrent.setHullVariantId(moduleCurrent.getHullVariantId() + "_" + index);
				}
			}
			index++;
			
//				String variantId = current.getStationModules().get(slotId);
//				ShipVariantAPI moduleTarget = Global.getSettings().getVariant(variantId);
			ShipVariantAPI moduleTarget = target.getModuleVariant(slotId);
			if (moduleTarget == null) continue;
			
			fittingModule = true;
			doFit(moduleCurrent, moduleTarget, 0, delegate);
			fittingModule = false;
			
			current.setModuleVariant(slotId, moduleCurrent);
		}
		current.setSource(VariantSource.REFIT);
		
		weaponFilterSeed = random.nextLong();
		
		emptyWingTarget = null;
		if (delegate.getAvailableFighters().size() > 0) {
			emptyWingTarget = delegate.getAvailableFighters().get(random.nextInt(delegate.getAvailableFighters().size())).getId();
		}
		
		altWeaponCats.clear();
		altFighterCats.clear();
		
		slotsToSkip.clear();
		baysToSkip.clear();
		
		missilesWithAmmoOnCurrent = 0;

		slotsToSkip.addAll(current.getFittedWeaponSlots()); 
		for (int i = 0; i < 20; i++) {
			String wingId = current.getWingId(i);
			if (wingId != null && !wingId.isEmpty()) {
				baysToSkip.add(i);
			}
		}
		
		// all the spare OP is gonna come from smods, so add them first if there's gonna be any
		if (maxSMods > 0)
			convertToSMods(current, maxSMods);
		
		// in case we're anomalously fitting a station or a stock variant, go ahead and retread the old steps
		fitFighters(current, target, false, delegate);
		fitWeapons(current, target, false, delegate);

		if (current.hasHullMod(HullMods.FRAGILE_SUBSYSTEMS) && 
			(current.getHullSize() == HullSize.FRIGATE || current.getHullSize() == HullSize.DESTROYER)) {
			addHullmods(current, delegate, HullMods.HARDENED_SUBSYSTEMS);
		}
		
		float ventsCapsFraction = 1f;
		
		addVentsAndCaps(current, target, ventsCapsFraction);

		// now that we're at the target level of vents and caps
		// see if we can upgrade some weapons
		fitFighters(current, target, true, delegate);
		fitWeapons(current, target, true, delegate);
		addVentsAndCaps(current, target, 1f - ventsCapsFraction);
		
//		float dissipation = current.getHullSpec().getFluxDissipation() + current.getNumFluxVents() * 10f;
//		float generation = 0f;
//		for (String slotId : current.getFittedWeaponSlots()) {
//			WeaponSpecAPI spec = current.getWeaponSpec(slotId);
//			generation += spec.getDerivedStats().getSustainedFluxPerSecond();
//		}
		
		addExtraVentsAndCaps(current, target);
		addHullmods(current, delegate, HullMods.REINFORCEDHULL, HullMods.BLAST_DOORS, HullMods.HARDENED_SUBSYSTEMS);
		addModsWithSpareOPIfAny(current, target, false, delegate);
		
		if (maxSMods > 0) {
			addExtraVents(current);
			addExtraCaps(current);
			//addHullmods(current, delegate, HullMods.FLUX_DISTRIBUTOR, HullMods.FLUX_COIL);
			if (!current.hasHullMod(HullMods.FLUX_DISTRIBUTOR)) {
				addDistributor(current, delegate);
			}
			if (!current.hasHullMod(HullMods.FLUX_COIL)) {
				addCoil(current, delegate);
			}
			//addModsWithSpareOPIfAny(current, target, true, delegate);
			//addHullmods(current, delegate, HullMods.FLUX_DISTRIBUTOR, HullMods.FLUX_COIL);
			if (current.getHullSize() == HullSize.FRIGATE || current.hasHullMod(HullMods.SAFETYOVERRIDES)) {
				addHullmods(current, delegate, HullMods.HARDENED_SUBSYSTEMS);
			}
			int remaining = maxSMods - current.getSMods().size();
			if (remaining > 0) {
				List<String> mods = new ArrayList<String>();
				mods.add(HullMods.FLUX_DISTRIBUTOR);
				mods.add(HullMods.FLUX_COIL);
				if (current.getHullSize() == HullSize.FRIGATE || current.hasHullMod(HullMods.SAFETYOVERRIDES)) {
					mods.add(HullMods.HARDENED_SUBSYSTEMS);
					mods.add(HullMods.REINFORCEDHULL);
				} else {
					mods.add(HullMods.REINFORCEDHULL);
					mods.add(HullMods.HARDENED_SUBSYSTEMS);
				}
				mods.add(HullMods.BLAST_DOORS);
				Iterator<String> iter = mods.iterator();
				while (iter.hasNext()) {
					String modId = iter.next();
					if (current.getPermaMods().contains(modId)) {
						iter.remove();
					}
				}
				for (int i = 0; i < remaining && !mods.isEmpty(); i++) {
					current.setNumFluxCapacitors(0);
					current.setNumFluxVents(0);
					String modId = mods.get(Math.min(i, mods.size() - 1));
					addHullmods(current, delegate, modId);
					convertToSMods(current, 1);
//					addExtraVents(current);
//					addExtraCaps(current);
				}
			}
		}
		
		if (current.getHullSpec().isPhase()) {
			addExtraCaps(current);
		} else {
			addExtraVents(current);
		}
		
		addHullmods(current, delegate, HullMods.ARMOREDWEAPONS);
		
		if (current.getHullSpec().isPhase()) {
			addExtraVents(current);
		} else {
			addExtraCaps(current);
		}
		
		current.setVariantDisplayName(target.getDisplayName());
		
		// just removing empty weapon groups ig?
		current.getWeaponGroups().clear();
		for (WeaponGroupSpec group : target.getWeaponGroups()) {
			WeaponGroupSpec copy = new WeaponGroupSpec(group.getType());
			copy.setAutofireOnByDefault(group.isAutofireOnByDefault());
			for (String slotId : group.getSlots()) {
				if (current.getWeaponId(slotId) != null) {
					copy.addSlot(slotId);
				}
			}
			if (!copy.getSlots().isEmpty()) {
				current.addWeaponGroup(copy);
			}
		}
		
		if (!fittingModule) {
			delegate.syncUIWithVariant(current);
		}
	}
	
	protected int convertToSMods(ShipVariantAPI current, int num) {
		if (num <= 0) return 0;
		
		List<HullModSpecAPI> mods = new ArrayList<HullModSpecAPI>(); 
		for (String id : current.getHullMods()) {
			if (current.getPermaMods().contains(id)) continue;
			if (current.getHullSpec().getBuiltInMods().contains(id)) continue;
			HullModSpecAPI mod = DModManager.getMod(id);
			if (mod.hasTag(Tags.HULLMOD_NO_BUILD_IN)) continue;
			mods.add(mod);
		}
		
		final HullSize size = current.getHullSize();
		Collections.sort(mods, new Comparator<HullModSpecAPI>() {
			public int compare(HullModSpecAPI o1, HullModSpecAPI o2) {
				return Misc.getOPCost(o2, size) - Misc.getOPCost(o1, size);
			}
		});
		
		int count = 0;
		for (int i = 0; i < num && i < mods.size(); i++) {
			String id = mods.get(i).getId();
			current.addPermaMod(id, true);
			count++;
		}
		return count;
	}
	
	protected void addModsWithSpareOPIfAny(ShipVariantAPI current, ShipVariantAPI target, boolean sModMode, AutofitPluginDelegate delegate) {
		int opCost = current.computeOPCost(stats);
		int opMax = current.getHullSpec().getOrdnancePoints(stats);
		int opLeft = opMax - opCost;
	
		if (opLeft <= 0) return;
		
		float total = target.getNumFluxVents() + target.getNumFluxCapacitors();
		float ventsFraction = 1f;
		if (total > 0) {
			ventsFraction = target.getNumFluxVents() / total;
		}
		
		if (sModMode) {
			if (ventsFraction >= 0.5f) {
				addDistributorRemoveVentsIfNeeded(current, delegate);
				addCoilRemoveCapsIfNeeded(current, delegate);
			} else {
				addCoil(current, delegate);
				addCoilRemoveCapsIfNeeded(current, delegate);
			}
		} else {
			if (ventsFraction >= 0.5f) {
				addDistributor(current, delegate);
				addCoil(current, delegate);
			} else {
				addCoil(current, delegate);
				addDistributor(current, delegate);
			}
		}
	}
	
	protected void addCoil(ShipVariantAPI current, AutofitPluginDelegate delegate) {
		int opCost = current.computeOPCost(stats);
		int opMax = current.getHullSpec().getOrdnancePoints(stats);
		int opLeft = opMax - opCost;
	
		if (opLeft <= 0) return;
		
		int vents = current.getNumFluxVents();
		
		HullModSpecAPI coil = Misc.getMod(HullMods.FLUX_COIL);
		int cost = coil.getCostFor(current.getHullSize());
		
		if (cost < opLeft + vents * 0.3f) {
			int remove = cost - opLeft;
			if (remove > 0) {
				opLeft -= addVents(-remove, current, 1000);
			}
			opLeft -= addModIfPossible(HullMods.FLUX_COIL, delegate, current, opLeft);
		}
	}
	
	protected void addCoilRemoveCapsIfNeeded(ShipVariantAPI current, AutofitPluginDelegate delegate) {
		int opCost = current.computeOPCost(stats);
		int opMax = current.getHullSpec().getOrdnancePoints(stats);
		int opLeft = opMax - opCost;
		
		if (opLeft <= 0) return;
		
		int caps = current.getNumFluxCapacitors();
		
		HullModSpecAPI coil = Misc.getMod(HullMods.FLUX_COIL);
		int cost = coil.getCostFor(current.getHullSize());
		
		if (cost < opLeft + caps * 0.3f) {
			int remove = cost - opLeft;
			if (remove > 0) {
				opLeft -= addCapacitors(-remove, current, 1000);
			}
			opLeft -= addModIfPossible(HullMods.FLUX_COIL, delegate, current, opLeft);
		}
	}
	
	protected void addDistributor(ShipVariantAPI current, AutofitPluginDelegate delegate) {
		int opCost = current.computeOPCost(stats);
		int opMax = current.getHullSpec().getOrdnancePoints(stats);
		int opLeft = opMax - opCost;
		
		if (opLeft <= 0) return;
		
		int caps = current.getNumFluxCapacitors();
		
		HullModSpecAPI distributor = Misc.getMod(HullMods.FLUX_DISTRIBUTOR);
		int cost = distributor.getCostFor(current.getHullSize());
		
		if (cost <= opLeft + caps * 0.3f) {
			int remove = cost - opLeft;
			if (remove > 0) {
				opLeft -= addCapacitors(-remove, current, 1000);
			}
			opLeft -= addModIfPossible(HullMods.FLUX_DISTRIBUTOR, delegate, current, opLeft);
		}
	}
	
	protected void addDistributorRemoveVentsIfNeeded(ShipVariantAPI current, AutofitPluginDelegate delegate) {
		int opCost = current.computeOPCost(stats);
		int opMax = current.getHullSpec().getOrdnancePoints(stats);
		int opLeft = opMax - opCost;
		
		if (opLeft <= 0) return;
		
		int vents = current.getNumFluxVents();
		
		HullModSpecAPI distributor = Misc.getMod(HullMods.FLUX_DISTRIBUTOR);
		int cost = distributor.getCostFor(current.getHullSize());
		
		if (cost <= opLeft + vents * 0.3f) {
			int remove = cost - opLeft;
			if (remove > 0) {
				opLeft -= addVents(-remove, current, 1000);
			}
			opLeft -= addModIfPossible(HullMods.FLUX_DISTRIBUTOR, delegate, current, opLeft);
		}
	}
	
	protected List<AvailableWeapon> getWeapons(AutofitPluginDelegate delegate) {
		return delegate.getAvailableWeapons();
	}
	
	protected List<AvailableFighter> getFighters(AutofitPluginDelegate delegate) {
		boolean automated = Misc.isAutomated(delegate.getShip());
		List<AvailableFighter> fighters = new ArrayList<AvailableFighter>(delegate.getAvailableFighters());
		Iterator<AvailableFighter> iter = fighters.iterator();
		while (iter.hasNext()) {
			AvailableFighter f = iter.next();
			if (automated && !f.getWingSpec().hasTag(Tags.AUTOMATED_FIGHTER))
				iter.remove();
		}
		return fighters;
	}
	
	public int addHullmods(ShipVariantAPI current, AutofitPluginDelegate delegate, String ... mods) {
		if (fittingModule) return 0;
		
		int opCost = current.computeOPCost(stats);
		int opMax = current.getHullSpec().getOrdnancePoints(stats);
		int opLeft = opMax - opCost;
		
		int addedTotal = 0;
		for (String mod : mods) {
			if (current.hasHullMod(mod)) continue;
//			if (mod.equals(HullMods.INTEGRATED_TARGETING_UNIT)) {
//				System.out.println("wefwefwefe");
//			}
			if (!availableMods.contains(mod)) {
				if (mod.equals(HullMods.INTEGRATED_TARGETING_UNIT) && 
						current.getHullSize().ordinal() >= HullSize.CRUISER.ordinal()) {
					mod = HullMods.DEDICATED_TARGETING_CORE;
				} else {
					continue;
				}
			}
			
			if (mod.equals(HullMods.DEDICATED_TARGETING_CORE) && 
					availableMods.contains(HullMods.INTEGRATED_TARGETING_UNIT)) {
				mod = HullMods.INTEGRATED_TARGETING_UNIT;
			}
			
			HullModSpecAPI modSpec = Misc.getMod(mod);
			
			if (mod.equals(HullMods.INTEGRATED_TARGETING_UNIT) && 
					current.hasHullMod(HullMods.DEDICATED_TARGETING_CORE)) {
				current.removeMod(HullMods.DEDICATED_TARGETING_CORE);
				HullModSpecAPI dtc = Misc.getMod(HullMods.DEDICATED_TARGETING_CORE);
				int cost = dtc.getCostFor(current.getHullSize());;
				addedTotal -= cost;
				opLeft += cost;
			}
			
			
			if (current.hasHullMod(HullMods.ADVANCED_TARGETING_CORE) || current.hasHullMod(HullMods.DISTRIBUTED_FIRE_CONTROL)) {
				if (mod.equals(HullMods.INTEGRATED_TARGETING_UNIT)) {
					continue;
				}
				if (mod.equals(HullMods.DEDICATED_TARGETING_CORE)) {
					continue;
				}
			}
			
			if (current.getHullSpec().isPhase()) {
				if (modSpec.hasTag(HullMods.TAG_NON_PHASE)) {
					continue;
				}
			}
			if (!current.getHullSpec().isPhase()) {
				if (modSpec.hasTag(HullMods.TAG_PHASE)) {
					continue;
				}
			}
			
			int cost = addModIfPossible(modSpec, delegate, current, opLeft);;
			//int cost = addModIfPossible(mod, delegate, current, opLeft);
			
			opLeft -= cost;
			addedTotal += cost;
		}
		return addedTotal;
	}
	
	public int addModIfPossible(String id, AutofitPluginDelegate delegate, ShipVariantAPI current, int opLeft) {
		if (current.hasHullMod(id)) return 0;
		if (delegate.isPlayerCampaignRefit() && !delegate.canAddRemoveHullmodInPlayerCampaignRefit(id)) return 0;
		
		HullModSpecAPI mod = Misc.getMod(id);
		return addModIfPossible(mod, delegate, current, opLeft);
	}
	
	public int addModIfPossible(HullModSpecAPI mod, AutofitPluginDelegate delegate, ShipVariantAPI current, int opLeft) {
		if (mod == null) return 0;
		
		if (current.hasHullMod(mod.getId())) return 0;
		if (delegate.isPlayerCampaignRefit() && !delegate.canAddRemoveHullmodInPlayerCampaignRefit(mod.getId())) return 0;
		
		
		int cost = mod.getCostFor(current.getHullSize());
		if (cost > opLeft) return 0;

		ShipAPI ship = delegate.getShip();
		ShipVariantAPI orig = null;
		// why is this commented out? It fixes an issue with logistics hullmods not being properly applied
		// if the current variant already has some
		// but probably? causes some other issues
		// possibly: it was not setting the orig variant back when returning 0; this is now fixed
		if (ship != null) {
			orig = ship.getVariant();
			ship.setVariantForHullmodCheckOnly(current);
		}
		if (ship != null && mod.getEffect() != null && ship.getVariant() != null && !mod.getEffect().isApplicableToShip(ship)
				&& !ship.getVariant().hasHullMod(mod.getId())) {
			if (orig != null) {
				ship.setVariantForHullmodCheckOnly(orig);
			}
			return 0;
		}
		
		if (orig != null && ship != null) {
			ship.setVariantForHullmodCheckOnly(orig);
		}
		
		current.addMod(mod.getId());
		return cost;
	}
	
	
	
	public void addVentsAndCaps(ShipVariantAPI current, ShipVariantAPI target, float fraction) {
		if (fraction < 0) return;
		
		int opCost = current.computeOPCost(stats);
		int opMax = current.getHullSpec().getOrdnancePoints(stats);
		int opLeft = opMax - opCost;
		
		int maxVents = getMaxVents(current.getHullSize());
		int maxCapacitors = getMaxCaps(current.getHullSize());
		
		int add = Math.max((int)Math.ceil(target.getNumFluxVents() * fraction) - current.getNumFluxVents(), 0);
		if (add > opLeft) add = opLeft;
		opLeft -= addVents(add, current, maxVents);
		
		add = Math.max((int)Math.ceil(target.getNumFluxCapacitors() * fraction) - current.getNumFluxCapacitors(), 0);
		if (add > opLeft) add = opLeft;
		opLeft -= addCapacitors(add, current, maxCapacitors);
	}
	
	public void addExtraVents(ShipVariantAPI current) {
		int opCost = current.computeOPCost(stats);
		int opMax = current.getHullSpec().getOrdnancePoints(stats);
		int opLeft = opMax - opCost;
		
		if (opLeft > 0) {
			int maxVents = getMaxVents(current.getHullSize());
			opLeft -= addVents((int) opLeft, current, maxVents);
		}
	}
	
	public void addExtraCaps(ShipVariantAPI current) {
		int opCost = current.computeOPCost(stats);
		int opMax = current.getHullSpec().getOrdnancePoints(stats);
		int opLeft = opMax - opCost;
		
		if (opLeft > 0) {
			int maxCaps = getMaxCaps(current.getHullSize());
			opLeft -= addCapacitors((int) opLeft, current, maxCaps);
		}
	}
	
	public void addExtraVentsAndCaps(ShipVariantAPI current, ShipVariantAPI target) {
		int opCost = current.computeOPCost(stats);
		int opMax = current.getHullSpec().getOrdnancePoints(stats);
		int opLeft = opMax - opCost;
		
		int maxVents = getMaxVents(current.getHullSize());
		int maxCapacitors = getMaxCaps(current.getHullSize());
		if (opLeft > 0) {
			
			float total = current.getNumFluxVents() + current.getNumFluxCapacitors();
			float ventsFraction = 1f;
			if (total > 0) {
				ventsFraction = current.getNumFluxVents() / total;
			}
			
			int add = (int) (opLeft * ventsFraction);
			opLeft -= addVents(add, current, maxVents);
			add = opLeft;
			opLeft -= addCapacitors(add, current, maxCapacitors);
			
			add = opLeft;
			opLeft -= addVents(add, current, maxVents);
		
			// if we ended up with more capacitors than desired, move some of them to vents
			if (target != null) {
				float targetVents = target.getNumFluxVents();
				float targetCaps = target.getNumFluxCapacitors();
				
				if (targetVents > targetCaps || targetVents >= maxVents) {
					float currVents = current.getNumFluxVents();
					float currCaps = current.getNumFluxCapacitors();
					float currTotal = currVents + currCaps;
					
					int currVentsDesired = (int) (currVents + currCaps * 0.5f);
					if (currVentsDesired > maxVents) currVentsDesired = maxVents;
					int currCapsDesired = (int) (currTotal - currVentsDesired);
					if (currCapsDesired > maxCapacitors) currCapsDesired = maxCapacitors;
					current.setNumFluxVents(currVentsDesired);
					current.setNumFluxCapacitors(currCapsDesired);
				}
				
	//			if (targetVents > 0 && currVents + currCaps > 0) {
	//				float ratioTarget = targetVents / (targetVents + targetCaps);
	//				float ratioCurr = currVents / (currVents + currCaps);
	//				if (ratioTarget > ratioCurr) {
	//					float currTotal = currVents + currCaps;
	//					int currVentsDesired = (int) (ratioTarget * currTotal);
	//					if (currVentsDesired > maxVents) currVentsDesired = maxVents;
	//					int currCapsDesired = (int) (currTotal - currVents);
	//					if (currCapsDesired > maxCapacitors) currCapsDesired = maxCapacitors;
	//					current.setNumFluxVents(currVentsDesired);
	//					current.setNumFluxCapacitors(currCapsDesired);
	//				}
	//			}
			}
		}
		
	}
	
	public int getMaxVents(HullSize size) {
		int maxVents = getBaseMax(size);
		if (stats != null) {
			maxVents = (int) stats.getMaxVentsBonus().computeEffective(maxVents);
		}
		return maxVents;
	}
	
	public int getMaxCaps(HullSize size) {
		int maxCapacitors = getBaseMax(size);
		if (stats != null) {
			maxCapacitors = (int) stats.getMaxCapacitorsBonus().computeEffective(maxCapacitors);
		}
		return maxCapacitors;
	}
	
	public static int getBaseMax(HullSize size) {
		int max = 100;
		switch (size) {
		case CAPITAL_SHIP: max = 50; break;
		case CRUISER: max = 30; break;
		case DESTROYER:	max = 20; break;
		case FRIGATE: max = 10; break;
		case FIGHTER: max = 5; break;
		default:
			max = 10; break;
		}
		return max;
	}
	
	public int addVents(int add, ShipVariantAPI current, int max) {
		int target = current.getNumFluxVents() + add;
		if (target > max) target = max;
		if (target < 0) target = 0;
		int actual = target - current.getNumFluxVents();
		current.setNumFluxVents(target);
		return actual;
	}
	
	public int addCapacitors(int add, ShipVariantAPI current, int max) {
		int target = current.getNumFluxCapacitors() + add;
		if (target > max) target = max;
		if (target < 0) target = 0;
		int actual = target - current.getNumFluxCapacitors();
		current.setNumFluxCapacitors(target);
		return actual;
	}

	public void clearWeaponSlot(WeaponSlotAPI slot, AutofitPluginDelegate delegate, ShipVariantAPI variant) {
		fittedWeapons.remove(variant.getHullVariantId() + "_" + slot.getId());
		delegate.clearWeaponSlot(slot, variant);
	}
	
	public void clearFighterSlot(int index, AutofitPluginDelegate delegate, ShipVariantAPI variant) {
		fittedFighters.remove(variant.getHullVariantId() + "_" + index);
		delegate.clearFighterSlot(index, variant);
	}

	public void fitWeapons(ShipVariantAPI current, ShipVariantAPI target, boolean upgradeMode, AutofitPluginDelegate delegate) {
		
		Set<String> alreadyUsed = new HashSet<String>();
		for (WeaponSlotAPI slot : getWeaponSlotsInPriorityOrder(current, target, upgradeMode)) {
			if (slotsToSkip.contains(slot.getId())) continue;
			
			float opCost = current.computeOPCost(stats);
			float opMax = current.getHullSpec().getOrdnancePoints(stats);
			float opLeft = opMax - opCost;
			
			float levelToBeat = -1;
			if (upgradeMode) {
				WeaponSpecAPI curr = current.getWeaponSpec(slot.getId());
				if (curr != null) {
					float cost = curr.getOrdnancePointCost(stats, current.getStatsForOpCosts());
					opLeft += cost;
					
					for (String tag : curr.getTags()) {
						levelToBeat = Math.max(levelToBeat, getLevel(tag));
					}
					if (delegate.isPriority(curr)) {
						levelToBeat += PRIORITY;
					}
				}
			}
			
			WeaponSpecAPI desired = target.getWeaponSpec(slot.getId());
			if (desired == null) continue;
			
			List<AvailableWeapon> weapons = getWeapons(delegate);
			List<AvailableWeapon> possible = getPossibleWeapons(slot, desired, current, opLeft, weapons);
			if (possible.isEmpty()) continue;
			
			List<String> categories = desired.getAutofitCategoriesInPriorityOrder(); 
			altWeaponCats.put(desired, new ArrayList<String>());
			
			AvailableWeapon pick = null;
			for (String catId : categories) {
				pick = getBestMatch(desired, upgradeMode, catId, alreadyUsed, possible, slot, delegate);
				if (pick != null) {
					break;
				}
				if (upgradeMode) break; // don't pick from secondary categories when upgrading
			}
			
			if (pick == null && !upgradeMode) {
				OUTER: for (String catId : categories) {
					Category cat = this.categories.get(catId);
					if (cat == null) continue;
					
					for (String fallbackCatId : cat.fallback) {
						pick = getBestMatch(desired, true, fallbackCatId, alreadyUsed, possible, delegate);
						if (pick != null) {
							break OUTER;
						}
					}
				}
			}
			
			if (pick != null) {
				if (upgradeMode) {
					float pickLevel = -1;
					if (!categories.isEmpty()) {
						Category cat = this.categories.get(categories.get(0));
						if (cat != null) {
							String tag = getCategoryTag(cat, pick.getSpec().getTags());
							pickLevel = getLevel(tag);
							if (delegate.isPriority(pick.getSpec())) {
								pickLevel += PRIORITY;
							}
						}
					}
					if (pickLevel <= levelToBeat) continue;
				}
				
				alreadyUsed.add(pick.getId());
				
				clearWeaponSlot(slot, delegate, current);
				delegate.fitWeaponInSlot(slot, pick, current);
				fittedWeapons.put(current.getHullVariantId() + "_" + slot.getId(), pick);
				
				if (pick.getSpec().getType() == WeaponType.MISSILE && pick.getSpec().usesAmmo()) {
					missilesWithAmmoOnCurrent++;
				}
			}
		}
		
	}
	
	
	public void fitFighters(ShipVariantAPI current, ShipVariantAPI target, boolean upgradeMode, AutofitPluginDelegate delegate) {
		int numBays = Global.getSettings().computeNumFighterBays(current);
		
		Set<String> alreadyUsed = new HashSet<String>();
		
		for (int i = 0; i < numBays; i++) {
			if (baysToSkip.contains(i)) continue;
			
			float opCost = current.computeOPCost(stats);
			float opMax = current.getHullSpec().getOrdnancePoints(stats);
			float opLeft = opMax - opCost;
			
			float levelToBeat = -1;
			if (upgradeMode) {
				FighterWingSpecAPI curr = current.getWing(i);
				if (curr != null) {
					float cost = curr.getOpCost(current.getStatsForOpCosts());
					opLeft += cost;
					
					for (String tag : curr.getTags()) {
						levelToBeat = Math.max(levelToBeat, getLevel(tag));
					}
					if (delegate.isPriority(curr)) {
						levelToBeat += PRIORITY;
					}
				}
			} else {
				if (current.getWingId(i) != null) {
					continue;
				}
			}
			
			List<AvailableFighter> fighters = getFighters(delegate);
			List<AvailableFighter> possible = getPossibleFighters(current, opLeft, fighters);
			if (possible.isEmpty()) continue;
			
			String desiredWingId = target.getWingId(i);
			if (desiredWingId == null || desiredWingId.isEmpty()) {
				desiredWingId = emptyWingTarget;
			}
			
			FighterWingSpecAPI desired = Global.getSettings().getFighterWingSpec(desiredWingId);
			if (desired == null) continue;
			
			//List<String> categories = getCategoriesInPriorityOrder(desired.getTags()); 
			List<String> categories = desired.getAutofitCategoriesInPriorityOrder();
			
			AvailableFighter pick = null;
			for (String catId : categories) {
				pick = getBestMatch(desired, upgradeMode, catId, alreadyUsed, possible, delegate);
				if (pick != null) {
					break;
				}
				if (upgradeMode) break; // don't pick from secondary categories when upgrading
			}
			
			if (pick == null && !upgradeMode) {
				OUTER: for (String catId : categories) {
					Category cat = this.categories.get(catId);
					if (cat == null) continue;
					
					for (String fallbackCatId : cat.fallback) {
						pick = getBestMatch(desired, true, fallbackCatId, alreadyUsed, possible, delegate);
						if (pick != null) {
							break OUTER;
						}
					}
				}
			}
			
			if (pick != null) {
				if (upgradeMode) {
					float pickLevel = -1;
					if (!categories.isEmpty()) {
						Category cat = this.categories.get(categories.get(0));
						if (cat != null) {
							String tag = getCategoryTag(cat, pick.getWingSpec().getTags());
							pickLevel = getLevel(tag);
							if (delegate.isPriority(pick.getWingSpec())) {
								pickLevel += PRIORITY;
							}
						}
					}
					if (pickLevel <= levelToBeat) continue;
				}
				
				alreadyUsed.add(pick.getId());
				
				clearFighterSlot(i, delegate, current);
				delegate.fitFighterInSlot(i, pick, current);
				fittedFighters.put(current.getHullVariantId() + "_" + i, pick);
			}
		}
		
	}

	
	
	
	
	public AvailableWeapon getBestMatch(WeaponSpecAPI desired, boolean useBetter,
			String catId, Set<String> alreadyUsed, List<AvailableWeapon> possible,
			AutofitPluginDelegate delegate) {
		return getBestMatch(desired, useBetter, catId, alreadyUsed, possible, null, delegate);
	}
	
	public AvailableWeapon getBestMatch(WeaponSpecAPI desired, boolean useBetter,
			   							String catId, Set<String> alreadyUsed, List<AvailableWeapon> possible,
			   							WeaponSlotAPI slot,
			   							AutofitPluginDelegate delegate) {
		//AvailableWeapon best = null;
		float bestScore = -1f;
		boolean bestIsPriority = false;
		int bestSize = -1;
		
		Category cat = categories.get(catId);
		if (cat == null) return null;
		
		String desiredTag = getCategoryTag(cat, desired.getTags());
		float desiredLevel = getLevel(desiredTag);
		
		if (desiredTag == null) {
			// fallback to categories that aren't in the tags of the desired weapon
//			for (String tag : desired.getTags()) {
//				desiredLevel = Math.max(desiredLevel, getLevel(tag));
//			}
			desiredLevel = 10000f;
		}
		
		boolean longRange = desired.hasTag(CoreAutofitPlugin.LR);
		boolean shortRange = desired.hasTag(CoreAutofitPlugin.SR);
		boolean midRange = !longRange && !shortRange;
		boolean desiredPD = desired.getAIHints().contains(AIHints.PD);
		
		WeightedRandomPicker<AvailableWeapon> best = new WeightedRandomPicker<AvailableWeapon>(random);
		
		int iter = 0;
		for (AvailableWeapon w : possible) {
			iter++;
			WeaponSpecAPI spec = w.getSpec();
			String catTag = getCategoryTag(cat, spec.getTags());
			if (catTag == null) continue; // not in this category
			
			boolean currShortRange = spec.hasTag(CoreAutofitPlugin.SR);
			
			// don't fit short-range weapons instead of long-range ones unless it's PD 
			if (!desiredPD && currShortRange && (midRange || longRange)) continue;
			//if (currMidRange && longRange) continue;
			
			boolean isPrimaryCategory = cat.base.equals(spec.getAutofitCategory());
			boolean currIsPriority = isPrimaryCategory && delegate.isPriority(spec);
			int currSize = spec.getSize().ordinal();
			boolean betterDueToPriority = currSize >= bestSize && currIsPriority && !bestIsPriority;
			boolean worseDueToPriority = currSize <= bestSize && !currIsPriority && bestIsPriority;
			
			if (worseDueToPriority) continue;
			
			float level = getLevel(catTag);
			if (!useBetter && !betterDueToPriority && level > desiredLevel) continue;
			int rMag = 0;
			if (rMag > 0) {
				boolean symmetric = random.nextFloat() < 0.75f;
				if (slot != null && symmetric) {
					long seed = (Math.abs((int)(slot.getLocation().x/2f)) * 723489413945245311L) ^ 1181783497276652981L;
					Random r = new Random((seed + weaponFilterSeed) * iter);
					level += r.nextInt(rMag);
				} else {
					level += random.nextInt(rMag);
				}
			}
			
			
			float score = level;
//			if (delegate.isPriority(spec)) {
//				score += PRIORITY;
//			}
			if ((score > bestScore || betterDueToPriority)) {
				//best = w;
				best.clear();
				best.add(w);
				bestScore = score;
				bestSize = currSize;
				bestIsPriority = currIsPriority;
			} else if (score == bestScore) {
				best.add(w);
			}
		}
//		if (desired.getWeaponId().equals("autopulse")) {
//			System.out.println("wefwefwe");
//		}
		
		
		// if the best-match tier includes the weapon specified in the target variant, use that
		// prefer one we already have to buying
		List<AvailableWeapon> allMatches = new ArrayList<AvailableWeapon>();
		List<AvailableWeapon> freeMatches = new ArrayList<AvailableWeapon>();
		for (AvailableWeapon w : best.getItems()) {
			if (desired.getWeaponId().equals(w.getId())) {
				allMatches.add(w);
				if (w.getPrice() <= 0) {
					freeMatches.add(w);
				}
			}
		}
		if (!freeMatches.isEmpty()) return freeMatches.get(0);
		if (!allMatches.isEmpty()) return allMatches.get(0);
		
		// if the best-match tier includes a weapon that we already own, filter out all non-free ones
		boolean hasFree = false;
		boolean hasNonBlackMarket = false;
		for (AvailableWeapon w : best.getItems()) {
			if (w.getPrice() <= 0) {
				hasFree = true;
			}
			if (w.getSubmarket() == null || !w.getSubmarket().getPlugin().isBlackMarket()) {
				hasNonBlackMarket = true;
			}
		}
		if (hasFree) {
			for (AvailableWeapon w : new ArrayList<AvailableWeapon>(best.getItems())) {
				if (w.getPrice() > 0) {
					best.remove(w);	
				}
			}
		} else if (hasNonBlackMarket) {
			for (AvailableWeapon w : new ArrayList<AvailableWeapon>(best.getItems())) {
				if (w.getSubmarket() != null && w.getSubmarket().getPlugin().isBlackMarket()) {
					best.remove(w);	
				}
			}
		}
		
		// if the best-match tier includes a weapon we used already, use that
		if (!alreadyUsed.isEmpty()) {
			for (AvailableWeapon w : best.getItems()) {
				if (alreadyUsed.contains(w.getId())) return w;
			}
		}
		
		if (best.isEmpty()) return null;
		
		//return best.getItems().get(0);
		return best.pick();
	}
	

	public AvailableFighter getBestMatch(FighterWingSpecAPI desired, boolean useBetter,
										String catId, Set<String> alreadyUsed, List<AvailableFighter> possible,
										AutofitPluginDelegate delegate) {
		float bestScore = -1f;
		boolean bestIsPriority = false;
		
		Category cat = categories.get(catId);
		if (cat == null) return null;

		String desiredTag = getCategoryTag(cat, desired.getTags());
		float desiredLevel = getLevel(desiredTag);

		WeightedRandomPicker<AvailableFighter> best = new WeightedRandomPicker<AvailableFighter>(random);

		for (AvailableFighter f : possible) {
			FighterWingSpecAPI spec = f.getWingSpec();
			String catTag = getCategoryTag(cat, spec.getTags());
			if (catTag == null) continue; // not in this category

			boolean isPrimaryCategory = cat.base.equals(spec.getAutofitCategory());
			boolean currIsPriority = isPrimaryCategory && delegate.isPriority(spec);
			boolean betterDueToPriority = currIsPriority && !bestIsPriority;
			boolean worseDueToPriority = !currIsPriority && bestIsPriority;

			if (worseDueToPriority) continue;
			
			float level = getLevel(catTag);
			if (!useBetter && !betterDueToPriority && level > desiredLevel) continue;
			//if (randomize) level += random.nextInt(20);
			
			int rMag = 0;
			if (rMag > 0) {
				level += random.nextInt(rMag);
			}

			float score = level;
//			if (delegate.isPriority(spec)) {
//				score += PRIORITY;
//			}
			if (score > bestScore || betterDueToPriority) {
				best.clear();
				best.add(f);
				bestScore = score;
				bestScore = score;
				bestIsPriority = currIsPriority;
			} else if (score == bestScore) {
				best.add(f);
			}
		}


		// if the best-match tier includes the fighter specified in the target variant, use that
		List<AvailableFighter> allMatches = new ArrayList<AvailableFighter>();
		List<AvailableFighter> freeMatches = new ArrayList<AvailableFighter>();
		for (AvailableFighter f : best.getItems()) {
			if (desired.getId().equals(f.getId())) {
				allMatches.add(f);
				if (f.getPrice() <= 0) {
					freeMatches.add(f);
				}
			}
		}
		if (!freeMatches.isEmpty()) return freeMatches.get(0);
		if (!allMatches.isEmpty()) return allMatches.get(0);

		// if the best-match tier includes a fighter that we already own, filter out all non-free ones
		// prefer one we already have to buying
		boolean hasFree = false;
		boolean hasNonBlackMarket = false;
		for (AvailableFighter f : best.getItems()) {
			if (f.getPrice() <= 0) {
				hasFree = true;
			}
			if (f.getSubmarket() == null || !f.getSubmarket().getPlugin().isBlackMarket()) {
				hasNonBlackMarket = true;
			}
		}
		if (hasFree) {
			for (AvailableFighter f : new ArrayList<AvailableFighter>(best.getItems())) {
				if (f.getPrice() > 0) {
					best.remove(f);	
				}
			}
		} else if (hasNonBlackMarket) {
			for (AvailableFighter f : new ArrayList<AvailableFighter>(best.getItems())) {
				if (f.getSubmarket() != null && f.getSubmarket().getPlugin().isBlackMarket()) {
					best.remove(f);	
				}
			}
		}

		
		// if the best-match tier includes a fighter we used already, use that
		if (!alreadyUsed.isEmpty()) {
			for (AvailableFighter f : best.getItems()) {
				if (alreadyUsed.contains(f.getId())) return f;
			}
		}

		if (best.isEmpty()) return null;

		//return best.getItems().get(0);
		return best.pick();
	}
	
	public String getCategoryTag(Category cat, Set<String> tags) {
		String catTag = null;
		for (String tag : tags) {
			if (cat.tags.contains(tag)) {
				catTag = tag;
				break;
			}
		}
		return catTag;
	}
	

	protected static transient Map<String, Integer> tagLevels = new HashMap<String, Integer>();

	
	public float getLevel(String tag) {
		Integer result = tagLevels.get(tag);
		if (result != null) return result;
		Category cat = categories.get(tag);
		if (cat == null) {
			tagLevels.put(tag, -1);
			return -1f;
		}
		try {
			result = (int) Float.parseFloat(tag.replaceAll(cat.base, ""));
			tagLevels.put(tag, result);
			return result;
		} catch (Throwable t) {
			tagLevels.put(tag, -1);
			return -1f;
		}
	}
	
//	public List<String> getCategoriesInPriorityOrder(Set<String> tags) {
////		final Map<String, Float> levels = new HashMap<String, Float>();
//		List<String> result = new ArrayList<String>();
//		result.addAll(tags);
////		for (String tag : tags) {
////			float level = getLevel(tag);
////			if (level < 0) continue;
////			levels.put(tag, level);
////			result.add(tag);
////		}
//		
//		Collections.sort(result, new Comparator<String>() {
//			public int compare(String o1, String o2) {
//				//return (int)Math.signum(levels.get(o2) - levels.get(o1));
//				return (int)Math.signum(getLevel(o2) - getLevel(o1));
//			}
//		});
//		
//		return result;
//	}
	
	
	public List<WeaponSlotAPI> getWeaponSlotsInPriorityOrder(ShipVariantAPI current, ShipVariantAPI target, boolean upgradeMode) {
		List<WeaponSlotAPI> result = new ArrayList<WeaponSlotAPI>();

		for (WeaponSlotAPI slot : current.getHullSpec().getAllWeaponSlotsCopy()) {
			if (slot.isBuiltIn() || slot.isDecorative()) continue;
			if (target.getWeaponId(slot.getId()) == null) continue;
			if (!upgradeMode && current.getWeaponId(slot.getId()) != null) continue;
			result.add(slot);
		}
		
		Collections.sort(result, new Comparator<WeaponSlotAPI>() {
			public int compare(WeaponSlotAPI w1, WeaponSlotAPI w2) {
				float s1 = getSlotPriorityScore(w1);
				float s2 = getSlotPriorityScore(w2);
				return (int) Math.signum(s2 - s1);
			}
		});
		
		return result;
	}

	public float getSlotPriorityScore(WeaponSlotAPI slot) {
		float score = 0;
		
		switch (slot.getSlotSize()) {
		case LARGE: score = 10000; break;
		case MEDIUM: score = 5000; break;
		case SMALL: score = 2500; break;
		}
		float angleDiff = Misc.getAngleDiff(slot.getAngle(), 0);
		boolean front = Misc.isInArc(slot.getAngle(), slot.getArc(), 0);
		if (front) {
			//score += 10f;
			score += 180f - angleDiff;
		}
		
		return score;
	}
	
	
	
	public List<AvailableWeapon> getPossibleWeapons(WeaponSlotAPI slot, WeaponSpecAPI desired, ShipVariantAPI current, float opLeft, List<AvailableWeapon> weapons) {
		List<AvailableWeapon> result = new ArrayList<AvailableWeapon>();
		
		for (AvailableWeapon w : weapons) {
			if (w.getQuantity() <= 0) continue;
			
			WeaponSpecAPI spec = w.getSpec();
			//float cost = spec.getOrdnancePointCost(stats, current.getStatsForOpCosts());
			float cost = w.getOPCost(stats, current.getStatsForOpCosts());
			if (cost > opLeft) continue;
			if (!slot.weaponFits(spec)) continue;
			
			if (spec != desired && 
					(spec.getType() == WeaponType.MISSILE || spec.getAIHints().contains(AIHints.STRIKE))) {
				boolean guided = spec.getAIHints().contains(AIHints.DO_NOT_AIM);
				if (!guided) {
					boolean guidedPoor = spec.getAIHints().contains(AIHints.GUIDED_POOR);
					float angleDiff = Misc.getDistanceFromArc(slot.getAngle(), slot.getArc(), 0);
					if (angleDiff > 45 || (!guidedPoor && angleDiff > 20)) continue;
				}
			}
			
			result.add(w);
		}
		
		return result;
	}
	
	public List<AvailableFighter> getPossibleFighters(ShipVariantAPI current, float opLeft, List<AvailableFighter> fighters) {
		List<AvailableFighter> result = new ArrayList<AvailableFighter>();
		
		for (AvailableFighter f : fighters) {
			if (f.getQuantity() <= 0) continue;
			
			FighterWingSpecAPI spec = f.getWingSpec();
			float cost = spec.getOpCost(current.getStatsForOpCosts());
			if (cost > opLeft) continue;
			
			result.add(f);
		}
		
		Random filterRandom = new Random(weaponFilterSeed);
		int num = Math.max(1, result.size() / 3 * 2);
		Set<Integer> picks = DefaultFleetInflater.makePicks(num, result.size(), filterRandom);
		List<AvailableFighter> filtered = new ArrayList<AvailableFighter>();
		for (Integer pick : picks) {
			filtered.add(result.get(pick));
		}
		result = filtered;
		
		return result;
	}
	
	
	public List<AutofitOption> getOptions() {
		return options;
	}

	public float getRating(ShipVariantAPI current, ShipVariantAPI target, AutofitPluginDelegate delegate) {
		return 0;
	}

	@Override
	public void doQuickAction(ShipVariantAPI current, AutofitPluginDelegate delegate) {
//		if (!fittingModule) {
//			availableMods = new LinkedHashSet<String>(delegate.getAvailableHullmods());
//		}
//		
//		int index = 0;
//		for (String slotId : current.getStationModules().keySet()) {
//			ShipVariantAPI moduleCurrent = current.getModuleVariant(slotId);
//			if (moduleCurrent == null) continue;
//			if (moduleCurrent.isStockVariant()) {
//				moduleCurrent = moduleCurrent.clone();
//				moduleCurrent.setSource(VariantSource.REFIT);
//				//moduleCurrent.setHullVariantId(Misc.genUID());
//				moduleCurrent.setHullVariantId(moduleCurrent.getHullVariantId() + "_" + index);
//			}
//			index++;
//			
//			fittingModule = true;
//			doQuickAction(moduleCurrent, delegate);
//			fittingModule = false;
//			
//			current.setModuleVariant(slotId, moduleCurrent);
//			current.setSource(VariantSource.REFIT);
//		}
		availableMods = new LinkedHashSet<String>(delegate.getAvailableHullmods());
		
		if (current.getHullSize().ordinal() >= HullSize.DESTROYER.ordinal() && !current.isCivilian()) {
			addHullmods(current, delegate, HullMods.INTEGRATED_TARGETING_UNIT);
		}
		
		//addHullmods(current, delegate, HullMods.REINFORCEDHULL);
		addExtraVentsAndCaps(current, null);
//		addExtraVents(current);
//		addExtraCaps(current);
		addDistributor(current, delegate);
		addDistributorRemoveVentsIfNeeded(current, delegate);
		addCoilRemoveCapsIfNeeded(current, delegate);
		//addModsWithSpareOPIfAny(current, current, false, delegate);
		addHullmods(current, delegate, HullMods.REINFORCEDHULL, HullMods.BLAST_DOORS, HullMods.HARDENED_SUBSYSTEMS);
		
		if (!fittingModule) {
			delegate.syncUIWithVariant(current);
		}
	}
	
	public boolean isQuickActionEnabled(ShipVariantAPI currentVariant) {
		int unusedOpTotal = 0;
		for (String slotId : currentVariant.getStationModules().keySet()) {
			ShipVariantAPI moduleCurrent = currentVariant.getModuleVariant(slotId);
			if (moduleCurrent == null) continue;
			unusedOpTotal += moduleCurrent.getUnusedOP(stats);
		}
		unusedOpTotal += currentVariant.getUnusedOP(stats);
		return unusedOpTotal > 0;
		
		//return currentVariant.getUnusedOP(stats) > 0;
	}
	
	
	public static class AutoAssignScore {
		public float [] score;
		public FleetMemberAPI member;
		public PersonAPI officer;
	}
	
	
	@Override
	public void autoAssignOfficers(CampaignFleetAPI fleet) {
		List<FleetMemberAPI> members = new ArrayList<FleetMemberAPI>();
		for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
			if (member.isMothballed()) {
				continue;
			}
			if (!member.getCaptain().isDefault()) {
				continue;
			}
			if (fleet.isPlayerFleet() && Misc.isAutomated(member)) continue;
			members.add(member);
		}
		
		List<OfficerDataAPI> officers = new ArrayList<OfficerDataAPI>();
		int max = (int) fleet.getCommander().getStats().getOfficerNumber().getModifiedValue();
		int count = 0;
		for (OfficerDataAPI officer : fleet.getFleetData().getOfficersCopy()) {
			boolean merc = Misc.isMercenary(officer.getPerson());
			if (!merc) {
				count++;
			}
			if (count > max && !merc) continue;
			
			boolean found = false;
			for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
				if (member.getCaptain() == officer.getPerson()) {
					found = true;
					break;
				}
			}
			if (!found) {
				officers.add(officer);
			}
		}

		
		List<AutoAssignScore> shipScores = new ArrayList<AutoAssignScore>();
		List<AutoAssignScore> officerScores = new ArrayList<AutoAssignScore>();
		
		float maxMemberTotal = 1f;
		float maxOfficerTotal = 1f;
		
		for (FleetMemberAPI member : members) {
			AutoAssignScore score = new AutoAssignScore();
			shipScores.add(score);
			score.member = member;
			score.score = computeMemberScore(member);
			
			maxMemberTotal = Math.max(maxMemberTotal, score.score[4]);
		}
		
		for (OfficerDataAPI officer : officers) {
			AutoAssignScore score = new AutoAssignScore();
			officerScores.add(score);
			score.officer = officer.getPerson();
			score.score = computeOfficerScore(officer.getPerson());
			maxOfficerTotal = Math.max(maxOfficerTotal, score.score[4]);
		}
		
		for (AutoAssignScore score : officerScores) {
			// so that the best officers are closer to the best ships
			// and the lowest-level officers are still closer to the best ships than to the worst ships
			score.score[4] = maxMemberTotal + (maxOfficerTotal - score.score[4]);
		}
		
		while (!shipScores.isEmpty() && !officerScores.isEmpty()) {
			float minDist = Float.MAX_VALUE;
			AutoAssignScore bestShip = null;
			AutoAssignScore bestOfficer = null;
			for (AutoAssignScore ship : shipScores) {
//				if (ship.member.getHullId().equals("condor")) {
//					System.out.println("wefewfew");
//				}
				for (AutoAssignScore officer : officerScores) {
					float dist = Math.abs(ship.score[0] - officer.score[0]) + 
								 Math.abs(ship.score[1] - officer.score[1]) +
								 Math.abs(ship.score[2] - officer.score[2]) +
								 Math.abs(ship.score[3] - officer.score[3]) +
								 Math.abs(ship.score[4] - officer.score[4]);

					if (dist < minDist) {
						minDist = dist;
						bestShip = ship;
						bestOfficer = officer;
					}
				}
			}
			if (bestShip == null) {
				break;
			}
			
			shipScores.remove(bestShip);
			officerScores.remove(bestOfficer);
			bestShip.member.setCaptain(bestOfficer.officer);
		}
	}

	public float [] computeOfficerScore(PersonAPI officer) {
		float energy = 0f;
		float ballistic = 0f;
		float missile = 0f;
		float defense = 0f;
		float total = 0f;
		
		for (SkillLevelAPI sl : officer.getStats().getSkillsCopy()) {
			if (!sl.getSkill().isCombatOfficerSkill()) continue;
			float w = sl.getLevel();
			if (w == 2) w = 1.33f; // weigh elite skills as less than double
			if (w <= 0f) {
				continue;
			}
			
			if (sl.getSkill().hasTag(Skills.TAG_ENERGY_WEAPONS)) {
				energy++;
			} else if (sl.getSkill().hasTag(Skills.TAG_BALLISTIC_WEAPONS)) {
				ballistic++;
			} else if (sl.getSkill().hasTag(Skills.TAG_MISSILE_WEAPONS)) {
				missile++;
			} else if (sl.getSkill().hasTag(Skills.TAG_ACTIVE_DEFENSES)) {
				defense++;
			}
			total++;
		}
		
		if (total < 1f) total = 1f;
		energy /= total;
		ballistic /= total;
		missile /= total;
		defense /= total;
		
		float [] result = new float [5];
		result[0] = energy;
		result[1] = ballistic;
		result[2] = missile;
		result[3] = defense;
		result[4] = total;
		return result;
	}
	
	public float [] computeMemberScore(FleetMemberAPI member) {
		float energy = 0f;
		float ballistic = 0f;
		float missile = 0f;
		float total = 0f;
		
		boolean civ = member.isCivilian();
		
		for (String slotId : member.getVariant().getFittedWeaponSlots()) {
			WeaponSlotAPI slot = member.getVariant().getSlot(slotId);
			if (slot.isDecorative() || slot.isSystemSlot()) continue;
			
			WeaponSpecAPI weapon = member.getVariant().getWeaponSpec(slotId);
			float w = 1f;
			switch (weapon.getSize()) {
			case LARGE: w = 4f; break;
			case MEDIUM: w = 2f; break;
			case SMALL: w = 1f; break;
			}
			if (civ) w *= 0.1f;
			WeaponType type = weapon.getType();
			if (type == WeaponType.BALLISTIC) { 
				ballistic += w;
				total += w;
			} else if (type == WeaponType.ENERGY) { 
				energy += w;
				total += w;
			} else if (type == WeaponType.MISSILE) { 
				missile += w;
				total += w;
			} else {
				total += w;
			}
		}
		if (total < 1f) total = 1f;
		energy /= total;
		ballistic /= total;
		missile /= total;

		boolean d = member.getHullSpec().getShieldType() == ShieldType.FRONT ||
			 		member.getHullSpec().getShieldType() == ShieldType.OMNI || 
			 		member.getHullSpec().isPhase();
		
		float [] result = new float [5];
		result[0] = energy;
		result[1] = ballistic;
		result[2] = missile;
		if (d) {
			result[3] = 1f;
		} else {
			result[3] = 0f;
		}
		result[4] = total;
		
		return result;
	}
	
	public float getVariantOPFraction(FleetMemberAPI member) {
		float f = 1f;
		float op = member.getVariant().getHullSpec().getOrdnancePoints(stats);
		if (op > 0) {
			f = (op - member.getVariant().getUnusedOP(stats)) / op;
		}
		return f;
	}
	
	public float getSkillTotal(OfficerDataAPI officer, boolean carrier) {
		float total = 0f;
		for (SkillLevelAPI skill : officer.getPerson().getStats().getSkillsCopy()) {
			SkillSpecAPI spec = skill.getSkill();
			if (!spec.isCombatOfficerSkill()) continue;
			
			float level = skill.getLevel();
			if (level <= 0) continue;
			
			if (!carrier || spec.hasTag(Skills.TAG_CARRIER)) {
				total += level;
			}
		}
		return total;
	}
	
}











