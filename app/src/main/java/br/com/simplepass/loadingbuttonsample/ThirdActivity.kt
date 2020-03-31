package br.com.simplepass.loadingbuttonsample

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_third.*

class ThirdActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_third)
    }

    override fun onStart() {
        super.onStart()
        button.setOnClickListener {
            stopButton.visibility = View.VISIBLE
            button.startAnimation()
        }
        stopButton.setOnClickListener {
            stopButton.visibility = View.GONE
            button.revertAnimation()
        }
    }
}
