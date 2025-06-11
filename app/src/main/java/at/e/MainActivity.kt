package at.e

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import at.e.ui.Common
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.contract.ApiTaskResult
import com.google.android.gms.wallet.contract.TaskResultContracts

class MainActivity : FragmentActivity() {
    private var paymentsCallback: ((ApiTaskResult<PaymentData>) -> Unit)? = null
    private val paymentsLauncher = registerForActivityResult(
        TaskResultContracts.GetPaymentDataResult()
    ) { taskResult ->
        paymentsCallback?.invoke(taskResult)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @SuppressLint("SourceLockedOrientationActivity")
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContent {
            val nc = rememberNavController()
            val gvm: GlobalViewModel by viewModels { GlobalViewModel.Factory(application, nc) }
            this.paymentsCallback = { taskResult -> gvm.notifyPaymentResult(taskResult, nc) }
            gvm.paymentsLauncher = paymentsLauncher
            Common.Container(gvm, nc) { innerPadding ->
                Navigation.Setup(nc, this, gvm, innerPadding)
            }
        }
    }
}
