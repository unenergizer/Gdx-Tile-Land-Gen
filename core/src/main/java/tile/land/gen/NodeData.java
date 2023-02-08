package tile.land.gen;

import com.badlogic.gdx.graphics.g3d.model.Node;
import lombok.Data;

/**
 * Simple data class that holds info needed to edit a {@link Node}
 */
@Data
public class NodeData {
    private final int localX, localZ;

    private final Node node;
}
