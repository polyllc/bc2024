package poly;


public class Duck {
    RobotController rc;
    Lib lib;
    public Duck(RobotPlayer robot){
        rc = robot;
        nav = new Navigation(rc);
        lib = new Lib(rc);
    }

    public void takeTurn(){

    }
}