package ch.heigvd.iict.dma.labo4.ui

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import ch.heigvd.iict.dma.labo4.R
import ch.heigvd.iict.dma.labo4.databinding.FragmentConnectedBinding
import ch.heigvd.iict.dma.labo4.viewmodels.BleViewModel

class BleConnectedFragment : Fragment(), MenuProvider {

    private val bleViewModel : BleViewModel by activityViewModels()

    private var _binding : FragmentConnectedBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConnectedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO implement connected fragment

        val temperatureTextView = view.findViewById<TextView>(R.id.temperatureTextView)
        val timeTextView = view.findViewById<TextView>(R.id.timeTextView)
        val clickCountTextView = view.findViewById<TextView>(R.id.clickCountTextView)
        val intInputField = view.findViewById<EditText>(R.id.intInputField)
        val sendIntButton = view.findViewById<Button>(R.id.sendIntButton)
        val readTempButton = view.findViewById<Button>(R.id.readTempButton)
        val updateTimeButton = view.findViewById<Button>(R.id.updateTimeButton)

        val vm = bleViewModel


        // Actions
        readTempButton.setOnClickListener {
            Log.d("BleFragment", "readTempButton clicked")
            vm.readTemperature()
        }

        sendIntButton.setOnClickListener {
            val value = intInputField.text.toString().toIntOrNull()
            if (value != null) {
                vm.sendValue(value)
            } else {
                Toast.makeText(requireContext(), "Entrez un entier valide", Toast.LENGTH_SHORT).show()
            }
        }

        updateTimeButton.setOnClickListener {
            vm.setTime()
        }

        // Observateurs
        vm.temperature.observe(viewLifecycleOwner) {
            temperatureTextView.text = "Température : $it °C"
        }

        vm.buttonClick.observe(viewLifecycleOwner) {
            clickCountTextView.text = "Clics : $it"
        }

        vm.currentTime.observe(viewLifecycleOwner) { calendar ->
            if (calendar != null) {
                timeTextView.text = calendar.time.toString()
            } else {
                timeTextView.text = "Pas de date"
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
        menuInflater.inflate(R.menu.connected_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when(menuItem.itemId) {
            R.id.menu_ble_connected_disconnect -> {
                bleViewModel.disconnect()
                true
            }
            else -> false
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = BleConnectedFragment()
    }

}