/**
 *  Mailman
 *
 *  Copyright 2015 Bruce Adelsman
 *
 */
definition(
    name: "Mailman",
    namespace: "naissan",
    author: "Bruce Adelsman",
    description: "Monitors mailbox sensor, setting the mailbox status (full or empty), and send a reminder if the mailbox is not emptied.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-MailboxMonitor.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-MailboxMonitor@2x.png"
)

preferences {
	section("Mailbox sensor") {
		input "sensor", "device.mailboxSensor"
	}
    section("Delivery") {
       	input "deliveryNotice", "bool", title: "Push notification", required: false, defaultValue: true
 	}   
    section("Reminder") {
        input "reminderHour", "enum", title: "Reminder time (default: 8 pm)?", required: false, options: [ "5 pm", "6 pm", "7 pm", "8 pm", "9 pm", "10 pm" ]
       	input "phoneNumber", "phone", title: "Send a text message to:", required: false
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

	// monitor the mailbox
    subscribe(sensor, "mailbox", mailboxAccessed)

	TimeZone.setDefault(location.timeZone)
	def hourString = "8 pm"
    if (reminderHour) {
    	hourString = reminderHour
    }
   	Date reminderTime = new Date().parse("h a", hourString)
    log.debug "Mail reminder set for $reminderTime"
    // schedule reminder check for each day
    schedule(reminderTime, forgottenMail)
}

def mailboxAccessed(evt) {

	log.debug "Mailbox accessed event: $evt.value change: $evt.isStateChange"
    if (evt.value == "full") {
	    // if deliveryNotice push message, "Mailbox delivery"
        if (deliveryNotice) {
        	log.info "Mail was delivered"
        	sendPush "Mailbox delivery"
        }
    } else {
    	if (evt.isStateChange) {
       		log.info "Mail was picked up"
            // sendPush "Mailbox retrieval"
        } else if (deliveryNotice) {
        	log.info "Mailbox access was detected"
        	sendPush "Mailbox access"
        }
    }
}

def forgottenMail() {
	// if mailbox is still full
    if (sensor.currentValue("mailbox") == "full") {
    	def msg = "Mailbox has not been checked today"
	    // if textmsg send as text, else push
    	if (phoneNumber) {
			sendSms phoneNumber, msg
        } else {
        	sendPush msg
        }
    }
}