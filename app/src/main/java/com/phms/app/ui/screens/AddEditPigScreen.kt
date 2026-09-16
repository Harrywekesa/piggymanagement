package com.phms.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.phms.app.data.local.entity.PigEntity
import com.phms.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPigScreen(pigId: Long, viewModel: MainViewModel, navController: NavController) {
    val isEdit = pigId > 0
    val pig by viewModel.selectedPig.collectAsState()
    val stages by viewModel.stages.collectAsState()
    val pens by viewModel.pens.collectAsState()
    val batches by viewModel.batches.collectAsState()

    LaunchedEffect(pigId) { if (isEdit) viewModel.selectPig(pigId) }

    val activePigs by viewModel.activePigs.collectAsState()

    // Form state
    var tagNumber by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("F") }
    var source by remember { mutableStateOf("Born") }
    var notes by remember { mutableStateOf("") }
    var selectedStageId by remember { mutableLongStateOf(1L) }
    var selectedPenId by remember { mutableStateOf<Long?>(null) }
    var selectedBatchId by remember { mutableStateOf<Long?>(null) }
    var selectedDamId by remember { mutableStateOf<Long?>(null) }
    var selectedSireId by remember { mutableStateOf<Long?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoPath by remember { mutableStateOf<String?>(null) }

    var originFarm by remember { mutableStateOf("") }
    var sellerContact by remember { mutableStateOf("") }
    var purchasePriceStr by remember { mutableStateOf("") }
    var transportCostStr by remember { mutableStateOf("") }

    var showPhotoOptionDialog by remember { mutableStateOf(false) }

    // Populate form if editing
    LaunchedEffect(pig) {
        pig?.let { p ->
            tagNumber = p.tag_number
            breed = p.breed
            sex = p.sex
            source = p.source
            notes = p.notes ?: ""
            selectedStageId = p.current_stage_id
            selectedPenId = p.pen_id
            selectedBatchId = p.batch_id
            selectedDamId = p.dam_id
            selectedSireId = p.sire_id
            photoPath = p.photo_path
            originFarm = p.origin_farm ?: ""
            sellerContact = p.seller_contact ?: ""
            purchasePriceStr = p.purchase_price?.toString() ?: ""
            transportCostStr = p.transport_cost?.toString() ?: ""
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            photoUri = uri
            photoPath = uri.toString()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            try {
                val file = java.io.File(context.cacheDir, "pig_cam_${System.currentTimeMillis()}.jpg")
                val stream = java.io.FileOutputStream(file)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
                stream.flush()
                stream.close()
                val savedUri = Uri.fromFile(file)
                photoUri = savedUri
                photoPath = savedUri.toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Could not open camera: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            android.widget.Toast.makeText(context, "Camera permission is required to take pig photos", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun launchCameraWithPermission() {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Camera error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        containerColor = Color(0xFF0D1117),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEdit) "Edit Pig #${pig?.tag_number}" else "Add New Pig",
                        color = Color.White, fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF161B22))
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photo picker
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Pig Photo", color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold)
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF21262D))
                                .border(2.dp, Color(0xFF4CAF50), CircleShape)
                                .clickable { showPhotoOptionDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            val displayUri = photoUri ?: photoPath?.let { Uri.parse(it) }
                            if (displayUri != null) {
                                AsyncImage(
                                    model = displayUri,
                                    contentDescription = "Pig photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📸", fontSize = 32.sp)
                                    Text("Tap to add photo", color = Color(0xFF6E7681), fontSize = 10.sp)
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { launchCameraWithPermission() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50)),
                                border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Camera", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8B949E)),
                                border = BorderStroke(1.dp, Color(0xFF30363D)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Gallery", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Basic info
            item {
                SectionCard("Basic Information") {
                    FormField("Tag / Ear Number *", tagNumber, { tagNumber = it })
                    Spacer(Modifier.height(12.dp))
                    FormField("Breed", breed, { breed = it }, placeholder = "e.g. Large White, Landrace, Duroc")
                    Spacer(Modifier.height(12.dp))
                    Text("Sex", color = Color(0xFF8B949E), fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("F" to "Sow (Female)", "M" to "Boar (Male)").forEach { (value, label) ->
                            FilterChip(
                                selected = sex == value,
                                onClick = { sex = value },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1B5E20),
                                    selectedLabelColor = Color(0xFF4CAF50)
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Acquisition Source", color = Color(0xFF8B949E), fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Born", "Purchased").forEach { s ->
                            FilterChip(
                                selected = source == s,
                                onClick = { source = s },
                                label = { Text(s) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1B5E20),
                                    selectedLabelColor = Color(0xFF4CAF50)
                                )
                            )
                        }
                    }
                }
            }

            // Purchased details (if source == Purchased)
            if (source.equals("Purchased", true)) {
                item {
                    SectionCard("Purchase & Origin Details") {
                        FormField("Origin Farm / Breeder Name", originFarm, { originFarm = it }, placeholder = "e.g. Greenhill Farm")
                        Spacer(Modifier.height(12.dp))
                        FormField("Seller Contact / Phone", sellerContact, { sellerContact = it }, placeholder = "e.g. +254 700 000000", keyboardType = KeyboardType.Phone)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.weight(1f)) {
                                FormField("Purchase Price (KSh)", purchasePriceStr, { purchasePriceStr = it }, placeholder = "0", keyboardType = KeyboardType.Number)
                            }
                            Box(Modifier.weight(1f)) {
                                FormField("Transport Cost (KSh)", transportCostStr, { transportCostStr = it }, placeholder = "0", keyboardType = KeyboardType.Number)
                            }
                        }
                    }
                }
            }

            // Stage & Assignment
            item {
                SectionCard("Farm Assignment") {
                    if (stages.isNotEmpty()) {
                        Text("Growth Stage *", color = Color(0xFF8B949E), fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        DropdownSelector(
                            label = "Stage",
                            options = stages.map { it.id to it.name },
                            selectedId = selectedStageId,
                            onSelect = { selectedStageId = it ?: 1L }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    // Pen
                    Text("Pen Assignment", color = Color(0xFF8B949E), fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    if (pens.isEmpty()) {
                        Text(
                            "⚠ No pens configured yet — pens can be set up in the Farm Settings.",
                            color = Color(0xFF6E7681), fontSize = 12.sp
                        )
                    } else {
                        DropdownSelector(
                            label = "Pen (optional)",
                            options = listOf(null to "None") + pens.map { it.id to it.name },
                            selectedId = selectedPenId,
                            onSelect = { selectedPenId = it }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    // Batch
                    Text("Batch / Group", color = Color(0xFF8B949E), fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    if (batches.isEmpty()) {
                        Text(
                            "⚠ No batches configured yet — batches help group pigs for herd operations.",
                            color = Color(0xFF6E7681), fontSize = 12.sp
                        )
                    } else {
                        DropdownSelector(
                            label = "Batch (optional)",
                            options = listOf(null to "None") + batches.map { it.id to it.name },
                            selectedId = selectedBatchId,
                            onSelect = { selectedBatchId = it }
                        )
                    }
                }
            }

            // Lineage & Parentage
            item {
                val sowOptions = listOf(null to "None / Unknown") + activePigs.filter { it.sex == "F" && (pigId <= 0 || it.id != pigId) }.map { it.id to "Sow #${it.tag_number} (${it.breed})" }
                val boarOptions = listOf(null to "None / Unknown") + activePigs.filter { it.sex == "M" && (pigId <= 0 || it.id != pigId) }.map { it.id to "Boar #${it.tag_number} (${it.breed})" }
                SectionCard("Parentage & Pedigree Lineage") {
                    Text("Mother Sow (Dam)", color = Color(0xFF8B949E), fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    DropdownSelector(
                        label = "Mother Sow",
                        options = sowOptions,
                        selectedId = selectedDamId,
                        onSelect = { selectedDamId = it }
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Father Boar (Sire)", color = Color(0xFF8B949E), fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    DropdownSelector(
                        label = "Father Boar",
                        options = boarOptions,
                        selectedId = selectedSireId,
                        onSelect = { selectedSireId = it }
                    )
                }
            }

            // Notes
            item {
                SectionCard("Notes") {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        placeholder = { Text("Additional notes...", color = Color(0xFF6E7681)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color(0xFF30363D),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        maxLines = 4
                    )
                }
            }

            // Save button
            item {
                Button(
                    onClick = {
                        if (tagNumber.isNotBlank()) {
                            val priceVal = purchasePriceStr.toDoubleOrNull()
                            val transVal = transportCostStr.toDoubleOrNull()
                            if (isEdit && pig != null) {
                                viewModel.updatePig(
                                    pig!!.copy(
                                        tag_number = tagNumber,
                                        breed = breed,
                                        sex = sex,
                                        source = source,
                                        notes = notes.ifBlank { null },
                                        current_stage_id = selectedStageId,
                                        pen_id = selectedPenId,
                                        batch_id = selectedBatchId,
                                        dam_id = selectedDamId,
                                        sire_id = selectedSireId,
                                        photo_path = photoPath,
                                        origin_farm = originFarm.ifBlank { null },
                                        seller_contact = sellerContact.ifBlank { null },
                                        purchase_price = priceVal,
                                        transport_cost = transVal
                                    )
                                )
                            } else {
                                viewModel.addPig(
                                    tag = tagNumber,
                                    breed = breed,
                                    sex = sex,
                                    source = source,
                                    penId = selectedPenId,
                                    batchId = selectedBatchId,
                                    stageId = selectedStageId,
                                    damId = selectedDamId,
                                    sireId = selectedSireId,
                                    photoPath = photoPath,
                                    originFarm = originFarm.ifBlank { null },
                                    sellerContact = sellerContact.ifBlank { null },
                                    purchasePrice = priceVal,
                                    transportCost = transVal
                                )
                            }
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isEdit) "Save Changes" else "Add Pig", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        placeholder = { Text(placeholder, color = Color(0xFF6E7681)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF4CAF50),
            unfocusedBorderColor = Color(0xFF30363D),
            focusedLabelColor = Color(0xFF4CAF50),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    label: String,
    options: List<Pair<Long?, String>>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == selectedId }?.second ?: label

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4CAF50),
                unfocusedBorderColor = Color(0xFF30363D),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name, color = Color.White) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    }
                )
            }
        }
    }
}
