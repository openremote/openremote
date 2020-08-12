package io.openremote.generic_app

import androidx.annotation.NonNull;
import android.content.Context
import android.content.Intent
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugins.GeneratedPluginRegistrant

class MainActivity: FlutterActivity() {
    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        GeneratedPluginRegistrant.registerWith(flutterEngine);
    }

    override fun onDestroy() {
        flutterEngine?.platformViewsController?.onFlutterViewDestroyed()
        super.onDestroy()
    }

//    companion object {
//        fun createIntent(context: Context, initialRoute: String = "/"): Intent {
//            return Intent(context, MainActivity::class.java)
//                    .putExtra("initial_route", initialRoute)
//                    .putExtra("background_mode", "opaque")
//                    .putExtra("destroy_engine_with_activity", true)
//        }
//    }
}
