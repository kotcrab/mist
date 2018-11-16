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

//import kotlinx.coroutines.CommonPool
//import kotlinx.coroutines.channels.Channel
//import kotlinx.coroutines.launch

//class PpssppClientV2(private val ip: String = "127.0.0.1", private val port: Int = 54230) {
//    private lateinit var socket: Socket
//    @Volatile
//    private lateinit var output: LittleEndianDataOutputStream
//    private lateinit var input: LittleEndianDataInputStream
//
//    private val socketChannel = Channel<PpssppMessage>(10)
//
//    private val listeners = ConcurrentHashMap<KClass<out PpssppMessage>,
//            MutableList<Continuation<PpssppMessage>>>()
//
//    private fun createSocket() {
//        launch(CommonPool) {
//            try {
//                socket = Socket(ip, port)
//                output = LittleEndianDataOutputStream(socket.getOutputStream())
//                input = LittleEndianDataInputStream(socket.getInputStream())
//                socketChannel.send(PpssppSocketReady())
//                while (true) {
//                    val packetId = input.readShort().toUnsignedInt()
//                    handlePacket(packetId)
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                socketChannel.send(PpssppSocketError(e))
//                socketChannel.close()
//            }
//        }
//    }
//
//    suspend fun connect() {
//        subscribeForThen(PpssppSocketReady::class) {
//            createSocket()
//        }
//    }
//
//    fun close() {
//        socket.close()
//    }
//
//    fun receiveMessages(context: CoroutineContext, processMessages: (PpssppMessage) -> Unit) {
//        launch(context) {
//            while (!socketChannel.isClosedForReceive) {
//                val msg = socketChannel.receive()
//                val list = listeners.remove(msg::class)
//                if (list != null) {
//                    for (cont in list) {
//                        cont.resume(msg)
//                    }
//                }
//                processMessages(msg)
//            }
//        }
//    }
//
//    private suspend fun handlePacket(packetId: Int) {
//        when (packetId) {
//            0x3330 -> {
//                val addr = input.readInt()
//                socketChannel.send(BreakpointPausedEvent(addr))
//            }
//            0x3331 -> {
//                val addr = input.readInt()
//                socketChannel.send(BreakpointLoggedEvent(addr))
//            }
//            0x3341 -> {
//                socketChannel.send(RunningSetResponse())
//            }
//            0x3343 -> {
//                val running = input.readBoolean()
//                socketChannel.send(RunningGetResponse(running))
//            }
//            0x3352 -> {
//                socketChannel.send(BreakpointAddResponse())
//            }
//            0x3354 -> {
//                socketChannel.send(BreakpointRemoveResponse())
//            }
//            0x3361 -> {
//                val regs = mutableListOf<Int>()
//                repeat(32) {
//                    regs.add(input.readInt())
//                }
//                val pc = input.readInt()
//                val hi = input.readInt()
//                val lo = input.readInt()
//                socketChannel.send(GprRegistersGetResponse(regs.toTypedArray(), pc, hi, lo))
//            }
//            0x3371 -> {
//                val regs = mutableListOf<Int>()
//                repeat(32) {
//                    regs.add(input.readInt())
//                }
//                socketChannel.send(FpuRegistersGetResponse(regs.toTypedArray()))
//            }
//            0x3381 -> {
//                val addr = input.readInt()
//                val size = input.readInt()
//                val mem = ByteArray(size)
//                input.readFully(mem)
//                socketChannel.send(MemGetResponse(addr, size, mem))
//            }
//            else -> {
//                error("Unsupported packet id ${packetId.toWHex()}")
//            }
//        }
//    }
//
//    suspend fun setRunning(running: Boolean) {
//        subscribeForThen(RunningSetResponse::class) {
//            output.writeShort(0x3340)
//            output.writeByte(running.toInt())
//            output.flush()
//        }
//    }
//
//    suspend fun isRunning(): Boolean {
//        return subscribeForThen(RunningGetResponse::class) {
//            output.writeShort(0x3342)
//            output.flush()
//        }.running
//    }
//
//    suspend fun addBreakpoint(addr: Int, temp: Boolean = false) {
//        subscribeForThen(BreakpointAddResponse::class) {
//            output.writeShort(0x3351)
//            output.writeInt(addr)
//            output.writeBoolean(temp)
//            output.flush()
//        }
//    }
//
//    suspend fun removeBreakpoint(addr: Int) {
//        subscribeForThen(BreakpointRemoveResponse::class) {
//            output.writeShort(0x3353)
//            output.writeInt(addr)
//            output.flush()
//        }
//    }
//
//    suspend fun getGprRegisters(): GprRegistersGetResponse {
//        return subscribeForThen(GprRegistersGetResponse::class) {
//            output.writeShort(0x3360)
//            output.flush()
//        }
//    }
//
//    suspend fun getFpuRegisters(): FpuRegistersGetResponse {
//        return subscribeForThen(FpuRegistersGetResponse::class) {
//            output.writeShort(0x3370)
//            output.flush()
//        }
//    }
//
//    suspend fun getMemory(addr: Int, size: Int): MemGetResponse {
//        return subscribeForThen(MemGetResponse::class) {
//            output.writeShort(0x3380)
//            output.writeInt(addr)
//            output.writeInt(size)
//            output.flush()
//        }
//    }
//
//    private suspend fun <T : PpssppMessage> subscribeFor(clazz: KClass<T>): T {
//        return subscribeForThen(clazz, {})
//    }
//
//    private suspend fun <T : PpssppMessage> subscribeForThen(clazz: KClass<T>, block: () -> Unit): T {
//        return suspendCoroutine { continuation ->
//            val listeners = listeners.getOrPut(clazz, { Collections.synchronizedList(mutableListOf()) })
//            listeners.add(continuation as Continuation<PpssppMessage>)
//            block()
//        }
//    }
//}

interface PpssppMessage
class PpssppSocketReady : PpssppMessage
class PpssppSocketError(e: Exception) : PpssppMessage
class BreakpointPausedEvent(val addr: Int) : PpssppMessage
class BreakpointLoggedEvent(val addr: Int) : PpssppMessage
class RunningSetResponse : PpssppMessage
class RunningGetResponse(val running: Boolean) : PpssppMessage
class BreakpointAddResponse : PpssppMessage
class BreakpointRemoveResponse : PpssppMessage
class GprRegistersGetResponse(val registers: Array<Int>, val pc: Int, val hi: Int, val lo: Int) : PpssppMessage
class FpuRegistersGetResponse(val registers: Array<Int>) : PpssppMessage
class MemGetResponse(val addr: Int, val size: Int, val mem: ByteArray) : PpssppMessage
