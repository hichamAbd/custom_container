package com.reelriot.vegas.gfx;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.BaseShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * Per-pixel shading for every 3D object: a key light, a fill light, two
 * colored point lights, and reflections of an imagined casino studio (warm
 * softboxes, a magenta fill, a dark velvet floor). Metal surfaces take
 * their color into the reflection, the way gold does.
 */
public class ToyShader implements Shader {

    /** How metallic a surface is, 0..1. */
    public static final long Metal = ToyAttr.METAL;
    /** How rough a surface is, 0 (mirror) .. 1 (velvet). */
    public static final long Rough = ToyAttr.ROUGH;

    /** The scene's lighting, set by whichever scene is drawing. */
    public static final class Lighting {
        public final Vector3 keyDir = new Vector3(-0.4f, -0.8f, -0.5f).nor();
        public final Color key = new Color(1.0f, 0.95f, 0.88f, 1);
        public final Vector3 fillDir = new Vector3(0.7f, -0.2f, -0.6f).nor();
        public final Color fill = new Color(0.45f, 0.25f, 0.4f, 1);
        public final Color ambient = new Color(0.16f, 0.1f, 0.12f, 1);
        public final Vector3[] pointPos = { new Vector3(2, 1, 2), new Vector3(-2, 2, 1) };
        public final Color[] point = { new Color(1f, 0.5f, 0.2f, 1), new Color(0.6f, 0.2f, 0.8f, 1) };
        public final Color envTint = new Color(1, 1, 1, 1);
        public final Color fog = new Color(0.08f, 0.01f, 0.04f, 1);
        public float fogNear = 8, fogFar = 30;
    }

    public static final Lighting LIGHT = new Lighting();

    private static final String VERT =
        "attribute vec3 a_position;\n" +
        "attribute vec3 a_normal;\n" +
        "attribute vec2 a_texCoord0;\n" +
        "uniform mat4 u_projViewTrans;\n" +
        "uniform mat4 u_worldTrans;\n" +
        "uniform mat3 u_normalMatrix;\n" +
        "varying vec3 v_pos;\n" +
        "varying vec3 v_normal;\n" +
        "varying vec2 v_uv;\n" +
        "void main() {\n" +
        "  vec4 wp = u_worldTrans * vec4(a_position, 1.0);\n" +
        "  v_pos = wp.xyz;\n" +
        "  v_normal = normalize(u_normalMatrix * a_normal);\n" +
        "  v_uv = a_texCoord0;\n" +
        "  gl_Position = u_projViewTrans * wp;\n" +
        "}\n";

    private static final String FRAG =
        "#ifdef GL_ES\n" +
        "precision highp float;\n" +
        "#endif\n" +
        "uniform vec3 u_camPos;\n" +
        "uniform vec4 u_color;\n" +
        "uniform vec3 u_emissive;\n" +
        "uniform float u_metal;\n" +
        "uniform float u_rough;\n" +
        "uniform float u_opacity;\n" +
        "uniform float u_hasTex;\n" +
        "uniform float u_unlit;\n" +
        "uniform sampler2D u_tex;\n" +
        "uniform vec3 u_keyDir;\n" +
        "uniform vec3 u_key;\n" +
        "uniform vec3 u_fillDir;\n" +
        "uniform vec3 u_fill;\n" +
        "uniform vec3 u_ambient;\n" +
        "uniform vec3 u_pPos0;\n" +
        "uniform vec3 u_pCol0;\n" +
        "uniform vec3 u_pPos1;\n" +
        "uniform vec3 u_pCol1;\n" +
        "uniform vec3 u_envTint;\n" +
        "uniform vec3 u_fog;\n" +
        "uniform vec2 u_fogRange;\n" +
        "varying vec3 v_pos;\n" +
        "varying vec3 v_normal;\n" +
        "varying vec2 v_uv;\n" +
        // the casino studio seen in reflections
        "vec3 studio(vec3 r) {\n" +
        "  vec3 c = mix(vec3(0.32, 0.12, 0.08), vec3(0.95, 0.66, 0.42), smoothstep(-0.45, 0.6, r.y));\n" +
        "  c += vec3(3.0, 2.7, 2.2) * pow(max(dot(r, normalize(vec3(-0.45, 0.75, 0.5))), 0.0), 70.0);\n" +
        "  c += vec3(1.9, 1.05, 0.55) * pow(max(dot(r, normalize(vec3(0.75, 0.35, 0.55))), 0.0), 26.0);\n" +
        "  c += vec3(1.0, 0.3, 0.75) * pow(max(dot(r, normalize(vec3(-0.85, -0.05, 0.4))), 0.0), 18.0);\n" +
        "  c += vec3(1.3, 1.1, 0.8) * smoothstep(0.86, 0.97, r.y);\n" +
        "  c += vec3(1.5, 1.3, 1.0) * pow(max(dot(r, normalize(vec3(0.15, 0.3, 1.0))), 0.0), 6.0);\n" +
        "  c += vec3(0.9, 0.7, 0.4) * smoothstep(0.9, 1.0, abs(r.x)) * smoothstep(-0.2, 0.3, r.y);\n" +
        "  return c * u_envTint;\n" +
        "}\n" +
        "void main() {\n" +
        "  vec4 base = u_color;\n" +
        "  if (u_hasTex > 0.5) base *= texture2D(u_tex, v_uv);\n" +
        "  if (u_unlit > 0.5) { gl_FragColor = vec4(base.rgb + u_emissive, base.a * u_opacity); return; }\n" +
        "  vec3 n = normalize(v_normal);\n" +
        "  vec3 v = normalize(u_camPos - v_pos);\n" +
        "  if (dot(n, v) < 0.0) n = -n;\n" +
        "  float shin = mix(180.0, 6.0, u_rough);\n" +
        "  float specK = (1.0 - u_rough) * (1.0 - u_rough);\n" +
        "  vec3 L = -u_keyDir;\n" +
        "  float ndl = max(dot(n, L), 0.0);\n" +
        "  float spec = pow(max(dot(n, normalize(L + v)), 0.0), shin) * specK;\n" +
        "  vec3 light = u_ambient + u_key * ndl + u_fill * max(dot(n, -u_fillDir), 0.0);\n" +
        "  vec3 specCol = u_key * spec;\n" +
        "  vec3 d0 = u_pPos0 - v_pos; float l0 = length(d0); d0 /= l0; float a0 = 1.0 / (1.0 + 0.35 * l0 * l0);\n" +
        "  light += u_pCol0 * max(dot(n, d0), 0.0) * a0;\n" +
        "  specCol += u_pCol0 * pow(max(dot(n, normalize(d0 + v)), 0.0), shin) * a0 * specK;\n" +
        "  vec3 d1 = u_pPos1 - v_pos; float l1 = length(d1); d1 /= l1; float a1 = 1.0 / (1.0 + 0.35 * l1 * l1);\n" +
        "  light += u_pCol1 * max(dot(n, d1), 0.0) * a1;\n" +
        "  specCol += u_pCol1 * pow(max(dot(n, normalize(d1 + v)), 0.0), shin) * a1 * specK;\n" +
        "  vec3 r = reflect(-v, n);\n" +
        "  vec3 env = mix(studio(r), vec3(0.42, 0.26, 0.2) * u_envTint, u_rough * 0.85);\n" +
        "  float fres = pow(1.0 - max(dot(n, v), 0.0), 5.0);\n" +
        "  vec3 F0 = mix(vec3(0.04), base.rgb, u_metal);\n" +
        "  vec3 F = F0 + (1.0 - F0) * fres * (1.0 - u_rough * 0.6);\n" +
        "  vec3 col = base.rgb * light * (1.0 - u_metal * 0.9) + env * F + specCol * mix(vec3(1.0), base.rgb, u_metal);\n" +
        "  col += u_emissive;\n" +
        "  col = col / (1.0 + col * 0.28);\n" +
        "  float fogK = smoothstep(u_fogRange.x, u_fogRange.y, length(u_camPos - v_pos));\n" +
        "  col = mix(col, u_fog, fogK);\n" +
        "  gl_FragColor = vec4(col, base.a * u_opacity);\n" +
        "}\n";

    private ShaderProgram program;
    private Camera camera;
    private RenderContext context;
    private final Matrix3 normalMatrix = new Matrix3();
    private final Color tmp = new Color();

    @Override
    public void init() {
        ShaderProgram.pedantic = false;
        program = new ShaderProgram(VERT, FRAG);
        if (!program.isCompiled()) throw new GdxRuntimeException("ToyShader: " + program.getLog());
    }

    @Override
    public int compareTo(Shader other) { return 0; }

    @Override
    public boolean canRender(Renderable instance) { return true; }

    @Override
    public void begin(Camera camera, RenderContext context) {
        this.camera = camera;
        this.context = context;
        program.bind();
        program.setUniformMatrix("u_projViewTrans", camera.combined);
        program.setUniformf("u_camPos", camera.position);
        Lighting l = LIGHT;
        program.setUniformf("u_keyDir", l.keyDir);
        program.setUniformf("u_key", l.key.r, l.key.g, l.key.b);
        program.setUniformf("u_fillDir", l.fillDir);
        program.setUniformf("u_fill", l.fill.r, l.fill.g, l.fill.b);
        program.setUniformf("u_ambient", l.ambient.r, l.ambient.g, l.ambient.b);
        program.setUniformf("u_pPos0", l.pointPos[0]);
        program.setUniformf("u_pCol0", l.point[0].r, l.point[0].g, l.point[0].b);
        program.setUniformf("u_pPos1", l.pointPos[1]);
        program.setUniformf("u_pCol1", l.point[1].r, l.point[1].g, l.point[1].b);
        program.setUniformf("u_envTint", l.envTint.r, l.envTint.g, l.envTint.b);
        program.setUniformf("u_fog", l.fog.r, l.fog.g, l.fog.b);
        program.setUniformf("u_fogRange", l.fogNear, l.fogFar);
        program.setUniformi("u_tex", 0);
        context.setDepthTest(GL20.GL_LEQUAL);
    }

    @Override
    public void render(Renderable r) {
        Attributes m = r.material;
        program.setUniformMatrix("u_worldTrans", r.worldTransform);
        program.setUniformMatrix("u_normalMatrix", normalMatrix.set(r.worldTransform).inv().transpose());

        ColorAttribute diffuse = (ColorAttribute) m.get(ColorAttribute.Diffuse);
        tmp.set(diffuse != null ? diffuse.color : Color.WHITE);
        program.setUniformf("u_color", tmp);
        ColorAttribute em = (ColorAttribute) m.get(ColorAttribute.Emissive);
        if (em != null) program.setUniformf("u_emissive", em.color.r, em.color.g, em.color.b);
        else program.setUniformf("u_emissive", 0, 0, 0);
        FloatAttribute metal = (FloatAttribute) m.get(Metal);
        FloatAttribute rough = (FloatAttribute) m.get(Rough);
        program.setUniformf("u_metal", metal != null ? metal.value : 0f);
        program.setUniformf("u_rough", rough != null ? rough.value : 0.5f);
        program.setUniformf("u_unlit", m.has(Mat.Unlit) ? 1f : 0f);

        TextureAttribute tex = (TextureAttribute) m.get(TextureAttribute.Diffuse);
        if (tex != null) {
            program.setUniformi("u_tex", context.textureBinder.bind(tex.textureDescription));
            program.setUniformf("u_hasTex", 1f);
        } else {
            program.setUniformf("u_hasTex", 0f);
        }

        BlendingAttribute blend = (BlendingAttribute) m.get(BlendingAttribute.Type);
        if (blend != null && blend.blended) {
            context.setBlending(true, blend.sourceFunction, blend.destFunction);
            context.setDepthMask(false);
            program.setUniformf("u_opacity", blend.opacity);
        } else {
            context.setBlending(false, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            context.setDepthMask(true);
            program.setUniformf("u_opacity", 1f);
        }
        IntAttribute cull = (IntAttribute) m.get(IntAttribute.CullFace);
        context.setCullFace(cull != null ? cull.value : GL20.GL_BACK);

        r.meshPart.render(program);
    }

    @Override
    public void end() {
        context.setDepthMask(true);
    }

    @Override
    public void dispose() {
        if (program != null) program.dispose();
    }

    /** Hands every renderable to the one ToyShader. */
    public static class Provider extends BaseShaderProvider {
        @Override
        protected Shader createShader(Renderable renderable) { return new ToyShader(); }
    }
}
