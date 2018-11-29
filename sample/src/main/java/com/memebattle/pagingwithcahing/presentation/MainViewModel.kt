package com.memebattle.pagingwithcahing.presentation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.memebattle.pagingwithcahing.domain.MainRepository

class MainViewModel(private val repository: MainRepository) : ViewModel() {

    private val subredditName = MutableLiveData<String>()
    private val repoResult = Transformations.map(subredditName) {
        repository.postsOfSubreddit(it, 10)
    }
    val posts = Transformations.switchMap(repoResult) { it.pagedList }!!
    val networkState = Transformations.switchMap(repoResult) { it.networkState }!!
    val refreshState = Transformations.switchMap(repoResult) { it.refreshState }!!

    fun refresh() {
        repoResult.value?.refresh?.invoke()
    }

    fun showSubReddit(subreddit: String): Boolean {
        if (subredditName.value == subreddit) {
            return false
        }
        subredditName.value = subreddit
        return true
    }

    fun retry() {
        val listing = repoResult?.value
        listing?.retry?.invoke()
    }

    fun currentSubreddit(): String? = subredditName.value
}