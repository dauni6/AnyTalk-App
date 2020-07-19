package com.twitter.twitterclone.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.twitter.twitterclone.R
import com.twitter.twitterclone.fragments.HomeFragment
import com.twitter.twitterclone.fragments.MyActivityFragment
import com.twitter.twitterclone.fragments.SearchFragment
import com.twitter.twitterclone.fragments.TwitterFragment
import com.twitter.twitterclone.listeners.HomeCallback
import com.twitter.twitterclone.util.DATA_USERS
import com.twitter.twitterclone.util.User
import com.twitter.twitterclone.util.loadUrl
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity(), HomeCallback {

    private lateinit var sectionPagerAdapter: SectionPagerAdapter
    private val homeFragment = HomeFragment()
    private val searchFragment = SearchFragment()
    private val myActivityFragment = MyActivityFragment()
    private var currentFragment: TwitterFragment = homeFragment

    private val firebaseDB = FirebaseFirestore.getInstance()
    private var userId = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var user: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        sectionPagerAdapter = SectionPagerAdapter(supportFragmentManager)
        container.adapter = sectionPagerAdapter
        container.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabs))
        tabs.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(container))
        tabs.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener{
            override fun onTabReselected(p0: TabLayout.Tab?) {

            }
            override fun onTabUnselected(p0: TabLayout.Tab?) {

            }
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when(tab?.position) {
                    0 -> {
                        titleBar.visibility = View.VISIBLE
                        titleBar.text = "뉴스피드"
                        searchBar.visibility = View.GONE
                        currentFragment = homeFragment
                    }
                    1 -> {
                        titleBar.visibility = View.GONE
                        searchBar.visibility = View.VISIBLE
                        currentFragment = searchFragment
                    }
                    2 -> {
                        titleBar.visibility = View.VISIBLE
                        titleBar.text = "나의 글"
                        searchBar.visibility = View.GONE
                        currentFragment = myActivityFragment
                    }
                }
            }
        })

        logo.setOnClickListener {  view ->
            startActivity(ProfileActivity.newIntent(this, user?.username))
        }

        fab.setOnClickListener {
            startActivity(TweetActivity.newIntent(this, userId, user?.username, user?.imageUrl))
        }

        homeProgressLayout.setOnTouchListener{ v , event -> true}

        searchET.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchFragment.newHashtag(v?.text.toString())
            }
            true
        }
    }

    inner class SectionPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return when(position) {
                0 -> homeFragment
                1 -> searchFragment
                else -> myActivityFragment
            }
        }

        override fun getCount() = 3
    }


    fun populate() { //회원사진 가져오기
        homeProgressLayout.visibility = View.VISIBLE
        firebaseDB.collection(DATA_USERS).document(userId!!).get()
            .addOnSuccessListener {
                homeProgressLayout.visibility = View.GONE
                user = it.toObject(User::class.java)!!
                user.imageUrl?.let {
                    logo.loadUrl(it, R.drawable.default_user)
                }
                updateFagmentUser()
            }
            .addOnFailureListener {
                it.printStackTrace()
                finish()
            }
    }

    fun updateFagmentUser() {
        homeFragment.setUser(user)
        searchFragment.setUser(user)
        myActivityFragment.setUser(user)
        currentFragment.updateList()
    }

    override fun onUserUpdate() {
        populate()
    }

    override fun onRefresh() {
        currentFragment.updateList()
    }

    override fun onResume() {
        super.onResume()
        userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) { //로그인 되어 있지 않다면 다시 로그인화면으로 감
            startActivity(LoginActivity.newIntent(this))
            finish()
        } else {
            populate()
        }
    }


    companion object {
        fun newIntent(context: Context) = Intent(context, HomeActivity::class.java)
    }
}
