/**
 *  NodeMCU Blinds Switch Device Handler
 *
 *  Copyright 2018 Viet Nguyen
 */

import java.security.MessageDigest

preferences {
    input("serverIP", "text", title: "NodeMCU Server IP Address")
}

metadata {
    definition (name: "NodeMCU Blinds", namespace: "smartthings-users", author: "Viet Nguyen") {
        capability "Switch"
        capability "Refresh"
        capability "Polling"
        command "subscribe"
        command "setOffline"
    }

    simulator {}

    tiles(scale: 2) {
        multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"off"
                attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"On"
                attributeState "offline", label:'${name}', icon:"st.switches.switch.off", backgroundColor:"#cccccc"
            }
        }
        standardTile("refresh", "device.switch", inactiveLabel: false, height: 2, width: 2, decoration: "flat") {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
		
        main "switch"
        details(["switch","refresh"])
    }

}

def setLevel(value) {
	log.trace "setLevel($value)"
    sendEvent(name: "level", value: value)
    
}

// handle commands
def on() {
    sendEvent(name: "switch", value: 'on')
    apiGet("open")
}

def off() {
    sendEvent(name: "switch", value: 'off')
    apiGet("close")
}

private apiGet(action) {


    log.debug "Posting Body: NODEMCU"
	def result = new physicalgraph.device.HubAction(
        method:     'GET',
        path:       '/'+action,
        headers:    [
            HOST:       settings.serverIP + ':80',
            'Content-Type': 'application/json'
        ],
        body: []
    )

    return sendHubCommand(result)

}

def refresh() {
 	log.debug "Executing NodeMCU Blinds refresh'"
	poll()
}

def poll() {
 	log.debug "Executing NodeMCU Blinds POLL'"
    
    def cmd = new physicalgraph.device.HubAction([
        method:     'GET',
        path:       '/status',
        headers:    [
            HOST:       settings.serverIP + ':80',
            Accept:     "*/*"
        ]
    ], '', [callback: calledBackHandler])
	
    def result = sendHubCommand(cmd)
    
    log.debug "cmd: $cmd"
    log.debug "result: $result"
}

void calledBackHandler(physicalgraph.device.HubResponse hubResponse) {
	def body = hubResponse.json;
    	log.debug "body $body"
        if(body.isOpen == 1){
        	sendEvent(name: "switch", value: 'on')
        }else if(body.isOpen == 0){
        	sendEvent(name: "switch", value: 'off')
        }else{
        	log.debug "OFFFFFFFLINE"
        	setOffline()
        }
}

def updated() {
 	log.debug "Executing NodeMCU Blinds UPDATED'"
    unschedule()
    runEvery1Minute(poll)
}

def setOffline() {
    sendEvent(name: "switch", value: "offline", descriptionText: "The device is offline")
}
