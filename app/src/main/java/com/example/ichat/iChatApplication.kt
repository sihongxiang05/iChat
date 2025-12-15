package com.example.ichat

import android.app.Application
import android.os.Build
import android.util.Log
import com.example.ichat.mars.MarsRuntime
import com.tencent.mars.app.AppLogic
import com.tencent.mars.stn.StnLogic
import java.io.ByteArrayOutputStream

class iChatApplication : Application() {
    companion object {}

    override fun onCreate() {
        super.onCreate()
        initMars()
    }

    private fun initMars() {
        AppLogic.setCallBack(object : AppLogic.ICallBack {
            override fun getAppFilePath(): String {
                return try { filesDir.absolutePath } catch (e: Exception) { "" }
            }
            override fun getAccountInfo(): AppLogic.AccountInfo {
                return AppLogic.AccountInfo(0, "guest")
            }
            override fun getClientVersion(): Int { return 1 }
            override fun getDeviceType(): AppLogic.DeviceInfo {
                return AppLogic.DeviceInfo(Build.MODEL, "Android")
            }
        })

        StnLogic.setCallBack(object : StnLogic.ICallBack {
            override fun makesureAuthed(host: String): Boolean {
                Log.d("Mars", "makesureAuthed host=$host")
                return true
            }
            override fun onNewDns(host: String): Array<String>? { return null }
            override fun onPush(cmdid: Int, taskid: Int, data: ByteArray) {
                Log.i("Mars", "Push cmdid=$cmdid taskid=$taskid data=${String(data)}")
            }
            override fun req2Buf(
                taskID: Int,
                userContext: Any?,
                reqBuffer: ByteArrayOutputStream,
                errCode: IntArray,
                channelSelect: Int,
                host: String
            ): Boolean {
                return try {
                    val bytes = (userContext as? ByteArray) ?: ByteArray(0)
                    reqBuffer.write(bytes)
                    Log.i("Mars", "req2Buf taskID=$taskID host=$host ch=$channelSelect len=${bytes.size}")
                    errCode[0] = StnLogic.ectOK
                    true
                } catch (e: Exception) {
                    errCode[0] = StnLogic.ectLocal
                    false
                }
            }
            override fun buf2Resp(
                taskID: Int,
                userContext: Any?,
                respBuffer: ByteArray,
                errCode: IntArray,
                channelSelect: Int
            ): Int {
                try {
                    val text = try { String(respBuffer) } catch (_: Exception) { "<binary ${respBuffer.size}B>" }
                    Log.i("Mars", "buf2Resp taskID=$taskID len=${respBuffer.size} body=$text")
                } catch (_: Exception) {}
                return StnLogic.RESP_FAIL_HANDLE_NORMAL
            }
            override fun onTaskEnd(
                taskID: Int,
                userContext: Any?,
                errType: Int,
                errCode: Int,
                profile: StnLogic.CgiProfile
            ): Int {
                Log.d("Mars", "onTaskEnd taskID=$taskID errType=$errType errCode=$errCode")
                return StnLogic.TASK_END_SUCCESS
            }
            override fun trafficData(send: Int, recv: Int) {
                Log.d("Mars", "trafficData send=$send recv=$recv")
            }
            override fun reportConnectInfo(status: Int, longlinkstatus: Int) {
                Log.d("Mars", "connect status=$status long=$longlinkstatus")
            }
            override fun getLongLinkIdentifyCheckBuffer(
                identifyReqBuf: ByteArrayOutputStream,
                hashCodeBuffer: ByteArrayOutputStream,
                reqRespCmdID: IntArray
            ): Int { return StnLogic.ECHECK_NEVER }
            override fun onLongLinkIdentifyResp(buffer: ByteArray, hashCodeBuffer: ByteArray): Boolean { return false }
            override fun requestDoSync() {}
            override fun requestNetCheckShortLinkHosts(): Array<String>? { return null }
            override fun isLogoned(): Boolean { return true }
            override fun reportTaskProfile(taskString: String) {}
        })

        if (!MarsRuntime.tryLoadAndInit(applicationContext)) {
            Log.w("Mars", "Native libraries unavailable, skip STN setup")
            return
        }

        val longLinkHost = "mars.server.com"
        val longLinkPorts = intArrayOf(8081)
        val shortLinkPort = 8080
        StnLogic.setLonglinkSvrAddr(longLinkHost, longLinkPorts, "10.0.2.2")
        StnLogic.setShortlinkSvrAddr(shortLinkPort, "10.0.2.2")
        StnLogic.setDebugIP("mars.server.com", "10.0.2.2")
        StnLogic.makesureLongLinkConnected()
    }
}
