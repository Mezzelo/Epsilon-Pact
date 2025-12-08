// see: fisprojvfx

package data.scripts.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.awt.Color;
import java.util.List;
import java.util.Random;
import com.fs.starfarer.api.util.Misc;

import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.FastTrig;

import org.lwjgl.opengl.GL11;

import data.scripts.util.MezzUtils;
				
public class espc_SemiRiftTrail extends BaseEveryFrameCombatPlugin {
	
	private final DamagingProjectileAPI proj;
	private final Vector2f startPos;
	private final float startAng;
	private Vector2f startVel;
	
	private final float effectStart;
	private float effectEnd = -1f;
	
	private static final float PARTICLE_DUR_BASE = 2f;
	private static final float PARTICLE_INTERVAL = 0.0004f;
	private static final Color COLOR_INNER = new Color(255, 235, 220);
	private static final Color COLOR_MID = new Color(200, 80, 255);
	private static final Color COLOR_OUTER = new Color(85, 45, 255);
	
	private float angSin;
	private float angCos;
	private float angSinY;
	private float angCosY;
	
	private float rand;
	
    public espc_SemiRiftTrail(DamagingProjectileAPI proj) {
        this.proj = proj;
        this.effectStart = Global.getCombatEngine().getTotalElapsedTime(false);
		this.startPos = new Vector2f();
		Vector2f.add(proj.getLocation(), new Vector2f(), this.startPos);
		this.startAng = proj.getFacing();
		this.startVel = new Vector2f();
		Vector2f.add(proj.getVelocity(), new Vector2f(), this.startVel);
		this.angSin = (float) FastTrig.sin(Math.toRadians(startAng - 90f));
		this.angCos = (float) FastTrig.cos(Math.toRadians(startAng - 90f));
		this.angSinY = (float) FastTrig.sin(Math.toRadians(startAng));
		this.angCosY = (float) FastTrig.cos(Math.toRadians(startAng));
		this.rand = Misc.random.nextFloat();
    }

	
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
		CombatEngineAPI engine = Global.getCombatEngine();
		// if (engine.isPaused())
		// 	return;
		// initializes 0, very cool.
		if (startVel.length() == 0f) {
			Vector2f.add(proj.getVelocity(), new Vector2f(), this.startVel);
			return;
		}
		
		if (!engine.isEntityInPlay(proj) && effectEnd <= 0f)
			effectEnd = Global.getCombatEngine().getTotalElapsedTime(false);
		else if (effectEnd <= 0f) {
			Vector2f velChangeCheck = new Vector2f();
			Vector2f.sub(startVel, proj.getVelocity(), velChangeCheck);
			if (Math.abs(startAng - proj.getFacing()) > 1f || velChangeCheck.length() > 1f || proj.didDamage() 
				// || proj.isFading() 
				)
				effectEnd = Global.getCombatEngine().getTotalElapsedTime(false);
		}
			
		if (effectEnd > 0f && engine.getTotalElapsedTime(false) > effectEnd + PARTICLE_DUR_BASE)
			engine.removePlugin(this);
	}
	
    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
		
		CombatEngineAPI engine = Global.getCombatEngine();
		float cTime = engine.getTotalElapsedTime(false);

		float iMin = Math.max(cTime - PARTICLE_DUR_BASE - effectStart, 0f);
		iMin = iMin - iMin % PARTICLE_INTERVAL;
		float iMax = effectEnd <= 0f ? cTime - effectStart: effectEnd - effectStart;
		Random particleNoiseX = new Random((long) (effectStart + startPos.x * 10f + startPos.y * 10f));
		Random particleNoiseY = new Random((long) ((effectStart + startPos.x * 10f + startPos.y * 10f) * 2f));
		Random particleNoiseZ = new Random((long) ((effectStart + startPos.x * 10f + startPos.y * 10f) * 3f));

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
		SpriteAPI sprite = Global.getSettings().getSprite("systemMap", "radar_entity");
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, sprite.getTextureId());
		GL11.glBegin(GL11.GL_QUADS); 	
		
		for (float i = 0f; i < iMax; i += PARTICLE_INTERVAL) {
			// don't believe there's any other way to properly seed these random vals,
			// given the sequential nature of multiplicative congruential gen
			// the class is lightweight as can be.
			float randX = particleNoiseX.nextFloat();
			float randY = particleNoiseY.nextFloat();
			float randZ = particleNoiseZ.nextFloat();
			if (i < iMin)
				continue;
			
			float particleX = startPos.x + startVel.x * i +
				(float) FastTrig.cos(randX * Math.PI * 2f) * randY * 80f
				* (MezzUtils.halfSineIn((cTime - i - effectStart)/PARTICLE_DUR_BASE) * 0.8f  + 0.3f);
			float particleY = startPos.y + startVel.y * i +
				(float) FastTrig.sin(randY * Math.PI * 2f) * randZ * 80f
				* (MezzUtils.halfSineIn((cTime - i - effectStart)/PARTICLE_DUR_BASE) * 0.8f + 0.3f);
			float particleSize = 6f + randZ * 8f;
			
			// hybrid time+displacement approach for color, creating a hotter core
			if ((cTime - i - effectStart) / PARTICLE_DUR_BASE * (Math.abs(randY - 0.5f) + Math.abs(randZ - 0.5f)) * 2f < 0.7f)
				MezzUtils.colorHSBLerp4UBRev(COLOR_INNER, COLOR_MID, 
					(int)(MathUtils.clamp((PARTICLE_DUR_BASE * 2f + (i - cTime + effectStart) * 2f), 0f, PARTICLE_DUR_BASE)/PARTICLE_DUR_BASE * 255f),
					(cTime - i - effectStart) / PARTICLE_DUR_BASE * (Math.abs(randY - 0.5f) + Math.abs(randZ - 0.5f)) * 2f / 0.7f
				);
			else
				MezzUtils.colorHSBLerp4UB(COLOR_MID, COLOR_OUTER,
					(int)(MathUtils.clamp((PARTICLE_DUR_BASE * 2f + (i - cTime + effectStart) * 2f), 0f, PARTICLE_DUR_BASE)/PARTICLE_DUR_BASE * 255f),
					Math.min(((cTime - i - effectStart) / PARTICLE_DUR_BASE * (Math.abs(randY - 0.5f) + Math.abs(randZ - 0.5f)) * 2f - 0.7f) * 7f / 0.3f, 1f)
				);
			
			MezzUtils.glSquare(particleX, particleY, particleSize/2f);
			
		}
		
		if (effectEnd > 0f) {
			
			// no need to reset seed: we already iterate fully through it prior.
			
			for (float i = 0; i < 120; i ++) {
				// slightly different application of our noise here, but the same set of sequences suffices.
				float randX = particleNoiseX.nextFloat();
				float randY = particleNoiseY.nextFloat();
				float randZ = particleNoiseZ.nextFloat();
			
				float particleX = startPos.x + startVel.x * (effectEnd - effectStart) + startVel.x/(12f + randX * 12f) * (cTime - effectEnd) + 
					MezzUtils.halfSineOut((cTime - effectEnd)/PARTICLE_DUR_BASE) * (float) FastTrig.cos(Math.toRadians(startAng - 55f + 110f * randY)) * 25f;
				float particleY = startPos.y + startVel.y * (effectEnd - effectStart) + startVel.y/(12f + randX * 12f) * (cTime - effectEnd) + 
					MezzUtils.halfSineOut((cTime - effectEnd)/PARTICLE_DUR_BASE) * (float) FastTrig.sin(Math.toRadians(startAng - 55f + 110f * randY)) * 25f;
				float particleSize = 9f + randZ * 6f;
				
				if (Math.abs(randY - 0.5f) < 0.25f)
					MezzUtils.colorHSBLerp4UBRev(COLOR_INNER, COLOR_MID, 
						(int)(MathUtils.clamp((effectEnd + PARTICLE_DUR_BASE - cTime - i/24f) * 2f, 0f, PARTICLE_DUR_BASE)/PARTICLE_DUR_BASE * 255f),
						Math.abs(randY - 0.5f) * 4f
					);
				else
					MezzUtils.colorHSBLerp4UB(COLOR_MID, COLOR_OUTER, 
						(int)(MathUtils.clamp((effectEnd + PARTICLE_DUR_BASE - cTime - i/24f) * 2f, 0f, PARTICLE_DUR_BASE)/PARTICLE_DUR_BASE * 255f),
						(Math.abs(randY - 0.5f) - 0.25f) * 4f
					);

				MezzUtils.glSquare(particleX, particleY, particleSize/2f);
			
			}
		}
		
		GL11.glEnd();
		
	}
}
