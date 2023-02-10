package tile.land.gen;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.StringBuilder;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static tile.land.gen.Constants.*;

/**
 * This is the class that creates, manages, and disposes of all chunks.
 * The landmass is created here. See the example method below.
 */
@RequiredArgsConstructor
public class ChunkHandler extends ApplicationAdapter {
    private final ConcurrentMap<Chunk.Key, Chunk> chunkConcurrentMap = new ConcurrentHashMap<>();
    private final HeightmapProcessor heightmapProcessor = new HeightmapProcessor();
    private final StringBuilder stringBuilder;
    private final ModelBuilder modelBuilder;
    private final PerspectiveCamera camera;

    @Override
    public void create() {
        // Set the heightmap image we want to use
        heightmapProcessor.setHeightmapImage(Gdx.files.internal("heightmap4.jpg"));

        // Get the texture info ready
        Texture texture = new Texture(Gdx.files.internal("dirt.png"));
        Material material = new Material(TextureAttribute.createDiffuse(texture));
        long attributes = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates | VertexAttributes.Usage.ColorPacked;

        // Generate chunk
        for (int x = 0; x < WORLD_X_LENGTH; x++) {
            for (int z = 0; z < WORLD_Z_LENGTH; z++) {
                Chunk chunk = getChunk(x, z, true);
                Model model = generateChunkModel(texture, material, attributes, x, z);
                Objects.requireNonNull(chunk).setModel(model);
            }
        }

        // We can dispose of the heightmap now since all chunks have been generated.
        heightmapProcessor.dispose();

        // Print chunk data debug
        for (Chunk chunk : chunkConcurrentMap.values()) {
            System.out.println("[CHUNK DATA] " + chunk);
        }

        exampleModifyChunkTile(texture);
    }

    /**
     * This is some example code to get a tile and modify it. Here we recolor it and change it's size.
     * You can take what you get from here and plug this code into an editor. That is
     * currently beyond the scope of this demo.
     *
     * @param texture The current tile texture file.
     */
    private void exampleModifyChunkTile(Texture texture) {

        // Pick the tile you want to edit here
        int x = 1;
        int z = 1;

        NodeData nodeData = getChunkTile(x, z);
        Node node = nodeData.getNode();

        NodePart nodePart = node.parts.get(0);
        // Grab the existing material? If all rectangles share the same material, any edit here will be for all of them
        //        Material material1 = node.parts.get(0).material;
        Material material1 = new Material(TextureAttribute.createDiffuse(texture)); // Create a new material so to not affect other tiles
        material1.set(ColorAttribute.createDiffuse(Color.BLUE));
        nodePart.material = material1;

        // grab the mesh, lets modify it
        Mesh mesh = nodePart.meshPart.mesh;
        int vertCount = mesh.getVertexSize();
        resizeRectangleVertex(mesh, TileCorner.SOUTH_WEST, vertCount * (nodeData.getLocalX() * CHUNK_SIZE + nodeData.getLocalZ()), 0, 15, 0);
    }

    /**
     * Generates a chunk landscape model.
     *
     * @param texture            The texture to paint on this model.
     * @param material           The material for this model.
     * @param attributes         The attributes for this model.
     * @param chunkX             The X location of this chunk.
     * @param chunkZ             The Z location of this chunk.
     * @return A model that represents a landscape.
     */
    private Model generateChunkModel(Texture texture, Material material, long attributes, int chunkX, int chunkZ) {
        modelBuilder.begin();
        modelBuilder.manage(texture);

        Node node;
        MeshPartBuilder meshPartBuilder;

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                stringBuilder.append(x);
                stringBuilder.append(SLASH);
                stringBuilder.append(z);
                String id = stringBuilder.toStringAndClear();

                int tileX = x + chunkX * CHUNK_SIZE;
                int tileZ = z + chunkZ * CHUNK_SIZE;

                float y0 = heightmapProcessor.getHeight(tileX, tileZ);               // 0,0 - North West Corner
                float y1 = heightmapProcessor.getHeight(tileX, tileZ + 1);        // 0,1 - South West Corner
                float y2 = heightmapProcessor.getHeight(tileX + 1, tileZ);        // 1,0 - North East Corner
                float y3 = heightmapProcessor.getHeight(tileX + 1, tileZ + 1); // 1,1 - South East Corner

                // Define a new node for this chunk
                node = modelBuilder.node();
                node.id = id;
                meshPartBuilder = modelBuilder.part(id, GL20.GL_TRIANGLES, attributes, material);
//                meshPartBuilder.setColor(Color.GREEN); // Set a color if you like
                meshPartBuilder.rect(
                    x, y1, z + TILE_SIZE,                // South West Corner
                    x + TILE_SIZE, y3, z + TILE_SIZE,    // South East Corner
                    x + TILE_SIZE, y2, z,                    // North East Corner
                    x, y0, z,                                // North West Corner
                    1f, 1f, 1f);

                // Render debug lines over the tile. Currently, this will break the modification of the example tile code.
                // Not quiet sure how to fix that yet. Will investigate...
                if (!RENDER_DEBUG_LINES) continue;
                float debugLineHeight = .01f;

                // Line 1
                node = modelBuilder.node();
                node.id = id + "debug";
                meshPartBuilder = modelBuilder.part(id + "debug", GL20.GL_LINES, attributes, material);
                meshPartBuilder.setColor(Color.BLUE);
                meshPartBuilder.line(x, y0 + debugLineHeight, z, x, y1 + debugLineHeight, z + TILE_SIZE);

                // Line 2
                node = modelBuilder.node();
                node.id = id + "debug";
                meshPartBuilder = modelBuilder.part(id + "debug", GL20.GL_LINES, attributes, material);
                meshPartBuilder.setColor(Color.RED);
                meshPartBuilder.line(x, y1 + debugLineHeight, z + TILE_SIZE, x + TILE_SIZE, y3 + debugLineHeight, z + TILE_SIZE);
            }
        }

        return modelBuilder.end();
    }

    /**
     * This code was written by <a href="https://github.com/JamesTKhan">JamesTKhan</a>.
     * If it is broken find him in the LibGDX discord and ask him how to fix. It's beyond me right now.
     *
     * @param mesh            The mesh we want to edit.
     * @param tileCorner      The tile corner we want to edit.
     * @param vertCountOffset How many "tiles/meshes" to skip to get the right mesh to edit?
     * @param resizeX         The new x position.
     * @param resizeY         The new y position.
     * @param resizeZ         The new z position.
     */
    @SuppressWarnings("SameParameterValue")
    private void resizeRectangleVertex(Mesh mesh, TileCorner tileCorner, int vertCountOffset, int resizeX, float resizeY, int resizeZ) {
        VertexAttributes vertexAttributes = mesh.getVertexAttributes();
        int offset = vertexAttributes.getOffset(VertexAttributes.Usage.Position);

        int vertexSize = mesh.getVertexSize() / 4;
        int vertCount = mesh.getNumVertices() * mesh.getVertexSize() / 4;

        float[] vertices = new float[vertCount];
        mesh.getVertices(vertices); // Gets the vertices we want to edit? (do not delete)

        System.out.println("[MODIFY DATA] VertexAttributes: " + vertexAttributes.vertexSize + ", Offset: " + offset + ", VertexSize: " + vertexSize + ", Vertex Count: " + vertCount + ", Verticies: " + vertices.length);

        // Get XYZ vertices position data
        int currentVertex = 0;
        for (int i = vertCountOffset; i < vertices.length; i += vertexSize) {
            if (currentVertex == tileCorner.getVertexID()) {
                int indexX = i + offset;
                int indexY = i + 1 + offset;
                int indexZ = i + 2 + offset;

                float x = vertices[indexX];
                float y = vertices[indexY];
                float z = vertices[indexZ];

                // Grow/shrink the vertices
                vertices[indexX] = x + resizeX;
                vertices[indexY] = y + resizeY;
                vertices[indexZ] = z + resizeZ;
                break;
            }
            currentVertex++;
        }

        mesh.updateVertices(offset, vertices);
    }

    /**
     * Gets a chunk tile in the world.
     *
     * @param worldX The world x location.
     * @param worldZ The world z location.
     * @return NodeData with info about this tile.
     */
    private NodeData getChunkTile(int worldX, int worldZ) {
        if (worldX < 0 || worldX > WORLD_X_LENGTH * CHUNK_SIZE || worldZ < 0 || worldZ > WORLD_Z_LENGTH * CHUNK_SIZE) {
            throw new RuntimeException("World cords out of bounds: " + worldX + SLASH + worldZ);
        }

        int chunkX = (int) (worldX / (float) CHUNK_SIZE);
        int chunkZ = (int) (worldZ / (float) CHUNK_SIZE);

        Chunk chunk = getChunk(chunkX, chunkZ, false);

        int localX = worldX - chunkX * CHUNK_SIZE;
        int localZ = worldZ - chunkZ * CHUNK_SIZE;

        stringBuilder.append(localX);
        stringBuilder.append(SLASH);
        stringBuilder.append(localZ);

        Node node = Objects.requireNonNull(chunk).getModelInstance().getNode(stringBuilder.toStringAndClear());
        return new NodeData(localX, localZ, node);
    }

    /**
     * Gets a world chunk.
     *
     * @param x           the x location of the chunk
     * @param z           the z location of the chunk
     * @param createChunk if true, we will create a chunk if it doesn't exist
     * @return A world chunk.
     */
    private Chunk getChunk(int x, int z, boolean createChunk) {
        int hashCode = x * 31 + z;
        for (Map.Entry<Chunk.Key, Chunk> chunkEntry : chunkConcurrentMap.entrySet()) {
            Chunk.Key key = chunkEntry.getKey();
            Chunk chunk = chunkEntry.getValue();
            if (key.hashCode() == hashCode) return chunk;
        }

        if (!createChunk) return null;
        // No chunk exists, create a new one
        Chunk chunk = new Chunk(x, z);
        System.out.println("[NEW CHUNK] Location: " + x + SLASH + z);
        chunkConcurrentMap.put(new Chunk.Key(x, z), chunk);
        return chunk;
    }

    @Override
    public void dispose() {
        heightmapProcessor.dispose();

        for (Chunk chunk : chunkConcurrentMap.values()) {
            if (chunk != null && chunk.getModel() != null) chunk.getModel().dispose();
        }
    }

    /**
     * This is going to get the nearby chunks and only render those. This isn't the best way to do this
     * and this should only be considered a hack.
     */
    public void getNearbyChunks(ModelCache cache) {

        int camX = (int) camera.position.x;
        int camZ = (int) camera.position.z;
        int chunkX = camX / CHUNK_SIZE;
        int chunkZ = camZ / CHUNK_SIZE;

        for (int x = chunkX - CHUNK_VIEW_RADIUS; x < chunkX + CHUNK_VIEW_RADIUS + 1; x++) {
            for (int z = chunkZ - CHUNK_VIEW_RADIUS; z < chunkZ + CHUNK_VIEW_RADIUS + 1; z++) {
                if (x < 0 || z < 0) continue; // No negative chunks or models instances exist here (in this project)...

                Chunk chunk = getChunk(x, z, false);
                if (chunk == null) continue;

                ModelCache modelCache = chunk.getModelCache();
                if (modelCache == null) continue;

                cache.add(modelCache);
            }
        }
    }

    /**
     * Used for debug statements.
     */
    public int getCurrentChunkX() {
        return (int) camera.position.x / CHUNK_SIZE;
    }

    /**
     * Used for debug statements.
     */
    public int getCurrentChunkZ() {
        return (int) camera.position.z / CHUNK_SIZE;
    }

    /**
     * Used for debug statements.
     */
    public int getChunkTileX() {
        int camX = (int) camera.position.x;
        int chunkX = camX / CHUNK_SIZE;
        return camX - chunkX * CHUNK_SIZE;
    }

    /**
     * Used for debug statements.
     */
    public int getChunkTileZ() {
        int camZ = (int) camera.position.z;
        int chunkZ = camZ / CHUNK_SIZE;
        return camZ - chunkZ * CHUNK_SIZE;
    }
}
