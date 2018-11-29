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

package com.memebattle.pagingwithcahing.domain

import androidx.paging.PagedList
import androidx.annotation.MainThread
import com.memebattle.pagingwithcahing.data.api.RedditApi
import com.memebattle.pagingwithcahing.domain.model.api.ListingResponse
import com.memebattle.pagingwithcahing.domain.model.db.RedditPost
import com.memebattle.pagingwithrepository.domain.repository.network.createStatusLiveData
import com.memebattle.pwc.helper.PwcPagingRequestHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executor

class SubredditBoundaryCallback(
        private val subredditName: String,
        private val webservice: RedditApi,
        private val handleResponse: (String, ListingResponse?) -> Unit,
        private val ioExecutor: Executor,
        private val networkPageSize: Int)
    : PagedList.BoundaryCallback<RedditPost>() {

    val helper = PwcPagingRequestHelper(ioExecutor)
    val networkState = helper.createStatusLiveData()

    /**
     * Database returned 0 items. We should query the backend for more items.
     */
    @MainThread
    override fun onZeroItemsLoaded() {
        helper.runIfNotRunning(PwcPagingRequestHelper.RequestType.INITIAL) {
            webservice.getTop(
                    subreddit = subredditName,
                    limit = networkPageSize)
                    .enqueue(createWebserviceCallback(it))
        }
    }

    /**
     * User reached to the end of the list.
     */
    @MainThread
    override fun onItemAtEndLoaded(itemAtEnd: RedditPost) {
        helper.runIfNotRunning(PwcPagingRequestHelper.RequestType.AFTER) {
            webservice.getTopAfter(
                    subreddit = subredditName,
                    after = itemAtEnd.name,
                    limit = networkPageSize)
                    .enqueue(createWebserviceCallback(it))
        }
    }

    /**
     * every time it gets new items, boundary callback simply inserts them into the database and
     * paging library takes care of refreshing the list if necessary.
     */
    private fun insertItemsIntoDb(
            response: Response<ListingResponse>,
            it: PwcPagingRequestHelper.Request.Callback) {
        ioExecutor.execute {
            handleResponse(subredditName, response.body())
            it.recordSuccess()
        }
    }

    override fun onItemAtFrontLoaded(itemAtFront: RedditPost) {
        // ignored, since we only ever append to what's in the DB
    }

    private fun createWebserviceCallback(it: PwcPagingRequestHelper.Request.Callback)
            : Callback<ListingResponse> {
        return object : Callback<ListingResponse> {
            override fun onFailure(call: Call<ListingResponse>, t: Throwable) {
                it.recordFailure(t)
            }

            override fun onResponse(
                    call: Call<ListingResponse>,
                    response: Response<ListingResponse>) {
                insertItemsIntoDb(response, it)
            }
        }
    }
}