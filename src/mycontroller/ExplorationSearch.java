package mycontroller;

import tiles.*;
import utilities.Coordinate;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static world.WorldSpatial.Direction;

/**
 * ExplorationSearch aims to reveal more of the map while avoiding damage to the vehicle
 * It is based heavily on AStarSearch.java
 * So far, mNeedHealing is not used
 */
public class ExplorationSearch extends SearchStrategy {
    private List<RelativeDirection> mDirections;
    private Path mPath;
    private List<Path> mPastPaths;
    private boolean mNeedHealing; // determines whether to target healing tiles

    public ExplorationSearch() {
        mDirections = new ArrayList<>();
        mPath = new Path();
        mPastPaths = new ArrayList<>();
    }

    @Override
    public void startSearch(HashMap<Coordinate, MapTile> map, Direction initialOrientation,
                            Coordinate initialPos, List<Coordinate> intermediateGoals,
                            List<Coordinate> finalGoals, Set<Integer> keysCollected) {
        // Reset the directions as we're starting a new search
        mDirections = new ArrayList<>();
        mPath = new Path();
        mNeedHealing = false;

        if (intermediateGoals.isEmpty() && finalGoals.isEmpty()) {
            return;
        }

        if (intermediateGoals.isEmpty()) {
            mPath = (search(map, initialPos, nearestFinalGoal(initialPos, finalGoals)));
        } else {
            // Go to the nearest intermediateGoal with no damage available
            intermediateGoals.sort(Comparator.comparingInt(c -> manhattanDistance(initialPos, c)));

            List<Path> intermediatePaths = new ArrayList();
            mPath = search(map, initialPos, intermediateGoals.get(0));
            int minDamage = mPath.damage;

            for (int i = 0; i < intermediateGoals.size(); i++) {

                intermediatePaths.add(search(map, initialPos, intermediateGoals.get(i)));
                int damage = intermediatePaths.get(i).damage;
                if (damage <= minDamage) {
                    mPath = intermediatePaths.get(i).clone();
                    minDamage = damage;
                    if (damage <= 0)
                        break;
                }
            }
        }

        if (!mPastPaths.contains(mPath)) {
            System.out.println("Adding path to history");
            Path pathCpy = mPath.clone();
            mPastPaths.add(pathCpy);
            if (mPastPaths.size() > 3) {
                mPastPaths = mPastPaths.subList(1, mPastPaths.size());
            }
        } else {
            // AI is thrashing
            System.out.println("AI is thrashing");
//            int randIdx = ThreadLocalRandom.current().nextInt(0, mPastPaths.size());
            mPath = mPastPaths.get(mPastPaths.size() - 1);
//            mPath = search(map, initialPos, nearestFinalGoal(initialPos, finalGoals));
        }
        mDirections = toRelativeDirection(mPath.path, initialOrientation);
    }

    /**
     * Find the final goal nearest to a given position
     *
     * @param pos        Position to find the nearest final goal to
     * @param finalGoals Choice of final goals
     * @return Coordinates of closest final goal
     */
    private Coordinate nearestFinalGoal(Coordinate pos, List<Coordinate> finalGoals) {
        Coordinate nearest = finalGoals.stream()
                .min(Comparator.comparingInt(c -> manhattanDistance(pos, c)))
                .get();
        return nearest;
    }

    @Override
    public List<RelativeDirection> getDirections() {
        return mDirections;
    }

    @Override
    public List<Coordinate> getPath() {
        return mPath.path;
    }

    /**
     * Run an A* search between two points
     *
     * @param map   Map we're navigating on
     * @param start Coordinate to start search from
     * @param goal  Coordinate to end search on
     * @return List of coordinates to get from start to goal
     */
    Path search(HashMap<Coordinate, MapTile> map, Coordinate start, Coordinate goal) {
        // Evaluated nodes
        List<Coordinate> closedSet = new ArrayList<>();
        // Discovered but unevaluated nodes
        List<Coordinate> openSet = new ArrayList<>(Collections.singleton(start));

        Map<Coordinate, Coordinate> cameFrom = new HashMap<>();

        Map<Coordinate, Integer> gScore = new DefaultDict<>(Integer.MAX_VALUE);
        gScore.put(start, 0);

        Map<Coordinate, Integer> fScore = new DefaultDict<>(Integer.MAX_VALUE);
        fScore.put(start, heuristicCost(start, goal));

        // damage score, number of Lava tiles traversed
        Map<Coordinate, Integer> dScore = new HashMap<>();
        dScore.put(start, 0);

        Path path = new Path();
        while (!openSet.isEmpty()) {
            Coordinate current = openSet.stream().min(Comparator.comparingInt(dScore::get)).get();
            if (current.equals(goal)) {
                path = reconstructPath(cameFrom, current, dScore);
                break;
            }

            openSet.remove(current);
            closedSet.add(current);

            for (Coordinate neighbour : getNeighbours(current, map)) {
                if (closedSet.contains(neighbour)) {
                    continue;
                }
                int tentativeGScore = gScore.get(current) + 1;
                int tentativeDScore = dScore.get(current);
                if (map.get(neighbour) instanceof HealthTrap) {
                    if (mNeedHealing) // Check if the car needs repairs, if not this tile is no better than Road
                        tentativeDScore -= HealthTrap.HealthDelta;
                } else if (map.get(neighbour) instanceof LavaTrap) {
                    tentativeDScore += LavaTrap.HealthDelta;
                } else if (map.get(neighbour).isType(MapTile.Type.UTILITY)) {
                    tentativeGScore -= 1; // prioritise unknown tiles since we are exploring
                } else if (map.get(neighbour) instanceof TrapTile) {
                    tentativeGScore += 100;
                }

                if (!openSet.contains(neighbour)) {
                    openSet.add(neighbour);
                } else if (tentativeDScore > dScore.get(neighbour) ||
                        (tentativeDScore == dScore.get(neighbour) && tentativeGScore >= gScore.get(neighbour))) {
                    continue;
                }
                cameFrom.put(neighbour, current);
                gScore.put(neighbour, tentativeGScore);
                fScore.put(neighbour, gScore.get(neighbour) + heuristicCost(neighbour, goal));
                dScore.put(neighbour, tentativeDScore);
            }
        }
        path.reverse();
        return path;
    }

    /**
     * Get all the neighbours for the given node
     *
     * @param current Node to get neighbours from
     * @param map     Map on which we can find the neighbours
     * @return Node's neighbours
     */
    private List<Coordinate> getNeighbours(Coordinate current, HashMap<Coordinate, MapTile> map) {
        List<Coordinate> possibleNeighbours = new ArrayList<>(Arrays.asList(
                new Coordinate(current.x + 1, current.y),
                new Coordinate(current.x - 1, current.y),
                new Coordinate(current.x, current.y + 1),
                new Coordinate(current.x, current.y - 1)
        ));

        return possibleNeighbours.stream().filter(coordinate -> {
            MapTile tile = map.get(coordinate);
            return tile != null && !(tile.isType(MapTile.Type.WALL) || tile instanceof MudTrap || tile.isType(MapTile.Type.EMPTY));
        }).collect(Collectors.toList());
    }

    /**
     * Heuristic cost between two coordinates. Here we use the Manhattan distance between the two coordinates as there
     * the actual cost will be no lower than this.
     *
     * @param coord1 Coordinate we're starting at.
     * @param coord2 Coordinate we're ending at.
     * @return Heuristic cost of doing from coord1 to coord2
     */
    private int heuristicCost(Coordinate coord1, Coordinate coord2) {
        return manhattanDistance(coord1, coord2);
    }

    private int manhattanDistance(Coordinate coordinate1, Coordinate coordinate2) {
        return Math.abs(coordinate1.x - coordinate2.x) + Math.abs(coordinate1.y - coordinate2.y);
    }

    /**
     * Reconstruct the path using the map of nodes built during the search.
     *
     * @param cameFrom Map of shortest paths from one node to another
     * @param current  Node to start constructing path from
     * @return Reconstructed path
     */
    private Path reconstructPath(Map<Coordinate, Coordinate> cameFrom, Coordinate current, Map<Coordinate, Integer> damage) {
        Path path = new Path();
        path.damage = damage.get(current);
        path.path = new ArrayList<>(Collections.singleton(current));
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(current, 0);
        }
        return path;
    }

}
