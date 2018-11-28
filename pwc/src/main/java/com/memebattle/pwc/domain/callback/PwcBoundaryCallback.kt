/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.memebattle.pwc.domain.callback

import androidx.paging.PagedList
import androidx.annotation.MainThread
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executor
import com.memebattle.pagingwithrepository.domain.repository.network.createStatusLiveData
import com.memebattle.pwc.data.PwcApi
import com.memebattle.pwc.domain.helper.PagingRequestHelper
import com.memebattle.pwc.domain.model.PwcDbModel


/**
 * This boundary callback gets notified when user reaches to the edges of the list such that the
 * database cannot provide any more data.
 * <p>
 * The boundary callback might be called multiple times for the same direction so it does its own
 * rate limiting using the com.memebattle.pagingwithrepository.domain.util.PagingRequestHelper class.
 */
class PwcBoundaryCallback<ApiModel, Key>(
        private val subredditName: String,
        private val webservice: PwcApi<ApiModel, Key>,
        private val handleResponse: (String, ApiModel?) -> Unit,
        private val ioExecutor: Executor,
        private val networkPageSize: Int)
    : PagedList.BoundaryCallback<PwcDbModel<Key>>() {

    val helper = PagingRequestHelper(ioExecutor)
    val networkState = helper.createStatusLiveData()

    /**
     * Database returned 0 items. We should query the backend for more items.
     */
    @MainThread
    override fun onZeroItemsLoaded() {
        helper.runIfNotRunning(PagingRequestHelper.RequestType.INITIAL) {
            webservice.getTop(
                    path = subredditName,
                    limit = networkPageSize)
                    .enqueue(createWebserviceCallback(it))
        }
    }

    /**
     * User reached to the end of the list.
     */
    @MainThread
    override fun onItemAtEndLoaded(itemAtEnd: PwcDbModel<Key>) {
        helper.runIfNotRunning(PagingRequestHelper.RequestType.AFTER) {
            webservice.getTopAfter(
                    path = subredditName,
                    after = itemAtEnd.getKey(),
                    limit = networkPageSize)
                    .enqueue(createWebserviceCallback(it))
        }
    }

    /**
     * every time it gets new items, boundary callback simply inserts them into the database and
     * paging library takes care of refreshing the list if necessary.
     */
    private fun insertItemsIntoDb(
            response: Response<ApiModel>,
            it: PagingRequestHelper.Request.Callback) {
        ioExecutor.execute {
            handleResponse(subredditName, response.body())
            it.recordSuccess()
        }
    }

    override fun onItemAtFrontLoaded(itemAtFront: PwcDbModel<Key>) {
        // ignored, since we only ever append to what's in the DB
    }

    private fun createWebserviceCallback(it: PagingRequestHelper.Request.Callback)
            : Callback<ApiModel> {
        return object : Callback<ApiModel> {
            override fun onFailure(call: Call<ApiModel>, t: Throwable) {
                it.recordFailure(t)
            }

            override fun onResponse(
                    call: Call<ApiModel>,
                    response: Response<ApiModel>) {
                insertItemsIntoDb(response, it)
            }
        }
    }
}