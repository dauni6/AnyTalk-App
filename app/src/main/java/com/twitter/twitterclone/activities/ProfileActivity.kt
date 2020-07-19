package com.twitter.twitterclone.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.twitter.twitterclone.R
import com.twitter.twitterclone.util.*
import kotlinx.android.synthetic.main.activity_profile.*

class ProfileActivity : AppCompatActivity() {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseDB = FirebaseFirestore.getInstance()
    private val firebaseStorage = FirebaseStorage.getInstance().reference
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private var imageUrl: String? = null
    private var userName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        if (userId == null) {
            finish()
        }

        if (intent.hasExtra(PARAM_USER_NAME)) {
            userName = intent.getStringExtra(PARAM_USER_NAME)
        } else {
            Toast.makeText(this, "회원정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }

        setTextChangeListener(usernameET, usernameTIL)
        profileProgressLayout.setOnTouchListener { v, event ->  true }

        photoIV.setOnClickListener { //사진 업로드
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_CODE_PHOTO)
        }

        populateInfo()
    }



    fun setTextChangeListener(et: TextInputEditText, til: TextInputLayout) {
        et.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                til.isErrorEnabled = false
            }
        })
    }

    fun populateInfo() { //정보 불러오기
        profileProgressLayout.visibility = View.VISIBLE
        firebaseDB.collection(DATA_USERS).document(userId!!).get() //해당 userId에 맞는 데이터객체 가져오기
            .addOnSuccessListener {
                val user = it.toObject(User::class.java)
                emailET.setText(user?.email)
                usernameET.setText(user?.username, TextView.BufferType.EDITABLE)
                user?.imageUrl?.let {
                    photoIV.loadUrl(user.imageUrl, R.drawable.default_user) //디폴트 이미지도 설정하기
                }
                profileProgressLayout.visibility = View.GONE
            }
            .addOnFailureListener {
                it.printStackTrace()
                finish()
            }
    }

    fun onApply(v: View) { //회원정보 업데이트
        profileProgressLayout.visibility = View.VISIBLE
        var proceed  = true
        if(usernameET.text.isNullOrEmpty()) {
            profileProgressLayout.visibility = View.GONE
            usernameTIL.error = "닉네임을 입력해 주세요"
            usernameTIL.isErrorEnabled = true
            proceed = false
        }
        if (proceed) {
            val username = usernameET.text.toString()
            val map = HashMap<String, Any>()
            map[DATA_USER_USERNAME] = username
            firebaseDB.collection(DATA_USERS).document(userId!!).update(map) //닉네임변경
                .addOnSuccessListener {
                    Toast.makeText(this, "회원정보를 변경했습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    it.printStackTrace()
                    Toast.makeText(this, "변경실패. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                    profileProgressLayout.visibility = View.GONE
                }
            if (!passwordET.text.isNullOrEmpty()) { //비밀번호변경
                val password = passwordET.text.toString()
                firebaseAuth?.currentUser?.updatePassword(password)!!.addOnCompleteListener {
                        if (it.isSuccessful){
                            Toast.makeText(this, "비밀번호를 변경했습니다.", Toast.LENGTH_SHORT).show()
                        }
                }
                    ?.addOnFailureListener {
                        it.printStackTrace()
                        Toast.makeText(this, "변경실패. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                        profileProgressLayout.visibility = View.GONE
                    }
            }
            // 모든 글들에 프로필사진 및 닉네임 변경하기
            firebaseDB.collection(DATA_TWEETS).whereEqualTo(DATA_USER_USERNAME, userName).get()
                .addOnSuccessListener {
                    for (document in it.documents) {
                        val tweet = document.toObject(Tweet::class.java)
                        tweet?.let {
                            if (!this.imageUrl.isNullOrEmpty()){
                                firebaseDB.collection(DATA_TWEETS).document(tweet.tweetId!!).update(DATA_USER_SELFIE_URL, this.imageUrl) //새로운 이미지로 셀피사진 변경
                            }
                            firebaseDB.collection(DATA_TWEETS).document(tweet.tweetId!!).update(DATA_USER_USERNAME, username) //새로운 닉네임으로 변경
                        }
                    }
                }
                .addOnFailureListener {
                    it.printStackTrace()
                    profileProgressLayout.visibility = View.GONE
                }
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PHOTO) {
            storeImage(data?.data)
        }
    }

    fun storeImage(imageUrl: Uri?) {
        imageUrl?.let {
            Toast.makeText(this, "업로드중...", Toast.LENGTH_SHORT).show()
            profileProgressLayout.visibility = View.VISIBLE
            val filepath = firebaseStorage.child(DATA_IMAGES).child(userId!!)
            filepath.putFile(imageUrl) //업로드
                .addOnSuccessListener { 
                    filepath.downloadUrl //바로 다운로드
                        .addOnSuccessListener { 
                            val url = it.toString()
                            firebaseDB.collection(DATA_USERS).document(userId!!).update(DATA_USER_IMAGE_URL, url) //회원정보에 이미지 url 업데이트
                                .addOnSuccessListener {
                                    this.imageUrl = url
                                    photoIV.loadUrl(this.imageUrl, R.drawable.default_user)
                                }
                            profileProgressLayout.visibility = View.GONE
                        }
                        .addOnFailureListener {
                            it.printStackTrace()
                            Toast.makeText(this, "이미지 업로드 실패. 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
                            profileProgressLayout.visibility = View.GONE
                        }
                }
                .addOnFailureListener{
                    it.printStackTrace()
                    Toast.makeText(this, "이미지 업로드 실패. 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
                    profileProgressLayout.visibility = View.GONE
                }
        }
    }

    fun onLogout(v: View) {
        firebaseAuth.signOut()
        finish() // HomeActivity로 돌아가면 onResume에서 로그인상태를 다시 검사. 없으면 LoginAcitivity로 감

    }


    companion object {
        val PARAM_USER_NAME = "username"

        fun newIntent(context: Context, userName: String?): Intent {
          val intent = Intent(context, ProfileActivity::class.java)
            intent.putExtra(PARAM_USER_NAME, userName)
            return intent
        }

    }
}
