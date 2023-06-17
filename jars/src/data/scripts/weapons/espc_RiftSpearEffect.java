package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponEffectPluginWithInit;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import data.scripts.plugin.espc_DamageListener;
import org.magiclib.util.MagicRender;
import java.awt.Color;
import data.scripts.util.MezzUtils;

public class espc_RiftSpearEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit {

	private ShipAPI ship;
	private IntervalUtil innerInterval;
	
	@Override
	public void init(WeaponAPI weapon) {
		if (!weapon.getShip().hasListenerOfClass(espc_DamageListener.class)){
			weapon.getShip().addListener(new espc_DamageListener());
		}
		innerInterval = new IntervalUtil(0.02f, 0.07f);
		ship = weapon.getShip();
	}
	
    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
		// fluxRemaining.add(weapon.getFluxCostToFire() * thisFluxPercent);
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon == null || engine.isPaused() || amount <= 0f)
			return;
		
		if (weapon.getChargeLevel() > 0f && weapon.getCooldownRemaining() <= 0f) {
			
			if (weapon.getChargeLevel() <= 0.85f) {
				
				innerInterval.advance(amount);
				if (innerInterval.intervalElapsed()) {
					float sparkHeight = MezzUtils.halfSineOut(Math.min(weapon.getChargeLevel() * 2f, 1f));
					Vector2f sparkOffset = new Vector2f((Misc.random.nextFloat() * 4f - 2f) * sparkHeight, (Misc.random.nextFloat() * 4f - 2f) * sparkHeight);
					float sparkAng = (VectorUtils.getAngle(Misc.ZERO, sparkOffset) + 90f) % 360f;
					MagicRender.battlespace(
						Global.getSettings().getSprite("fx", "espc_spark"),
						Vector2f.add(weapon.getFirePoint(0), sparkOffset, new Vector2f()),
						ship.getVelocity(),
						new Vector2f(2f + 2f * sparkHeight, 10f * sparkHeight),
						new Vector2f(-10f, 20f * sparkHeight),
						sparkAng,
						0f,
						new Color(255, 213, 158),
						true,
						0.03f,
						0.1f,
						0.04f
					);
				}
				/*
				MagicRender.singleframe(
					Global.getSettings().getSprite("systemMap", "radar_entity"),
					weapon.getFirePoint(0),
					new Vector2f(0f + (MezzUtils.halfSineOut(Math.min(weapon.getChargeLevel() * 1.3f, 1f)) * 15f) * 2f, 
						0f + (MezzUtils.halfSineOut(Math.min(weapon.getChargeLevel() * 1.3f, 1f)) * 15f) * 2f),
					weapon.getCurrAngle(),
					new Color(255, 222, 166, 200),
					true
				);
				*/
				MagicRender.singleframe(
					Global.getSettings().getSprite("systemMap", "radar_entity"),
					weapon.getFirePoint(0),
					new Vector2f(0f + (MezzUtils.halfSineOut(Math.min(weapon.getChargeLevel() * 1.3f, 1f)) * 15f) * 3.5f, 
						0f + (MezzUtils.halfSineOut(Math.min(weapon.getChargeLevel() * 1.3f, 1f)) * 15f) * 3.5f),
					weapon.getCurrAngle(),
					new Color(93, 0, 255, 100),
					true
				);/*
				MagicRender.singleframe(
					Global.getSettings().getSprite("fx", "espc_riftcharge1"),
					weapon.getFirePoint(0),
					new Vector2f(0f + MezzUtils.halfSineOut(Math.min(weapon.getChargeLevel() * 1.3f, 1f)) * 15f, 
						0f + MezzUtils.halfSineOut(Math.min(weapon.getChargeLevel() * 1.3f, 1f)) * 15f),
					weapon.getCurrAngle(),
					new Color(153,50,255,200),
					true
				);*/
			} else {
				/*
				MagicRender.singleframe(
					Global.getSettings().getSprite("systemMap", "radar_entity"),
					weapon.getFirePoint(0),
					new Vector2f(15f * 1f - MezzUtils.halfSineIn((weapon.getChargeLevel() - 0.85f) / 0.15f) * 10f * 2f, 
						15f * 1f - MezzUtils.halfSineIn((weapon.getChargeLevel() - 0.85f) / 0.15f) * 10f * 2f),
					weapon.getCurrAngle(),
					new Color(255, 222, 166, 200),
					true
				);*/
				MagicRender.singleframe(
					Global.getSettings().getSprite("systemMap", "radar_entity"),
					weapon.getFirePoint(0),
					new Vector2f(15f * 1f - MezzUtils.halfSineIn((weapon.getChargeLevel() - 0.85f) / 0.15f) * 10f * 3.5f, 
						15f * 1f - MezzUtils.halfSineIn((weapon.getChargeLevel() - 0.85f) / 0.15f) * 10f * 3.5f),
					weapon.getCurrAngle(),
					new Color(93, 0, 255, 100),
					true
				);/*
				MagicRender.singleframe(
					Global.getSettings().getSprite("fx", "espc_riftcharge1"),
					weapon.getFirePoint(0),
					new Vector2f(15f - MezzUtils.halfSineIn((weapon.getChargeLevel() - 0.85f) / 0.15f) * 10f, 
						15f - MezzUtils.halfSineIn((weapon.getChargeLevel() - 0.85f) / 0.15f) * 10f),
					weapon.getCurrAngle(),
					new Color(153,50,255,200),
					true
				);*/
			}
			if (weapon.getChargeLevel() < 0.7f) {
				Global.getSoundPlayer().playLoop(
					"espc_riftchargeloop",
					weapon,
					1.3f - weapon.getChargeLevel() / 0.7f * 0.3f,
					weapon.getChargeLevel() * 0.45f / 0.7f,
					weapon.getLocation(),
					weapon.getShip().getVelocity()
				);
			} else if (weapon.getChargeLevel() < 0.85f) {
				Global.getSoundPlayer().playLoop(
					"espc_riftchargeloop",
					weapon,
					1.0f,
					0.45f - (weapon.getChargeLevel() - 0.7f) / 0.15f * 0.45f,
					weapon.getLocation(),
					weapon.getShip().getVelocity()
				);
			}
			if (weapon.getChargeLevel() > 0.4f) {
				Global.getSoundPlayer().playLoop(
					"espc_riftchargesecondary",
					weapon,
					0.9f,
					(weapon.getChargeLevel() - 0.4f) / 0.6f * 0.7f,
					weapon.getLocation(),
					weapon.getShip().getVelocity()
				);
			}
		}
    }
}