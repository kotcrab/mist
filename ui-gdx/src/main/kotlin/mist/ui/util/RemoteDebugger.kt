package mist.ui.util

//import kotlinx.coroutines.launch
import mist.util.MistLogger
import mist.util.logTag

class RemoteDebugger(val logger: MistLogger) {
  private val tag = logTag()

  //    private var remoteDebugger: PpssppClientV2? = null
  private val listeners = mutableListOf<RemoteDebuggerListener>()

  fun connect() {
//        if (remoteDebugger != null) return
//        remoteDebugger = PpssppClientV2()
//        launch(KtxAsync) {
//            log.info(tag, "remote debugger connecting")
//            remoteDebugger!!.receiveMessages(KtxAsync) { msg ->
//                log.trace(tag, "got message ${msg::class.simpleName}")
//                listeners.forEach { it.handleMessage(msg) }
//            }
//            remoteDebugger!!.connect()
//            log.info(tag, "remote debugger connected")
//        }
  }

  fun disconnect() {
//        if (remoteDebugger == null) return
//        log.info(tag, "remote debugger disconnected")
//        remoteDebugger!!.close()
//        remoteDebugger = null
  }

  fun addListener(listener: RemoteDebuggerListener) {
    listeners.add(listener)
  }

  fun remoteListener(listener: RemoteDebuggerListener) {
    listeners.remove(listener)
  }

//    fun getClient(): PpssppClientV2? {
//        return remoteDebugger
//    }
}

interface RemoteDebuggerListener {
  fun handleMessage(msg: PpssppMessage)
}
