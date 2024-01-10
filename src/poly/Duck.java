package poly;

import battlecode.common.*;
public class Duck {
    //rulebook:
    // if there is a function that you use often that costs bytecode, but actually the information only updates either once or
    // every couple of rounds, make a variable and write to it once (or until it needs updating) and then read from it
    RobotController rc;
    Lib lib;
    Navigation nav;

    public Duck(RobotController robot){
        rc = robot;
        nav = new Navigation(rc);
        lib = new Lib(rc);
    }

    public void takeTurn() throws GameActionException{
        if (!rc.isSpawned()){

            // Pick a random spawn location to attempt spawning in.
            for(Direction dir : lib.startDirList(lib.dirToIndex(rc.getLocation().directionTo(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2))), 0)) {
                if (rc.canSpawn(rc.getLocation().add(dir))) rc.spawn(rc.getLocation().add(dir));
            }
        }
        else{

        }
    }
}