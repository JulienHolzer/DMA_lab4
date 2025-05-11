package ch.heigvd.iict.dma.labo4.adapters

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ch.heigvd.iict.dma.labo4.R

@SuppressLint("MissingPermission", "NotifyDataSetChanged")
class BleScanAdapter(_items : List<ScanResult> = listOf())  :  RecyclerView.Adapter<BleScanAdapter.ViewHolder>()  {

    var itemClickListener : OnItemClickListener? = null

    var items = listOf<ScanResult>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        items = _items
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.ble_scan_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private val name = view.findViewById<TextView>(R.id.scan_item_name)
        private val address = view.findViewById<TextView>(R.id.scan_item_address)

        fun bind(scanResult : ScanResult) {
            name.text = scanResult.device?.name ?: "unknown"
            address.text = scanResult.device.address
            view.setOnClickListener {
                itemClickListener?.onItemClick(scanResult)
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(scanEntry : ScanResult)
    }

}