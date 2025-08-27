package com.example.prog7314_part1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView

class SessionFragment : Fragment() {
    private var isRunning: Boolean = false
    private var elapsedMs: Long = 0
    private var timer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_session, container, false)

        val timerText: TextView = view.findViewById(R.id.timerText)
        val btnStart: Button = view.findViewById(R.id.btnStart)
        val btnPause: Button = view.findViewById(R.id.btnPause)
        val btnReset: Button = view.findViewById(R.id.btnReset)

        fun format(ms: Long): String {
            val totalSeconds = ms / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }

        fun startTimer() {
            if (isRunning) return
            isRunning = true
            timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    elapsedMs += 1000
                    timerText.text = format(elapsedMs)
                }
                override fun onFinish() {}
            }.start()
        }

        fun pauseTimer() {
            isRunning = false
            timer?.cancel()
        }

        fun resetTimer() {
            pauseTimer()
            elapsedMs = 0
            timerText.text = "00:00:00"
        }

        btnStart.setOnClickListener { startTimer() }
        btnPause.setOnClickListener { pauseTimer() }
        btnReset.setOnClickListener { resetTimer() }

        return view
    }
}


