package com.websarva.wings.android.uvforecast

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.core.os.HandlerCompat
import com.websarva.wings.android.uvforecast.databinding.ActivityCityBinding
import java.io.IOException
import java.lang.IllegalArgumentException
import java.util.concurrent.Executors

class CityActivity : AppCompatActivity() {

    //bindingのやつ
    private lateinit var binding: ActivityCityBinding

    companion object {
        private const val DEBUG_TAG ="Async"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_city)

        binding = ActivityCityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        //ステータスバーの色
        getWindow().setStatusBarColor(Color.parseColor("#0033ff"))

        //ツールバーの指定
        binding.toolbar3.let{
            it.setTitle(R.string.toolbar_title)
            it.setTitleTextColor(Color.WHITE)
            setSupportActionBar(it)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }




        //遷移元から都道県名と市町村リストを取得
        val prefecture = intent.getStringExtra("prefecture")
        val cities = intent.getStringArrayExtra("cities")




        prefecture?.let {
            cities?.let {
                //渡された市町村リストをアダプタ
                val prefecture_adapter =
                    ArrayAdapter(this@CityActivity, android.R.layout.simple_list_item_1, cities)
                binding.cityList.let{
                    it.adapter = prefecture_adapter

                    //市町村リストクリック時リスな(都道府県名渡す)
                    it.onItemClickListener = ListItemClickListener(prefecture)
                }
            }
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }



    //市町村りすなクリックされたら
    private inner class ListItemClickListener(prefecture :String) : AdapterView.OnItemClickListener {

        private val _prefecture = prefecture
        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {

            //クリックした場所を取得
            val placeName = parent.getItemAtPosition(position) as String

            //渡された都道府県名と取得した市町村名を足して、住所を作る
            val addressName = "$_prefecture$placeName"

            //緯度経度取得へ
            getLocation(addressName)

        }
    }

    //緯度経度取得のためのスレッド処理
    private fun getLocation(addressName:String) {

        //ハンドラー
        val handler = HandlerCompat.createAsync(mainLooper)

        //緯度経度の取得処理のインテント
        val backgroundReceiver = LocationBackgroundReceiver(handler, addressName)
        //警告表示を避けるために別スレッドで処理させる
        val executeService = Executors.newSingleThreadExecutor()
        executeService.submit(backgroundReceiver)
    }

    //緯度経度取得の別スレッド処理
    private inner class LocationBackgroundReceiver (handler: Handler, addressName: String): Runnable {

        private val _handler = handler
        private val _addressName = addressName

        //別スレッド内の処理内容
        override fun run() {

            try {
                //緯度経度取得
                //API33以上の場合
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val capabitities = connectCheck()
                    if(capabitities != null) {
                        Geocoder(this@CityActivity).getFromLocationName(_addressName, 1, geocode())
                    }else{
                        val postAlert = getAlertPostExecutor(R.string.dialog_msg3)
                        _handler.post(postAlert)
                    }
                    //API32以下の場合
                } else {
                    val GeocodeList =
                        Geocoder(this@CityActivity).getFromLocationName(_addressName, 1)
                    GeocodeList?.let {
                        val latitude = it[0].latitude
                        val longitude = it[0].longitude
                        //UIスレッドへ戻す
                        val postExecutor =
                            getLocationPostExecutor(latitude, longitude, _addressName)
                        _handler.post(postExecutor)
                    }
                }

            } catch (ex: Exception) {
                //エラったら警告表示
                when(ex) {
                    is IOException -> {
                        val postAlert = getAlertPostExecutor(R.string.dialog_msg3)
                        _handler.post(postAlert)
                        Log.w(DEBUG_TAG, "接続失敗", ex)
                    }
                    is IllegalArgumentException -> {
                        val postAlert = getAlertPostExecutor(R.string.dialog_msg4)
                        _handler.post(postAlert)
                        Log.w(DEBUG_TAG, "地名非該当", ex)
                    }
                }
            }
        }


        //API33以上の時の緯度経度取得リスな
        @RequiresApi(33)
        private inner class geocode() : Geocoder.GeocodeListener {

            override fun onGeocode(geo: MutableList<Address>) {

                val latitude = geo[0].latitude
                val longitude = geo[0].longitude
                //UIスレッドへ
                val postExecutor =
                    getLocationPostExecutor(latitude, longitude, _addressName)
                _handler.post(postExecutor)
            }
        }
    }



    private fun connectCheck() : NetworkCapabilities?{
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.getNetworkCapabilities(cm.activeNetwork)
    }


    //UIスレッドに戻って画面遷移する
    private inner class getLocationPostExecutor(latitude: Double, longitude: Double, addressName :String):Runnable {

        private val _latitude = latitude.toString()
        private val _longitude = longitude.toString()
        private val _addressName = addressName

        @UiThread
        override fun run() {

            //緯度経度をSharedPreferencesへ保存
            val sharedLocation = getSharedPreferences("location", Context.MODE_PRIVATE)

            sharedLocation.edit().putString("latitude", _latitude).apply()
            sharedLocation.edit().putString("longitude", _longitude).apply()

            //住所もSharedPreferencesへ保存
            val sharedAddress = getSharedPreferences("address", Context.MODE_PRIVATE)

            sharedAddress.edit().putString("addressName", _addressName).apply()


            //メイン画面へ遷移
            val intentMain = Intent(this@CityActivity, MainActivity::class.java)
            startActivity(intentMain)
            finish()

        }
    }

    //警告表示のクラス
    private inner class getAlertPostExecutor(msgCode: Int):Runnable {
        val _msgCode = msgCode
        @UiThread
        override fun run() {
            AlertDialog.Builder(this@CityActivity)
                .setTitle(R.string.dialog_title)
                .setMessage(_msgCode)
                .setPositiveButton(R.string.dialog_btn_ok) { dialog, which -> }
                .show()
        }
    }
}