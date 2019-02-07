package mycontroller;

import tiles.HealthTrap;
import tiles.LavaTrap;
import tiles.MapTile;
import utilities.Coordinate;
import world.Car;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MyAIController extends WallFollowingAIController {
    /**
     * Controller's knowledge ot the world
     */
    private HashMap<Coordinate, MapTile> mWorldMap;
    /**
     * Path that we are following
     */
    private List<Coordinate> mPath;
    /**
     * Direction the car is to follow on each update. This is just the path converted to relative directions.
     */
    private List<RelativeDirection> mDirections;
    /**
     * Strategies for reaching goals.
     *
     * mSearchStrategy is the strategy used for keys, health and exists.
     * mExplorationStrategy is used when we need to explore more of the map.
     */
    private SearchStrategy mSearchStrategy, mExplorationStrategy;

    public MyAIController(Car car) {
        super(car);
        mWorldMap = getMap();
        mPath = new ArrayList<>();
        mDirections = new ArrayList<>();
        mSearchStrategy = new AStarSearch();
        mExplorationStrategy = new ExplorationSearch();

        // Set all the roads to utility to mark them as "unexplored"
        mWorldMap.entrySet().stream()
                .map(entry -> {
                    if (entry.getValue().isType(MapTile.Type.ROAD)) {
                        entry.setValue(new MapTile(MapTile.Type.UTILITY));
                    }
                    return entry;
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public void update() {
        updateWorldMap();

        if (explorationNeeded()) {
            explore();
        } else if (mWorldMap.get(getCarPosition()) instanceof HealthTrap && getHealth() < 100) {
            applyBrake();
            return;
        } else if (healthNeeded()) {
            maybeFindHealth();
        } else {
            setFollowing(false);
            updatePath();
            if (mDirections.isEmpty()) {
                // Search couldn't find a path to any keys, therefore they are inaccessible
                uncollectedKeys().clear();
                explore();
            }
        }


        // When the search turns up nothing, we'll just fall back to the wall follower
        if (mDirections.isEmpty()) {
            super.update();
            return;
        }

        RelativeDirection nextDirection = mDirections.remove(0);
        mPath.remove(0);

        switch (nextDirection) {
            case LEFT:
                if (mCar.getVelocity() < 0)
                    applyReverseAcceleration();
                else
                    applyForwardAcceleration();
                turnLeft();
                break;
            case RIGHT:
                if (mCar.getVelocity() < 0)
                    applyReverseAcceleration();
                else
                    applyForwardAcceleration();
                turnRight();
                break;
            case FORWARD:
                applyForwardAcceleration();
                break;
            case BACKWARD:
                applyReverseAcceleration();
                break;
            default:
                // Sanity check
                throw new RuntimeException("Unhandled relative direction");
        }
    }

    /**
     * Potentially find a route to health.
     *
     * If there is a route to a goal (key or exit) that does not do damage then it is taken, otherwise, we go try to
     * find a health trap.
     */
    private void maybeFindHealth() {
        mSearchStrategy.startSearch(mWorldMap, getOrientation(), getCarPosition(), uncollectedKeys(),
                exits(), getKeys());
        if (noDamagePath(mSearchStrategy.getPath())) {
            mDirections = mSearchStrategy.getDirections();
            mPath = mSearchStrategy.getPath();
            return;
        }
        List<Coordinate> healths = mWorldMap.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof HealthTrap)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        mSearchStrategy.startSearch(mWorldMap, getOrientation(), getCarPosition(), healths, exits(), mCar.getKeys());
        mDirections = mSearchStrategy.getDirections();
        mPath = mSearchStrategy.getPath();
    }

    /**
     * Check if the given path does not do any damage.
     * @param path Path to check
     * @return
     */
    private boolean noDamagePath(List<Coordinate> path) {
        return path.stream()
                .noneMatch(c -> mWorldMap.get(c).isType(MapTile.Type.TRAP) && !(mWorldMap.get(c) instanceof HealthTrap));
    }

    /**
     * Check if the car requires health
     * @return
     */
    private boolean healthNeeded() {
        return getHealth() < 30;
    }

    /**
     * Set the AI on a course that will explore more of the map.
     */
    private void explore() {

        List<Coordinate> utilities = mWorldMap.entrySet().stream()
                .filter(entry -> entry.getValue().isType(MapTile.Type.UTILITY))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        mExplorationStrategy.startSearch(mWorldMap, getOrientation(), getCarPosition(), utilities, exits(), mCar.getKeys());
        mDirections = mExplorationStrategy.getDirections();
        mPath = mExplorationStrategy.getPath();
    }

    /**
     * Check if exploration of the map is needed
     * @return
     */
    private boolean explorationNeeded() {
        // We need to do some exploring to find more keys
        return uncollectedKeys().isEmpty() && mCar.getKeys().size() < numKeys();
    }

    public List<Coordinate> getPath() {
        return mPath;
    }

    /**
     * Update the controller's path based on the current map of the world.
     */
    private void updatePath() {
        mSearchStrategy.startSearch(mWorldMap, mCar.getOrientation(), getCarPosition(), uncollectedKeys(), exits(), mCar.getKeys());
        mDirections = mSearchStrategy.getDirections();
        mPath = mSearchStrategy.getPath();
    }

    /**
     * Find all the exits on the current map
     * @return
     */
    private List<Coordinate> exits() {
        return mWorldMap.entrySet().stream()
                .filter(entry -> entry.getValue().isType(MapTile.Type.FINISH))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Find all keys that we know of but have not been collected.
     *
     * This method excluded duplicate keys.
     * @return
     */
    private List<Coordinate> uncollectedKeys() {
        return mWorldMap.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof LavaTrap &&
                        ((LavaTrap) entry.getValue()).getKey() > 0 &&
                        !(mCar.getKeys().contains(((LavaTrap) entry.getValue()).getKey())))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Get the car's current position as CarController's getPosition method returns a string.
     * @return
     */
    private Coordinate getCarPosition() {
        return new Coordinate(Math.round(mCar.getX()), Math.round(mCar.getY()));
    }

    /**
     * Update the world map based on the car's current view.
     */
    private void updateWorldMap() {
        HashMap<Coordinate, MapTile> relevantView = mCar.getView();
        relevantView.entrySet().removeIf(entry -> entry.getValue().isType(MapTile.Type.EMPTY));
        mWorldMap.putAll(relevantView);
    }
}
