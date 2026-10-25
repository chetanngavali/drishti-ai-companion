package com.example.drishti.ui.screens.info

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.drishti.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TERMS_VERSION = "2.0-2026-IN"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfUseScreen(
    onBack: () -> Unit,
    onAccepted: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val termsToast = stringResource(R.string.toast_terms_acknowledged)
    val scope = rememberCoroutineScope()
    val repository = remember { com.example.drishti.supabase.SupabaseRepository(context) }
    val sharedPrefs = remember { context.getSharedPreferences("drishti_settings", Context.MODE_PRIVATE) }
    val scrollState = rememberScrollState()

    var agreedToTerms by remember {
        mutableStateOf(sharedPrefs.getBoolean("terms_agreed_$TERMS_VERSION", false))
    }
    val agreedTimestamp = remember {
        sharedPrefs.getString("terms_agreed_time_$TERMS_VERSION", null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.item_terms_of_use_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.terms_banner_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (agreedTimestamp != null) stringResource(R.string.terms_accepted_on, agreedTimestamp) else stringResource(R.string.terms_banner_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Terms Sections
            TermsSectionCard(
                number = "1",
                title = stringResource(R.string.terms_sec1_title),
                content = stringResource(R.string.terms_sec1_desc)
            )

            TermsSectionCard(
                number = "2",
                title = stringResource(R.string.terms_sec2_title),
                content = stringResource(R.string.terms_sec2_desc)
            )

            TermsSectionCard(
                number = "3",
                title = stringResource(R.string.terms_sec3_title),
                content = stringResource(R.string.terms_sec3_desc)
            )

            // Acceptance Box
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = agreedToTerms,
                        onCheckedChange = { checked ->
                            agreedToTerms = checked
                            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                            sharedPrefs.edit()
                                .putBoolean("terms_agreed_$TERMS_VERSION", checked)
                                .putString("terms_agreed_time_$TERMS_VERSION", if (checked) now else null)
                                .apply()

                            if (checked) {
                                scope.launch {
                                    try {
                                        repository.recordUserConsent("TERMS_OF_USE", TERMS_VERSION, true)
                                    } catch (_: Exception) {}
                                }
                                Toast.makeText(context, termsToast, Toast.LENGTH_SHORT).show()
                                onAccepted?.invoke()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.terms_agreement_checkbox),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TermsSectionCard(number: String, title: String, content: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
