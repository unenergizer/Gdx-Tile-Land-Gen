package tile.land.gen;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TileCorner {
    SOUTH_WEST(0),
    SOUTH_EAST(1),
    NORTH_EAST(2),
    NORTH_WEST(3);

    private final int vertexID;
}
