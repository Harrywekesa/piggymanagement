package com.phms.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phms.app.data.KenyaLocations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelector(
    selectedCounty: String,
    selectedSubCounty: String,
    selectedWard: String,
    onLocationChanged: (county: String, subCounty: String, ward: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var countyExpanded by remember { mutableStateOf(false) }
    var subCountyExpanded by remember { mutableStateOf(false) }
    var wardExpanded by remember { mutableStateOf(false) }

    val counties = KenyaLocations.getCountyNames()
    val subCounties = if (selectedCounty.isNotBlank()) KenyaLocations.getSubCountyNames(selectedCounty) else emptyList()
    val wards = if (selectedCounty.isNotBlank() && selectedSubCounty.isNotBlank()) KenyaLocations.getWardNames(selectedCounty, selectedSubCounty) else emptyList()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // County Dropdown
        ExposedDropdownMenuBox(
            expanded = countyExpanded,
            onExpandedChange = { countyExpanded = !countyExpanded }
        ) {
            OutlinedTextField(
                value = selectedCounty.ifBlank { "Select County" },
                onValueChange = {},
                readOnly = true,
                label = { Text("County", fontSize = 12.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countyExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4CAF50),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            ExposedDropdownMenu(
                expanded = countyExpanded,
                onDismissRequest = { countyExpanded = false }
            ) {
                counties.forEach { county ->
                    DropdownMenuItem(
                        text = { Text(county) },
                        onClick = {
                            countyExpanded = false
                            onLocationChanged(county, "", "")
                        }
                    )
                }
            }
        }

        // Sub-County Dropdown
        ExposedDropdownMenuBox(
            expanded = subCountyExpanded,
            onExpandedChange = { if (selectedCounty.isNotBlank()) subCountyExpanded = !subCountyExpanded }
        ) {
            OutlinedTextField(
                value = selectedSubCounty.ifBlank { if (selectedCounty.isBlank()) "Select County First" else "Select Sub-County" },
                onValueChange = {},
                readOnly = true,
                enabled = selectedCounty.isNotBlank(),
                label = { Text("Sub-County", fontSize = 12.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subCountyExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4CAF50),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            ExposedDropdownMenu(
                expanded = subCountyExpanded,
                onDismissRequest = { subCountyExpanded = false }
            ) {
                subCounties.forEach { subCounty ->
                    DropdownMenuItem(
                        text = { Text(subCounty) },
                        onClick = {
                            subCountyExpanded = false
                            onLocationChanged(selectedCounty, subCounty, "")
                        }
                    )
                }
            }
        }

        // Ward Dropdown
        ExposedDropdownMenuBox(
            expanded = wardExpanded,
            onExpandedChange = { if (selectedSubCounty.isNotBlank()) wardExpanded = !wardExpanded }
        ) {
            OutlinedTextField(
                value = selectedWard.ifBlank { if (selectedSubCounty.isBlank()) "Select Sub-County First" else "Select Ward" },
                onValueChange = {},
                readOnly = true,
                enabled = selectedSubCounty.isNotBlank(),
                label = { Text("Ward", fontSize = 12.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = wardExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4CAF50),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            ExposedDropdownMenu(
                expanded = wardExpanded,
                onDismissRequest = { wardExpanded = false }
            ) {
                wards.forEach { ward ->
                    DropdownMenuItem(
                        text = { Text(ward) },
                        onClick = {
                            wardExpanded = false
                            onLocationChanged(selectedCounty, selectedSubCounty, ward)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationFilterSelector(
    selectedCounty: String,
    selectedSubCounty: String,
    selectedWard: String,
    onFilterChanged: (county: String, subCounty: String, ward: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var countyExpanded by remember { mutableStateOf(false) }
    var subCountyExpanded by remember { mutableStateOf(false) }
    var wardExpanded by remember { mutableStateOf(false) }

    val counties = listOf("All Counties") + KenyaLocations.getCountyNames()
    val subCounties = if (selectedCounty.isNotBlank() && selectedCounty != "All Counties") {
        listOf("All Sub-Counties") + KenyaLocations.getSubCountyNames(selectedCounty)
    } else emptyList()
    val wards = if (selectedCounty.isNotBlank() && selectedCounty != "All Counties" && selectedSubCounty.isNotBlank() && selectedSubCounty != "All Sub-Counties") {
        listOf("All Wards") + KenyaLocations.getWardNames(selectedCounty, selectedSubCounty)
    } else emptyList()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // County Dropdown
        ExposedDropdownMenuBox(
            expanded = countyExpanded,
            onExpandedChange = { countyExpanded = !countyExpanded }
        ) {
            OutlinedTextField(
                value = if (selectedCounty.isBlank()) "All Counties" else selectedCounty,
                onValueChange = {},
                readOnly = true,
                label = { Text("Filter County (47 Counties)", fontSize = 11.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countyExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4CAF50),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            ExposedDropdownMenu(
                expanded = countyExpanded,
                onDismissRequest = { countyExpanded = false }
            ) {
                counties.forEach { county ->
                    DropdownMenuItem(
                        text = { Text(county) },
                        onClick = {
                            countyExpanded = false
                            onFilterChanged(if (county == "All Counties") "All Counties" else county, "All Sub-Counties", "All Wards")
                        }
                    )
                }
            }
        }

        // Sub-County & Ward Row
        if (selectedCounty.isNotBlank() && selectedCounty != "All Counties") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    ExposedDropdownMenuBox(
                        expanded = subCountyExpanded,
                        onExpandedChange = { subCountyExpanded = !subCountyExpanded }
                    ) {
                        OutlinedTextField(
                            value = if (selectedSubCounty.isBlank()) "All Sub-Counties" else selectedSubCounty,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Sub-County", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subCountyExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4CAF50),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = subCountyExpanded,
                            onDismissRequest = { subCountyExpanded = false }
                        ) {
                            subCounties.forEach { subCounty ->
                                DropdownMenuItem(
                                    text = { Text(subCounty) },
                                    onClick = {
                                        subCountyExpanded = false
                                        onFilterChanged(selectedCounty, if (subCounty == "All Sub-Counties") "All Sub-Counties" else subCounty, "All Wards")
                                    }
                                )
                            }
                        }
                    }
                }

                if (selectedSubCounty.isNotBlank() && selectedSubCounty != "All Sub-Counties") {
                    Box(modifier = Modifier.weight(1f)) {
                        ExposedDropdownMenuBox(
                            expanded = wardExpanded,
                            onExpandedChange = { wardExpanded = !wardExpanded }
                        ) {
                            OutlinedTextField(
                                value = if (selectedWard.isBlank()) "All Wards" else selectedWard,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Ward", fontSize = 11.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = wardExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4CAF50),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = wardExpanded,
                                onDismissRequest = { wardExpanded = false }
                            ) {
                                wards.forEach { ward ->
                                    DropdownMenuItem(
                                        text = { Text(ward) },
                                        onClick = {
                                            wardExpanded = false
                                            onFilterChanged(selectedCounty, selectedSubCounty, if (ward == "All Wards") "All Wards" else ward)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
