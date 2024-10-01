package com.example.contextmonitor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import com.example.contextmonitor.databinding.ActivitySymptomsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SymptomsActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivitySymptomsBinding
    private lateinit var database: AppDatabase
    private var heartRate: Float = 0f
    private var respiratoryRate: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivitySymptomsBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        database = AppDatabase.getDatabase(this)

        heartRate = intent.getFloatExtra("HEART_RATE", 0f)
        respiratoryRate = intent.getFloatExtra("RESPIRATORY_RATE", 0f)

        viewBinding.buttonUploadSymptoms.setOnClickListener { uploadSymptoms() }
//        viewBinding.buttonClear.setOnClickListener { clearRatings() }
    }

    private fun uploadSymptoms() {
        val nauseaRating = viewBinding.ratingNausea.rating.toInt()
        val headacheRating = viewBinding.ratingHeadache.rating.toInt()
        val diarrheaRating = viewBinding.ratingDiarrhea.rating.toInt()
        val soreThroatRating = viewBinding.ratingSoreThroat.rating.toInt()
        val feverRating = viewBinding.ratingFever.rating.toInt()
        val coughRating = viewBinding.ratingCough.rating.toInt()
        val muscleAcheRating = viewBinding.ratingMuscleAche.rating.toInt()
        val feelingTiredRating = viewBinding.ratingFeelingTired.rating.toInt()
        val lossTasteSmellRating = viewBinding.ratingLossSmellTaste.rating.toInt()
        val shortnessOfBreathRating = viewBinding.ratingShortnesOfBreath.rating.toInt()

        val healthData = HealthData(
            heartRate = heartRate,
            respiratoryRate = respiratoryRate,
            nausea = nauseaRating,
            headache = headacheRating,
            diarrhea = diarrheaRating,
            soreThroat = soreThroatRating,
            fever = feverRating,
            cough = coughRating,
            muscleAche = muscleAcheRating,
            feelingTired = feelingTiredRating,
            shortnessOfBreath = shortnessOfBreathRating,
            lossOfSmellAndTaste = lossTasteSmellRating
        )

        CoroutineScope(Dispatchers.IO).launch {
            database.healthDataDao().insertHealthData(healthData)
            runOnUiThread {
                val intent = Intent(this@SymptomsActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun clearRatings() {
    }
}