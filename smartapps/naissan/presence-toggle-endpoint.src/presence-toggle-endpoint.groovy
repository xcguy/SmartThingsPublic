/**
 *  Presence Toggle Endpoint
 *
 *  Copyright 2015 Bruce Adelsman
 *
 */
definition(
    name: "Presence Toggle Endpoint",
    namespace: "naissan",
    author: "Bruce Adelsman",
    description: "Endpoint for presence detector system.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Solution/people-cars-active.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Solution/people-cars-active@2x.png",
    oauth: true)


preferences {
	section("Presence Sensors to Monitor") {
		input "sensors", "capability.presenceSensor", title: "Which Presence Sensors?", multiple: true
	}
}

mappings {

	path("/sensors") {
		action: [
			GET: "listSensors"
		]
	}
	path("/sensors/:id") {
		action: [
			GET: "showSensors"
		]
	}
	path("/sensors/:id/:command") {
		action: [
			GET: "updateSensors"
		]
	}
    
}

def installed() {}

def updated() {}


//switches
def listSensors() {
	sensors.collect{device(it,"sensor")}
}

def showSensors() {
	show(sensors, "sensor")
}
void updateSensors() {
	update(sensors)
}



def deviceHandler(evt) {}

private void update(devices) {
	log.debug "update: params: ${params}, devices: $devices.id"
    
    
	//def command = request.JSON?.command
    def command = params.command
	if (command) 
    {
		def device = devices.find { it.id == params.id }
		if (!device) 
			httpError(404, "Sensor not found")
		else {
        	switch (command) {
            case "toggle":
            	device.toggle()
                break
            case "active":
            case "on":
            	device.active()
                break
            case "inactive":
            case "off":
            	device.inactive()
                break
            default:
            	log.warn "update received unknown command: $command"
       		}
		}
	}
}

private show(devices, type) {
	def device = devices.find { it.id == params.id }
	if (!device) {
		httpError(404, "Sensor not found")
	}
	else {
		def attributeName = type == "motionSensor" ? "motion" : type
		def s = device.currentState(attributeName)
		[id: device.id, label: device.displayName, value: s?.value, unitTime: s?.date?.time, type: type]
	}
}


private device(it, type) {
	it ? [id: it.id, label: it.label, type: type] : null
}