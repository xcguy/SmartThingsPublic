/**
 *  Copyright 2015 SmartThings
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
 *  Garage Door Auto Close
 *	Automatically closes the garage door at night (when mode is 'Night' or 30 mins after sunset/30 mins before sunrise).
 *	Sends push message.  Will also alert (text) if door does not close after being issued close.
 *  Assumptions:
 *  - Garage door sensor is open/close contact sensor
 *	- Garage door opener is momentary switch (on activates, off does nothing)
 *	- Mode Night exists
 *
 *  Author: Bruce Adelsman
 */
definition(
    name: "Garage Door Auto Close",
    namespace: "xcguy",
    author: "Bruce Adelsman",
    description: "Close the garage door automatically at night/after dark, warn if it doesn't close.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet@2x.png"
)

preferences {
	section("Garage Door Sensor"){
		input "door", "capability.contactSensor"
	}
	section("Garage Door Opener Switch"){
		input "opener", "capability.switch"
	}
	section("Delay before closing Garage Door") {
		input "closeDelay", "number", description: "Number of minutes", default: 15
	}
	section("Text message if garage door opener fails") {
		input("recipients", "contact", title: "Send notifications to") {
			input "phone", "phone", title: "Phone number (optional)", required: false
		}
	}    
    // optional:  pick night mode?
}


def installed()
{
	setup()
}

def updated()
{
	unsubscribe()
	setup()
}

def setup() {
	subscribe(door, "contact.open", contactOpenHandler)
}

def contactOpenHandler(evt) {
	// schedule doorCheck in X minutes
    runIn(closeDelay*60, doorCheck)
}

def doorCheck() {
	def doorState = door.currentState("contact")
	// if door open    
	if (doorState.value == "open") {
    //   currentMode = location.mode
    	def currentMode = location.mode
    //   get sunset/sunrise
 		def s = getSunriseAndSunset()    
    //	 currenttime = new Date()
    	def currentTime = new Date()
        // log.debug "Time - now: $currentTime  sunset: ${s.sunset}  sunrise: ${s.sunrise}"
        // log.debug "Mode $currentMode"
    //   if mode = "Night" or (currenttime.after(sunset) and currenttime.before(sunrise))
    	if (currentMode == "Night" || (currentTime.after(s.sunset) || currentTime.before(s.sunrise))) {
    //     push "Auto closing garage door"
    		sendPush "Auto closing ${door.displayName}"
	//     turn on opener
    		opener.on()
    //     schedule doorClosedCheck in 2 minutes
    		runIn(2*60, doorClosedCheck)
        } else {
	// else not the right mode or time of day, try again later
	//      schedule doorCheck in X minutes
    		// log.debug "Reschedule doorCheck"
    	   	runIn(closeDelay*60, doorCheck)
		}
    }
}

def doorClosedCheck() {
	def doorState = door.currentState("contact")
	// if door open    
	if (doorState.value == "open") {
    //   text "Auto closing of garage door failed, door will remain open"
    	def msg = "Auto closing of ${door.displayName} failed, door will remain open"
		log.info msg
		if (location.contactBookEnabled) {
			sendNotificationToContacts(msg, recipients)
		}
		else {
			if (phone) {
				sendSms phone, msg
			} else {
				sendPush msg
			}
		}
    }
}