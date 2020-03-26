package dev.chujohiroto.felicaapplication

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcF
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {

    private var intentFiltersArray: Array<IntentFilter>? = null
    private var techListsArray: Array<Array<String>>? = null
    private var mAdapter: NfcAdapter? = null
    private val nfcReader = NfcReader()
    private var pendingIntent: PendingIntent ? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pendingIntent = PendingIntent.getActivity(
                this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)

        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)

        try {
            ndef.addDataType("text/plain")
        } catch (e: IntentFilter.MalformedMimeTypeException) {
            throw RuntimeException("fail", e)
        }

        intentFiltersArray = arrayOf(ndef)

        techListsArray = arrayOf(arrayOf(NfcF::class.java.name))

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.NFC)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("Felica","PERMISSION_GRANTED")
        }
        else{
            Log.d("Felica","PERMISSION_OPEN")
            // NfcAdapterを取得
            mAdapter = NfcAdapter.getDefaultAdapter(applicationContext)

            if (mAdapter == null){
                toastMake("NFCリーダーがありません。", 0, 0);
            } else {
                if (!mAdapter!!.isEnabled()) {
                    toastMake("NFCリーダーが有効ではありません。", 0, 0);
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        Log.d("Felica","Init")
        // NFCの読み込みを有効化
        mAdapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)

    }

    override fun onNewIntent(intent: Intent) {
        // IntentにTagの基本データが入ってくるので取得。
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
        Log.d("Felica","Read")
        var NFCans = ByteArray(256)

        try {
            NFCans = nfcReader.readTag(tag)!!
        } catch (e: Exception) {
            toastMake("カードが学生証ではありません", 0, 0);
            e.printStackTrace()
        }

        if (NFCans.size == 256) {
            toastMake("カードが学生証ではありません", 0, 0);
            return
        }

        var student = ""
        //データをとりあえず吐かせる
        for (a in 5..10) {
            student += hexToAscii(Integer.toHexString(NFCans[a].toInt()))
        }

        toastMake(student,0,0);
    }

    private fun hexToAscii(hexStr: String): String {
        val output = StringBuilder("")
        var i = 0
        while (i < hexStr.length) {
            val str = hexStr.substring(i, i + 2)
            output.append(str.toInt(16).toChar())
            i += 2
        }
        return output.toString()
    }

    private var t: Toast? = null

    private fun toastMake(message: String, x: Int, y: Int) {
        if (t == null) {
            t = Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT)
            t!!.setGravity(Gravity.CENTER, x, y)
        } else {
            t!!.setText(message)
            t!!.setGravity(Gravity.CENTER, x, y)
        }
        t?.show()
    }

    override fun onPause() {
        super.onPause()
        mAdapter?.disableForegroundDispatch(this)
    }

}
