package ch.heigvd.iict.dma.labo4.ui

import android.bluetooth.le.ScanResult
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import ch.heigvd.iict.dma.labo4.MainActivity
import ch.heigvd.iict.dma.labo4.R
import ch.heigvd.iict.dma.labo4.adapters.BleScanAdapter
import ch.heigvd.iict.dma.labo4.databinding.FragmentScanBinding
import ch.heigvd.iict.dma.labo4.viewmodels.BleViewModel

class BleScanFragment : Fragment(), MenuProvider {

    private val bleViewModel : BleViewModel by activityViewModels()

    private var _binding : FragmentScanBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val scanResultAdapter = BleScanAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false).apply {
            bleScanResults.adapter = scanResultAdapter
            bleScanResults.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bleViewModel.bleScanResults.observe(viewLifecycleOwner) { scannedDevices ->
            if(scannedDevices.isNotEmpty()) {
                binding.bleScanResults.visibility = View.VISIBLE
                binding.bleScanResultsEmpty.visibility = View.GONE
                scanResultAdapter.items = scannedDevices
            } else {
                binding.bleScanResults.visibility = View.GONE
                binding.bleScanResultsEmpty.visibility = View.VISIBLE
            }
        }

        scanResultAdapter.itemClickListener = object : BleScanAdapter.OnItemClickListener {
            override fun onItemClick(scanEntry: ScanResult) {
                bleViewModel.connect(scanEntry.device)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as AppCompatActivity).addMenuProvider(this)
    }

    override fun onPause() {
        super.onPause()
        (requireActivity() as AppCompatActivity).removeMenuProvider(this)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.scan_menu, menu)

        val startMenuEntry = menu.findItem(R.id.menu_ble_scan_start)
        val stopMenuEntry = menu.findItem(R.id.menu_ble_scan_stop)

        // manage menu state
        bleViewModel.blePermissionsGranted.observe(viewLifecycleOwner) {hasPermission ->
            startMenuEntry.isEnabled = hasPermission
            stopMenuEntry.isEnabled = hasPermission
        }

        bleViewModel.isScanning.observe(viewLifecycleOwner) {isScanning ->
            startMenuEntry.isVisible = !isScanning
            stopMenuEntry.isVisible = isScanning
        }

    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when(menuItem.itemId) {
            R.id.menu_ble_scan_start -> {
                (requireActivity() as MainActivity).scanLeDevice(true)
                true
            }
            R.id.menu_ble_scan_stop -> {
                (requireActivity() as MainActivity).scanLeDevice(false)
                true
            }
            else -> false
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = BleScanFragment()
    }

}