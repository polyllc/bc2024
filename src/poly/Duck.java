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

    enum Jobs {
        GETTINGFLAG,
        RETRIEVINGFLAG,
        GETTINGCRUMBS,
        IDLING
    }

    Jobs job;

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
                    job = Jobs.IDLING;
                    break;
                }
            }
        }
        else{
            //we want to explore around so
            if(directionGoing == Direction.CENTER){
                //we don't want it to not do anything, but most likely that won't happen for now
            }

            if(job == Jobs.IDLING) {
                MapLocation[] crumbs = rc.senseNearbyCrumbs(20);
                if (crumbs.length > 0) {
                    crumbPlace = crumbs[0];
                    locationGoing = crumbs[0];
                    job = Jobs.GETTINGCRUMBS;
                }
            }



            if(job == Jobs.GETTINGCRUMBS) {
                if (crumbPlace == locationGoing) {
                    if (rc.canSenseLocation(crumbPlace)) {
                        if (rc.senseMapInfo(crumbPlace).getCrumbs() == 0) {
                            crumbPlace = Lib.noLoc;
                            locationGoing = Lib.noLoc;
                            directionGoing = Lib.directions[rng.nextInt(8)];
                            job = Jobs.IDLING;
                        }
                    }
                }

                if (lib.surroundedByWater(crumbPlace)) {
                    if (rc.getCrumbs() > 10) {
                        if (rc.getLocation().distanceSquaredTo(crumbPlace) < 6) {
                            MapLocation ahead = rc.getLocation().add(rc.getLocation().directionTo(crumbPlace));
                            if (rc.canFill(ahead)) {
                                rc.fill(ahead);
                            }
                        }
                    }
                }
            }




            if(crumbPlace == Lib.noLoc){
                if(rc.getRoundNum() > 200){
                    FlagInfo[] nearestFlags = lib.getNearestFlags(rc.getLocation());
                    if(nearestFlags.length > 0){ //currently the array is just 1 length, so we can just grab the first one
                        locationGoing = nearestFlags[0].getLocation();
                    }
                }
            }

            move();
            rc.setIndicatorString(String.valueOf(directionGoing));
        }
        if(rc.getRoundNum() > 500){
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