package com.r0ld3x.telegramsticker

import FileDownloader
import InternalStorageManager
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.r0ld3x.telegramsticker.adapters.ShowStickerPackAdapter
import com.r0ld3x.telegramsticker.databinding.FragmentDownloadBinding
import com.r0ld3x.telegramsticker.dependency.BuildConfig
import com.r0ld3x.telegramsticker.types.Sticker
import com.r0ld3x.telegramsticker.types.StickerSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class DownloadFragment : Fragment() {

    private lateinit var binding: FragmentDownloadBinding
    private lateinit var stickerPack: StickerSet


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentDownloadBinding.inflate(inflater, container, false)
        val recyclerview = binding.recyclerview
        recyclerview.layoutManager = GridLayoutManager(this.requireContext(), 3)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.button.setOnClickListener {
            onSubmitButtonClick()
        }
        binding.downloadButton.setOnClickListener {
            onDownloadClick()

//            val packs = stickerPack.stickers.chunked(30).mapIndexed { index, stickersChunk ->
//                StickerSet(
//                    name = "${stickerPack.name}__${index + 1}",
//                    title = stickerPack.title,
//                    stickerType = stickerPack.stickerType,
//                    stickers = stickersChunk.toMutableList()
//                )
//            }

//            packs.forEachIndexed { index, pack ->
//                println("Pack ${index + 1}: ${pack.name} ${pack.title} stickers")
//                pack.stickers.forEach { sticker ->
//                    Log.i("DEBUG", sticker.fileUniqueId)
//                }
//            }
        }
    }

    private fun onSubmitButtonClick() {
        view?.hideKeyboard()
        val text = binding.stickerPackEditText.text.toString()
        val stickerName = Helper.extractStickerName(text)
        val context = this.requireContext()
        binding.progressBar.visibility = View.VISIBLE
        binding.button.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val stickerPack = withContext(Dispatchers.IO) {
                    TelegramHelper.getInstance().getStickerSet(stickerName)
                }
                if (stickerPack != null) {
                    handleStickerPack(stickerPack)
                } else {
                    Toast.makeText(context, "No Sticker Pack Found.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "No Sticker Pack Found.", Toast.LENGTH_SHORT).show()

            } finally {
                binding.progressBar.visibility = View.GONE
                binding.button.visibility = View.VISIBLE
            }
        }
    }

    private suspend fun handleStickerPack(stickerPack: StickerSet) {
        val context = this.requireContext()

        Toast.makeText(
            context,
            "Sticker pack ${stickerPack.name} is found.",
            Toast.LENGTH_SHORT
        ).show()
        try {
            val stickerList: MutableList<Sticker> = mutableListOf()
            stickerPack.stickers.forEach { sticker ->
                val file = withContext(Dispatchers.IO) {
                    TelegramHelper.getInstance().getFile(sticker.thumbnail.fileId)
                }
                if (file == null) {
                    return@forEach
                }
                val url =
                    "https://api.telegram.org/file/bot${TelegramHelper.BOT_TOKEN}/${file.filePath}"
                val newSticker =
                    sticker.copy(thumbnail = sticker.thumbnail.copy(thumbnailUri = url))

                stickerList.add(newSticker)
            }

            this.stickerPack = stickerPack.copy(stickers = stickerList)
            val adapter = ShowStickerPackAdapter(this.stickerPack.stickers)
            binding.recyclerview.adapter = adapter
            binding.downloadButton.visibility = View.VISIBLE
        } catch (e: Exception) {
            Toast.makeText(this.requireContext(), e.message, Toast.LENGTH_SHORT).show()
        } finally {
            binding.progressBar.visibility = View.GONE
            binding.button.visibility = View.VISIBLE
        }


    }

    private fun onDownloadClick() {
        val context = this.requireContext()
        binding.downloadButton.visibility = View.GONE
        binding.downloadContainer.visibility = View.VISIBLE // Show progress UI
        binding.downloadProgressBar.progress = 0
        Toast.makeText(context, "Fetching Stickers... ", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val updatedStickers: MutableList<Sticker> = mutableListOf()

                stickerPack.stickers.forEachIndexed { index, sticker ->
                    val file = withContext(Dispatchers.IO) {
                        TelegramHelper.getInstance().getFile(sticker.fileId)
                    }
                    if (file != null) {
                        val url =
                            "https://api.telegram.org/file/bot${TelegramHelper.BOT_TOKEN}/${file.filePath}"
                        updatedStickers.add(sticker.copy(downloadUri = url))
                    }

                    withContext(Dispatchers.Main) {
                        val progress = ((index + 1) * 100) / stickerPack.stickers.size
                        binding.downloadProgressBar.progress = progress

                    }
                }

                val updatedStickerPack = stickerPack.copy(stickers = updatedStickers)
                this@DownloadFragment.stickerPack = updatedStickerPack

                val chunkedData = updatedStickerPack.stickers.chunked(30)
                val stickerSets = chunkedData.mapIndexed { index, stickers ->
                    StickerSet(
                        name = "${updatedStickerPack.name}___${index + 1}__${chunkedData.size}",
                        title = updatedStickerPack.title,
                        stickerType = "regular",
                        stickers = stickers
                    )
                }

                val totalStickers = stickerSets.sumOf { it.stickers.size }
                var downloadedCount = 0
                activity?.runOnUiThread {
                    Toast.makeText(context, "Downloading Stickers...", Toast.LENGTH_SHORT).show()
                }
                stickerSets.forEach { stickerSet ->
                    val folder = InternalStorageManager.getStickerFolder(context, stickerSet.name)
                    val finalStickers: MutableList<Sticker> = mutableListOf()
                    stickerSet.stickers
                        .filter { it.downloadUri != null }
                        .forEachIndexed { _, sticker ->
                            val fileName = sticker.downloadUri?.substringAfterLast("/") ?: return@forEach
                            val destinationFile = File(folder, fileName)
                            val destination = withContext(Dispatchers.IO) {
                                FileDownloader.downloadAndConvertToWebP(
                                    sticker.downloadUri,
                                    destinationFile,
                                    context,
                                    sticker.isVideo
                                )
                            }?: return@forEach
                            finalStickers.add(sticker.copy(absolutePath = destination))
                            downloadedCount++
                            withContext(Dispatchers.Main) {
                                binding.downloadProgressBar.progress =
                                    50 + (downloadedCount * 50) / totalStickers
                            }

                        }
                    val isVideo = stickerSet.stickers.any { it.isVideo }
                    if(isVideo) {
                        val destinationFile = File(folder, "tray.png")
                        withContext(Dispatchers.IO) {
                            stickerSet.stickers[0].thumbnail.thumbnailUri?.let {
                                FileDownloader.downloadAndConvertToWebP(
                                    it,
                                    destinationFile,
                                    context,
                                )
                            }
                        } ?: return@forEach
                    }
                    downloadedCount++
                    withContext(Dispatchers.Main) {
                        binding.downloadProgressBar.progress =
                            50 + (downloadedCount * 50) / totalStickers
                    }
                    val updatedStickerPack = stickerSet.copy(stickers = finalStickers)
                    val json = Gson().toJson(updatedStickerPack)
                    InternalStorageManager.saveJson(context, stickerSet.name, json)
                    InternalStorageManager.saveIdentifier(context, stickerSet.name)
                    try {
                        val uri = Uri.parse("content://${BuildConfig.CONTENT_PROVIDER_AUTHORITY}")
                        if (uri.authority.isNullOrEmpty()) {
                            throw IllegalArgumentException("Invalid CONTENT_PROVIDER_AUTHORITY")
                        }
                        context.contentResolver.notifyChange(uri, null)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }

                // Hide Progress Bar and Show Download Button Again
                withContext(Dispatchers.Main) {
                    binding.downloadProgressBar.visibility = View.GONE
                    binding.downloadButton.visibility = View.VISIBLE
                    Toast.makeText(context, "Download Completed!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()

                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @SuppressLint("ServiceCast")
    fun View.hideKeyboard() {
        val inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }

}