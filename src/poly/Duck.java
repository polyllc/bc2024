package poly;

import battlecode.common.*;


import java.util.Arrays;
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
    boolean lastMovement = false;

    int turnsMovingInDirection = 0;


    int flagCarrierIndex = 0;

    enum Jobs {
        GETTINGFLAG,
        RETRIEVINGFLAG,
        GETTINGCRUMBS,
        IDLING,
        FINDINGFLAG,
        GUARDINGFLAGHOLDER
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
         //   System.out.println(Arrays.toString(lib.spawnLocations));
            // for now this is random, but in the future, we spawn where it is most needed
            for(MapLocation loc : lib.spawnLocations) {
                for (Direction dir : Lib.directions) {
                    if (rc.canSpawn(loc.add(dir))) {
                        rc.spawn(loc.add(dir));
                        spawnLocation = loc.add(dir);
                        directionGoing = loc.add(dir).directionTo(lib.mapCenter());
                        job = Jobs.IDLING;
                        break;
                    }
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

                if(rc.getRoundNum() > 200){
                    MapLocation nearestFlagCarrier = lib.getNearestFlagCarrier();
                    if(!nearestFlagCarrier.equals(Lib.noLoc) && !nearestFlagCarrier.equals(Lib.noFlag)){
                        job = Jobs.GUARDINGFLAGHOLDER;
                        flagCarrierIndex = lib.getFlagIndex(nearestFlagCarrier);
                        locationGoing = nearestFlagCarrier;
                    }

                    FlagInfo[] nearestFlags = lib.getNearestFlags(rc.getLocation());
                    System.out.println(Arrays.toString(nearestFlags));
                    if(nearestFlags.length > 0){ //currently the array is just 1 length, so we can just grab the first one
                        locationGoing = nearestFlags[0].getLocation();
                        job = Jobs.GETTINGFLAG;
                        flagCarrierIndex = lib.getNextClearFlagIndex(); //we pray we hope, this will never be 0! (0, not 1)
                    }
                }
            }



            if(job == Jobs.GETTINGCRUMBS) {
                if(rc.getRoundNum() > 200){
                    //job = Jobs.IDLING;
                }
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

                if(rc.getRoundNum() > 200){
                    FlagInfo[] nearestFlags = lib.getNearestFlags(rc.getLocation());
                    if(nearestFlags.length > 0){ //currently the array is just 1 length, so we can just grab the first one
                        locationGoing = nearestFlags[0].getLocation();
                        job = Jobs.GETTINGFLAG;
                        flagCarrierIndex = lib.getNextClearFlagIndex(); //we pray we hope, this will never be 0! (0, not 1)
                    }

                    MapLocation nearestFlagCarrier = lib.getNearestFlagCarrier();
                    if(!nearestFlagCarrier.equals(Lib.noLoc) && !nearestFlagCarrier.equals(Lib.noFlag)){
                        job = Jobs.GUARDINGFLAGHOLDER;
                        flagCarrierIndex = lib.getFlagIndex(nearestFlagCarrier);
                        locationGoing = nearestFlagCarrier;
                    }
                }
            }

            if(job == Jobs.GETTINGFLAG){

                lib.setEnemyFlagLoc(rc.getLocation(), flagCarrierIndex);

                if(rc.getLocation().distanceSquaredTo(locationGoing) <= 2){
                    for(Direction dir : lib.startDirList(lib.dirToIndex(rc.getLocation().directionTo(locationGoing)), 0)){
                        if(rc.canPickupFlag(locationGoing)){
                            rc.pickupFlag(locationGoing);
                            job = Jobs.RETRIEVINGFLAG;
                            locationGoing = lib.getNearestSpawns(rc.getLocation())[0];
                            lib.setEnemyFlagLoc(Lib.noLoc, flagCarrierIndex);
                            flagCarrierIndex = 0;
                        }
                    }
                }
                if(!rc.hasFlag()){
                    job = Jobs.IDLING;
                    locationGoing = Lib.noLoc;
                    directionGoing = Lib.directions[rng.nextInt(8)];
                    lib.setEnemyFlagLoc(Lib.noLoc, flagCarrierIndex);
                    flagCarrierIndex = 0;
                }
            }

            if(job == Jobs.RETRIEVINGFLAG){
                lib.setEnemyFlagLoc(rc.getLocation(), flagCarrierIndex);
                if(lib.contains(rc.getAllySpawnLocations(), rc.getLocation())){
                    job = Jobs.IDLING;
                    if(rc.canDropFlag(rc.getLocation())) {
                        rc.dropFlag(rc.getLocation());
                    }
                    locationGoing = Lib.noLoc;
                    directionGoing = Lib.directions[rng.nextInt(8)];
                }
                if(!rc.hasFlag()){
                    job = Jobs.IDLING;
                    locationGoing = Lib.noLoc;
                    directionGoing = Lib.directions[rng.nextInt(8)];
                    lib.setEnemyFlagLoc(Lib.noLoc, flagCarrierIndex);
                    flagCarrierIndex = 0;
                }
            }

           rc.setIndicatorString("location Going: " + locationGoing + " , Job: " + job + " last: " + lastMovement);

            if(turnsMovingInDirection > (rc.getMapHeight() + rc.getMapWidth())){
                switch(rc.getRoundNum() % 3){
                    case 0: directionGoing = directionGoing.opposite(); break;
                    case 1: directionGoing = directionGoing.opposite().rotateLeft(); break;
                    case 2: directionGoing = directionGoing.opposite().rotateRight(); break;
                }
                turnsMovingInDirection = 0;
            }

            if(job == Jobs.GUARDINGFLAGHOLDER){ //todo, if the flag holder dies, well this doesn't update, so do that
                MapLocation flagHolder = lib.getEnemyFlagLoc(flagCarrierIndex);
                if(!flagHolder.equals(Lib.noFlag) && !flagHolder.equals(Lib.noLoc)){
                    locationGoing = flagHolder;
                }
                else {
                    locationGoing = Lib.noLoc;
                    directionGoing = Lib.directions[rng.nextInt(8)];
                    flagCarrierIndex = 0;
                    job = Jobs.IDLING;
                }
            }


            attack();

           // if(lib.

            move();

            //lib.printSharedArray(8);


        }
        if(rc.getRoundNum() > 500){
           // rc.resign();
        }
    }

    void move() throws GameActionException {
        if(lib.detectCorner(directionGoing)){
            directionGoing = rc.getLocation().directionTo(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2));
            //directionGoing = directionGoing.opposite();
        }
        if(locationGoing == Lib.noLoc) {
            if (directionGoing != Direction.CENTER) {
                nav.goTo(directionGoing);
                turnsMovingInDirection++;
            }
        }
        else{
           lastMovement = nav.goTo(locationGoing, false); //if we need to save bytecode, well this is where we're saving it
           if(!lastMovement){
               lastMovement = nav.bugNavTo(locationGoing);
               if(!lastMovement){
                   lastMovement = nav.navTo(locationGoing);
                   if(!lastMovement){
                      // lastMovement = nav.goTo(rc.getLocation().directionTo(locationGoing));
                   }
               }
           }
           turnsMovingInDirection = 0;
        }
    }

    void attack() throws GameActionException{
        RobotInfo[] robotInfos = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if(robotInfos.length > 0){
            if(rc.canAttack(robotInfos[0].getLocation())){
                rc.attack(robotInfos[0].getLocation());
            }
        }
    }
}