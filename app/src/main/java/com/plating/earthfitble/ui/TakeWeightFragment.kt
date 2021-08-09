package com.plating.earthfitble.ui

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.plating.earthfitble.R
import com.plating.earthfitble.databinding.TakeImageFragmentBinding
import com.plating.earthfitble.databinding.TakeWeightFragmentBinding
import com.plating.earthfitble.viewmodels.MainViewModel
import kotlinx.coroutines.*

class TakeWeightFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: TakeWeightFragmentBinding?=null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =  TakeWeightFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CoroutineScope(Dispatchers.Default).launch {
            delay(2500)
            withContext(Dispatchers.Main){
                findNavController().navigate(R.id.action_sendDatatFragment, null)
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}