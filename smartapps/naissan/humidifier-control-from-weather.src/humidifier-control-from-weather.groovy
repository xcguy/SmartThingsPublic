/**
 *  Humidifier Control from Weather
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
 *
 * Notes on usage
 * --------------
 * This SmartApp requires an outdoor temperature sensor, as well as a thermostat that implements setHumiditySetpoint
 * method.  The virtual device type Smart Weather Station can be used for an outdoor temperature reading.  The
 * Nest thermostat code @notoriousbdg implements humidity controls:
 *   https://github.com/notoriousbdg/device-type.nest
 * 
 */
definition(
    name: "Humidifier Control from Weather",
    namespace: "naissan",
    author: "Bruce Adelsman",
    description: "Adjusts the target humidity level on the thermostat based on an outdoor temperature reading.   Designed for cold climates, where humidity levels may need to be lowered according to temperature to help prevent condensation.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Change these thermostats...") {
		input name: "thermostats", type: "capability.thermostat", title: "Thermostats", multiple: true, description: "Thermostats must implement setHumidityTarget() in order to be adjustable. Nest thermostat device type should be compatible."
	}
    section("Monitor outdoor temperature") {
		input name: "outdoorTemp", type: "capability.temperatureMeasurement", title: "Outdoor thermometer",  description: "Select an outdoor thermometer.  If you don't have one, install the SmartWeather Station Tile device to get your local outdoor temperature."
        input name: "tempScale",  type: "enum", options: ["F","C"], title: "Temperature Scale", description: "Temperature scale - Fahrenheit or Celsius"
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// reset the last adjustments
    state.lastHumidity = -999
    // monitor temperature changes
	subscribe(outdoorTemp, "temperature", temperatureHandler)
}

def temperatureHandler(evt) {
	def tempValue = evt.value as float
	log.debug "temperature update $tempValue"
    
    def oldHumidityLevel = state.lastHumidity as int

	// convert C to F just for the humidity calculation
	if (tempScale == "C")
    	tempValue = celsiusToFahrenheit(tempValue)
    // calculate the target humidity
    // formula is the standard humidity recommendation:  -20F = 15%, 0F = 25%,  20F = 35%, 
    def humidityLevel = Math.round((tempValue/10)*5+25) as int
    // keep within reasonable range
    if (humidityLevel < 0)
    	humidityLevel = 0
    if (humidityLevel > 45)
    	humidityLevel = 45
    // if humidityLevel has changed
    if (humidityLevel != oldHumidityLevel) {
    	log.info "Temperature change requires humidity adjustment to $humidityLevel%"
        try {
        	// consider only adjustment the thermostats that are in 'auto' or 'heat' mode?
    		thermostats.setHumiditySetpoint(humidityLevel)
        	state.lastHumidity = humidityLevel
        } catch (Throwable e) {
			log.error "Thermostat error: $e"
		}
    }
}
