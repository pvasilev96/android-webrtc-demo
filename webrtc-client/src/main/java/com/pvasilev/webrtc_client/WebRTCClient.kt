package com.pvasilev.webrtc_client

import android.content.Context
import org.webrtc.*

class WebRTCClient(private val context: Context, private val eglBase: EglBase, private val events: PeerConnectionEvents) {
    private val factory: PeerConnectionFactory

    private val pcObserver: PeerConnection.Observer

    private val sdpObserver: SdpObserver

    private val sdpMediaConstraints: MediaConstraints

    private var peerConnection: PeerConnection? = null

    private var videoCapturer: VideoCapturer? = null

    private var localSink: VideoSink? = null

    private var remoteSink: VideoSink? = null

    private var queuedRemoteCandidates: MutableList<IceCandidate>? = mutableListOf()

    private var isInitiator: Boolean = false

    init {
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .createInitializationOptions()
        )
        factory = PeerConnectionFactory.builder()
                .createPeerConnectionFactory()
        pcObserver = PCObserver()
        sdpObserver = SDPObserver()
        sdpMediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
    }

    fun createPeerConnection(localSink: VideoSink?, remoteSink: VideoSink?, videoCapturer: VideoCapturer, iceServers: List<PeerConnection.IceServer>) {
        this.localSink = localSink
        this.remoteSink = remoteSink
        this.videoCapturer = videoCapturer
        peerConnection = factory.createPeerConnection(iceServers, pcObserver)?.apply {
            addTrack(createVideoTrack(videoCapturer))
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

    private fun createVideoTrack(videoCapturer: VideoCapturer): VideoTrack {
        val videoSource = factory.createVideoSource(videoCapturer.isScreencast)
        val videoTrack = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource)
        val surfaceTextureHelper = SurfaceTextureHelper.create(CAPTURE_THREAD_NAME, eglBase.eglBaseContext)
        videoCapturer.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
        videoCapturer.startCapture(WIDTH, HEIGHT, FPS)
        videoTrack.addSink(localSink)
        return videoTrack
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

    private inner class PCObserver : PeerConnection.Observer {
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

    private inner class SDPObserver : SdpObserver {
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
        private const val VIDEO_TRACK_ID = "ARDAMSv0"
        private const val AUDIO_TRACK_ID = "ARDAMSa0"
        private const val CAPTURE_THREAD_NAME = "CaptureThread"
        private const val WIDTH = 1280
        private const val HEIGHT = 720
        private const val FPS = 30 * 1000
    }
}