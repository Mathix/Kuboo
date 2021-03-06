package com.sethchhim.kuboo_remote.task

import androidx.lifecycle.MutableLiveData
import com.sethchhim.kuboo_remote.KubooRemote
import com.sethchhim.kuboo_remote.model.Login
import com.sethchhim.kuboo_remote.util.Settings.isDebugOkHttp
import org.apache.commons.io.input.BoundedInputStream
import timber.log.Timber
import java.net.MalformedURLException
import java.net.SocketTimeoutException

class Task_RemoteItemCount(kubooRemote: KubooRemote, login: Login, stringUrl: String) {

    private val okHttpHelper = kubooRemote.okHttpHelper
    private val parseService = okHttpHelper.parseService

    internal val liveData = MutableLiveData<String>()

    init {
        kubooRemote.networkIO.execute {
            try {
                val call = okHttpHelper.getCall(login, stringUrl, javaClass.simpleName)
                val response = call.execute()
                val inputStream = response.body()?.byteStream()
                if (response.isSuccessful && inputStream != null) {
                    val boundInputStream = BoundedInputStream(inputStream, 4 * 1024)
                    val inputAsString = boundInputStream.bufferedReader().use { it.readText() }
                    val result = parseService.parseItemCount(inputAsString)
                    kubooRemote.mainThread.execute { liveData.value = result }
                    boundInputStream.close()
                    inputStream.close()
                } else {
                    kubooRemote.mainThread.execute { liveData.value = null }
                }
                response.close()
            } catch (e: SocketTimeoutException) {
                if (isDebugOkHttp) Timber.w("Connection timed out! $stringUrl")
                kubooRemote.mainThread.execute { liveData.value = null }
            } catch (e: MalformedURLException) {
                if (isDebugOkHttp) Timber.e("URL is bad! $stringUrl")
                kubooRemote.mainThread.execute { liveData.value = null }
            } catch (e: Exception) {
                if (isDebugOkHttp) Timber.e("Something went wrong! $stringUrl")
                kubooRemote.mainThread.execute { liveData.value = null }
            }
        }
    }

}