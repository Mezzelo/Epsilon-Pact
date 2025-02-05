package data.scripts.shipsystems;

import java.awt.Color;
import java.util.Iterator;
import java.util.LinkedList;

import org.lazywizard.lazylib.MathUtils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class espc_ShieldwallStats extends BaseShipSystemScript {

	// addditive bonus
	private static final float ARC_BONUS = 0.1f;
	// ditto self explanatory
	private static final float SHIELD_EFFICIENCY_BONUS = 0.1f;
	
	private static final float BONUS_MAX = 0.5f;
	private static final float EFFECT_RADIUS = 1250f;
	
	private float shieldSizeBonusMult, shieldEfficiencyBonusMult;
	
	// private HashMap<String, ShipAPI> shipTargs = new HashMap<String, ShipAPI>();
	// private HashMap<String, ShipAPI> recedeTargs = new HashMap<String, ShipAPI>();
	
	private LinkedList<ShipAPI> shipTargs;
	
	private int stateLast = 0;
	
	/*private void modifyTargStats(ShipAPI shipTarg, String id, float effectLevel) {
		
	}*/
	
	private void setBonuses(String id, float effectLevel, boolean doJitter) {
		Iterator<ShipAPI> targIterator = shipTargs.iterator();
		while (targIterator.hasNext()) {
			ShipAPI currShip = (ShipAPI) targIterator.next();
			if (currShip.getShield() == null || currShip.getShield().getType() == ShieldType.PHASE)
				continue;
			float desiredArc;
			if (effectLevel > 0f) {
				currShip.getMutableStats().getShieldArcBonus().modifyMult(id, 1f + shieldSizeBonusMult * effectLevel);
				currShip.getMutableStats().getShieldDamageTakenMult().modifyMult(id, 1f - shieldEfficiencyBonusMult * effectLevel);
			} else {
				currShip.getMutableStats().getShieldArcBonus().unmodify(id);
				currShip.getMutableStats().getShieldDamageTakenMult().unmodify(id);
			}
			desiredArc = Math.min(360f,
            	currShip.getHullSpec().getShieldSpec().getArc() *
	            currShip.getMutableStats().getShieldArcBonus().getBonusMult() +
	            currShip.getHullSpec().getShieldSpec().getArc() *
	            	(currShip.getMutableStats().getShieldArcBonus().getPercentMod()) / 100f +
	            currShip.getMutableStats().getShieldArcBonus().getFlatBonus()
	            );
            currShip.getShield().setArc(desiredArc);
            
            if (doJitter)
            	currShip.setJitter(this, new Color(50, 50, 255, 100), 1f - effectLevel, 2, (1f - effectLevel) * 15f);
            
            if (currShip.getShield().getActiveArc() > desiredArc)
            	currShip.getShield().setActiveArc(desiredArc);
		}
		if (effectLevel <= 0f) {
			shipTargs = null;
			stateLast = 0;
		}
	}

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        if (stats.getEntity() == null)
            return;
		
		ShipAPI ship = (ShipAPI) stats.getEntity();
		CombatEngineAPI combatEngine = Global.getCombatEngine();
		
		if (state == State.IN || state == State.ACTIVE) {
			if (stateLast == 0) {
				stateLast = 1;
				// doesn't affect fighters: too easy to stack, too trivial/obtuse to do one-way, and a pain in the ass.
				shipTargs = new LinkedList<ShipAPI>();
				
				Iterator<ShipAPI> shipSearch = combatEngine.getShips().iterator();
				ShipAPI currShip;
				
				while (shipSearch.hasNext()) {
					currShip = (ShipAPI) shipSearch.next();
					if (!currShip.isShuttlePod() && !currShip.isHulk() && !currShip.isFighter() &&
					MathUtils.isWithinRange(currShip, ship.getLocation(), EFFECT_RADIUS) && currShip.getOwner() == ship.getOwner()) {
						shipTargs.addLast(currShip);
					}
				}
				
				// effect is contingent on number of ships affected, so we'll have to iterate twice over.
				shieldSizeBonusMult = Math.min(ARC_BONUS * shipTargs.size(), BONUS_MAX);
				shieldEfficiencyBonusMult = Math.min(SHIELD_EFFICIENCY_BONUS * shipTargs.size(), BONUS_MAX);
			}
			if (state == State.IN)
				setBonuses(id, effectLevel, true);
			else if (state == State.ACTIVE && stateLast == 1) {
				stateLast = 2;
				setBonuses(id, 1f, false);
			}
			
		} else if (state == State.OUT) {
			setBonuses(id, effectLevel, false);
		}
		
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		if (stateLast != 0)
			setBonuses(id, 0f, false);
	}

	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0)
			return new StatusData("nearby friendly shield size +" + (int) (shieldSizeBonusMult * effectLevel * 100f) + "%", false);
		else if (index == 1)
			return new StatusData("nearby friendly shield damage taken -" +
				(int) (shieldEfficiencyBonusMult * effectLevel * 100f) + "%", false);
		return null;
	}
}
