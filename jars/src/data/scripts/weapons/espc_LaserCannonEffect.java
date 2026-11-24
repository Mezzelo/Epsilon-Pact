package data.scripts.weapons;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatAsteroidAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponEffectPluginWithInit;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.shipsystems.espc_CollapseStats;

import org.magiclib.util.MagicFakeBeam;
import data.scripts.weapons.proj.espc_LaserCannonRenderPlugin;

import com.fs.starfarer.api.combat.ShipAPI;

import java.util.ArrayDeque;
import java.util.Iterator;

import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.FastTrig;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.MissileAPI;
/*
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.BattleObjectiveAPI;
import org.lazywizard.lazylib.combat.CombatUtils;
*/

public class espc_LaserCannonEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit{
	
	private ShipAPI ship;
	
	private ArrayDeque<DamagingProjectileAPI> projs = null;
	private WeaponAPI thisWep;

	espc_LaserCannonRenderPlugin plugin = null;
	
	public espc_LaserCannonRenderPlugin getPlugin() {
		return plugin;
	}
	
	public WeaponAPI getWeapon() {
		return thisWep;
	}
    
	// TODO: among other things, make sure you add a damage listener
	@Override
	public void init(WeaponAPI weapon) {
		thisWep = weapon;
		ship = weapon.getShip();
		if (ship == null)
			return;
		
		projs = new ArrayDeque<DamagingProjectileAPI>();
		
	}

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (Global.getCurrentState() != GameState.COMBAT ||
        	weapon == null || engine.isPaused() || amount <= 0f) return;
    }

    
    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
    }
}