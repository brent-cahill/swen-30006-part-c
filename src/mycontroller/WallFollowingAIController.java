package mycontroller;

import controller.CarController;
import tiles.LavaTrap;
import tiles.MapTile;
import tiles.MudTrap;
import tiles.TrapTile;
import utilities.Coordinate;
import world.Car;
import world.WorldSpatial;

import java.util.HashMap;

/**
 * Simple AI that just follows the wall. This is based on the provided AIController class.
 */
public class WallFollowingAIController extends CarController {
    public static final int CAR_MAX_SPEED = 1;
    public static final int CAR_MAX_REVERSE_SPEED = -1;
    private boolean mIsFollowingWall = false;
    private boolean mIsReversing = false;
    protected Car mCar;
    private int mWallSensitivity = 1;

    public WallFollowingAIController(Car car) {
        super(car);
        mCar = car;
    }

    @Override
    public void update() {
        HashMap<Coordinate, MapTile> currentView = getView();

        if (getSpeed() < CAR_MAX_SPEED && getSpeed() > CAR_MAX_REVERSE_SPEED) {

            if (mIsReversing)
                applyReverseAcceleration();
            else
                applyForwardAcceleration();
        }

        if (mIsFollowingWall) {
            // Keep a wall on our (relative) left
            if (mIsReversing && !checkWallRight(getOrientation(), currentView)) {
                turnRight();
            } else if (!mIsReversing && !checkWallLeft(getOrientation(), currentView)) {
                turnLeft();
            } else {
                if (!mIsReversing && checkWallAhead(getOrientation(), currentView) || mIsReversing && checkWallBehind(getOrientation(), currentView)) {
                    // there is a wall ahead and to the left
                    // turn right if possible
                    if (mIsReversing && !checkWallLeft(getOrientation(), currentView))
                        turnLeft();
                    else if (!mIsReversing && !checkWallRight(getOrientation(), currentView))
                        turnRight();
                    else {// have to change direction
                        if (mIsReversing) {
                            applyForwardAcceleration();
                            mIsReversing = false;
                        } else {
                            applyReverseAcceleration();
                            mIsReversing = true;
                        }
                    }
                }
            }
        } else {
            if (!mIsReversing && checkWallAhead(getOrientation(), currentView)
                    || mIsReversing && checkWallBehind(getOrientation(), currentView)) {

                // Found a wall, put it on our (relative) left and follow it
                mIsFollowingWall = true;

                if (mIsReversing) {
                    if (!checkWallLeft(getOrientation(), currentView)) {
                        turnLeft();
                    } else {
                        applyForwardAcceleration();
                        mIsReversing = false;
                    }
                } else {
                    if (!checkWallRight(getOrientation(), currentView)) {
                        turnRight();
                    } else {
                        applyReverseAcceleration();
                        mIsReversing = true;
                    }
                }
            }
        }
    }

    public void setFollowing(boolean f) {mIsFollowingWall = f;}

    /**
     * Check if you have a wall in front of you!
     *
     * @param orientation the orientation we are in based on WorldSpatial
     * @param currentView what the car can currently see
     * @return
     */
    private boolean checkWallAhead(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView) {
        switch (orientation) {
            case EAST:
                return checkEast(currentView);
            case NORTH:
                return checkNorth(currentView);
            case SOUTH:
                return checkSouth(currentView);
            case WEST:
                return checkWest(currentView);
            default:
                return false;
        }
    }

    private boolean checkWallBehind(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView) {
        return checkWallAhead(WorldSpatial.reverseDirection(orientation), currentView);
    }

    /**
     * Check if the wall is on your left hand side given your orientation
     *
     * @param orientation
     * @param currentView
     * @return
     */
    private boolean checkWallLeft(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView) {

        switch (orientation) {
            case EAST:
                return checkNorth(currentView);
            case NORTH:
                return checkWest(currentView);
            case SOUTH:
                return checkEast(currentView);
            case WEST:
                return checkSouth(currentView);
            default:
                return false;
        }
    }

    private boolean checkWallRight(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView) {

        switch (orientation) {
            case EAST:
                return checkSouth(currentView);
            case NORTH:
                return checkEast(currentView);
            case SOUTH:
                return checkWest(currentView);
            case WEST:
                return checkNorth(currentView);
            default:
                return false;
        }
    }

    /**
     * Method below just iterates through the list and check in the correct coordinates.
     * i.e. Given your current position is 10,10
     * checkEast will check up to mWallSensitivity amount of tiles to the right.
     * checkWest will check up to mWallSensitivity amount of tiles to the left.
     * checkNorth will check up to mWallSensitivity amount of tiles to the top.
     * checkSouth will check up to mWallSensitivity amount of tiles below.
     */
    public boolean checkEast(HashMap<Coordinate, MapTile> currentView) {
        // Check tiles to my right
        Coordinate currentPosition = new Coordinate(getPosition());
        for (int i = 0; i <= mWallSensitivity; i++) {
            MapTile tile = currentView.get(new Coordinate(currentPosition.x + i, currentPosition.y));
            if (tile.isType(MapTile.Type.WALL) || tile instanceof  MudTrap) {
                return true;
            }
        }
        return false;
    }

    public boolean checkWest(HashMap<Coordinate, MapTile> currentView) {
        // Check tiles to my left
        Coordinate currentPosition = new Coordinate(getPosition());
        for (int i = 0; i <= mWallSensitivity; i++) {
            MapTile tile = currentView.get(new Coordinate(currentPosition.x - i, currentPosition.y));
            if (tile.isType(MapTile.Type.WALL) || tile instanceof  MudTrap) {
                return true;
            }
        }
        return false;
    }

    public boolean checkNorth(HashMap<Coordinate, MapTile> currentView) {
        // Check tiles to towards the top
        Coordinate currentPosition = new Coordinate(getPosition());
        for (int i = 0; i <= mWallSensitivity; i++) {
            MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y + i));
            if (tile.isType(MapTile.Type.WALL) || tile instanceof  MudTrap) {
                return true;
            }
        }
        return false;
    }

    public boolean checkSouth(HashMap<Coordinate, MapTile> currentView) {
        // Check tiles towards the bottom
        Coordinate currentPosition = new Coordinate(getPosition());
        for (int i = 0; i <= mWallSensitivity; i++) {
            MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y - i));
            if (tile.isType(MapTile.Type.WALL) || tile instanceof  MudTrap) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check for adjacent walls and start following them if possible
     * Not complete yet.
     * @param currentView
     *
     */
    public void attachToWall(HashMap<Coordinate, MapTile> currentView) {
        boolean left = checkWallLeft(getOrientation(), currentView);
        boolean right = checkWallRight(getOrientation(), currentView);
        boolean ahead = checkWallAhead(getOrientation(), currentView);
        boolean behind = checkWallBehind(getOrientation(), currentView);

        // first check if we are following a wall without realising
        if ((!mIsReversing && left)
                || (mIsReversing && right)) {
            mIsFollowingWall = true;
            return;
        } else if ((!mIsReversing && right) // or if there is a wall on the wrong side
                || (mIsReversing && left)) {
            mIsFollowingWall = true;
            mIsReversing = !mIsReversing;
            return;
        }

        if (!mIsReversing && ahead || mIsReversing && behind) {

            // Found a wall, put it on our (relative) left and follow it
            mIsFollowingWall = true;

            if (mIsReversing) {
                if (!left) {
                    turnLeft();
                } else {
                    applyForwardAcceleration();
                    mIsReversing = false;
                }
            } else {
                if (!right) {
                    turnRight();
                } else {
                    applyReverseAcceleration();
                    mIsReversing = true;
                }
            }
        }
    }
}
