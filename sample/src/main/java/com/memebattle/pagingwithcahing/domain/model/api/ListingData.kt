package com.memebattle.pagingwithcahing.domain.model.api

class ListingData(
        val children: List<RedditChildrenResponse>,
        val after: String?,
        val before: String?
)