/**
 *  Presence Sensor/Toggle
 *
 *  Author: Bruce Adelsman
 *
 *  Date: 2015-10-12
 */
metadata {
	definition (name: "Presence Toggle", namespace: "xcguy", author: "Bruce Adelsman", oauth: true) {
		capability "Actuator"
  		capability "Presence Sensor"
   		capability "Motion Sensor"

        attribute "wakeTile", "string"
        
   		command "wakeupSystem"
        command "active"
        command "inactive"
        command "toggle"
	}
    
    preferences {
    	input "sensorType", "enum", title: "Device Type?",
       		options: [
            	"cellphone",
                "computer",
                "power",
                "other"
                ],
        	defaultValue: "computer", required: true
	}
    
            
	// simulator metadata
	simulator {
        status "Active": "presence:present"
        status "Inactive": "presence:not present"
        status "Toggle": "toggle"

        status "Wake": "wake"
	}


	// UI tile definitions
	tiles {
 		standardTile("presence", "device.presence", width: 2, height: 2, canChangeIcon: true, canChangeBackground: true) {
			state "not present", labelIcon:"st.presence.tile.not-present", backgroundColor:"#ffffff", defaultState: true
  			state "present", labelIcon:"st.presence.tile.present", backgroundColor:"#53a7c0"
		}
		standardTile("motion", "device.motion", width: 1, height: 1) {
			state "inactive", label:'no motion', icon:"st.motion.motion.inactive", backgroundColor:"#ffffff", defaultState: true
			state "active", label:'motion', icon:"st.motion.motion.active", backgroundColor:"#53a7c0"
		}
        standardTile("toggle", "device.presence", width: 1, height: 1, canChangeIcon: false, canChangeBackground: false, decoration: "flat") {
			state "default", label: "Toggle", action: "toggle", icon: ""
        }
   		standardTile("wakeTile", "device.wakeTile", width: 1, height: 1, canChangeIcon: false, canChangeBackground: false, decoration: "flat") {
			state "enabled", label: "Wake", action: "wakeupSystem", icon: ""
			state "disabled", label: "", action: "", icon: ""
		}

		main(["presence", "motion"])
		details(["presence", "motion", "toggle", "wakeTile"])
	}
}

def parse(String description) {
	log.debug "Parsing '${description}'"
    def cmd = description.split(':')
    switch (cmd[0]) {
    case "presence":
    	if (cmd[1] == "present") 
    		active()
        else 
            inactive()
        break
    case "toggle":
    	toggle()
        break
	case "wake":
    	wakeupSystem()
        break
    default:
    	log.debug "parse received unknown command: $cmd[0]"
    }
}

def installed() {
	initialize()
}
    
def updated() {
	initialize()
}

def initialize() {
	log.debug "initialize ${sensorType}"
    
	if (sensorType == "computer") 
   		sendEvent(name: 'wakeTile', value: 'enabled')
   	else
   		sendEvent(name: 'wakeTile', value: 'disabled')
}

def active() {
	log.debug "Active"
	sendEvent(name: 'presence', value: 'present')
   	sendEvent(name: 'motion', value: 'active')
}

def inactive() {
	log.debug "Inactive"
	sendEvent(name: 'presence', value: 'not present')
   	sendEvent(name: 'motion', value: 'inactive')
}

def toggle() {
	log.debug "Toggle forced..." 
    
	if (device.currentValue("presence") == "not present") 
		active()
    else 
    	inactive()
}


def wakeupSystem() {
	log.debug "Wakeup action for ${device.deviceNetworkId}"

	def wakehostaddress = "192.168.123.125:80"
	def uri = "/wakeup.php?host=${device.deviceNetworkId}"
	def hubAction = new physicalgraph.device.HubAction(
		method: "PUT",
		path: uri,
		headers: [HOST: wakehostaddress ]
	)
	hubAction
}
