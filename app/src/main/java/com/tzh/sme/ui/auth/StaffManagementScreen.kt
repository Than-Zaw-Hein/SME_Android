package com.tzh.sme.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tzh.sme.domain.repository.User
import com.tzh.sme.domain.repository.UserRole
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var staffToDelete by remember { mutableStateOf<User?>(null) }
    var staffToEdit by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(uiState.currentUser?.shopId) {
        uiState.currentUser?.shopId?.let {
            viewModel.sendIntent(AuthUiIntent.LoadShopStaff(it))
        }
    }

    LaunchedEffect(viewModel.sideEffect) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is AuthUiSideEffect.Error -> snackbarHostState.showSnackbar(effect.message)
                is AuthUiSideEffect.Success -> snackbarHostState.showSnackbar(effect.message)
                AuthUiSideEffect.VerificationPending -> {}
                AuthUiSideEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Staff Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (uiState.staffList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No staff added yet", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.staffList) { member ->
                        val isOwner = uiState.currentUser?.id == uiState.currentShop?.ownerId
                        val isSelf = member.id == uiState.currentUser?.id

                        StaffMemberItem(
                            member = member,
                            canManage = (isOwner || member.role != UserRole.ADMIN) && !isSelf,
                            onDelete = { staffToDelete = member },
                            onEdit = { staffToEdit = member }
                        )
                    }
                }
            }
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Staff")
            }
        }
    }

    if (showAddDialog) {
        AddStaffDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, email, pass, phone, role ->
                viewModel.sendIntent(
                    AuthUiIntent.CreateStaffAccount(
                        name,
                        email,
                        pass,
                        phone,
                        role
                    )
                )
                showAddDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Staff created. You may need to log back in.")
                }
            }
        )
    }

    staffToEdit?.let { member ->
        EditStaffDialog(
            member = member,
            onDismiss = { staffToEdit = null },
            onConfirm = { name, phone, role ->
                viewModel.sendIntent(AuthUiIntent.UpdateStaffAccount(member.id, name, phone, role))
                staffToEdit = null
            }
        )
    }

    staffToDelete?.let { member ->
        AlertDialog(
            onDismissRequest = { staffToDelete = null },
            title = { Text("Remove Staff") },
            text = { Text("Are you sure you want to remove ${member.displayName} from this shop?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.sendIntent(AuthUiIntent.RemoveStaff(member.id))
                    staffToDelete = null
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { staffToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StaffMemberItem(member: User, canManage: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.displayName ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = member.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Badge(
                    containerColor = if (member.role == UserRole.ADMIN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(member.role.name, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (canManage) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStaffDialog(
    member: User,
    onDismiss: () -> Unit,
    onConfirm: (String, String, UserRole) -> Unit
) {
    var name by remember { mutableStateOf(member.displayName ?: "") }
    var phone by remember { mutableStateOf(member.phone ?: "") }
    var selectedRole by remember { mutableStateOf(member.role) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Staff Member") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Staff Name") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("Role", style = MaterialTheme.typography.labelLarge)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedRole.name,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }) {
                        UserRole.entries.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.name) },
                                onClick = { selectedRole = role; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, phone, selectedRole) },
                enabled = name.isNotBlank()
            ) { Text("Update") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStaffDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, UserRole) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.SELLER) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {},
        title = { Text("Add New Staff") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Staff Name") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Initial Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
                    Text(
                        "Passwords do not match",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Role", style = MaterialTheme.typography.labelLarge)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedRole.name,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }) {
                        UserRole.entries.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.name) },
                                onClick = { selectedRole = role; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, email, password, phone, selectedRole) },
                enabled = name.isNotBlank() && email.isNotBlank() && password.length >= 6 && password == confirmPassword
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
