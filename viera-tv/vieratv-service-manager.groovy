/**
 *  Generic UPnP Service Manager
 *
 *  Copyright 2016 SmartThings
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
		name: "Panasonic Viera ST60 Service Manager",
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

def getUpnpSearchTarger() {
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


/*
// implementation from https://github.com/logic-dev/VieraControl/blob/master/VieraControl/Client.cs

private bool _FindTV()
        {
            IPEndPoint LocalEndPoint = new IPEndPoint(IPAddress.Any, 60000);
            IPEndPoint MulticastEndPoint = new IPEndPoint(IPAddress.Parse("239.255.255.250"), 1900);

            Socket UdpSocket = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, ProtocolType.Udp);

            UdpSocket.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true);
            UdpSocket.Bind(LocalEndPoint);
            UdpSocket.SetSocketOption(SocketOptionLevel.IP, SocketOptionName.AddMembership, new MulticastOption(MulticastEndPoint.Address, IPAddress.Any));
            UdpSocket.SetSocketOption(SocketOptionLevel.IP, SocketOptionName.MulticastTimeToLive, 2);
            UdpSocket.SetSocketOption(SocketOptionLevel.IP, SocketOptionName.MulticastLoopback, true);

            Console.WriteLine("UDP-Socket setup done...\r\n");

            string SearchString = "M-SEARCH * HTTP/1.1\r\nHOST:239.255.255.250:1900\r\nMAN:\"ssdp:discover\"\r\nST:urn:panasonic-com:device:p00RemoteController:1\r\nMX:1\r\n\r\n";

            UdpSocket.SendTo(Encoding.UTF8.GetBytes(SearchString), SocketFlags.None, MulticastEndPoint);

            Console.WriteLine("M-Search sent...\r\n");

            byte[] ReceiveBuffer = new byte[64000];

            int ReceivedBytes = 0;

            Stopwatch s = new Stopwatch();
            s.Start();

            bool found = false;

            while (found)
            {
                if (UdpSocket.Available > 0)
                {
                    ReceivedBytes = UdpSocket.Receive(ReceiveBuffer, SocketFlags.None);

                    if (ReceivedBytes > 0)
                    {
                        Console.WriteLine(Encoding.UTF8.GetString(ReceiveBuffer, 0, ReceivedBytes));
                        found = true;
                        return true;
                    }
                    else
                        return false;
                }

                if (s.Elapsed > TimeSpan.FromSeconds(5))
                {
                    found = true;
                }
            }

            return false;
        }
    }
*/

void ssdpDiscover() {
    log.debug "ssdpDiscover"
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery ${searchTarget}", physicalgraph.device.Protocol.LAN))
}

void ssdpSubscribe() {
    log.debug "subscribe"
	subscribe(location, "ssdpTerm.${searchTarget}", ssdpHandler)
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
				"data": [
					"mac": selectedDevice.value.mac,
					"TvIP": selectedDevice.value.networkAddress,
					"port": selectedDevice.value.deviceAddress
				]
			])

      child.sync(selectedDevice.value.networkAddress, selectedDevice.value.deviceAddress)
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
				child.sync(parsedEvent.networkAddress, parsedEvent.deviceAddress)
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
