package data.scripts.util;

import java.awt.Color;

import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lwjgl.util.vector.Vector2f;

//for light instances that need per-frame control
public class espc_LightWrapper {
	private StandardLight light;
	
	public espc_LightWrapper(Vector2f loc, Vector2f vel, float intensity, float size, Color color, float dur, float in, float out) {
        light = new StandardLight(intensity, size);
        light.setLocation(loc);
        if (vel != null)
        	light.setVelocity(vel);
        if (dur > 0f)
        	light.setLifetime(dur);
        light.setColor(color);
        if (in > 0f)
        	light.fadeIn(in);
        if (out > 0f)
        	light.setAutoFadeOutTime(out);
        LightShader.addLight(light);
	}
	
	public void setColor(Color color) {
		light.setColor(color);
	}
	
	public void setSize(float size) {
		light.setSize(size);
	}
	
	public void setIntensity(float intensity) {
		light.setIntensity(intensity);
	}
	
	public void setDur(float dur) {
		light.setLifetime(dur);
	}
	
	public void setFade(float out) {
		light.setIntensityFade(out);
	}
	
	public void remove() {
		LightShader.removeLight(light);
	}
}
