package poly;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Map;


public class Lib {

    RobotController rc;

    int roundNum;
    int lastRoundNum;
    MapLocation spawnLocations[];


    static MapLocation noLoc = new MapLocation(256,256);
    static MapLocation noFlag = new MapLocation(257,257);

    public Lib(RobotController robot){
        rc = robot;
        roundNum = rc.getRoundNum();
        lastRoundNum = roundNum--;
        spawnLocations = rc.getAllySpawnLocations();
    }
    //pretty much any useful function or variables go here
    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    static final Direction[] directionsCenter = {
            Direction.CENTER,
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    public int getQuadrant(){
        int width = rc.getMapWidth();
        int height = rc.getMapHeight();

        if(width/2 > rc.getLocation().x) { //left section
            if(height/2 <= rc.getLocation().y) { //top section, quadrant 2
                return 2;
            }
            if(height/2 >= rc.getLocation().y) { //bottom section quadrant 3
                return 3;
            }
        }
        if(width/2 <= rc.getLocation().x) { //right section
            if(height/2 <= rc.getLocation().y) { //top section, quadrant 1
                return 1;
            }
            if(height/2 > rc.getLocation().y) { //bottom section quadrant 4
                return 4;
            }
        }
        return 1;
    }

    public MapLocation mapCenter(){
        return  new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
    }

    public int getQuadrant(MapLocation m){
        int width = rc.getMapWidth();
        int height = rc.getMapHeight();

        if(width/2 > m.x) { //left section
            if(height/2 <= m.y) { //top section, quadrant 2
                return 2;
            }
            if(height/2 >= m.y) { //bottom section quadrant 3
                return 3;
            }
        }
        if(width/2 <= m.x) { //right section
            if(height/2 <= m.y) { //top section, quadrant 1
                return 1;
            }
            if(height/2 > m.y) { //bottom section quadrant 4
                return 4;
            }
        }
        return 1;
    }

    public MapLocation getOrigin(int q){
        if(q == 1){
            return new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
        }
        if(q == 2){
            return new MapLocation(rc.getMapWidth()/2-1, rc.getMapHeight()/2);
        }
        if(q == 3){
            return new MapLocation(rc.getMapWidth()/2-1, rc.getMapHeight()/2-1);
        }
        if(q == 4){
            return new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2-1);
        }
        return new MapLocation(0,0);
    }

    RobotInfo[] currentRoundRobots =  new RobotInfo[0];

   public RobotInfo[] getRobots(){
        roundNum = rc.getRoundNum();
        if(currentRoundRobots.length == 0 || lastRoundNum < roundNum){
            currentRoundRobots = sort(rc.senseNearbyRobots());
            lastRoundNum = roundNum;
        }
        return currentRoundRobots;
    }

    public boolean contains(RobotInfo[] robots, RobotInfo robot){
        for(RobotInfo r : robots){
            if(robot.equals(r)){
                return true;
            }
        }
        return false;
    }

    public boolean contains(MapLocation[] locs, MapLocation loc){
        for(MapLocation l : locs){
            if(l.equals(loc)){
                return true;
            }
        }
        return false;
    }

    public boolean contains(int[] ints, int i){
        for(int j : ints){
            if(j == i){
                return true;
            }
        }
        return false;
    }

    boolean detectCorner(Direction dirGoing) throws GameActionException { 
        if(rc.getLocation().equals(new MapLocation(rc.getMapWidth() - 1, rc.getMapHeight() - 1)) ||
                rc.getLocation().equals(new MapLocation(0, rc.getMapHeight() - 1)) ||
                rc.getLocation().equals(new MapLocation(rc.getMapWidth() - 1, 0)) ||
                rc.getLocation().equals(new MapLocation(0,0))){
            return true;
        }

        if(dirGoing != Direction.CENTER) {
            int[] walls = new int[8];
            int i = 0;
            for (Direction dir : directions) {
                if (rc.canSenseLocation(rc.getLocation().add(dir))) {
                    if (!rc.sensePassability(rc.getLocation().add(dir))) {
                        walls[i] = 1;
                    }
                }
                i++;
            }

            if (walls[0] == 1 && walls[1] == 1 && walls[2] == 1 && dirGoing == Direction.NORTHEAST) { //corner northeast
                return true;
            }
            if (walls[2] == 1 && walls[3] == 1 && walls[4] == 1 && dirGoing == Direction.SOUTHEAST) { //corner southeast
                return true;
            }
            if (walls[4] == 1 && walls[5] == 1 && walls[6] == 1 && dirGoing == Direction.SOUTHWEST) { //corner southwest
                return true;
            }
            if (walls[6] == 1 && walls[7] == 1 && walls[0] == 1 && dirGoing == Direction.NORTHWEST) { //corner northwest
                return true;
            }
        }

        return false;
    }

     boolean detectCorner(MapLocation loc, int radius){
         if(loc.distanceSquaredTo(new MapLocation(0 ,0)) <= radius){
             return true;
         } else if(loc.distanceSquaredTo(new MapLocation(rc.getMapWidth()-1, 0)) <= radius){
             return true;
         } else if(loc.distanceSquaredTo(new MapLocation(0, rc.getMapHeight()-1)) <= radius){
             return true;
         } else if(loc.distanceSquaredTo(new MapLocation(rc.getMapWidth()-1, rc.getMapHeight()-1)) <= radius) {
            return true;
         }
         return false;
     }


    public Direction[] startDirList(int index, int offset){
        Direction[] dirs = new Direction[8];
        index = (index + offset) % 8;
        for(Direction dir : directions){
            dirs[index] = dir;
            index++;
            if(index == 8){
                index = 0;
            }
        }
        return dirs;
    }

    public Direction[] reverse(Direction[] dirs){ //meant for the hq
        if(getQuadrant() == 1 || getQuadrant() == 4){
            Direction[] newDirs = new Direction[dirs.length];
            int j = dirs.length-1;
            for(int i = 0; i < dirs.length; i++){
                newDirs[i] = dirs[j];
                j--;
            }
            return newDirs;
        }
        return dirs;
    }

    public int dirToIndex(Direction dir){
        switch(dir){
            case NORTH: return 0;
            case NORTHEAST: return 1;
            case EAST: return 2;
            case SOUTHEAST: return 3;
            case SOUTH: return 4;
            case SOUTHWEST: return 5;
            case WEST: return 6;
            case NORTHWEST: return 7;
        }
        return 0;
    }
/*

    Direction educatedGuess(MapLocation hq){
        return rc.getLocation().directionTo(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2));
    }
*/


    //doesn't actually sort anything, but what it does instead is put up the lowest health robot on the top
    public RobotInfo[] sort(RobotInfo[] items){
        if(items.length > 0) {
            RobotInfo lowest = items[0];
            int lowestIndex = 0;
            int i = 0;
            for (RobotInfo r : items) {
                if (rc.getTeam() != r.getTeam()){
                    if(lowest.getTeam() == rc.getTeam()){
                        lowest = r;
                        lowestIndex = i;
                    }
                    if(lowest.getHealth() > r.getHealth()){
                        lowest = r;
                        lowestIndex = i;
                    }
                }
                i++;
            }

            if(items.length > 1) {
                RobotInfo temp = items[0];
                items[0] = lowest;
                items[lowestIndex] = temp;
            }
        }
        return items;
    }

    public MapLocation[] getNearestSpawns(MapLocation loc){
        MapLocation[] spawns = rc.getAllySpawnLocations();
        if(spawns.length > 0){
            int i = 0;
            do {
                i = 0;
                for(int j = 0; j < spawns.length-1; j++){
                    if(spawns[j].distanceSquaredTo(loc) > spawns[j+1].distanceSquaredTo(loc)){
                        MapLocation temp = spawns[j];
                        spawns[j] = spawns[j+1];
                        spawns[j+1] = temp;
                        i++;
                    }
                }
            } while(i > 0);
        }
        return spawns;
    }

    //senses if a tile is surrounded by water in a 3x3 area, anything larger, no dice (should probably upgrade to a 5x5 one day, lukas you want to do that?)
    public boolean surroundedByWater(MapLocation loc) throws GameActionException {
        if(rc.canSenseLocation(loc)) {
            if (rc.senseMapInfo(loc).isWater()) {
                return true;
            }
            else {
                for(Direction dir : directions){
                    if(rc.canSenseLocation(loc.add(dir))){
                        if(!rc.senseMapInfo(loc.add(dir)).isWater()){
                            return false; // if there is an opening in the water, then go
                        }
                    }
                    else { //since we can't sense the location, we can't be sure that it is surrounded
                        return false;
                    }
                }
            }
        }
        return false;
    }

    public FlagInfo[] getNearestFlags(MapLocation loc) throws GameActionException {
        FlagInfo[] flagInfos = rc.senseNearbyFlags(-1);
        if(flagInfos.length > 0){
            for(FlagInfo flag : flagInfos){
                if(!flag.isPickedUp()){
                    if(flag.getTeam() != rc.getTeam()){
                        return new FlagInfo[]{flag};
                    }
                }
            }
        }
        return new FlagInfo[]{};
    }

    public void setEnemyFlagLoc(MapLocation loc, int flagNum) throws GameActionException {
        switch(flagNum){
            case 1: writeArray(0, loc.x+1); writeArray(1, loc.y+1); break;
            case 2: writeArray(2, loc.x+1); writeArray(3, loc.y+1); break;
            case 3: writeArray(4, loc.x+1); writeArray(5, loc.y+1); break;
            case 4: writeArray(6, loc.x+1); writeArray(7, loc.y+1); break; //we add one because 0,0 is a valid position on the map, and the default values of the array are 0
        }
    }

    public MapLocation getEnemyFlagLoc(int flagNum) throws GameActionException {
        switch (flagNum) {
            case 1: return new MapLocation(rc.readSharedArray(0)-1, rc.readSharedArray(1)-1);
            case 2: return new MapLocation(rc.readSharedArray(2)-1, rc.readSharedArray(3)-1);
            case 3: return new MapLocation(rc.readSharedArray(4)-1, rc.readSharedArray(5)-1);
            case 4: return new MapLocation(rc.readSharedArray(6)-1, rc.readSharedArray(7)-1);
        }
        return noLoc;
    }

    public void writeArray(int index, int value) throws GameActionException {
        if(rc.canWriteSharedArray(index, value)){
            rc.writeSharedArray(index, value);
        }
    }

    public MapLocation getNearestFlagCarrier() throws GameActionException {
        MapLocation[] flags = new MapLocation[]{getEnemyFlagLoc(1), getEnemyFlagLoc(2), getEnemyFlagLoc(3), getEnemyFlagLoc(4)};
        if(flags[0].equals(new MapLocation(-1, -1))){
            flags[0] = noLoc;
        }
        if(flags[1].equals(new MapLocation(-1, -1))){
            flags[1] = noLoc;
        }
        if(flags[2].equals(new MapLocation(-1, -1))){
            flags[2] = noLoc;
        }
        if(flags[3].equals(new MapLocation(-1, -1))){
            flags[3] = noLoc;
        }

        MapLocation closest = noLoc;

        if(flags[0].distanceSquaredTo(rc.getLocation()) <= closest.distanceSquaredTo(rc.getLocation())){
            closest = flags[0];
        }
        if(flags[1].distanceSquaredTo(rc.getLocation()) <= closest.distanceSquaredTo(rc.getLocation())){
            closest = flags[1];
        }
        if(flags[2].distanceSquaredTo(rc.getLocation()) <= closest.distanceSquaredTo(rc.getLocation())){
            closest = flags[2];
        }
        if(flags[3].distanceSquaredTo(rc.getLocation()) <= closest.distanceSquaredTo(rc.getLocation())){
            closest = flags[3];
        }


        return closest;
    }

    public int getNextClearFlagIndex() throws GameActionException {
        MapLocation[] flags = new MapLocation[]{getEnemyFlagLoc(1), getEnemyFlagLoc(2), getEnemyFlagLoc(3), getEnemyFlagLoc(4)};
        System.out.println(Arrays.toString(flags));
        if(getEnemyFlagLoc(1).equals(noLoc) || getEnemyFlagLoc(1).equals(new MapLocation(-1,-1))) {
            return 1;
        } else if(getEnemyFlagLoc(2).equals(noLoc) || getEnemyFlagLoc(2).equals(new MapLocation(-1,-1))){
            return 2;
        } else if(getEnemyFlagLoc(3).equals(noLoc)|| getEnemyFlagLoc(3).equals(new MapLocation(-1,-1))){
            return 3;
        } else if(getEnemyFlagLoc(4).equals(noLoc) || getEnemyFlagLoc(4).equals(new MapLocation(-1,-1))) {
            return 4;
        } else {
            return 0;
        }
    }

    public int getFlagIndex(MapLocation loc) throws GameActionException {
        if(getEnemyFlagLoc(1).equals(loc)){
            return 1;
        } else if(getEnemyFlagLoc(2).equals(loc)){
            return 2;
        } else if(getEnemyFlagLoc(3).equals(loc)){
            return 3;
        } else if(getEnemyFlagLoc(4).equals(loc)){
            return 4;
        }
        return 0;
    }

    public void printSharedArray(int limit) throws GameActionException {
        limit = limit % 64;
        StringBuilder output = new StringBuilder();
        for(int i = 0; i < limit; i++){
            output.append("I" + i + ": " + rc.readSharedArray(i) + " | ");
        }
        System.out.println(output);
    }

    //we do this way cause the other way has I think 27*8*27*2*3 bytecode usage, which is way over our 25000 limit
    public void setAllySpawnZones(MapLocation loc) throws GameActionException {
        if(allySpawnZones().length != 3){
            for(MapInfo allySpawn : rc.senseNearbyMapInfos()) {
                if (allySpawn.isSpawnZone()) {
                    int teamInInt = rc.getTeam() == Team.A ? 1 : 2;
                    if (allySpawn.getSpawnZoneTeam() == teamInInt) {
                        if (checkIfCenter(loc)) {
                            if(!isAllyCenter(loc)) {
                                setAllyCenter(loc);
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean isAllyCenter(MapLocation loc) throws GameActionException {
        if(new MapLocation(rc.readSharedArray(17), rc.readSharedArray(18)).equals(loc)){
            return true;
        } else if(new MapLocation(rc.readSharedArray(19), rc.readSharedArray(20)).equals(loc)){
            return true;
        } else if(new MapLocation(rc.readSharedArray(21), rc.readSharedArray(22)).equals(loc)){
            return true;
        }
        return false;
    }

    public void setAllyCenter(MapLocation loc) throws GameActionException {
        if(new MapLocation(rc.readSharedArray(17), rc.readSharedArray(18)).equals(new MapLocation(0,0))){
            rc.writeSharedArray(17, loc.x);
            rc.writeSharedArray(18, loc.y);
        } else if(new MapLocation(rc.readSharedArray(19), rc.readSharedArray(20)).equals(new MapLocation(0,0))) {
            rc.writeSharedArray(19, loc.x);
            rc.writeSharedArray(20, loc.y);
        } else if(new MapLocation(rc.readSharedArray(21), rc.readSharedArray(22)).equals(new MapLocation(0,0))) {
            rc.writeSharedArray(21, loc.x);
            rc.writeSharedArray(22, loc.y);
        }
    }

    public MapLocation[] allySpawnZones() throws GameActionException {
        MapLocation[] zones = new MapLocation[0];
        if(!new MapLocation(rc.readSharedArray(17), rc.readSharedArray(18)).equals(new MapLocation(0,0))){
            zones = new MapLocation[]{new MapLocation(rc.readSharedArray(17), rc.readSharedArray(18))};
            if(!new MapLocation(rc.readSharedArray(19), rc.readSharedArray(20)).equals(new MapLocation(0,0))){
                zones = new MapLocation[]{new MapLocation(rc.readSharedArray(17), rc.readSharedArray(18)),
                                          new MapLocation(rc.readSharedArray(19), rc.readSharedArray(20))};
                if(!new MapLocation(rc.readSharedArray(21), rc.readSharedArray(22)).equals(new MapLocation(0,0))){
                    zones = new MapLocation[]{new MapLocation(rc.readSharedArray(17), rc.readSharedArray(18)),
                                              new MapLocation(rc.readSharedArray(19), rc.readSharedArray(20)),
                                              new MapLocation(rc.readSharedArray(21), rc.readSharedArray(22))};
                }
            }
        }
        return zones;
    }

    // to save bytecode, we can only run this continuously only until all 3 spawn points are found
    // we can also make this way smarter by simply by knowing one of the spawn locations, we know by our spawn locations whether the map is rotation\
    //    horizontal reflection or vertical reflection by running some cool calculations
    // we can also make it so if it senses one of the corners, it will automagically register the mid-point as it can only be the opposite matching
    //    corner with the duck that discovers
    // this sounds like a job for mr lukasz!
    public void enemySpawnPoints(MapLocation loc) throws GameActionException { //set enemy spawn points, the middle of the 3x3 area
        for(MapInfo enemySpawn : rc.senseNearbyMapInfos()) {
            if (enemySpawn.isSpawnZone()) {
                int teamInInt = rc.getTeam().opponent() == Team.A ? 1 : 2;
                if (enemySpawn.getSpawnZoneTeam() == teamInInt) {
                    autofillEnemySpawnPoints(loc);
                    if (checkIfCenter(loc)) {
                        if(!isEnemyCenter(loc)) {
                            setEnemyCenter(loc);
                        }
                    }
                }
            }
        }
    }

    //ok this will be a bit complicated so ill explain:
    //we know that if we sense an enemy spawn point we can determine where the center is by just one spawn point by:
    // - using rc.getLocation().direction(loc) and getting the opposite direction and then adding it to that loc
    //      - even if it is off by a couple, that doesn't matter because it will never be off by more than 1
    // - then setting that new added loc to the new enemy center
    // - once we have one we can use our spawn point locations to determine what symmetry type we have
    // - with rotational, we can simply rotate around the map center (to which I already have the code for)
    // - with mirrored symmetry along the y-axis, that is also easy by just calculating the distance and adding it from that origin
    // - same thing with x-axis
    // - what is really cool is that the code is the exact same, except the quadrants will be different depending on the symmetry
    public void autofillEnemySpawnPoints(MapLocation loc){

    }

    public boolean checkIfCenter(MapLocation loc) throws GameActionException {
        int i = 0;
        for(Direction dir : directions){
            if(rc.canSenseLocation(loc.add(dir))){
                if(rc.senseMapInfo(loc.add(dir)).isSpawnZone()){
                    i++;
                }
            }
        }
        return i == 8;
    }

    public void setEnemyCenter(MapLocation loc) throws GameActionException {
        if(new MapLocation(rc.readSharedArray(11), rc.readSharedArray(12)).equals(new MapLocation(0,0))){
            rc.writeSharedArray(11, loc.x);
            rc.writeSharedArray(12, loc.y);
        } else if(new MapLocation(rc.readSharedArray(13), rc.readSharedArray(14)).equals(new MapLocation(0,0))) {
            rc.writeSharedArray(13, loc.x);
            rc.writeSharedArray(14, loc.y);
        } else if(new MapLocation(rc.readSharedArray(15), rc.readSharedArray(16)).equals(new MapLocation(0,0))) {
            rc.writeSharedArray(15, loc.x);
            rc.writeSharedArray(16, loc.y);
        }
    }

    public boolean isEnemyCenter(MapLocation loc) throws GameActionException {
        if(new MapLocation(rc.readSharedArray(11), rc.readSharedArray(12)).equals(loc)){
            return true;
        } else if(new MapLocation(rc.readSharedArray(13), rc.readSharedArray(14)).equals(loc)){
            return true;
        } else if(new MapLocation(rc.readSharedArray(15), rc.readSharedArray(16)).equals(loc)){
            return true;
        }
        return false;
    }

    public MapLocation getNearestEnemyCenter(MapLocation loc) throws GameActionException {
        MapLocation[] enemyCenter = new MapLocation[]{
                new MapLocation(rc.readSharedArray(11), rc.readSharedArray(12)),
                new MapLocation(rc.readSharedArray(13), rc.readSharedArray(14)),
                new MapLocation(rc.readSharedArray(15), rc.readSharedArray(16))};

        if(enemyCenter[0].equals(new MapLocation(0, 0))){
            enemyCenter[0] = noLoc;
        }
        if(enemyCenter[1].equals(new MapLocation(0, 0))){
            enemyCenter[1] = noLoc;
        }
        if(enemyCenter[2].equals(new MapLocation(0, 0))){
            enemyCenter[2] = noLoc;
        }

        MapLocation closest = noLoc;

        if(enemyCenter[0].distanceSquaredTo(rc.getLocation()) <= closest.distanceSquaredTo(rc.getLocation())){
            closest = enemyCenter[0];
        }
        if(enemyCenter[1].distanceSquaredTo(rc.getLocation()) <= closest.distanceSquaredTo(rc.getLocation())){
            closest = enemyCenter[1];
        }
        if(enemyCenter[2].distanceSquaredTo(rc.getLocation()) <= closest.distanceSquaredTo(rc.getLocation())){
            closest = enemyCenter[2];
        }


        return closest;
    }

    public boolean isNearDam(MapLocation loc) throws GameActionException {
        int i = 0;
        for(Direction dir : directions){
            if(rc.canSenseLocation(loc.add(dir))) {//technically we don't need this as it will be the robots location, but
                if(isDam(loc.add(dir))){
                    i++;
                }
            }
        }
        return i > 0;
    }

    public boolean isDam(MapLocation loc) throws GameActionException {
        MapInfo mapInfo = rc.senseMapInfo(loc);
        return !mapInfo.isWater() && !mapInfo.isWall() && !rc.sensePassability(loc);
    }

}
