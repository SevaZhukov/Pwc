package com.memebattle.pwc.data

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PwcApi<T, Key> {

    @GET("{path}")
        fun getTop(
            @Path("path") path: String,
            @Query("limit") limit: Int): Call<T>

    // for after/before param, either get from RedditDataResponse.after/before,
    // or pass RedditNewsDataResponse.name (though this is technically incorrect)
    @GET("{path}")
    fun getTopAfter(
            @Path("path") path: String,
            @Query("after") after: Key,
            @Query("limit") limit: Int): Call<T>

    @GET("{path}")
    fun getTopBefore(
            @Path("path") path: String,
            @Query("before") before: Key,
            @Query("limit") limit: Int): Call<T>
}