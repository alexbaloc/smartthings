/** 
 *  
 *  Generic Camera Device v1.1.01302017
 *
 *  Copyright 2017 patrick@patrickstuart.com
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

metadata {
	definition (name: "Panasonic VIERA St60", namespace: "alexbaloc", author: "alexbaloc") {
    capability "Switch"
			command "mute" 
			command "source"
			command "menu"    
      command "close"
	}

    preferences {
    input("TvIP", "string", title:"TV IP Address", description: "Please enter your TV's IP Address", required: true, displayDuringSetup: true)
	}
    
	simulator {
    
	}

    tiles {
        standardTile("switch", "device.switch", width: 1, height: 1, canChangeIcon: true) {
            state "default", label:'TV', action:"switch.off", icon:"st.Electronics.electronics15", backgroundColor:"#ffffff"
        }
        standardTile("power", "device.switch", width: 1, height: 1, canChangeIcon: false) {
            state "default", label:'', action:"switch.off", decoration: "flat", icon:"st.thermostat.heating-cooling-off", backgroundColor:"#ffffff"
        }
        standardTile("mute", "device.switch", decoration: "flat", canChangeIcon: false) {
            state "default", label:'Mute', action:"mute", icon:"st.custom.sonos.muted", backgroundColor:"#ffffff"
        }    
        standardTile("source", "device.switch", decoration: "flat", canChangeIcon: false) {
            state "default", label:'Source', action:"source", icon:"st.Electronics.electronics15"
        }
       standardTile("menu", "device.switch", decoration: "flat", canChangeIcon: false) {
            state "default", label:'Menu', action:"menu", icon:"st.vents.vent"
        }        
        main "switch"
        details([ "power", "mute", "source", "menu"])
    }
}

def parse(String description) {
    log.debug "Parsing '${description}'"
    return null;
}

def off() {
	log.debug "Turning TV OFF"  
  	sendCommand('command', 'X_SendKey', 'POWER')
//  sendEvent(name:"Command", value: "Power Off", displayed: true) 
}

def mute() {
	log.debug "Toggle Mute"  
	sendCommand('command', 'X_SendKey', 'MUTE')
    //sendEvent(name:"Command", value: "Mute", displayed: true) 
}

// handle commands
def sendCommand(type, action, commandCode) {
    def host = TvIP 
    def port = 55000
    def command = "<X_KeyEvent>NRC_$commandCode-ONOFF</X_KeyEvent>"
    def hosthex = convertIPtoHex(host).toUpperCase() //thanks to @foxxyben for catching this
    def porthex = convertPortToHex(port).toUpperCase()
    device.deviceNetworkId = "$hosthex:$porthex" 
    
    log.debug "The device id configured is: $device.deviceNetworkId"
            
    def url
    def urn
    if (type == "command") {
        url = "/nrc/control_0"
        urn = "panasonic-com:service:p00NetworkControl:1"
    } else if (type == 'render') {
        url = "/dmr/control_0"
        urn = "schemas-upnp-org:service:RenderingControl:1"
    }

    log.debug "ULR: $url, URN: $urn"
  
    def headers = ["Content-Type": "text/xml; charset=\"utf-8\""] 
    headers.put("HOST", "$host:$port")
    headers.put("SOAPACTION", "\"urn:$urn#$action\"")
    
    log.debug "The Header is $headers"

    def soapBody = "<?xml version='1.0' encoding='utf-8'?> \
                    <s:Envelope xmlns:s='http://schemas.xmlsoap.org/soap/envelope/' s:encodingStyle='http://schemas.xmlsoap.org/soap/encoding/'> \
                      <s:Body> \
                        <u:$action xmlns:u='urn:$urn'> \
                        $command \
                        </u:$action> \
                      </s:Body> \
                    </s:Envelope>";

    log.debug soapBody

    try {
    def hubAction = new physicalgraph.device.HubAction(
    	method: "POST",
    	path: "http://$host:$port$url",
     	body: soapBody,
    	headers: headers
    )
        	
    hubAction.options = [outputMsgToS3:true]
    log.debug hubAction
    hubAction
    }
    catch (Exception e) {
    	log.debug "Hit Exception $e on $hubAction"
    }
    
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex

}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    log.debug hexport
    return hexport
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}


private String convertHexToIP(hex) {
	log.debug("Convert hex to ip: $hex") 
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private getHostAddress() {
	def parts = device.deviceNetworkId.split(":")
    log.debug device.deviceNetworkId
	def ip = convertHexToIP(parts[0])
	def port = convertHexToInt(parts[1])
	return ip + ":" + port
}