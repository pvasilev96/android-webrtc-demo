package com.pvasilev.webrtc_client

import android.content.Context
import org.webrtc.*

class WebRTCClient(context: Context, private val events: PeerConnectionEvents) {
    private val factory: PeerConnectionFactory

    private val pcObserver: PeerConnection.Observer = PCObserver()

    private val sdpObserver: SdpObserver = SDPObserver()

    private val sdpMediaConstraints: MediaConstraints

    private var peerConnection: PeerConnection? = null

    private var queuedRemoteCandidates: MutableList<IceCandidate>? = mutableListOf()

    private var isInitiator: Boolean = false

    init {
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .createInitializationOptions()
        )
        factory = PeerConnectionFactory.builder()
                .createPeerConnectionFactory()
        sdpMediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
    }

    fun createPeerConnection(iceServers: List<PeerConnection.IceServer>) {
        peerConnection = factory.createPeerConnection(iceServers, pcObserver)?.apply {
            addTrack(createVideoTrack())
            addTrack(createAudioTrack())
        }
    }

    fun createOffer() {
        isInitiator = true
        peerConnection?.createOffer(sdpObserver, sdpMediaConstraints)
    }

    fun createAnswer() {
        if (isInitiator) throw IllegalStateException("caller must receive remote answer")
        peerConnection?.createAnswer(sdpObserver, sdpMediaConstraints)
    }

    fun setRemoteDescription(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(sdpObserver, sdp)
    }

    fun addRemoteCandidate(iceCandidate: IceCandidate) {
        if (queuedRemoteCandidates != null) {
            queuedRemoteCandidates?.add(iceCandidate)
        } else {
            peerConnection?.addIceCandidate(iceCandidate)
        }
    }

    private fun createVideoTrack(): VideoTrack {
        TODO("not implemented yet")
    }

    private fun createAudioTrack(): AudioTrack {
        val audioSource = factory.createAudioSource(MediaConstraints())
        return factory.createAudioTrack(AUDIO_TRACK_ID, audioSource)
    }

    private fun drainCandidates() {
        if (queuedRemoteCandidates != null) {
            queuedRemoteCandidates!!.forEach { peerConnection?.addIceCandidate(it) }
            queuedRemoteCandidates = null
        }
    }

    inner class PCObserver : PeerConnection.Observer {
        override fun onIceCandidate(iceCandidate: IceCandidate) {
            events.onIceCandidate(iceCandidate)
        }

        override fun onDataChannel(dataChannel: DataChannel) {
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) {
        }

        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
            when (newState) {
                PeerConnection.IceConnectionState.CONNECTED -> events.onIceConnected()
                PeerConnection.IceConnectionState.FAILED -> events.onIceFailed()
                PeerConnection.IceConnectionState.DISCONNECTED -> events.onIceDisconnected()
                else -> {
                }
            }
        }

        override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
        }

        override fun onAddStream(mediaStream: MediaStream?) {
        }

        override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
        }

        override fun onIceCandidatesRemoved(iceCandidates: Array<out IceCandidate>?) {
        }

        override fun onRemoveStream(mediaStream: MediaStream?) {
        }

        override fun onRenegotiationNeeded() {
        }

        override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
        }
    }

    inner class SDPObserver : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription) {
            peerConnection?.setLocalDescription(sdpObserver, sdp)
        }

        override fun onCreateFailure(error: String?) {
        }

        override fun onSetSuccess() {
            if (isInitiator) {
                if (peerConnection?.remoteDescription == null) {
                    events.onLocalDescription(peerConnection!!.localDescription)
                } else {
                    drainCandidates()
                }
            } else {
                if (peerConnection?.localDescription != null) {
                    events.onLocalDescription(peerConnection!!.localDescription)
                    drainCandidates()
                }
            }
        }

        override fun onSetFailure(error: String?) {
        }
    }

    companion object {
        const val VIDEO_TRACK_ID = "ARDAMSv0"
        const val AUDIO_TRACK_ID = "ARDAMSa0"
    }
}