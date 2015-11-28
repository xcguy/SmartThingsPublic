/**
 *  Light Up The Night
 *
 *  Author: SmartThings
 */
definition(
    name: "Smart Lighting",
    namespace: "xcguy",
    author: "Bruce Adelsman",
    description: "Turn  lights on when it gets dark and off when it becomes light again.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet-luminance.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet-luminance@2x.png"
)

preferences {
	section("Luminosity sensor") {
		input "lightSensor", "capability.illuminanceMeasurement"
	}
    section("Below what lux level is dark (default: 15)?") {
		input "darkLux", "number", title: "Dark lux threshold?", defaultValue: 15, required: false
	}
    section("Above what lux level is bright (default: 35)?") {
		input "brightLux", "number", title: "Bright lux threshold?", defaultValue: 35, required: false
	}
	section("Lights to control") {
		input "lights", "capability.switch", multiple: true
	}
    section("Only active after this time of day (optional)") {
		input "timeOfDay", "time", title: "Time of day?", required: false
	}
	section("Only when presense sensors are active (optional)") {
   		input "presenceSensors", "capability.presenceSensor", multiple: true, required: false
        
	}
}

def installed() {
	subscribe(lightSensor, "illuminance", illuminanceHandler)
    subscribe(location, modeHandler)
    if (presenceSensors != null)
	   	subscribe(presenceSensors, "presence", activityHandler)
}

def updated() {
	unsubscribe()
    state.lastStatus = null
	subscribe(lightSensor, "illuminance", illuminanceHandler)
    subscribe(location, modeHandler)
    if (presenceSensors != null)
    	subscribe(presenceSensors, "presence", activityHandler)
}

def illuminanceHandler(evt) { 
	processIllumination(evt.integerValue)
}

def modeHandler(evt) {
	log.debug "Smart Lighting mode change: ${evt.value}"
    processIllumination(lightSensor.currentValue("illuminance").toInteger())
}

def activityHandler(evt) {
	log.debug "Smart Lighting presence change: ${evt.value}"
	if (evt.value == "present") 
		processIllumination(lightSensor.currentValue("illuminance").toInteger())
}

def processIllumination(currentLux) {
    	
	if (isActiveTime() && isActivePresence()) {
	    def darkLux = darkLux ?: 15
        def brightLux = brightLux ?: 35
		if (currentLux <= darkLux && isStatusChange("on")) {
	    	log.debug "Lights below darkness lux ${darkLux}, turning on"
			lights.on()
		}
		else if (currentLux >= brightLux && isStatusChange("off")) {
	    	log.debug "Lights above brightness lux ${brightLux}, turning off"
	 		lights.off()
		}
    }
}

private isActivePresence() {
	if (presenceSensors != null) {
		// check all the presence sensors to find none are present
		def noPresence = presenceSensors.find{it.currentPresence == "present"} == null
		return !noPresence
    } else
    	return true
}

private isActiveTime() {
	if (timeOfDay != null) {
    	// check if current time is past the optional start time
		def currentTime = new Date()
        def startTime = timeToday(timeOfDay, location.timeZone)
		if (currentTime < startTime)
        	return false            
        else
        	return true
    } else
    	return true
}

private isStatusChange(val) {
	// if no status, status has changed, or status hasn't been updated in more than 3 hours
	if (state.lastUpdate == null || state.lastStatus == null || state.lastStatus != val || state.lastUpdate < (now() - 1000*60*60*3)) {
    	state.lastUpdate = now()
        state.lastStatus = val
        return true
    } else {
    	return false
    }
}
