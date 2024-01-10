package poly;

import battlecode.common.*;
import java.util.Random;

public class Duck {
    //rulebook:
    // if there is a function that you use often that costs bytecode, but actually the information only updates either once or
    // every couple of rounds, make a variable and write to it once (or until it needs updating) and then read from it
    RobotController rc;
    Lib lib;
    Navigation nav;
    Direction directionGoing = Direction.CENTER; //center is essentially our null for directions, but it is actually a direction
    MapLocation locationGoing = Lib.noLoc;

    MapLocation spawnLocation = Lib.noLoc;

    SkillType duckSkill; //the skill that the duck is supposed to level up
    static final Random rng = new Random(6147);

    public Duck(RobotController robot){
        rc = robot;
        nav = new Navigation(rc);
        lib = new Lib(rc);
    }

    public void takeTurn() throws GameActionException{
        if (!rc.isSpawned()){

            // Pick a random spawn location to attempt spawning in.
            MapLocation randomLoc = lib.spawnLocations[rng.nextInt(lib.spawnLocations.length)];
            // for now this is random, but in the future, we spawn where it is most needed
            for(Direction dir : lib.startDirList(lib.dirToIndex(rc.getLocation().directionTo(lib.mapCenter())), 0)) {
                if (rc.canSpawn(rc.getLocation().add(dir))) rc.spawn(rc.getLocation().add(dir));
                spawnLocation = rc.getLocation().add(dir);
                directionGoing = rc.getLocation().directionTo(lib.mapCenter());
            }
        }
        else{
            //we want to explore around so
            if(directionGoing == Direction.CENTER){
                //we don't want it to not do anything, but most likely that won't happen for now
            }
        }
    }

    void move() throws GameActionException {
        if(locationGoing != Lib.noLoc) {
            if (directionGoing != Direction.CENTER) {
                nav.tryMove(directionGoing);
            }
        }
    }
}