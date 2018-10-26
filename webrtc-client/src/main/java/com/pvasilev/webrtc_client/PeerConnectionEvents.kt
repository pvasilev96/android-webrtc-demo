package com.pvasilev.webrtc_client

import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface PeerConnectionEvents {
    fun onLocalDescription(sdp: SessionDescription)

    fun onIceCandidate(iceCandidate: IceCandidate)

    fun onIceConnected()

    fun onIceDisconnected()

    fun onIceFailed()
}