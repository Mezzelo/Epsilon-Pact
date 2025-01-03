package data.scripts.shipsystems.ai;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.util.IntervalUtil;

import data.scripts.shipsystems.espc_ResonatorShieldStats;

public class espc_AlternatorAI implements ShipSystemAIScript {
    
    private static final float THINK_INTERVAL_NORMAL = 0.35f;

    private IntervalUtil aiInterval = new IntervalUtil(THINK_INTERVAL_NORMAL, THINK_INTERVAL_NORMAL + 0.05f);

    private espc_ResonatorShieldStats systemScript = null;
    private ShipSystemAPI system;
    private ShipAPI ship;
    private FluxTrackerAPI flux;
    private ShipwideAIFlags flags;
    
    private float ballisticRangeAvg = 0f;
    private float energyRangeAvg = 0f;
    
    private int ballisticPDWeight = 0;
    private int energyPDWeight = 0;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
        this.system = system;
        this.flux = ship.getFluxTracker();
        
        int ballisticWeps = 0;
        int energyWeps = 0;
        for (WeaponAPI weapon : ship.getAllWeapons()) {
        	if (weapon.getType() != WeaponType.BALLISTIC || weapon.getType() != WeaponType.ENERGY)
        		continue;
        	if (weapon.hasAIHint(AIHints.PD) || weapon.hasAIHint(AIHints.PD_ALSO) || weapon.hasAIHint(AIHints.PD_ONLY)) {
        		if (weapon.getType() == WeaponType.BALLISTIC) {
        			if (weapon.getSize() == WeaponSize.LARGE)
        				ballisticPDWeight += 5;
        			else if (weapon.getSize() == WeaponSize.MEDIUM)
        				ballisticPDWeight += 3;
        			else
        				ballisticPDWeight ++;
        		}
        		continue;
        	}
			// if (weapon.getType() == WeaponType.BALLISTIC)
			// 	ballisticDPS += weapon.getDerivedStats().getSustainedDps();
			// else if (weapon.getType() == WeaponType.ENERGY)
			//	energyDPS += weapon.getDerivedStats().getSustainedDps();
			
		}
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
    	if (amount <= 0f)
    		return;
    	
    	if (systemScript == null) {
    		return;
    	}
    	if (flux.isOverloadedOrVenting())
    		return;
    	
        aiInterval.advance(amount);

        //Wait for interval elapse before doing a check
        if (aiInterval.intervalElapsed()) {
        }
    }
}
