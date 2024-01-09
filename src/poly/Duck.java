package poly;

import battlecode.common.*;
public class Duck {
    RobotController rc;
    Lib lib;
    Navigation nav;
    public Duck(RobotController robot){
        rc = robot;
        nav = new Navigation(rc);
        lib = new Lib(rc);
    }

    public void takeTurn() throws GameActionException{

    }
}