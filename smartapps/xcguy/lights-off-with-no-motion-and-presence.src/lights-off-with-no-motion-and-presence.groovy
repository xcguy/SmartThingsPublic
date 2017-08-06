/**
 *  Lights Off with No Motion and Presence
 *
 *  Author: Bruce Adelsman
 */

definition(
		name: "Lights Off with No Motion and Presence",
		namespace: "xcguy",
		author: "Bruce Adelsman",
		description: "Turn lights off when no motion and presence is detected for a set period of time.",
		category: "Convenience",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_presence-outlet.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_presence-outlet@2x.png"
)

preferences {
	section("Light switches to turn off") {
		input "switches", "capability.switch", title: "Choose light switches", multiple: true
	}
	section("Turn off when there is no motion and presence") {
		input "motionSensors", "capability.motionSensor", title: "Choose motion sensors", multiple: true
		input "presenceSensors", "capability.presenceSensor", title: "Choose presence sensors", multiple: true
	}
	section("Delay before turning off") {                    
		input "delayMins", "number", title: "Minutes of inactivity?"
	}
}

def installed() {
	subscribe(motionSensors, "motion", motionHandler)
	subscribe(presenceSensors, "presence", presenceHandler)
}

def updated() {
	unsubscribe()
	subscribe(motionSensors, "motion", motionHandler)
	subscribe(presenceSensors, "presence", presenceHandler)
}

def motionHandler(evt) {
	log.debug "handler $evt.name: $evt.value"
	if (evt.value == "inactive") {
		runIn(delayMins * 60, scheduleCheck)
	}
}

def presenceHandler(evt) {
	log.debug "handler $evt.name: $evt.value"
	if (evt.value == "not present") {
		runIn(delayMins * 60, scheduleCheck)
	}
}

def isPresenceActive() {
	// check all the presence sensors to find none are present
	def noPresence = presenceSensors.find{it.currentPresence == "present"} == null
	!noPresence		
}

def isMotionActive() {
	// check all the motion sensors to find none are active
	def noMotion = motionSensors.find{it.currentMotion == "active"} == null
	!noMotion	
}

def isLightsOn() {
	// check all the switches to find none are on
	def noLights = switches.find{it.currentSwitch == "on"} == null
	!noLights
}

def scheduleCheck() {
	log.debug "scheduled check"
	if (isLightsOn()) {
		if (!isMotionActive() && !isPresenceActive()) {
            log.debug "Motion/presence has been inactive for at least $delayMins mins:  turning lights off"
			switches.off()
		} else 
			log.debug "Motion/presence is active again: do nothing"
	}
	else 
		log.debug "No lights on: do nothing"
}
