package data.scripts.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Random;
import com.fs.starfarer.api.util.Misc;

import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.FastTrig;

import org.lwjgl.opengl.GL11;

import data.scripts.util.MezzUtils;
import data.scripts.weapons.espc_IonAccumulatorEffect;
				
public class espc_IonAccumulatorRenderPlugin extends BaseEveryFrameCombatPlugin {
	private ArrayDeque<DamagingProjectileAPI> projs;
	private final espc_IonAccumulatorEffect plugin;
	
	public espc_IonAccumulatorRenderPlugin(espc_IonAccumulatorEffect plugin) {
		projs = new ArrayDeque<DamagingProjectileAPI>();
		this.plugin = plugin;
	}
	
	public void addProj(DamagingProjectileAPI proj) {
		projs.add(proj);
	}
	
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
		CombatEngineAPI engine = Global.getCombatEngine();
		// engine.removePlugin(this);
	}
	
    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
		
		CombatEngineAPI engine = Global.getCombatEngine();
		float cTime = engine.getTotalElapsedTime(false);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
		SpriteAPI sprite = Global.getSettings().getSprite("systemMap", "radar_entity");
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, sprite.getTextureId());
		GL11.glBegin(GL11.GL_QUADS);
		/*
		
		for (float i = 0f; i < iMax; i += PARTICLE_INTERVAL) {
			// don't believe there's any other way to properly seed these random vals,
			// given the sequential nature of multiplicative congruential gen
			// the class is lightweight as can be.
			float randX = particleNoiseX.nextFloat();
			float randY = particleNoiseY.nextFloat();
			float randZ = particleNoiseZ.nextFloat();
			if (i < iMin)
				continue;
			
			// basically a layer of pseudo-random-enough bullshit noise over a chaotic trig func
			// grants a nice degree of uniformity to the patterns
			// random distribution is square instead of circular, but it's not really noticeable /cope
			float particleX = startPos.x + startVel.x * i + 
				(float) (FastTrig.sin(((i + rand * 5f) * (15f + rand * 20f))) * 30f + randX * 36f - 18f) 
				* angCos * MezzUtils.halfSineOut((cTime - i - effectStart)/PARTICLE_DUR_BASE)
				+ (float) (FastTrig.cos(((i + rand * 7f) * (20f + rand * 30f))) * 65f + randY * 36f - 18f) 
				* angCosY * MezzUtils.halfSineIn((cTime - i - effectStart)/PARTICLE_DUR_BASE);
			float particleY = startPos.y + startVel.y * i + 
				(float) (FastTrig.sin(((i + rand * 5f) * (15f + rand * 20f))) * 30f + randX * 36f - 18f) 
				* angSin * MezzUtils.halfSineOut((cTime - i - effectStart)/PARTICLE_DUR_BASE)
				+ (float) (FastTrig.cos(((i + rand * 7f) * (20f + rand * 30f))) * 65f + randY * 36f - 18f) 
				* angSinY * MezzUtils.halfSineIn((cTime - i - effectStart)/PARTICLE_DUR_BASE);
			float particleSize = 6f + randZ * 8f;
			
			// hybrid time+displacement approach for color, creating a hotter core
			if ((cTime - i - effectStart) / PARTICLE_DUR_BASE * (Math.abs(randX - 0.5f) + Math.abs(randY - 0.5f)) * 2f < 0.7f)
				MezzUtils.colorHSBLerp4UBRev(COLOR_INNER, COLOR_MID, 
					(int)(MathUtils.clamp((PARTICLE_DUR_BASE * 2f + (i - cTime + effectStart) * 2f), 0f, PARTICLE_DUR_BASE)/PARTICLE_DUR_BASE * 255f),
					(cTime - i - effectStart) / PARTICLE_DUR_BASE * (Math.abs(randX - 0.5f) + Math.abs(randY - 0.5f)) * 2f / 0.7f
				);
			else
				MezzUtils.colorHSBLerp4UB(COLOR_MID, COLOR_OUTER,
					(int)(MathUtils.clamp((PARTICLE_DUR_BASE * 2f + (i - cTime + effectStart) * 2f), 0f, PARTICLE_DUR_BASE)/PARTICLE_DUR_BASE * 255f),
					Math.min(((cTime - i - effectStart) / PARTICLE_DUR_BASE * (Math.abs(randX - 0.5f) + Math.abs(randY - 0.5f)) * 2f - 0.7f) * 7f / 0.3f, 1f)
				);
			
			MezzUtils.glSquare(particleX, particleY, particleSize/2f);
			
		}
		
		if (effectEnd > 0f) {
			
			// no need to reset seed: we already iterate fully through it prior.
			
			for (float i = 0; i < 18; i ++) {
				// slightly different application of our noise here, but the same set of sequences suffices.
				float randX = particleNoiseX.nextFloat();
				float randY = particleNoiseY.nextFloat();
				float randZ = particleNoiseZ.nextFloat();
			
				float particleX = startPos.x + startVel.x * (effectEnd - effectStart) + startVel.x/(2f + randX * 7f) * (cTime - effectEnd) + 
					MezzUtils.halfSineOut((cTime - effectEnd)/PARTICLE_DUR_BASE) * (float) FastTrig.cos(Math.toRadians(startAng - 55f + 110f * randY)) * 25f;
				float particleY = startPos.y + startVel.y * (effectEnd - effectStart) + startVel.y/(2f + randX * 7f) * (cTime - effectEnd) + 
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
		*/
		
		GL11.glEnd();
		
	}
}
