package com.websarva.wings.android.uvforecast

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.websarva.wings.android.uvforecast.databinding.ActivityRegionBinding

class RegionActivity : AppCompatActivity() {

    //bindingのやつ
    private lateinit var binding: ActivityRegionBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_region)

        binding = ActivityRegionBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        //ステータスバーの色
        getWindow().setStatusBarColor(Color.parseColor("#0033ff"))

        //ツールバーの指定

        binding.toolbar1.let {
            it.setTitle(R.string.toolbar_title)
            it.setTitleTextColor(Color.WHITE)
            setSupportActionBar(it)
        }


        //広告
        MobileAds.initialize(this) {}

        val adRequest= AdRequest.Builder().build()
        binding.adView2.loadAd(adRequest)


        //住所が保存されているか確認
        val sharedAddress = getSharedPreferences("address", Context.MODE_PRIVATE)
        val addressName = sharedAddress.getString("addressName", "unknown")

        //保存されてなかったら、住所選択を指示
        if(addressName == "unknown") {
            AlertDialog.Builder(this@RegionActivity)
                .setTitle(R.string.setting_title)
                .setMessage(R.string.setting_msg)
                .setPositiveButton(R.string.dialog_btn_ok) { dialog, which -> }
                .show()
            //保存されていた状態で更新ボタン経て来たら、戻るボタン実装する
        } else {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }


        //まずは地方リストを取得
        val regions = resources.getStringArray(R.array.regions)

        //初期画面は地方リストを表示するようアダプタ
        val region_adapter = ArrayAdapter(this@RegionActivity, android.R.layout.simple_list_item_1,regions)
        binding.regionsList.let{
            it.adapter = region_adapter

            //地方リストタップされた時
            it.onItemClickListener = ListItemClickListener()
        }

    }




    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }



    //リストタップ時
    private inner class ListItemClickListener : AdapterView.OnItemClickListener{
        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {

            //各地方ごとの都道府県リストを取得
            val Hokkaido = resources.getStringArray(R.array.北海道)

            val Tohoku = resources.getStringArray(R.array.Tohoku)

            val Kanto = resources.getStringArray(R.array.Kanto)

            val Chubu = resources.getStringArray(R.array.Chubu)

            val Kansai = resources.getStringArray(R.array.Kansai)

            val Chugoku = resources.getStringArray(R.array.Chugoku)

            val Sikoku = resources.getStringArray(R.array.Sikoku)

            val Kyusyu = resources.getStringArray(R.array.Kyusyu)

            //クリックした場所を取得
            val placeName = parent.getItemAtPosition(position) as String


            //クリックした地方リストに応じて、都道府県リストを選択、画面遷移へ
            if (placeName == "東北地方") {
                CreateIntent(Tohoku)

            } else if (placeName == "関東地方") {
                CreateIntent(Kanto)

            } else if (placeName == "中部地方") {
                CreateIntent(Chubu)

            } else if (placeName == "関西地方") {
                CreateIntent(Kansai)

            } else if (placeName == "中国地方") {
                CreateIntent(Chugoku)

            } else if (placeName == "四国地方") {
                CreateIntent(Sikoku)

            } else if (placeName == "九州地方") {
                CreateIntent(Kyusyu)

                //北海道のみ、市町村選択画面へ直接遷移する
            } else if (placeName == "北海道") {

                //遷移先は都道府県選択画面
                val intentCity = Intent(this@RegionActivity, CityActivity::class.java)
                //クリックした地方名(北海道なので実質都道府県名扱い)を渡す
                intentCity.putExtra("prefecture", placeName)
                //地方名に対応した市町村リストを渡す
                intentCity.putExtra("cities", Hokkaido)
                startActivity(intentCity)
            }
        }
    }


    //都道府県選択画面へ遷移
    private fun CreateIntent(prefectures: Array<String>) {
        //遷移先は都道府県選択画面
        val intentPrefecture = Intent(this@RegionActivity, PrefectureActivity::class.java)
        //地方名に対応した都道府県リストを渡す
        intentPrefecture.putExtra("prefectures", prefectures)
        startActivity(intentPrefecture)
    }



    override fun onPause() {
        binding.adView2.let{
            if(it != null) {
                it.pause()
            }
        }
        super.onPause()
    }



    override fun onResume() {
        binding.adView2.let {
            if (it!= null) {
                it.resume()
            }
        }
        super.onResume()
    }


    override fun onDestroy() {
        binding.adView2.let {
            if (it != null) {
                it.destroy()
            }
        }
        super.onDestroy()
    }

}