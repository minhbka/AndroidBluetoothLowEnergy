package com.plating.earthfitble.ui

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.plating.earthfitble.R
import com.plating.earthfitble.databinding.HomeFragmentBinding
import com.plating.earthfitble.databinding.SendDataFragmentBinding
import com.plating.earthfitble.viewmodels.MainViewModel

class SendDataFragment : Fragment() {

    companion object {
        fun newInstance() = SendDataFragment()
    }

    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: SendDataFragmentBinding?=null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SendDataFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}