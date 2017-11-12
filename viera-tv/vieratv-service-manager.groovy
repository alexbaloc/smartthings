/**
 *  Panasonic Viera ST60 Service manager
 *
 *  Copyright 2017 alexbaloc
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
		name: "Panasonic Viera TV Service Manager",
		namespace: "alexbaloc",
		author: "alexbaloc",
		description: "Service Manager used to disover Panasonic Viera ST60 devices",
		category: "SmartThings Labs",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
		iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "deviceDiscovery", title: "Searching for Panasonic Viera devices", content: "deviceDiscovery")
}

def getUpnpSearchTarget() {
  return "urn:panasonic-com:device:p00RemoteController:1"
}
def deviceDiscovery() {

	def options = [:]
	def devices = getVerifiedDevices()
	devices.each {
		def value = it.value.name ?: "Viera Panasonic ${it.value.ssdpUSN.split(':')[1][-3..-1]}"
		def key = it.value.mac
		options["${key}"] = value
	}

	ssdpSubscribe()

	ssdpDiscover()
	verifyDevices()

	return dynamicPage(name: "deviceDiscovery", title: "Discovery Started!", nextPage: "", refreshInterval: 5, install: true, uninstall: true) {
		section("Please wait while searching for any Panasonic Viera TVs. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.") {
			input "selectedDevices", "enum", required: false, title: "Select Devices to setup (${options.size() ?: 0} found)", multiple: true, options: options
		}
	}
}

def installed() {
	initialize()
}
/*
def uninstalled() {
  removeChildDevices(getDevices())
}

private removeChildDevices(delete) {
  delete.each {
    deleteChildDevice(it.deviceNetworkId)
  }
}
*/

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	unsubscribe()
	unschedule()

	ssdpSubscribe()

	if (selectedDevices) {
		addDevices()
	}

	runEvery5Minutes("ssdpDiscover")
}

void ssdpDiscover() {
    log.debug "ssdpDiscover"
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery ${getUpnpSearchTarget()}", physicalgraph.device.Protocol.LAN))
}

void ssdpSubscribe() {
    log.debug "subscribe"
	subscribe(location, "ssdpTerm.${getUpnpSearchTarget()}", ssdpHandler)
}

Map verifiedDevices() {
	def devices = getVerifiedDevices()
	def map = [:]
	devices.each {
		def value = it.value.name ?: "Panasonic Viera ${it.value.ssdpUSN.split(':')[1][-3..-1]}"
		def key = it.value.mac
		map["${key}"] = value
	}
	map
}

void verifyDevices() {
  log.debug "verify devices called"
	def devices = getDevices().findAll { it?.value?.verified != true }
	devices.each {
		int port = convertHexToInt(it.value.deviceAddress)
		String ip = convertHexToIP(it.value.networkAddress)
		String host = "${ip}:${port}"
		sendHubCommand(new physicalgraph.device.HubAction("""GET ${it.value.ssdpPath} HTTP/1.1\r\nHOST: $host\r\n\r\n""", physicalgraph.device.Protocol.LAN, host, [callback: deviceDescriptionHandler]))
	}
}

def getVerifiedDevices() {
	getDevices().findAll{ it.value.verified == true }
}

def getDevices() {
	if (!state.devices) {
		state.devices = [:]
	}
	state.devices
}

def addDevices() {
	def devices = getDevices()

  log.debug "add devices - $devices"

	selectedDevices.each { dni ->
   log.debug "add devices for device - $dni"
		def selectedDevice = devices.find { it.value.mac == dni }
		def d
		if (selectedDevice) {
      log.debug "selected device - $selectedDevice"
			d = getChildDevices()?.find {
				it.deviceNetworkId == selectedDevice.value.mac
			}
		} else {
      log.debug "no selection"
    }

		if (!d) {
			log.debug "Creating New TV with dni: ${selectedDevice.value.mac}"
			def child = addChildDevice("alexbaloc", "Panasonic VIERA ST60", selectedDevice.value.mac, selectedDevice?.value.hub, [
				"label": selectedDevice?.value?.name ?: "Viera TV",
        //data below cannot be used in the client device
				"data": [
					"mac": selectedDevice.value.mac,
					"ip": selectedDevice.value.networkAddress,
					"port": selectedDevice.value.deviceAddress
				]
			])

      def ip = convertHexToIP(selectedDevice.value.networkAddress)
      def port = convertHexToInt(selectedDevice.value.deviceAddress)
      child.sync(ip, port)
		} else {
      log.debug "nothing to add"
    }
	}
}

def ssdpHandler(evt) {
  log.debug "handler $evt"

	def description = evt.description
	def hub = evt?.hubId

	def parsedEvent = parseLanMessage(description)
	parsedEvent << ["hub":hub]

  log.debug "parsedEvent : $parsedEvent"

	def devices = getDevices()
	String ssdpUSN = parsedEvent.ssdpUSN.toString()

  log.debug "ssdpUSN : $ssdpUSN"

	if (devices."${ssdpUSN}") {
    log.debug "ssdpHandler  - $ssdpUSN already exists"
		def d = devices."${ssdpUSN}"
		if (d.networkAddress != parsedEvent.networkAddress || d.deviceAddress != parsedEvent.deviceAddress) {
			d.networkAddress = parsedEvent.networkAddress
			d.deviceAddress = parsedEvent.deviceAddress
			def child = getChildDevice(parsedEvent.mac)
			if (child) {
        log.debug "ssdpHandler  - synch child: $child"

        def ip = convertHexToIP(parsedEvent.networkAddress)
        def port = convertHexToInt(parsedEvent.deviceAddress)

				child.sync(ip, port)
			}
		}
	} else {
    log.debug "ssdpHandler  - adding new usn $ssdpUSN"
		devices << ["${ssdpUSN}": parsedEvent]
    log.debug "ssdpHandler  - devices = $devices"
	}
}

void deviceDescriptionHandler(physicalgraph.device.HubResponse hubResponse) {
	def body = hubResponse.xml
	def devices = getDevices()
	def device = devices.find { it?.key?.contains(body?.device?.UDN?.text()) }
	if (device) {
		device.value << [name: body?.device?.roomName?.text(), model:body?.device?.modelName?.text(), serialNumber:body?.device?.serialNum?.text(), verified: true]
	}
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}
