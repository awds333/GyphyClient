package io.demo.fedchenko.giphyclient.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.transition.Transition
import android.transition.TransitionListenerAdapter
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.app.SharedElementCallback
import androidx.core.graphics.drawable.toDrawable
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import io.demo.fedchenko.giphyclient.databinding.ActivityGifViewBinding
import io.demo.fedchenko.giphyclient.model.GifModel
import kotlinx.android.synthetic.main.activity_gif_view.*
import kotlinx.android.synthetic.main.gif_image_view.*


class GifViewActivity : AppCompatActivity() {
    private lateinit var model: GifModel
    private lateinit var binding: ActivityGifViewBinding

    interface GifInfoViewer {
        fun showInfo()
    }

    interface GifDistributor {
        fun share()
    }

    companion object {
        const val MODEL = "model"
        const val REQUEST_CODE = 375

        private var bitmap: Bitmap? = null

        fun start(model: GifModel, bitmap: Bitmap, context: Context, bundle: Bundle?) {
            this.bitmap = bitmap
            val intent = Intent(context, GifViewActivity::class.java)
                .putExtra(MODEL, model)

            (context as Activity).startActivityForResult(intent, REQUEST_CODE, bundle)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.extras?.getParcelable<GifModel>(MODEL)?.also {
            model = it
        } ?: return run {
            finish()
        }

        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: MutableList<String>?,
                sharedElements: MutableMap<String, View>?
            ) {
                if (names == null || sharedElements == null || gifImageViewInclude == null)
                    return
                val sharedElementEnterTransition = window.sharedElementEnterTransition
                sharedElementEnterTransition.addListener(object : TransitionListenerAdapter() {
                    override fun onTransitionEnd(transition: Transition) {
                        super.onTransitionEnd(transition)
                        binding.areButtonsVisible = true
                    }
                })
                sharedElements[names[0]] = gifImageViewInclude
            }
        })

        binding = DataBindingUtil.setContentView(
            this,
            io.demo.fedchenko.giphyclient.R.layout.activity_gif_view
        )

        binding.apply {
            distributor = object : GifDistributor {
                override fun share() {
                    ShareCompat.IntentBuilder.from(this@GifViewActivity)
                        .setType("text/plain")
                        .setChooserTitle("Share Gif")
                        .setText(model.original.url)
                        .startChooser()
                }
            }

            postponeEnterTransition()

            binding.placeHolder = bitmap?.toDrawable(resources)

            gifModel = model

            areButtonsVisible = false

            infoViewer = object : GifInfoViewer {
                override fun showInfo() {
                    val infoDialog: GifInfoDialogFragment = GifInfoDialogFragment.create(model)
                    infoDialog.show(supportFragmentManager, "info_dialog")
                }
            }
        }

    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        binding.areButtonsVisible = true
    }

    override fun onDestroy() {
        if (gifImageView != null)
            Glide.clear(gifImageView)
        super.onDestroy()
    }
}