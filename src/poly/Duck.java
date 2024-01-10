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
    MapLocation crumbPlace = Lib.noLoc;

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
            for(Direction dir : lib.startDirList(lib.dirToIndex(randomLoc.directionTo(lib.mapCenter())), 0)) {
                if (rc.canSpawn(randomLoc.add(dir))) {
                    rc.spawn(randomLoc.add(dir));
                    spawnLocation = randomLoc.add(dir);
                    directionGoing = randomLoc.add(dir).directionTo(lib.mapCenter());
                    break;
                }
            }
        }
        else{
            //we want to explore around so
            if(directionGoing == Direction.CENTER){
                //we don't want it to not do anything, but most likely that won't happen for now
            }

            MapLocation[] crumbs = rc.senseNearbyCrumbs(20);
            if(crumbs.length > 0){
                crumbPlace = crumbs[0];
                locationGoing = crumbs[0];
            }
            if(crumbPlace == locationGoing){
                if(rc.canSenseLocation(crumbPlace)){
                    if(rc.senseMapInfo(crumbPlace).getCrumbs() == 0){
                        crumbPlace = Lib.noLoc;
                        locationGoing = Lib.noLoc;
                        directionGoing = Lib.directions[rng.nextInt(8)];
                    }
                }
            }

            move();
            rc.setIndicatorString(String.valueOf(directionGoing));
        }
        if(rc.getRoundNum() > 200){
            rc.resign();
        }
    }

    void move() throws GameActionException {
        if(locationGoing == Lib.noLoc) {
            if (directionGoing != Direction.CENTER) {
                nav.goTo(directionGoing);
            }
        }
        else{
            nav.goTo(locationGoing);
        }
    }
}