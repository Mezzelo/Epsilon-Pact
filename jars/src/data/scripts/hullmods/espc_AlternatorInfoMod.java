package data.scripts.hullmods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class espc_AlternatorInfoMod extends BaseHullMod {

	public static class AlternatorWeaponData {
		public boolean isBallistic;
		public String id;
		public String name;
		public WeaponSize size;
		public float dps;
		public int count = 1;
		public int isFragOrPD;
		public AlternatorWeaponData(String id, String name, WeaponSize size, boolean isBallistic, float dps,
			int isFragOrPD) {
			this.id = id;
			this.name = name;
			this.size = size;
			this.isBallistic = isBallistic;
			this.dps = dps;
			this.isFragOrPD = isFragOrPD;
		}
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, final ShipAPI ship, float width, boolean isForModSpec) {
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		
		tooltip.addPara("The ship's grid is engineered to facilitate an increased fire rate of %s and %s weapons, "
				+ "but only either weapon type can be active at a time.", opad, h, 
				"ballistic", "energy");
		tooltip.addPara("The increase in fire rate is %s. "
				+ "Sustained beam weapons contribute to this calculation, but gain no bonuses themselves.", opad,
				h, "based on the sustained DPS of the inactive weapon type");

		float row2W = 100f;
		float row3W = 85f;
		float sizeW = width - row2W - row3W - 10f;
		
		float ballisticDPS = 0f;
		float energyDPS = 0f;
		List<AlternatorWeaponData> weapons = null;
		
		if (!isForModSpec && ship != null && !Global.CODEX_TOOLTIP_MODE) {
			weapons = new ArrayList<AlternatorWeaponData>();
			for (WeaponAPI w : ship.getAllWeapons()) {
				if (w.isDecorative() || w.getSlot() != null && w.getSlot().isSystemSlot() || 
						!w.getType().equals(WeaponType.BALLISTIC) &&
						!w.getType().equals(WeaponType.ENERGY))
					continue;
				boolean found = false;
				for (int i = 0; i < weapons.size(); i++) {
					if (weapons.get(i).id.equals(w.getId())) {
						weapons.get(i).count++;
						if (weapons.get(i).isBallistic)
							ballisticDPS += weapons.get(i).dps;
						else
							energyDPS += weapons.get(i).dps;
						found = true;
						break;
					}
				}
				if (!found) {
					float dps = w.getDerivedStats().getSustainedDps() 
						* (w.getDamageType().equals(DamageType.FRAGMENTATION) ? 0.5f : 1f) *
						(w.hasAIHint(AIHints.PD) && !w.hasAIHint(AIHints.PD_ALSO) ? 0.5f : 1f);
					if (w.getType().equals(WeaponType.BALLISTIC))
						ballisticDPS += dps;
					else
						energyDPS += dps;
					weapons.add(new AlternatorWeaponData(
						w.getId(),
						w.getDisplayName(),
						w.getSize(),
						w.getType().equals(WeaponType.BALLISTIC),
						dps,
						(w.getDamageType().equals(DamageType.FRAGMENTATION) ? 1 : 0) +
							((w.hasAIHint(AIHints.PD) && !w.hasAIHint(AIHints.PD_ALSO)) ? 1 : 0)
					));
				}
			}
			if (ballisticDPS + energyDPS == 0f) {
				
			} else if (ballisticDPS == 0f || energyDPS == 0f) {
				tooltip.addPara("As %s, the ship system will have no effect.", 
					opad, Misc.getNegativeHighlightColor(), 
					"no " + (ballisticDPS > 0f ? "energy" : "ballistic") + " weapons are currently mounted");
			} else {
				Collections.sort(weapons, new Comparator<AlternatorWeaponData>() {
					public int compare(AlternatorWeaponData o1, AlternatorWeaponData o2) {
						if (o1.isBallistic != o2.isBallistic)
							return o1.isBallistic ? -1 : 1;
						if (!o1.size.equals(o2.size))
							return o1.size.compareTo(o2.size);
						return (int) Math.signum(o1.dps * o1.count - o2.dps * o2.count);
					}
				});

				tooltip.beginTable(Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
					20f, true, true, 
					new Object [] {"Weapon type", sizeW, "Total DPS", row2W, "Increase", row3W});

				tooltip.addRow(Alignment.MID, Misc.getTextColor(), "Ballistic",
					Alignment.MID, Misc.getTextColor(), "" + (int) ballisticDPS,
					Alignment.MID, energyDPS / ballisticDPS >= 1.5f ? Misc.getHighlightColor() : 
						(energyDPS / ballisticDPS <= 0.75f ? Misc.getGrayColor() : Misc.getTextColor()), 
						"" + (int) Math.min(energyDPS / ballisticDPS * 100f, 200f) + "%"
				);
				tooltip.addRow(Alignment.MID, Misc.getTextColor(), "Energy",
					Alignment.MID, Misc.getTextColor(), "" + (int) energyDPS,
					Alignment.MID, ballisticDPS / energyDPS >= 1.5f ? Misc.getHighlightColor() : 
						(ballisticDPS / energyDPS <= 0.75f ? Misc.getGrayColor() : Misc.getTextColor()), 
						"" + (int) Math.min(ballisticDPS / energyDPS * 100f, 200f) + "%"
				);
				
				tooltip.addTable("", 0, opad);
				tooltip.addSpacer(5f);
			}
		}
		
		tooltip.addSectionHeading("Fire rate increase calculation", Alignment.MID, opad);
		tooltip.addPara("Fragmentation and PD weapons count for %s DPS. "
				+ "Weapons that are both count for %s DPS.", opad, h, 
				"50%", "25%");
		tooltip.addPara("Fire rate increase for both types is %s.", opad, h, 
				"limited to 200%");
		
		if (Global.CODEX_TOOLTIP_MODE) {
			tooltip.addSpacer(5f);
			return;
		}
		
		if (isForModSpec || (ship == null && !Global.CODEX_TOOLTIP_MODE)) return;
		
		tooltip.setBgAlpha(0.9f);
		
		if (ballisticDPS == 0f || energyDPS == 0f)
			return;
		
		tooltip.beginTable(Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
			20f, true, true, 
			new Object [] {"Weapon", sizeW, "Total DPS", row2W, "Count", row3W});
		
		for (AlternatorWeaponData curr : weapons) {
			tooltip.addRow(Alignment.MID, Misc.getTextColor(), curr.name,
				Alignment.MID, curr.isFragOrPD == 2 ? Misc.getGrayColor() : 
					(curr.isFragOrPD == 1 ? Misc.getNegativeHighlightColor() : Misc.getTextColor()), 
					"" + (int) (curr.dps * curr.count),
				Alignment.MID, Misc.getTextColor(), "" + curr.count);
		}
		tooltip.addTable("", 0, opad);
	}
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		
	}
	
	@Override
	public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}
	
	
}