/**
 *  PowerMon
 *
 *  Copyright 2015 Bruce Adelsman
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
    namespace: "naissan",
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
 	   		description: "Power level at which presence is considered active (default: 20).", defaultValue: 20,
  			required: false, displayDuringSetup: true
	}
	section("Activity Sensor") {                    
	    input "activitySensor", "capability.presenceSensor", title: "Choose activity sensor"
	}
}

def installed() {
  subscribe(powerMeter, "power", powerHandler)
  log.debug "PowerMon - power active: $powerActive"
}

def updated() {
	unsubscribe()
  subscribe(powerMeter, "power", powerHandler)
  log.debug "PowerMon - power active: $powerActive"
}

def powerHandler(evt) {

  def meterValue = evt.value as int
  def powerActiveLevel = powerActive as int
  def activityState = activitySensor.currentValue("presence")
	log.debug "PowerMon - power event handler - power: ${meterValue}, cur presence: ${activityState}"
  if (meterValue > powerActiveLevel) {
  	if (activityState == "not present") {
    	log.debug "${powerMeter} reported energy consumption above ${powerActive}. Turning on presence for ${activitySensor}."
    	activitySensor.active()
    }
  } else if (activityState == "present") {
   	log.debug "${powerMeter} reported energy consumption below ${powerActive}. Turning off presence ${activitySensor}."
  	activitySensor.inactive()
  }
}



