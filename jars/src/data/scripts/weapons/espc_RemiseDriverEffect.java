package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponEffectPluginWithInit;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import java.util.Iterator;

import org.lazywizard.lazylib.MathUtils;

public class espc_RemiseDriverEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit {
	
	private static final float MAX_VIS_RECOIL = 7f;
	private static final float VIS_RECOIL_PER_SHOT = 2f;
	private static final float VIS_RECOIL_DECAY = 4.5f;
	private static final float PENALTY_PER_MORTAR = 0.25f;
	
    private SpriteAPI barrel;
	private float barrelBaseY = 0f;
	private float currRecoil = 0f;
	private float dmgMult = -1f;
	
	@Override
	public void init(WeaponAPI weapon) {
		barrel = weapon.getBarrelSpriteAPI();
		if (barrel != null)
			barrelBaseY = barrel.getCenterY();
		// weapon.getSlot().isTurret() - potential offset required based on hardpoint
	}

	private void initWithWeapons(WeaponAPI weapon) {
		dmgMult = 1f;
		if (weapon.getShip() == null)
			return;
		int numMortars = 0;
		Iterator<WeaponAPI> weaponIterator = weapon.getShip().getAllWeapons().iterator();
		while (weaponIterator.hasNext()) {
			WeaponAPI currWeapon = (WeaponAPI) weaponIterator.next();
			if (currWeapon.getId().equals("espc_remmortar"))
				numMortars++;
		}
		if (numMortars < 2)
			return;
		numMortars--;
		dmgMult = (float) Math.pow(1f - PENALTY_PER_MORTAR, numMortars);
		
	}
	
	
    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
		currRecoil = Math.min(currRecoil + VIS_RECOIL_PER_SHOT, MAX_VIS_RECOIL);
		
		if (dmgMult < 0f)
			initWithWeapons(weapon);
		proj.setDamageAmount(
			proj.getProjectileSpec().getDamage().getDamage() * dmgMult
		);
		
        for (int i = 0; i < 8; i++) {
        	engine.addHitParticle(
        		MathUtils.getRandomPointInCone(
        			proj.getLocation(),
        			4f * i + 10f,
        			proj.getFacing() - 13f,
        			proj.getFacing() + 13f
        		),
				proj.getSource().getVelocity(),
				Misc.random.nextFloat() * 5f + 25f - i / 2f,
				1,
				0.2f + 0.02f * i,
				new Color(200, 235, 255, 80)
        	);
        }
    	
		Global.getSoundPlayer().playSound(
			"espc_remdriver_fire",
			1f,
			1f,
			proj.getLocation(),
			proj.getWeapon().getShip().getVelocity()
		);
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon == null || engine.isPaused() || amount <= 0f) return;
		
		if (barrel != null) {
			currRecoil = Math.max(
				0f, 
				currRecoil - amount * VIS_RECOIL_DECAY * (currRecoil > MAX_VIS_RECOIL - VIS_RECOIL_PER_SHOT ? 2f : 1f)
			);
	        barrel.setCenterY(barrelBaseY + currRecoil);
		}
    }
}