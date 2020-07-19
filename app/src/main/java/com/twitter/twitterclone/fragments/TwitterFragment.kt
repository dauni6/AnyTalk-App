package com.twitter.twitterclone.fragments

import android.content.Context
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.twitter.twitterclone.adapters.TweetListAdapter
import com.twitter.twitterclone.listeners.HomeCallback
import com.twitter.twitterclone.listeners.TweetListener
import com.twitter.twitterclone.listeners.TwitterListenerImpl
import com.twitter.twitterclone.util.User
import java.lang.RuntimeException

abstract class TwitterFragment : Fragment() {
    protected var tweetsAdapter: TweetListAdapter? = null
    protected var currentUser: User? = null
    protected val firebaseDB = FirebaseFirestore.getInstance()
    protected val userId = FirebaseAuth.getInstance().currentUser?.uid
    protected var listener: TwitterListenerImpl? = null
    protected var callback: HomeCallback? = null

    override fun onAttach(context: Context) { //액티비티에 프래그먼트가 붙여질 때, 가장 먼저 실행 됨
        super.onAttach(context)
        if (context is HomeCallback) {
            callback = context
        } else {
            throw RuntimeException("$context must implement HomeCallback")
        }
    }

    fun setUser(user: User?) {
        this.currentUser = user
        listener?.user = user
    }

    abstract fun updateList()

    override fun onResume() {
        //TwitterFragment를 상속받는 Home,Search,MyActivity Fragment들은 실행 시
        //부모프래그먼트인 TwitterFragment의 onResume의 updateList()가 먼저 실행되고 override 한 자신들의 updateList()가 실행된다.
        //이렇게 함으로써 변화가 생기면 모든 자식프래그먼트들이 업데이트가 되는 것 이다. *상속관계
        super.onResume()
        updateList()
    }

}