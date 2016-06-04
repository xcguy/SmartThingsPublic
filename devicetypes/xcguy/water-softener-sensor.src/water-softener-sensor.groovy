metadata {
	definition (name: "Water Softener Sensor", namespace: "xcguy", author: "Bruce Adelsman", oauth: true) {
		capability "Sensor"
        capability "Actuator"
        
        attribute "lowSaltFlag", "enum", [ "On, Off" ]
        attribute "saltLevel", "number"
        
        command "updateSaltLevel", ["number" ] 
    }


	preferences {
    	input "lowSaltLevel", "number", title: "Low salt warning percentage?", defaultValue: 20, required: false
	}
    
    // simulator metadata
	simulator {
		for (int i = 2; i <= 100; i += 10) {
			status "${i}%": "saltLevel: $i %"
		}

	}

    
	// UI tile definitions
	tiles {
		valueTile("saltLevel", "device.saltLevel", width: 2, height: 2) {
			state("default", label:'${currentValue}%', 
            	backgroundColors:[
					[value: 10, color: "#FF0000"],
					[value: 30, color: "#FF9933"],
					[value: 50, color: "#FFFF00"],
					[value: 70, color: "#66FF99"],
					[value: 90, color: "#33CC33"],
					[value: 100, color: "#00AA00"]
				]
			)
 
		}

		main("saltLevel")
		details("saltLevel")
	}
}

    
// Parse incoming device messages to generate events
def parse(String description) {
	log.debug "Parsing ... ${description}"
    def name = parseName(description)
	def value = parseValue(description)
    if (value != null) {
		createEvent(name: name, value: value)
    }
}


def updateSaltLevel(value) {
	log.debug "updateSaltLevel ${value}"
    if (value != null) {
		sendEvent(name: "saltLevel", value: value)
    	if ((value as int) <= (device.currentValue("lowSaltLevel") as int)) {
	    	if (!(device.currentValue("lowSaltFlag") == "On")) {
    			log.debug "setting low salt flag to on"
   				sendEvent(name: "lowSaltFlag", value: "On")
        	}
    	} else if (!(device.currentValue("lowSaltFlag") == "Off")) {
    		log.debug "setting low salt flag to off"
   			sendEvent(name: "lowSaltFlag", value: "Off")
    	}
	}
}

private String parseName(String description) {
	if (description?.startsWith("saltLevel:")) {
		return "saltLevel"
	} 
	null
}

private String parseValue(String description) {
	if (description?.startsWith("saltLevel:")) {
		def pct = (description - "saltLevel:" - "%").trim()
		if (pct.isNumber()) {
			return Math.round(new BigDecimal(pct)).toString()
		}
	}
	null
}
