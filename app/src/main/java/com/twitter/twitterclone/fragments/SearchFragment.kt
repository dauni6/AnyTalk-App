package com.twitter.twitterclone.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import com.twitter.twitterclone.R
import com.twitter.twitterclone.adapters.TweetListAdapter
import com.twitter.twitterclone.listeners.TweetListener
import com.twitter.twitterclone.listeners.TwitterListenerImpl
import com.twitter.twitterclone.util.*
import kotlinx.android.synthetic.main.fragment_search.*

class SearchFragment : TwitterFragment() {

    private var currentHashtag = ""
    private var hashtagFollowed = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
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

        followHashtag.setOnClickListener {
            followHashtag.isClickable = false
            var followed = currentUser?.followHashtags
            if (hashtagFollowed) {
                followed?.remove(currentHashtag) //해쉬태그 팔로우
            } else {
                followed?.add(currentHashtag) //해쉬태그 언팔
            }
            firebaseDB.collection(DATA_USERS).document(userId).update(DATA_USER_HASHTAGS, followed)
                .addOnSuccessListener {
                    callback?.onUserUpdate()
                    followHashtag.isClickable = true
                }
                .addOnFailureListener {
                    it.printStackTrace()
                    followHashtag.isClickable = true
                }
        }

    }


    fun newHashtag(term: String) {
        currentHashtag = term
        followHashtag.visibility = View.VISIBLE
        updateList() //검색 내용 보여주기
    }

    override fun updateList() {
        tweetList?.visibility = View.GONE
        firebaseDB.collection(DATA_TWEETS).whereArrayContains(DATA_TWEET_HASHTAGS, currentHashtag).get()
            .addOnSuccessListener {
                tweetList?.visibility = View.VISIBLE
                val tweets = arrayListOf<Tweet>()
                for (document in it.documents) {
                    val tweet = document.toObject(Tweet::class.java)
                    tweet?.let {tweets.add(it) }
                }
                val sortedTweets = tweets.sortedWith(compareByDescending { it.timestamp }) //작성시간순으로 정렬
                tweetsAdapter?.updateTweets(sortedTweets)
            }
            .addOnFailureListener {
                it.printStackTrace()
            }
        updateFollowDrawable() //팔로우 클릭시 별 이미지 바꾸기
    }

    private fun updateFollowDrawable() {
        hashtagFollowed = currentUser?.followHashtags?.contains(currentHashtag) == true
        context?.let {
            if (hashtagFollowed) {
                followHashtag.setImageDrawable(ContextCompat.getDrawable(it, R.drawable.follow))
            } else {
                followHashtag.setImageDrawable(ContextCompat.getDrawable(it, R.drawable.follow_inactive))
            }
        }

    }

}
