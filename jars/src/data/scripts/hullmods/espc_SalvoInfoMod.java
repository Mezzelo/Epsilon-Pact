package data.scripts.hullmods;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.shipsystems.espc_SalvoStats;
import data.scripts.shipsystems.espc_SalvoStats.MissileToFire;

public class espc_SalvoInfoMod extends BaseHullMod {
	
	public static class SalvoMissileData {
		public String id;
		public String name;
		public WeaponSize size;
		public boolean isLimited;
		public int OPCost;
		public float maxAmmoOrSustainedRate;
		public int missilesFired;
		public int burstSizeBase;
		public int OPPerMissile;
		public SalvoMissileData(String id, String name, WeaponSize size, boolean isLimited, int OPCost,
			float maxAmmoOrSustainedRate, int missilesFired, int burstSizeBase, int OPPerMissile) {
			this.id = id;
			this.name = name;
			this.size = size;
			this.isLimited = isLimited;
			this.OPCost = OPCost;
			this.maxAmmoOrSustainedRate = maxAmmoOrSustainedRate;
			this.missilesFired = missilesFired;
			this.burstSizeBase = burstSizeBase;
			this.OPPerMissile = OPPerMissile;
		}
		public SalvoMissileData(String id) {
			this.id = id;
		}
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, final ShipAPI ship, float width, boolean isForModSpec) {
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		
		tooltip.addPara("Retrofitted clamps enable fighters to launch the ship's equipped missiles via the system, "
				+ "without requiring or expending ammunition.", opad);
		tooltip.addPara("Each bay gives a budget of %s OP worth of missiles and %s fighters to fire missiles from. "
				+ "A single fighter cannot launch more than %s equipped missile.", opad,
				h, "" + (int) espc_SalvoStats.OP_PER_WING, "" + (int) espc_SalvoStats.FIGHTERS_PER_WING, "1");
		
		boolean hasLimitedMissiles = false;
		boolean hasReloadingMissiles = false;
		boolean onlyFragmentMissiles = false;
		boolean onlyIneligibleMissiles = false;
		List<SalvoMissileData> weapons = null;
		
		int maxCost = 3;
		
		if (!isForModSpec && ship != null && !Global.CODEX_TOOLTIP_MODE) {
			maxCost = Math.min(
				espc_SalvoStats.MAX_MISSILE_OP, 
				espc_SalvoStats.OP_PER_WING * ship.getHullSpec().getFighterBays());
			weapons = new ArrayList<SalvoMissileData>();
			for (WeaponAPI w : ship.getAllWeapons()) {
				if (w.isDecorative() || 
					w.getSlot() != null && (w.getSlot().isSystemSlot() || w.getSlot().isBuiltIn()) || 
					!w.getType().equals(WeaponType.MISSILE))
					continue;
				if (w.getSpec().hasTag(Tags.FRAGMENT)) {
					if (weapons.size() <= 0) {
						onlyFragmentMissiles = true;
						onlyIneligibleMissiles = true;
					}
					continue;
				}
				onlyFragmentMissiles = false;
				
				boolean found = false;
				for (int i = 0; i < weapons.size(); i++) {
					if (weapons.get(i).id.equals(w.getId())) {
						found = true;
						break;
					}
				}
				if (!found) {
					MissileToFire data = espc_SalvoStats.calcMissile(w);
					if (data.missilesPerMinute <= 0f)
						hasLimitedMissiles = true;
					else
						hasReloadingMissiles = true;
					if (data.cost > maxCost) {
						if (weapons.size() <= 0)
							onlyIneligibleMissiles = true;
						continue;
					}
					onlyIneligibleMissiles = false;
					
					weapons.add(new SalvoMissileData(
						w.getId(),
						w.getDisplayName(),
						w.getSize(),
						data.missilesPerMinute <= 0f,
						(int) w.getSpec().getOrdnancePointCost(null),
						(data.missilesPerMinute <= 0f ? w.getSpec().getMaxAmmo() : data.missilesPerMinute),
						data.count,
						w.getSpec().getBurstSize(),
						data.cost
					));
				}
			}
			if (hasLimitedMissiles || hasReloadingMissiles) {

				float row2W = 100f;
				float row3W = 120f;
				float sizeW = width - row2W - row3W - 10f;

				tooltip.beginTable(Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
					20f, true, true, 
					new Object [] {
						"Missile", sizeW, 
						"OP / volley", row2W,
						"Missiles / fighter", row3W});

				for (SalvoMissileData curr : weapons) {
					if (curr.OPPerMissile < maxCost)
					tooltip.addRow(Alignment.MID, Misc.getTextColor(), curr.name,
						Alignment.MID, Misc.getTextColor(), "" + (int) curr.OPPerMissile,
						Alignment.MID, 
						curr.missilesFired == curr.burstSizeBase * espc_SalvoStats.BURSTS_PER_FIGHTER ? 
							Misc.getNegativeHighlightColor() : Misc.getTextColor(), 
						"" + curr.missilesFired
					);
				}
				
				tooltip.addTable("", 0, opad);
				tooltip.addSpacer(5f);
			}
			if (onlyFragmentMissiles || onlyIneligibleMissiles) {
				tooltip.addPara("The ship has %s, and will not fire them when the system is used.", 
					opad, Misc.getNegativeHighlightColor(), 
					"no eligible missiles mounted");
			}
		}
		
		tooltip.addSectionHeading("Missile cost calculations", Alignment.MID, opad);
		tooltip.addPara("Missile cost is based on base maximum ammo divided by base OP cost. "
				+ "Fighters are limited to %s bursts each.", opad, h,
				"" + espc_SalvoStats.BURSTS_PER_FIGHTER);
		
		if (Global.CODEX_TOOLTIP_MODE) {
			tooltip.addSpacer(5f);
			return;
		}
		
		if (isForModSpec || (ship == null && !Global.CODEX_TOOLTIP_MODE)) return;
		
		tooltip.setBgAlpha(0.9f);
		
		if (!hasLimitedMissiles && !hasReloadingMissiles || onlyFragmentMissiles)
			return;
		
		/*
		Collections.sort(weapons, new Comparator<SalvoMissileData>() {
			public int compare(SalvoMissileData o1, SalvoMissileData o2) {
				if (o1.isLimited != o2.isLimited)
					return o1.isLimited ? -1 : 1;
				if (!o1.size.equals(o2.size))
					return o1.size.compareTo(o2.size);
				if (o1.OPPerMissile != o2.OPPerMissile)
					return (int) Math.signum(o2.OPPerMissile - o1.OPPerMissile);
				if (o1.missilesFired != o2.missilesFired)
					return (int) Math.signum(o2.OPPerMissile - o1.OPPerMissile);
				return o1.name.compareTo(o2.name);
			}
		});
		*/

		float row2W = 70f;
		float row3W = 85f;
		float row4W = 100f;
		float sizeW = width - row2W - row3W - row4W - 10f;
		
		if (hasLimitedMissiles) {
			tooltip.beginTable(Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
					20f, true, true, 
					new Object [] {
						"Missile", sizeW, 
						"OP Cost", row2W, 
						"Max ammo", row3W,
						"OP / burst", row4W});
			
			for (SalvoMissileData curr : weapons) {
				if (!curr.isLimited)
					continue;
				boolean isEligible = curr.OPPerMissile <= maxCost;
				tooltip.addRow(Alignment.MID, Misc.getTextColor(), curr.name,
					Alignment.MID, Misc.getTextColor(), "" + curr.OPCost,
					Alignment.MID, Misc.getTextColor(), "" + (int) curr.maxAmmoOrSustainedRate,
					Alignment.MID, 
						curr.OPPerMissile == 1 && curr.missilesFired > curr.burstSizeBase ?
							Misc.getGrayColor() : (isEligible ? Misc.getTextColor() : Misc.getNegativeHighlightColor()),
						curr.OPPerMissile == 1 && curr.missilesFired > curr.burstSizeBase ?
							"<1" : "" + curr.OPPerMissile);
			}
			tooltip.addTable("", 0, opad);
		}
		
		if (hasReloadingMissiles) {
			tooltip.addPara("Reloading missiles and missiles that do not use ammo factor in their "
					+ "%s instead.", opad, h,
					"sustained fire rate");
			
			row3W = 100f;
			sizeW = width - row2W - row3W - row4W - 10f;

			tooltip.beginTable(Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
					20f, true, true, 
					new Object [] {
						"Missile", sizeW, 
						"OP Cost", row2W, 
						"volleys / min", row3W,
						"OP / burst", row4W});
			
			for (SalvoMissileData curr : weapons) {
				if (curr.isLimited)
					continue;
				boolean isEligible = curr.OPPerMissile <= maxCost;
				tooltip.addRow(Alignment.MID, Misc.getTextColor(), curr.name,
					Alignment.MID, Misc.getTextColor(), "" + curr.OPCost,
					Alignment.MID, Misc.getTextColor(), String.format("%.1f", curr.maxAmmoOrSustainedRate),
					Alignment.MID, 
						curr.OPPerMissile == 1 && curr.missilesFired > curr.burstSizeBase ?
							Misc.getGrayColor() : (isEligible ? Misc.getTextColor() : Misc.getNegativeHighlightColor()),
						curr.OPPerMissile == 1 && curr.missilesFired > curr.burstSizeBase ?
							"<1" : "" + curr.OPPerMissile);
			}
			tooltip.addTable("", 0, opad);
		}
		
	}
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		
	}
	
	@Override
	public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}
	
	
}