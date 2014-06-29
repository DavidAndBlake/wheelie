package org.wintrisstech.erik.iaroc;

/**************************************************************************
 * Happy version...ultrasonics working...Version 140505A...mods by Vic
 * Added compass class...works
 **************************************************************************/
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

import org.wintrisstech.sensors.UltraSonicSensors;

import android.os.SystemClock;

/**
 * A Lada is an implementation of the IRobotCreateInterface, inspired by Vic's
 * awesome API. It is entirely event driven.
 * 
 * @author Erik
 */
public class Lada extends IRobotCreateAdapter {

	private static final int DEFAULT_DRAG_RACE_LEFT_WHEEL_SPEED = 500;

	private static final int DEFAULT_DRAG_RACE_RIGHT_WHEEL_SPEED = 480;

	private int currentDragRaceLeftWheelSpeed = DEFAULT_DRAG_RACE_LEFT_WHEEL_SPEED;

	private int currentDragRaceRightWheelSpeed = DEFAULT_DRAG_RACE_RIGHT_WHEEL_SPEED;

	private final Dashboard dashboard;

	public UltraSonicSensors sonar;

	private RobotState robotState = RobotState.STARTING;

	private long startTime;

	private Timer timer;

	/**
	 * Constructs a Lada, an amazing machine!
	 * 
	 * @param ioio
	 *            the IOIO instance that the Lada can use to communicate with
	 *            other peripherals such as sensors
	 * @param create
	 *            an implementation of an iRobot
	 * @param dashboard
	 *            the Dashboard instance that is connected to the Lada
	 * @throws ConnectionLostException
	 */
	public Lada(IOIO ioio, IRobotCreateInterface create, Dashboard dashboard)
			throws ConnectionLostException {

		super(create);
		sonar = new UltraSonicSensors(ioio);
		this.dashboard = dashboard;
	}

	public void initialize() throws ConnectionLostException {

		dashboard.log("iAndroid2014 happy version 140427A");
	}

	/**
	 * This method is called repeatedly
	 * 
	 * @throws ConnectionLostException
	 */
	public void loop() throws ConnectionLostException {

		// maze();
		newMaze();
		//dragRace();
	}

	public void dragRace() throws ConnectionLostException {

		if (robotState == RobotState.STARTING) {

			go(currentDragRaceLeftWheelSpeed, currentDragRaceRightWheelSpeed);

			robotState = RobotState.GOING_FORWARD;

		} else if (robotState == RobotState.GOING_FORWARD) {

			readSensors(SENSORS_WALL_SIGNAL);
			int wallSignal = getWallSignal();

			dashboard.log("wallSignal: " + wallSignal);

			if (wallSignal > 10) {

				currentDragRaceLeftWheelSpeed = DEFAULT_DRAG_RACE_LEFT_WHEEL_SPEED - 10;
				currentDragRaceRightWheelSpeed = DEFAULT_DRAG_RACE_RIGHT_WHEEL_SPEED + 20;

				go(currentDragRaceLeftWheelSpeed, currentDragRaceRightWheelSpeed);

			} else {

				currentDragRaceLeftWheelSpeed = DEFAULT_DRAG_RACE_LEFT_WHEEL_SPEED;
				currentDragRaceRightWheelSpeed = DEFAULT_DRAG_RACE_RIGHT_WHEEL_SPEED;

				go(currentDragRaceLeftWheelSpeed, currentDragRaceRightWheelSpeed);
			}
		}
	}

	public void newMaze() throws ConnectionLostException {

		checkEnvironment();

		performActionsBasedOnState();
	}

	public void checkEnvironment() throws ConnectionLostException {

		readSensors(SENSORS_BUMPS_AND_WHEEL_DROPS);

		if (isBumpRight() || isBumpLeft()) {

			robotState = RobotState.BUMPING_BOTH;
			dashboard.log("isBumpRight and isBumpLeft");
		}

		/*else if (isBumpRight()) {

			robotState = RobotState.BUMPING_RIGHT;
			dashboard.log("isBumpRight");
		}

		else if (isBumpLeft()) {

			robotState = RobotState.BUMPING_LEFT;
			dashboard.log("isBumpLeft");
		}*/

	}

	public void performActionsBasedOnState() throws ConnectionLostException {

		if (robotState == RobotState.STARTING) {

			go(200, 100);

			robotState = RobotState.GOING_FORWARD;

		} else if (robotState == RobotState.BUMPING_BOTH) {

			go(-150, -150);

			robotState = RobotState.GOING_BACKWARD;

			timer = new Timer(500);

		} else if (robotState == RobotState.BUMPING_RIGHT) {

			go(-100, -200);

			robotState = RobotState.TURNING_LEFT;

			startTime = System.currentTimeMillis();

		} else if (robotState == RobotState.TURNING_LEFT && System.currentTimeMillis() >= 1000) {

			go(300, 300);

			robotState = RobotState.GOING_STRAIGHT_FORWARD;

		} else if (robotState == RobotState.BUMPING_LEFT) {

			go(300, -300);

			robotState = RobotState.TURNING_RIGHT_AFTER_LEFT_BUMP;

			startTime = System.currentTimeMillis();

		} else if (robotState == RobotState.GOING_BACKWARD && timer.isExpired()) {

			go(300, -300);

			robotState = RobotState.TURNING_RIGHT_AFTER_FRONT_BUMP;

			startTime = System.currentTimeMillis();

		} else if (robotState == RobotState.TURNING_RIGHT_AFTER_FRONT_BUMP
				&& System.currentTimeMillis() >= startTime + 250) {

			go(300, 300);

			robotState = RobotState.GOING_STRAIGHT_FORWARD;

		} else if (robotState == RobotState.TURNING_LEFT
				&& System.currentTimeMillis() >= startTime + 250) {

			go(150, 500);

			robotState = RobotState.GOING_FORWARD;

		} else if (robotState == RobotState.GOING_STRAIGHT_FORWARD
				&& System.currentTimeMillis() >= startTime + 1000) {

			go(150, 500);

			robotState = RobotState.GOING_FORWARD;

		} else if (robotState == RobotState.TURNING_RIGHT_AFTER_LEFT_BUMP
				&& System.currentTimeMillis() >= 450) {

			go(150, 500);

			robotState = RobotState.GOING_FORWARD;
		}
	}

	public void go(int leftWheelSpeed, int rightWheelSpeed) throws ConnectionLostException {

		driveDirect(rightWheelSpeed, leftWheelSpeed);
	}

	public void go(int distanceToTravel) throws ConnectionLostException {

		int distanceGone = 0;
		while (distanceGone < distanceToTravel) {
			readSensors(SENSORS_DISTANCE);
			distanceGone = getDistance() + distanceGone;
			// dashboard.log(distanceGone + "");
		}
	}

	public void stop() throws ConnectionLostException {

		go(0, 0);
	}

	public void goStraight(int desiredDirection) throws ConnectionLostException {

		int directionError = 0;
		int directionErrorFactor = 2;
		int startSpeed = 300;
		int wheelSpeedDelta = 0;
		directionError = (int) (desiredDirection - dashboard.getAzimuth());
		if (dashboard.getAzimuth() > 62)// turnleft
		{
			wheelSpeedDelta = directionError * directionErrorFactor;
			go(startSpeed - wheelSpeedDelta / 2, startSpeed + wheelSpeedDelta / 2);
		}
		if (dashboard.getAzimuth() < 58)// turnright
		{
			wheelSpeedDelta = directionError * directionErrorFactor;
			go(startSpeed + wheelSpeedDelta / 2, startSpeed - wheelSpeedDelta / 2);
		}
		dashboard.log((int) dashboard.getAzimuth() + "");
	}

	public void maze() throws ConnectionLostException {

		readSensors(SENSORS_BUMPS_AND_WHEEL_DROPS);
		if (isBumpRight() && !isBumpLeft()) {
			driveDirect(300, -300);

			SystemClock.sleep(500);
			driveDirect(0, 0);
		}
		if (isBumpLeft() && !isBumpRight()) {
			driveDirect(-300, 300);

			SystemClock.sleep(500);

			driveDirect(0, 0);

		}
		if (isBumpRight() && isBumpLeft()) {
			driveDirect(-300, -300);
			SystemClock.sleep(1000);
			driveDirect(300, -300);
			SystemClock.sleep(500);
			driveDirect(300, 300);
			SystemClock.sleep(1000);
		}
		driveDirect(200, 500);

	}
}