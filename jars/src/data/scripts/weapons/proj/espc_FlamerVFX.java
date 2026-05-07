package data.scripts.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.EmpArcEntityAPI.EmpArcParams;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import com.fs.starfarer.api.util.Misc;

import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.FastTrig;

import org.lwjgl.opengl.GL11;

import data.scripts.util.MezzUtils;
import data.scripts.weapons.espc_IonAccumulatorEffect;
				
public class espc_FlamerVFX extends BaseEveryFrameCombatPlugin {
	
	private static final float BASE_DAMAGE = 80f;
	private static final float MAX_DAMAGE = 140f;
	private static final float BASE_WIDTH = 50f;
	private static final float MAX_WIDTH = 100f;
	
	
	private ArrayDeque<ArrayDeque<DamagingProjectileAPI>> trails;
	private CombatEngineAPI engine; 
	
	public espc_FlamerVFX() {
		trails = new ArrayDeque<ArrayDeque<DamagingProjectileAPI>>();
		
		
	}
	
	public void addProj(DamagingProjectileAPI proj) {
		float cTime = Global.getCombatEngine().getTotalElapsedTime(false);
		
	}
	
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
    	if (Global.getCombatEngine().isPaused())
    		return;
		if (trails.size() == 0)
			return;
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine == null)
			return;
		Iterator<ArrayDeque<DamagingProjectileAPI>> iter = trails.iterator();
		while (iter.hasNext()) {
			ArrayDeque<DamagingProjectileAPI> trail = iter.next();
			Iterator<DamagingProjectileAPI> projIter = trail.iterator();
			int index = -1;
			while (projIter.hasNext()) {
				DamagingProjectileAPI proj = projIter.next();
				index++;
				if (proj == null || !engine.isEntityInPlay(proj)) {
					iter.remove();
					if (index > 0 && trail.size() > 1) {
						ArrayDeque<DamagingProjectileAPI> newTrail = new ArrayDeque<DamagingProjectileAPI>();
						while (trail.size() > index + 1)
							newTrail.addFirst(trail.removeLast());
						trails.add(newTrail);
					}
					index--;
				}
			}
			if (trail.size() == 0) {
				iter.remove();
				continue;
			}
		}
	}
	
    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
		
		CombatEngineAPI engine = Global.getCombatEngine();
		float cTime = engine.getTotalElapsedTime(false);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
		SpriteAPI sprite = Global.getSettings().getSprite("fx", "espc_accumulatorsphere");
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, sprite.getTextureId());
		GL11.glBegin(GL11.GL_QUADS);

		Iterator<ArrayDeque<DamagingProjectileAPI>> trailIter = trails.iterator();
		while (trailIter.hasNext()) {
			ArrayDeque<DamagingProjectileAPI> trail = trailIter.next();
			Iterator<DamagingProjectileAPI> iter = trail.iterator();
			while (iter.hasNext()) {
				DamagingProjectileAPI proj = iter.next();
				if (proj == null || !engine.isEntityInPlay(proj)) {
					iter.remove();
					continue;
				}
				if (proj.getElapsed() < 0.3f) {
					
				}
				if (proj.isFading()) {
					continue;
				}
				GL11.glColor4ub(
					(byte) 120, (byte) 215, (byte) 255, (byte) 205);
				MezzUtils.glSquare(proj.getLocation().x, proj.getLocation().y, 
					3f + proj.getDamageAmount() * 0.025f * (proj.getWeapon().getSize().equals(WeaponSize.MEDIUM) ? 0.6f : 1f));
				
			}
		}
		GL11.glEnd();
		
	}
}
