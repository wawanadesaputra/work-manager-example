package id.ac.polbeng.wawansaputra.workmanagerexample

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.workDataOf
import id.ac.polbeng.wawansaputra.workmanagerexample.databinding.ActivityMainBinding
import java.util.UUID
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private val workManager by lazy { WorkManager.getInstance(applicationContext) }

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresStorageNotLow(true)
        .setRequiresBatteryNotLow(true)
        .build()

    private val maxCounter = workDataOf(LoopWorker.COUNTER to 10)
    private lateinit var activityHomeBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityHomeBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityHomeBinding.root)

        activityHomeBinding.btnImageDownload.setOnClickListener {
            showLottieAnimation()
            activityHomeBinding.tvProgress.visibility = View.VISIBLE
            activityHomeBinding.downloadLayout.visibility = View.GONE
            createOneTimeWorkRequest()
            // createPeriodicWorkRequest()
            // createDelayedWorkRequest()
        }

        activityHomeBinding.btnQueryWork.setOnClickListener {
            queryWorkInfo()
        }
    }

    private fun showLottieAnimation() {
        activityHomeBinding.animationView.visibility = View.VISIBLE
    }

    private fun hideLottieAnimation() {
        activityHomeBinding.animationView.visibility = View.GONE
    }

    private fun createOneTimeWorkRequest() {
        // 1
        val imageWorker = OneTimeWorkRequestBuilder<LoopWorker>()
            .setInputData(maxCounter)
            .setConstraints(constraints)
            .addTag("imageWork")
            .build()

        // 2
        workManager.enqueueUniqueWork("oneTimeImageDownload", ExistingWorkPolicy.KEEP, imageWorker)

        // 3
        observeWork(imageWorker.id)
    }

    private fun observeWork(id: UUID) {
        // 1
        workManager.getWorkInfoByIdLiveData(id).observe(this) { info ->
            if (info != null) {
                val progress = info.progress
                val value = progress.getInt(LoopWorker.PROGRESS, 0)
                activityHomeBinding.tvProgress.text = "Progress $value%"

                // 2
                if (info.state.isFinished) {
                    hideLottieAnimation()
                    activityHomeBinding.tvProgress.visibility = View.GONE
                    activityHomeBinding.btnImageDownload.visibility = View.GONE
                    activityHomeBinding.btnQueryWork.visibility = View.VISIBLE
                    activityHomeBinding.downloadLayout.visibility = View.VISIBLE

                    // 3
                    val message = info.outputData.getString(LoopWorker.MESSAGE)
                    if (message != null) {
                        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun createPeriodicWorkRequest() {
        // 1
        val imageWorker = PeriodicWorkRequestBuilder<LoopWorker>(2, TimeUnit.MINUTES)
            .setInputData(maxCounter)
            .setConstraints(constraints)
            .addTag("imageWork")
            .build()

        // 2
        workManager.enqueueUniquePeriodicWork("periodicImageDownload", ExistingPeriodicWorkPolicy.KEEP, imageWorker)
        observeWork(imageWorker.id)
    }

    private fun createDelayedWorkRequest() {
        val imageWorker = OneTimeWorkRequestBuilder<LoopWorker>()
            .setInputData(maxCounter)
            .setConstraints(constraints)
            .setInitialDelay(30, TimeUnit.SECONDS)
            .addTag("imageWork")
            .build()

        workManager.enqueueUniqueWork("delayedImageDownload", ExistingWorkPolicy.KEEP, imageWorker)
        observeWork(imageWorker.id)
    }

    private fun queryWorkInfo() {
        // 1
        val workQuery = WorkQuery.Builder
            .fromTags(listOf("imageWork"))
            .addStates(listOf(WorkInfo.State.SUCCEEDED))
            .addUniqueWorkNames(
                listOf("oneTimeImageDownload", "delayedImageDownload", "periodicImageDownload")
            )
            .build()

        // 2
        workManager.getWorkInfosLiveData(workQuery).observe(this) { workInfos ->
            activityHomeBinding.tvWorkInfo.visibility = View.VISIBLE
            activityHomeBinding.tvWorkInfo.text = resources.getQuantityString(R.plurals.text_work_desc, workInfos.size, workInfos.size)
        }
    }
}
