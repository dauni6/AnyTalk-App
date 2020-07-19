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
import com.twitter.twitterclone.util.DATA_TWEET_HASHTAGS
import com.twitter.twitterclone.util.DATA_TWEET_USER_IDS
import com.twitter.twitterclone.util.Tweet
import kotlinx.android.synthetic.main.fragment_home.*

class HomeFragment : TwitterFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
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

    override fun updateList() {
        tweetList?.visibility = View.GONE
        currentUser?.let {
            val tweets = arrayListOf<Tweet>()
            for (hashtag in it.followHashtags!!) {
                firebaseDB.collection(DATA_TWEETS).whereArrayContains(DATA_TWEET_HASHTAGS, hashtag).get()
                    .addOnSuccessListener {
                        for (document in it.documents) {
                            val tweet = document.toObject(Tweet::class.java)
                            tweet?.let { tweets.add(tweet) }
                        }
                        updateAdapter(tweets)
                        tweetList?.visibility = View.VISIBLE
                    }
                    .addOnFailureListener {
                        it.printStackTrace()
                        tweetList?.visibility = View.VISIBLE
                    }
            }
            for (followedUser in it.followUsers!!) {
                firebaseDB.collection(DATA_TWEETS).whereArrayContains(DATA_TWEET_USER_IDS, followedUser).get()
                    .addOnSuccessListener {
                        for (document in it.documents) {
                            val tweet = document.toObject(Tweet::class.java)
                            tweet?.let { tweets.add(it) }
                        }
                        updateAdapter(tweets)
                        tweetList?.visibility = View.VISIBLE
                    }
                    .addOnFailureListener {
                        it.printStackTrace()
                        tweetList?.visibility = View.VISIBLE
                    }
            }
        }
    }

    private fun updateAdapter(tweets: List<Tweet>) {
        val sortedTweets = tweets.sortedWith(compareByDescending { it.timestamp })
        tweetsAdapter?.updateTweets(removeDuplicates(sortedTweets))
    }

    private fun removeDuplicates(originalList: List<Tweet>) = originalList.distinctBy { it.tweetId }


}
