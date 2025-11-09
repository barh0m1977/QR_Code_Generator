package com.ibrahim.qrcodegenerator.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.core.graphics.toColorInt

class ComposeStyleBottomSheet(
    private val onStyleSelected: (
        fgColor: String,
        bgColor: String,
        eyeStyle: String,
        bodyStyle: String,
        logoUri: Uri?
    ) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
                setContent {
                    val darkTheme = isSystemInDarkTheme()
                    val colorScheme = if (darkTheme)
                        dynamicDarkColorScheme(requireContext())
                    else
                        dynamicLightColorScheme(requireContext())

                    MaterialTheme(colorScheme = colorScheme) {
                        Surface(
                            tonalElevation = 6.dp,
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        ) {
                        StyleBottomSheetContent(
                            onConfirm = { fg, bg, eye, body, logo ->
                                onStyleSelected(fg, bg, eye, body, logo)
                                dismiss()
                            },
                            onDismiss = { dismiss() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StyleBottomSheetContent(
    onConfirm: (String, String, String, String, Uri?) -> Unit,
    onDismiss: () -> Unit
) {
    var fgColor by remember { mutableStateOf("#000000") }
    var bgColor by remember { mutableStateOf("#FFFFFF") }
    var selectedEye by remember { mutableStateOf("Square") }
    var selectedBody by remember { mutableStateOf("Square") }
    var selectedImage by remember { mutableStateOf<Uri?>(null) }

    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            selectedImage = uri
        }

    // Collapsible states
    var fgExpanded by remember { mutableStateOf(false) }
    var bgExpanded by remember { mutableStateOf(false) }
    var eyeExpanded by remember { mutableStateOf(false) }
    var bodyExpanded by remember { mutableStateOf(false) }
    var logoExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("QR Code Style", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        // Foreground Color
        ExpandableSection(
            title = "Foreground Color",
            expanded = fgExpanded,
            onToggle = { fgExpanded = !fgExpanded }
        ) {
            BuiltInColorPicker { color ->
                fgColor = String.format("#%06X", 0xFFFFFF and color.toArgb())
            }
            Text("OR", style = MaterialTheme.typography.titleSmall)
            ColorSelector(
                colors = listOf(
                    "#000000", // Black
                    "#1E1E1E", // Charcoal
                    "#FF0000", // Red
                    "#E91E63", // Pink
                    "#9C27B0", // Purple
                    "#3F51B5", // Indigo
                    "#2196F3", // Blue
                    "#03A9F4", // Light Blue
                    "#00BCD4", // Cyan
                    "#009688", // Teal
                    "#4CAF50", // Green
                    "#77C536", // Light Green
                    "#8BC34A", // Lime
                    "#CDDC39", // Yellow-Green
                    "#FFC107", // Amber
                    "#FF9800", // Orange
                    "#FF5722", // Deep Orange
                    "#795548", // Brown
                    "#607D8B", // Blue Gray
                    "#fc688a", // Soft Pink
                    "#009492"  // Deep Teal
                ),
                selected = fgColor,
                onSelect = { fgColor = it }
            )
        }

        // Background Color
        ExpandableSection(
            title = "Background Color",
            expanded = bgExpanded,
            onToggle = { bgExpanded = !bgExpanded }
        ) {
            BuiltInColorPicker { color ->
                bgColor = String.format("#%06X", 0xFFFFFF and color.toArgb())
            }
            Text("OR", style = MaterialTheme.typography.titleSmall)
            ColorSelector(
                colors = listOf(
                    "#FFFFFF", // White
                    "#F8F8F8", // Light Gray
                    "#E0E0E0", // Silver
                    "#FFFAF0", // Floral White
                    "#FAFAD2", // Light Goldenrod
                    "#FFF8DC", // Cornsilk
                    "#000000", // Black
                    "#1C1C1C", // Charcoal
                    "#2E2E2E", // Dark Gray
                    "#121212", // Almost Black
                    "#F5F5DC", // Beige
                    "#F0FFF0", // Honeydew
                    "#F0F8FF", // Alice Blue
                    "#FAEBD7"  // Antique White
                ) ,
                selected = bgColor,
                onSelect = { bgColor = it }
            )
        }

        // Eye Style
        ExpandableSection(
            title = "Eye Style",
            expanded = eyeExpanded,
            onToggle = { eyeExpanded = !eyeExpanded }
        ) {
            OptionSelector(
                options = listOf("Square", "Rounded", "Circle"),
                selected = selectedEye,
                onSelect = { selectedEye = it }
            )
        }

        // Body Style
        ExpandableSection(
            title = "Body Style",
            expanded = bodyExpanded,
            onToggle = { bodyExpanded = !bodyExpanded }
        ) {
            OptionSelector(
                options = listOf("Square", "Circle", "Rounded", "Diamond"),
                selected = selectedBody,
                onSelect = { selectedBody = it }
            )
        }

        // Logo Picker
        ExpandableSection(
            title = "Logo Image (optional)",
            expanded = logoExpanded,
            onToggle = { logoExpanded = !logoExpanded }
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .clickable { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImage != null) {

                    Image(
                        painter = rememberAsyncImagePainter(selectedImage),
                        contentDescription = null,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Text("Pick", color = Color.DarkGray)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    onConfirm(
                        fgColor,
                        bgColor,
                        selectedEye,
                        selectedBody,
                        selectedImage
                    )
                }) {
                Text("Apply", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun ExpandableSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            )
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
            }
        }
    }
}

@Composable
fun ColorSelector(
    colors: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {

        items(colors) { colorHex ->
            val color = Color(android.graphics.Color.parseColor(colorHex))
            Box(
                modifier = Modifier
                    .size(if (selected == colorHex) 55.dp else 45.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onSelect(colorHex) }
            )
        }
    }
}

@Composable
fun OptionSelector(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(options) { option ->
            OutlinedButton(
                onClick = { onSelect(option) },
                colors = if (selected == option)
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                else ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent)
            ) {
                Text(option)
            }
        }
    }
}
