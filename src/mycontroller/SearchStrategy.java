package mycontroller;

import tiles.MapTile;
import utilities.Coordinate;
import world.WorldSpatial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static mycontroller.RelativeDirection.*;

public abstract class SearchStrategy {
    public abstract void startSearch(HashMap<Coordinate, MapTile> map, WorldSpatial.Direction direction, Coordinate initialPos, List<Coordinate> intermediateGoals,
                                     List<Coordinate> finalGoals, Set<Integer> keysCollected);

    public abstract List<RelativeDirection> getDirections();

    public abstract List<Coordinate> getPath();

    /**
     * Convert a path given in coordinates to a list of relative directions.
     *
     * @param path        Path of coordinates
     * @param orientation Starting orientation to base relative directions off.
     * @return Relative directions based on the given coordinate path
     */
    protected List<RelativeDirection> toRelativeDirection(List<Coordinate> path, WorldSpatial.Direction orientation) {
        List<RelativeDirection> relativeDirections = new ArrayList<>();

        for (int i = 1; i < path.size(); i++) {
            RelativeDirection relativeDirection = relativeDirection(orientation, path.get(i - 1), path.get(i));
            relativeDirections.add(relativeDirection);

            switch (relativeDirection) {
                case LEFT:
                    orientation = WorldSpatial.changeDirection(orientation, WorldSpatial.RelativeDirection.LEFT);
                    break;
                case RIGHT:
                    orientation = WorldSpatial.changeDirection(orientation, WorldSpatial.RelativeDirection.RIGHT);
                    break;
                case FORWARD:
                    break;
                case BACKWARD:
                    orientation = WorldSpatial.reverseDirection(orientation);
                    break;
                case NONE:
                    // Do nothing
                    break;

            }
        }
        return relativeDirections;
    }

    /**
     * Determine the relative direction of the first coordinate from the second give an an object with the specified
     * orientation
     *
     * @param orientation Orientation of the object
     * @param coord1      Coordinate the object is currently at
     * @param coord2      Coordinate the object is going towards
     * @return Relative direction the object must turn to go from coord1 to coord2
     */
    private RelativeDirection relativeDirection(WorldSpatial.Direction orientation, Coordinate coord1, Coordinate coord2) {
        int xDelta = coord1.x - coord2.x;
        int yDelta = coord1.y - coord2.y;

        switch (orientation) {
            case EAST:
                if (xDelta < 0) {
                    return FORWARD;
                } else if (xDelta > 0) {
                    return BACKWARD;
                } else if (yDelta < 0) {
                    return LEFT;
                } else if (yDelta > 0) {
                    return RIGHT;
                }
                break;
            case WEST:
                if (xDelta < 0) {
                    return BACKWARD;
                } else if (xDelta > 0) {
                    return FORWARD;
                } else if (yDelta < 0) {
                    return RIGHT;
                } else if (yDelta > 0) {
                    return LEFT;
                }
                break;
            case SOUTH:
                if (xDelta < 0) {
                    return LEFT;
                } else if (xDelta > 0) {
                    return RIGHT;
                } else if (yDelta < 0) {
                    return BACKWARD;
                } else if (yDelta > 0) {
                    return FORWARD;
                }
                break;
            case NORTH:
                if (xDelta < 0) {
                    return RIGHT;
                } else if (xDelta > 0) {
                    return LEFT;
                } else if (yDelta < 0) {
                    return FORWARD;
                } else if (yDelta > 0) {
                    return BACKWARD;
                }
                break;
        }
        return null;
    }

    private RelativeDirection opposite(RelativeDirection direction) {
        switch (direction) {
            case LEFT:
                return RIGHT;
            case RIGHT:
                return LEFT;
            case FORWARD:
                return BACKWARD;
            case BACKWARD:
                return FORWARD;
            case NONE:
                return NONE;
        }
        return NONE;
    }
}
