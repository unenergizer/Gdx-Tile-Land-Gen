package tile.land.gen;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Disposable;

import static tile.land.gen.Constants.*;

/**
 * Process a Heightmap based on an image file.
 */
public class HeightmapProcessor implements Disposable {

    private Pixmap heightmapImage;

    /**
     * Set the image to be used as a heightmap for the terrain generation.
     *
     * @param fileHandle The image file.
     */
    public void setHeightmapImage(FileHandle fileHandle) {
        if (heightmapImage != null) heightmapImage.dispose();
        heightmapImage = new Pixmap(fileHandle);

        int worldXLength = CHUNK_SIZE * WORLD_X_LENGTH;
        int worldZLength = CHUNK_SIZE * WORLD_Z_LENGTH;
        int supportedXLength = heightmapImage.getWidth() / CHUNK_SIZE;
        int supportedZLength = heightmapImage.getHeight() / CHUNK_SIZE;

        if (worldXLength > heightmapImage.getWidth() || worldZLength > heightmapImage.getHeight()) {
            throw new RuntimeException("The world length or width is larger than this heightmap " + fileHandle.name() + " supports.\n" +
                "Max World X Length Supported: " + supportedXLength + ", Length Supplied: " + WORLD_X_LENGTH + "\n" +
                "Max World Z Length Supported: " + supportedZLength + ", Length Supplied: " + WORLD_Z_LENGTH);
        }
    }

    /**
     * Gets a pixel at the given x and z location. We then use that pixels color to
     * find a height value. Method made by <a href="https://github.com/tommyettinger">Tommy Ettinger</a>.
     *
     * @param x The x location of a heightmap pixel.
     * @param z The z (Y) location of a heightmap pixel.
     * @return The height value based on the pixels color.
     */
    public float getHeight(int x, int z) {
        // Gets pixel as RGBA8888
        // Isolates red channel, from 0 to 255 after this
        // 0.0f to 1.0f now
        // 0.0f to MAX_HEIGHT now
        return (heightmapImage.getPixel(x, z) >>> 24) / 255f * MAX_HEIGHT;
    }

    @Override
    public void dispose() {
        if (!heightmapImage.isDisposed()) heightmapImage.dispose();
    }
}
