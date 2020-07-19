package com.twitter.twitterclone.listeners

import android.app.AlertDialog
import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.twitter.twitterclone.util.*

class TwitterListenerImpl(val tweetList: RecyclerView, var user: User?, val callback: HomeCallback?): TweetListener {

    private val firebaseDB = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onLayoutClick(tweet: Tweet?) {
        tweet?.let {
            val owner = tweet.userIds?.get(0) //첫번째는 글쓴이가 항상 들어가 있음
            if (owner != userId) { //내가 쓴 글이 아니라면
                if (user?.followUsers?.contains(owner) == true) {
                    AlertDialog.Builder(tweetList.context)
                        .setTitle(" ${tweet.username}님을 언팔로우할까요?")
                        .setPositiveButton("네") { dialog, which ->
                            tweetList.isClickable = false
                            var followedUsers = user?.followUsers
                            if (followedUsers == null) {
                                followedUsers = arrayListOf()
                            }
                            followedUsers?.remove(owner)
                            firebaseDB.collection(DATA_USERS).document(userId!!).update(DATA_USER_FOLLOW, followedUsers)
                                .addOnSuccessListener {
                                    tweetList.isClickable = true
                                    callback?.onUserUpdate()
                                }
                                .addOnFailureListener {
                                    it.printStackTrace()
                                    tweetList.isClickable = true
                                }
                        }
                        .setNegativeButton("취소") {dialog, which ->  }
                        .show()
                } else {
                    AlertDialog.Builder(tweetList.context)
                        .setTitle(" ${tweet.username}님을 팔로우할까요?")
                        .setPositiveButton("네") { dialog, which ->
                            tweetList.isClickable = false
                            var followedUsers = user?.followUsers
                            if (followedUsers == null) {
                                followedUsers = arrayListOf()
                            }
                            owner?.let {
                            followedUsers?.add(owner)
                            firebaseDB.collection(DATA_USERS).document(userId!!).update(DATA_USER_FOLLOW, followedUsers)
                                .addOnSuccessListener {
                                    tweetList.isClickable = true
                                    callback?.onUserUpdate()
                                }
                                .addOnFailureListener {
                                    it.printStackTrace()
                                    tweetList.isClickable = true
                                }
                            }
                        }
                        .setNegativeButton("취소") {dialog, which ->  }
                        .show()
                }
            }
        }
    }

    override fun onLike(tweet: Tweet?) {
        tweet?.let {
            tweetList.isClickable = false
            val likes = tweet.likes
            if (tweet.likes?.contains(userId) == true) { //이미 좋아요 눌렸다면
                likes?.remove(userId)
            } else {
                likes?.add(userId!!)
            }
            firebaseDB.collection(DATA_TWEETS).document(tweet.tweetId!!).update(DATA_TWEETS_LIKES, likes)
                .addOnSuccessListener {
                    tweetList.isClickable = true
                    callback?.onRefresh()
                }
                .addOnFailureListener {
                    it.printStackTrace()
                    tweetList.isClickable = true
                }
        }
    }

    override fun onRetweet(tweet: Tweet?) {
        tweet?.let {
            tweetList.isClickable = false
            val retweets = tweet.userIds
            if (retweets?.contains(userId) == true) { //이미 리트윗 했다면
                retweets?.remove(userId)
            } else {
                retweets?.add(userId!!)
            }
                firebaseDB.collection(DATA_TWEETS).document(tweet.tweetId!!).update(DATA_TWEET_USER_IDS, retweets)
                    .addOnSuccessListener {
                        tweetList.isClickable = true
                        callback?.onRefresh()
                    }
                    .addOnFailureListener {
                        tweetList.isClickable = true
                    }
        }
    }

}