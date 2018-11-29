package com.memebattle.pagingwithcahing.domain

import android.content.Context
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import androidx.room.Room
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executors
import androidx.paging.LivePagedListBuilder
import com.memebattle.pagingwithcahing.data.api.RedditApi
import com.memebattle.pagingwithcahing.data.db.RedditDb
import com.memebattle.pagingwithcahing.data.db.RedditPostDao
import com.memebattle.pagingwithcahing.domain.model.api.ListingResponse
import com.memebattle.pagingwithcahing.domain.model.db.RedditPost
import com.memebattle.pwc.domain.util.NetworkState
import com.memebattle.pwc.domain.util.PwcListing

class MainRepository(context: Context) {

    private var retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("https://www.reddit.com/") //Базовая часть адреса
            .addConverterFactory(GsonConverterFactory.create()) //Конвертер, необходимый для преобразования JSON'а в объекты
            .build()

    var db = Room.databaseBuilder(context,
            RedditDb::class.java, "database").build()

    private var redditApi: RedditApi
    private var dao: RedditPostDao

    val ioExecutor = Executors.newSingleThreadExecutor()

    init {
        redditApi = retrofit.create(RedditApi::class.java) //Создаем объект, при помощи которого будем выполнять запросы
        dao = db.posts()
    }

    private fun insertResultIntoDb(subredditName: String, body: ListingResponse?) {
        body!!.data.children.let { posts ->
            db.runInTransaction {
                val start = db.posts().getNextIndexInSubreddit(subredditName)
                val items = posts.mapIndexed { index, child ->
                    child.data.indexInResponse = start + index
                    child.data
                }
                db.posts().insert(items)
            }
        }
    }

    @MainThread
    private fun refresh(subredditName: String): LiveData<NetworkState> {
        val networkState = MutableLiveData<NetworkState>()
        networkState.value = NetworkState.LOADING
        redditApi.getTop(subredditName, 10).enqueue(
                object : Callback<ListingResponse> {
                    override fun onFailure(call: Call<ListingResponse>, t: Throwable) {
                        // retrofit calls this on main thread so safe to call set value
                        networkState.value = NetworkState.error(t.message)
                    }

                    override fun onResponse(call: Call<ListingResponse>, response: Response<ListingResponse>) {
                        ioExecutor.execute {
                            db.runInTransaction {
                                db.posts().deleteBySubreddit(subredditName)
                                insertResultIntoDb(subredditName, response.body())
                            }
                            // since we are in bg thread now, post the result.
                            networkState.postValue(NetworkState.LOADED)
                        }
                    }
                }
        )
        return networkState
    }

    fun postsOfSubreddit(subReddit: String, pageSize: Int): PwcListing<RedditPost> {

        val boundaryCallback = SubredditBoundaryCallback(
                webservice = redditApi,
                subredditName = subReddit,
                handleResponse = this::insertResultIntoDb,
                ioExecutor = ioExecutor,
                networkPageSize = pageSize)

        val refreshTrigger = MutableLiveData<Unit>()
        val refreshState = Transformations.switchMap(refreshTrigger) {
            refresh(subReddit)
        }

        val livePagedList = LivePagedListBuilder(db.posts().postsBySubreddit(subReddit), pageSize)
                .setBoundaryCallback(boundaryCallback)
                .build()

        return PwcListing(
                pagedList = livePagedList,
                networkState = boundaryCallback.networkState,
                retry = {
                    boundaryCallback.helper.retryAllFailed()
                },
                refresh = {
                    refreshTrigger.value = null
                },
                refreshState = refreshState
        )
    }
}