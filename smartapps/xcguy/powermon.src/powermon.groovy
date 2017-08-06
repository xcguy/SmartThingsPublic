/**
 *  PowerMon
 *
 *  Copyright 2015-17 Bruce Adelsman
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "PowerMon",
    namespace: "xcguy",
    author: "Bruce Adelsman",
    description: "Monitors an energy switch and reports to an activity sensor, using energy levels to reflect a presence status.   One of these SmartApps is needed for each energy switch/presence pairing.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Power monitoring") {
 	   input "powerMeter", "capability.powerMeter", title: "Choose power meter"
 	   input "powerActive", "number", title: "Power active level",
 	   		description: "Power level at which device is considered active (default: 20).", defaultValue: 20,
  			required: false, displayDuringSetup: true
	}
	section("Activity Sensor") {                    
	    input "activitySensor", "capability.presenceSensor", title: "Choose activity sensor"
        input "inactivityDelay", "number", title: "Sensor inactivity delay",
 	   		description: "Delay in minutes before registering inactivity (default: 0).", defaultValue: 0,
  			required: false, displayDuringSetup: true
	}
    section("Notifications") {
       	input "powerOn", "bool", title: "Power on", required: false, defaultValue: false
       	input "powerOff", "bool", title: "Power off", required: false, defaultValue: false
 	}  
}

def installed() {
  subscribe(powerMeter, "power", powerHandler)
}

def updated() {
	unsubscribe()
    subscribe(powerMeter, "power", powerHandler)
}

def isPowerActive(level) {
  return(level > getPowerActiveLevel())
}

def getPowerActiveLevel() {
	return (powerActive == null ? 20 : powerActive as int)
}

def getInactivityDelayMins() {
	return (inactivityDelay == null ? 0 : inactivityDelay as int)
}


def powerHandler(evt) {
  def activityState = activitySensor.currentValue("presence")
  def meterValue = evt == null ? powerMeter.currentValue("power") as int : evt.value as int

  log.debug "PowerMon - handler - power: ${meterValue}, presence: ${activityState}"
  if (isPowerActive(meterValue)) {
  	log.debug "PowerMon - power active"
  	if (activityState == "not present") {
    	log.debug "${powerMeter} reported energy consumption above ${powerActive}. Turning on presence for ${activitySensor}."
    	activitySensor.active()
        if (powerOn)
        	sendPush "${activitySensor.displayName} is on"
    }
  } else if (activityState == "present") {
  	log.debug "PowerMon - power inactive"
  	if (evt == null || getInactivityDelayMins() == 0) {
   		log.debug "${powerMeter} reported energy consumption below ${powerActive}. Turning off presence ${activitySensor}."
  		activitySensor.inactive()
   		if (powerOff)
        	sendPush "${activitySensor.displayName} is off"
  	} else {
    	log.debug "${powerMeter} reported energy consumption below ${powerActive}, inactivity delay for ${getInactivityDelayMins()} mins."
  		runIn(getInactivityDelayMins()*60, powerHandler)
    }
  }
}


