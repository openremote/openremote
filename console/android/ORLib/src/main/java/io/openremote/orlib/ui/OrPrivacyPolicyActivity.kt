package io.openremote.orlib.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import io.openremote.orlib.databinding.ActivityOrPrivacyPolicyBinding

class OrPrivacyPolicyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrPrivacyPolicyBinding
    private var isAccepted = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrPrivacyPolicyBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.webView.loadUrl("https://openremote.io/privacy-policy/")

        binding.buttonAccept.setOnClickListener {
            if (isAccepted) {
                val intent = Intent()
                setResult(RESULT_OK, intent)
                finish()
            }
        }

        binding.checkbox.setOnClickListener(View.OnClickListener { v: View? ->
            isAccepted = !isAccepted

            binding.buttonAccept.setEnabled(isAccepted)
        })
    }
}