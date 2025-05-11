package ch.heigvd.iict.dma.labo4.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class DMABleManager(applicationContext: Context, private val dmaServiceListener: DMAServiceListener? = null) : BleManager(applicationContext) {

    //Services and Characteristics of the SYM Pixl
    private var timeService: BluetoothGattService? = null
    private var symService: BluetoothGattService? = null
    private var currentTimeChar: BluetoothGattCharacteristic? = null
    private var integerChar: BluetoothGattCharacteristic? = null
    private var temperatureChar: BluetoothGattCharacteristic? = null
    private var buttonClickChar: BluetoothGattCharacteristic? = null

    fun requestDisconnection() {
        this.disconnect().enqueue()
    }

    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {

        Log.d(TAG, "isRequiredServiceSupported - discovered services:")
        for (service in gatt.services) {
            Log.d(TAG, service.uuid.toString())
        }

        /* TODO
        - Nous devons vérifier ici que le périphérique auquel on vient de se connecter possède
          bien tous les services et les caractéristiques attendus, on vérifiera aussi que les
          caractéristiques présentent bien les opérations attendues
        - On en profitera aussi pour garder les références vers les différents services et
          caractéristiques (déclarés en lignes 14 à 19)
        */

        // UUIDs des services et caractéristiques attendus
        val TIME_SERVICE_UUID = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb")
        val CURRENT_TIME_CHAR_UUID = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb")

        val SYM_SERVICE_UUID = UUID.fromString("3c0a1000-281d-4b48-b2a7-f15579a1c38f")
        val INTEGER_CHAR_UUID = UUID.fromString("3c0a1001-281d-4b48-b2a7-f15579a1c38f")
        val TEMPERATURE_CHAR_UUID = UUID.fromString("3c0a1002-281d-4b48-b2a7-f15579a1c38f")
        val BUTTON_CLICK_CHAR_UUID = UUID.fromString("3c0a1003-281d-4b48-b2a7-f15579a1c38f")

        // Récupération des services
        timeService = gatt.getService(TIME_SERVICE_UUID)
        symService = gatt.getService(SYM_SERVICE_UUID)

        // Récupération des caractéristiques
        currentTimeChar = timeService?.getCharacteristic(CURRENT_TIME_CHAR_UUID)
        integerChar = symService?.getCharacteristic(INTEGER_CHAR_UUID)
        temperatureChar = symService?.getCharacteristic(TEMPERATURE_CHAR_UUID)
        buttonClickChar = symService?.getCharacteristic(BUTTON_CLICK_CHAR_UUID)

        val allCharacteristicsPresent = listOf(
            currentTimeChar,
            integerChar,
            temperatureChar,
            buttonClickChar
        ).all { it != null }

        return allCharacteristicsPresent

        // si tout est OK, on doit retourner true
        // sinon la librairie appelera la méthode onDeviceDisconnected() avec le flag REASON_NOT_SUPPORTED
    }

    override fun initialize() {
        super.initialize()
        /* TODO
            Ici nous somme sûr que le périphérique possède bien tous les services et caractéristiques
            attendus et que nous y sommes connectés. Nous pouvous effectuer les premiers échanges BLE.
            Dans notre cas il s'agit de s'enregistrer pour recevoir les notifications proposées par certaines
            caractéristiques, on en profitera aussi pour mettre en place les callbacks correspondants.
            CF. méthodes setNotificationCallback().with{} et enableNotifications().enqueue()
         */

        // Initialisation, clickCount à 0 au démarrage
        dmaServiceListener?.clickCountUpdate(0)

        // Notifications pour buttonClick
        setNotificationCallback(buttonClickChar).with { _, data ->

            val clickCount = data.getIntValue(Data.FORMAT_UINT8, 0)
            dmaServiceListener?.clickCountUpdate(clickCount ?: 0)
        }

        // Notifications pour currentTime
        setNotificationCallback(currentTimeChar).with { _, data ->

            val calendar = Calendar.getInstance()

            val year = data.getIntValue(Data.FORMAT_UINT16_LE, 0) ?: 0
            val month = (data.getIntValue(Data.FORMAT_UINT8, 2) ?: 1) - 1 // Calendar months are 0-based
            val day = data.getIntValue(Data.FORMAT_UINT8, 3)?: 0
            val hour = data.getIntValue(Data.FORMAT_UINT8, 4)?: 0
            val minute = data.getIntValue(Data.FORMAT_UINT8, 5)?: 0
            val second = data.getIntValue(Data.FORMAT_UINT8, 6)?: 0

            calendar.set(year, month, day, hour, minute, second)
            dmaServiceListener?.dateUpdate(calendar)
        }

        // Activation des notifications
        enableNotifications(buttonClickChar).enqueue()
        enableNotifications(currentTimeChar).enqueue()
    }

    override fun onServicesInvalidated() {
        super.onServicesInvalidated()
        //we reset services and characteristics
        timeService = null
        currentTimeChar = null
        symService = null
        integerChar = null
        temperatureChar = null
        buttonClickChar = null
    }

    fun readTemperature(): Boolean {
        /* TODO
            on peut effectuer ici la lecture de la caractéristique température
            la valeur récupérée sera envoyée à au ViewModel en utilisant le mécanisme
            du DMAServiceListener: Cf. temperatureUpdate()
                Cf. méthode readCharacteristic().with{}.enqueue()
            On placera des méthodes similaires pour les autres opérations
                Cf. méthode writeCharacteristic().enqueue()
        */

        if (temperatureChar == null) {
            return false
        }

        readCharacteristic(temperatureChar).with { _, data ->

            val rawTemp = data.getIntValue(Data.FORMAT_UINT16_LE, 0) ?: 0
            val temperature = rawTemp / 10.0f

            // Notification au ViewModel via le listener
            dmaServiceListener?.temperatureUpdate(temperature)
        }.enqueue()

        return true
    }

    fun writeInt(value: Int): Boolean {


        if (integerChar == null) {
            return false
        }

        val buffer = ByteBuffer.allocate(4)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(value)

        writeCharacteristic(integerChar, buffer.array(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            .enqueue()
        return true
    }

    fun writeCurrentTime(calendar: Calendar): Boolean {

        if (currentTimeChar == null) {
            return false
        }

        val buffer = ByteBuffer.allocate(10)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        buffer.putShort(calendar.get(Calendar.YEAR).toShort())
        buffer.put((calendar.get(Calendar.MONTH) + 1).toByte()) // +1 car Calendar utilise 0-11 pour les mois
        buffer.put(calendar.get(Calendar.DAY_OF_MONTH).toByte())
        buffer.put(calendar.get(Calendar.HOUR_OF_DAY).toByte())
        buffer.put(calendar.get(Calendar.MINUTE).toByte())
        buffer.put(calendar.get(Calendar.SECOND).toByte())
        buffer.put((calendar.get(Calendar.DAY_OF_WEEK) - 1).toByte()) // -1 car Calendar commence à 1 pour dimanche
        buffer.put(0.toByte()) // Fractions256
        buffer.put(0.toByte()) // Adjust Reason

        writeCharacteristic(currentTimeChar, buffer.array(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            .enqueue()

        return true
    }

    companion object {
        private val TAG = DMABleManager::class.java.simpleName
    }

}

interface DMAServiceListener {
    fun dateUpdate(date : Calendar)
    fun temperatureUpdate(temperature : Float)
    fun clickCountUpdate(clickCount : Int)
}
