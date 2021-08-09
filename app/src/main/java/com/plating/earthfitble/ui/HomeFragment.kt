package com.plating.earthfitble.ui

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.plating.earthfitble.R
import com.plating.earthfitble.databinding.HomeFragmentBinding
import com.plating.earthfitble.viewmodels.MainViewModel

class HomeFragment : Fragment() {

    private val viewModel:MainViewModel by activityViewModels()
    private var _binding:HomeFragmentBinding?=null
    private val binding get() = _binding!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = HomeFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("DEBUG", "HomeFragment-OnCreated")
        binding.scanButton.setOnClickListener {
            val fm = parentFragmentManager
            ScanDialogFragment().show(fm, null)
        }

        binding.goNext.setOnClickListener {
            findNavController().navigate(R.id.action_takeImageFragment, null)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}