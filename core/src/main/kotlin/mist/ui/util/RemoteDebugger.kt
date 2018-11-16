/*
 * mist - interactive mips disassembler and decompiler
 * Copyright (C) 2018 Pawel Pastuszak
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package mist.ui.util

//import kotlinx.coroutines.launch
import mist.util.DecompLog
import mist.util.logTag

/** @author Kotcrab */
class RemoteDebugger(val log: DecompLog) {
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

    fun disonnect() {
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
