package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponEffectPluginWithInit;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.util.IntervalUtil;

import data.scripts.plugin.espc_DamageListener;
import org.magiclib.util.MagicRender;
import java.awt.Color;
import data.scripts.util.MezzUtils;
import com.fs.starfarer.api.util.Misc;

import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;

public class espc_RiftPikeEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit {

	private ShipAPI ship;
	private IntervalUtil sparkInterval, innerInterval;
	private float chargeJostleX = 0f;
	// private Vector2f barrelOffset;
	
	@Override
	public void init(WeaponAPI weapon) {
		if (!weapon.getShip().hasListenerOfClass(espc_DamageListener.class)){
			weapon.getShip().addListener(new espc_DamageListener());
		}
		sparkInterval = new IntervalUtil(0.6f, 1.0f);
		innerInterval = new IntervalUtil(0.02f, 0.07f);
		ship = weapon.getShip();
		// barrelOffset = Vector2f.sub(weapon.getFirePoint(0), ship.getLocation(), new Vector2f());
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
			
			if (chargeJostleX > 0f)
				chargeJostleX = Math.max(0f, chargeJostleX - amount);
			
			sparkInterval.advance(amount);
			if (sparkInterval.intervalElapsed()) {
				Global.getSoundPlayer().playSound(
					"espc_spark",
					0.8f + Misc.random.nextFloat() * 0.4f + weapon.getChargeLevel() * 0.2f,
					weapon.getChargeLevel() * 0.6f + 0.3f,
					weapon.getLocation(),
					weapon.getShip().getVelocity()
				);
				sparkInterval.setInterval(0.6f - 0.4f * weapon.getChargeLevel(), 1.0f - 0.65f * weapon.getChargeLevel());
				Vector2f sparkOffset = new Vector2f(Misc.random.nextFloat() * 40f - 20f, Misc.random.nextFloat() * 40f - 20f);
				float sparkAng = VectorUtils.getAngle(Misc.ZERO, sparkOffset);
				MagicRender.battlespace(
						Global.getSettings().getSprite("fx", "espc_spark"),
						Vector2f.add(weapon.getFirePoint(0), sparkOffset, new Vector2f()),
						ship.getVelocity(),
						new Vector2f(10f, 20f),
						new Vector2f(-30f, 100f),
						(sparkAng + 90f + Misc.random.nextFloat() * 70f - 35f) % 360f,
						0f,
						new Color(235, 155, 255),
						true,
						0.03f,
						0.1f,
						0.04f
				);
				// if (Misc.random.nextFloat() > 0.5f)
					chargeJostleX += Misc.random.nextFloat() * 0.25f + 0.2f;
				// else
				// 	chargeJostleY += Misc.random.nextFloat() * 0.15f + 0.1f;
			}
			
			if (weapon.getChargeLevel() <= 0.85f) {
				
				if (weapon.getChargeLevel() < 0.7f) {
					innerInterval.advance(amount);
					if (innerInterval.intervalElapsed()) {
						float sparkHeight = MezzUtils.halfSineOut(Math.min(weapon.getChargeLevel() * 2f, 1f));
						Vector2f sparkOffset = new Vector2f((Misc.random.nextFloat() * 20f - 10f) * sparkHeight, (Misc.random.nextFloat() * 20f - 10f) * sparkHeight);
						float sparkAng = (VectorUtils.getAngle(Misc.ZERO, sparkOffset) + 90f) % 360f;
						MagicRender.battlespace(
							Global.getSettings().getSprite("fx", "espc_spark"),
							Vector2f.add(weapon.getFirePoint(0), sparkOffset, new Vector2f()),
							ship.getVelocity(),
							new Vector2f(3f + 3.5f * sparkHeight, 12f * sparkHeight),
							new Vector2f(-20f, 40f * sparkHeight),
							sparkAng,
							0f,
							new Color(255, 213, 158),
							true,
							0.03f,
							0.1f,
							0.04f
						);
					}
				}
				
				MagicRender.singleframe(
					Global.getSettings().getSprite("systemMap", "radar_entity"),
					weapon.getFirePoint(0),
					new Vector2f(0f + (MezzUtils.halfSineOut(Math.min(weapon.getChargeLevel() * 1.3f, 1f)) * 35f * (1f + chargeJostleX * 2f)) * 1.3f, 
						0f + (MezzUtils.halfSineOut(Math.min(weapon.getChargeLevel() * 1.3f, 1f)) * 25f * (1f + chargeJostleX * 2f)) * 1.3f),
					weapon.getCurrAngle(),
					new Color(255, 222, 166, 200),
					true
				);
				MagicRender.singleframe(
					Global.getSettings().getSprite("systemMap", "radar_entity"),
					weapon.getFirePoint(0),
					new Vector2f(0f + MezzUtils.halfSineOut(Math.min(weapon.getChargeLevel() * 1.3f, 1f)) * 25f * 4.5f * (1f + chargeJostleX * 2f), 
						0f + MezzUtils.halfSineOut(Math.min(weapon.getChargeLevel() * 1.3f, 1f)) * 25f * 4.5f * (1f + chargeJostleX * 2f)),
					weapon.getCurrAngle(),
					new Color(93, 0, 255, 70 + (int) chargeJostleX * 180),
					true
				);/*
				MagicRender.singleframe(
					Global.getSettings().getSprite("fx", "espc_riftcharge1"),
					weapon.getFirePoint(0),
					new Vector2f(0f + MezzUtils.halfSineOut(Math.min(weapon.getChargeLevel() * 1.3f, 1f)) * 25f * (1f + chargeJostleX * 2f) / (1f + chargeJostleY * 2f), 
						0f + MezzUtils.halfSineOut(Math.min(weapon.getChargeLevel() * 1.3f, 1f)) * 25f * (1f + chargeJostleY * 2f) / (1f + chargeJostleX * 2f)),
					weapon.getCurrAngle(),
					new Color(153,50,255,200),
					true
				);*/
			} else {
				
				MagicRender.singleframe(
					Global.getSettings().getSprite("systemMap", "radar_entity"),
					weapon.getFirePoint(0),
					new Vector2f(25f * 1.3f - MezzUtils.halfSineIn(Math.min((weapon.getChargeLevel() - 0.85f) / 0.1f, 1f)) * 35f * 1.3f, 
						25f * 1.3f - MezzUtils.halfSineIn(Math.min((weapon.getChargeLevel() - 0.85f) / 0.1f, 1f)) * 15f * 1.3f),
					weapon.getCurrAngle(),
					new Color(255, 222, 166, 200),
					true
				);
				MagicRender.singleframe(
					Global.getSettings().getSprite("systemMap", "radar_entity"),
					weapon.getFirePoint(0),
					new Vector2f(25f * 4.5f - MezzUtils.halfSineIn(Math.min((weapon.getChargeLevel() - 0.85f) / 0.1f, 1f)) * 25f * 4.5f, 
						25f * 4.5f - MezzUtils.halfSineIn(Math.min((weapon.getChargeLevel() - 0.85f) / 0.1f, 1f)) * 15f * 4.5f),
					weapon.getCurrAngle(),
					new Color(93, 0, 255, 70),
					true
				);/*
				MagicRender.singleframe(
					Global.getSettings().getSprite("fx", "espc_riftcharge1"),
					weapon.getFirePoint(0),
					new Vector2f(25f + chargeJostleX * 0f - MezzUtils.halfSineIn(Math.min((weapon.getChargeLevel() - 0.85f) / 0.1f, 1f)) * 15f, 
						25f + chargeJostleY * 0f - MezzUtils.halfSineIn(Math.min((weapon.getChargeLevel() - 0.85f) / 0.1f, 1f)) * 15f),
					weapon.getCurrAngle(),
					new Color(153,50,255,200),
					true
				);*/
			}
			if (weapon.getChargeLevel() < 0.7f) {
				Global.getSoundPlayer().playLoop(
					"espc_riftchargeloop",
					weapon,
					1.0f - weapon.getChargeLevel() / 0.7f * 0.3f,
					weapon.getChargeLevel() / 0.7f * 2.5f,
					weapon.getLocation(),
					weapon.getShip().getVelocity()
				);
			} else if (weapon.getChargeLevel() < 0.85f) {
				Global.getSoundPlayer().playLoop(
					"espc_riftchargeloop",
					weapon,
					0.7f,
					2.5f - (weapon.getChargeLevel() - 0.7f) / 0.15f * 2.5f,
					weapon.getLocation(),
					weapon.getShip().getVelocity()
				);
			}
			if (weapon.getChargeLevel() > 0.5f) {
    			// Global.getLogger(espc_RiftPikeEffect.class).info(Global.getCombatEngine().getViewport().getViewMult());
				if (weapon.getChargeLevel() < 0.9f) {
					float dist = MathUtils.getDistance(weapon.getFirePoint(0), Global.getCombatEngine().getViewport().getCenter());
					// dist = dist * Global.getCombatEngine().getViewport().getViewMult();
					if (dist < 1000f) {
						Global.getSoundPlayer().applyLowPassFilter(
								1f - (weapon.getChargeLevel() - 0.4f) * 0.2f * (1000f - dist)/1000f,
								1f - (weapon.getChargeLevel() - 0.55f) * 2f * (1000f - dist)/1000f
							);
					}
				}
				Global.getSoundPlayer().playLoop(
					"espc_riftchargesecondary",
					weapon,
					0.7f,
					(weapon.getChargeLevel() - 0.5f) / 0.5f * 1.8f,
					weapon.getLocation(),
					weapon.getShip().getVelocity()
				);
			}
		} else if (weapon.getCooldownRemaining() / weapon.getCooldown() > 0.3f) {
			chargeJostleX = 0f;
			sparkInterval.advance(amount);
			if (sparkInterval.intervalElapsed()) {
				Global.getSoundPlayer().playSound(
					"espc_spark",
					0.8f + Misc.random.nextFloat() * 0.4f,
					0.25f,
					weapon.getLocation(),
					weapon.getShip().getVelocity()
				);
				sparkInterval.setInterval(0.8f, 1.3f);
				Vector2f sparkOffset = new Vector2f(Misc.random.nextFloat() * 40f - 20f, Misc.random.nextFloat() * 40f - 20f);
				float sparkAng = VectorUtils.getAngle(Misc.ZERO, sparkOffset);
				MagicRender.battlespace(
						Global.getSettings().getSprite("fx", "espc_spark"),
						Vector2f.add(weapon.getFirePoint(0), sparkOffset, new Vector2f()),
						ship.getVelocity(),
						new Vector2f(10f, 20f),
						new Vector2f(-30f, 100f),
						sparkAng + Misc.random.nextFloat() * 100f - 50f,
						0f,
						new Color(235, 155, 255),
						true,
						0.03f,
						0.1f,
						0.04f
				);
			}
		}
    }
}