package com.belajar.naraspeak.data.repository

import android.content.Context
import com.belajar.naraspeak.data.model.DataModel
import com.belajar.naraspeak.data.model.DataModelType
import com.belajar.naraspeak.data.webrtc.FirebaseClient
import com.belajar.naraspeak.data.webrtc.PeerConnectionObserver
import com.belajar.naraspeak.data.webrtc.WebRtcClient
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer

class VideoCallRepository(
    private val firebaseClient: FirebaseClient
) {

    private var currentUsername: String? = null
    private var currentTarget: String? = null
    private var remoteView: SurfaceViewRenderer? = null

    private lateinit var webRtcClient: WebRtcClient

    var connectionListener: WebRTCConnectionListener? = null


    fun login(
        username: String,
        statusListener: FirebaseClient.FirebaseStatusListener,
        context: Context
    ) {
        webRtcClient = WebRtcClient(
            context,
            username,
            object : PeerConnectionObserver() {
                override fun onAddStream(mediaStream: MediaStream?) {
                    super.onAddStream(mediaStream)
                    mediaStream?.videoTracks?.get(0)?.addSink(remoteView)
                }

                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                    super.onConnectionChange(newState)
                    newState?.let {
                        if (it == PeerConnection.PeerConnectionState.DISCONNECTED) {
                            connectionListener?.webRtcClosed()
                        }
                        if (it == PeerConnection.PeerConnectionState.CONNECTED) {
                            connectionListener?.webRtcConnected()
                        }
                    }
                }

                override fun onIceCandidate(iceCandidate: IceCandidate?) {
                    super.onIceCandidate(iceCandidate)
                    if (iceCandidate != null) {
                        webRtcClient.sendIceCandidate(iceCandidate, currentTarget.toString())
                    }

                }
            }
        )
        firebaseClient.login(username, object : FirebaseClient.FirebaseStatusListener,
            WebRtcClient.PeerListener {
            override fun onError() {
                statusListener.onError()

            }

            override fun onSuccess() {
                statusListener.onSuccess()
                currentUsername = username
                webRtcClient.peerListener = this
            }

            override fun onTransferDataToOtherPeer(model: DataModel) {
                firebaseClient.sendData(
                    model,
                    object : FirebaseClient.FirebaseStatusListener {
                        override fun onError() {

                        }

                        override fun onSuccess() {

                        }

                    }
                )
            }
        })
    }

    fun sendCallRequest(username: String, statusListener: FirebaseClient.FirebaseStatusListener) {
        firebaseClient.sendData(
            DataModel(
                target = username,
                sender = currentUsername,
                data = null,
                DataModelType.StartCall
            ), statusListener
        )
    }

    fun detectCallRequest(newEventListener: FirebaseClient.NewEventListener) {
        firebaseClient.observeIncomingData(object : FirebaseClient.NewEventListener {
            override fun onNewEvent(dataModel: DataModel) {
                when (dataModel.dataModelType) {
                    DataModelType.StartCall -> {
                        currentTarget = dataModel.sender
                        newEventListener.onNewEvent(dataModel)
                    }

                    DataModelType.Offer -> {
                        currentTarget = dataModel.sender

                        webRtcClient.onRemoteSessionReceived(
                            SessionDescription(
                                SessionDescription.Type.OFFER,
                                dataModel.data
                            )
                        )

                        webRtcClient.answer(dataModel.sender.toString())
                    }

                    DataModelType.Answer -> {
                        currentTarget = dataModel.sender

                        webRtcClient.onRemoteSessionReceived(
                            SessionDescription(
                                SessionDescription.Type.ANSWER,
                                dataModel.data
                            )
                        )

                    }

                    DataModelType.IceCandidate -> {
                        val gson = Gson()
                        val iceCandidate = gson.fromJson(dataModel.data, IceCandidate::class.java)
                        webRtcClient.addIceCandidate(iceCandidate)

                    }

                    null -> {}
                }
            }

        })
    }

    fun setRemoteView(surfaceViewRenderer: SurfaceViewRenderer) {
        webRtcClient.initRemoteViewRenderer(surfaceViewRenderer)
        remoteView = surfaceViewRenderer
    }

    fun setLocalView(surfaceViewRenderer: SurfaceViewRenderer) {
        webRtcClient.initLocalViewRenderer(surfaceViewRenderer)

    }

    fun startCall(target: String) {
        webRtcClient.call(target)
    }

    fun switchCamera() {
        webRtcClient.switchCamera()
    }

    fun muteMicrophone(isEnabled: Boolean) {
        webRtcClient.muteMicrophone(isEnabled)
    }

    fun disableCamera(isEnabled: Boolean) {
        webRtcClient.disableVideo(isEnabled)
    }

    fun disconnect() {
        webRtcClient.disconnect()
    }

    interface WebRTCConnectionListener {
        fun webRtcConnected()
        fun webRtcClosed()
    }


    companion object {
        private var instance: VideoCallRepository? = null
        fun getInstance(
            firebaseClient: FirebaseClient
        ): VideoCallRepository =
            instance ?: synchronized(this) {
                instance ?: VideoCallRepository(firebaseClient)
            }.also { instance = it }
    }

}