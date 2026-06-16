package com.tzh.sme.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tzh.sme.ui.SettingsType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDetailScreen(
    type: SettingsType,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(type.labelRes)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (type) {
                SettingsType.LANGUAGES -> LanguageSettings()
                SettingsType.FAQ -> FAQSettings()
                SettingsType.PRIVACY_POLICY -> LegalContent(title = "Privacy Policy")
                SettingsType.TERMS_OF_SERVICE -> LegalContent(title = "Terms of Service")
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Content for ${stringResource(type.labelRes)} is coming soon.")
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageSettings() {
    val languages = listOf("English (en)", "Myanmar (my)", "Thai (th)")
    var selectedLanguage by remember { mutableStateOf(languages[0]) }

    LazyColumn {
        items(languages) { language ->
            ListItem(
                headlineContent = { Text(language) },
                trailingContent = {
                    if (language == selectedLanguage) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                modifier = Modifier.clickable { selectedLanguage = language }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
        }
    }
}

@Composable
fun FAQSettings() {
    val faqs = listOf(
        "How to add a product?" to "Go to Stock Management and tap the '+' button.",
        "How to process a sale?" to "Select products in POS tab and tap CHECKOUT.",
        "Can I use the app offline?" to "Yes, most features work offline and sync when connected.",
        "How to backup my data?" to "Data is automatically synced to your cloud account.",
        "How to change my profile?" to "Go to Profile tab to update your information."
    )

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(faqs) { (question, answer) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = answer, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun LegalContent(title: String) {
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item {
            Text(
                text = "Last updated: May 2026",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "$title Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Welcome to our $title. This document explains how we handle your information and the rules for using our SME Business application.\n\n" +
                        "1. Information Collection\nWe collect data necessary to provide POS and stock management services.\n\n" +
                        "2. Data Usage\nYour data is used to maintain your business records and improve app performance.\n\n" +
                        "3. Security\nWe implement industry-standard security measures to protect your business data.\n\n" +
                        "4. User Responsibilities\nUsers are responsible for maintaining the confidentiality of their account credentials.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
