package com.github.mpetkov.hiddengemsdeluxe.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.List;

/**
 * 3D renderer for the gem blocks.
 *
 * Uses block.png as the gem texture so the 3D block matches the same
 * faceted-diamond style as the 2D asset, with per-gem color tinting.
 */
public class Gem3DRenderer {

    private static ModelBatch modelBatch;
    private static Model gemModel;
    private static Model edgeLinesModel;
    private static Model burstSphereModel;
    private static Texture blockTexture;
    private static Environment environment;
    private static Camera camera;

    private static final Array<ModelInstance> instances = new Array<>();
    private static final Array<ModelInstance> edgeInstances = new Array<>();
    private static boolean initialized = false;

    /** 3D burst effects (expanding sphere when gems are cleared). */
    private static final List<BurstEffect> burstEffects = new ArrayList<>();
    /** 3D shard particles (flying gem fragments on land/match). */
    private static final List<ShardParticle> shardParticles = new ArrayList<>();

    private static final float BURST_DURATION = 0.5f;
    private static final float BURST_MAX_SCALE = 1.8f;
    private static final float SHARD_LIFE = 0.55f;
    private static final float SHARD_SPEED_MIN = 80f;
    private static final float SHARD_SPEED_MAX = 160f;
    private static final float SHARD_SIZE_SCALE = 0.22f;

    /** Текущ ъгъл на въртене (градуси) – всички камъни се въртят синхронно. */
    private static float rotationAngleDeg = 0f;
    /** Скорост на въртене: градуси в секунда. */
    private static final float ROTATION_SPEED = 45f;

    private Gem3DRenderer() {
        // static utility
    }

    /**
     * Зарежда block.png и прави фона (оцветяването извън формата на камъка) прозрачен,
     * за да се вижда само контурът/формата на камъка.
     */
    private static Texture loadBlockTextureWithTransparentBackground() {
        Pixmap pixmap = new Pixmap(Gdx.files.internal("block.png"));
        int w = pixmap.getWidth();
        int h = pixmap.getHeight();
        // Формат с алфа, ако няма
        if (pixmap.getFormat() != Pixmap.Format.RGBA8888) {
            Pixmap rgba = new Pixmap(w, h, Pixmap.Format.RGBA8888);
            rgba.drawPixmap(pixmap, 0, 0);
            pixmap.dispose();
            pixmap = rgba;
        }
        // Фонът обикновено е в ъглите – взимаме цвят от горния ляв ъгъл
        int cornerPixel = pixmap.getPixel(0, 0);
        int cr = (cornerPixel >>> 24) & 0xff;
        int cg = (cornerPixel >> 16) & 0xff;
        int cb = (cornerPixel >> 8) & 0xff;
        // Допуск: пиксели много близки до фона (ъгъл) стават напълно прозрачни
        final int tolerance = 32;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = pixmap.getPixel(x, y);
                int pr = (p >>> 24) & 0xff;
                int pg = (p >> 16) & 0xff;
                int pb = (p >> 8) & 0xff;
                int dr = Math.abs(pr - cr);
                int dg = Math.abs(pg - cg);
                int db = Math.abs(pb - cb);
                if (dr <= tolerance && dg <= tolerance && db <= tolerance) {
                    pixmap.drawPixel(x, y, 0); // напълно прозрачен
                }
            }
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private static final float INV_SQ3 = 0.57735027f;

    /** По-„анимационна” форма: леко разчупен октаедър (по-висок, по-мек контур). */
    private static final float GEM_SCALE_Y = 1.08f;
    private static final float GEM_SCALE_XZ = 0.94f;
    /** Малко издуване по нормалата за по-жив вид (без идеална геометрия). */
    private static final float GEM_BULGE = 0.018f;

    /** Посока на „осветление” за различен нюанс по стените (нормализована). */
    private static final float LIGHT_X = 0.4f;
    private static final float LIGHT_Y = 0.7f;
    private static final float LIGHT_Z = 0.5f;
    private static final float LIGHT_LEN = (float) Math.sqrt(LIGHT_X * LIGHT_X + LIGHT_Y * LIGHT_Y + LIGHT_Z * LIGHT_Z);

    /** Октаедър с per-vertex цвят – всяка страна различен нюанс (по-реалистично). */
    private static Model createOctahedronGemModel(Material material) {
        float t = INV_SQ3;
        float lx = LIGHT_X / LIGHT_LEN;
        float ly = LIGHT_Y / LIGHT_LEN;
        float lz = LIGHT_Z / LIGHT_LEN;
        float s = 0.5f;
        float[] v = new float[24 * 9]; // +1 за ColorPacked на връх
        int idx = 0;
        float[][] tris = new float[][] {
            { 0,1,0, 0,0,1, 1,0,0,  t,t,t },
            { 0,1,0, 1,0,0, 0,0,-1,  t,t,-t },
            { 0,1,0, 0,0,-1, -1,0,0, -t,t,-t },
            { 0,1,0, -1,0,0, 0,0,1, -t,t,t },
            { 0,-1,0, 1,0,0, 0,0,1,  t,-t,t },
            { 0,-1,0, 0,0,-1, 1,0,0,  t,-t,-t },
            { 0,-1,0, -1,0,0, 0,0,-1, -t,-t,-t },
            { 0,-1,0, 0,0,1, -1,0,0, -t,-t,t }
        };
        for (float[] tri : tris) {
            float nx = tri[9], ny = tri[10], nz = tri[11];
            float dot = nx * lx + ny * ly + nz * lz;
            float shade = 0.32f + 0.68f * Math.max(0f, dot);
            float colorPacked = Color.toFloatBits(shade, shade, shade, 1f);
            for (int i = 0; i < 3; i++) {
                float px = tri[i*3] * s;
                float py = tri[i*3+1] * s;
                float pz = tri[i*3+2] * s;
                px *= GEM_SCALE_XZ;
                py *= GEM_SCALE_Y;
                pz *= GEM_SCALE_XZ;
                int vi = idx / 9;
                float bulge = GEM_BULGE * (float) Math.sin(vi * 1.3);
                px += nx * bulge;
                py += ny * bulge;
                pz += nz * bulge;
                v[idx++] = px;
                v[idx++] = py;
                v[idx++] = pz;
                v[idx++] = nx;
                v[idx++] = ny;
                v[idx++] = nz;
                v[idx++] = 0.45f;
                v[idx++] = 0.5f;
                v[idx++] = colorPacked;
            }
        }
        short[] indices = new short[24];
        for (short i = 0; i < 24; i++) indices[i] = i;
        Mesh mesh = new Mesh(true, 24, 24,
                VertexAttribute.Position(), VertexAttribute.Normal(), VertexAttribute.TexCoords(0),
                new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, "a_color"));
        mesh.setVertices(v);
        mesh.setIndices(indices);
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.part("gem", mesh, GL20.GL_TRIANGLES, material);
        return mb.end();
    }

    /** Ефект при неутрализиране: разширяваща се полупрозрачна сфера. */
    private static class BurstEffect {
        float x, y;
        Color color;
        float life;
        final float maxLife;
        ModelInstance instance;

        BurstEffect(float x, float y, Color color, float maxLife) {
            this.x = x;
            this.y = y;
            this.color = new Color(color);
            this.life = maxLife;
            this.maxLife = maxLife;
            if (burstSphereModel != null) {
                this.instance = new ModelInstance(burstSphereModel);
            } else {
                this.instance = null;
            }
        }
    }

    /** 3D осколка при допир/кацане или при изчистване на съвпадение. */
    private static class ShardParticle {
        float x, y;
        float dx, dy;
        float life;
        final float initialLife;
        Color color;
        float rotSpeed;

        ShardParticle(float x, float y, float dx, float dy, Color color, float life) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.color = new Color(color);
            this.life = life;
            this.initialLife = life;
            this.rotSpeed = (MathUtils.random.nextFloat() - 0.5f) * 360f;
        }
    }

    /** 12 ръба на октаедъра – същата „разчупена” форма като камъка. */
    private static Model createOctahedronEdgeLinesModel(Material material) {
        float s = 0.5f;
        float[] verts = new float[] {
            0, s * GEM_SCALE_Y, 0,
            0, 0, s * GEM_SCALE_XZ,
            s * GEM_SCALE_XZ, 0, 0,
            0, 0, -s * GEM_SCALE_XZ,
            -s * GEM_SCALE_XZ, 0, 0,
            0, -s * GEM_SCALE_Y, 0
        };
        int[] edges = new int[] {
            0, 1,  0, 2,  0, 3,  0, 4,
            5, 1,  5, 2,  5, 3,  5, 4,
            1, 2,  2, 3,  3, 4,  4, 1
        };
        float[] vertices = new float[24 * 6];
        short[] indices = new short[24];
        for (int i = 0; i < 24; i++) {
            int vi = edges[i];
            vertices[i * 6]     = verts[vi * 3];
            vertices[i * 6 + 1] = verts[vi * 3 + 1];
            vertices[i * 6 + 2] = verts[vi * 3 + 2];
            vertices[i * 6 + 3] = 0f;
            vertices[i * 6 + 4] = 1f;
            vertices[i * 6 + 5] = 0f;
            indices[i] = (short) i;
        }
        Mesh mesh = new Mesh(true, 24, 24,
                VertexAttribute.Position(), VertexAttribute.Normal());
        mesh.setVertices(vertices);
        mesh.setIndices(indices);
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.part("edges", mesh, GL20.GL_LINES, material);
        return mb.end();
    }

    public static void initialize() {
        if (initialized) return;

        try {
            blockTexture = loadBlockTextureWithTransparentBackground();
            blockTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

            modelBatch = new ModelBatch();

            // Стил „скъпоценен камък”: силна специларност, прозрачност, жив цвят
            Material material = new Material(
                    TextureAttribute.createDiffuse(blockTexture),
                    ColorAttribute.createDiffuse(Color.WHITE),
                    ColorAttribute.createSpecular(0.95f, 0.98f, 1f, 1f),   // ярък, студен отблясък като при кристал
                    FloatAttribute.createShininess(64f),                   // висока гладкост – фасетите „искрят”
                    new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
            );

            gemModel = createOctahedronGemModel(material);

            // Контур по ръбовете (12 ръба при октаедър – стабилен, без wireframe)
            Material edgeMaterial = new Material(
                    ColorAttribute.createDiffuse(0.22f, 0.24f, 0.28f, 1f)
            );
            edgeLinesModel = createOctahedronEdgeLinesModel(edgeMaterial);

            // Сфера за 3D burst ефект при неутрализиране (полупрозрачна)
            Material burstMaterial = new Material(
                    ColorAttribute.createDiffuse(1f, 1f, 1f, 0.85f),
                    new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
            );
            ModelBuilder mbBurst = new ModelBuilder();
            mbBurst.begin();
            MeshPartBuilder burstPart = mbBurst.part("burst", GL20.GL_TRIANGLES,
                    VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, burstMaterial);
            burstPart.sphere(1f, 1f, 1f, 16, 12);
            burstSphereModel = mbBurst.end();

            // Осветление за „драгоценен” вид: по-ярка среда + няколко източника за отблески по фасетите
            environment = new Environment();
            environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.55f, 0.58f, 0.65f, 1f));
            environment.add(new DirectionalLight().set(
                    1f, 1f, 1f,
                    -0.4f, -0.6f, -0.5f
            ));
            environment.add(new DirectionalLight().set(
                    0.5f, 0.55f, 0.7f,
                    0.8f, -0.3f, 0.4f
            ));
            environment.add(new DirectionalLight().set(
                    0.25f, 0.3f, 0.4f,
                    -0.3f, 0.9f, 0.2f
            ));

            // Use an orthographic camera so world units line up with screen pixels.
            OrthographicCamera ortho = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            ortho.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            ortho.position.set(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, 1000f);
            ortho.lookAt(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, 0f);
            ortho.near = 1f;
            ortho.far = 2000f;
            ortho.update();
            camera = ortho;

            initialized = true;
        } catch (Exception e) {
            Gdx.app.error("Gem3DRenderer", "Failed to initialize 3D gems", e);
            dispose();
            initialized = false;
        }
    }

    public static void resize(int width, int height) {
        if (!(camera instanceof OrthographicCamera)) return;
        OrthographicCamera ortho = (OrthographicCamera) camera;
        ortho.setToOrtho(false, width, height);
        ortho.position.set(width / 2f, height / 2f, 1000f);
        ortho.lookAt(width / 2f, height / 2f, 0f);
        ortho.near = 1f;
        ortho.far = 2000f;
        ortho.update();
    }

    /** Call at the start of each frame before any blocks are queued. */
    public static void beginFrame() {
        instances.clear();
        edgeInstances.clear();
        rotationAngleDeg += Gdx.graphics.getDeltaTime() * ROTATION_SPEED;
        if (rotationAngleDeg >= 360f) rotationAngleDeg -= 360f;
        if (rotationAngleDeg < 0f) rotationAngleDeg += 360f;
    }

    /**
     * Добавя 3D burst ефект при неутрализиране на съвпадение (разширяваща се сфера).
     */
    public static void addBurstEffect(float centerX, float centerY, Color color) {
        if (!initialized || burstSphereModel == null) return;
        burstEffects.add(new BurstEffect(centerX, centerY, color, BURST_DURATION));
    }

    /**
     * Добавя 3D осколки при допир (кацане) или при изчистване на камъни.
     */
    public static void addShardExplosion(float centerX, float centerY, Color color) {
        if (!initialized || gemModel == null) return;
        for (int i = 0; i < 12; i++) {
            float angle = MathUtils.random.nextFloat() * MathUtils.PI2;
            float speed = SHARD_SPEED_MIN + MathUtils.random.nextFloat() * (SHARD_SPEED_MAX - SHARD_SPEED_MIN);
            float dx = MathUtils.cos(angle) * speed;
            float dy = MathUtils.sin(angle) * speed;
            shardParticles.add(new ShardParticle(centerX, centerY, dx, dy, color, SHARD_LIFE));
        }
    }

    /**
     * Обновява живота и позициите на 3D ефектите. Да се извиква от играта всеки кадър.
     */
    public static void updateEffects(float delta) {
        for (int i = burstEffects.size() - 1; i >= 0; i--) {
            BurstEffect b = burstEffects.get(i);
            b.life -= delta;
            if (b.life <= 0) {
                burstEffects.remove(i);
            }
        }
        for (int i = shardParticles.size() - 1; i >= 0; i--) {
            ShardParticle s = shardParticles.get(i);
            s.x += s.dx * delta;
            s.y += s.dy * delta;
            s.life -= delta;
            if (s.life <= 0) {
                shardParticles.remove(i);
            }
        }
    }

    /**
     * Queue a gem instance to be rendered this frame.
     *
     * @param centerX   screen/world X center of the gem (in pixels)
     * @param centerY   screen/world Y center of the gem (in pixels)
     * @param size      block cell size (pixels)
     * @param baseColor color of this gem
     */
    public static void addGem(float centerX, float centerY, float size, Color baseColor) {
        if (gemModel == null || edgeLinesModel == null) return;

        float uniformScale = size * 0.9f;

        ModelInstance instance = new ModelInstance(gemModel);
        instance.transform.idt();
        instance.transform.setToTranslation(centerX, centerY, 0f);
        instance.transform.scale(uniformScale, uniformScale, uniformScale);
        instance.transform.rotate(Vector3.X, 22f);
        instance.transform.rotate(Vector3.Y, rotationAngleDeg);

        Material mat = instance.materials.first();
        // По-светъл дифузен нюанс – същите основни цветове, но по-живи
        ColorAttribute diffuse = (ColorAttribute) mat.get(ColorAttribute.Diffuse);
        if (diffuse != null) {
            diffuse.color.set(baseColor.cpy().lerp(Color.WHITE, 0.2f));
        }
        // Отблясъкът по фасетите в тона на камъка (червен камък = топъл отблясък, син = студен и т.н.)
        ColorAttribute specular = (ColorAttribute) mat.get(ColorAttribute.Specular);
        if (specular != null) {
            specular.color.set(baseColor.cpy().lerp(Color.WHITE, 0.6f));
        }
        instances.add(instance);

        ModelInstance edgeInstance = new ModelInstance(edgeLinesModel);
        edgeInstance.transform.set(instance.transform);
        edgeInstances.add(edgeInstance);
    }

    /** Render all queued gems and 3D effects. */
    public static void renderAll() {
        if (!initialized || modelBatch == null || camera == null) return;

        camera.update();
        modelBatch.begin(camera);
        modelBatch.render(instances, environment);
        modelBatch.end();

        // Черни ръбове с леко по-дебели линии
        if (edgeInstances.size > 0) {
            Gdx.gl.glLineWidth(1.5f);
            modelBatch.begin(camera);
            modelBatch.render(edgeInstances, environment);
            modelBatch.end();
            Gdx.gl.glLineWidth(1f);
        }

        // 3D burst ефекти при неутрализиране
        if (burstEffects.size() > 0) {
            Array<ModelInstance> burstInstances = new Array<>();
            for (BurstEffect b : burstEffects) {
                if (b.instance == null) continue;
                float t = 1f - b.life / b.maxLife;
                float scale = t * BURST_MAX_SCALE * 25f;
                float alpha = 1f - t;
                b.instance.transform.idt();
                b.instance.transform.setToTranslation(b.x, b.y, 0f);
                b.instance.transform.scale(scale, scale, scale);
                ColorAttribute diffuse = (ColorAttribute) b.instance.materials.first().get(ColorAttribute.Diffuse);
                if (diffuse != null) {
                    diffuse.color.set(b.color.r, b.color.g, b.color.b, alpha * 0.85f);
                }
                burstInstances.add(b.instance);
            }
            if (burstInstances.size > 0) {
                modelBatch.begin(camera);
                modelBatch.render(burstInstances, environment);
                modelBatch.end();
            }
        }

        // 3D осколки при допир/изчистване
        if (shardParticles.size() > 0 && gemModel != null) {
            Array<ModelInstance> shardInstances = new Array<>();
            float shardBaseScale = 25f * SHARD_SIZE_SCALE;
            for (ShardParticle s : shardParticles) {
                float alpha = s.life / s.initialLife;
                float scale = shardBaseScale * alpha;
                ModelInstance shard = new ModelInstance(gemModel);
                shard.transform.idt();
                shard.transform.setToTranslation(s.x, s.y, 0f);
                shard.transform.scale(scale, scale, scale);
                shard.transform.rotate(Vector3.X, 22f);
                shard.transform.rotate(Vector3.Y, rotationAngleDeg + s.rotSpeed * (1f - alpha));
                Material mat = shard.materials.first();
                ColorAttribute diffuse = (ColorAttribute) mat.get(ColorAttribute.Diffuse);
                if (diffuse != null) {
                    diffuse.color.set(s.color.r, s.color.g, s.color.b, alpha);
                }
                ColorAttribute specular = (ColorAttribute) mat.get(ColorAttribute.Specular);
                if (specular != null) {
                    specular.color.set(s.color.r, s.color.g, s.color.b, alpha);
                }
                shardInstances.add(shard);
            }
            modelBatch.begin(camera);
            modelBatch.render(shardInstances, environment);
            modelBatch.end();
        }
    }

    public static void dispose() {
        if (gemModel != null) {
            gemModel.dispose();
            gemModel = null;
        }
        if (edgeLinesModel != null) {
            edgeLinesModel.dispose();
            edgeLinesModel = null;
        }
        if (burstSphereModel != null) {
            burstSphereModel.dispose();
            burstSphereModel = null;
        }
        if (modelBatch != null) {
            modelBatch.dispose();
            modelBatch = null;
        }
        if (blockTexture != null) {
            blockTexture.dispose();
            blockTexture = null;
        }
        burstEffects.clear();
        shardParticles.clear();
        initialized = false;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}

