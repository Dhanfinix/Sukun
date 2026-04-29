package dhanfinix.android.sukun.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dhanfinix.android.sukun.MainViewModel
import dhanfinix.android.sukun.R
import dhanfinix.android.sukun.core.datastore.AppLanguage
import dhanfinix.android.sukun.core.datastore.AppTheme
import dhanfinix.android.sukun.core.datastore.TimeFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    mainVm: MainViewModel,
    onOpenAbout: () -> Unit,
    onOpenWebView: (String, String) -> Unit,
    onDonate: () -> Unit,
    onBack: () -> Unit
) {
    val appTheme by mainVm.appTheme.collectAsState()
    val useDynamicColor by mainVm.useDynamicColor.collectAsState()
    val appLanguage by mainVm.appLanguage.collectAsState()

    val timeFormat by mainVm.timeFormat.collectAsState()
    val isReminderEnabled by mainVm.isReminderEnabled.collectAsState()
    val reminderMinutes by mainVm.reminderMinutes.collectAsState()

    var showThemeSheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showTimeFormatSheet by remember { mutableStateOf(false) }
    var showReminderSheet by remember { mutableStateOf(false) }
    var showExtendMinutesSheet by remember { mutableStateOf(false) }
    
    val silenceExtendMinutes by mainVm.silenceExtendMinutes.collectAsState()



    val isArabic = java.util.Locale.getDefault().language == "ar"
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.btn_cancel))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance Section
            SettingsSectionTitle(stringResource(R.string.section_appearance))
            
            SettingsItem(
                title = stringResource(R.string.app_theme),
                subtitle = when (appTheme) {
                    AppTheme.SYSTEM -> stringResource(R.string.theme_system)
                    AppTheme.LIGHT -> stringResource(R.string.theme_light)
                    AppTheme.DARK -> stringResource(R.string.theme_dark)
                },
                icon = Icons.Rounded.Palette,
                onClick = { showThemeSheet = true }
            )

            SettingsItem(
                title = stringResource(R.string.dynamic_color),
                subtitle = stringResource(R.string.dynamic_color_desc),
                icon = Icons.Rounded.ColorLens,
                onClick = { mainVm.setUseDynamicColor(!useDynamicColor) },
                trailing = {
                    Switch(
                        checked = useDynamicColor,
                        onCheckedChange = { mainVm.setUseDynamicColor(it) }
                    )
                }
            )

            SettingsItem(
                title = stringResource(R.string.app_language),
                subtitle = when (appLanguage) {
                    AppLanguage.SYSTEM -> stringResource(R.string.language_system)
                    AppLanguage.EN -> stringResource(R.string.language_english)
                    AppLanguage.ID -> stringResource(R.string.language_indonesian)
                    AppLanguage.AR -> stringResource(R.string.language_arabic)
                },
                icon = Icons.Rounded.Language,
                onClick = { showLanguageSheet = true }
            )

            SettingsItem(
                title = stringResource(R.string.time_format_title),
                subtitle = when (timeFormat) {
                    TimeFormat.AUTO -> stringResource(R.string.time_format_auto)
                    TimeFormat.H12 -> stringResource(R.string.time_format_12)
                    TimeFormat.H24 -> stringResource(R.string.time_format_24)
                },
                icon = Icons.Rounded.AccessTime,
                onClick = { showTimeFormatSheet = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Notifications Section
            SettingsSectionTitle(stringResource(R.string.section_notifications))
            
            SettingsItem(
                title = stringResource(R.string.prayer_reminder),
                subtitle = stringResource(R.string.reminder_desc),
                icon = Icons.Rounded.Notifications,
                trailing = {
                    Switch(
                        checked = isReminderEnabled,
                        onCheckedChange = { mainVm.setReminderEnabled(it) }
                    )
                },
                onClick = { mainVm.setReminderEnabled(!isReminderEnabled) }
            )

            if (isReminderEnabled) {
                SettingsItem(
                    title = stringResource(R.string.reminder_time),
                    subtitle = stringResource(R.string.minutes_before, reminderMinutes),
                    icon = Icons.Rounded.NotificationAdd,
                    onClick = { showReminderSheet = true }
                )
            }
            
            SettingsItem(
                title = stringResource(R.string.setting_extend_silence),
                subtitle = stringResource(R.string.setting_extend_silence_desc) + " (${stringResource(R.string.minutes_format, silenceExtendMinutes)})",
                icon = Icons.Rounded.MoreTime,
                onClick = { showExtendMinutesSheet = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // General Section
            SettingsSectionTitle(stringResource(R.string.section_general))
            
            SettingsItem(
                title = stringResource(R.string.show_onboarding),
                subtitle = stringResource(R.string.show_onboarding_desc),
                icon = Icons.AutoMirrored.Rounded.Help,
                onClick = { 
                    mainVm.setCoachmarkShown(false)
                    onBack()
                }
            )

            SettingsItem(
                title = stringResource(R.string.rate_app),
                subtitle = stringResource(R.string.rate_app_desc),
                icon = Icons.Rounded.Star,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=dhanfinix.android.sukun"))
                    context.startActivity(intent)
                }
            )

            SettingsItem(
                title = stringResource(R.string.label_support_sukun),
                subtitle = stringResource(R.string.desc_support_sukun),
                icon = Icons.Rounded.Favorite,
                onClick = onDonate
            )

            SettingsItem(
                title = stringResource(R.string.about_sukun),
                subtitle = null,
                icon = Icons.Rounded.Info,
                onClick = onOpenAbout
            )
        }

        if (showThemeSheet) {
            ThemeSelectionSheet(
                currentTheme = appTheme,
                onThemeSelected = { mainVm.setTheme(it) },
                onDismiss = { showThemeSheet = false }
            )
        }

        if (showLanguageSheet) {
            LanguageSelectionSheet(
                currentLanguage = appLanguage,
                onLanguageSelected = { mainVm.setLanguage(it) },
                onDismiss = { showLanguageSheet = false }
            )
        }




        if (showTimeFormatSheet) {
            TimeFormatSelectionSheet(
                currentFormat = timeFormat,
                onFormatSelected = { mainVm.setTimeFormat(it) },
                onDismiss = { showTimeFormatSheet = false }
            )
        }

        if (showReminderSheet) {
            ReminderSelectionSheet(
                currentMinutes = reminderMinutes,
                onMinutesSelected = { mainVm.setReminderMinutes(it) },
                onDismiss = { showReminderSheet = false }
            )
        }
        
        if (showExtendMinutesSheet) {
            ExtendMinutesSelectionSheet(
                currentMinutes = silenceExtendMinutes,
                onMinutesSelected = { mainVm.setSilenceExtendMinutes(it) },
                onDismiss = { showExtendMinutesSheet = false }
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (trailing != null) {
                trailing()
            } else if (onClick != null) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelectionSheet(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.app_theme),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
            )

            val options = listOf(
                AppTheme.SYSTEM to stringResource(R.string.theme_system),
                AppTheme.LIGHT to stringResource(R.string.theme_light),
                AppTheme.DARK to stringResource(R.string.theme_dark)
            )

            options.forEach { (value, label) ->
                val isSelected = value == currentTheme
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { 
                            onThemeSelected(value)
                            onDismiss()
                        },
                    shape = MaterialTheme.shapes.medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelectionSheet(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.app_language),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
            )

            val options = listOf(
                AppLanguage.SYSTEM to stringResource(R.string.language_system),
                AppLanguage.EN to stringResource(R.string.language_english),
                AppLanguage.ID to stringResource(R.string.language_indonesian),
                AppLanguage.AR to stringResource(R.string.language_arabic)
            )

            options.forEach { (value, label) ->
                val isSelected = value == currentLanguage
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { 
                            onLanguageSelected(value)
                            onDismiss()
                        },
                    shape = MaterialTheme.shapes.medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeFormatSelectionSheet(
    currentFormat: TimeFormat,
    onFormatSelected: (TimeFormat) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.time_format_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
            )

            val options = listOf(
                TimeFormat.AUTO to stringResource(R.string.time_format_auto),
                TimeFormat.H12 to stringResource(R.string.time_format_12),
                TimeFormat.H24 to stringResource(R.string.time_format_24)
            )

            options.forEach { (value, label) ->
                val isSelected = value == currentFormat
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            onFormatSelected(value)
                            onDismiss()
                        },
                    shape = MaterialTheme.shapes.medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderSelectionSheet(
    currentMinutes: Int,
    onMinutesSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.reminder_time),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
            )

            val options = listOf(5, 10, 15, 30)

            options.forEach { minutes ->
                val isSelected = minutes == currentMinutes
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            onMinutesSelected(minutes)
                            onDismiss()
                        },
                    shape = MaterialTheme.shapes.medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.minutes_before, minutes),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtendMinutesSelectionSheet(
    currentMinutes: Int,
    onMinutesSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.setting_extend_silence),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
            )

            val options = listOf(5, 10, 15, 30, 60)

            options.forEach { minutes ->
                val isSelected = minutes == currentMinutes
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            onMinutesSelected(minutes)
                            onDismiss()
                        },
                    shape = MaterialTheme.shapes.medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.minutes_format, minutes),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
