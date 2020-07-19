package com.twitter.twitterclone.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.twitter.twitterclone.R
import com.twitter.twitterclone.util.*
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_tweet.*

class TweetActivity : AppCompatActivity() {

    private val firebaseDB = FirebaseFirestore.getInstance()
    private val firebaseStorage = FirebaseStorage.getInstance().reference
    private var imageUrl: String? = null
    private var selfieUrl: String? = null
    private var userId: String? = null
    private var userName: String? = null
    private var tweetFromDB: DocumentReference = firebaseDB.collection(DATA_TWEETS).document()  //document 안에 아무것도 넣지 않으면 새로운 고유값이 id로 만들어짐

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tweet)

        if (intent.hasExtra(PARAM_USER_ID) && intent.hasExtra(PARAM_USER_NAME)) {
            userId = intent.getStringExtra(PARAM_USER_ID)
            userName = intent.getStringExtra(PARAM_USER_NAME)
            selfieUrl = intent.getStringExtra(PARAM_USER_SELFIE_URL)
        } else {
            Toast.makeText(this, "톡 작성을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }

        tweetProgressLayout.setOnTouchListener{ v , event -> true}
    }

    fun onAddImage(v: View) {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE_PHOTO)
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
            tweetProgressLayout.visibility = View.VISIBLE
            val filepath = firebaseStorage.child(DATA_IMAGES).child(tweetFromDB!!.id)
            filepath.putFile(imageUrl) //업로드
                .addOnSuccessListener {
                    filepath.downloadUrl //바로 다운로드
                        .addOnSuccessListener {
                            this.imageUrl = it.toString()
                            tweetImage.loadUrl(this.imageUrl, R.drawable.default_user)
                            tweetProgressLayout.visibility = View.GONE
                        }
                        .addOnFailureListener {
                            it.printStackTrace()
                            Toast.makeText(this, "이미지 업로드 실패. 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
                            tweetProgressLayout.visibility = View.GONE
                        }
                }
                .addOnFailureListener{
                    it.printStackTrace()
                    Toast.makeText(this, "이미지 업로드 실패. 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
                    tweetProgressLayout.visibility = View.GONE
                }
        }
    }

    fun onPostTweet(v: View) { //글 업로드하기
        tweetProgressLayout.visibility = View.VISIBLE
        var proceed = true
        if (imageUrl.isNullOrEmpty()) {
            tweetProgressLayout.visibility = View.GONE
            Toast.makeText(this, "업로드 하기 위해서 사진은 필수 입니다.", Toast.LENGTH_SHORT).show()
            proceed = false
        }
        if (proceed) {
            val text = tweetText.text.toString()
            val hashtags = getHashtags(text)
            val tweet = Tweet(tweetFromDB!!.id, arrayListOf(userId!!), userName, text, selfieUrl ,imageUrl, System.currentTimeMillis(), hashtags, arrayListOf())
            tweetFromDB!!.set(tweet)
                .addOnCompleteListener { finish() }
                .addOnFailureListener {
                    it.printStackTrace()
                    tweetProgressLayout.visibility = View.GONE
                    Toast.makeText(this, "작성 실패", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun getHashtags(source: String): ArrayList<String> { //리턴 타입이 arrayList<String>, 가져온 text가 source 파라미터에 들어감
        //이 메서드가 이해되지 않으면 아래 코드 줄 부터 while문 끝날때 까지 복사한 뒤 더미 파일 하나 만들어서 테스트 해보면 이해가 된다.
        val hashtags = arrayListOf<String>() // 빈 배열
        var text = source

        while (text.contains("#")) { //#이라는 문자를 포함한다면 true 리턴해서 while이 돌아감
            var hashtag = ""
            val hash = text.indexOf("#") //해당요소의 인덱스 가져옴, 참고로 indexOf는 첫번째 문자의 인덱스만 가져 온다 예를들어 "#hi #hello #fd " 하면 이 문자열에서 첫번째#의 위치인 0 하나만 리턴함,  참고로 인덱스는 0부터 시작
            /*println(hash)*/
            text = text.substring(hash + 1) //# 을 걸러내기 위해 이렇게 함. 그럼 #hi 였다면 +1 했으니 subString에 의해 hi만 된다. subString은 시작 인덱스부터 쭈욱 반환
            /*println(text)*/
            val firstSpace = text.indexOf(" ") // indexOf가 반환할게 없으면 -1을 반환한다.  마지막 단어만 남았다면 걍 -1 리턴함
            val firstHash = text.indexOf("#") //마지막 단어만 남았다면 걍 -1 리턴함

            if (firstSpace == -1 && firstHash == -1) { //indexOf가 반환할게 없으면 -1을 반환한다. 이 상황에서는 담은 단어가 " " 없고 #도 없는 그냥 hello 같은 단어만 남은 경우이다.
                hashtag = text.substring(0) //이거는 마지막 해쉬태그 딱 하나 남은 상화이니까 그냥 넣는것
            } else if(firstSpace != -1 && firstSpace < firstHash) {
                hashtag = text.substring(0, firstSpace) //첫번째 해쉬태그 넣기
                text = text.substring(firstSpace + 1) //첫 번째 해쉬태그에 넣은거 빼고 다시 다 넣기
            } else { //이 마지막 else가 해쉬태그가 2개 남았을 경우이다. ex) hi #hello
                hashtag = text.substring(0, firstSpace) //앞에 인덱스는 시작이고 뒤에 인덱스는 그 인덱스의 앞 까지가 범위다. ex) #hi #hello 에서 #hi만 하려면 subString(0, 3) 해야 #hi 만 얻을 수 있다.
                text = text.substring(firstSpace) //나머지 다시 다 넣기
            }

            if(!hashtag.isNullOrEmpty()) { //여기서 배열에 넣음
                hashtags.add(hashtag)
            }
        } // while end

        /*  for (element in hashtags){ //이거 돌려보면 어떻게 해쉬태그가 파싱되는지 알 수 있다.
              println(element)
          }*/
        return hashtags
    }

    companion object {
        val PARAM_USER_ID = "UserId"
        val PARAM_USER_NAME = "UserName"
        val PARAM_USER_SELFIE_URL = "UserSelfieUrl"

        fun newIntent(context: Context, userId: String?, userName: String?, userSelfieUrl: String?): Intent { //homeActivity 에서 tweetActivity 버튼 누르면 정보를 유저정보를 보냄
            val intent = Intent(context, TweetActivity::class.java)
            intent.putExtra(PARAM_USER_ID, userId)
            intent.putExtra(PARAM_USER_NAME, userName)
            intent.putExtra(PARAM_USER_SELFIE_URL, userSelfieUrl)
            return intent
        }
    }

}
