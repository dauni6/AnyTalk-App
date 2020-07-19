package com.twitter.twitterclone.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager

import com.twitter.twitterclone.R
import com.twitter.twitterclone.adapters.TweetListAdapter
import com.twitter.twitterclone.listeners.TwitterListenerImpl
import com.twitter.twitterclone.util.DATA_TWEETS
import com.twitter.twitterclone.util.DATA_TWEET_USER_IDS
import com.twitter.twitterclone.util.DATA_USER_USERNAME
import com.twitter.twitterclone.util.Tweet
import kotlinx.android.synthetic.main.fragment_my_activity.*

class MyActivityFragment : TwitterFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_activity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listener = TwitterListenerImpl(tweetList, currentUser, callback)

        tweetsAdapter = TweetListAdapter(userId!!, arrayListOf())
        tweetsAdapter?.setListener(listener)
        tweetList?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tweetsAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        swipeRefresh.setOnRefreshListener {
            swipeRefresh.isRefreshing = false
            updateList()
        }
    }

    override fun updateList() { //내가 작성한 글만 보기
        tweetList?.visibility = View.GONE
        val tweets = arrayListOf<Tweet>()

        firebaseDB.collection(DATA_TWEETS).whereEqualTo(DATA_USER_USERNAME, currentUser!!.username).get()
            .addOnSuccessListener {
                for (document in it.documents) {
                    val tweet = document.toObject(Tweet::class.java)
                    tweet?.let { tweets.add(tweet) }
                }
                val sortedList = tweets.sortedWith(compareByDescending { it.timestamp })
                tweetsAdapter?.updateTweets(sortedList)
                tweetList?.visibility = View.VISIBLE
            }
            .addOnFailureListener {
                it.printStackTrace()
                tweetList?.visibility = View.VISIBLE
            }
    }

}
