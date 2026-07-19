package com.voicebridge.data.network

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NearbyConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "NearbyConnManager"
    private val SERVICE_ID = "com.voicebridge.offline.p2p"
    private val STRATEGY = Strategy.P2P_CLUSTER

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    // Maps: Endpoint ID -> Username/Device Info
    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices

    // Active connection endpoints: Endpoint ID -> Device ID
    private val activeConnections = Collections.synchronizedMap(HashMap<String, String>())
    // Device ID -> Endpoint ID
    private val deviceToEndpointMap = Collections.synchronizedMap(HashMap<String, String>())
    // Cached Endpoint ID -> Username/Device Info (before connection finishes)
    private val pendingNames = Collections.synchronizedMap(HashMap<String, String>())

    private val _connectedPeers = MutableStateFlow<List<String>>(emptyList()) // List of connected device IDs
    val connectedPeers: StateFlow<List<String>> = _connectedPeers

    // Connected devices details for UI
    private val _connectedPeersList = MutableStateFlow<List<ConnectedDevice>>(emptyList())
    val connectedPeersList: StateFlow<List<ConnectedDevice>> = _connectedPeersList

    // Incoming packets flow
    private val _incomingPackets = MutableSharedFlow<MeshPacket>(extraBufferCapacity = 50)
    val incomingPackets: SharedFlow<MeshPacket> = _incomingPackets

    // Cache of recently processed packet IDs to prevent loops (thread-safe)
    private val recentlyProcessedPackets = Collections.synchronizedSet(LinkedHashSet<String>())
    private val maxCacheSize = 500

    // Queue for packets waiting for connection establishment
    private val pendingPacketQueue = Collections.synchronizedList(ArrayList<MeshPacket>())

    var onConnectedNodeCallback: ((endpointId: String, devId: String) -> Unit)? = null

    private var localUsername: String = "Unknown"
    private var localDeviceId: String = ""

    fun initialize(deviceId: String, username: String) {
        localDeviceId = deviceId
        localUsername = username
    }

    /**
     * Start broadcasting presence and scanning for other devices
     */
    fun startP2P() {
        if (localDeviceId.isEmpty()) return
        startAdvertising()
        startDiscovery()
    }

    fun restartP2P() {
        if (localDeviceId.isEmpty()) return
        Log.d(TAG, "Restarting P2P scanning & advertising...")
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        startAdvertising()
        startDiscovery()
    }

    fun stopP2P() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        activeConnections.clear()
        deviceToEndpointMap.clear()
        pendingPacketQueue.clear()
        _connectedPeers.value = emptyList()
        _discoveredDevices.value = emptyList()
        Log.d(TAG, "P2P stopped and disconnected.")
    }

    private fun startAdvertising() {
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        
        // Broadcast name format: deviceId:username
        val nameInfo = "$localDeviceId:$localUsername"
        
        connectionsClient.startAdvertising(
            nameInfo,
            SERVICE_ID,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Advertising started successfully.")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Advertising failed: ${e.message}")
        }
    }

    private fun startDiscovery() {
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Discovery started successfully.")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Discovery failed: ${e.message}")
        }
    }

    /**
     * Connect to a discovered device
     */
    fun connectToDevice(endpointId: String) {
        connectionsClient.requestConnection(
            "$localDeviceId:$localUsername",
            endpointId,
            connectionLifecycleCallback
        ).addOnFailureListener { e ->
            Log.e(TAG, "Request connection failed: ${e.message}")
        }
    }

    /**
     * Send packet to the mesh network
     */
    fun sendPacket(packet: MeshPacket) {
        scope.launch {
            // Add to recently processed to not relay our own sent packet back
            addToCache(packet.packetId)
            
            val json = packet.toJson()
            val payload = Payload.fromBytes(json.toByteArray(Charsets.UTF_8))
            
            // 1. Check if we have a direct connection to the recipient
            val directEndpoint = deviceToEndpointMap[packet.recipientId]
            if (directEndpoint != null && packet.recipientId != "EVERYONE") {
                Log.d(TAG, "Sending directly to endpoint: $directEndpoint for recipient: ${packet.recipientId}")
                connectionsClient.sendPayload(directEndpoint, payload)
            } else if (activeConnections.isNotEmpty()) {
                // 2. Broadcast / flood the packet to ALL active neighbors
                Log.d(TAG, "Flooding packet ${packet.packetId} to all neighbors (count: ${activeConnections.size})")
                val endpoints = activeConnections.keys.toList()
                connectionsClient.sendPayload(endpoints, payload)
            } else if (packet.recipientId != "EVERYONE") {
                // 3. No active connections right now. Check if recipient is discovered
                val discovered = _discoveredDevices.value.find { it.deviceId == packet.recipientId }
                if (discovered != null) {
                    Log.d(TAG, "Queueing packet ${packet.packetId} and connecting to discovered endpoint ${discovered.endpointId}")
                    pendingPacketQueue.add(packet)
                    connectToDevice(discovered.endpointId)
                } else {
                    Log.w(TAG, "No active connections or discovered endpoints for recipient: ${packet.recipientId}. Queueing packet.")
                    pendingPacketQueue.add(packet)
                }
            } else {
                Log.w(TAG, "Cannot broadcast EVERYONE packet right now: 0 active neighbors.")
            }
        }
    }

    /**
     * Handles routing of incoming packets
     */
    private fun routeIncomingPacket(packet: MeshPacket, fromEndpointId: String) {
        // 1. Prevent duplicate packet processing
        if (recentlyProcessedPackets.contains(packet.packetId)) {
            Log.d(TAG, "Packet ${packet.packetId} already processed. Dropping.")
            return
        }
        addToCache(packet.packetId)

        Log.d(TAG, "Processing packet ${packet.packetId} from $fromEndpointId. Sender: ${packet.senderId}, Recipient: ${packet.recipientId}")

        // 2. Process locally if it's for us or for everyone
        val isForUs = packet.recipientId == localDeviceId
        val isBroadcast = packet.recipientId == "EVERYONE"

        if (isForUs || isBroadcast) {
            scope.launch {
                _incomingPackets.emit(packet)
            }
        }

        // 3. Relay/Hop the message in the mesh network if necessary
        // Rules:
        // - If broadcast: Relay it further so other devices get it (since it needs to cover the whole mesh).
        // - If directed to someone else: Relay it further.
        // - ONLY relay if TTL > 1 (so it can decrement to 0 on the next hop).
        val shouldRelay = (!isForUs || isBroadcast) && packet.ttl > 1
        if (shouldRelay) {
            val relayedPacket = packet.copy(ttl = packet.ttl - 1)
            val json = relayedPacket.toJson()
            val payload = Payload.fromBytes(json.toByteArray(Charsets.UTF_8))
            
            // Send to all neighbors EXCEPT the one it arrived from
            val relayTargets = activeConnections.keys.filter { it != fromEndpointId }
            if (relayTargets.isNotEmpty()) {
                Log.d(TAG, "Relaying packet ${packet.packetId} to neighbors: $relayTargets (TTL remaining: ${relayedPacket.ttl})")
                connectionsClient.sendPayload(relayTargets, payload)
            }
        }
    }

    private fun addToCache(packetId: String) {
        synchronized(recentlyProcessedPackets) {
            if (recentlyProcessedPackets.size >= maxCacheSize) {
                val first = recentlyProcessedPackets.iterator().next()
                recentlyProcessedPackets.remove(first)
            }
            recentlyProcessedPackets.add(packetId)
        }
    }

    // Callbacks for discovery
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "Endpoint found: $endpointId (${info.endpointName})")
            val parts = info.endpointName.split(":")
            if (parts.size >= 2) {
                val devId = parts[0]
                val userName = parts[1]
                
                // Add to discovered devices
                val newList = _discoveredDevices.value.toMutableList()
                newList.removeAll { it.endpointId == endpointId || it.deviceId == devId }
                newList.add(DiscoveredDevice(endpointId, devId, userName))
                _discoveredDevices.value = newList
                
                // Cache name immediately for initiator fallback mapping
                pendingNames[endpointId] = info.endpointName

                // Auto connect to build mesh network automatically!
                // Lexicographical check to prevent simultaneous connection collisions, with fallback
                if (localDeviceId > devId) {
                    Log.d(TAG, "Lexicographical rule: $localDeviceId > $devId. Initiating connection immediately.")
                    connectToDevice(endpointId)
                } else {
                    Log.d(TAG, "Lexicographical rule: $localDeviceId <= $devId. Waiting 1500ms for peer to initiate connection.")
                    scope.launch {
                        kotlinx.coroutines.delay(1500)
                        if (!activeConnections.containsKey(endpointId) && _discoveredDevices.value.any { it.endpointId == endpointId }) {
                            Log.d(TAG, "Lexicographical fallback: peer did not initiate within 1500ms. Initiating connection now.")
                            connectToDevice(endpointId)
                        }
                    }
                }
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
            val newList = _discoveredDevices.value.toMutableList()
            newList.removeAll { it.endpointId == endpointId }
            _discoveredDevices.value = newList
        }
    }

    // Callbacks for connection life cycle
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d(TAG, "Connection initiated with $endpointId (${connectionInfo.endpointName})")
            pendingNames[endpointId] = connectionInfo.endpointName
            // In mesh, automatically accept connection
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            val endpointName = pendingNames.remove(endpointId) ?: ""
            if (result.status.isSuccess) {
                Log.d(TAG, "Connection successful with $endpointId")
                
                val parts = endpointName.split(":")
                if (parts.size >= 2) {
                    val devId = parts[0]
                    val userName = parts[1]
                    
                    activeConnections[endpointId] = devId
                    deviceToEndpointMap[devId] = endpointId
                    
                    _connectedPeers.value = activeConnections.values.toList()
                    
                    // Add to connected list
                    val newConnected = _connectedPeersList.value.toMutableList()
                    newConnected.removeAll { it.deviceId == devId }
                    newConnected.add(ConnectedDevice(endpointId, devId, userName))
                    _connectedPeersList.value = newConnected
                    
                    // Remove from discovered devices list since we are now connected
                    val newList = _discoveredDevices.value.toMutableList()
                    newList.removeAll { it.endpointId == endpointId || it.deviceId == devId }
                    _discoveredDevices.value = newList
                    
                    // Notify callback to trigger key sync / NODE_HELLO across mesh
                    scope.launch {
                        try {
                            onConnectedNodeCallback?.invoke(endpointId, devId)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in onConnectedNodeCallback: ${e.message}", e)
                        }
                    }

                    // Flush any queued packets waiting for connection
                    scope.launch {
                        val toSend = synchronized(pendingPacketQueue) {
                            val list = ArrayList(pendingPacketQueue)
                            pendingPacketQueue.clear()
                            list
                        }
                        for (queued in toSend) {
                            sendPacket(queued)
                        }
                    }
                }
            } else {
                Log.e(TAG, "Connection failed with $endpointId: ${result.status.statusMessage}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from $endpointId")
            val devId = activeConnections.remove(endpointId)
            if (devId != null) {
                deviceToEndpointMap.remove(devId)
            }
            _connectedPeers.value = activeConnections.values.toList()
            
            // Remove from connected list
            val newConnected = _connectedPeersList.value.toMutableList()
            newConnected.removeAll { it.endpointId == endpointId }
            _connectedPeersList.value = newConnected
        }
    }

    // Callbacks for receiving payload (bytes representing packets)
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val bytes = payload.asBytes()
                if (bytes != null) {
                    val json = String(bytes, Charsets.UTF_8)
                    val packet = MeshPacket.fromJson(json)
                    if (packet != null) {
                        routeIncomingPacket(packet, endpointId)
                    }
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Tracking progress (useful for larger payloads like base64 images/files if needed)
        }
    }
}

data class DiscoveredDevice(
    val endpointId: String,
    val deviceId: String,
    val username: String
)

data class ConnectedDevice(
    val endpointId: String,
    val deviceId: String,
    val username: String
)
