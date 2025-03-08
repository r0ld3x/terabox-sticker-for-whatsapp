package com.r0ld3x.telegramsticker

import InternalStorageManager
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.r0ld3x.telegramsticker.adapters.ShowStickerListAdapter
import com.r0ld3x.telegramsticker.adapters.ShowStickerPackAdapter
import com.r0ld3x.telegramsticker.databinding.FragmentFirstBinding
import com.r0ld3x.telegramsticker.dependency.BuildConfig

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)

        return binding.root

    }

    private fun addStickersToWhatsApp(identifier: String, name: String) {
        val authority = BuildConfig.CONTENT_PROVIDER_AUTHORITY

        val intent = Intent("com.whatsapp.intent.action.ENABLE_STICKER_PACK")
        intent.putExtra("sticker_pack_id", identifier)
        intent.putExtra("sticker_pack_authority", authority)
        intent.putExtra("sticker_pack_name", name)

        try {
            startActivityForResult(intent, 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val allStickers = InternalStorageManager.getAllDataJsonFiles(this.requireContext())
        val recyclerview = binding.recyclerview
        recyclerview.layoutManager = LinearLayoutManager(this.requireContext(),)
        val adapter = ShowStickerListAdapter(allStickers, ::addStickersToWhatsApp, this.requireContext())
        recyclerview.adapter = adapter



        binding.fab.setOnClickListener {

            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}