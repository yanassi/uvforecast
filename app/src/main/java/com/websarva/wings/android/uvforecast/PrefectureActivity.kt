package com.websarva.wings.android.uvforecast

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.websarva.wings.android.uvforecast.databinding.ActivityPrefectureBinding

class PrefectureActivity : AppCompatActivity() {

    //bindingのやつ
    private lateinit var binding: ActivityPrefectureBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prefecture)

        binding = ActivityPrefectureBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        //ステータスバーの色
        getWindow().setStatusBarColor(Color.parseColor("#0033ff"))

        //ツールバーの指定
        binding.toolbar2.let{
            it.setTitle(R.string.toolbar_title)
            it.setTitleTextColor(Color.WHITE)
            setSupportActionBar(it)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }


        //広告
        MobileAds.initialize(this) {}

        val adRequest= AdRequest.Builder().build()
        binding.adView3.loadAd(adRequest)


        //遷移元から、都道府県リストを受け取り
        val prefectures = intent.getStringArrayExtra("prefectures")



        prefectures?.let {
            //受け取った都道府県リストを、リストビューへアダプタ
            val prefecture_adapter = ArrayAdapter(this@PrefectureActivity, android.R.layout.simple_list_item_1, prefectures)
            binding.prefecturesList.let{
                it.adapter = prefecture_adapter
                //都道府県リストクリック時リスな
                it.onItemClickListener = ListItemClickListener()
            }
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }




    //都道府県リストクリック時処理
    private inner class ListItemClickListener : AdapterView.OnItemClickListener{
        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {


            //クリックした場所を取得
            val placeName = parent.getItemAtPosition(position) as String

            //クリックした都道府県ごとに処理
            if (placeName == "青森県") {

                createCity(R.array.青森県,placeName)

            }else if (placeName == "岩手県") {

                createCity(R.array.岩手県,placeName)

            }else if (placeName == "宮城県") {

                createCity(R.array.宮城県,placeName)

            }else if (placeName == "秋田県") {

                createCity(R.array.秋田県,placeName)

            }else if (placeName == "山形県") {

                createCity(R.array.山形県,placeName)

            }else if (placeName == "福島県") {

                createCity(R.array.福島県,placeName)

            }else if (placeName == "茨城県") {

                createCity(R.array.茨城県,placeName)

            }else if (placeName == "栃木県") {

                createCity(R.array.栃木県,placeName)

            }else if (placeName == "群馬県") {

                createCity(R.array.群馬県,placeName)

            }else if (placeName == "埼玉県") {

                createCity(R.array.埼玉県,placeName)

            }else if (placeName == "千葉県") {

                createCity(R.array.千葉県,placeName)

            }else if (placeName == "東京都") {

                createCity(R.array.東京都,placeName)

            }else if (placeName == "神奈川県") {

                createCity(R.array.神奈川県,placeName)

            }else if (placeName == "新潟県") {

                createCity(R.array.新潟県,placeName)

            }else if (placeName == "富山県") {

                createCity(R.array.富山県,placeName)

            }else if (placeName == "石川県") {

                createCity(R.array.石川県,placeName)

            }else if (placeName == "福井県") {

                createCity(R.array.福井県,placeName)

            }else if (placeName == "山梨県") {

                createCity(R.array.山梨県,placeName)

            }else if (placeName == "長野県") {

                createCity(R.array.長野県,placeName)

            }else if (placeName == "岐阜県") {

                createCity(R.array.岐阜県,placeName)

            }else if (placeName == "静岡県") {

                createCity(R.array.静岡県,placeName)

            }else if (placeName == "愛知県") {

                createCity(R.array.愛知県,placeName)

            }else if (placeName == "三重県") {

                createCity(R.array.三重県,placeName)

            }else if (placeName == "滋賀県") {

                createCity(R.array.滋賀県,placeName)

            }else if (placeName == "京都府") {

                createCity(R.array.京都府,placeName)

            }else if (placeName == "大阪府") {

                createCity(R.array.大阪府,placeName)

            }else if (placeName == "兵庫県") {

                createCity(R.array.兵庫県,placeName)

            }else if (placeName == "奈良県") {

                createCity(R.array.奈良県,placeName)

            }else if (placeName == "和歌山県") {

                createCity(R.array.和歌山県,placeName)

            }else if (placeName == "鳥取県") {

                createCity(R.array.鳥取県,placeName)

            }else if (placeName == "島根県") {

                createCity(R.array.島根県,placeName)

            }else if (placeName == "岡山県") {

                createCity(R.array.岡山県,placeName)

            }else if (placeName == "広島県") {

                createCity(R.array.広島県,placeName)

            }else if (placeName == "山口県") {

                createCity(R.array.山口県,placeName)

            }else if (placeName == "徳島県") {

                createCity(R.array.徳島県,placeName)

            }else if (placeName == "香川県") {

                createCity(R.array.香川県,placeName)

            }else if (placeName == "愛媛県") {

                createCity(R.array.愛媛県,placeName)

            }else if (placeName == "高知県") {

                createCity(R.array.高知県,placeName)

            }else if (placeName == "福岡県") {

                createCity(R.array.福岡県,placeName)

            }else if (placeName == "佐賀県") {

                createCity(R.array.佐賀県,placeName)

            }else if (placeName == "長崎県") {

                createCity(R.array.長崎県,placeName)

            }else if (placeName== "熊本県") {

                createCity(R.array.熊本県,placeName)

            }else if (placeName == "大分県") {

                createCity(R.array.大分県,placeName)

            }else if (placeName == "宮崎県") {

                createCity(R.array.宮崎県,placeName)

            }else if (placeName == "鹿児島県") {

                createCity(R.array.鹿児島県,placeName)

            }else if (placeName == "沖縄県") {

                createCity(R.array.沖縄県,placeName)

            }
        }
    }


    private fun createCity(citycode: Int, placeName: String) {
        val cities = resources.getStringArray(citycode)
        CreateIntent(placeName, cities)
    }


    //画面遷移処理
    private fun CreateIntent(prefecture: String, cities: Array<String>) {
        //遷移先は市町村選択画面
        val intentCity = Intent(this@PrefectureActivity, CityActivity::class.java)
        //選択した都道府県名を渡す(遷移先で住所の一部に組み込む)
        intentCity.putExtra("prefecture", prefecture)
        //都道府県名に対応した市町村リストを渡す
        intentCity.putExtra("cities", cities)
        startActivity(intentCity)
    }



    override fun onPause() {
        binding.adView3.let{
            if(it != null) {
                it.pause()
            }
        }
        super.onPause()
    }



    override fun onResume() {
        binding.adView3.let {
            if (it != null) {
                it.resume()
            }
        }
        super.onResume()
    }


    override fun onDestroy() {
        binding.adView3.let {
            if (it != null) {
                it.destroy()
            }
        }
        super.onDestroy()
    }
}