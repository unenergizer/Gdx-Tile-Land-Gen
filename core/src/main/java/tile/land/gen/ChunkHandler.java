package tile.land.gen;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
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

    private int currentChunkX;
    private int currentChunkZ;

    @Override
    public void create() {
        // Set the heightmap image we want to use
        heightmapProcessor.setHeightmapImage(Gdx.files.internal("heightmap4.jpg"));

        // Get the texture info ready
        Texture texture = new Texture(Gdx.files.internal("dirt.png"));
        Color color = Color.WHITE;

        // Generate chunk
        for (int chunkX = 0; chunkX < WORLD_X_LENGTH; chunkX++) {
            for (int chunkZ = 0; chunkZ < WORLD_Z_LENGTH; chunkZ++) {
                Chunk chunk = getChunk(chunkX, chunkZ, true);
                Model model = generateChunkModel(texture, color, chunkX, chunkZ);
                Objects.requireNonNull(chunk).setModel(model);
            }
        }

        // We can dispose of the heightmap now since all chunks have been generated.
        heightmapProcessor.dispose();

        // Print chunk data debug
        for (Chunk chunk : chunkConcurrentMap.values()) {
            System.out.println("[CHUNK DATA] " + chunk);
        }

        exampleModifyChunkTile();
    }

    /**
     * Generates a chunk landscape model.
     *
     * @param texture The texture to paint on this model.
     * @param color   The color we want to apply to the texture.
     * @param chunkX  The X location of this chunk.
     * @param chunkZ  The Z location of this chunk.
     * @return A model that represents a landscape.
     */
    @SuppressWarnings("PointlessArithmeticExpression")
    private Model generateChunkModel(Texture texture, Color color, int chunkX, int chunkZ) {
        // Define the attributes for this model
        VertexAttribute position = new VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE);
        VertexAttribute colorPacked = new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE);
        VertexAttribute textureCoordinates = new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0");

        // Init vertices array
        final int quadVertices = 4; // A quad has 4 vertices, one at each corner
        float[] vertices = new float[(position.numComponents + colorPacked.numComponents + textureCoordinates.numComponents) * quadVertices * CHUNK_SIZE * CHUNK_SIZE];

        // Populate the vertices array with data
        int vertexOffset = 0;
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {

                int tileX = x + chunkX * CHUNK_SIZE;
                int tileZ = z + chunkZ * CHUNK_SIZE;

                float y0 = heightmapProcessor.getHeight(tileX, tileZ);               // [0,0] - North West Corner
                float y1 = heightmapProcessor.getHeight(tileX, tileZ + 1);        // [0,1] - South West Corner
                float y2 = heightmapProcessor.getHeight(tileX + 1, tileZ);        // [1,0] - North East Corner
                float y3 = heightmapProcessor.getHeight(tileX + 1, tileZ + 1); // [1,1] - South East Corner

                vertexOffset = floorTile(vertices, vertexOffset, x, z, y0, y1, y2, y3, color, new TextureRegion(texture));
            }
        }

        // Generate the indices
        short[] indices = new short[6 * CHUNK_SIZE * CHUNK_SIZE];
        short j = 0;
        for (int i = 0; i < indices.length; i += 6, j += 4) {
            indices[i + 0] = (short) (j + 2);
            indices[i + 1] = (short) (j + 1);
            indices[i + 2] = (short) (j + 3);
            indices[i + 3] = (short) (j + 0);
            indices[i + 4] = (short) (j + 3);
            indices[i + 5] = (short) (j + 1);
        }

        // Create the mesh
        Mesh mesh = new Mesh(true, vertices.length, indices.length, position, colorPacked, textureCoordinates);
        mesh.setVertices(vertices);
        mesh.setIndices(indices);

        // Create the MeshPart I'd
        stringBuilder.append(chunkX);
        stringBuilder.append(SLASH);
        stringBuilder.append(chunkZ);

        // Create the MeshPart
        MeshPart meshPart = new MeshPart(stringBuilder.toStringAndClear(), mesh, 0, 6 * CHUNK_SIZE * CHUNK_SIZE, GL30.GL_TRIANGLES);

        // Create a model out of the MeshPart
        modelBuilder.begin();
        modelBuilder.part(meshPart, new Material("texture", TextureAttribute.createDiffuse(texture)));
        return modelBuilder.end();
    }

    private int floorTile(float[] vertices, int vertexOffset, float x, float z, float y0, float y1, float y2, float y3, Color tileColor, TextureRegion textureRegion) {
        final float color = Color.toFloatBits(tileColor.r, tileColor.g, tileColor.b, tileColor.a);
        float u1 = textureRegion.getU();
        float v1 = textureRegion.getV2();
        float u2 = textureRegion.getU2();
        float v2 = textureRegion.getV();

        // Bottom Left [0,0]
        vertices[vertexOffset++] = x;
        vertices[vertexOffset++] = y0;
        vertices[vertexOffset++] = z;
        vertices[vertexOffset++] = color;
        vertices[vertexOffset++] = u1;
        vertices[vertexOffset++] = v1;

        // Bottom Right [1,0]
        vertices[vertexOffset++] = x + TILE_SIZE;
        vertices[vertexOffset++] = y2;
        vertices[vertexOffset++] = z;
        vertices[vertexOffset++] = color;
        vertices[vertexOffset++] = u2;
        vertices[vertexOffset++] = v1;

        // Top Right [1,1]
        vertices[vertexOffset++] = x + TILE_SIZE;
        vertices[vertexOffset++] = y3;
        vertices[vertexOffset++] = z + TILE_SIZE;
        vertices[vertexOffset++] = color;
        vertices[vertexOffset++] = u2;
        vertices[vertexOffset++] = v2;

        // Top Left [0,1]
        vertices[vertexOffset++] = x;
        vertices[vertexOffset++] = y1;
        vertices[vertexOffset++] = z + TILE_SIZE;
        vertices[vertexOffset++] = color;
        vertices[vertexOffset++] = u1;
        vertices[vertexOffset++] = v2;

        return vertexOffset;
    }

    /**
     * This is some example code to get a tile and modify it. Here we recolor it and change it's size.
     * You can take what you get from here and plug this code into an editor. That is
     * currently beyond the scope of this demo.
     */
    private void exampleModifyChunkTile() {
        // Pick the tile you want to edit here
        int worldX = 2;
        int worldZ = 2;

        // Get chunk coordinates
        int chunkX = (int) (worldX / (float) CHUNK_SIZE);
        int chunkZ = (int) (worldZ / (float) CHUNK_SIZE);

        Chunk chunk = getChunk(chunkX, chunkZ, false);

        // Get the local tile on the chunk (0 - CHUNK_SIZE)
        int localX = worldX - chunkX * CHUNK_SIZE;
        int localZ = worldZ - chunkZ * CHUNK_SIZE;

        // Grab the mesh from this chunk
        ModelInstance modelInstance = Objects.requireNonNull(chunk).getModelInstance();
        Mesh mesh = modelInstance.model.meshes.get(0);

        // Lazy modify mesh
        Mesh mesh1 = resizeRectangleVertex(mesh, localX, localZ, 0, 5, 0, 0, 5, 0, 0, 5, 0, 0, 5, 0);

        // Rebuild the mesh
        MeshPart meshPart = new MeshPart(stringBuilder.toStringAndClear(), mesh1, 0, 6 * CHUNK_SIZE * CHUNK_SIZE, GL30.GL_TRIANGLES);

        // Create a model out of the MeshPart
        modelBuilder.begin();
        modelBuilder.part(meshPart, modelInstance.getMaterial("texture"));
        Model model = modelBuilder.end();

        // Set and cache the model
        chunk.setModel(model);
    }

    /**
     * Resizes a tile inside the mesh. This is done by modifying the position attribute of the vertex.
     *
     * @param mesh   The mesh we want to edit.
     * @param localX The local X tile we want to edit. Must be between 0 - CHUNK_SIZE.
     * @param localZ The local X tile we want to edit. Must be between 0 - CHUNK_SIZE.
     * @param x0     First corner X
     * @param y0     First corner Y
     * @param z0     First corner Z
     * @param x1     Second corner X
     * @param y1     Second corner Y
     * @param z1     Second corner Z
     * @param x2     Third corner X
     * @param y2     Third corner Y
     * @param z2     Third corner Z
     * @param x3     Forth corner X
     * @param y3     Forth corner Y
     * @param z3     Forth corner Z
     * @return A mesh containing the changes made here.
     */
    @SuppressWarnings("SameParameterValue")
    private Mesh resizeRectangleVertex(Mesh mesh, int localX, int localZ, int x0, float y0, int z0, int x1, float y1, int z1, int x2, float y2, int z2, int x3, float y3, int z3) {

        // Get the VertexAttributes. We need to know which ones are here.
        // If we only want to edit the position, then we need to skip over the
        // other VertexAttributes.
        VertexAttributes vertexAttributes = mesh.getVertexAttributes();
        int offset = vertexAttributes.getOffset(VertexAttributes.Usage.Position);

        // Get the exact tile we want to edit
        int attributesOffset = mesh.getVertexAttributes().vertexSize;
        int tile = attributesOffset * (localX * CHUNK_SIZE + localZ);

        // Calculate how big the vertices array should be
        int vertCount = mesh.getNumVertices() * mesh.getVertexSize() / 4;

        // Get the vertex size
        int vertexSize = mesh.getVertexSize() / 4;

        // Gets the vertices we want to edit
        float[] vertices = new float[vertCount];
        mesh.getVertices(vertices);

        // Corner [0,0] ///////////////////////////////////////
        int vertex = TileCorner.NORTH_WEST.getVertexID() * vertexSize + tile;
        int indexX = vertex + offset;
        int indexY = vertex + 1 + offset;
        int indexZ = vertex + 2 + offset;

        float x = vertices[indexX];
        float y = vertices[indexY];
        float z = vertices[indexZ];

        // Grow/shrink the vertices
        vertices[indexX] = x + x0;
        vertices[indexY] = y + y0;
        vertices[indexZ] = z + z0;

        // Corner [0,1] ///////////////////////////////////////
        vertex = TileCorner.SOUTH_WEST.getVertexID() * vertexSize + tile;
        indexX = vertex + offset;
        indexY = vertex + 1 + offset;
        indexZ = vertex + 2 + offset;

        x = vertices[indexX];
        y = vertices[indexY];
        z = vertices[indexZ];

        // Grow/shrink the vertices
        vertices[indexX] = x + x1;
        vertices[indexY] = y + y1;
        vertices[indexZ] = z + z1;

        // Corner [1,0] ///////////////////////////////////////
        vertex = TileCorner.NORTH_EAST.getVertexID() * vertexSize + tile;
        indexX = vertex + offset;
        indexY = vertex + 1 + offset;
        indexZ = vertex + 2 + offset;

        x = vertices[indexX];
        y = vertices[indexY];
        z = vertices[indexZ];

        // Grow/shrink the vertices
        vertices[indexX] = x + x2;
        vertices[indexY] = y + y2;
        vertices[indexZ] = z + z2;

        // Corner [1,1] ///////////////////////////////////////
        vertex = TileCorner.SOUTH_EAST.getVertexID() * vertexSize + tile;
        indexX = vertex + offset;
        indexY = vertex + 1 + offset;
        indexZ = vertex + 2 + offset;

        x = vertices[indexX];
        y = vertices[indexY];
        z = vertices[indexZ];

        // Grow/shrink the vertices
        vertices[indexX] = x + x3;
        vertices[indexY] = y + y3;
        vertices[indexZ] = z + z3;

        return mesh.updateVertices(offset, vertices);
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
     * Checks to see if the camera has moved out of their current chunk.
     *
     * @return True if the camera has moved to a new chunk, false otherwise.
     */
    public boolean hasLeftChunk() {
        int newChunkX = getChunkTileX();
        int newChunkZ = getChunkTileZ();

        if (currentChunkX == newChunkX && currentChunkZ == newChunkZ) return false;
        currentChunkX = newChunkX;
        currentChunkZ = newChunkZ;
        return true;
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
