package com.twitter.twitterclone.util

data class User (
    val username:String? = "",
    val email: String? = "",
    val imageUrl: String? = "",
    val followHashtags: ArrayList<String>? = arrayListOf(),
    val followUsers: ArrayList<String>? = arrayListOf()
)

data class Tweet(
    val tweetId: String? ="",
    val userIds: ArrayList<String>? = arrayListOf(),
    val username: String? = "",
    val text: String? = "",
    val selfieUrl: String? = "",
    val imageUrl: String? = "",
    val timestamp : Long? = 0,
    val hashTags: ArrayList<String>? = arrayListOf(),
    val likes: ArrayList<String>? = arrayListOf()
)