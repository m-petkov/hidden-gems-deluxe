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
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

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
    private static Texture blockTexture;
    private static Environment environment;
    private static Camera camera;

    private static final Array<ModelInstance> instances = new Array<>();
    private static final Array<ModelInstance> edgeInstances = new Array<>();
    private static boolean initialized = false;

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

    private static final float INV_SQ3 = 0.57735027f; // 1/sqrt(3)

    /**
     * Създава истински 3D камък – октаедър (две пирамиди с основа до основа), както при
     * класически диамантен разрез. При въртене се виждат реални ръбове и фасети.
     */
    private static Model createOctahedronGemModel(Material material) {
        float t = INV_SQ3;
        float s = 0.5f; // мащаб за побиране в клетка (като предишния ромб)
        float[] v = new float[24 * 8];
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
            for (int i = 0; i < 3; i++) {
                v[idx++] = tri[i*3] * s; v[idx++] = tri[i*3+1] * s; v[idx++] = tri[i*3+2] * s;
                v[idx++] = nx; v[idx++] = ny; v[idx++] = nz;
                v[idx++] = 0.45f; v[idx++] = 0.5f;
            }
        }
        short[] indices = new short[24];
        for (short i = 0; i < 24; i++) indices[i] = i;

        Mesh mesh = new Mesh(true, 24, 24,
                VertexAttribute.Position(), VertexAttribute.Normal(), VertexAttribute.TexCoords(0));
        mesh.setVertices(v);
        mesh.setIndices(indices);

        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        modelBuilder.part("gem", mesh, GL20.GL_TRIANGLES, material);
        return modelBuilder.end();
    }

    /**
     * Създава модел само с ръбовете на октаедъра (12 линии) за черен контур по ръбовете –
     * без „раздуто” копие, няма сенки по стените.
     */
    private static Model createOctahedronEdgeLinesModel(Material material) {
        float s = 0.5f;
        float[] verts = new float[] {
            0, s, 0,    0, 0, s,    s, 0, 0,    0, 0, -s,   -s, 0, 0,    0, -s, 0
        };
        int[] edges = new int[] {
            0, 1,  0, 2,  0, 3,  0, 4,
            5, 1,  5, 2,  5, 3,  5, 4,
            1, 2,  2, 3,  3, 4,  4, 1
        };
        // Position + Normal (шейдърът очаква нормал; за линии ползваме 0,1,0)
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

            // Черни линии по ръбовете на октаедъра – стабилен контур, без сенки по стените
            Material edgeMaterial = new Material(
                    ColorAttribute.createDiffuse(0.22f, 0.24f, 0.28f, 1f)  // по-мек сив контур
            );
            edgeLinesModel = createOctahedronEdgeLinesModel(edgeMaterial);

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

        // 1. Цветният камък
        ModelInstance instance = new ModelInstance(gemModel);
        instance.transform.idt();
        instance.transform.setToTranslation(centerX, centerY, 0f);
        instance.transform.scale(uniformScale, uniformScale, uniformScale);
        instance.transform.rotate(Vector3.X, 22f);
        instance.transform.rotate(Vector3.Y, rotationAngleDeg);

        Material mat = instance.materials.first();
        ColorAttribute diffuse = (ColorAttribute) mat.get(ColorAttribute.Diffuse);
        if (diffuse != null) {
            diffuse.color.set(baseColor.cpy().lerp(Color.WHITE, 0.12f));
        }
        instances.add(instance);

        // 2. Черни линии по ръбовете (същата трансформация)
        ModelInstance edgeInstance = new ModelInstance(edgeLinesModel);
        edgeInstance.transform.set(instance.transform);
        edgeInstances.add(edgeInstance);
    }

    /** Render all queued gems. */
    public static void renderAll() {
        if (!initialized || modelBatch == null || camera == null) return;
        if (instances.size == 0 && edgeInstances.size == 0) return;

        camera.update();
        modelBatch.begin(camera);
        modelBatch.render(instances, environment);
        modelBatch.end();

        // Черни ръбове с леко по-дебели линии
        if (edgeInstances.size > 0) {
            Gdx.gl.glLineWidth(1f);
            modelBatch.begin(camera);
            modelBatch.render(edgeInstances, environment);
            modelBatch.end();
            Gdx.gl.glLineWidth(1f);
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
        if (modelBatch != null) {
            modelBatch.dispose();
            modelBatch = null;
        }
        if (blockTexture != null) {
            blockTexture.dispose();
            blockTexture = null;
        }
        initialized = false;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}

