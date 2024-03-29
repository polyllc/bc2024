package poly;

import battlecode.common.*;
import battlecode.world.Flag;


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
    MapLocation lastLocationGoing = Lib.noLoc;

    MapLocation spawnLocation = Lib.noLoc;
    MapLocation crumbPlace = Lib.noLoc;
    boolean lastMovement = false;

    MapLocation comingFromFlag = Lib.noLoc;

    int turnsMovingInDirection = 0;
    int guardTime = 0;

    boolean stopMoving = false;


    int groupNumber = 0;

    int spawnRound = 0;

    int flagCarrierIndex = 0;

    enum Jobs {
        GETTINGFLAG,
        RETRIEVINGFLAG,
        GETTINGCRUMBS,
        IDLING,
        FINDINGFLAG,
        GUARDINGFLAGHOLDER, //guarding a flag holder that holds an enemy flag
        GUARDINGFLAG, //the duck that sits at our flag all game
        DEFENDINGFLAG //defending our flag at base
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
                        if(rc.getRoundNum() <= 200) {
                            directionGoing = loc.add(dir).directionTo(lib.mapCenter()); //spawn towards flags
                        }
                        else {
                            if(lib.getNearestEnemyCenter(rc.getLocation(), lib.nextPointToSpawn()) != Lib.noLoc){
                                directionGoing = loc.add(dir).directionTo(lib.getNearestEnemyCenter(rc.getLocation(), lib.nextPointToSpawn()));
                            }
                            else {
                                directionGoing = loc.add(dir).directionTo(lib.mapCenter());
                            }
                        }
                        job = Jobs.IDLING;
                        spawnRound = rc.getRoundNum();
                        break;
                    }
                }
            }

        }
        else{

            if(rc.getRoundNum() > 5 && groupNumber == 0){
                groupNumber = lib.getGroupNumber();
            }

            if(rc.getRoundNum() == 5){
                lib.preliminaryAutofillEnemySpawnPoints();
            }


            if(rc.canBuyGlobal(GlobalUpgrade.ACTION)){
                rc.buyGlobal(GlobalUpgrade.ACTION);
            }
            if(rc.canBuyGlobal(GlobalUpgrade.HEALING)){
                rc.buyGlobal(GlobalUpgrade.HEALING);
            }
            if(rc.canBuyGlobal(GlobalUpgrade.CAPTURING)){
                rc.buyGlobal(GlobalUpgrade.CAPTURING);
            }

            // assigns a duck to stay on the flag
            FlagInfo[] flagInfos = rc.senseNearbyFlags(-1, rc.getTeam());
            if(flagInfos.length > 0) {
                if(rc.canSenseLocation(flagInfos[0].getLocation())) {
                    if(rc.senseRobotAtLocation(flagInfos[0].getLocation()) == null || rc.getLocation().equals(flagInfos[0].getLocation())) {
                        job = Jobs.GUARDINGFLAG;
                    }
                }
            }

            // assigns skillTypes
            if(duckSkill != SkillType.ATTACK && duckSkill != SkillType.HEAL){
              if(rc.readSharedArray(23) % 2 == 0) {
                  duckSkill = SkillType.HEAL;
                  rc.writeSharedArray(23, 1);
              }
              else {
                  duckSkill = SkillType.ATTACK;
                  rc.writeSharedArray(23, 0);
              }
            }

            if(rc.getRoundNum() == 2){
                lib.setAllySpawnZones(rc.getLocation());
            }

            //we want to explore around so
            if(directionGoing == Direction.CENTER){
                //we don't want it to not do anything, but most likely that won't happen for now
            }

            // drop traps around enemy spawn points
                /*if(job == Jobs.IDLING || job == Jobs.GUARDINGFLAGHOLDER){
                for(MapLocation loc : lib.enemySpawnZones()){
                    for(Direction dir: Lib.directions){
                        if(rc.getLocation() == loc.add(dir).add(dir).add(dir)){
                            if(rc.canBuild(TrapType.STUN, loc.add(dir).add(dir).add(dir))) {
                                rc.build(TrapType.STUN, loc.add(dir).add(dir).add(dir));
                            }
                        }
                    }

                }
            } */

            if(rc.getRoundNum() < 250 && rc.getRoundNum() > 150 && job == Jobs.IDLING){
                job = Jobs.FINDINGFLAG;
                locationGoing = lib.enemySpawnZones()[groupNumber == 0 ? 0 : (groupNumber == 3) ? 0 : (groupNumber == 2) ? 1 : (groupNumber == 1) ? 2 : 0];
            }
            if(job == Jobs.FINDINGFLAG){

               // if(rc.getRoundNum() > 200){
                    findFlag();
                    MapLocation nearestFlagCarrier = lib.getNearestFlagCarrier();
                    if(rc.getRoundNum() > 250 + (rc.getMapHeight() + rc.getMapWidth())/2 || locationGoing.distanceSquaredTo(nearestFlagCarrier) < 10) {
                        if (!nearestFlagCarrier.equals(Lib.noLoc) && !nearestFlagCarrier.equals(Lib.noFlag)) {
                            job = Jobs.GUARDINGFLAGHOLDER;
                            flagCarrierIndex = lib.getFlagIndex(nearestFlagCarrier);
                            locationGoing = nearestFlagCarrier;
                        }
                    }
                    // directionGoing = getNextDirection();
              //  }

                if(rc.hasFlag()){
                    job = Jobs.RETRIEVINGFLAG;
                }

                if(rc.getLocation().distanceSquaredTo(locationGoing) < 10){
                    job = Jobs.IDLING;
                    locationGoing = Lib.noLoc;
                    directionGoing = rc.getLocation().directionTo(lib.getNearestEnemyCenter(rc.getLocation()));
                }

            }

            /*if(rc.hasFlag())
                for (MapInfo loc : rc.senseNearbyMapInfos()) {
                    if(lib.isEnemyCenter(loc.getMapLocation())){

                    }
                }*/


            if(job == Jobs.IDLING) {

//fix pathfindinb
                //bad exploration
                locationGoing = Lib.noLoc;
                MapLocation[] crumbs = rc.senseNearbyCrumbs(-1);
                if (crumbs.length > 0) {
                    crumbPlace = crumbs[0];
                    locationGoing = crumbs[0];
                    job = Jobs.GETTINGCRUMBS;
                }
                int x = lib.borderDetection();
                if(x != -1){
                    if(x == 0){
                        switch(this.directionGoing){
                            case EAST:
                            case NORTHEAST:
                                canAndMove(Direction.SOUTHEAST);
                                break;
                            case WEST:
                            case NORTHWEST:
                                canAndMove(Direction.SOUTHWEST);
                                break;
                            case NORTH:
                                canAndMove(Direction.SOUTH);
                        }
                    }
                    else if(x == 1){
                        switch(this.directionGoing){
                            case NORTH:
                            case NORTHWEST:
                                canAndMove(Direction.NORTHEAST);
                                break;
                            case SOUTH:
                            case SOUTHWEST:
                                canAndMove(Direction.SOUTHEAST);
                                break;
                            case WEST:
                                canAndMove(Direction.EAST);
                        }
                    }
                    else if(x == 2){
                        switch(this.directionGoing){
                            case NORTH:
                            case NORTHEAST:
                                canAndMove(Direction.NORTHWEST);
                                break;
                            case SOUTH:
                            case SOUTHEAST:
                                canAndMove(Direction.SOUTHWEST);
                                break;
                            case EAST:
                                canAndMove(Direction.WEST);
                        }
                    }
                    else if(x == 3){
                        switch(this.directionGoing){
                            case EAST:
                            case SOUTHEAST:
                                canAndMove(Direction.NORTHEAST);
                                break;
                            case WEST:
                            case SOUTHWEST:
                                canAndMove(Direction.NORTHWEST);
                                break;
                            case SOUTH:
                                canAndMove(Direction.NORTH);
                        }
                    }
                }


                if(rc.getRoundNum() > 500) {

                }

                if(rc.getRoundNum() > 200){
                    findFlag();
                    MapLocation nearestFlagCarrier = lib.getNearestFlagCarrier();
                    if(!nearestFlagCarrier.equals(Lib.noLoc) && !nearestFlagCarrier.equals(Lib.noFlag)){
                        job = Jobs.GUARDINGFLAGHOLDER;
                        flagCarrierIndex = lib.getFlagIndex(nearestFlagCarrier);
                        locationGoing = nearestFlagCarrier;
                    }
                   // directionGoing = getNextDirection();
                }

                if(rc.hasFlag()){
                    job = Jobs.RETRIEVINGFLAG;
                }


            }

            fill();

            if(job == Jobs.GETTINGCRUMBS) {
                if(rc.getRoundNum() > 200){
                    //job = Jobs.IDLING;
                }
                if (crumbPlace == locationGoing) {
                    if (rc.canSenseLocation(crumbPlace)) {
                        if (rc.senseMapInfo(crumbPlace).getCrumbs() == 0) {
                            crumbPlace = Lib.noLoc;
                            locationGoing = Lib.noLoc;
                            if(rc.getRoundNum() > 200) {
                                directionGoing = rc.getLocation().directionTo(lib.getNearestEnemyCenter(rc.getLocation()));
                            }
                            else {
                                directionGoing = rc.getLocation().directionTo(lib.mapCenter());
                            }
                            job = Jobs.IDLING;
                        }
                    }

                }

                if (lib.surroundedByWater(crumbPlace)) {
                    if (rc.getCrumbs() > 10) {
                       // if (rc.getLocation().distanceSquaredTo(crumbPlace) < 6) {
                            MapLocation ahead = rc.getLocation().add(rc.getLocation().directionTo(crumbPlace));
                            if (rc.canFill(ahead)) {
                                rc.fill(ahead);
                            }
                       // }
                    }
                }

                if(rc.getRoundNum() > 200){
                    findFlag();

                    MapLocation nearestFlagCarrier = lib.getNearestFlagCarrier();
                    if(!nearestFlagCarrier.equals(Lib.noLoc) && !nearestFlagCarrier.equals(Lib.noFlag)){
                        job = Jobs.GUARDINGFLAGHOLDER;
                        flagCarrierIndex = lib.getFlagIndex(nearestFlagCarrier);
                        locationGoing = nearestFlagCarrier;
                    }
                }
            }

            if(job == Jobs.GETTINGFLAG){
                //todo, a lot of ducks stuck on this, maybe also work on not assigning too many ducks on this?

                if(flagCarrierIndex == 0){
                    flagCarrierIndex = lib.getNextClearFlagIndex(locationGoing);
                }
                lib.setEnemyFlagLoc(rc.getLocation(), flagCarrierIndex);

                for (MapInfo loc : rc.senseNearbyMapInfos()) {
                    if(lib.isEnemyCenter(loc.getMapLocation())){
                        //get loc of loc going
                        //get loc of the current place
                        //pythagorean of one left vs one right
                        //locationGoing
                        if ((Math.sqrt (Math.pow (rc.getLocation().x + 1, 2) + Math.pow (rc.getLocation().y, 2))) >
                                (Math.sqrt (Math.pow (rc.getLocation().x - 1, 2) + Math.pow (rc.getLocation().y, 2)))) {
                            nav.goTo(rc.getLocation().directionTo(locationGoing), false);
                            //rc.move(Direction.EAST);
                            //rc.getLocation().directionTo(locationGoing)
                        }
                        else{
                            //rc.navTo
                            //wedt
                            nav.goTo(rc.getLocation().directionTo(locationGoing), true);
                        }
                    }
                }

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
                    if(lib.getNearestFlags(rc.getLocation()).length == 0){
                        job = Jobs.IDLING;
                        locationGoing = Lib.noLoc;
                        directionGoing = rc.getLocation().directionTo(lib.getNearestEnemyCenter(rc.getLocation()));
                        lib.setEnemyFlagLoc(Lib.noLoc, flagCarrierIndex);
                        flagCarrierIndex = 0;
                    }
                }
                if(lib.getNearestFlags(rc.getLocation()).length > 0 && lib.getNearestFlags(rc.getLocation())[0].isPickedUp()){ //todo fix this
                    job = Jobs.IDLING;
                    locationGoing = Lib.noLoc;
                    directionGoing = rc.getLocation().directionTo(lib.getNearestEnemyCenter(rc.getLocation()));
                    lib.setEnemyFlagLoc(Lib.noLoc, flagCarrierIndex);
                    flagCarrierIndex = 0;
                }
                if(rc.canSenseLocation(locationGoing)){
                    if(lib.getNearestFlags(rc.getLocation()).length == 0){
                        job = Jobs.IDLING;
                        locationGoing = Lib.noLoc;
                        directionGoing = rc.getLocation().directionTo(lib.getNearestEnemyCenter(rc.getLocation()));
                        lib.setEnemyFlagLoc(Lib.noLoc, flagCarrierIndex);
                        flagCarrierIndex = 0;
                    }
                    else {
                        RobotInfo flagBot = rc.senseRobotAtLocation(lib.getNearestFlags(rc.getLocation())[0].getLocation());
                        if(flagBot != null) {
                            if(flagBot.getTeam() == rc.getTeam()){
                                job = Jobs.IDLING;
                                locationGoing = Lib.noLoc;
                                directionGoing = rc.getLocation().directionTo(lib.getNearestEnemyCenter(rc.getLocation()));
                                lib.setEnemyFlagLoc(Lib.noLoc, flagCarrierIndex);
                                flagCarrierIndex = 0;
                            }
                        }
                    }
                }
            }

            if(job == Jobs.RETRIEVINGFLAG){ //todo, no one is listening to this ducks
                if(flagCarrierIndex == 0){
                    flagCarrierIndex = lib.getNextClearFlagIndex();
                }
                if(!lib.contains(rc.getAllySpawnLocations(), locationGoing)) {
                    locationGoing = lib.getNearestSpawns(rc.getLocation())[0];
                }
                lib.setEnemyFlagLoc(rc.getLocation(), flagCarrierIndex);
               // System.out.println("Set position to: " + lib.getEnemyFlagLoc(flagCarrierIndex) + " vs " + rc.getLocation() + " and flag num " + flagCarrierIndex);
                if(lib.contains(rc.getAllySpawnLocations(), rc.getLocation())){
                    job = Jobs.IDLING;
                    if(rc.canDropFlag(rc.getLocation())) {
                        rc.dropFlag(rc.getLocation());
                        lib.setEnemyFlagLoc(Lib.noLoc, flagCarrierIndex);
                    }
                    locationGoing = Lib.noLoc;
                    directionGoing = rc.getLocation().directionTo(lib.getNearestEnemyCenter(rc.getLocation()));
                }
               /* if(!rc.hasFlag()){
                    job = Jobs.IDLING;
                    locationGoing = Lib.noLoc;
                    directionGoing = rc.getLocation().directionTo(lib.getNearestEnemyCenter(rc.getLocation()));
                    lib.setEnemyFlagLoc(Lib.noLoc, flagCarrierIndex);
                    flagCarrierIndex = 0;
                }*/
            }

            if(job == Jobs.GUARDINGFLAG){
                // for the ducks that are guarding our flags (possibly on the flag itself)
                // if enemy within radii then announce that there's enemies
                // ASSUMES THAT THERE'S A DUCK ON THE FLAG
                // turn other ducks to defense??????????
                RobotInfo[] enemyRobots = lib.enemiesInRadius();
                if(enemyRobots.length > 0) {
                    if (rc.canWriteSharedArray(8, 1)) {
                        rc.writeSharedArray(8, 1);
                        rc.writeSharedArray(9, rc.getLocation().x);
                        rc.writeSharedArray(10, rc.getLocation().y);
                    }
                }
                else if(lib.getAllyAttacked().distanceSquaredTo(rc.getLocation()) < 5){
                    rc.writeSharedArray(8, 0);
                }

                if(flagInfos.length > 0){
                    locationGoing = flagInfos[0].getLocation();
                    if(rc.senseRobotAtLocation(locationGoing) != null){
                        if(rc.senseRobotAtLocation(locationGoing).getID() != rc.getID()){
                            job = Jobs.IDLING;
                            locationGoing = Lib.noLoc;
                            //System.out.println("going back to idle");
                        }
                    }
                }
                else {
                    job = Jobs.IDLING;
                    locationGoing = Lib.noLoc;
                }

            }

           rc.setIndicatorString("loc: " + locationGoing + " , Job: " + job + " gn: " + groupNumber + " near: " + lib.getNearestFlagCarrier() + directionGoing);

            if(turnsMovingInDirection > (rc.getMapHeight() + rc.getMapWidth())){
                switch(rng.nextInt(3)-1){
                    case 0: directionGoing = directionGoing.opposite(); break;
                    case 1: directionGoing = directionGoing.opposite().rotateLeft(); break;
                    case 2: directionGoing = directionGoing.opposite().rotateRight(); break;
                }

                turnsMovingInDirection = 0;
            }

            move();

            if(job == Jobs.GUARDINGFLAGHOLDER){

                if(rc.hasFlag()){
                    job = Jobs.RETRIEVINGFLAG;
                }

                findFlag();

                MapLocation flagHolder = lib.getEnemyFlagLoc(flagCarrierIndex);

                if(rc.canSenseLocation(flagHolder)) {
                    if (rc.canSenseRobotAtLocation(flagHolder)) {
                        RobotInfo holder = rc.senseRobotAtLocation(flagHolder);
                        if (holder != null) {
                            if (holder.getTeam() == rc.getTeam()) {
                                if (!holder.hasFlag) {
                                    if(guardTime > 5) {
                                        lib.setEnemyFlagLoc(Lib.noLoc, flagCarrierIndex);
                                        guardTime = 0;
                                    }
                                    flagHolder = Lib.noLoc;
                                }
                            } else {
                                if(guardTime > 5) {
                                    lib.setEnemyFlagLoc(Lib.noLoc, flagCarrierIndex);
                                    guardTime = 0;
                                }
                                flagHolder = Lib.noLoc;
                            }
                        } else {
                            if(guardTime > 5) {
                                lib.setEnemyFlagLoc(Lib.noLoc, flagCarrierIndex);
                                guardTime = 0;
                            }
                            flagHolder = Lib.noLoc;
                        }
                    } else {
                        if(guardTime > 5) {
                            lib.setEnemyFlagLoc(Lib.noLoc, flagCarrierIndex);
                            guardTime = 0;
                        }
                        flagHolder = Lib.noLoc;
                    }
                }

                if(lib.contains(lib.spawnLocations, flagHolder)){
                    locationGoing = Lib.noLoc;
                    directionGoing = rc.getLocation().directionTo(lib.getNearestEnemyCenter(rc.getLocation()));
                    flagCarrierIndex = 0;
                    job = Jobs.IDLING;
                }

                if(!flagHolder.equals(Lib.noFlag) && !flagHolder.equals(Lib.noLoc)){
                    locationGoing = flagHolder;
                    guardTime++;
                }
                else {
                    locationGoing = Lib.noLoc;
                    MapLocation nearestEnemyCenter = lib.getNearestEnemyCenter(rc.getLocation());
                    if(nearestEnemyCenter.equals(Lib.noLoc)){
                        directionGoing = rc.getLocation().directionTo(lib.mapCenter());
                    }
                    else {
                        directionGoing = rc.getLocation().directionTo(nearestEnemyCenter);
                    }
                   // System.out.println("Going to nearest enemy center: " + nearestEnemyCenter);
                    flagCarrierIndex = 0;
                    job = Jobs.IDLING;
                    guardTime = 0;
                }
            }

            if(job == Jobs.IDLING || job == Jobs.GUARDINGFLAGHOLDER || job == Jobs.GETTINGCRUMBS) {

                RobotInfo[] enemyRobots = lib.enemiesInRadius();
                RobotInfo robotInfo = lib.enemyHasFlag(enemyRobots);
                if(enemyRobots.length > 0 && robotInfo != null) {
                    if (rc.canWriteSharedArray(8, 1)) {
                        rc.writeSharedArray(8, 1);
                        rc.writeSharedArray(9, robotInfo.getLocation().x);
                        rc.writeSharedArray(10, robotInfo.getLocation().y);
                    }
                }
                else if(lib.getAllyAttacked().distanceSquaredTo(rc.getLocation()) < 5){
                    rc.writeSharedArray(8, 0);
                }

                if(rc.readSharedArray(8) == 1){
                    if(groupNumber == 1){
                        job = Jobs.DEFENDINGFLAG;
                        locationGoing = new MapLocation(rc.readSharedArray(9), rc.readSharedArray(10));
                    }
                }

            }

            if(job == Jobs.DEFENDINGFLAG){

                if(rc.readSharedArray(8) == 0){
                    job = Jobs.IDLING;
                    directionGoing = rc.getLocation().directionTo(lib.getNearestEnemyCenter(rc.getLocation()));
                    locationGoing = Lib.noLoc;
                }
                if(rc.readSharedArray(8) == 1){
                    if(groupNumber == 1 || groupNumber == 2){
                        job = Jobs.DEFENDINGFLAG;
                        locationGoing = new MapLocation(rc.readSharedArray(9), rc.readSharedArray(10));
                    }
                }

                if(flagInfos.length > 0){
                    if(rc.getRoundNum() < 400) {
                        //System.out.println("flag is nearby: " + flagInfos[0].isPickedUp());
                    }
                    //if(flagInfos[0].getTeam() == rc.getTeam()){
                        if(flagInfos[0].isPickedUp()){
                            locationGoing = flagInfos[0].getLocation();
                        }
                        else {
                            job = Jobs.IDLING;
                            directionGoing = rc.getLocation().directionTo(lib.getNearestEnemyCenter(rc.getLocation()));
                            locationGoing = Lib.noLoc;
                        }
                   //}
                }

                if(rc.canSenseLocation(locationGoing)){
                    if(rc.canSenseRobotAtLocation(locationGoing)){
                        if(!rc.senseRobotAtLocation(locationGoing).hasFlag) {
                            job = Jobs.IDLING;
                            directionGoing = rc.getLocation().directionTo(lib.getNearestEnemyCenter(rc.getLocation()));
                            locationGoing = Lib.noLoc;
                        }
                    }
                }
                if(flagInfos.length > 0) {
                    if (rc.getLocation().distanceSquaredTo(flagInfos[0].getLocation()) < 10) {
                        findFlag();
                    }
                }
            }

            placeTraps();
            healOrAttack();
            lib.enemySpawnPoints(rc.getLocation());


            senseFlags();

            if(rc.getRoundNum() % 20 == 0) System.out.println(Arrays.toString(lib.enemySpawnZones()));

           // rc.setIndicatorString(Arrays.toString(rc.senseNearbyFlags(-1, rc.getTeam().opponent())));
        }
        if(rc.getRoundNum() > 400){
          //  rc.resign();
        }


    }

    private void findFlag() throws GameActionException {
        FlagInfo[] nearestFlags = lib.getNearestFlags(rc.getLocation());
        //System.out.println(Arrays.toString(nearestFlags));
        if(nearestFlags.length > 0){ //currently the array is just 1 length, so we can just grab the first one
            locationGoing = nearestFlags[0].getLocation();
            job = Jobs.GETTINGFLAG;
            flagCarrierIndex = lib.getNextClearFlagIndex(); //we pray we hope, this will never be 0! (0, not 1)
            //System.out.println("flagCarrierIndex: " + flagCarrierIndex);
        }
    }

    void move() throws GameActionException {

        if(rc.getRoundNum() < 200) {
            stopMoving = lib.isNearDam(rc.getLocation());
        }
        else {
            stopMoving = false;
        }

        if(!stopMoving) {
            if (lib.detectCorner(directionGoing) || lib.detectCorner(rc.getLocation(), 5)) {
                // directionGoing = rc.getLocation().directionTo(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2));
                if (rng.nextBoolean()) {
                    directionGoing = directionGoing.rotateLeft().rotateLeft();
                } else {
                    directionGoing = directionGoing.rotateRight().rotateRight();
                }
            }
            if (locationGoing == Lib.noLoc) {
                if (directionGoing != Direction.CENTER) {
                    nav.goTo(rc.getLocation().add(directionGoing));
                    //turnsMovingInDirection++;
                }
            } else {
                if (job == Jobs.GUARDINGFLAGHOLDER) {
                    if (rc.getLocation().distanceSquaredTo(locationGoing) <= 10 || lib.contains(rc.getAllySpawnLocations(), rc.getLocation())) {
                        nav.goTo(locationGoing.directionTo(rc.getLocation()));
                    }
                }


             /*   if(job == Jobs.RETRIEVINGFLAG){
                    MapLocation nearestEnemyCenter = lib.getNearestEnemyCenter(rc.getLocation());
                    if(rc.getLocation().distanceSquaredTo(nearestEnemyCenter) < 30){
                        if(rc.getLocation().directionTo(nearestEnemyCenter) == rc.getLocation().directionTo(locationGoing).rotateLeft().rotateLeft()) {
                            nav.goTo(rc.getLocation().directionTo(locationGoing).rotateRight());
                        }
                        if(rc.getLocation().directionTo(nearestEnemyCenter) == rc.getLocation().directionTo(locationGoing).rotateLeft()) {
                            nav.goTo(rc.getLocation().directionTo(locationGoing).rotateRight().rotateRight());
                        }
                        if(rc.getLocation().directionTo(nearestEnemyCenter) == rc.getLocation().directionTo(locationGoing).rotateRight().rotateRight()) {
                            nav.goTo(rc.getLocation().directionTo(locationGoing).rotateLeft());
                        }
                        if(rc.getLocation().directionTo(nearestEnemyCenter) == rc.getLocation().directionTo(locationGoing).rotateRight()) {
                            nav.goTo(rc.getLocation().directionTo(locationGoing).rotateLeft().rotateLeft());
                        }
                    }
                }*/

                lastMovement = nav.goTo(locationGoing, false); //if we need to save bytecode, well this is where we're saving it
                lastLocationGoing = locationGoing;
                if (!lastMovement) {
                    lastMovement = nav.goTo(locationGoing, false);
                    if (!lastMovement) {
                    //    lastMovement = nav.navTo(locationGoing);
                        if (!lastMovement) {
                            // lastMovement = nav.goTo(rc.getLocation().directionTo(locationGoing));
                        }
                    }
                }
                turnsMovingInDirection = 0;
            }
        }
    }

    void fill() throws GameActionException {
        for(Direction dir : Lib.directions) {
            if (rc.canFill(rc.getLocation().add(dir))) {
                rc.fill(rc.getLocation().add(dir));
            }
        }
    }

    void attack() throws GameActionException{
        RobotInfo[] robotInfos = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if(robotInfos.length > 0){
            MapLocation enemyWithFlagNearby = lib.getEnemyWithFlagNearby(robotInfos);
            if(!enemyWithFlagNearby.equals(Lib.noLoc)){
                if(rc.canAttack(enemyWithFlagNearby)){
                    rc.attack(enemyWithFlagNearby);
                }
            }
            robotInfos = lib.sort(robotInfos);
            if(rc.canAttack(robotInfos[0].getLocation())){
                rc.attack(robotInfos[0].getLocation());
            }
        }
    }

    void heal() throws GameActionException{
        RobotInfo[] robotInfos = rc.senseNearbyRobots(-1, rc.getTeam());
        robotInfos = lib.sortByXP(robotInfos);
        if(robotInfos.length > 0){
            if(rc.canHeal(robotInfos[0].getLocation())){
                rc.heal(robotInfos[0].getLocation());
            }
        }
    }


    void healOrAttack() throws GameActionException {
        RobotInfo[] opponentsNear = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        RobotInfo[] alliesNear = rc.senseNearbyRobots(-1, rc.getTeam());

        if(opponentsNear.length > 0 && alliesNear.length > 0){
            if(duckSkill.name().equals("HEAL")){
                for(RobotInfo ally : alliesNear){
                    if(ally.health < 151){
                        heal();
                    }
                }
                attack();
            }
            else if(duckSkill.name().equals("ATTACK")) {
                for (RobotInfo opp : opponentsNear) {
                    if (opp.health < 151) {
                        attack();
                    }
                }
                heal();
            }
            else { // ducks that are BUILD or none will choose based on # of ducks nearby
                if(alliesNear.length < opponentsNear.length){
                    for(RobotInfo ally : alliesNear){
                        if(ally.health < 151){
                            heal();
                        }
                    }
                }
                else {
                    for (RobotInfo opp : opponentsNear) {
                        if (opp.health < 151) {
                            attack();
                        }
                    }
                }
            }
        }




    }


    Direction getNextDirection(){
        return Lib.directions[rng.nextInt(8)];
    }

    void placeTraps() throws GameActionException {
        if(job == Jobs.IDLING || job == Jobs.GUARDINGFLAGHOLDER){
            for(MapLocation loc1 : lib.enemySpawnZones()) {
                for (MapInfo loc : rc.senseNearbyMapInfos()) {
                    if (!loc.isWater() && !loc.isWall()) {
                        if (loc1.distanceSquaredTo(rc.getLocation()) < 10) {
                            if (rc.canBuild(TrapType.STUN, loc.getMapLocation())) {
                                rc.build(TrapType.STUN, loc.getMapLocation());
                            }
                        }
                    }
                }
            }
        }

        if(rc.getRoundNum() < 200 && rc.getCrumbs() > 200) {
            if (lib.isNearDam(rc.getLocation())) {
                    if (rc.canBuild(TrapType.STUN, rc.getLocation())){
                        rc.build(TrapType.STUN, rc.getLocation());
                    }
            }
        }

        if(job == Jobs.GUARDINGFLAG){
            for(Direction dir : Lib.directions){
                if(rc.canBuild(TrapType.STUN, rc.getLocation().add(dir))){
                    rc.build(TrapType.STUN, rc.getLocation().add(dir));
                }
            }
        }


        if(rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length >= 1 || rc.getCrumbs() > 500) {
            if(job == Jobs.GUARDINGFLAGHOLDER && rc.getLocation().distanceSquaredTo(locationGoing) < 40){
                RobotInfo robotInfo = rc.senseRobotAtLocation(rc.getLocation().add(lastLocationGoing.directionTo(rc.getLocation())));
                if(robotInfo == null){
                    for(Direction dir : Lib.directions){
                        if(rc.canBuild(TrapType.STUN, rc.getLocation().add(dir))){
                            rc.build(TrapType.STUN, rc.getLocation().add(dir));
                        }
                    }
                }
            }

            if (job == Jobs.DEFENDINGFLAG || job == Jobs.GETTINGFLAG) { //more enemies?? build explosive
                if (rc.getLocation().distanceSquaredTo(locationGoing) < 40) {
                    for (Direction dir : lib.startDirList(lib.dirToIndex(rc.getLocation().directionTo(locationGoing)), 0)) {
                        if (rc.canBuild(TrapType.STUN, rc.getLocation().add(dir))) {
                            rc.build(TrapType.STUN, rc.getLocation().add(dir));
                        }
                    }
                }
            }
        }


    }



    void senseFlags() throws GameActionException { //remove any unnecessary flag locations that aren't valid
        MapLocation[] flags = new MapLocation[]{lib.getEnemyFlagLoc(1), lib.getEnemyFlagLoc(2), lib.getEnemyFlagLoc(3), lib.getEnemyFlagLoc(4)};
        if(flags[0].equals(new MapLocation(-1, -1))){
            flags[0] = Lib.noLoc;
        }
        if(flags[1].equals(new MapLocation(-1, -1))){
            flags[1] = Lib.noLoc;
        }
        if(flags[2].equals(new MapLocation(-1, -1))){
            flags[2] = Lib.noLoc;
        }
        if(flags[3].equals(new MapLocation(-1, -1))){
            flags[3] = Lib.noLoc;
        }
        for(MapLocation flag : flags){
            if(rc.canSenseLocation(flag)) {
                if (rc.canSenseRobotAtLocation(flag)) {
                    RobotInfo holder = rc.senseRobotAtLocation(flag);
                    if (holder != null) {
                        if (holder.getTeam() == rc.getTeam()) {
                            if (!holder.hasFlag) {
                                if(flagCarrierIndex == 0){
                                    flagCarrierIndex = lib.getFlagIndex(flag);
                                }
                                lib.setEnemyFlagLoc(Lib.noLoc, flagCarrierIndex);
                                flag = Lib.noLoc;
                            }
                        } else {
                            if(flagCarrierIndex == 0){
                                flagCarrierIndex = lib.getFlagIndex(flag);
                            }
                            lib.setEnemyFlagLoc(Lib.noLoc, flagCarrierIndex);
                            flag = Lib.noLoc;
                        }
                    } else {
                        if(flagCarrierIndex == 0){
                            flagCarrierIndex = lib.getFlagIndex(flag);
                        }
                        lib.setEnemyFlagLoc(Lib.noLoc, flagCarrierIndex);
                        flag = Lib.noLoc;
                    }
                } else {
                    if(flagCarrierIndex == 0){
                        flagCarrierIndex = lib.getFlagIndex(flag);
                    }
                    lib.setEnemyFlagLoc(Lib.noLoc, flagCarrierIndex);
                    flag = Lib.noLoc;
                }
            }
        }
    }

    //Given a direction, checks if the duck can move there. If it can, sets the movement direction there, else does nothing
    public void canAndMove(Direction direct){
        if(rc.canMove(direct)){
            this.directionGoing = direct;
        }
    }
}