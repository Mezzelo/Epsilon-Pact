package data.scripts.util;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.dark.shaders.distortion.WaveDistortion;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lwjgl.util.vector.Vector2f;

public class espc_ShaderDistortionWrapper {
	
	public static void StandardLight() {
		StandardLight light = new StandardLight();
	}

	public static void AddLight(Vector2f location) {
		LightShader.addLight(null);
	}
}
