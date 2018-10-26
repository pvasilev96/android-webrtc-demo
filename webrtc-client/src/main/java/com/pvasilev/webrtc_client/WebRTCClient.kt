package com.pvasilev.webrtc_client

import android.content.Context
import org.webrtc.*

class WebRTCClient(context: Context, iceServers: List<PeerConnection.IceServer>) {
    private val factory: PeerConnectionFactory

    private val peerConnection: PeerConnection?

    private val sdpMediaConstraints: MediaConstraints

    private val pcObserver: PeerConnection.Observer = PCObserver()

    private val sdpObserver: SdpObserver = SDPObserver()

    init {
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .createInitializationOptions()
        )
        factory = PeerConnectionFactory.builder()
                .createPeerConnectionFactory()
        peerConnection = factory.createPeerConnection(iceServers, pcObserver)
        peerConnection?.addTrack(createVideoTrack())
        peerConnection?.addTrack(createAudioTrack())
        sdpMediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
    }

    fun createOffer() {
        peerConnection?.createOffer(sdpObserver, sdpMediaConstraints)
    }

    fun createAnswer() {
        peerConnection?.createAnswer(sdpObserver, sdpMediaConstraints)
    }

    private fun createVideoTrack(): VideoTrack {
        TODO("not implemented yet")
    }

    private fun createAudioTrack(): AudioTrack {
        val audioSource = factory.createAudioSource(MediaConstraints())
        return factory.createAudioTrack(AUDIO_TRACK_ID, audioSource)
    }

    inner class PCObserver : PeerConnection.Observer {
        override fun onIceCandidate(iceCandidate: IceCandidate?) {
        }

        override fun onDataChannel(dataChannel: DataChannel) {
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) {
        }

        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
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
        override fun onCreateSuccess(origSdp: SessionDescription?) {
        }

        override fun onCreateFailure(error: String?) {
        }

        override fun onSetSuccess() {
        }

        override fun onSetFailure(error: String?) {
        }
    }

    companion object {
        const val VIDEO_TRACK_ID = "ARDAMSv0"
        const val AUDIO_TRACK_ID = "ARDAMSa0"
    }
}