package data.scripts.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicTrailPlugin;

import data.scripts.util.MezzUtils;
import data.scripts.weapons.espc_IonFlailEffect;
// import org.lazywizard.lazylib.VectorUtils;
				
public class espc_IonFlailRenderPlugin extends BaseEveryFrameCombatPlugin {
	
	public static final float FLAIL_RADIUS = 30f;
	public static final float FLAIL_DEGS = 680f;

	
	private static final int PARTICLE_COUNT = 128;
	private static final float PARTICLE_DUR = 0.7f;
	private static final float PARTICLE_DUR_MIN = 0.2f;
	private static final float PARTICLE_DIST_MAX = 250f;
	private static final float PARTICLE_DIST_MIN = 50f;
	private static final float DIST_MULT_MIN = 0.6f;
	private static final float DIST_MULT_MAX = 1.0f;
	private static final float PARTICLE_WIDTH_MAX = 3f;
	private static final float PARTICLE_WIDTH_MIN = 1.5f;
	private static final float PARTICLE_DIST_FORWARDS = 50f;
	
	private ArrayDeque<DamagingProjectileAPI> projs;
	private ArrayDeque<espc_IonFlailEffect> weps;
	private ArrayDeque<IonFlailImpact> impacts;
	
    private class IonFlailImpact {
    	private final Vector2f startPos;
    	private final float startAng;
    	private final float effectStart;
    	private final float angSinY;
    	private final float angCosY;
    	private final float distMax;
    	private final float durMax;
    	private final int particleCount;
    	
    	public IonFlailImpact(Vector2f pos, float angle) {
            effectStart = Global.getCombatEngine().getTotalElapsedTime(false);
    		startPos = pos;
    		startAng = angle;
    		angSinY = (float) FastTrig.sin(Math.toRadians(startAng));
    		angCosY = (float) FastTrig.cos(Math.toRadians(startAng));
    		distMax = Misc.random.nextFloat() * (DIST_MULT_MAX - DIST_MULT_MIN) * PARTICLE_DIST_MAX + DIST_MULT_MIN;
    		durMax = PARTICLE_DUR;
    		particleCount = PARTICLE_COUNT;
    		
    	}
    }
	
	public espc_IonFlailRenderPlugin(espc_IonFlailEffect plugin) {
		projs = new ArrayDeque<DamagingProjectileAPI>();
		weps = new ArrayDeque<espc_IonFlailEffect>();
		impacts = new ArrayDeque<IonFlailImpact>();
	}
	
	public void addWeapon(espc_IonFlailEffect wep) {
		weps.add(wep);
	}
	
	public void addProj(DamagingProjectileAPI proj) {
		projs.addLast(proj);
	}
	
	public void addImpact(Vector2f point, float ang) {
		impacts.addLast(new IonFlailImpact(point, ang));
	}
	
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
		if (amount <= 0f)
			return;
    	if (Global.getCombatEngine().isPaused())
    		return;
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine == null)
			return;
		if (projs.size() == 0 && weps.size() == 0 && impacts.size() == 0) {
			engine.removePlugin(this);
			return;
		}
		
		
		if (projs.size() > 0) {
			Iterator<DamagingProjectileAPI> iter = projs.iterator();
			while (iter.hasNext()) {
				DamagingProjectileAPI proj = iter.next();
				if (proj == null || !engine.isEntityInPlay(proj)) {
					iter.remove();
					continue;
				}
				/*
				if (!proj.getCustomData().containsKey("espc_ionflailtrailid"))
					continue;
                MagicTrailPlugin.addTrailMemberAdvanced(
    				proj,
    				(float) proj.getCustomData().get("espc_ionflailtrailid") + 5f,	
    				Global.getSettings().getSprite("fx", "base_trail_mild"),
    				Vector2f.add(proj.getLocation(), 
    					new Vector2f(proj.getVelocity().x * amount, proj.getVelocity().y * amount), 
    					new Vector2f()),
    				0f,
    				0f,
    				VectorUtils.getFacing(proj.getVelocity()),
    				0f,
    				0f,
    				36f * proj.getDamageAmount() / espc_IonFlailEffect.BASE_DAMAGE,
    				8f * proj.getDamageAmount() / espc_IonFlailEffect.BASE_DAMAGE,
                	new Color(255, 255, 255),
                	new Color(60, 210, 255),
                	255f,
    				0.0f,
    				0.06f,
    				0.12f,
    				GL11.GL_SRC_ALPHA,
    				GL11.GL_ONE,
    				128f,
    				64f,
    				((float) proj.getCustomData().get("espc_ionflailtrailid") + 5f) * 2f,
    				Misc.ZERO,
    				null,
                	CombatEngineLayers.ABOVE_SHIPS_LAYER,
    				1f
                );
                MagicTrailPlugin.addTrailMemberAdvanced(
    				proj,
    				(float) proj.getCustomData().get("espc_ionflailtrailid"),	
    				Global.getSettings().getSprite("fx", "base_trail_zap"),
    				Vector2f.add(proj.getLocation(), 
        				new Vector2f(proj.getVelocity().x * amount, proj.getVelocity().y * amount), 
        				new Vector2f()),
    				0f,
    				0f,
    				VectorUtils.getFacing(proj.getVelocity()),
    				0f,
    				0f,
    				48f * proj.getDamageAmount() / espc_IonFlailEffect.BASE_DAMAGE,
    				12f * proj.getDamageAmount() / espc_IonFlailEffect.BASE_DAMAGE,
                	new Color(0, 255, 215),
                	new Color(0, 225, 155),
                	255f,
    				0.0f,
    				0.06f,
    				0.12f,
    				GL11.GL_SRC_ALPHA,
    				GL11.GL_ONE,
    				256f,
    				64f,
    				(float) proj.getCustomData().get("espc_ionflailtrailid") * 2f,
    				Misc.ZERO,
    				null,
                	CombatEngineLayers.ABOVE_SHIPS_LAYER,
    				1f
                );
                */
			}
		}
		if (weps.size() > 0) {
			Iterator<espc_IonFlailEffect> wepIter = weps.iterator();
			while (wepIter.hasNext()) {
				espc_IonFlailEffect wep = wepIter.next();
				if (wep == null || wep.getWeapon() == null || wep.getWeapon().getShip() == null ||
					!engine.isEntityInPlay(wep.getWeapon().getShip())) {
					wepIter.remove();
					continue;
				}
				WeaponAPI weapon = wep.getWeapon();
				if (weapon.getChargeLevel() <= 0f || !weapon.isFiring() || wep.isCd)
					continue;
	            MagicTrailPlugin.addTrailMemberAdvanced(
					null,
					wep.trailId + 5f,	
					Global.getSettings().getSprite("fx", "base_trail_mild"),
					new Vector2f(weapon.getFirePoint(0).x + 
						MezzUtils.halfSineOut(weapon.getChargeLevel()) * FLAIL_RADIUS * (float) FastTrig.cos(
						Math.toRadians(weapon.getCurrAngle() + FLAIL_DEGS * -wep.arcDirection
							* (weapon.getSlot().getLocation().y < 0f ? -1f : 1f) *
						(float) Math.pow(MezzUtils.halfSineIn(weapon.getChargeLevel()), 1.5f))), 
						weapon.getFirePoint(0).y +
						MezzUtils.halfSineOut(weapon.getChargeLevel()) * FLAIL_RADIUS * (float) FastTrig.sin(
						Math.toRadians(weapon.getCurrAngle() + FLAIL_DEGS * -wep.arcDirection
							* (weapon.getSlot().getLocation().y < 0f ? -1f : 1f) *
						(float) Math.pow(MezzUtils.halfSineIn(weapon.getChargeLevel()), 1.5f)))),
					0f,
					0f,
					weapon.getCurrAngle() + FLAIL_DEGS * -wep.arcDirection
						* (weapon.getSlot().getLocation().y < 0f ? -1f : 1f)
						* (float) Math.pow(MezzUtils.halfSineIn(weapon.getChargeLevel()), 1.5f)
						+ 90f * -wep.arcDirection * (weapon.getSlot().getLocation().y < 0f ? -1f : 1f),
					0f,
					0f,
					36f * wep.getWeapon().getChargeLevel(),
					8f * wep.getWeapon().getChargeLevel(),
	            	new Color(255, 255, 255),
	            	new Color(60, 210, 255),
	            	255f,
					0.04f,
					0.0f,
					0.08f,
					GL11.GL_SRC_ALPHA,
					GL11.GL_ONE,
					128f,
					64f,
					(wep.trailId + 5f) * 2f,
					Misc.ZERO,
					null,
	            	CombatEngineLayers.ABOVE_SHIPS_LAYER,
					1f
	            );
	            MagicTrailPlugin.addTrailMemberAdvanced(
					null,
					wep.trailId,	
					Global.getSettings().getSprite("fx", "base_trail_zap"),
					new Vector2f(weapon.getFirePoint(0).x + 
						MezzUtils.halfSineOut(weapon.getChargeLevel()) * FLAIL_RADIUS * (float) FastTrig.cos(
						Math.toRadians(weapon.getCurrAngle() + FLAIL_DEGS * -wep.arcDirection
							* (weapon.getSlot().getLocation().y < 0f ? -1f : 1f) *
						(float) Math.pow(MezzUtils.halfSineIn(weapon.getChargeLevel()), 1.5f))), 
						weapon.getFirePoint(0).y +
						MezzUtils.halfSineOut(weapon.getChargeLevel()) * FLAIL_RADIUS * (float) FastTrig.sin(
						Math.toRadians(weapon.getCurrAngle() + FLAIL_DEGS * -wep.arcDirection
							* (weapon.getSlot().getLocation().y < 0f ? -1f : 1f) *
						(float) Math.pow(MezzUtils.halfSineIn(weapon.getChargeLevel()), 1.5f)))),
					0f,
					0f,
					weapon.getCurrAngle() + FLAIL_DEGS * -wep.arcDirection
						* (weapon.getSlot().getLocation().y < 0f ? -1f : 1f)
						* (float) Math.pow(MezzUtils.halfSineIn(weapon.getChargeLevel()), 1.5f)
						+ 90f * -wep.arcDirection * (weapon.getSlot().getLocation().y < 0f ? -1f : 1f),
					0f,
					0f,
					48f * wep.getWeapon().getChargeLevel(),
					12f * wep.getWeapon().getChargeLevel(),
	            	new Color(0, 255, 215),
	            	new Color(0, 225, 155),
	            	255f,
					0.04f,
					0.0f,
					0.08f,
					GL11.GL_SRC_ALPHA,
					GL11.GL_ONE,
					256f,
					64f,
					wep.trailId * 2f,
					Misc.ZERO,
					null,
	            	CombatEngineLayers.ABOVE_SHIPS_LAYER,
					1f
	            );
			}
		}
		if (impacts.size() == 0)
			return;
		Iterator<IonFlailImpact> impactIter = impacts.iterator();
		while (impactIter.hasNext()) {
			IonFlailImpact impact = impactIter.next();
			if (engine.getTotalElapsedTime(false) > impact.effectStart + impact.durMax)
				impactIter.remove();
		}
	}
	
    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
		
		CombatEngineAPI engine = Global.getCombatEngine();
		float cTime = engine.getTotalElapsedTime(false);
		Random jitterRand = new Random((long) (cTime * 600f));

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
		SpriteAPI sprite = Global.getSettings().getSprite("systemMap", "radar_entity");
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, sprite.getTextureId());
		GL11.glBegin(GL11.GL_QUADS);
		
		if (weps.size() > 0) {
			Iterator<espc_IonFlailEffect> wepIter = weps.iterator();
			while (wepIter.hasNext()) {
				espc_IonFlailEffect plugin = wepIter.next();
				WeaponAPI wep = plugin.getWeapon();
				if (wep.getChargeLevel() > 0f && wep.isFiring() && !plugin.isCd) {
					Vector2f pos = new Vector2f(wep.getFirePoint(0).x + 
						MezzUtils.halfSineOut(wep.getChargeLevel()) * FLAIL_RADIUS * (float) FastTrig.cos(
						Math.toRadians(wep.getCurrAngle() + FLAIL_DEGS * -plugin.arcDirection
							* (wep.getSlot().getLocation().y < 0f ? -1f : 1f) *
						(float) Math.pow(MezzUtils.halfSineIn(wep.getChargeLevel()), 1.5f))), 
						wep.getFirePoint(0).y +
						MezzUtils.halfSineOut(wep.getChargeLevel()) * FLAIL_RADIUS * (float) FastTrig.sin(
						Math.toRadians(wep.getCurrAngle() + FLAIL_DEGS * -plugin.arcDirection
							* (wep.getSlot().getLocation().y < 0f ? -1f : 1f) *
						(float) Math.pow(MezzUtils.halfSineIn(wep.getChargeLevel()), 1.5f))));
					
					GL11.glColor4ub(
						(byte) 255, (byte) 255, (byte) 255, (byte) 255);
					MezzUtils.glSquare(pos, MezzUtils.halfSineOut(wep.getChargeLevel()) *
						(20f * jitterRand.nextFloat() + 4f));
					GL11.glColor4ub(
						(byte) 0, (byte) 255, (byte) 215, (byte) 205);
					MezzUtils.glSquare(pos, MezzUtils.halfSineOut(wep.getChargeLevel()) *
						MezzUtils.halfSineOut(wep.getChargeLevel()) *
						(30f * jitterRand.nextFloat() + 6f));
					GL11.glColor4ub(
						(byte) 0, (byte) 225, (byte) 155, (byte) 85);
					MezzUtils.glSquare(pos, MezzUtils.halfSineOut(wep.getChargeLevel()) *
						MezzUtils.halfSineOut(wep.getChargeLevel()) *
						(48f * jitterRand.nextFloat() + 12f));
				}
	
			}
		}

		Iterator<DamagingProjectileAPI> iter = projs.iterator();
		while (iter.hasNext()) {
			DamagingProjectileAPI proj = iter.next();
			GL11.glColor4ub(
				(byte) 255, (byte) 255, (byte) 255, (byte) 255);
			MezzUtils.glSquare(proj.getLocation().x, proj.getLocation().y, 
				Math.min(proj.getDamageAmount() / espc_IonFlailEffect.BASE_DAMAGE, 1f) * 
				(12f * jitterRand.nextFloat() + 3f));
			GL11.glColor4ub(
				(byte) 0, (byte) 255, (byte) 215, (byte) 205);
			MezzUtils.glSquare(proj.getLocation().x, proj.getLocation().y, 
				Math.min(proj.getDamageAmount() / espc_IonFlailEffect.BASE_DAMAGE, 1f) * 
				(30f * jitterRand.nextFloat() + 6f));
			GL11.glColor4ub(
				(byte) 0, (byte) 225, (byte) 155, (byte) 85);
				MezzUtils.glSquare(proj.getLocation().x, proj.getLocation().y, 
				Math.min(proj.getDamageAmount() / espc_IonFlailEffect.BASE_DAMAGE, 1f) * 
				(60f * jitterRand.nextFloat() + 15f));
			
		}
		
		for (IonFlailImpact impact: impacts) {
			if (!engine.getViewport().isNearViewport(impact.startPos, 500f)) {
				jitterRand.nextFloat();
				jitterRand.nextFloat();
				continue;
			}
			
			Random rand = new Random((long) (impact.effectStart + impact.startPos.x * 10f + impact.startPos.y * 10f));
			
			for (int i = 0; i < impact.particleCount; i ++) {
				float dur = MezzUtils.halfSineIn(rand.nextFloat()) * (impact.durMax - PARTICLE_DUR_MIN) + PARTICLE_DUR_MIN;
				float ang = rand.nextFloat() * 360f;
				float distRand = rand.nextFloat();
				float dist = distRand * (impact.distMax - PARTICLE_DIST_MIN) + PARTICLE_DIST_MIN;
				if (cTime - impact.effectStart > dur)
					continue;
				float randCos = (float) FastTrig.cos((ang + impact.startAng) * MathUtils.FPI / 180f);
				float randSin = (float) FastTrig.sin((ang + impact.startAng) * MathUtils.FPI / 180f);
				float locSin = (float) FastTrig.sin(ang * MathUtils.FPI / 180f);
				float interp = Math.min((cTime - impact.effectStart)/dur, 1f);
				float scale = 1f - 
					Math.min(Math.max((cTime - impact.effectStart - dur / 2f) * 2f / dur, 0f), 1f);
				
				float width = (PARTICLE_WIDTH_MAX - PARTICLE_WIDTH_MIN) * jitterRand.nextFloat() + PARTICLE_WIDTH_MIN;
				float opacity = jitterRand.nextFloat();
				
				float x = impact.startPos.x + 
					MezzUtils.halfSineOut(interp) * randCos * dist * (0.65f + Math.abs(locSin) * 0.35f) +
					MezzUtils.halfSineIn(interp) * impact.angCosY * PARTICLE_DIST_FORWARDS * (distRand * 0.7f + 0.3f);
				float y = impact.startPos.y +
					MezzUtils.halfSineOut(interp) * randSin * dist * (0.65f + Math.abs(locSin) * 0.35f) +
					MezzUtils.halfSineIn(interp) * impact.angSinY * PARTICLE_DIST_FORWARDS * (distRand * 0.7f + 0.3f);
				
				GL11.glColor4ub(
					(byte) 255, (byte) 255, (byte) 255, (byte) 255);
				MezzUtils.glSquare(x, y, scale * width * (opacity * 0.5f + 0.5f));
				MezzUtils.colorHSBLerp4UB(
					new Color(0, 255, 215), 
					new Color(0, 205, 255), 
					(int) (205 * opacity), (int) (205 * opacity), MezzUtils.halfSineOut(interp));
				MezzUtils.glSquare(x, y, scale * width * 2f);
				MezzUtils.colorHSBLerp4UB(
					new Color(0, 225, 155), 
					new Color(0, 165, 255), 
					125, 125, MezzUtils.halfSineOut(interp));
				MezzUtils.glSquare(x, y, scale * width * 4.5f * (opacity * 0.5f + 0.5f));
				
			}
		}
		
		GL11.glEnd();
		
	}
}
