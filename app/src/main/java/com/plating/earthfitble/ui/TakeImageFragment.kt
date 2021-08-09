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
import com.plating.earthfitble.databinding.SendDataFragmentBinding
import com.plating.earthfitble.databinding.TakeImageFragmentBinding
import com.plating.earthfitble.utils.Utils
import com.plating.earthfitble.utils.hide
import com.plating.earthfitble.utils.show
import com.plating.earthfitble.viewmodels.MainViewModel

class TakeImageFragment : Fragment() {

    private val viewModel:MainViewModel by activityViewModels()
    private var _binding: TakeImageFragmentBinding?=null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding =  TakeImageFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.progressBar.hide()

        binding.takeImageBtn.setOnClickListener {
            binding.takeImageBtn.text = "촬영 중"
            binding.progressBar.show()
            viewModel.sendMessageToCamera("TAKE:${Utils.getDateString()}")
        }
        binding.goNext.setOnClickListener {
            findNavController().navigate(R.id.action_takeWeightFragment, null)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}