package data.scripts.weapons;

import java.util.ArrayList;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponEffectPluginWithInit;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.graphics.SpriteAPI;

public class espc_DecoGlow implements EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit {

	private ShipAPI ship;
	private float currAlpha = 0f;
	private float fluxLast = -1f;
	
	private static final float ALPHA_INCREASE_MULT = 3f;
	private static final float ALPHA_DECAY_RATE = 0.8f;
	private static final float ACTIVE_ALPHA_MIN = 0.35f;

	private float changeMult = ALPHA_INCREASE_MULT;
	
	@Override
	public void init(WeaponAPI weapon) {
		ship = weapon.getShip();
        weapon.getSprite().setAdditiveBlend();
        weapon.getAnimation().setAlphaMult(0f);
        weapon.getAnimation().setFrame(0);
        if (ship.getHullSpec().getBaseHullId().equals("espc_flagbearer"))
        	changeMult *= 4.5f;
	}
	
	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon == null || engine.isPaused() || amount <= 0f || ship == null)
			return;

        if (ship != null && (ship.isHulk() || !ship.isAlive())) {
            weapon.getAnimation().setAlphaMult(0f);
            ship = null;
            return;
        }
        
        if (ship.getSystem().isOn()) {
        	if (fluxLast < 0f) {
        		fluxLast = ship.getFluxLevel();
                weapon.getAnimation().setFrame(1);
        		return;
        	}
        	if (currAlpha < ACTIVE_ALPHA_MIN)
        		currAlpha += amount * ALPHA_DECAY_RATE * 0.3f;
        	
        	if (ship.getFluxLevel() > fluxLast)
        		currAlpha = Math.min(currAlpha + (ship.getFluxLevel() - fluxLast) * changeMult, 1.0f);
        	else if (currAlpha > ACTIVE_ALPHA_MIN)
        		currAlpha = Math.max(currAlpha - amount * ALPHA_DECAY_RATE, ACTIVE_ALPHA_MIN);

            weapon.getAnimation().setAlphaMult(currAlpha);
            fluxLast = ship.getFluxLevel();
        	
        } else if (currAlpha > 0f){
        	currAlpha = Math.max(currAlpha - ALPHA_DECAY_RATE * amount * 0.3f, 0f);
            weapon.getAnimation().setAlphaMult(currAlpha);
            if (currAlpha <= 0f) {
            	fluxLast = -1f;
                weapon.getAnimation().setFrame(0);
            }
        }
        
	}
	

}