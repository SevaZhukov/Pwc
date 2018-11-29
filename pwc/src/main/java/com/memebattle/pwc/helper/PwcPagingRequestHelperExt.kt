package com.memebattle.pagingwithrepository.domain.repository.network

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.memebattle.pwc.helper.PwcPagingRequestHelper
import com.memebattle.pwc.util.NetworkState

private fun getErrorMessage(report: PwcPagingRequestHelper.StatusReport): String {
    return PwcPagingRequestHelper.RequestType.values().mapNotNull {
        report.getErrorFor(it)?.message
    }.first()
}

fun PwcPagingRequestHelper.createStatusLiveData(): LiveData<NetworkState> {
    val liveData = MutableLiveData<NetworkState>()
    addListener { report ->
        when {
            report.hasRunning() -> liveData.postValue(NetworkState.LOADING)
            report.hasError() -> liveData.postValue(
                    NetworkState.error(getErrorMessage(report)))
            else -> liveData.postValue(NetworkState.LOADED)
        }
    }
    return liveData
}
