package dev.chujohiroto.felicaapplication

import android.content.ContentValues.TAG
import android.nfc.Tag
import android.nfc.tech.NfcF
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

class NfcReader {

    fun readTag(tag: Tag): ByteArray? {
        val nfc = NfcF.get(tag)
        try {
            nfc.connect()
            // System 1 System code -> 0x0003
            //！変更箇所 共通領域のシステムコードは0xFE00　 System code -> 0xFE00
            val targetSystemCode = byteArrayOf(0xFE.toByte(), 0x00.toByte())

            // make polling command
            val polling = polling(targetSystemCode)

            // send command
            val pollingRes = nfc.transceive(polling)

            // Get System 0's idm
            val targetIDm = Arrays.copyOfRange(pollingRes, 2, 10)

            //　サービスに存在しているデータの長さ　学生証の場合は多分1
            val size = 4

            // Target service code -> 0x090f
            // ！変更箇所　 学生証の該当箇所
            val targetServiceCode = byteArrayOf(0x1a.toByte(), 0x8b.toByte())

            // Read Without Encryption
            val req = readWithoutEncryption(targetIDm, size, targetServiceCode)

            // Send a command to get results
            val res = nfc.transceive(req)

            Log.d("ans", res.toString())

            nfc.close()

            // Parsing results and getting data only
            return parse(res)
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
        }

        return null
    }

    /**
     * Acquisition of Polling command.
     * @param systemCode byte[]
     * @return Polling command
     * @throws IOException
     */
    private fun polling(systemCode: ByteArray): ByteArray {
        val bout = ByteArrayOutputStream(100)

        bout.write(0x00)           //　ダミー
        bout.write(0x00)           // コマンドコード
        bout.write(systemCode[0].toInt())  // systemCode
        bout.write(systemCode[1].toInt())  // systemCode
        bout.write(0x01)           // リクエストコード
        bout.write(0x0f)           // タイムスロット

        val msg = bout.toByteArray()
        msg[0] = msg.size.toByte()
        for (a in msg.indices) {
            Log.d("tag", msg[a].toString())
        } //logdで確認
        return msg
    }

    /**
     * Read Without Encryptionコマンドの取得。
     * @param idm 指定するシステムのID
     * @param size 取得するデータの数
     * @return Read Without Encryptionコマンド
     * @throws IOException
     */
    @Throws(IOException::class)

    private fun readWithoutEncryption(idm: ByteArray, size: Int, serviceCode: ByteArray): ByteArray {
        val bout = ByteArrayOutputStream(100)

        bout.write(0)              // データ長バイトのダミー
        bout.write(0x06)           // コマンドコード
        bout.write(idm)            // IDm 8byte
        bout.write(1)

        bout.write(serviceCode[1].toInt()) // サービスコード下位バイト
        bout.write(serviceCode[0].toInt()) // サービスコード上位バイト
        bout.write(size)           // ブロック数


        for (i in 0 until size) {
            bout.write(0x80)
            bout.write(i)          // ブロック番号
        }

        val msg = bout.toByteArray()
        msg[0] = msg.size.toByte()
        return msg
    }

    /**
     * Read Without Encryption応答の解析。
     * @param res byte[]
     * @return 文字列表現
     * @throws Exception
     */
    @Throws(Exception::class)

    private fun parse(res: ByteArray): ByteArray? {
        if (res[10].toInt() != 0x00) {
            throw RuntimeException("this code is " + res[10])
        }
        // res[12] 応答ブロック数
        //　13からreturnで返す
        val data: ByteArray = ByteArray(64)
        for(a in 0..res.size - 14) {
            data[a] = res[a + 13]
        }
        return data
    }
}