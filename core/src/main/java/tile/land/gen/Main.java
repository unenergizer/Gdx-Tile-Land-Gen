package tile.land.gen;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.StringBuilder;

import java.util.LinkedList;
import java.util.List;

/**
 * The main class, the {@link ApplicationListener} is the implementation shared by all platforms.
 */
public class Main extends ApplicationAdapter {
    /**
     * One StringBuilder to rule them all, one StringBuilder to find them,
     * One StringBuilder to bring them all, and in the darkness bind them;
     * In the Land of Gdx where the triangles lie. ~ Future Tolkien (probably)
     */
    private final StringBuilder stringBuilder = new StringBuilder();
    /**
     * Used to construct models in code.
     */
    private final ModelBuilder modelBuilder = new ModelBuilder();
    /**
     * ModelInstances to be rendered.
     */
    private final List<ModelInstance> instances = new LinkedList<>();
    /**
     * Handles the creation of land chunks.
     */
    private ChunkHandler chunkHandler;
    /**
     * Updates the user interface (debug data)
     */
    private StageHandler stageHandler;
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private FirstPersonCameraController camController;
    private Model xyzModel;
    private ModelInstance xyzModelInstance;

    @Override
    public void create() {

        // Init 3D Environment
        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        // Init Camera
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 10f, -5f);
        camera.lookAt(0, 0, 0);
        camera.up.set(Vector3.Y);
        camera.near = 1f;
        camera.far = 1000f;
        camera.update();

        // Init Input
        InputMultiplexer inputMultiplexer = new InputMultiplexer();
        camController = new FirstPersonCameraController(camera);
        camController.setVelocity(30);
        inputMultiplexer.addProcessor(camController);
        Gdx.input.setInputProcessor(inputMultiplexer);

        // Init Models
        xyzModel = modelBuilder.createXYZCoordinates(10, new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorUnpacked);
        xyzModelInstance = new ModelInstance(xyzModel);
        xyzModelInstance.transform.translate(0, 0, 0);

        // Init the ChunkHandler
        chunkHandler = new ChunkHandler(stringBuilder, modelBuilder, camera);
        chunkHandler.create();

        // Init Scene2D and VisUI
        stageHandler = new StageHandler(stringBuilder, camera, chunkHandler);
        stageHandler.create();
    }

    private void updateModelInstanceList() {
        instances.clear();
        instances.add(xyzModelInstance);
        chunkHandler.getNearbyChunks(instances);
    }

    @Override
    public void render() {
        updateModelInstanceList();
        camController.update();
        stageHandler.updateDebugText();

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        modelBatch.render(instances, environment);
        modelBatch.end();

        stageHandler.render();
    }

    @Override
    public void dispose() {
        stageHandler.dispose();

        // models
        instances.clear();
        chunkHandler.dispose();
        modelBatch.dispose();
        xyzModel.dispose();
    }
}
