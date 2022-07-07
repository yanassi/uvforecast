package com.websarva.wings.android.uvforecast

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AlertDialog
import androidx.core.os.HandlerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.websarva.wings.android.uvforecast.databinding.ActivityMainBinding
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.sql.Date
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    //bindingのやつ
    private lateinit var binding: ActivityMainBinding

    //データベースヘルパーオブジェクト
    private val _helper = DatabaseHelper(this@MainActivity)


    companion object {
        private const val DEBUG_TAG ="Async"
        private const val UVIINFO_URL = "https://api.openweathermap.org/data/3.0/onecall?"
        private const val APP_ID = "8af69fc3d9673922ca2508bad454b6fe"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        //ツールバーの色
        getWindow().setStatusBarColor(Color.parseColor("#0033ff"))


        //広告
        MobileAds.initialize(this) {}

        val adRequest= AdRequest.Builder().build()
        binding.adView1.loadAd(adRequest)


        //変更ボタンの取得
        val btlistener = btlistener()
        binding.update.setOnClickListener(btlistener)

        //更新ボタンの取得
        val refleshlitener = refleshlistener()
        binding.refresh.setOnClickListener(refleshlitener)


        //前画面で保存した住所の取り出し
        val sharedAddress = getSharedPreferences("address", Context.MODE_PRIVATE)

        val addressName = sharedAddress.getString("addressName", "unknown")

        //住所がなかったら、住所選択画面へ遷移
        if(addressName == "unknown") {
            val intentMain = Intent(this@MainActivity, RegionActivity::class.java)
            startActivity(intentMain)
        }
    }


    //変更ボタンのリスな
    private inner class btlistener: View.OnClickListener {
        override fun onClick(view: View) {

            val intentMain = Intent(this@MainActivity, RegionActivity::class.java)
            intentMain.putExtra("update", "update")
            startActivity(intentMain)
        }
    }


    //更新ボタンのりすな
    private inner class refleshlistener: View.OnClickListener {
        override fun onClick(view: View) {
            onStart()
        }
    }


    //フォアグラウンドに戻った時に更新してくれるように、ここに処理を書く
    override fun onStart() {
        super.onStart()

        //緯度経度取得
        val sharedLocation = getSharedPreferences("location", Context.MODE_PRIVATE)
        val latitude = sharedLocation.getString("latitude", "0")
        val longitude = sharedLocation.getString("longitude", "0")
        //住所取得
        val sharedAddress = getSharedPreferences("address", Context.MODE_PRIVATE)
        val addressName = sharedAddress.getString("addressName", "unknown")


        addressName?.let {
            latitude?.let {
                longitude?.let {
                    //API取得の有無を分岐へ
                    compare(addressName, latitude, longitude)
                }
            }
        }
    }

    override fun onPause() {
        binding.adView1.let{
            if(it != null) {
                it.pause()
            }
        }

        super.onPause()
    }


    override fun onResume() {
        binding.adView1.let{
            if(it != null) {
                it.resume()
            }
        }
        super.onResume()
    }


    //ヘルパー閉じる用
    override fun onDestroy() {
        _helper.close()
        binding.adView1.let{
            if(it != null) {
                it.destroy()
            }
        }
        super.onDestroy()
    }



    //SQL内の日時と現在の日時比較し、更新するか否かを判断
    private fun compare (addressName: String, latitude: String, longitude: String) {
        //今の年月日を取得する(使いやすいようにString型にする)
        val _date = LocalDate.now()
        //日付を取得
        val date = _date.toString()
        //時間を取得(後に計算で使うのでInt型で)
        val hour = LocalDateTime.now().hour


        //SQLから前回データ取り出し
        //データベースヘルパーを使えるようにする
        val db = _helper.writableDatabase
        //検索SQL文字列の用意
        val sql = "SELECT * FROM UVIs WHERE _id = '${addressName}' "
        //SQLから前回データの取り出し
        val cursor = db.rawQuery(sql, null)

        //前回の取得日時の殻
        var past: String? = ""
        //前回の取得データの殻
        var data: String? = ""


        while(cursor.moveToNext()) {

            //前回の取得日時を取り出す
            val idxDate = cursor.getColumnIndex("date")
            past = cursor.getString(idxDate)

            //前回の取得データを取り出す
            val idxData = cursor.getColumnIndex("data")
            data = cursor.getString(idxData)
        }

        cursor.close()

        //ネットに繋がってるかチェックする関数のインスタンス
        val capabilities = connectCheck()

        //前回取得時の年月日が現在と異なるor前回の保存データが無ければ 、API取得へ
        if(past != date || data == "") {
            //ネットに繋がってればAPI取得へ
            if(capabilities != null) {
                val urlFull = "${UVIINFO_URL}lat=${latitude}&lon=${longitude}&exclude=daily&appid=${APP_ID}"
                receiveUVIInfo(hour, urlFull, addressName, date)
                //繋がってなければ警告出す
            } else {
                Alert(R.string.dialog_msg2)
            }

            //年月日同じ且つ前回データ存在するなら、加工して、表示画面へ
        } else {
            //文字列データをjSONオブジェクトへ
            val json = JSONObject(data!!)
            //その中から一週間毎の天気情報配列を取得
            val hourly = json.getJSONArray("hourly")
            //必要なデータを配列に変換する
            adjust(hourly, hour, addressName)
        }
    }


    //ネットに繋がってるかチェック
    private fun connectCheck() : NetworkCapabilities?{
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.getNetworkCapabilities(cm.activeNetwork)
    }



    //APIを取得する前に、別スレッドへの処理を誘導
    @UiThread
    private fun receiveUVIInfo(hour: Int, urlFull: String, addressName: String, date: String) {
        val handler = HandlerCompat.createAsync(mainLooper)

        val backgroundReceiver = UVIInfoBackgroundReceiver(handler, hour, urlFull, addressName, date)
        val executeService = Executors.newSingleThreadExecutor()
        executeService.submit(backgroundReceiver)
    }



    //APIの取得
    private inner class UVIInfoBackgroundReceiver(handler: Handler, hour: Int, urlFull: String, addressName: String, date: String):Runnable {

        val _handler = handler
        val _hour = hour
        val _urlFull = urlFull
        val _addressName = addressName
        val _date = date

        @WorkerThread
        override fun run() {

            var result = ""

            val url = URL(_urlFull)

            val con = url.openConnection() as? HttpURLConnection

            con?.let {
                try {
                    it.connectTimeout = 3000

                    it.readTimeout = 3000

                    it.requestMethod = "GET"

                    it.connect()

                    val stream = it.inputStream

                    result = is2String(stream)

                    stream.close()

                    //タイムアウトしたら受け止めて、警告出す
                }catch(ex: RuntimeException){

                    val Alert = getAlertPostExecutor()
                    _handler.post(Alert)
                    Log.w(DEBUG_TAG, "通信タイムアウト", ex)

                }
                it.disconnect()
            }

            //API取得後SQLへ保存(住所、データ、取得年月日)
            store(_addressName, result, _date)

            //元スレッドへ戻して、配列作成へ
            val postExecutor = UVIInfoPostExecutor(result, _hour, _addressName)
            _handler.post(postExecutor)

        }
    }

    //InputStreamをString型へ変換
    private fun is2String(stream: InputStream): String {
        val sb = StringBuilder()
        val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
        var line = reader.readLine()
        while(line != null) {
            sb.append(line)
            line = reader.readLine()
        }
        reader.close()
        return sb.toString()
    }


    //UIスレッドへ戻って、警告出す関数へ渡す
    private inner class getAlertPostExecutor():Runnable {

        @UiThread
        override fun run() {
            Alert(R.string.dialog_msg2)
        }
    }



    //警告作る
    private fun Alert(message: Int) {

        AlertDialog.Builder(this@MainActivity)
            .setTitle(R.string.dialog_title)
            .setMessage(message)
            .setPositiveButton(R.string.dialog_btn_ok) { dialog, which -> }
            .show()
    }



    //UVI取得後の処理
    private inner class UVIInfoPostExecutor(result: String, hour: Int, addressName: String): Runnable {

        val _result = result
        val _hour = hour
        val _addressName = addressName

        @UiThread
        override fun run() {
            //文字列をJSONオブジェクトへ
            val json = JSONObject(_result)
            //まずは一週間毎の天気情報配列を取得
            val hourly = json.getJSONArray("hourly")

            //配列作る
            adjust(hourly, _hour, _addressName)
        }
    }




    //取得したデータを表示させるために、配列化する
    private fun adjust (hourly: JSONArray, hour: Int, addressName: String){
        val UVIList: MutableList<MutableMap<String, Any>> = mutableListOf()

        //前から削るインデックス数と後ろから削るインデックス数を取得
        //一番最初のhourを取得
        val firsthour:Int = firstIndexCreate(hourly)
        //前から削るインデックス数
        val firstIndex = hour - firsthour
        //後ろから削るインデックス数
        val lastIndex = 47-firsthour

        //指定したインデックス数まで、配列を作る
        for(i in firstIndex..lastIndex) {
            //i番目のデータを取得
            val firstObject = hourly.getJSONObject(i)
            //uvi取り出し
            val uvi_double = firstObject.getDouble("uvi")
            val uvi = Math.round(uvi_double*10.0)/10.0
            //UNIX型の時間取り出し
            val unixTime = firstObject.getString("dt")
            //時間のUNIX型を、hourへ変換する
            val hr = unixChange(unixTime,"HH:mm")
            //日にちへ変換
            val dt = unixChange(unixTime, "MM/dd")


            //配列の一要素に格納
            val set= mutableMapOf<String, Any>("time" to hr, "UVI" to uvi, "day" to dt)
            //配列そのものに格納
            UVIList.add(set)
        }

        val currenttime:String = currentTimeCreate(hourly, firstIndex)

        val weatherObject = hourly.getJSONObject(firstIndex).getJSONArray("weather").getJSONObject(0)
        val currentweather = weatherObject.getString("main")
        val description = weatherObject.getString("description")


        represent(UVIList, addressName, currenttime, currentweather,description, hour)
    }


    //JSONデータから、一番最初のhourを取り出す
    private fun firstIndexCreate(hourly: JSONArray): Int {
        val firstUNIX = hourly.getJSONObject(0).getString("dt")
        val firsttime = Date(firstUNIX.toInt()*1000L)
        val sdf = SimpleDateFormat("HH")
        return sdf.format(firsttime).toInt()
    }


    //UnixTimeから日付か時間を出す
    private fun unixChange(unixTime: String, format :String) :String {
        val nowTime = Date(unixTime.toInt()*1000L)
        val sdf = SimpleDateFormat(format)
        return sdf.format(nowTime)
    }


    private fun currentTimeCreate(hourly: JSONArray, firstIndex: Int): String {
        val firstUNIX = hourly.getJSONObject(firstIndex).getString("dt")
        val firsttime = Date(firstUNIX.toInt()*1000L)
        val sdf = SimpleDateFormat("MM/dd HH:mm")
        return sdf.format(firsttime)
    }


    //UVIを画面に表示する
    private fun represent(UVIList: MutableList<MutableMap<String, Any>>, addressName: String, currenttime: String, currentweather: String, description: String,  hour: Int) {


        //住所表示
        binding.location.setText(addressName)

        //現在のUV値と時間を表示
        val firstList = UVIList[0]
        val firstUVI = firstList["UVI"].toString()

        binding.mainUVI.setText(firstUVI)
        binding.mainTime.setText(currenttime)


        //UVIの値に応じて危険度を設定
        when(firstUVI.toDouble().toInt()) {

            in 8..10 -> {
                createMainzone(R.drawable.redgradation, "非常に強い")
            }
            in 6..7 -> {
                createMainzone(R.drawable.orangegradation, "強い")
            }
            in 3..5 -> {
                createMainzone(R.drawable.yellowgradation, "普通")
            }
            in 1..2 -> {
                createMainzone(R.drawable.greengradation, "弱い")
            }
            0 -> {
                createMainzone(R.drawable.graygradation, "非常に弱い")
            }
            else -> {
                createMainzone(R.drawable.purplegradation, "危険!!")
            }
        }


        if(hour >= 19) {
            createBackground(R.drawable.nightcolor, R.drawable.night, "#000000")
        } else {
            when(currentweather) {

                "Clear" -> {
                    createBackground(R.drawable.skycolor, R.drawable.sunny, "#0095d9")
                }
                "Rain","Thunderstorm","Squall" -> {
                    createBackground(R.drawable.raincolor, R.drawable.rainy, "#003399")
                }
                "Snow" -> {
                    createBackground(R.drawable.snowcolor, R.drawable.snow, "#999999")
                }
                else -> {
                    when(description) {
                        "overcast clouds"-> {
                            createBackground(R.drawable.cloudcolor, R.drawable.cloud, "#666666")
                        }
                        else -> {
                            createBackground(R.drawable.skycolor, R.drawable.sunny, "#0095d9")
                        }
                    }
                }
            }
        }



        //下部のRecyclerViewの処理
        val Radapter = subListAdapter(this, UVIList)
        binding.subListR.adapter = Radapter
        binding.subListR.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

    }

    //中心部の色と表示
    private fun createMainzone(gradation: Int, risk: String) {

        binding.mainView.setBackgroundResource(gradation)
        binding.risk.setText(risk)
    }

    private fun createBackground(backcolor: Int, weather: Int, btncolor: String) {
        binding.mainLayout.setBackgroundResource(backcolor)
        binding.weatherIcon.setImageResource(weather)
        binding.update.backgroundTintList = ColorStateList.valueOf(Color.parseColor(btncolor))
        binding.refresh.backgroundTintList = ColorStateList.valueOf(Color.parseColor(btncolor))

    }


    //SQLに保存する
    private fun store(addressName: String, data: String, date: String) {
        val db = _helper.writableDatabase
        //最初は既存のデータを削除
        val sqldelete = "DELETE FROM UVIs WHERE _id = ?"
        var stmt = db.compileStatement(sqldelete)
        stmt.bindString(1, addressName)
        stmt.executeUpdateDelete()

        //住所、データ、取得日を保存
        val sqlInsert = "INSERT INTO UVIs (_id, data, date) VALUES (?, ?, ?)"
        stmt = db.compileStatement(sqlInsert)
        stmt.bindString(1, addressName)
        stmt.bindString(2, data)
        stmt.bindString(3, date)
        stmt.executeInsert()
    }

}



//ここら辺はRecyclerViewを作るところ。
class subListAdapter(context: Context, private val UVIList: MutableList<MutableMap<String, Any>>)
    : RecyclerView.Adapter<subListAdapter.subListViewHolder>() {

    private val inflater = LayoutInflater.from(context)


    override fun getItemCount() = UVIList.size


    override fun onBindViewHolder(holder: subListViewHolder, position: Int) {
        val uviList = UVIList[position]
        holder.sub_uvi.text =uviList["UVI"].toString()
        holder.sub_time.text = uviList["time"].toString()

        when(uviList["UVI"].toString().toDouble().toInt()) {

            in 8..10 -> {
                holder.sub_view.setBackgroundResource(R.drawable.redgradation)

            }
            in 6..7 -> {
                holder.sub_view.setBackgroundResource(R.drawable.orangegradation)

            }
            in 3..5 -> {
                holder.sub_view.setBackgroundResource(R.drawable.yellowgradation)

            }
            in 1..2 -> {
                holder.sub_view.setBackgroundResource(R.drawable.greengradation)

            }
            0 -> {
                holder.sub_view.setBackgroundResource(R.drawable.graygradation)

            }
            else -> {
                holder.sub_view.setBackgroundResource(R.drawable.purplegradation)

            }

        }

        if(uviList["time"].toString() == "00:00" || position == 0 ) {
            holder.sub_day.text = uviList["day"].toString()
        }else {
            holder.sub_day.text = ""
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): subListViewHolder {
        val view = inflater.inflate(R.layout.row, parent, false)
        val viewHolder = subListViewHolder(view)

        return viewHolder
    }

    class subListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val sub_time = view.findViewById<TextView>(R.id.sub_time)
        val sub_uvi = view.findViewById<TextView>(R.id.sub_UVI)
        val sub_day = view.findViewById<TextView>(R.id.sub_day)
        val sub_view = view.findViewById<TextView>(R.id.sub_view)


    }



}